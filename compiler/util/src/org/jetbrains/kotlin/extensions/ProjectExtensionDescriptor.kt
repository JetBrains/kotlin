/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * Class representing an extension point. Used as a key in structures which store
 * specific instances of extensions.
 */
abstract class ExtensionPointDescriptor<T : Any>(val name: String, val extensionClass: Class<T>)

open class ProjectExtensionDescriptor<T : Any>(name: String, extensionClass: Class<T>) : ExtensionPointDescriptor<T>(name, extensionClass) {
    val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    fun registerExtensionPoint(project: Project) {
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            extensionPointName.name,
            extensionClass
        )
    }

    fun registerExtension(project: Project, extension: T) {
        val extensionPoint = project.extensionArea.getExtensionPoint(extensionPointName)
        if (extension !in extensionPoint.extensions) {
            extensionPoint.registerExtension(extension, project)
        }
    }

    fun getInstances(project: Project): List<T> {
        val projectArea = project.extensionArea
        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return listOf()

        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
    }
}
