/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LazyParseableElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.deepCopy
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Test

class SourceStubsTest : AbstractAnalysisApiExecutionTest("testData/source/customData") {
    override val configurator: AnalysisApiTestConfigurator = LLSourceLikeTestConfigurator()

    @Test
    fun localFunction(file: KtFile, testServices: TestServices) {
        val fileStub = file.calcStubTree().root as KotlinFileStubImpl
        fun KotlinFileStubImpl.findLocalFunction(): KotlinFunctionStubImpl {
            val topLevelFunctionStub = findChildStubByElementType(KtNodeTypes.FUN) as KotlinFunctionStubImpl
            val localFunctionStub = topLevelFunctionStub.findChildStubByElementType(KtNodeTypes.FUN) as KotlinFunctionStubImpl
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

        val fileStubCopy = fileStub.deepCopy()
        val localFunctionStubCopy = fileStubCopy.findLocalFunction()
        validateLocalFunctionStub(localFunctionStubCopy)
    }

    @Test
    fun codeBlocksAreParsedOnlyIfNecessary(file: KtFile, testServices: TestServices) {
        // The test infrastructure has already parsed the original file completely, so a fresh file with collapsed code blocks is required
        val virtualFile = LightVirtualFile(file.name, KotlinFileType.INSTANCE, file.text)
        val freshFile = PsiManager.getInstance(file.project).findFile(virtualFile) as KtFile
        freshFile.calcStubTree()

        fun isParsed(node: ASTNode?): Boolean = (node as LazyParseableElement).isParsed

        val functions = freshFile.node.getChildren(null)
            .filter { it.elementType == KtNodeTypes.FUN }
            .associateBy { (it.psi as KtNamedFunction).name }

        fun bodyOf(name: String): ASTNode? = functions.getValue(name).findChildByType(KtNodeTypes.BLOCK)

        // Neither a stubbed declaration nor a contract can be found inside, so the body is skipped
        testServices.assertions.assertFalse(isParsed(bodyOf("withoutDeclarations")))

        // The local class has to be stubbed
        testServices.assertions.assertTrue(isParsed(bodyOf("withLocalClass")))

        // The first statement has to be checked for a contract
        testServices.assertions.assertTrue(isParsed(bodyOf("withContract")))

        val lambda = PsiTreeUtil.findChildOfType(freshFile, KtLambdaExpression::class.java, /* strict = */ true)
        testServices.assertions.assertFalse(isParsed(lambda?.node))
    }
}
