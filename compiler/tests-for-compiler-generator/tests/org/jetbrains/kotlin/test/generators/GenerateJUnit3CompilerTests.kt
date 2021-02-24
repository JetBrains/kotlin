/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.asJava.AbstractCompilerLightClassTest
import org.jetbrains.kotlin.cfg.AbstractControlFlowTest
import org.jetbrains.kotlin.cfg.AbstractDataFlowTest
import org.jetbrains.kotlin.cfg.AbstractPseudoValueTest
import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.debugInformation.AbstractIrLocalVariableTest
import org.jetbrains.kotlin.codegen.debugInformation.AbstractIrSteppingTest
import org.jetbrains.kotlin.codegen.debugInformation.AbstractLocalVariableTest
import org.jetbrains.kotlin.codegen.debugInformation.AbstractSteppingTest
import org.jetbrains.kotlin.codegen.defaultConstructor.AbstractDefaultArgumentsReflectionTest
import org.jetbrains.kotlin.codegen.flags.AbstractWriteFlagsTest
import org.jetbrains.kotlin.codegen.ir.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.AbstractPartialRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderLazyBodiesTestCase
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderSourceElementMappingTestCase
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.java.AbstractFirOldFrontendLightClassesTest
import org.jetbrains.kotlin.fir.java.AbstractFirTypeEnhancementTest
import org.jetbrains.kotlin.fir.java.AbstractOwnFirTypeEnhancementTest
import org.jetbrains.kotlin.fir.lightTree.AbstractLightTree2FirConverterTestCase
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
import org.jetbrains.kotlin.integration.AbstractAntTaskTest
import org.jetbrains.kotlin.ir.AbstractIrCfgTestCase
import org.jetbrains.kotlin.ir.AbstractIrJsTextTestCase
import org.jetbrains.kotlin.ir.AbstractIrSourceRangesTestCase
import org.jetbrains.kotlin.jvm.compiler.*
import org.jetbrains.kotlin.jvm.compiler.ir.AbstractIrCompileJavaAgainstKotlinTest
import org.jetbrains.kotlin.jvm.compiler.ir.AbstractIrCompileKotlinAgainstJavaTest
import org.jetbrains.kotlin.jvm.compiler.ir.AbstractIrLoadJavaTest
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

