/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

@Suppress("unused")
fun FrameworkSupportInModuleProvider.isInNewProject(module: Module): Boolean {
    return module.project.allModules().isEmpty()
}