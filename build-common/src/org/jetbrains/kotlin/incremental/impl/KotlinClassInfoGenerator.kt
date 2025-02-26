/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

sealed class ClassInfoGeneratorContext()

/**
 * We need a "default" context for generic KotlinClassInfo.createFrom users, it must be simple and immutable
 */
object DefaultClassInfoGeneratorContext : ClassInfoGeneratorContext()

/**
 * This version of [ClassInfoGeneratorContext] is used to enable snapshotting of inlined local classes.
 */
data class ClassInfoGeneratorContextWithLocalClassSnapshotting(
    val multiHashProvider: ClassMultiHashProvider,
) : ClassInfoGeneratorContext() {

    interface ClassMultiHashProvider {
        /**
         * Provides multi-hash of required class' abis.
         *
         * Some of these classes might reference other classes, so the implementation is required to
         * fetch transitive dependencies, deduplicate the whole dependency tree, and only then
         * to apply the multihash (if it's symmetric, which is probably a good trait)
         */
        fun searchAndGetFullAbiHashOfUsedClasses(rootClasses: Set<JvmClassName>): Long
    }
}

/**
 * We need to provide the normal behavior for compatibility with pre-depgraph JPS,
 * but we also need to allow configurable behavior for Gradle Classpath Snapshotting transformations
 *
 * Current jps implements its own module structure model, so eventually
 * api snapshotting logic could be removed from build-common
 */
class KotlinClassInfoGenerator(
    val context: ClassInfoGeneratorContext = DefaultClassInfoGeneratorContext
) {
    fun createFrom(classId: ClassId, classHeader: KotlinClassHeader, classContents: ByteArray): KotlinClassInfo {
        return KotlinClassInfo(
            classId,
            classHeader.kind,
            classHeader.data ?: classHeader.incompatibleData ?: emptyArray(),
            classHeader.strings ?: emptyArray(),
            classHeader.multifileClassName,
            extraInfo = ExtraClassInfoGenerator.getExtraInfo(classHeader, classContents, context)
        )
    }
}
