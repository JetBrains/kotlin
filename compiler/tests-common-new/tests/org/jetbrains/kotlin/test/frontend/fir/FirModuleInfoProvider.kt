/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirJvmModuleInfo
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.dependencyProvider
import org.jetbrains.kotlin.test.model.TestModule

class FirModuleInfoProvider(
    private val testServices: TestServices
) : TestService {
    val firSessionProvider = FirProjectSessionProvider()

    private val builtinsByModule: MutableMap<TestModule, FirJvmModuleInfo> = mutableMapOf()
    private val firModuleInfoByModule: MutableMap<TestModule, FirJvmModuleInfo> = mutableMapOf()

    fun convertToFirModuleInfo(module: TestModule): FirJvmModuleInfo {
        return firModuleInfoByModule.getOrPut(module) {
            val dependencies = mutableListOf(builtinsModuleInfoForModule(module))
            module.dependencies.mapTo(dependencies) {
                convertToFirModuleInfo(testServices.dependencyProvider.getTestModule(it.moduleName))
            }
            FirJvmModuleInfo(
                module.name,
                dependencies
            )
        }
    }

    fun builtinsModuleInfoForModule(module: TestModule): FirJvmModuleInfo {
        return builtinsByModule.getOrPut(module) {
            FirJvmModuleInfo(Name.special("<built-ins>"), emptyList())
        }
    }
}

val TestServices.firModuleInfoProvider: FirModuleInfoProvider by TestServices.testServiceAccessor()
