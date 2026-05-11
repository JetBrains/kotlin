/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.generator

import org.jetbrains.kotlin.arguments.description.*
import org.jetbrains.kotlin.arguments.description.removed.removedCommonCompilerArguments
import org.jetbrains.kotlin.arguments.description.removed.removedJvmCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.generator.BtaCompilerArgument.CustomCompilerArgument
import org.jetbrains.kotlin.buildtools.generator.BtaCompilerArgument.SSoTCompilerArgument
import org.jetbrains.kotlin.cli.arguments.generator.calculateName

sealed interface ArgumentTransform {
    object NoOp : ArgumentTransform
    object Drop : ArgumentTransform

    /**
     * Like [Drop], the argument is not exposed in the public API.
     * Additionally, attempts to set this argument via [applyArgumentStrings][org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.applyArgumentStrings]
     * will produce a warning (starting from [warningSince]) or an error (starting from [errorSince], if specified).
     */
    class Restrict(
        val reason: String? = null,
        val warningSince: KotlinReleaseVersion,
        val errorSince: KotlinReleaseVersion? = null,
    ) : ArgumentTransform

    class CustomArgument(val argument: CustomCompilerArgument) : ArgumentTransform
    class Override(val argument: CustomCompilerArgument) : ArgumentTransform
    class Fix(val argument: KotlinCompilerArgument) : ArgumentTransform
//    data class Rename(val to: String) : ArgumentTransform // possible future operations
}

private val levelsToArgumentTransforms: Map<String, Map<String, ArgumentTransform>> = buildMap {
    put(actualCommonCompilerArguments.name, buildMap {
        with(actualCommonCompilerArguments) {
            drop("script")
            restrict("Xrepl", warningSince = KotlinReleaseVersion.v2_4_0, errorSince = KotlinReleaseVersion.v2_5_0)
            drop("Xstdlib-compilation")
            drop("Xallow-kotlin-package")
            drop("P")
            drop("Xplugin")
            drop("Xcompiler-plugin")
            drop("Xcompiler-plugin-order")
            drop("Xintellij-plugin-root")
            drop("Xcommon-sources")
            restrict(
                "Xenable-incremental-compilation",
                reason = "Configure it via the JvmCompilationOperation.INCREMENTAL_COMPILATION option instead.",
                warningSince = KotlinReleaseVersion.v2_4_0,
                errorSince = KotlinReleaseVersion.v2_5_0
            ) // managed by BTA, conflicts with its IC handling
            custom(CustomCompilerArguments.compilerPlugins)
            override("Xwarning-level", CustomCompilerArguments.warningLevel)

            // KMP related
            drop("Xmulti-platform")
            drop("Xno-check-actual")
            drop("Xfragments")
            drop("Xfragment-sources")
            drop("Xfragment-refines")
            drop("Xfragment-dependency")
            drop("Xseparate-kmp-compilation")
            drop("Xdirect-java-actualization")
            drop("Xfragment-friend-dependency")

            // "wrong" metadata in argument description - argument existed before, but was added to argument description in 2.3.0
            fix("XXdump-model") { it.copy(releaseVersionsMetadata = it.releaseVersionsMetadata.copy(introducedVersion = KotlinReleaseVersion.v2_3_0)) }
            fix("XXLanguage") { it.copy(releaseVersionsMetadata = it.releaseVersionsMetadata.copy(introducedVersion = KotlinReleaseVersion.v2_3_0)) }
        }
        with(removedCommonCompilerArguments) {
            drop("Xuse-k2")
        }
    })
    put(actualCommonToolsArguments.name, buildMap {
        with(actualCommonToolsArguments) {
            drop("help")
            drop("X")
        }
    })
    put(actualMetadataArguments.name, buildMap {
        with(actualMetadataArguments) {
            restrict(
                "d",
                reason = "The destination is configured via the destinationDirectory parameter of jvmCompilationOperationBuilder.",
                warningSince = KotlinReleaseVersion.v2_4_0,
                errorSince = KotlinReleaseVersion.v2_5_0
            ) // configured explicitly when instantiating operations
            drop("Xlegacy-metadata-jar-k2")
        }
    })
    put(actualJvmCompilerArguments.name, buildMap {
        with(actualJvmCompilerArguments) {
            restrict(
                "d",
                reason = "The destination is configured via the destinationDirectory parameter of jvmCompilationOperationBuilder.",
                warningSince = KotlinReleaseVersion.v2_4_0,
                errorSince = KotlinReleaseVersion.v2_5_0
            ) // configured explicitly when instantiating operations
            restrict("expression", warningSince = KotlinReleaseVersion.v2_4_0, errorSince = KotlinReleaseVersion.v2_5_0)
            restrict(
                "include-runtime",
                warningSince = KotlinReleaseVersion.v2_4_0,
                errorSince = KotlinReleaseVersion.v2_5_0
            ) // we're only considering building into directories for now (not jars)
            restrict(
                "Xbuild-file",
                warningSince = KotlinReleaseVersion.v2_4_0,
                errorSince = KotlinReleaseVersion.v2_5_0
            ) // breaks incremental compilation (KT-75540)
            override("Xprofile", CustomCompilerArguments.profileCompilerCommandArgumentFactory)
            override("Xnullability-annotations", CustomCompilerArguments.nullabilityAnnotationFactory)
            override("Xjsr305", CustomCompilerArguments.jsr305Factory)
        }
        with(removedJvmCompilerArguments) {
            drop("Xuse-javac")
            drop("Xcompile-java")
            drop("Xjavac-arguments")
        }
    })
    put(actualCommonJsAndWasmArguments.name, buildMap {
        with(actualCommonJsAndWasmArguments) {
            drop("Xir-produce-js")
            drop("Xir-produce-klib-dir")
            drop("Xir-produce-klib-file")
        }
    })
}

