/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.test

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.TestKtSourceModule
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.projectModuleProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPackageProviderFactory
import org.jetbrains.kotlin.test.services.*

class KtFe10ModuleRegistrarPreAnalysisHandler(
    testServices: TestServices,
    @Suppress("UNUSED_PARAMETER") parentDisposable: Disposable
) : PreAnalysisHandler(testServices) {

    private val moduleInfoProvider = testServices.projectModuleProvider

    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        // todo rework after all modules will have the same Project instance
        val ktFilesByModule = moduleStructure.modules.associateWith { testModule ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            testServices.sourceFileProvider.getKtFilesForSourceFiles(testModule.files, project)
        }

        val allKtFiles = ktFilesByModule.values.flatMap { it.values.toList() }

        ktFilesByModule.forEach { (testModule, ktFiles) ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            moduleInfoProvider.registerModuleInfo(testModule, TestKtSourceModule(project, testModule, ktFiles, testServices))

            with(project as MockProject) {
                registerService(KotlinModificationTrackerFactory::class.java, KotlinStaticModificationTrackerFactory::class.java)
                registerService(
                    KotlinDeclarationProviderFactory::class.java,
                    KotlinStaticDeclarationProviderFactory(project, allKtFiles)
                )
                registerService(KotlinPackageProviderFactory::class.java, KotlinStaticPackageProviderFactory(allKtFiles))
            }
        }
    }
}
