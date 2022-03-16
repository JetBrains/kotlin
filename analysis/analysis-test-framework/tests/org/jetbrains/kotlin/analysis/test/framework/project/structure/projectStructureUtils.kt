/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

fun getKtFilesFromModule(testServices: TestServices, testModule: TestModule): List<KtFile> {
    val moduleInfoProvider = testServices.projectModuleProvider
    return when (val moduleInfo = moduleInfoProvider.getModule(testModule.name)) {
        is TestKtSourceModule -> moduleInfo.testFilesToKtFiles.filterKeys { testFile -> !testFile.isAdditional }.values.toList()
        is TestKtLibraryModule -> moduleInfo.ktFiles.toList()
        is TestKtLibrarySourceModule -> moduleInfo.ktFiles.toList()
        else -> error("Unexpected $moduleInfo")
    }
}
