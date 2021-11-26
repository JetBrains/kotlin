/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.barebone.test

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

class TestKtModuleProvider(
    private val testServices: TestServices
) : TestService {
    private val cache = mutableMapOf<String, TestKtSourceModule>()

    fun registerModuleInfo(project: Project, testModule: TestModule, ktFiles: Map<TestFile, KtFile>) {
        cache[testModule.name] = TestKtSourceModule(project, testModule, ktFiles, testServices)
    }

    fun getModuleInfoByKtFile(ktFile: KtFile): TestKtSourceModule =
        cache.values.first { moduleSourceInfo ->
            (if (ktFile.isPhysical) ktFile else ktFile.originalFile) in moduleSourceInfo.ktFiles
        }

    fun getModule(moduleName: String): TestKtSourceModule =
        cache.getValue(moduleName)
}

val TestServices.projectModuleProvider: TestKtModuleProvider by TestServices.testServiceAccessor()
