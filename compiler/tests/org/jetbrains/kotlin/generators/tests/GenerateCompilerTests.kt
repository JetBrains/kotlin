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

package org.jetbrains.kotlin.generators.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.asJava.AbstractCompilerLightClassTest
import org.jetbrains.kotlin.cfg.AbstractControlFlowTest
import org.jetbrains.kotlin.cfg.AbstractDataFlowTest
import org.jetbrains.kotlin.cfg.AbstractDiagnosticsWithModifiedMockJdkTest
import org.jetbrains.kotlin.cfg.AbstractPseudoValueTest
import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.checkers.javac.*
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.defaultConstructor.AbstractDefaultArgumentsReflectionTest
import org.jetbrains.kotlin.codegen.flags.AbstractWriteFlagsTest
import org.jetbrains.kotlin.codegen.ir.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.integration.AbstractAntTaskTest
import org.jetbrains.kotlin.ir.AbstractIrCfgTestCase
import org.jetbrains.kotlin.ir.AbstractIrSourceRangesTestCase
import org.jetbrains.kotlin.ir.AbstractIrTextTestCase
import org.jetbrains.kotlin.jvm.compiler.*
import org.jetbrains.kotlin.jvm.compiler.javac.AbstractLoadJavaUsingJavacTest
import org.jetbrains.kotlin.jvm.runtime.AbstractJvmRuntimeDescriptorLoaderTest
import org.jetbrains.kotlin.kdoc.AbstractKDocLexerTest
import org.jetbrains.kotlin.modules.xml.AbstractModuleXmlParserTest
import org.jetbrains.kotlin.multiplatform.AbstractMultiPlatformIntegrationTest
import org.jetbrains.kotlin.parsing.AbstractParsingTest
import org.jetbrains.kotlin.renderer.AbstractDescriptorRendererTest
import org.jetbrains.kotlin.renderer.AbstractFunctionDescriptorInExpressionRendererTest
import org.jetbrains.kotlin.repl.AbstractReplInterpreterTest
import org.jetbrains.kotlin.resolve.AbstractResolveTest
import org.jetbrains.kotlin.resolve.annotation.AbstractAnnotationParameterTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedCallsTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedConstructorDelegationCallsTests
import org.jetbrains.kotlin.resolve.constants.evaluate.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.resolve.constraintSystem.AbstractConstraintSystemTest
import org.jetbrains.kotlin.serialization.AbstractLocalClassProtoTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.types.AbstractTypeBindingTest

