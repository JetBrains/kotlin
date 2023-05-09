/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

open class ProjectExtensionDescriptor<T : Any>(name: String, private val extensionClass: Class<T>) {
    val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    fun registerExtensionPoint(project: Project) {
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            extensionPointName.name,
            extensionClass
        )
    }

    fun registerExtension(project: Project, extension: T) {
        project.extensionArea.getExtensionPoint(extensionPointName).registerExtension(extension, project)
    }

    fun getInstances(project: Project): List<T> {
        val projectArea = project.extensionArea
        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return listOf()

        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
    }
}
