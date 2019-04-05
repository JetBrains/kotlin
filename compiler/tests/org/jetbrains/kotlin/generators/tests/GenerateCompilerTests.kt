/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

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
import org.jetbrains.kotlin.codegen.ir.*
import org.jetbrains.kotlin.fir.AbstractFirDiagnosticsSmokeTest
import org.jetbrains.kotlin.fir.AbstractFirLoadCompiledKotlin
import org.jetbrains.kotlin.fir.AbstractFir2IrTextTest
import org.jetbrains.kotlin.fir.AbstractFirResolveTestCase
import org.jetbrains.kotlin.fir.AbstractFirResolveTestCaseWithStdlib
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.java.AbstractFirTypeEnhancementTest
import org.jetbrains.kotlin.fir.java.AbstractOwnFirTypeEnhancementTest
import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.generators.util.KT_OR_KTS_WITHOUT_DOTS_IN_NAME
import org.jetbrains.kotlin.generators.util.KT_WITHOUT_DOTS_IN_NAME
import org.jetbrains.kotlin.integration.AbstractAntTaskTest
import org.jetbrains.kotlin.ir.AbstractIrCfgTestCase
import org.jetbrains.kotlin.ir.AbstractIrJsTextTestCase
import org.jetbrains.kotlin.ir.AbstractIrSourceRangesTestCase
import org.jetbrains.kotlin.ir.AbstractIrTextTestCase
import org.jetbrains.kotlin.jvm.compiler.*
import org.jetbrains.kotlin.jvm.compiler.javac.AbstractLoadJavaUsingJavacTest
import org.jetbrains.kotlin.lexer.kdoc.AbstractKDocLexerTest
import org.jetbrains.kotlin.lexer.kotlin.AbstractKotlinLexerTest
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

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("compiler/tests", "compiler/testData") {

        testClass<AbstractDiagnosticsTest> {
            model("diagnostics/tests", pattern = "^(.*)\\.kts?$")
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

        testClass<AbstractDiagnosticsWithUnsignedTypes> {
            model("diagnostics/testsWithUnsignedTypes")
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

        testClass<AbstractAsmLikeInstructionListingTest> {
            model("codegen/asmLike", targetBackend = TargetBackend.JVM)
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

        testClass<AbstractCustomScriptCodegenTest> {
            model("codegen/customScript", pattern = "^(.*)$")
        }

        testClass<AbstractBytecodeTextTest> {
            model("codegen/bytecodeText", targetBackend = TargetBackend.JVM)
        }

        testClass<AbstractIrTextTestCase> {
            model("ir/irText")
        }

        testClass<AbstractIrJsTextTestCase> {
            model("ir/irJsText")
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
            model("checkLocalVariablesTable", targetBackend = TargetBackend.JVM)
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
            model("cli/metadata", extension = "args", testMethod = "doMetadataTest", recursive = false)
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
            model("lineNumber")
        }

        testClass<AbstractLocalClassProtoTest> {
            model("serialization/local")
        }

        testClass<AbstractKDocLexerTest> {
            model("lexer/kdoc")
        }

        testClass<AbstractKotlinLexerTest> {
            model("lexer/kotlin")
        }

        testClass<AbstractIrBlackBoxCodegenTest> {
            model("codegen/box", targetBackend = TargetBackend.JVM_IR)
        }

        testClass<AbstractIrBlackBoxAgainstJavaCodegenTest> {
            model("codegen/boxAgainstJava", targetBackend = TargetBackend.JVM_IR)
        }

        testClass<AbstractIrCompileKotlinAgainstKotlinTest> {
            model("compileKotlinAgainstKotlin", targetBackend = TargetBackend.JVM_IR)
        }

        testClass<AbstractIrCheckLocalVariablesTableTest> {
            model("checkLocalVariablesTable", targetBackend = TargetBackend.JVM_IR)
        }

        testClass<AbstractIrLineNumberTest> {
            model("lineNumber", targetBackend = TargetBackend.JVM_IR)
        }

        testClass<AbstractIrBlackBoxInlineCodegenTest> {
            model("codegen/boxInline", targetBackend = TargetBackend.JVM_IR)
        }

        testClass<AbstractIrBytecodeTextTest> {
            model("codegen/bytecodeText", targetBackend = TargetBackend.JVM_IR)
        }
    }

    testGroup("compiler/fir/psi2fir/tests", "compiler/fir/psi2fir/testData") {
        testClass<AbstractRawFirBuilderTestCase> {
            model("rawBuilder", testMethod = "doRawFirTest")
        }
    }

    testGroup("compiler/fir/resolve/tests", "compiler/fir/resolve/testData") {
        testClass<AbstractFirResolveTestCase> {
            model("resolve", pattern = KT_WITHOUT_DOTS_IN_NAME, excludeDirs = listOf("stdlib"))
        }

        testClass<AbstractFirResolveTestCaseWithStdlib> {
            model("resolve/stdlib", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    testGroup("compiler/fir/resolve/tests", "compiler/testData") {
        testClass<AbstractFirLoadCompiledKotlin> {
            model("loadJava/compiledKotlin", extension = "kt")
        }
    }

    testGroup("compiler/fir/resolve/tests", "compiler/testData") {
        testClass<AbstractFirTypeEnhancementTest> {
            model("loadJava/compiledJava", extension = "java")
        }
    }

    testGroup("compiler/fir/resolve/tests", "compiler/fir/resolve/testData") {
        testClass<AbstractOwnFirTypeEnhancementTest> {
            model("enhancement", extension = "java")
        }
    }

    testGroup("compiler/fir/resolve/tests", "compiler/testData") {

        testClass<AbstractFirDiagnosticsSmokeTest> {
            model("diagnostics/tests")
        }
    }

    testGroup("compiler/fir/fir2ir/tests", "compiler/testData") {
        testClass<AbstractFir2IrTextTest> {
            model("ir/irText")
        }
    }

}