context(level: KotlinCompilerArgumentsLevel)
private fun MutableMap<String, ArgumentTransform>.drop(name: String) {
    require(level.arguments.any { it.name == name }) { "Argument $name is not found in level $level" }
    put(name, ArgumentTransform.Drop)
}

context(level: KotlinCompilerArgumentsLevel)
private fun MutableMap<String, ArgumentTransform>.restrict(
    name: String,
    reason: String? = null,
    warningSince: KotlinReleaseVersion,
    errorSince: KotlinReleaseVersion? = null,
) {
    require(level.arguments.any { it.name == name }) { "Argument $name is not found in level $level" }
    put(name, ArgumentTransform.Restrict(reason, warningSince, errorSince))
}

context(level: KotlinCompilerArgumentsLevel)
private fun MutableMap<String, ArgumentTransform>.custom(argument: BtaCompilerArgument.CustomCompilerArgument) {
    put(argument.name, ArgumentTransform.CustomArgument(argument))
}

context(level: KotlinCompilerArgumentsLevel)
private fun MutableMap<String, ArgumentTransform>.override(name: String, argumentFactory: CustomCompilerArgumentFactory) {
    val argument = level.arguments.find { it.name == name } ?: error("Argument $name is not found in level $level")
    put(name, ArgumentTransform.Override(argumentFactory.create(argument)))
}

context(level: KotlinCompilerArgumentsLevel)
private fun MutableMap<String, ArgumentTransform>.fix(name: String, argumentTransform: (KotlinCompilerArgument) -> KotlinCompilerArgument) {
    val argument = level.arguments.find { it.name == name } ?: error("Argument $name is not found in level $level")
    put(name, ArgumentTransform.Fix(argumentTransform(argument)))
}

context(level: KotlinCompilerArgumentsLevel)
private fun KotlinCompilerArgument.transform(): ArgumentTransform =
    levelsToArgumentTransforms[level.name]?.get(name) ?: ArgumentTransform.NoOp

private fun KotlinCompilerArgumentsLevel.generateCustomArguments(): List<BtaCompilerArgument<*>> {
    val levelTransforms = levelsToArgumentTransforms[name] ?: emptyMap()
    return levelTransforms.values.filterIsInstance<ArgumentTransform.CustomArgument>().map { it.argument }
}

private fun KotlinCompilerArgumentsLevel.generateOverriddenArguments(): List<BtaCompilerArgument<*>> {
    val levelTransforms = levelsToArgumentTransforms[name] ?: emptyMap()
    return levelTransforms.values.filterIsInstance<ArgumentTransform.Override>().map { it.argument }
}

internal fun KotlinCompilerArgumentsLevel.transformApiArguments(): List<BtaCompilerArgument<*>> {
    val transformedArguments = arguments.mapNotNull { argument ->
        when (val op = argument.transform()) {
            is ArgumentTransform.NoOp -> SSoTCompilerArgument(argument)
            is ArgumentTransform.Drop, is ArgumentTransform.Restrict,
            is ArgumentTransform.CustomArgument, is ArgumentTransform.Override,
                -> null
            is ArgumentTransform.Fix -> SSoTCompilerArgument(op.argument)
        }
    }

    return transformedArguments + generateCustomArguments() + generateOverriddenArguments()
}

internal fun KotlinCompilerArgumentsLevel.transformImplArguments(): List<BtaCompilerArgument<*>> {
    val transformedArguments = arguments.mapNotNull { argument ->
        when (val op = argument.transform()) {
            is ArgumentTransform.NoOp, is ArgumentTransform.Drop, is ArgumentTransform.Restrict -> SSoTCompilerArgument(argument)
            is ArgumentTransform.CustomArgument, is ArgumentTransform.Override -> null
            is ArgumentTransform.Fix -> SSoTCompilerArgument(op.argument)
        }
    }

    return transformedArguments + generateCustomArguments() + generateOverriddenArguments()
}

internal data class RestrictedArgInfo(
    val fieldName: String,
    val primaryCli: String,
    val shortName: String?,
    val deprecatedName: String?,
    val reason: String?,
    val warningSince: KotlinReleaseVersion,
    val errorSince: KotlinReleaseVersion?,
)

/**
 * Collects metadata for restricted arguments defined directly at [level] (not including ancestors).
 */
internal fun collectRestrictedArgInfo(level: KotlinCompilerArgumentsLevel): List<RestrictedArgInfo> {
    val transforms = levelsToArgumentTransforms[level.name] ?: return emptyList()
    val result = mutableListOf<RestrictedArgInfo>()
    for ((argName, transform) in transforms) {
        if (transform !is ArgumentTransform.Restrict) continue
        val arg = level.arguments.find { it.name == argName } ?: continue
        result.add(
            RestrictedArgInfo(
                fieldName = arg.calculateName(),
                primaryCli = "-${arg.name}",
                shortName = arg.shortName?.let { "-$it" },
                deprecatedName = arg.deprecatedName?.let { "-$it" },
                reason = transform.reason,
                warningSince = transform.warningSince,
                errorSince = transform.errorSince,
            )
        )
    }
    return result
}

/**
 * Returns all levels on the path from the root down to [targetLevel] (inclusive),
 * or `null` if [targetLevel] is not reachable from this level.
 */
private fun KotlinCompilerArgumentsLevel.findAncestorsTo(targetLevel: KotlinCompilerArgumentsLevel): List<KotlinCompilerArgumentsLevel>? {
    if (name == targetLevel.name) return listOf(this)
    for (nested in nestedLevels) {
        val path = nested.findAncestorsTo(targetLevel)
        if (path != null) return listOf(this) + path
    }
    return null
}
