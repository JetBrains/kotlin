/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.trackers

import com.intellij.openapi.module.Module
import com.intellij.openapi.util.ModificationTracker

class KotlinFE10OutOfBlockModificationTracker : KotlinOutOfBlockModificationTrackerFactory() {
    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker =
        throwUnsupportedError()

    override fun createModuleWithoutDependenciesOutOfBlockModificationTracker(module: Module): ModificationTracker =
        throwUnsupportedError()

    private fun throwUnsupportedError(): Nothing =
        error("Supported only for FIR plugin now, sorry, consider using KotlinModificationTrackerService instead")
}