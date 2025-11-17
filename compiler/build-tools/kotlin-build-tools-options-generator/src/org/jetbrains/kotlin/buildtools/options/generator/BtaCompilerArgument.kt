/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.cli.arguments.generator.calculateName
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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
        val effectiveCompilerName: String,
        origin: KotlinCompilerArgument,
    ) : BtaCompilerArgument(
        name = origin.name,
        description = origin.description.current,
        valueType = BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType(origin.valueType),
        introducedSinceVersion = origin.releaseVersionsMetadata.introducedVersion,
        deprecatedSinceVersion = origin.releaseVersionsMetadata.deprecatedVersion,
        removedSinceVersion = origin.releaseVersionsMetadata.removedVersion
    ) {
        constructor(origin: KotlinCompilerArgument) : this(origin.calculateName(), origin)
    }

    class CustomCompilerArgument(
        name: String,
        description: String,
        valueType: BtaCompilerArgumentValueType,
        introducedSinceVersion: KotlinReleaseVersion,
        deprecatedSinceVersion: KotlinReleaseVersion?,
        removedSinceVersion: KotlinReleaseVersion?,
        val generateConverters: (theClass: MemberName, argument: BtaCompilerArgument, name: String, wasIntroducedRecently: Boolean, wasRemoved: Boolean, toCompilerConverterFun: FunSpec.Builder, generateCompatLayer: Boolean) -> Unit,
    ) : BtaCompilerArgument(
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
        val origin: KotlinArgumentValueType<*>
    ) : BtaCompilerArgumentValueType(isNullable = origin.isNullable.current)

    class CustomArgumentValueType(
        val type: KType,
        isNullable: Boolean = false,
    ) : BtaCompilerArgumentValueType(isNullable = isNullable)
}

object CustomCompilerArguments {
    val compilerPlugins = BtaCompilerArgument.CustomCompilerArgument(
        name = "compiler-plugins",
        description = "List of compiler plugins to load for this compilation.",
        valueType = BtaCompilerArgumentValueType.CustomArgumentValueType(type = typeOf<List<CompilerPlugin>>(), isNullable = false),
        introducedSinceVersion = KotlinReleaseVersion.v2_3_20,
        deprecatedSinceVersion = null,
        removedSinceVersion = null,
        generateConverters = { theClass: MemberName, argument: BtaCompilerArgument, name: String, wasIntroducedRecently: Boolean, wasRemoved: Boolean, toCompilerConverterFun: FunSpec.Builder, generateCompatLayer: Boolean ->
            val relation = ClassName(API_ARGUMENTS_PACKAGE, "CompilerPluginPartialOrderRelation")
            val absolutePathString = MemberName("kotlin.io.path", "absolutePathString")

            CodeBlock.builder().apply {
                beginControlFlow("if (%M in this)", theClass)
                add("val compilerPlugins = get(%M)\n", theClass)
                add(
                    "arguments.pluginClasspaths = compilerPlugins.flatMap { it.classpath }.map { it.%M() }.toTypedArray()\n",
                    absolutePathString,
                )
                add(
                    $$"arguments.pluginOptions = compilerPlugins.flatMap { plugin -> plugin.rawArguments.map { option -> \"plugin:${plugin.pluginId}:${option.key}=${option.value}\" } }.toTypedArray()\n"
                )
                add(
                    $$"arguments.pluginOrderConstraints = compilerPlugins.flatMap { plugin -> plugin.orderingRequirements.map { order -> when (order.relation) { %T.BEFORE -> \"${plugin.pluginId}<${order.otherPluginId}\"; %T.AFTER -> \"${order.otherPluginId}>${plugin.pluginId}\" } } }.toTypedArray()\n",
                    relation,
                    relation,
                )
                endControlFlow()
            }.build().also { block ->
                toCompilerConverterFun.addSafeSetStatement(
                    wasIntroducedRecently,
                    wasRemoved,
                    name,
                    argument,
                    block,
                    generateCompatLayer,
                )
            }
        },
    )
}