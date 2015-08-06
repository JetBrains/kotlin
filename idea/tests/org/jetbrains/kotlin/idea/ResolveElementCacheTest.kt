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
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class ResolveElementCacheTest : JetLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = JetLightProjectDescriptor.INSTANCE

    private val FILE_TEXT =
"""
class C(param1: String = "", param2: Int = 0) {
    fun a(p: Int = 0) {
        b(1, 2)
        val x = c()
        d(x)
    }

    fun b() {
    }

    fun c() {
    }
}
"""

    private data class Data(
            val file: JetFile,
            val klass: JetClass,
            val members: List<JetDeclaration>,
            val statements: List<JetExpression>,
            val factory: JetPsiFactory
    )

    private fun doTest(handler: Data.() -> Unit) {
        val file = myFixture.configureByText("Test.kt", FILE_TEXT) as JetFile
        val klass = file.getDeclarations().single() as JetClass
        val members = klass.getDeclarations()
        val function = members.first() as JetNamedFunction
        val statements = (function.getBodyExpression() as JetBlockExpression).getStatements()
        myFixture.getProject().executeWriteCommand("") {
            Data(file, klass, members, statements, JetPsiFactory(getProject())).handler()
        }
    }

    public fun testResolveCaching() {
        doTest {
            // resolve statements in "a()"
            val statement1 = statements[0]
            val statement2 = statements[1]
            val bindingContext1 = statement1.analyze(BodyResolveMode.FULL)
            val bindingContext2 = statement2.analyze(BodyResolveMode.FULL)
            assert(bindingContext1 === bindingContext2)

            val bindingContext3 = statement1.analyze(BodyResolveMode.FULL)
            assert(bindingContext3 === bindingContext1)

            // modify body of "b()"
            val bFun = members[1] as JetNamedFunction
            val bBody = bFun.bodyExpression as JetBlockExpression
            bBody.addAfter(factory.createExpression("x()"), bBody.lBrace)

            val bindingContext4 = statement1.analyze(BodyResolveMode.FULL)
            assert(bindingContext4 === bindingContext1) // change in other function's body should not affect resolve of other one

            // add parameter to "b()" this should invalidate all resolve
            bFun.valueParameterList!!.addParameter(factory.createParameter("p: Int"))

            val bindingContext5 = statement1.analyze(BodyResolveMode.FULL)
            assert(bindingContext5 !== bindingContext1)

            statement1.parent.addAfter(factory.createExpression("x()"), statement1)

            val bindingContext6 = statement1.analyze(BodyResolveMode.FULL)
            assert(bindingContext6 !== bindingContext5)
        }
    }

    public fun testResolveSurvivesTypingInCodeBlock() {
        doTest {
            val statement = statements[0]
            val bindingContext1 = statement.analyze(BodyResolveMode.FULL)

            val classConstructorParamTypeRef = klass.getPrimaryConstructor()!!.valueParameters.first().typeReference!!
            val bindingContext2 = classConstructorParamTypeRef.analyze(BodyResolveMode.FULL)

            val documentManager = PsiDocumentManager.getInstance(getProject())
            val document = documentManager.getDocument(file)!!
            documentManager.doPostponedOperationsAndUnblockDocument(document)

            // modify body of "b()" via document
            val bFun = members[1] as JetNamedFunction
            val bBody = bFun.bodyExpression as JetBlockExpression
            document.insertString(bBody.lBrace!!.startOffset + 1, "x()")
            documentManager.commitAllDocuments()

            val bindingContext3 = statement.analyze(BodyResolveMode.FULL)
            assert(bindingContext3 === bindingContext1)

            val bindingContext4 = classConstructorParamTypeRef.analyze(BodyResolveMode.FULL)
            assert(bindingContext4 === bindingContext2)

            // insert statement in "a()"
            document.insertString(statement.startOffset, "x()\n")
            documentManager.commitAllDocuments()

            val bindingContext5 = (members[0] as JetNamedFunction).bodyExpression!!.analyze(BodyResolveMode.FULL)
            assert(bindingContext5 !== bindingContext1)

            val bindingContext6 = classConstructorParamTypeRef.analyze(BodyResolveMode.FULL)
            assert(bindingContext6 === bindingContext2)
        }
    }

    public fun testPartialResolveUsesFullResolveCached() {
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

    public fun testPartialResolveCaching() {
        doTest {
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

            statement1.getParent().addAfter(factory.createExpression("x()"), statement1)

            val bindingContext6 = statement1.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext6 !== bindingContext5)
        }
    }

    public fun testPartialResolveCachedForWholeStatement() {
        doTest {
            val statement = statements[0] as JetCallExpression
            val argument1 = statement.getValueArguments()[0]
            val argument2 = statement.getValueArguments()[1]
            val bindingContext1 = argument1.analyze(BodyResolveMode.PARTIAL)
            val bindingContext2 = argument2.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext1 === bindingContext2)
        }
    }

    public fun testPartialResolveCachedForAllStatementsResolved() {
        doTest {
            val bindingContext1 = statements[2].analyze(BodyResolveMode.PARTIAL) // resolve 'd(x)'
            val bindingContext2 = (statements[1] as JetVariableDeclaration).getInitializer()!!.analyze(BodyResolveMode.PARTIAL) // resolve initializer in 'val x = c()' - it required for resolved 'd(x)' and should be already resolved
            assert(bindingContext1 === bindingContext2)

            val bindingContext3 = statements[0].analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext3 !== bindingContext1)
        }
    }

    public fun testPartialResolveCachedForDefaultParameterValue() {
        doTest {
            val defaultValue = (members[0] as JetNamedFunction).getValueParameters()[0].getDefaultValue()
            val bindingContext1 = defaultValue!!.analyze(BodyResolveMode.PARTIAL)
            val bindingContext2 = defaultValue.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext1 === bindingContext2)

            val bindingContext3 = statements[0].analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext3 !== bindingContext2)
        }
    }

    public fun testFullResolvedCachedWhenPartialForConstructorInvoked() {
        doTest {
            val defaultValue1 = klass.getPrimaryConstructorParameters()[0].getDefaultValue()!!
            val defaultValue2 = klass.getPrimaryConstructorParameters()[1].getDefaultValue()!!
            val bindingContext1 = defaultValue1.analyze(BodyResolveMode.PARTIAL)
            val bindingContext2 = defaultValue2.analyze(BodyResolveMode.FULL)
            assert(bindingContext1 === bindingContext2)
        }
    }

    public fun testAnnotationEntry() {
        val file = myFixture.configureByText("Test.kt", """
        annotation class A
        A class B {}
        """) as JetFile

        val klass = file.getDeclarations()[1] as JetClass
        val annotationEntry = klass.getAnnotationEntries().single()

        val context = annotationEntry.analyze(BodyResolveMode.PARTIAL)
        assert(context[BindingContext.ANNOTATION, annotationEntry] != null)
    }
}