fun generateJUnit3CompilerTests(args: Array<String>) {
    generateTestGroupSuite(args) {
        testGroup("compiler/tests-gen", "compiler/testData") {
            testClass<AbstractDiagnosticsTestWithJsStdLibAndBackendCompilation> {
                model("diagnostics/testsWithJsStdLibAndBackendCompilation")
            }

            testClass<AbstractMultiPlatformIntegrationTest> {
                model("multiplatform", extension = null, recursive = true, excludeParentDirs = true)
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

            testClass<AbstractLightAnalysisModeTest> {
                // "ranges/stepped" is excluded because it contains hundreds of generated tests and only have a box() method.
                // There isn't much to be gained from running light analysis tests on them.
                model(
                    "codegen/box",
                    targetBackend = TargetBackend.JVM,
                    skipIgnored = true,
                    excludeDirs = listOf("ranges/stepped", "compileKotlinAgainstKotlin")
                )
            }

            testClass<AbstractKapt3BuilderModeBytecodeShapeTest> {
                model("codegen/kapt", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractAsmLikeInstructionListingTest> {
                model("codegen/asmLike", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractJdk15BlackBoxCodegenTest> {
                model("codegen/java15/box")
            }

            testClass<AbstractJdk15IrBlackBoxCodegenTest> {
                model("codegen/java15/box", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractJdk9BlackBoxCodegenTest> {
                model("codegen/java9/box")
            }

            testClass<AbstractJdk9IrBlackBoxCodegenTest> {
                model("codegen/java9/box", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractScriptCodegenTest> {
                model("codegen/script", extension = "kts")
            }

            testClass<AbstractCustomScriptCodegenTest> {
                model("codegen/customScript", pattern = "^(.*)$")
            }

            testClass<AbstractIrJsTextTestCase> {
                model("ir/irJsText", pattern = "^(.+)\\.kt(s)?\$")
            }

            testClass<AbstractIrCfgTestCase> {
                model("ir/irCfg")
            }

            testClass<AbstractIrSourceRangesTestCase> {
                model("ir/sourceRanges")
            }

            testClass<AbstractBytecodeListingTest> {
                model("codegen/bytecodeListing", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractTopLevelMembersInvocationTest> {
                model("codegen/topLevelMemberInvocation", extension = null, recursive = false)
            }

            testClass<AbstractCheckLocalVariablesTableTest> {
                model("checkLocalVariablesTable", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractWriteFlagsTest> {
                model("writeFlags", targetBackend = TargetBackend.JVM)
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
                model(
                    "loadJava/compiledJavaIncludeObjectMethods",
                    extension = "java",
                    testMethod = "doTestCompiledJavaIncludeObjectMethods"
                )
                model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
                model("loadJava/compiledKotlinWithStdlib", testMethod = "doTestCompiledKotlinWithStdlib")
                model("loadJava/javaAgainstKotlin", extension = "txt", testMethod = "doTestJavaAgainstKotlin")
                model(
                    "loadJava/kotlinAgainstCompiledJavaWithKotlin",
                    extension = "kt",
                    testMethod = "doTestKotlinAgainstCompiledJavaWithKotlin",
                    recursive = false
                )
                model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava")
            }

            testClass<AbstractLoadJavaUsingJavacTest> {
                model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
                model("loadJava/compiledJavaAndKotlin", extension = "txt", testMethod = "doTestCompiledJavaAndKotlin")
                model(
                    "loadJava/compiledJavaIncludeObjectMethods",
                    extension = "java",
                    testMethod = "doTestCompiledJavaIncludeObjectMethods"
                )
                model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
                model("loadJava/compiledKotlinWithStdlib", testMethod = "doTestCompiledKotlinWithStdlib")
                model("loadJava/javaAgainstKotlin", extension = "txt", testMethod = "doTestJavaAgainstKotlin")
                model(
                    "loadJava/kotlinAgainstCompiledJavaWithKotlin",
                    extension = "kt",
                    testMethod = "doTestKotlinAgainstCompiledJavaWithKotlin",
                    recursive = false
                )
                model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava")
            }

            testClass<AbstractLoadKotlinWithTypeTableTest> {
                model("loadJava/compiledKotlin")
            }

            testClass<AbstractLoadJavaWithPsiClassReadingTest> {
                model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
            }

            testClass<AbstractLoadJava15Test> {
                model("loadJava15", extension = "java", testMethod = "doTestCompiledJava", testClassName = "CompiledJava")
                model("loadJava15", extension = "java", testMethod = "doTestSourceJava", testClassName = "SourceJava")
            }

            testClass<AbstractLoadJava15WithPsiClassReadingTest> {
                model("loadJava15", extension = "java", testMethod = "doTestCompiledJava")
            }

            testClass<AbstractCompileJavaAgainstKotlinTest> {
                model(
                    "compileJavaAgainstKotlin",
                    testClassName = "WithoutJavac",
                    testMethod = "doTestWithoutJavac",
                    targetBackend = TargetBackend.JVM
                )
                model(
                    "compileJavaAgainstKotlin",
                    testClassName = "WithJavac",
                    testMethod = "doTestWithJavac",
                    targetBackend = TargetBackend.JVM
                )
            }

            testClass<AbstractCompileKotlinAgainstJavaTest> {
                model(
                    "compileKotlinAgainstJava",
                    testClassName = "WithAPT",
                    testMethod = "doTestWithAPT",
                    targetBackend = TargetBackend.JVM
                )
                model(
                    "compileKotlinAgainstJava",
                    testClassName = "WithoutAPT",
                    testMethod = "doTestWithoutAPT",
                    targetBackend = TargetBackend.JVM
                )
            }

            testClass<AbstractCompileKotlinAgainstKotlinJdk15Test> {
                model("compileKotlinAgainstKotlinJdk15")
            }

            testClass<AbstractIrCompileKotlinAgainstKotlinJdk15Test> {
                model("compileKotlinAgainstKotlinJdk15", targetBackend = TargetBackend.JVM_IR)
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
                model("integration/ant/jvm", extension = null, recursive = false)
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

            testClass<AbstractSteppingTest>(useJunit4 = true) {
                model("debug/stepping", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractLocalVariableTest>(useJunit4 = true) {
                model("debug/localVariables", targetBackend = TargetBackend.JVM)
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

            testClass<AbstractComposeLikeIrBlackBoxCodegenTest> {
                model("codegen/composeLike", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractComposeLikeIrBytecodeTextTest> {
                model("codegen/composeLikeBytecodeText", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrCompileJavaAgainstKotlinTest> {
                model(
                    "compileJavaAgainstKotlin",
                    testClassName = "WithoutJavac",
                    testMethod = "doTestWithoutJavac",
                    targetBackend = TargetBackend.JVM_IR
                )
                model(
                    "compileJavaAgainstKotlin",
                    testClassName = "WithJavac",
                    testMethod = "doTestWithJavac",
                    targetBackend = TargetBackend.JVM_IR
                )
            }

            testClass<AbstractIrCompileKotlinAgainstJavaTest> {
                model(
                    "compileKotlinAgainstJava",
                    testClassName = "WithAPT",
                    testMethod = "doTestWithAPT",
                    targetBackend = TargetBackend.JVM_IR
                )
                model(
                    "compileKotlinAgainstJava",
                    testClassName = "WithoutAPT",
                    testMethod = "doTestWithoutAPT",
                    targetBackend = TargetBackend.JVM_IR
                )
            }

            testClass<AbstractIrBytecodeListingTest> {
                model("codegen/bytecodeListing", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrCheckLocalVariablesTableTest> {
                model("checkLocalVariablesTable", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrWriteFlagsTest> {
                model("writeFlags", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrWriteSignatureTest> {
                model("writeSignature", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrLoadJavaTest> {
                model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava", targetBackend = TargetBackend.JVM_IR)
                model(
                    "loadJava/compiledJavaAndKotlin",
                    extension = "txt",
                    testMethod = "doTestCompiledJavaAndKotlin",
                    targetBackend = TargetBackend.JVM_IR
                )
                model(
                    "loadJava/compiledJavaIncludeObjectMethods",
                    extension = "java",
                    testMethod = "doTestCompiledJavaIncludeObjectMethods",
                    targetBackend = TargetBackend.JVM_IR
                )
                model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin", targetBackend = TargetBackend.JVM_IR)
                model(
                    "loadJava/compiledKotlinWithStdlib",
                    testMethod = "doTestCompiledKotlinWithStdlib",
                    targetBackend = TargetBackend.JVM_IR
                )
                model(
                    "loadJava/javaAgainstKotlin",
                    extension = "txt",
                    testMethod = "doTestJavaAgainstKotlin",
                    targetBackend = TargetBackend.JVM_IR
                )
                model(
                    "loadJava/kotlinAgainstCompiledJavaWithKotlin",
                    extension = "kt",
                    testMethod = "doTestKotlinAgainstCompiledJavaWithKotlin",
                    recursive = false,
                    targetBackend = TargetBackend.JVM_IR
                )
                model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrSteppingTest>(useJunit4 = true) {
                model("debug/stepping", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrLocalVariableTest>(useJunit4 = true) {
                model("debug/localVariables", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrAsmLikeInstructionListingTest> {
                model("codegen/asmLike", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrScriptCodegenTest> {
                model("codegen/script", extension = "kts", targetBackend = TargetBackend.JVM_IR)
            }
        }

        testGroup("compiler/fir/raw-fir/psi2fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractRawFirBuilderTestCase> {
                model("rawBuilder", testMethod = "doRawFirTest")
            }

            testClass<AbstractRawFirBuilderLazyBodiesTestCase> {
                model("rawBuilder", testMethod = "doRawFirTest")
            }

            testClass<AbstractRawFirBuilderSourceElementMappingTestCase> {
                model("sourceElementMapping", testMethod = "doRawFirTest")
            }
        }

        testGroup("compiler/fir/raw-fir/psi2fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractPartialRawFirBuilderTestCase> {
                model("partialRawBuilder", testMethod = "doRawFirTest")
            }
        }

        testGroup("compiler/fir/raw-fir/light-tree2fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractLightTree2FirConverterTestCase> {
                model("rawBuilder")
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractLazyBodyIsNotTouchedTilContractsPhaseTest> {
                model("resolve", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/testData") {
            testClass<AbstractFirLoadCompiledKotlin> {
                model("loadJava/compiledKotlin", extension = "kt")
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/testData") {
            testClass<AbstractFirTypeEnhancementTest> {
                model("loadJava/compiledJava", extension = "java")
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractOwnFirTypeEnhancementTest> {
                model("enhancement", extension = "java")
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractFirOldFrontendLightClassesTest> {
                model("lightClasses")
            }
        }
    }
}
