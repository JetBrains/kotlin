/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.types

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.expressions.returnType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclarationWithReturnType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractTypeByDeclarationReturnTypeTest : AbstractTypeTest() {
    context(session: KaSession)
    override fun getType(ktFile: KtFile, module: KtTestModule, testServices: TestServices): KaType {
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclarationWithReturnType>(ktFile)
        return declaration.returnType
    }
}
