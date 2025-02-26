/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff.impl

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.incremental.classpathDiff.InaccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.impl.SingleClassSnapshotter.snapshotJavaClass
import org.jetbrains.kotlin.incremental.classpathDiff.impl.SingleClassSnapshotter.snapshotKotlinClass
import org.jetbrains.kotlin.incremental.impl.ClassInfoGeneratorContextWithLocalClassSnapshotting
import org.jetbrains.kotlin.incremental.impl.ClassInfoGeneratorContextWithLocalClassSnapshotting.ClassMultiHashProvider
import org.jetbrains.kotlin.incremental.impl.DefaultClassInfoGeneratorContext
import org.jetbrains.kotlin.incremental.impl.KotlinClassInfoGenerator
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.CLASS
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.SYNTHETIC_CLASS
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import kotlin.math.ceil


/**
 * Computes [ClassSnapshot]s of classes.
 *
 * A few key restrictions to consider:
 *
 * 1. We don't want to keep all loaded classContents in memory at the same time. It can easily be a 400 mb fatJar,
 * and it can put severe stress on user's Gradle daemon - even before you start calculating multiple classpathEntrySnapshots in parallel.
 *
 * 2. We must handle ill-formed jars gracefully. It can be purposefully edited, or it can be a corrupted published jar of an old library.
 * Either way, if it's good enough for compiler, it must be good enough for snapshotter too.
 */
internal sealed interface ClassListSnapshotter {
    fun snapshot(): List<ClassSnapshot>
}

internal class PlainClassListSnapshotter(
    private val classes: List<ClassFileWithContentsProvider>,
    private val settings: ClasspathEntrySnapshotter.Settings,
    private val metrics: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric> = DoNothingBuildMetricsReporter
) : ClassListSnapshotter {
    private val classNameToClassFileMap: Map<JvmClassName, ClassFileWithContentsProvider> = classes.associateBy { it.classFile.getClassName() }
    private val classFileToSnapshotMap = mutableMapOf<ClassFileWithContentsProvider, ClassSnapshot>()

    private val classInfoGenerator = KotlinClassInfoGenerator(DefaultClassInfoGeneratorContext)

    private fun snapshotClass(classFile: ClassFileWithContentsProvider): ClassSnapshot {
        return classFileToSnapshotMap.getOrPut(classFile) {
            val clazz = metrics.measure(GradleBuildTime.LOAD_CONTENTS_OF_CLASSES) {
                classFile.loadContents()
            }
            // Snapshot outer class first as we need this info to determine whether a class is transitively inaccessible (see below)
            val outerClassSnapshot = clazz.classInfo.classId.outerClassId?.let { outerClassId ->
                val outerClassFile = classNameToClassFileMap[JvmClassName.byClassId(outerClassId)]
                // It's possible that the outer class is not found in the given classes (it could happen with faulty jars)
                outerClassFile?.let { snapshotClass(it) }
            }
            when {
                // We don't need to snapshot (directly or transitively) inaccessible classes.
                // A class is transitively inaccessible if its outer class is inaccessible.
                clazz.classInfo.isInaccessible() || outerClassSnapshot is InaccessibleClassSnapshot -> {
                    InaccessibleClassSnapshot
                }
                clazz.classInfo.isKotlinClass -> metrics.measure(GradleBuildTime.SNAPSHOT_KOTLIN_CLASSES) {
                    snapshotKotlinClass(clazz, settings.granularity, classInfoGenerator = classInfoGenerator)
                }
                else -> metrics.measure(GradleBuildTime.SNAPSHOT_JAVA_CLASSES) {
                    snapshotJavaClass(clazz, settings.granularity)
                }
            }
        }
    }

    override fun snapshot(): List<ClassSnapshot> {
        return classes.map { snapshotClass(it) }
    }
}

