/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.deepCopy
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Test

class SourceStubsTest : AbstractAnalysisApiExecutionTest("testData/source/customData") {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun localFunction(file: KtFile, testServices: TestServices) {
        val fileStub = file.calcStubTree().root as KotlinFileStubImpl
        fun KotlinFileStubImpl.findLocalFunction(): KotlinFunctionStubImpl {
            val topLevelFunctionStub = findChildStubByType(KtStubElementTypes.FUNCTION) as KotlinFunctionStubImpl
            val localFunctionStub = topLevelFunctionStub.findChildStubByType(KtStubElementTypes.FUNCTION) as KotlinFunctionStubImpl
            return localFunctionStub
        }

        fun validateLocalFunctionStub(stub: KotlinFunctionStubImpl) {
            testServices.assertions.assertEquals("localFunction", stub.name)
            testServices.assertions.assertTrue(stub.isDangling)
        }

        val localFunctionStub = fileStub.findLocalFunction()
        validateLocalFunctionStub(localFunctionStub)

        // The assertion to ensure that the parent is computed correctly even for dangling stubs
        testServices.assertions.assertEquals(KtBlockExpression::class.java, localFunctionStub.psi.parent::class.java)

        @OptIn(KtImplementationDetail::class)
        val fileStubCopy = fileStub.deepCopy()
        val localFunctionStubCopy = fileStubCopy.findLocalFunction()
        validateLocalFunctionStub(localFunctionStubCopy)
    }
}
