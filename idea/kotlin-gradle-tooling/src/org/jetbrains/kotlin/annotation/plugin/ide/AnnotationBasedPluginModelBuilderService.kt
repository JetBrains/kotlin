/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.annotation.plugin.ide

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import java.io.Serializable
import java.lang.Exception

interface DumpedPluginModel {
    val className: String

    // May contain primitives, Strings and collections of primitives/Strings
    val args: Array<*>

    operator fun component1() = className
    operator fun component2() = args
}

class DumpedPluginModelImpl(
    override val className: String,
    override val args: Array<*>
) : DumpedPluginModel, Serializable {
    constructor(clazz: Class<*>, vararg args: Any?) : this(clazz.canonicalName, args)
}

interface AnnotationBasedPluginModel : Serializable {
    val annotations: List<String>
    val presets: List<String>

    /*
        Objects returned from Gradle importer are implicitly wrapped in a proxy that can potentially leak internal Gradle structures.
        So we need a way to safely serialize the arbitrary annotation plugin model.
     */
    fun dump(): DumpedPluginModel

    val isEnabled get() = annotations.isNotEmpty() || presets.isNotEmpty()
}

abstract class AnnotationBasedPluginModelBuilderService<T : AnnotationBasedPluginModel> : AbstractKotlinGradleModelBuilder() {
    abstract val gradlePluginNames: List<String>
    abstract val extensionName: String

    abstract val modelClass: Class<T>
    abstract fun createModel(annotations: List<String>, presets: List<String>, extension: Any?): T

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
            .withDescription("Unable to build $gradlePluginNames plugin configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == modelClass.name

    override fun buildAll(modelName: String?, project: Project): Any {
        val plugin: Plugin<*>? =  project.findPlugin(gradlePluginNames)
        val extension: Any? = project.extensions.findByName(extensionName)

        val annotations = mutableListOf<String>()
        val presets = mutableListOf<String>()

        if (plugin != null && extension != null) {
            annotations += extension.getList("myAnnotations")
            presets += extension.getList("myPresets")
            return createModel(annotations, presets, extension)
        }

        return createModel(emptyList(), emptyList(), null)
    }

    private fun Project.findPlugin(names: List<String>): Plugin<*>? {
        for (name in names) {
            plugins.findPlugin(name)?.let { return it }
        }

        return null
    }

    private fun Any.getList(fieldName: String): List<String> {
        @Suppress("UNCHECKED_CAST")
        return getFieldValue(fieldName) as? List<String> ?: emptyList()
    }

    protected fun Any.getFieldValue(fieldName: String, clazz: Class<*> = this.javaClass): Any? {
        val field = clazz.declaredFields.firstOrNull { it.name == fieldName }
            ?: return getFieldValue(fieldName, clazz.superclass ?: return null)

        val oldIsAccessible = field.isAccessible
        try {
            field.isAccessible = true
            return field.get(this)
        } finally {
            field.isAccessible = oldIsAccessible
        }
    }
}