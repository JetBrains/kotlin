/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff.impl

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
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
    fun searchAndGetFullAbiHashOfUsedClasses(rootClasses: Set<JvmClassName>): Long
}

internal class ExtraInfoGeneratorWithInlinedClassSnapshotting(
    private val classMultiHashProvider: ClassMultiHashProvider
) : ExtraClassInfoGenerator() {
    private val methodToUsedFqNames = mutableMapOf<JvmMemberSignature.Method, MutableSet<JvmClassName>>()

    override fun makeClassVisitor(classNode: ClassNode): ClassVisitor {
        return InstanceOwnerRecordingClassVisitor(classNode, methodToUsedClassesMap = methodToUsedFqNames)
    }

    override fun calculateInlineMethodHash(methodSignature: JvmMemberSignature.Method, ownMethodHash: Long): Long {
        val usedInstances = methodToUsedFqNames[methodSignature] ?: mutableSetOf()
        return ownMethodHash xor classMultiHashProvider.searchAndGetFullAbiHashOfUsedClasses(usedInstances)
    }
}

internal class SearchAndCalculateOutcome(
    val calculatedHash: Long,
    val loadedClasses: List<Pair<ClassDescriptorForProcessing, ClassFileWithContents>>,
)

/**
 * Manages inlined class interdependencies.
 *
 * Most of the time we should be dealing with very small class sets and very small dependency graphs.
 */
internal class InlinedClassSnapshotter(
    private val classNameToClassFileMap: Map<JvmClassName, ClassFileWithContentsProvider>,
    private val classFileToDescriptorMap: Map<ClassFileWithContentsProvider, ClassDescriptorForProcessing>,
    private val metrics: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
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

    fun searchAndGetFullAbiHashOfUsedClasses(rootClasses: Set<JvmClassName>): SearchAndCalculateOutcome {
        metrics.measure(GradleBuildTime.SNAPSHOT_INLINED_CLASSES) {
            val classesWithTransitiveDependencies = rootClasses.toMutableSet()
            val queueForRegularSnapshot = mutableListOf<Pair<ClassDescriptorForProcessing, ClassFileWithContents>>()

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
                    val classFileWithContents = metrics.measure(GradleBuildTime.LOAD_CONTENTS_OF_CLASSES) {
                        classFileWithContentsProvider.loadContents()
                    }
                    descriptor!!.inlinedSnapshot = extractInlinedSnapshotAndDependenciesFromClassData(classFileWithContents)

                    if (descriptor.snapshot == null) {
                        queueForRegularSnapshot.add(descriptor to classFileWithContents)
                    }
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
            val finalHash = classesWithTransitiveDependencies.fold(0L) { currentHash, nextElement ->
                currentHash xor (nextElement.toDescriptor()?.inlinedSnapshot ?: 0L)
            }
            return SearchAndCalculateOutcome(finalHash, queueForRegularSnapshot)
        }
    }

    private fun JvmClassName.toDescriptor(): ClassDescriptorForProcessing? {
        return classNameToClassFileMap[this]?.let { classFileToDescriptorMap[it] }
    }
}
