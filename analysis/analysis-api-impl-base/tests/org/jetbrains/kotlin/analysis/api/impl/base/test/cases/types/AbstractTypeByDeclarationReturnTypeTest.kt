/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractTypeByDeclarationReturnTypeTest : AbstractTypeTest() {
    context(KtAnalysisSession)
    override fun getType(ktFile: KtFile, module: TestModule, testServices: TestServices): KtType {
        val declarationAtCaret = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)
        return declarationAtCaret.getReturnKtType()
    }
}