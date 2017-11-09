/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project

open class ProjectExtensionDescriptor<T>(name: String, private val extensionClass: Class<T>) {
    val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    fun registerExtensionPoint(project: Project) {
        Extensions.getArea(project).registerExtensionPoint(
                extensionPointName.name,
                extensionClass.name,
                ExtensionPoint.Kind.INTERFACE
        )
    }

    fun registerExtension(project: Project, extension: T) {
        Extensions.getArea(project).getExtensionPoint(extensionPointName).registerExtension(extension)
    }

    fun getInstances(project: Project): List<T> {
        val projectArea = Extensions.getArea(project)
        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return listOf()

        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
    }
}