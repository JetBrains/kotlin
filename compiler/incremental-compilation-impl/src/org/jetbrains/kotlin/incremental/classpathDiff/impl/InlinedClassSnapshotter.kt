/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff.impl

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.LOAD_CONTENTS_OF_CLASSES
import org.jetbrains.kotlin.build.report.metrics.SNAPSHOT_INLINED_CLASSES
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.classpathDiff.impl.ClassListSnapshotterWithInlinedClassSupport.ClassDescriptorForProcessing
import org.jetbrains.kotlin.incremental.impl.ExtraClassInfoGenerator
import org.jetbrains.kotlin.incremental.impl.InstanceOwnerRecordingClassVisitor
import org.jetbrains.kotlin.incremental.impl.hashToLong
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.tree.ClassNode

internal interface ClassMultiHashProvider {
    /**
     * Provides multi-hash of required class' abis.
     *
     * Some of these classes might reference other classes, so the implementation is required to
     * fetch transitive dependencies, deduplicate the whole dependency tree, and only then
     * to apply the multihash (if it's symmetric, which is usually a good trait)
     */
    fun searchAndGetFullAbiHashOfUsedClasses(rootClasses: Set<JvmClassName>, initialPrefix: String): Long
}

internal class ExtraInfoGeneratorWithInlinedClassSnapshotting(
    private val classMultiHashProvider: ClassMultiHashProvider
) : ExtraClassInfoGenerator() {
    private val methodToUsedFqNames = mutableMapOf<JvmMemberSignature.Method, MutableSet<JvmClassName>>()

    override fun makeClassVisitor(classNode: ClassNode): ClassVisitor {
        return InstanceOwnerRecordingClassVisitor(classNode, methodToUsedClassesMap = methodToUsedFqNames)
    }

    override fun calculateInlineMethodHash(
        methodSignature: JvmMemberSignature.Method,
        inlinedClassPrefix: String,
        ownMethodHash: Long
    ): Long {
        val usedInstances = methodToUsedFqNames[methodSignature] ?: mutableSetOf()
        return ownMethodHash xor classMultiHashProvider.searchAndGetFullAbiHashOfUsedClasses(usedInstances, inlinedClassPrefix)
    }
}

private class InstanceBasedSnapshotter(
    private val classNameToClassFileMap: Map<JvmClassName, ClassFileWithContentsProvider>,
    private val classFileToDescriptorMap: Map<ClassFileWithContentsProvider, ClassDescriptorForProcessing>,
    private val metrics: BuildMetricsReporter,
) {

    private val knownClassUsages = HashMap<JvmClassName, Set<JvmClassName>>()

    private fun expandClassSetWithTransitiveDependenciesOnce(fullSet: MutableSet<JvmClassName>): Boolean {
        return fullSet.addAll(fullSet.flatMapTo(mutableSetOf()) { knownClassUsages[it] ?: emptySet() })
    }

    /**
     * It's important to catch both bytecode and the constant pool, so in the first implementation we hash the full class
     */
    private fun extractInlinedSnapshotAndDependenciesFromClassData(classData: ClassFileWithContents): Long {
        //here we want to visit every method, so it's virtually impossible to reuse the classVisitor from regular snapshotting

        val usedClasses = mutableSetOf<JvmClassName>()
        val visitor = InstanceOwnerRecordingClassVisitor(delegateClassVisitor = null, allUsedClassesSet = usedClasses)
        val classReader = ClassReader(classData.contents)
        classReader.accept(visitor, 0)
        knownClassUsages[JvmClassName.byClassId(classData.classInfo.classId)] = usedClasses

        return classData.contents.hashToLong()
    }

    fun findInlinedClassesRecursively(rootClasses: Set<JvmClassName>): Set<JvmClassName> {
        val classesWithTransitiveDependencies = rootClasses.toMutableSet()

        fun getIncompleteClasses() =
            classesWithTransitiveDependencies.zip(classesWithTransitiveDependencies.map { jvmClassName ->
                jvmClassName.toDescriptor()
            }).filter { (_, descriptor) ->
                descriptor != null && descriptor.inlinedSnapshot == null
            }

        var incompleteClasses = getIncompleteClasses()
        while (incompleteClasses.isNotEmpty()) {
            for ((jvmClassName, descriptor) in incompleteClasses) {
                val classFileWithContentsProvider =
                    classNameToClassFileMap[jvmClassName]!! // not null by virtue of `descriptor != null` above
                val classFileWithContents = metrics.measure(LOAD_CONTENTS_OF_CLASSES) {
                    // Assuming that it's OK because these classes are tiny
                    classFileWithContentsProvider.loadContents()
                }
                descriptor!!.inlinedSnapshot = extractInlinedSnapshotAndDependenciesFromClassData(classFileWithContents)
            }
            // we've processed the class files for which we didn't have the inlined snapshot yet,
            // so we might have found new dependencies for them. check:
            if (!expandClassSetWithTransitiveDependenciesOnce(classesWithTransitiveDependencies)) {
                break
            }
            incompleteClasses = getIncompleteClasses()
        }

        while (expandClassSetWithTransitiveDependenciesOnce(classesWithTransitiveDependencies)) {
            // it's possible that the set can still be expanded, if current root classes were all processed previously
        }
        return classesWithTransitiveDependencies
    }

    private fun JvmClassName.toDescriptor(): ClassDescriptorForProcessing? {
        return classNameToClassFileMap[this]?.let { classFileToDescriptorMap[it] }
    }
}

