/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

sealed class ClassInfoGeneratorContext()

/**
 * We need a "default" context for generic KotlinClassInfo.createFrom users, and it must be immutable,
 * so sealed class looks like a good solution (singleton version of the api has no mutable state, and generic version
 * can work with some version of context)
 */
object DefaultClassInfoGeneratorContext : ClassInfoGeneratorContext()

/**
 * This version of [ClassInfoGeneratorContext] can be used in ClasspathSnapshot transform
 * to enable coordination on the jar snapshotter level:
 *
 * in the initial pass, inline functions are detected, and inlined local classes are noted
 * in the second pass, these local classes are processed, and their contents are used as part of inline function hash
 *
 * The intuition is, if there're inline functions in a module, there are a lot of them, and they invoke each other.
 * So a single local class could be used in multiple inline functions, and ...
 *
 * //TODO finish that thought
 * //TODO(KT-62555) more importantly, test scenario with f1(f2(localF3())) - is the f1 hash affected by change in localf3? it should, shouldn't it?
 * (maybe not, depends on the actual behavior down the line)
 */
data class ClassInfoGeneratorContextWithLocalClassSnapshotting(
    val localClassHashProvider: (FqName) -> Long,
    val methodToUsedClassesMap: HashMap<MethodWithOwner, MutableSet<FqName>> = HashMap()
) : ClassInfoGeneratorContext() {

    data class MethodWithOwner(
        val owner: String, val method: JvmMemberSignature.Method
    )

    fun addFqNameUsage(method: MethodWithOwner, usedType: FqName) {
        methodToUsedClassesMap.getOrPut(method) { mutableSetOf() }.add(usedType)
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
