/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.ide

import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedPluginModel
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedPluginModelBuilderService
import org.jetbrains.kotlin.annotation.plugin.ide.DumpedPluginModel
import org.jetbrains.kotlin.annotation.plugin.ide.DumpedPluginModelImpl

interface NoArgModel : AnnotationBasedPluginModel {
    val invokeInitializers: Boolean

    override fun dump(): DumpedPluginModel {
        return DumpedPluginModelImpl(NoArgModelImpl::class.java, annotations.toList(), presets.toList(), invokeInitializers)
    }
}

class NoArgModelImpl(
    override val annotations: List<String>,
    override val presets: List<String>,
    override val invokeInitializers: Boolean
) : NoArgModel

class NoArgModelBuilderService : AnnotationBasedPluginModelBuilderService<NoArgModel>() {
    override val gradlePluginNames get() = listOf("org.jetbrains.kotlin.plugin.noarg", "kotlin-noarg")
    override val extensionName get() = "noArg"
    override val modelClass get() = NoArgModel::class.java

    override fun createModel(annotations: List<String>, presets: List<String>, extension: Any?): NoArgModel {
        val invokeInitializers = extension?.getFieldValue("invokeInitializers") as? Boolean ?: false
        return NoArgModelImpl(annotations, presets, invokeInitializers)
    }
}
