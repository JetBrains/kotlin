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
    fun searchAndGetFullAbiHashOfUsedClasses(inlinedClassPrefix: String): Long
}

internal class ExtraInfoGeneratorWithInlinedClassSnapshotting(
    private val classMultiHashProvider: ClassMultiHashProvider
) : ExtraClassInfoGenerator() {
    override fun calculateInlineMethodHash(inlinedClassPrefix: String, ownMethodHash: Long): Long {
        return ownMethodHash xor classMultiHashProvider.searchAndGetFullAbiHashOfUsedClasses(inlinedClassPrefix)
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
    /**
     * Both bytecode and constant pool would be copied to the call site with inlining, so we hash the full class
     */
    private fun extractInlinedSnapshotAndDependenciesFromClassData(classData: ClassFileWithContents): Long {
        // in theory we can boot up a ClassVisitor and verify that class' outerClass is aligned with its name,
        // but if it's not, what do we do?
        return classData.contents.hashToLong()
    }

    fun searchAndGetFullAbiHashOfUsedClasses(inlinedClassPrefix: String): SearchAndCalculateOutcome {
        metrics.measure(GradleBuildTime.SNAPSHOT_INLINED_CLASSES) {
            return SearchAndCalculateOutcome(0L, emptyList()) //TODO
        }
    }

    private fun JvmClassName.toDescriptor(): ClassDescriptorForProcessing? {
        return classNameToClassFileMap[this]?.let { classFileToDescriptorMap[it] }
    }
}
