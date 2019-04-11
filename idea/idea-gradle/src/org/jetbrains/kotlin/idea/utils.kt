/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.impl.ModuleOrderEntryImpl
import  com.intellij.openapi.diagnostic.Logger

/**
 * Returns the dependency scope which has flags specific to the both provided scopes
 */
fun getScopeContainingBoth(scope1: DependencyScope, scope2: DependencyScope?): DependencyScope {
    if (scope2 == null) {
        return scope1
    }

    val compile = scope1.isForProductionCompile || scope2.isForProductionCompile
    val runtime = scope1.isForProductionRuntime || scope2.isForProductionRuntime
    val testCompile = scope1.isForTestCompile || scope2.isForTestCompile
    val testRuntime = scope1.isForTestRuntime || scope2.isForTestRuntime

    //we suppose that scope1 or scope 2 is at least Test
    val result = if (compile || runtime) DependencyScope.COMPILE else DependencyScope.TEST
    // validate that result is valid. We could not express several cases (e.g. RUNTIME + TEST), we will log this case
    if (result.isForProductionCompile != compile ||
        result.isForProductionRuntime != runtime ||
        result.isForTestCompile != testCompile ||
        result.isForTestRuntime != testRuntime
    ) {
        Logger.getInstance(KotlinJavaMPPSourceSetDataService::class.java)
            .warn("Could not express cross-module dependency with flags Compile=$compile Runtime=$runtime TestCompile=$testCompile TestRuntime=$testRuntime")
    }
    return result
}

fun addModuleDependencyIfNeeded(
    rootModel: ModifiableRootModel,
    dependeeModule: Module,
    testScope: Boolean,
    dependOnTest: Boolean
) {
    val existingEntry = rootModel.findModuleOrderEntry(dependeeModule)
    val existingDependOnTest = (existingEntry as? ModuleOrderEntryImpl)?.isProductionOnTestDependency ?: false

    val requiredScope = getScopeContainingBoth(if (testScope) DependencyScope.TEST else DependencyScope.COMPILE, existingEntry?.scope)
    if (existingEntry != null) {
        val existingScope = existingEntry.scope


        if (requiredScope != existingScope || (dependOnTest && !existingDependOnTest)) {
            rootModel.removeOrderEntry(existingEntry)
        } else {
            return
        }
    }
    rootModel.addModuleOrderEntry(dependeeModule).also {
        it.scope = requiredScope
        (it as? ModuleOrderEntryImpl)?.isProductionOnTestDependency = dependOnTest || existingDependOnTest
    }
}