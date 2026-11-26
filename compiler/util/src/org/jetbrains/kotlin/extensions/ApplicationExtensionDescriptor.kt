/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * This class is used only in `:jps:jps-common` (jps/jps-common/src/org/jetbrains/kotlin/platform/IdePlatformKind.kt).
 * It cannot be declared there, as that module doesn't directly depend on the intellij-platform.
 */
open class ApplicationExtensionDescriptor<T : Any>(name: String) {
    private val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    fun getInstances(): List<T> {
        val projectArea = ApplicationManager.getApplication().extensionArea
        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return listOf()

        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
    }
}
