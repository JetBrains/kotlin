/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractPackageScopeTest : AbstractScopeTestBase() {
    override fun KtAnalysisSession.getScope(mainFile: KtFile, testServices: TestServices): KtScope =
        getSingleTestTargetSymbolOfType<KtPackageSymbol>(mainFile, testDataPath).getPackageScope()

    override fun getAllowedContainingFiles(mainFile: KtFile, testServices: TestServices): Set<KtFile> {
        // Package scope tests may collect symbols from multiple files, so we need to allow all main test files.
        return testServices.ktTestModuleStructure.allMainKtFiles.toSet()
    }
}
