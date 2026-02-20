/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.cli.arguments.generator.calculateName
import org.jetbrains.kotlin.generators.kotlinpoet.listTypeNameOf
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes

/**
 * Public facade used by the build-tools options generator. Wraps the single source of truth (arguments DSL)
 * without exposing its internal types directly to the generator call sites.
 */
sealed class BtaCompilerArgument<T : BtaCompilerArgumentValueType>(
    val name: String,
    val description: String,
    val valueType: T,
    val introducedSinceVersion: KotlinReleaseVersion,
    val deprecatedSinceVersion: KotlinReleaseVersion?,
    val removedSinceVersion: KotlinReleaseVersion?,
) {

    class SSoTCompilerArgument(
        val effectiveCompilerName: String,
        origin: KotlinCompilerArgument,
    ) : BtaCompilerArgument<BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType>(
        name = origin.name,
        description = origin.description.current,
        valueType = BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType(origin.argumentType),
        introducedSinceVersion = origin.releaseVersionsMetadata.introducedVersion,
        deprecatedSinceVersion = origin.releaseVersionsMetadata.deprecatedVersion,
        removedSinceVersion = origin.releaseVersionsMetadata.removedVersion
    ) {
        constructor(origin: KotlinCompilerArgument) : this(origin.calculateName(), origin)
    }

    class SSoTCompilerArgumentCompat(
        origin: SSoTCompilerArgument,
    ) : BtaCompilerArgument<BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType>(
        name = origin.name,
        description = origin.description,
        valueType = origin.valueType,
        introducedSinceVersion = origin.introducedSinceVersion,
        deprecatedSinceVersion = origin.deprecatedSinceVersion,
        removedSinceVersion = origin.removedSinceVersion
    ) {
        constructor(origin: KotlinCompilerArgument) : this(SSoTCompilerArgument(origin))

        val effectiveCompilerName: String = origin.effectiveCompilerName
        val applierSimpleName = "apply${
            origin.effectiveCompilerName.replaceFirstChar { it.uppercase() }
        }"
    }

    class CustomCompilerArgument(
        name: String,
        description: String,
        valueType: BtaCompilerArgumentValueType.CustomArgumentValueType,
        introducedSinceVersion: KotlinReleaseVersion,
        deprecatedSinceVersion: KotlinReleaseVersion?,
        removedSinceVersion: KotlinReleaseVersion?,
        val applierSimpleName: String,
        val defaultValue: CodeBlock,
    ) : BtaCompilerArgument<BtaCompilerArgumentValueType.CustomArgumentValueType>(
        name = name,
        description = description,
        valueType = valueType,
        introducedSinceVersion = introducedSinceVersion,
        deprecatedSinceVersion = deprecatedSinceVersion,
        removedSinceVersion = removedSinceVersion,
    )
}

/**
 * Public abstraction of a compiler argument value type.
 */
sealed class BtaCompilerArgumentValueType(
    val isNullable: Boolean = false,
) {
    class SSoTCompilerArgumentValueType(
        val origin: KotlinArgumentValueType<*>,
    ) : BtaCompilerArgumentValueType(isNullable = origin.isNullable.current) {
        val kType: KType
            get() = origin::class.allSupertypes.single { it.classifier == KotlinArgumentValueType::class }.arguments.first().type!!
    }

    class CustomArgumentValueType(
        val type: TypeName,
    ) : BtaCompilerArgumentValueType(isNullable = type.isNullable)
}

object CustomCompilerArguments {
    val compilerPlugins = BtaCompilerArgument.CustomCompilerArgument(
        name = "compiler-plugins",
        description = "List of compiler plugins to load for this compilation.",
        valueType = BtaCompilerArgumentValueType.CustomArgumentValueType(
            type = listTypeNameOf(
                ClassName(
                    API_ARGUMENTS_PACKAGE,
                    "CompilerPlugin"
                )
            ),
        ),
        introducedSinceVersion = KotlinReleaseVersion.v2_3_20,
        deprecatedSinceVersion = null,
        removedSinceVersion = null,
        applierSimpleName = "applyCompilerPlugins",
        defaultValue = CodeBlock.of(
            "%M<%T>()",
            MemberName("kotlin.collections", "emptyList"),
            ClassName(API_ARGUMENTS_PACKAGE, "CompilerPlugin")
        ),
    )
}