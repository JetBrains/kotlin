/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractTypeByDeclarationReturnTypeTest : AbstractTypeTest() {
    override fun getType(analysisSession: KaSession, ktFile: KtFile, module: KtTestModule, testServices: TestServices): KaType {
        with(analysisSession) {
            val declarationAtCaret = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(ktFile)
            return declarationAtCaret.returnType
        }
    }
}