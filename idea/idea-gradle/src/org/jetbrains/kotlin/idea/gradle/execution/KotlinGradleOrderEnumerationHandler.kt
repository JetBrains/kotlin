/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.gradle.execution

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.caches.project.isMPPModule
import org.jetbrains.plugins.gradle.execution.GradleOrderEnumeratorHandler

open class KotlinGradleOrderEnumerationHandler(module: Module) : GradleOrderEnumeratorHandler(module) {
    override fun shouldIncludeTestsFromDependentModulesToTestClasspath(): Boolean {
        return true
    }

    open class Factory : GradleOrderEnumeratorHandler.FactoryImpl() {
        override fun isApplicable(module: Module): Boolean {
            return module.isMPPModule
        }

        override fun createHandler(module: Module) = KotlinGradleOrderEnumerationHandler(module)
    }
}
