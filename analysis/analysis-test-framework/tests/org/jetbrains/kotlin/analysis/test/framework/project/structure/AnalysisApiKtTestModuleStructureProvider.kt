/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

abstract class AnalysisApiKtTestModuleStructureProvider : TestService {
    protected abstract val testServices: TestServices

    abstract fun registerModuleStructure(moduleStructure: KtTestModuleStructure)

    abstract fun getModuleStructure(): KtTestModuleStructure
}

class AnalysisApiKtTestModuleStructureProviderImpl(
    override val testServices: TestServices,
) : AnalysisApiKtTestModuleStructureProvider() {
    private lateinit var moduleStructure: KtTestModuleStructure

    override fun registerModuleStructure(moduleStructure: KtTestModuleStructure) {
        require(!this::moduleStructure.isInitialized)

        this.moduleStructure = moduleStructure
    }

    override fun getModuleStructure(): KtTestModuleStructure = moduleStructure
}

val TestServices.ktTestModuleStructureProvider: AnalysisApiKtTestModuleStructureProvider by TestServices.testServiceAccessor()

val TestServices.ktTestModuleStructure: KtTestModuleStructure
    get() = ktTestModuleStructureProvider.getModuleStructure()