@Language("RegExp") const val KT_OR_KTS = """^(.+)\.(kt|kts)$"""
@Language("RegExp") const val KT_OR_KTS_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.(kt|kts)$"""

@Language("RegExp") const val KT_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.kt$"""

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("compiler/tests", "compiler/testData") {

        testClass<AbstractDiagnosticsTest> {
            model("diagnostics/tests")
            model("diagnostics/tests/script", extension = "kts")
            model("codegen/box/diagnostics")
        }

        testClass<AbstractDiagnosticsUsingJavacTest> {
            model("diagnostics/tests")
            model("codegen/box/diagnostics")
        }

        testClass<AbstractJavacDiagnosticsTest> {
            model("javac/diagnostics/tests")
            model("javac/diagnostics/tests", testClassName = "TestsWithoutJavac", testMethod = "doTestWithoutJavacWrapper")
        }

        testClass<AbstractJavacFieldResolutionTest> {
            model("javac/fieldsResolution/tests")
            model("javac/fieldsResolution/tests", testClassName = "TestsWithoutJavac", testMethod = "doTestWithoutJavacWrapper")
        }

        testClass<AbstractDiagnosticsTestWithStdLib> {
            model("diagnostics/testsWithStdLib")
        }

        testClass<AbstractDiagnosticsTestWithStdLibUsingJavac> {
            model("diagnostics/testsWithStdLib")
        }

        testClass<AbstractDiagnosticsTestWithJsStdLib> {
            model("diagnostics/testsWithJsStdLib")
        }

        testClass<AbstractDiagnosticsTestWithJsStdLibAndBackendCompilation> {
            model("diagnostics/testsWithJsStdLibAndBackendCompilation")
        }

        testClass<AbstractDiagnosticsWithModifiedMockJdkTest> {
            model("diagnostics/testWithModifiedMockJdk")
        }

        testClass<AbstractDiagnosticsWithJdk9Test> {
            model("diagnostics/testsWithJava9")
        }

        testClass<AbstractMultiPlatformIntegrationTest> {
            model("multiplatform", extension = null, recursive = true, excludeParentDirs = true)
        }

        testClass<AbstractForeignAnnotationsTest> {
            model("foreignAnnotations/tests")
        }

        testClass<AbstractForeignAnnotationsNoAnnotationInClasspathTest> {
            model("foreignAnnotations/tests")
        }

        testClass<AbstractForeignAnnotationsNoAnnotationInClasspathWithFastClassReadingTest> {
            model("foreignAnnotations/tests")
        }

        testClass<AbstractJavacForeignAnnotationsTest> {
            model("foreignAnnotations/tests")
        }

        testClass<AbstractResolveTest> {
            model("resolve", extension = "resolve")
        }

        testClass<AbstractResolvedCallsTest> {
            model("resolvedCalls", excludeDirs = listOf("enhancedSignatures"))
        }

        testClass<AbstractResolvedConstructorDelegationCallsTests> {
            model("resolveConstructorDelegationCalls")
        }

        testClass<AbstractConstraintSystemTest> {
            model("constraintSystem", extension = "constraints")
        }

        testClass<AbstractParsingTest> {
            model("psi", testMethod = "doParsingTest", pattern = "^(.*)\\.kts?$")
            model("parseCodeFragment/expression", testMethod = "doExpressionCodeFragmentParsingTest", extension = "kt")
            model("parseCodeFragment/block", testMethod = "doBlockCodeFragmentParsingTest", extension = "kt")
        }

        GenerateRangesCodegenTestData.main(emptyArray<String>())

        testClass<AbstractBlackBoxCodegenTest> {
            model("codegen/box", targetBackend = TargetBackend.JVM)
        }

        testClass<AbstractLightAnalysisModeTest> {
            model("codegen/box", targetBackend = TargetBackend.JVM, skipIgnored = true)
        }

        testClass<AbstractKapt3BuilderModeBytecodeShapeTest> {
            model("codegen/kapt", targetBackend = TargetBackend.JVM)
        }

        testClass<AbstractIrBlackBoxCodegenTest>("IrOnlyBoxCodegenTestGenerated") {
            model("ir/box", targetBackend = TargetBackend.JVM)
        }

        testClass<AbstractBlackBoxInlineCodegenTest> {
            model("codegen/boxInline")
        }

        testClass<AbstractCompileKotlinAgainstInlineKotlinTest> {
            model("codegen/boxInline")
        }

        testClass<AbstractBlackBoxAgainstJavaCodegenTest> {
            model("codegen/boxAgainstJava")
        }

        testClass<AbstractScriptCodegenTest> {
            model("codegen/script", extension = "kts")
        }

        testClass<AbstractBytecodeTextTest> {
            model("codegen/bytecodeText")
        }

        testClass<AbstractIrTextTestCase> {
            model("ir/irText")
        }

        testClass<AbstractIrCfgTestCase> {
            model("ir/irCfg")
        }

        testClass<AbstractIrSourceRangesTestCase> {
            model("ir/sourceRanges")
        }

        testClass<AbstractBytecodeListingTest> {
            model("codegen/bytecodeListing")
        }

        testClass<AbstractTopLevelMembersInvocationTest> {
            model("codegen/topLevelMemberInvocation", extension = null, recursive = false)
        }

        testClass<AbstractCheckLocalVariablesTableTest> {
            model("checkLocalVariablesTable")
        }

        testClass<AbstractWriteFlagsTest> {
            model("writeFlags")
        }

        testClass<AbstractDefaultArgumentsReflectionTest> {
            model("codegen/defaultArguments/reflection")
        }

        testClass<AbstractDumpDeclarationsTest> {
            model("codegen/dumpDeclarations")
        }

        testClass<AbstractLoadJavaTest> {
            model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
            model("loadJava/compiledJavaAndKotlin", extension = "txt", testMethod = "doTestCompiledJavaAndKotlin")
            model("loadJava/compiledJavaIncludeObjectMethods", extension = "java", testMethod = "doTestCompiledJavaIncludeObjectMethods")
            model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
            model("loadJava/compiledKotlinWithStdlib", testMethod = "doTestCompiledKotlinWithStdlib")
            model("loadJava/javaAgainstKotlin", extension = "txt", testMethod = "doTestJavaAgainstKotlin")
            model("loadJava/kotlinAgainstCompiledJavaWithKotlin", extension = "kt", testMethod = "doTestKotlinAgainstCompiledJavaWithKotlin", recursive = false)
            model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava")
        }

        testClass<AbstractLoadJavaUsingJavacTest> {
            model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
            model("loadJava/compiledJavaAndKotlin", extension = "txt", testMethod = "doTestCompiledJavaAndKotlin")
            model("loadJava/compiledJavaIncludeObjectMethods", extension = "java", testMethod = "doTestCompiledJavaIncludeObjectMethods")
            model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
            model("loadJava/compiledKotlinWithStdlib", testMethod = "doTestCompiledKotlinWithStdlib")
            model("loadJava/javaAgainstKotlin", extension = "txt", testMethod = "doTestJavaAgainstKotlin")
            model("loadJava/kotlinAgainstCompiledJavaWithKotlin", extension = "kt", testMethod = "doTestKotlinAgainstCompiledJavaWithKotlin", recursive = false)
            model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava")
        }

        testClass<AbstractLoadKotlinWithTypeTableTest> {
            model("loadJava/compiledKotlin")
        }

        testClass<AbstractJvmRuntimeDescriptorLoaderTest> {
            model("loadJava/compiledKotlin")
            model("loadJava/compiledJava", extension = "java", excludeDirs = listOf("sam", "kotlinSignature/propagation"))
        }

        testClass<AbstractLoadJavaWithFastClassReadingTest> {
            model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
        }

        testClass<AbstractCompileJavaAgainstKotlinTest> {
            model("compileJavaAgainstKotlin", testClassName = "WithoutJavac", testMethod = "doTestWithoutJavac")
            model("compileJavaAgainstKotlin", testClassName = "WithJavac", testMethod = "doTestWithJavac")
        }

        testClass<AbstractCompileKotlinAgainstJavaTest> {
            model("compileKotlinAgainstJava")
        }

        testClass<AbstractCompileKotlinAgainstKotlinTest> {
            model("compileKotlinAgainstKotlin")
        }

        testClass<AbstractDescriptorRendererTest> {
            model("renderer")
        }

        testClass<AbstractFunctionDescriptorInExpressionRendererTest> {
            model("renderFunctionDescriptorInExpression")
        }

        testClass<AbstractModuleXmlParserTest> {
            model("modules.xml", extension = "xml")
        }

        testClass<AbstractWriteSignatureTest> {
            model("writeSignature")
        }

        testClass<AbstractCliTest> {
            model("cli/jvm", extension = "args", testMethod = "doJvmTest", recursive = false)
            model("cli/js", extension = "args", testMethod = "doJsTest", recursive = false)
            model("cli/js-dce", extension = "args", testMethod = "doJsDceTest", recursive = false)
        }

        testClass<AbstractReplInterpreterTest> {
            model("repl", extension = "repl")
        }

        testClass<AbstractAntTaskTest> {
            model("integration/ant/jvm", extension = null, recursive = false, excludeParentDirs = true)
        }

        testClass<AbstractControlFlowTest> {
            model("cfg")
            model("cfgWithStdLib", testMethod = "doTestWithStdLib")
        }

        testClass<AbstractDataFlowTest> {
            model("cfg-variables")
            model("cfgVariablesWithStdLib", testMethod = "doTestWithStdLib")
        }

        testClass<AbstractPseudoValueTest> {
            model("cfg")
            model("cfgWithStdLib", testMethod = "doTestWithStdLib")
            model("cfg-variables")
            model("cfgVariablesWithStdLib", testMethod = "doTestWithStdLib")
        }

        testClass<AbstractAnnotationParameterTest> {
            model("resolveAnnotations/parameters")
        }

        testClass<AbstractCompileTimeConstantEvaluatorTest> {
            model("evaluate/constant", testMethod = "doConstantTest")
            model("evaluate/isPure", testMethod = "doIsPureTest")
            model("evaluate/usesVariableAsConstant", testMethod = "doUsesVariableAsConstantTest")
        }

        testClass<AbstractCompilerLightClassTest> {
            model("asJava/lightClasses", excludeDirs = listOf("local", "ideRegression"), pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractTypeBindingTest> {
            model("type/binding")
        }

        testClass<AbstractLineNumberTest> {
            model("lineNumber", recursive = false)
            model("lineNumber/custom", testMethod = "doTestCustom")
        }

        testClass<AbstractLocalClassProtoTest> {
            model("serialization/local")
        }

        testClass<AbstractKDocLexerTest> {
            model("kdoc/lexer")
        }
    }
}
