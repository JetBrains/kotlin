/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.dependencyProvider

class FirModuleInfoProvider(private val testServices: TestServices) : TestService {
    val firSessionProvider = FirProjectSessionProvider()

    private val firModuleDataByModule: MutableMap<TestModule, FirModuleData> = mutableMapOf()

    fun registerModuleData(module: TestModule, moduleData: FirModuleData) {
        if (module in firModuleDataByModule) error("module data for module $module already registered")
        firModuleDataByModule[module] = moduleData
    }

    fun getCorrespondingModuleData(module: TestModule): FirModuleData {
        return firModuleDataByModule[module] ?: error("module data for module $module is not registered")
    }

    fun getDependentSourceModules(module: TestModule): List<FirModuleData> {
        return getDependentModulesImpl(module.dependencies)
    }

    fun getDependentFriendSourceModules(module: TestModule): List<FirModuleData> {
        return getDependentModulesImpl(module.friends)
    }

    private fun getDependentModulesImpl(dependencies: List<DependencyDescription>): List<FirModuleData> {
        return dependencies.filter { it.kind == DependencyKind.Source }.map {
            getCorrespondingModuleData(testServices.dependencyProvider.getTestModule(it.moduleName))
        }
    }
}

val TestServices.firModuleInfoProvider: FirModuleInfoProvider by TestServices.testServiceAccessor()
