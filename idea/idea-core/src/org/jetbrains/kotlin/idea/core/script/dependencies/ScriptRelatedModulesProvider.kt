/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

abstract class ScriptRelatedModulesProvider {
    abstract fun getRelatedModules(file: VirtualFile, project: Project): List<Module>

    companion object {
        private val EP_NAME: ExtensionPointName<ScriptRelatedModulesProvider> =
            ExtensionPointName.create<ScriptRelatedModulesProvider>("org.jetbrains.kotlin.scriptRelatedModulesProvider")

        fun getRelatedModules(file: VirtualFile, project: Project): List<Module> {
            return Extensions.getArea(project).getExtensionPoint(EP_NAME).extensions
                .filterIsInstance<ScriptRelatedModulesProvider>()
                .flatMap { it.getRelatedModules(file, project) }
        }
    }
}