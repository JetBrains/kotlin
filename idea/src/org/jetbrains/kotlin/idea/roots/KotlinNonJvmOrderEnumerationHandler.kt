/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.OrderEnumerationHandler
import com.intellij.openapi.roots.OrderRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.TestResourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType

object KotlinNonJvmOrderEnumerationHandler : OrderEnumerationHandler() {
    class Factory : OrderEnumerationHandler.Factory() {
        override fun isApplicable(module: Module) = true
        override fun createHandler(module: Module) = KotlinNonJvmOrderEnumerationHandler
    }

    private val kotlinTestSourceRootTypes: Set<JpsModuleSourceRootType<*>> =
        setOf(TestSourceKotlinRootType, TestResourceKotlinRootType)

    override fun addCustomModuleRoots(
        type: OrderRootType,
        rootModel: ModuleRootModel,
        result: MutableCollection<String>,
        includeProduction: Boolean,
        includeTests: Boolean
    ): Boolean {
        if (type == OrderRootType.SOURCES) {
            if (includeProduction) {
                rootModel.getSourceRoots(includeTests).mapTo(result) { it.url }
            } else {
                rootModel.getSourceRoots(kotlinTestSourceRootTypes).mapTo(result) { it.url }
            }
            return true
        }
        return false
    }
}