/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.plugins.JavaPluginConvention
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder.Companion.getSourceSetName
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder.Companion.kotlinProjectExtensionClass
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder.Companion.kotlinSourceSetClass
import java.io.File
import java.io.Serializable
import java.lang.Exception

interface KotlinTaskProperties : Serializable {
    val incremental: Boolean?
    val packagePrefix: String?
    val pureKotlinSourceFolders: List<File>?
}

data class KotlinTaskPropertiesImpl(
    override val incremental: Boolean?,
    override val packagePrefix: String?,
    override val pureKotlinSourceFolders: List<File>?

) : KotlinTaskProperties

typealias KotlinTaskPropertiesBySourceSet = MutableMap<String, KotlinTaskProperties>

private fun Task.getPackagePrefix(): String? {
    try {
        val getJavaPackagePrefix = this.javaClass.getMethod("getJavaPackagePrefix")
        @Suppress("UNCHECKED_CAST")
        return (getJavaPackagePrefix.invoke(this) as? String)
    } catch (e: Exception) {
    }
    return null
}

private fun Task.getIsIncremental(): Boolean? {
    try {
        val abstractKotlinCompileClass = javaClass.classLoader.loadClass(AbstractKotlinGradleModelBuilder.ABSTRACT_KOTLIN_COMPILE_CLASS)
        val getIncremental = abstractKotlinCompileClass.getDeclaredMethod("getIncremental")
        @Suppress("UNCHECKED_CAST")
        return (getIncremental.invoke(this) as? Boolean)
    } catch (e: Exception) {
    }
    return null
}

private fun Task.getPureKotlinSourceRoots(sourceSet: String): List<File>? {
    try {
        val kotlinExtensionClass = project.extensions.findByType(javaClass.classLoader.loadClass(kotlinProjectExtensionClass))
        val getKotlinMethod = javaClass.classLoader.loadClass(kotlinSourceSetClass).getMethod("getKotlin")
        val kotlinSourceSet = (kotlinExtensionClass?.javaClass?.getMethod("getSourceSets")?.invoke(kotlinExtensionClass)
                as? FactoryNamedDomainObjectContainer<Any>)?.asMap?.get(sourceSet) ?: return null
        val javaSourceSet =
            (project.convention.getPlugin(JavaPluginConvention::class.java) as JavaPluginConvention).sourceSets.asMap[sourceSet]
        val pureJava = javaSourceSet?.java?.srcDirs

        return (getKotlinMethod.invoke(kotlinSourceSet) as? SourceDirectorySet)?.srcDirs?.filter {
            !(pureJava?.contains(it) ?: false)
        }?.toList()
    } catch (e: Exception) {
    }
    return null
}

fun KotlinTaskPropertiesBySourceSet.acknowledgeTask(compileTask: Task) {
    this[compileTask.getSourceSetName()] =
        KotlinTaskPropertiesImpl(
            compileTask.getIsIncremental(),
            compileTask.getPackagePrefix(),
            compileTask.getPureKotlinSourceRoots(compileTask.getSourceSetName())
        )
}