/**
 * Snapshotting inlined classes allows us to handle relevant types of cross-module incremental changes.
 *
 * See [org.jetbrains.kotlin.buildtools.api.tests.compilation.InlinedLambdaChangeTest] for examples where inlined lambdas affect other modules,
 * and be aware that the problem is more general (it can be other anonymous or named classes, it can be lambdas-in-lambdas, etc).
 *
 * Snapshotting inaccessible classes adds a few more "key restrictions":
 *
 * 1. We don't want to calculate "inlined snapshot" for every class in the class list, because usually most classes in a jar
 * are not defined inside an inline function.
 *
 * 2. We don't know that a local class is used by an inline function until we snapshot that inline function, so in a general case
 * we might have to backtrack and to load the local class twice (first to create a regular snapshot, and second to create an "inlined snapshot")
 *
 * 3. It is possible for one inlined class to reference another inlined class, or e.g. for inline functions from different classes to reference
 * each other's outer class. So the implementation must be robust against cyclical dependencies in inputs.
 */
internal class ClassListSnapshotterWithInlinedClassSupport(
    private val classes: List<ClassFileWithContentsProvider>,
    private val settings: ClasspathEntrySnapshotter.Settings,
    private val metrics: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric> = DoNothingBuildMetricsReporter
) : ClassListSnapshotter, ClassMultiHashProvider {

    private val classNameToClassFileMap = classes.associateByTo(
        unorderedMutableMapForClassCount<JvmClassName, ClassFileWithContentsProvider>()
    ) { it.classFile.getClassName() }
    private val classFileToDescriptorMap = unorderedMutableMapForClassCount<ClassFileWithContentsProvider, ClassDescriptorForProcessing>()

    init {
        for (inputClass in classes) {
            // we always need a descriptor for all classes, because we always produce a regular snapshot.
            // in practice, descriptor enables us to share loaded data between different types of processing.
            classFileToDescriptorMap[inputClass] = ClassDescriptorForProcessing()
        }
    }

    private class ClassDescriptorForProcessing(
        var snapshot: ClassSnapshot? = null,
        var inlinedSnapshot: Long? = null
    )

    private val inlinedClassSnapshotter = InlinedClassSnapshotter()

    private val classInfoGenerator = KotlinClassInfoGenerator(
        ClassInfoGeneratorContextWithLocalClassSnapshotting(multiHashProvider = this as ClassMultiHashProvider)
    )

    /**
     * regular snapshot might depend on a number of inlined snapshots
     *
     * inlined snapshots might add regular snapshots to queue
     *
     * it's impossible for regular snapshots to loop if the input is well-formed
     * (i.e. doesn't have two classes which are each other's outer classes, or a larger cycle like this)
     */
    private val queueForRegularSnapshot = mutableListOf<Pair<ClassDescriptorForProcessing, ClassFileWithContents>>()

    override fun snapshot(): List<ClassSnapshot> {
        return classes.map {
            val mapped = makeOrReuseClassSnapshot(it)

            // regular snapshotting might require inlined snapshotting; that leaves us with the queue of loaded class contents to process
            for ((descriptor, contents) in queueForRegularSnapshot) {
                // it's unlikely that queue would grow significantly during processing. it requires inner named classes to have inline functions
                // another example: if you consider an "util" class with 200 inline functions, there might be a big number of loaded classes in
                // queue, but they are likely to be tiny.
                makeOrReuseClassSnapshot(descriptor, contents)
            }
            queueForRegularSnapshot.clear()

            mapped
        }
    }

    private fun makeOrReuseClassSnapshot(classFile: ClassFileWithContentsProvider): ClassSnapshot {
        val descriptor = classFileToDescriptorMap[classFile] ?: error("snapshotter's state is broken, got no descriptor for $classFile")

        descriptor.snapshot?.let { return it }

        val classFileWithContents = metrics.measure(GradleBuildTime.LOAD_CONTENTS_OF_CLASSES) {
            classFile.loadContents()
        }

        return makeOrReuseClassSnapshot(descriptor, classFileWithContents)
    }

    private fun makeOrReuseClassSnapshot(descriptor: ClassDescriptorForProcessing, classFileWithContents: ClassFileWithContents): ClassSnapshot {
        descriptor.snapshot?.let { return it }

        val snapshot = if (isInaccessible(classFileWithContents)) {
            InaccessibleClassSnapshot
        } else if (classFileWithContents.classInfo.isKotlinClass) {
            metrics.measure(GradleBuildTime.SNAPSHOT_KOTLIN_CLASSES) {
                snapshotKotlinClass(classFileWithContents, settings.granularity, classInfoGenerator = classInfoGenerator)
                // beware of recursion - classInfoGenerator might require inlinedSnapshots!
                // inlinedSnapshots calculation must not directly call regular snapshotting to prevent infinite loops
            }
        } else {
            metrics.measure(GradleBuildTime.SNAPSHOT_JAVA_CLASSES) {
                snapshotJavaClass(classFileWithContents, settings.granularity)
            }
        }

        descriptor.snapshot = snapshot
        return snapshot
    }

    private fun isInaccessible(classFileWithContents: ClassFileWithContents): Boolean {
        val answer = if (classFileWithContents.classInfo.isInaccessible()) {
            true
        } else {
            val outerClassJvmName = classFileWithContents.classInfo.classId.outerClassId?.let { JvmClassName.byClassId(it) }
            val outerClassProvider = outerClassJvmName?.let { classNameToClassFileMap[outerClassJvmName] }
            outerClassProvider?.let { makeOrReuseClassSnapshot(outerClassProvider) } is InaccessibleClassSnapshot
        }

        return answer
    }

    // we don't support reusing loaded data from regular snapshot to inlined snapshot, because the flow would require extra state
    // (we know about loaded data on [makeOrReuseClassSnapshot] level, and we know of the need to do the inlined snapshot
    // on [searchAndGetFullAbiHashOfUsedClasses] level, which is effectively a callback)
    override fun searchAndGetFullAbiHashOfUsedClasses(rootClasses: Set<JvmClassName>): Long {
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
                val classFileWithContents = metrics.measure(GradleBuildTime.LOAD_CONTENTS_OF_CLASSES) {
                    classFileWithContentsProvider.loadContents()
                }
                descriptor!!.inlinedSnapshot = metrics.measure(GradleBuildTime.SNAPSHOT_INLINED_CLASSES) {
                    inlinedClassSnapshotter.extractInlinedSnapshotAndDependenciesFromClassData(classFileWithContents)
                }

                if (descriptor.snapshot == null) {
                    queueForRegularSnapshot.add(descriptor to classFileWithContents)
                }
            }
            // we've processed the class files for which we didn't have the inlined snapshot yet,
            // so we might have found new dependencies for them. check:
            if (!inlinedClassSnapshotter.expandClassSetWithTransitiveDependenciesOnce(classesWithTransitiveDependencies)) {
                break
            }
            incompleteClasses = getIncompleteClasses()
        }

        while (inlinedClassSnapshotter.expandClassSetWithTransitiveDependenciesOnce(classesWithTransitiveDependencies)) {
            // it's possible that the set can still be expanded, if current root classes were all processed previously
        }
        return classesWithTransitiveDependencies.fold(0L) { currentHash, nextElement ->
            currentHash xor (nextElement.toDescriptor()?.inlinedSnapshot ?: 0L)
        }
    }

    private fun <K, V> unorderedMutableMapForClassCount(): MutableMap<K, V> {
        val capacityForNoResizes = ceil(classes.count() / 0.75).toInt()
        return HashMap<K, V>(capacityForNoResizes)
    }

    private fun JvmClassName.toDescriptor(): ClassDescriptorForProcessing? {
        return classNameToClassFileMap[this]?.let { classFileToDescriptorMap[it] }
    }
}

/**
 * Returns `true` if this class is inaccessible, and `false` otherwise (or if we don't know).
 *
 * A class is inaccessible if it can't be referenced from other source files (and therefore any changes in an inaccessible class will
 * not require recompilation of other source files).
 */
private fun BasicClassInfo.isInaccessible(): Boolean {
    return when {
        isKotlinClass -> when (kotlinClassHeader!!.kind) {
            CLASS -> isPrivate || isLocal || isAnonymous || isSynthetic
            SYNTHETIC_CLASS -> true
            else -> false // We don't know about the other class kinds
        }
        else -> isPrivate || isLocal || isAnonymous || isSynthetic
    }
}

private fun ClassFile.getClassName(): JvmClassName {
    check(unixStyleRelativePath.endsWith(".class", ignoreCase = true))
    return JvmClassName.byInternalName(unixStyleRelativePath.dropLast(".class".length))
}
