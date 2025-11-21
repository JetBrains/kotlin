/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrderRelation
import kotlin.io.path.absolutePathString

public fun List<CompilerPlugin>.apply(arguments: org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments) {
    arguments.pluginClasspaths = flatMap { it.classpath }.map { it.absolutePathString() }.toTypedArray()
    arguments.pluginOptions = flatMap { plugin -> plugin.rawArguments.map { option -> "plugin:${plugin.pluginId}:${option.key}=${option.value}" } }.toTypedArray()
    arguments.pluginOrderConstraints = flatMap { plugin ->
        plugin.orderingRequirements.map { order ->
            when (order.relation) {
                CompilerPluginPartialOrderRelation.BEFORE -> "${plugin.pluginId}<${order.otherPluginId}"; CompilerPluginPartialOrderRelation.AFTER -> "${order.otherPluginId}>${plugin.pluginId}"
            }
        }
    }.toTypedArray()
}