private class PrefixBasedSnapshotter(
    classNameToClassFileMap: Map<JvmClassName, ClassFileWithContentsProvider>,
) {
    private val sortedInternalNames = classNameToClassFileMap.keys.sortedBy { it.internalName }

    fun getSetOfClasses(classPrefix: String): Set<JvmClassName> {
        return getRangeByPredicate(classPrefix).toSet()
    }

    fun getSetOfClasses(addedClasses: Set<JvmClassName>, processedClasses: Set<JvmClassName>): Set<JvmClassName> {
        val accumulator = mutableSetOf<JvmClassName>()
        addedClasses.flatMapTo(accumulator) { addedClass ->
            getRangeByPredicate(addedClass.internalName)
        }.retainAll {
            it !in processedClasses
        }
        return accumulator
    }

    private fun getRangeByPredicate(prefix: String): Iterable<JvmClassName> {
        return object : Iterable<JvmClassName> {
            override fun iterator() = object : Iterator<JvmClassName> {
                var position: Int = sortedInternalNames.binarySearchBy(prefix) { it.internalName }

                init {
                    if (position >= 0) {
                        // exact match found - since we use predicate logic for expanding known class sets, we can skip this item
                        position++
                    } else {
                        // insertion point found - means that further items are "bigger" than this
                        val trueInsertionPoint = -(position + 1)
                        position = trueInsertionPoint
                    }
                }

                override fun hasNext(): Boolean {
                    return position < sortedInternalNames.size
                            &&
                            sortedInternalNames[position].internalName.startsWith(prefix)
                }

                override fun next(): JvmClassName {
                    position++
                    return sortedInternalNames[position - 1]
                }
            }
        }
    }
}

/**
 * Manages inlined class interdependencies.
 *
 * Most of the time we should be dealing with very small class sets and very small dependency graphs.
 */
internal class InlinedClassSnapshotter(
    private val classNameToClassFileMap: Map<JvmClassName, ClassFileWithContentsProvider>,
    private val classFileToDescriptorMap: Map<ClassFileWithContentsProvider, ClassDescriptorForProcessing>,
    private val metrics: BuildMetricsReporter,
): ClassMultiHashProvider {
    private val instanceBasedSnapshotter = InstanceBasedSnapshotter(classNameToClassFileMap, classFileToDescriptorMap, metrics)
    private val prefixBasedSnapshotter = PrefixBasedSnapshotter(classNameToClassFileMap)

    override fun searchAndGetFullAbiHashOfUsedClasses(rootClasses: Set<JvmClassName>, initialPrefix: String): Long {

        metrics.measure(SNAPSHOT_INLINED_CLASSES) {
            var initialClassSet = rootClasses + prefixBasedSnapshotter.getSetOfClasses(initialPrefix)
            var instanceExpandedClassSet = instanceBasedSnapshotter.findInlinedClassesRecursively(initialClassSet)

            while (instanceExpandedClassSet.size > initialClassSet.size) {
                // the loop goes like this: prefix - instance - prefix - ...
                // it allows us to handle cases where inline functions are calling other inline functions in an optimized way
                val setDiff = instanceExpandedClassSet - initialClassSet
                val newClassesByPrefix = prefixBasedSnapshotter.getSetOfClasses(
                    addedClasses = setDiff,
                    processedClasses = instanceExpandedClassSet
                )
                initialClassSet = instanceExpandedClassSet
                instanceExpandedClassSet = instanceExpandedClassSet + instanceBasedSnapshotter.findInlinedClassesRecursively(newClassesByPrefix)
            }

            return instanceExpandedClassSet.fold(0L) { currentHash, nextElement ->
                currentHash xor (nextElement.toDescriptor()?.inlinedSnapshot ?: 0L)
            }
        }
    }

    private fun JvmClassName.toDescriptor(): ClassDescriptorForProcessing? {
        return classNameToClassFileMap[this]?.let { classFileToDescriptorMap[it] }
    }
}
