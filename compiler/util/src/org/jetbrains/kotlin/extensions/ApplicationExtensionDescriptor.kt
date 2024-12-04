/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName

open class ApplicationExtensionDescriptor<T : Any>(name: String, private val extensionClass: Class<T>) {
    private val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    fun registerExtensionPoint() {
        CoreApplicationEnvironment.registerExtensionPoint(
            ApplicationManager.getApplication().extensionArea,
            extensionPointName.name,
            extensionClass
        )
    }

    fun registerExtension(extension: T, disposable: Disposable) {
        ApplicationManager.getApplication().extensionArea.getExtensionPoint(extensionPointName).registerExtension(extension, disposable)
    }

    fun getInstances(): List<T> {
        val projectArea = ApplicationManager.getApplication().extensionArea
        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return listOf()

        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
    }
}
