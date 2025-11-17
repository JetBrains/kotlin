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
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel

sealed interface ArgumentTransform {
    object NoOp : ArgumentTransform
    object Drop : ArgumentTransform
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
            drop("Xintellij-plugin-root")
            drop("Xcommon-sources")
            drop("Xenable-incremental-compilation")

            // KMP related
            drop("Xmulti-platform")
            drop("Xno-check-actual")
            drop("Xfragments")
            drop("Xfragment-sources")
            drop("Xfragment-refines")
            drop("Xfragment-dependency")
            drop("Xseparate-kmp-compilation")
            drop("Xdirect-java-actualization")
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
internal fun KotlinCompilerArgument.transform(): ArgumentTransform =
    levelsToArgumentTransforms[level.name]?.get(name) ?: ArgumentTransform.NoOp

internal fun KotlinCompilerArgumentsLevel.filterOutDroppedArguments(): List<KotlinCompilerArgument> =
    arguments.filter { it.transform() != ArgumentTransform.Drop }

internal fun KotlinCompilerArgumentsLevel.transformArguments(): List<BtaCompilerArgument> {
    return filterOutDroppedArguments().map { BtaCompilerArgument.SSoTCompilerArgument(it) }
}