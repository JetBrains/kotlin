/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions

open class ApplicationExtensionDescriptor<T>(name: String, private val extensionClass: Class<T>) {
    val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    fun registerExtensionPoint() {
        Extensions.getRootArea().registerExtensionPoint(
            extensionPointName.name,
            extensionClass.name,
            ExtensionPoint.Kind.INTERFACE
        )
    }

    fun registerExtension(extension: T) {
        Extensions.getRootArea().getExtensionPoint(extensionPointName).registerExtension(extension)
    }

    fun getInstances(): List<T> {
        val projectArea = Extensions.getRootArea()
        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return listOf()

        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
    }
}