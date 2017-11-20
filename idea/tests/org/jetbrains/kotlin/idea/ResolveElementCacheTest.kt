/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.containsError

class ResolveElementCacheTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE

    //language=kotlin
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
        x(2)
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

            val bindingContext4 = statement2.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
            assert(bindingContext4 === bindingContext1)
        }
    }

    fun testPartialResolveCaching() {
        doTest { this.testPartialResolveCaching(BodyResolveMode.PARTIAL) }
    }

    fun testPartialForCompletionResolveCaching() {
        doTest { this.testPartialResolveCaching(BodyResolveMode.PARTIAL_FOR_COMPLETION) }
    }

    private fun Data.testPartialResolveCaching(mode: BodyResolveMode) {
        val statement1 = statements[0]
        val statement2 = statements[1]
        val bindingContext1 = statement1.analyze(mode)
        val bindingContext2 = statement2.analyze(mode)
        assert(bindingContext1 !== bindingContext2)

        val bindingContext3 = statement1.analyze(mode)
        val bindingContext4 = statement2.analyze(mode)
        assert(bindingContext3 === bindingContext1)
        assert(bindingContext4 === bindingContext2)

        file.add(factory.createFunction("fun foo(){}"))

        val bindingContext5 = statement1.analyze(mode)
        assert(bindingContext5 !== bindingContext1)

        statement1.parent.addAfter(factory.createExpression("x()"), statement1)

        val bindingContext6 = statement1.analyze(mode)
        assert(bindingContext6 !== bindingContext5)
    }

    fun testNonPhysicalFilePartialResolveCaching() {
        doTest {
            val nonPhysicalFile = KtPsiFactory(project).createAnalyzableFile("NonPhysical.kt", FILE_TEXT, file)
            val nonPhysicalData = extractData(nonPhysicalFile)
            nonPhysicalData.testPartialResolveCaching(BodyResolveMode.PARTIAL)
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

    fun testPartialForCompletionAndPartialAfter() {
        doTest {
            val statement = statements[0]
            val bindingContext1 = statement.analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION)
            val bindingContext2 = statement.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext2 === bindingContext1)

            val bindingContext3 = statement.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
            assert(bindingContext3 !== bindingContext1)
        }
    }

    fun testPartialWithDiagnosticsAndPartialAfter() {
        doTest {
            val statement = statements[0]
            val bindingContext1 = statement.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
            val bindingContext2 = statement.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext2 === bindingContext1)

            val bindingContext3 = statement.analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION)
            assert(bindingContext3 !== bindingContext1)
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
        val file = configureWithKotlin(
                """
                annotation class A
                @A class B {}
                """)

        val klass = file.declarations[1] as KtClass
        val annotationEntry = klass.annotationEntries.single()

        val context = annotationEntry.analyze(BodyResolveMode.PARTIAL)
        assert(context[BindingContext.ANNOTATION, annotationEntry] != null)
    }

    fun testFileAnnotationList() {
        val file = configureWithKotlin(
                """
                @file:Suppress("Some")
                @file:JvmName("Hi")
                """)

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
        val file = configureWithKotlin(
                """
                fun <K, V> intercept(block: (key: K, next: (K) -> V, K) -> V) {}
                """)

        val function = file.declarations[0] as KtNamedFunction
        val functionType = function.valueParameters.first().typeReference!!.typeElement as KtFunctionType
        val descriptorsForParameters = functionType.parameters.map { it.unsafeResolveToDescriptor() }

        assert(
                listOf("key", "next", SpecialNames.NO_NAME_PROVIDED.asString()) ==
                        descriptorsForParameters.map { it.name.asString() }
        )
    }

    fun testNoBodyResolveOnFunctionParameterAnalyze() {
        val file = configureWithKotlin(
                """
                fun test(a: String) {
                    unresolved // Check diagnostics is empty even in FULL mode when starting analyzing for parameter
                }
                """)

        val function = file.declarations[0] as KtNamedFunction
        val functionParameter = function.valueParameters.first()

        val context = functionParameter.analyze(BodyResolveMode.FULL)

        assertEmpty(context.diagnostics.all())
    }

    private fun configureWithKotlin(@Language("kotlin") text: String): KtFile {
        return myFixture.configureByText("Test.kt", text.trimIndent()) as KtFile
    }

    fun testPrimaryConstructorParameterFullAnalysis() {
        val file = myFixture.configureByText("Test.kt", """
        class My(param: Int = 0)
        """) as KtFile

        val defaultValue = ((file.declarations[0]) as KtClass).getPrimaryConstructor()!!.valueParameters[0].defaultValue!!
        defaultValue.analyzeFullyAndGetResult()
    }

    fun testPrimaryConstructorAnnotationFullAnalysis() {
        val file = myFixture.configureByText("Test.kt", """
        class My @Deprecated("xyz") protected constructor(param: Int)
        """) as KtFile

        val annotationArguments = ((file.declarations[0]) as KtClass).getPrimaryConstructor()!!.annotationEntries[0].valueArgumentList!!
        annotationArguments.analyzeFullyAndGetResult()
    }

    fun testFunctionParameterAnnotation() {
        val file = myFixture.configureByText("Test.kt", """
        annotation class Ann
        fun foo(@Ann p: Int) {
            bar()
        }
        """) as KtFile

        val function = (file.declarations[1]) as KtFunction
        val annotationEntry = function.valueParameters[0].annotationEntries[0]
        val typeRef = annotationEntry.typeReference!!

        val bindingContext = typeRef.analyze(BodyResolveMode.PARTIAL)

        val referenceExpr = (typeRef.typeElement as KtUserType).referenceExpression
        val target = bindingContext[BindingContext.REFERENCE_TARGET, referenceExpr]
        TestCase.assertEquals("Ann", target?.importableFqName?.asString())

        val statement = (function.bodyExpression as KtBlockExpression).statements[0]
        TestCase.assertEquals(null, bindingContext[BindingContext.PROCESSED, statement])
    }

    fun testPrimaryConstructorParameterAnnotation() {
        val file = myFixture.configureByText("Test.kt", """
        annotation class Ann
        class X(@set:Ann var p: Int)
        """) as KtFile

        val constructor = ((file.declarations[1]) as KtClass).getPrimaryConstructor()!!
        val annotationEntry = constructor.valueParameters[0].annotationEntries[0]
        val typeRef = annotationEntry.typeReference!!

        val bindingContext = typeRef.analyze(BodyResolveMode.PARTIAL)

        val referenceExpr = (typeRef.typeElement as KtUserType).referenceExpression
        val target = bindingContext[BindingContext.REFERENCE_TARGET, referenceExpr]
        TestCase.assertEquals("Ann", target?.importableFqName?.asString())
    }

    fun testSecondaryConstructorParameterAnnotation() {
        val file = myFixture.configureByText("Test.kt", """
        annotation class Ann
        class X {
            constructor(@Ann p: Int) {
                foo()
            }
        }
        """) as KtFile

        val constructor = ((file.declarations[1]) as KtClass).getSecondaryConstructors()[0]
        val annotationEntry = constructor.valueParameters[0].annotationEntries[0]
        val typeRef = annotationEntry.typeReference!!

        val bindingContext = typeRef.analyze(BodyResolveMode.PARTIAL)

        val referenceExpr = (typeRef.typeElement as KtUserType).referenceExpression
        val target = bindingContext[BindingContext.REFERENCE_TARGET, referenceExpr]
        TestCase.assertEquals("Ann", target?.importableFqName?.asString())

        val statement = (constructor.bodyExpression as KtBlockExpression).statements[0]
        TestCase.assertEquals(null, bindingContext[BindingContext.PROCESSED, statement])
    }

    fun testFullResolveMultiple() {
        doTest {
            val aBody = (members[0] as KtFunction).bodyExpression as KtBlockExpression
            val statement1InFunA = aBody.statements[0]
            val statement2InFunA = aBody.statements[1]
            val statementInFunB = ((members[1] as KtFunction).bodyExpression as KtBlockExpression).statements[0]
            val statementInFunC = ((members[2] as KtFunction).bodyExpression as KtBlockExpression).statements[0]

            val bindingContext = checkResolveMultiple(BodyResolveMode.FULL, statement1InFunA, statementInFunB)

            TestCase.assertEquals(true, bindingContext[BindingContext.PROCESSED, statement2InFunA])
            TestCase.assertEquals(null, bindingContext[BindingContext.PROCESSED, statementInFunC])
        }
    }

    fun testPartialResolveMultiple() {
        doTest {
            val aBody = (members[0] as KtFunction).bodyExpression as KtBlockExpression
            val statement1InFunA = aBody.statements[0]
            val statement2InFunA = aBody.statements[1]
            val statementInFunB = ((members[1] as KtFunction).bodyExpression as KtBlockExpression).statements[0]
            val constructorParameterDefault = klass.getPrimaryConstructor()!!.valueParameters[1].defaultValue!!
            val funC = members[2]

            checkResolveMultiple(BodyResolveMode.PARTIAL, statement1InFunA, statement2InFunA, statementInFunB, constructorParameterDefault, funC)
        }
    }

    fun testPartialResolveMultipleInOneFunction() {
        doTest {
            val aBody = (members[0] as KtFunction).bodyExpression as KtBlockExpression
            val statement1InFunA = aBody.statements[0]
            val statement2InFunA = aBody.statements[1]

            val bindingContext = checkResolveMultiple(BodyResolveMode.PARTIAL, statement1InFunA, statement2InFunA)

            val bindingContext1 = statement1InFunA.analyze(BodyResolveMode.PARTIAL)
            assert(bindingContext1 === bindingContext)
        }
    }

    fun testKT14376() {
        val file = myFixture.configureByText("Test.kt", "object Obj(val x: Int)") as KtFile
        val nameRef = file.findDescendantOfType<KtNameReferenceExpression>()!!
        val bindingContext = nameRef.analyze(BodyResolveMode.PARTIAL)
        assert(bindingContext[BindingContext.REFERENCE_TARGET, nameRef]?.fqNameSafe?.asString() == "kotlin.Int")
    }

    fun testResolveDefaultValueInPrimaryConstructor() {
        val file = myFixture.configureByText("Test.kt", """
        class ClassA<N> (
                messenger: ClassB<N> = object : ClassB<N> {
                    override fun methodOne(param: List<N>) {
                    }
                }
        )

        interface ClassB<N> {
            fun methodOne(param: List<N>)
        }
        """) as KtFile

        val classA = file.declarations[0] as KtClass
        val defaultValue = classA.primaryConstructor!!.valueParameters[0].defaultValue as KtObjectLiteralExpression
        val methodOne = defaultValue.objectDeclaration.declarations[0] as KtFunction

        val bindingContext = methodOne.analyze(BodyResolveMode.FULL)

        val parameter = methodOne.valueParameters[0]
        val parameterDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parameter] as ValueParameterDescriptor

        assert(!parameterDescriptor.type.containsError())
    }

    private fun checkResolveMultiple(mode: BodyResolveMode, vararg expressions: KtExpression): BindingContext {
        val resolutionFacade = expressions.first().getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(expressions.asList(), mode)

        expressions.forEach {
            if (it !is KtDeclaration) {
                TestCase.assertEquals(true, bindingContext[BindingContext.PROCESSED, it])
            }
            else {
                TestCase.assertNotNull(bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it])
            }
        }

        return bindingContext
    }
}
