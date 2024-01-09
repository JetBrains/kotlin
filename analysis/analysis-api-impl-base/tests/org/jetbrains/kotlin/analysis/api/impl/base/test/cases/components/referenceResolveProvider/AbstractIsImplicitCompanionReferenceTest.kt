/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.referenceResolveProvider

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsImplicitCompanionReferenceTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val referenceExpression = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtNameReferenceExpression>(mainFile)

        val isImplicitCompanionReference = executeOnPooledThreadInReadAction {
            analyseForTest(referenceExpression) {
                val reference = referenceExpression.reference as KtReference
                reference.isImplicitReferenceToCompanion()
            }
        }

        val actual = buildString {
            append("isImplicitCompanionReference: $isImplicitCompanionReference")
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
