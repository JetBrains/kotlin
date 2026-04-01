/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.options.generator

import org.jetbrains.kotlin.arguments.description.actualCommonCompilerArguments
import org.jetbrains.kotlin.arguments.description.actualCommonToolsArguments
import org.jetbrains.kotlin.arguments.description.actualJvmCompilerArguments
import org.jetbrains.kotlin.arguments.description.actualMetadataArguments
import org.jetbrains.kotlin.arguments.description.removed.removedCommonCompilerArguments
import org.jetbrains.kotlin.arguments.description.removed.removedJvmCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.buildtools.options.generator.BtaCompilerArgument.CustomCompilerArgument
import org.jetbrains.kotlin.buildtools.options.generator.BtaCompilerArgument.SSoTCompilerArgument

sealed interface ArgumentTransform {
    object NoOp : ArgumentTransform
    object Drop : ArgumentTransform
    class CustomArgument(val argument: CustomCompilerArgument) : ArgumentTransform
    class Override(val argument: CustomCompilerArgument) : ArgumentTransform
//    data class Rename(val to: String) : ArgumentTransform // possible future operations
}

private val levelsToArgumentTransforms: Map<String, Map<String, ArgumentTransform>> = buildMap {
    put(actualCommonCompilerArguments.name, buildMap {
        with(actualCommonCompilerArguments) {
            drop("script")
            drop("Xrepl")
            drop("Xstdlib-compilation")
            drop("Xallow-kotlin-package")
            drop("P")
            drop("Xplugin")
            drop("Xcompiler-plugin")
            drop("Xcompiler-plugin-order")
            drop("Xintellij-plugin-root")
            drop("Xcommon-sources")
            drop("Xenable-incremental-compilation")
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
            drop("d") // destination is configured explicitly when instantiating operations
            drop("Xlegacy-metadata-jar-k2")
        }
    })
    put(actualJvmCompilerArguments.name, buildMap {
        with(actualJvmCompilerArguments) {
            drop("d") // destination is configured explicitly when instantiating operations
            drop("expression")
            drop("include-runtime") // we're only considering building into directories for now (not jars)
            drop("Xbuild-file")
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
}

context(level: KotlinCompilerArgumentsLevel)
private fun MutableMap<String, ArgumentTransform>.drop(name: String) {
    require(level.arguments.any { it.name == name }) { "Argument $name is not found in level $level" }
    put(name, ArgumentTransform.Drop)
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
private fun KotlinCompilerArgument.transform(): ArgumentTransform =
    levelsToArgumentTransforms[level.name]?.get(name) ?: ArgumentTransform.NoOp

private fun KotlinCompilerArgumentsLevel.generateCustomArguments(): List<BtaCompilerArgument<*>> {
    val levelTransforms = levelsToArgumentTransforms[name] ?: error("Level $this is not found in levelsToArgumentTransforms")
    return levelTransforms.values.filterIsInstance<ArgumentTransform.CustomArgument>().map { it.argument }
}

private fun KotlinCompilerArgumentsLevel.generateOverriddenArguments(): List<BtaCompilerArgument<*>> {
    val levelTransforms = levelsToArgumentTransforms[name] ?: error("Level $this is not found in levelsToArgumentTransforms")
    return levelTransforms.values.filterIsInstance<ArgumentTransform.Override>().map { it.argument }
}

internal fun KotlinCompilerArgumentsLevel.transformApiArguments(): List<BtaCompilerArgument<*>> {
    val transformedArguments = arguments.mapNotNull { argument ->
        when (argument.transform()) {
            is ArgumentTransform.NoOp -> SSoTCompilerArgument(argument)
            is ArgumentTransform.Drop, is ArgumentTransform.CustomArgument, is ArgumentTransform.Override -> null
        }
    }

    return transformedArguments + generateCustomArguments() + generateOverriddenArguments()
}

internal fun KotlinCompilerArgumentsLevel.transformImplArguments(): List<BtaCompilerArgument<*>> {
    val transformedArguments = arguments.mapNotNull { argument ->
        when (argument.transform()) {
            is ArgumentTransform.NoOp, is ArgumentTransform.Drop -> SSoTCompilerArgument(argument)
            is ArgumentTransform.CustomArgument, is ArgumentTransform.Override -> null
        }
    }

    return transformedArguments + generateCustomArguments() + generateOverriddenArguments()
}