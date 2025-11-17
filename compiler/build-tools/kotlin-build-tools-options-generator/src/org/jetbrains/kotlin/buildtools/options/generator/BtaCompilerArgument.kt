/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import kotlin.reflect.KType

/**
 * Public facade used by the build-tools options generator. Wraps the single source of truth (arguments DSL)
 * without exposing its internal types directly to the generator call sites.
 */
sealed class BtaCompilerArgument(
    val name: String,
    val description: String,
    val valueType: BtaCompilerArgumentValueType,
    val introducedSinceVersion: KotlinReleaseVersion,
    val deprecatedSinceVersion: KotlinReleaseVersion?,
    val removedSinceVersion: KotlinReleaseVersion?,
) {
    class SSoTCompilerArgument(
        origin: KotlinCompilerArgument,
    ) : BtaCompilerArgument(
        name = origin.name,
        description = origin.description.current,
        valueType = BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType(origin.valueType),
        introducedSinceVersion = origin.releaseVersionsMetadata.introducedVersion,
        deprecatedSinceVersion = origin.releaseVersionsMetadata.deprecatedVersion,
        removedSinceVersion = origin.releaseVersionsMetadata.removedVersion
    )
}

/**
 * Public abstraction of a compiler argument value type.
 */
sealed class BtaCompilerArgumentValueType(
    val isNullable: Boolean = false,
) {
    class SSoTCompilerArgumentValueType(
        val origin: KotlinArgumentValueType<*>
    ) : BtaCompilerArgumentValueType(isNullable = origin.isNullable.current)

    class CustomArgumentValueType(
        val type: KType,
        isNullable: Boolean = false,
    ) : BtaCompilerArgumentValueType(isNullable = isNullable)
}