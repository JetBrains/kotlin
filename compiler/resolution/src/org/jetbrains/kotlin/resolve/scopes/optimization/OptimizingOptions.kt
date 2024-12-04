/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes.optimization

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

@DefaultImplementation(OptimizingOptions.Default::class)
interface OptimizingOptions {
    fun shouldCalculateAllNamesForLazyImportScopeOptimizing(moduleDescriptor: ModuleDescriptor?): Boolean

    object Default : OptimizingOptions {
        override fun shouldCalculateAllNamesForLazyImportScopeOptimizing(moduleDescriptor: ModuleDescriptor?): Boolean {
            return true
        }
    }
}
