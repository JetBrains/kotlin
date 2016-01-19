/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ResolveElementCacheTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE

    private val FILE_TEXT =
"""
class C(param1: String = "", param2: Int = 0) {
    fun a(p: Int = 0) {
        b(1, 2)
        val x = c()
        d(x)
    }

    fun b() {
        x(1)
    }

    fun c() {
    }
}
"""

    private data class Data(
            val file: KtFile,
            val klass: KtClass,
            val members: List<KtDeclaration>,
            val statements: List<KtExpression>,
            val factory: KtPsiFactory
    )

    private fun doTest(handler: Data.() -> Unit) {
        val file = myFixture.configureByText("Test.kt", FILE_TEXT) as KtFile
        val data = extractData(file)
        myFixture.project.executeWriteCommand("") { data.handler() }
    }

    private fun extractData(file: KtFile): Data {
        val klass = file.declarations.single() as KtClass
        val members = klass.declarations
        val function = members.first() as KtNamedFunction
        val statements = (function.bodyExpression as KtBlockExpression).statements
        return Data(file, klass, members, statements, KtPsiFactory(project))
    }

    fun testFullResolveCaching() {
        doTest { this.testResolveCaching() }
    }

    private fun Data.testResolveCaching() {
        // resolve statements in "a()"
        val statement1 = statements[0]
        val statement2 = statements[1]
        val aFunBodyContext1 = statement1.analyze(BodyResolveMode.FULL)
        val aFunBodyContext2 = statement2.analyze(BodyResolveMode.FULL)
        assert(aFunBodyContext1 === aFunBodyContext2)

        val aFunBodyContext3 = statement1.analyze(BodyResolveMode.FULL)
        assert(aFunBodyContext3 === aFunBodyContext1)

        val bFun = members[1] as KtNamedFunction
        val bBody = bFun.bodyExpression as KtBlockExpression
        val bStatement = bBody.statements[0]
        val bFunBodyContext = bStatement.analyze(BodyResolveMode.FULL)

        // modify body of "b()"
        bBody.addAfter(factory.createExpression("x()"), bBody.lBrace)
        val bFunBodyContextAfterChange1 = bStatement.analyze(BodyResolveMode.FULL)
        assert(bFunBodyContext !== bFunBodyContextAfterChange1)

        if (file.isPhysical) { // for non-physical files we reset caches for whole file
            val aFunBodyContextAfterChange1 = statement1.analyze(BodyResolveMode.FULL)
            assert(aFunBodyContextAfterChange1 === aFunBodyContext1) // change in other function's body should not affect resolve of other one
        }

        // add parameter to "b()" this should invalidate all resolve
        bFun.valueParameterList!!.addParameter(factory.createParameter("p: Int"))

        val aFunBodyContextAfterChange2 = statement1.analyze(BodyResolveMode.FULL)
        assert(aFunBodyContextAfterChange2 !== aFunBodyContext1)

        val bFunBodyContextAfterChange2 = bStatement.analyze(BodyResolveMode.FULL)
        assert(bFunBodyContextAfterChange2 !== bFunBodyContextAfterChange1)
    }

    fun testNonPhysicalFileFullResolveCaching() {
        doTest {
            val nonPhysicalFile = KtPsiFactory(project).createAnalyzableFile("NonPhysical.kt", FILE_TEXT, file)
            val nonPhysicalData = extractData(nonPhysicalFile)
            nonPhysicalData.testResolveCaching()

            // now check how changes in the physical file affect non-physical one
            val statement = nonPhysicalData.statements[0]
            val nonPhysicalContext1 = statement.analyze(BodyResolveMode.FULL)

            statements[0].delete()

            val nonPhysicalContext2 = statement.analyze(BodyResolveMode.FULL)
            assert(nonPhysicalContext2 === nonPhysicalContext1) // change inside function's body should not affect other files

            members[2].delete()

            val nonPhysicalContext3 = statement.analyze(BodyResolveMode.FULL)
            assert(nonPhysicalContext3 !== nonPhysicalContext1)

            // and now check that non-physical changes do not affect physical world
            val physicalContext1 = statements[1].analyze(BodyResolveMode.FULL)
            nonPhysicalData.members[0].delete()
            val physicalContext2 = statements[1].analyze(BodyResolveMode.FULL)
            assert(physicalContext1 === physicalContext2)
        }
    }

    fun testResolveSurvivesTypingInCodeBlock() {
        doTest {
            val statement = statements[0]
            val bindingContext1 = statement.analyze(BodyResolveMode.FULL)

            val classConstructorParamTypeRef = klass.getPrimaryConstructor()!!.valueParameters.first().typeReference!!
            val bindingContext2 = classConstructorParamTypeRef.analyze(BodyResolveMode.FULL)

            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file)!!
            documentManager.doPostponedOperationsAndUnblockDocument(document)

            // modify body of "b()" via document
            val bFun = members[1] as KtNamedFunction
            val bBody = bFun.bodyExpression as KtBlockExpression
            document.insertString(bBody.lBrace!!.startOffset + 1, "x()")
            documentManager.commitAllDocuments()

            val bindingContext3 = statement.analyze(BodyResolveMode.FULL)
            assert(bindingContext3 === bindingContext1)

            val bindingContext4 = classConstructorParamTypeRef.analyze(BodyResolveMode.FULL)
            assert(bindingContext4 === bindingContext2)

            // insert statement in "a()"
            document.insertString(statement.startOffset, "x()\n")
            documentManager.commitAllDocuments()

            val bindingContext5 = (members[0] as KtNamedFunction).bodyExpression!!.analyze(BodyResolveMode.FULL)
            assert(bindingContext5 !== bindingContext1)

            val bindingContext6 = classConstructorParamTypeRef.analyze(BodyResolveMode.FULL)
            assert(bindingContext6 === bindingContext2)
        }
    }

    fun testPartialResolveUsesFullResolveCached() {
        doTest {
            val statement1 = statements[0]
            val statement2 = statements[1]
            val bindingContext1 = statement1.analyze(BodyResolveMode.FULL)

            val bindingContext2 = statement2.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext2 === bindingContext1)

            val bindingContext3 = statement2.analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION)
            assert(bindingContext3 === bindingContext1)
        }
    }

    fun testPartialResolveCaching() {
        doTest { this.testPartialResolveCaching() }
    }

    private fun Data.testPartialResolveCaching() {
        val statement1 = statements[0]
        val statement2 = statements[1]
        val bindingContext1 = statement1.analyze(BodyResolveMode.PARTIAL)
        val bindingContext2 = statement2.analyze(BodyResolveMode.PARTIAL)
        assert(bindingContext1 !== bindingContext2)

        val bindingContext3 = statement1.analyze(BodyResolveMode.PARTIAL)
        val bindingContext4 = statement2.analyze(BodyResolveMode.PARTIAL)
        assert(bindingContext3 === bindingContext1)
        assert(bindingContext4 === bindingContext2)

        file.add(factory.createFunction("fun foo(){}"))

        val bindingContext5 = statement1.analyze(BodyResolveMode.PARTIAL)
        assert(bindingContext5 !== bindingContext1)

        statement1.parent.addAfter(factory.createExpression("x()"), statement1)

        val bindingContext6 = statement1.analyze(BodyResolveMode.PARTIAL)
        assert(bindingContext6 !== bindingContext5)
    }

    fun testNonPhysicalFilePartialResolveCaching() {
        doTest {
            val nonPhysicalFile = KtPsiFactory(project).createAnalyzableFile("NonPhysical.kt", FILE_TEXT, file)
            val nonPhysicalData = extractData(nonPhysicalFile)
            nonPhysicalData.testPartialResolveCaching()
        }
    }

    fun testPartialResolveCachedForWholeStatement() {
        doTest {
            val statement = statements[0] as KtCallExpression
            val argument1 = statement.valueArguments[0]
            val argument2 = statement.valueArguments[1]
            val bindingContext1 = argument1.analyze(BodyResolveMode.PARTIAL)
            val bindingContext2 = argument2.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext1 === bindingContext2)
        }
    }

    fun testPartialResolveCachedForAllStatementsResolved() {
        doTest {
            val bindingContext1 = statements[2].analyze(BodyResolveMode.PARTIAL) // resolve 'd(x)'
            val bindingContext2 = (statements[1] as KtVariableDeclaration).initializer!!.analyze(BodyResolveMode.PARTIAL) // resolve initializer in 'val x = c()' - it required for resolved 'd(x)' and should be already resolved
            assert(bindingContext1 === bindingContext2)

            val bindingContext3 = statements[0].analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext3 !== bindingContext1)
        }
    }

    fun testPartialResolveCachedForDefaultParameterValue() {
        doTest {
            val defaultValue = (members[0] as KtNamedFunction).valueParameters[0].defaultValue
            val bindingContext1 = defaultValue!!.analyze(BodyResolveMode.PARTIAL)
            val bindingContext2 = defaultValue.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext1 === bindingContext2)

            val bindingContext3 = statements[0].analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext3 !== bindingContext2)
        }
    }

    fun testFullResolvedCachedWhenPartialForConstructorInvoked() {
        doTest {
            val defaultValue1 = klass.getPrimaryConstructorParameters()[0].defaultValue!!
            val defaultValue2 = klass.getPrimaryConstructorParameters()[1].defaultValue!!
            val bindingContext1 = defaultValue1.analyze(BodyResolveMode.PARTIAL)
            val bindingContext2 = defaultValue2.analyze(BodyResolveMode.FULL)
            assert(bindingContext1 === bindingContext2)
        }
    }

    fun testAnnotationEntry() {
        val file = myFixture.configureByText("Test.kt", """
        annotation class A
        @A class B {}
        """) as KtFile

        val klass = file.declarations[1] as KtClass
        val annotationEntry = klass.annotationEntries.single()

        val context = annotationEntry.analyze(BodyResolveMode.PARTIAL)
        assert(context[BindingContext.ANNOTATION, annotationEntry] != null)
    }

    fun testFileAnnotationList() {
        val file = myFixture.configureByText("Test.kt", """
        @file:Suppress("Some")
        @file:JvmName("Hi")
        """) as KtFile

        val fileAnnotationList = file.fileAnnotationList!!
        val context = fileAnnotationList.analyze(BodyResolveMode.PARTIAL)
        assert(context[BindingContext.ANNOTATION, fileAnnotationList.annotationEntries[0]] != null)
        assert(context[BindingContext.ANNOTATION, fileAnnotationList.annotationEntries[1]] != null)
    }

    fun testIncompleteFileAnnotationList() {
        val file = myFixture.configureByText("Test.kt", """
        @file
        import some.hello
        """) as KtFile

        val fileAnnotationList = file.fileAnnotationList!!
        fileAnnotationList.analyze(BodyResolveMode.PARTIAL)
    }

    fun testNamedParametersInFunctionType() {
        val file = myFixture.configureByText("Test.kt", """
        fun <K, V> intercept(block: (key: K, next: (K) -> V, K) -> V) {}
        """) as KtFile

        val function = file.declarations[0] as KtNamedFunction
        val functionType = function.valueParameters.first().typeReference!!.typeElement as KtFunctionType
        val descriptorsForParameters = functionType.parameters.map { it.resolveToDescriptor() }

        assert(
                listOf("key", "next", SpecialNames.NO_NAME_PROVIDED.asString()) ==
                        descriptorsForParameters.map { it.name.asString() }
        )
    }
}
