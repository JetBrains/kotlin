/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi
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
    val delimiter: String?,
    val introducedSinceVersion: KotlinReleaseVersion,
    val deprecatedSinceVersion: KotlinReleaseVersion?,
    val removedSinceVersion: KotlinReleaseVersion?,
    val affectsCompilationOutcome: Boolean = true,
) {

    @OptIn(ExperimentalArgumentApi::class)
    class SSoTCompilerArgument(
        val effectiveCompilerName: String,
        origin: KotlinCompilerArgument,
    ) : BtaCompilerArgument<BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType>(
        name = origin.name,
        description = origin.description.current,
        valueType = BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType(origin.argumentType),
        delimiter = origin.delimiter?.toDelimiterString(),
        introducedSinceVersion = origin.releaseVersionsMetadata.introducedVersion,
        deprecatedSinceVersion = origin.releaseVersionsMetadata.deprecatedVersion,
        removedSinceVersion = origin.releaseVersionsMetadata.removedVersion,
        affectsCompilationOutcome = origin.affectsCompilationOutcome,
    ) {
        constructor(origin: KotlinCompilerArgument) : this(origin.calculateName(), origin)
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
        affectsCompilationOutcome: Boolean = true,
        delimiter: String? = null,
    ) : BtaCompilerArgument<BtaCompilerArgumentValueType.CustomArgumentValueType>(
        name = name,
        description = description,
        valueType = valueType,
        delimiter = delimiter,
        introducedSinceVersion = introducedSinceVersion,
        deprecatedSinceVersion = deprecatedSinceVersion,
        removedSinceVersion = removedSinceVersion,
        affectsCompilationOutcome = affectsCompilationOutcome,
    ) {
        constructor(
            origin: KotlinCompilerArgument,
            valueType: BtaCompilerArgumentValueType.CustomArgumentValueType,
            defaultValue: CodeBlock,
        ) : this(
            name = origin.name,
            description = origin.description.current,
            valueType = valueType,
            introducedSinceVersion = origin.releaseVersionsMetadata.introducedVersion,
            deprecatedSinceVersion = origin.releaseVersionsMetadata.deprecatedVersion,
            removedSinceVersion = origin.releaseVersionsMetadata.removedVersion,
            defaultValue = defaultValue,
            applierSimpleName = "apply${
                origin.calculateName().replaceFirstChar { it.uppercase() }
            }",
            affectsCompilationOutcome = origin.affectsCompilationOutcome,
        )
    }
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

    val profileCompilerCommandArgumentFactory = CustomCompilerArgumentFactory(
        valueType = BtaCompilerArgumentValueType.CustomArgumentValueType(
            type = ClassName(API_ARGUMENTS_PACKAGE, "ProfileCompilerCommand").copy(nullable = true),
        ),
        defaultValue = CodeBlock.of("null"),
    )

    val nullabilityAnnotationFactory = CustomCompilerArgumentFactory(
        valueType = BtaCompilerArgumentValueType.CustomArgumentValueType(
            type = listTypeNameOf(ClassName(API_ARGUMENTS_PACKAGE, "NullabilityAnnotation")),
        ),
        defaultValue = CodeBlock.of(
            "%M<%T>()",
            MemberName(KOTLIN_COLLECTIONS, "emptyList"),
            ClassName(API_ARGUMENTS_PACKAGE, "NullabilityAnnotation"),
        ),
    )

    val jsr305Factory = CustomCompilerArgumentFactory(
        valueType = BtaCompilerArgumentValueType.CustomArgumentValueType(
            type = listTypeNameOf(ClassName(API_ARGUMENTS_PACKAGE, "Jsr305")),
        ),
        defaultValue = CodeBlock.of(
            "%M<%T>()",
            MemberName(KOTLIN_COLLECTIONS, "emptyList"),
            ClassName(API_ARGUMENTS_PACKAGE, "Jsr305"),
        ),
    )

    val warningLevel = CustomCompilerArgumentFactory(
        valueType = BtaCompilerArgumentValueType.CustomArgumentValueType(
            type = listTypeNameOf(ClassName(API_ARGUMENTS_PACKAGE, "WarningLevel")),
        ),
        defaultValue = CodeBlock.of(
            "%M<%T>()",
            MemberName(KOTLIN_COLLECTIONS, "emptyList"),
            ClassName(API_ARGUMENTS_PACKAGE, "WarningLevel"),
        ),
    )
}

class CustomCompilerArgumentFactory(
    private val valueType: BtaCompilerArgumentValueType.CustomArgumentValueType,
    private val defaultValue: CodeBlock,
) {
    fun create(origin: KotlinCompilerArgument): BtaCompilerArgument.CustomCompilerArgument =
        BtaCompilerArgument.CustomCompilerArgument(origin, valueType, defaultValue)
}

private fun KotlinCompilerArgument.Delimiter.toDelimiterString(): String? =
    when (this) {
        KotlinCompilerArgument.Delimiter.Default -> ","
        KotlinCompilerArgument.Delimiter.None -> null
        KotlinCompilerArgument.Delimiter.PathSeparator -> $$"${File.pathSeparator}"
        KotlinCompilerArgument.Delimiter.Space -> " "
        KotlinCompilerArgument.Delimiter.Semicolon -> ";"
    }
