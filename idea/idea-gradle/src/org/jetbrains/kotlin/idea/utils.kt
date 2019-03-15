/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel

fun addModuleDependencyIfNeeded(
    rootModel: ModifiableRootModel,
    dependeeModule: Module,
    testScope: Boolean
) {
    val dependencyScope = if (testScope) DependencyScope.TEST else DependencyScope.COMPILE
    val existingEntry = rootModel.findModuleOrderEntry(dependeeModule)
    if (existingEntry != null) {
        val existingScope = existingEntry.scope
        if (existingScope == DependencyScope.COMPILE || existingScope == dependencyScope) return
        if (dependencyScope == DependencyScope.COMPILE) {
            rootModel.removeOrderEntry(existingEntry)
        }
    }
    rootModel.addModuleOrderEntry(dependeeModule).also { it.scope = dependencyScope }
}