/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

abstract class AnalysisApiKtTestModuleProjectStructureProvider : TestService {
    protected abstract val testServices: TestServices

    abstract fun registerModuleStructure(moduleStructure: KtTestModuleProjectStructure)

    abstract fun getModuleStructure(): KtTestModuleProjectStructure
}

class AnalysisApiKtTestModuleProjectStructureProviderImpl(
    override val testServices: TestServices,
) : AnalysisApiKtTestModuleProjectStructureProvider() {
    private lateinit var moduleStructure: KtTestModuleProjectStructure

    override fun registerModuleStructure(moduleStructure: KtTestModuleProjectStructure) {
        require(!this::moduleStructure.isInitialized)

        this.moduleStructure = moduleStructure
    }

    override fun getModuleStructure(): KtTestModuleProjectStructure = moduleStructure
}

val TestServices.ktTestModuleProjectStructureProvider: AnalysisApiKtTestModuleProjectStructureProvider by TestServices.testServiceAccessor()

val TestServices.ktTestModuleProjectStructure: KtTestModuleProjectStructure
    get() = ktTestModuleProjectStructureProvider.getModuleStructure()
