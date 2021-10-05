/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.ModuleRegistrarPreAnalysisHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalKtFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

object FirLowLevelFrontendApiTestConfiguratorService : FrontendApiTestConfiguratorService {
    override fun TestConfigurationBuilder.configureTest(disposable: Disposable) {
        usePreAnalysisHandlers(::ModuleRegistrarPreAnalysisHandler.bind(disposable))
    }

    override fun processTestFiles(files: List<KtFile>): List<KtFile> {
        return files.map {
            val fakeFile = it.copy() as KtFile
            fakeFile.originalKtFile = it
            fakeFile
        }
    }

    override fun getOriginalFile(file: KtFile): KtFile {
        return file.originalKtFile!!
    }

    override fun registerProjectServices(project: MockProject) {}
    override fun registerApplicationServices(application: MockApplication) {}
}