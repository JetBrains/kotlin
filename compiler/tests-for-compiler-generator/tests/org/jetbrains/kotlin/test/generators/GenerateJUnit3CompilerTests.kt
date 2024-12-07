/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.asJava.AbstractCompilerLightClassTest
import org.jetbrains.kotlin.cfg.AbstractControlFlowTest
import org.jetbrains.kotlin.cfg.AbstractDataFlowTest
import org.jetbrains.kotlin.cfg.AbstractPseudoValueTest
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.fir.*
import org.jetbrains.kotlin.codegen.ir.*
import org.jetbrains.kotlin.codegen.ir.AbstractIrWriteSignatureTest
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderLazyBodiesByAstTest
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderLazyBodiesByStubTest
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderSourceElementMappingTestCase
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.java.AbstractFirOldFrontendLightClassesTest
import org.jetbrains.kotlin.fir.java.AbstractFirTypeEnhancementTest
import org.jetbrains.kotlin.fir.java.AbstractOwnFirTypeEnhancementTest
import org.jetbrains.kotlin.fir.lightTree.AbstractLightTree2FirConverterTestCase
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME
import org.jetbrains.kotlin.integration.AbstractAntTaskTest
import org.jetbrains.kotlin.jvm.compiler.*
import org.jetbrains.kotlin.jvm.compiler.fir.AbstractFirLightTreeCompileJavaAgainstKotlinTest
import org.jetbrains.kotlin.jvm.compiler.fir.AbstractFirPsiCompileJavaAgainstKotlinTest
import org.jetbrains.kotlin.jvm.compiler.ir.*
import org.jetbrains.kotlin.jvm.compiler.javac.AbstractLoadJavaUsingJavacTest
import org.jetbrains.kotlin.lexer.kdoc.AbstractKDocLexerTest
import org.jetbrains.kotlin.lexer.kotlin.AbstractKotlinLexerTest
import org.jetbrains.kotlin.modules.xml.AbstractModuleXmlParserTest
import org.jetbrains.kotlin.multiplatform.AbstractMultiPlatformIntegrationTest
import org.jetbrains.kotlin.parsing.AbstractParsingTest
import org.jetbrains.kotlin.repl.AbstractReplInterpreterTest
import org.jetbrains.kotlin.resolve.AbstractResolveTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedCallsTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedConstructorDelegationCallsTests
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.jetbrains.kotlin.types.AbstractTypeBindingTest

fun generateJUnit3CompilerTests(args: Array<String>, mainClassName: String?) {
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN

    generateTestGroupSuite(args, mainClassName) {
        testGroup("compiler/tests-integration/tests-gen", "compiler/testData") {
            testClass<AbstractMultiPlatformIntegrationTest> {
                model("multiplatform", extension = null, recursive = true, excludeParentDirs = true)
            }

            testClass<AbstractIrCustomScriptCodegenTest> {
                model("codegen/customScript", pattern = "^(.*)$", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractReplInterpreterTest> {
                model("repl", extension = "repl")
            }

            testClass<AbstractCliTest> {
                model("cli/jvm/readingConfigFromEnvironment", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/plugins", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/hmpp", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/sourceFilesAndDirectories", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/js", extension = "args", testMethod = "doJsTest", recursive = false)
                model("cli/metadata", extension = "args", testMethod = "doMetadataTest", recursive = false)
            }

            testClass<AbstractAntTaskTest> {
                model("integration/ant/jvm", extension = null, recursive = false)
            }
        }

        testGroup("compiler/tests-gen", "compiler/testData") {
            testClass<AbstractResolveTest> {
                model("resolve", extension = "resolve")
            }

            testClass<AbstractResolvedCallsTest> {
                model("resolvedCalls", excludeDirs = listOf("enhancedSignatures"))
            }

            testClass<AbstractResolvedConstructorDelegationCallsTests> {
                model("resolveConstructorDelegationCalls")
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
                    targetBackend = TargetBackend.JVM_IR,
                    skipIgnored = true,
                    excludeDirs = listOf(
                        "ranges/stepped",
                        "compileKotlinAgainstKotlin",
                        "testsWithJava9",
                        "testsWithJava15",
                        "testsWithJava17",
                        "multiplatform/k2",
                    )
                )
            }

            testClass<AbstractKaptModeBytecodeShapeTest> {
                model("codegen/kapt", targetBackend = TargetBackend.JVM_IR)
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

            testClass<AbstractLoadJava17Test> {
                model("loadJava17", extension = "java", testMethod = "doTestCompiledJava", testClassName = "CompiledJava")
                model("loadJava17", extension = "java", testMethod = "doTestSourceJava", testClassName = "SourceJava")
            }

            testClass<AbstractLoadJava17WithPsiClassReadingTest> {
                model("loadJava17", extension = "java", testMethod = "doTestCompiledJava")
            }

            testClass<AbstractModuleXmlParserTest> {
                model("modules.xml", extension = "xml")
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

            testClass<AbstractCompilerLightClassTest> {
                model(
                    "asJava/lightClasses/lightClassByFqName",
                    excludeDirs = listOf("local", "ideRegression"),
                    pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME,
                )
            }

            testClass<AbstractTypeBindingTest> {
                model("type/binding")
            }

            testClass<AbstractKDocLexerTest> {
                model("lexer/kdoc")
            }

            testClass<AbstractKotlinLexerTest> {
                model("lexer/kotlin")
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

            testClass<AbstractFirLightTreeCompileJavaAgainstKotlinTest> {
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

            testClass<AbstractFirPsiCompileJavaAgainstKotlinTest> {
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


            testClass<AbstractIrCompileKotlinWithJavacIntegrationTest> {
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

            testClass<AbstractIrCheckLocalVariablesTableTest> {
                model("checkLocalVariablesTable", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrWriteFlagsTest> {
                model("writeFlags", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrWriteSignatureTest> {
                model("writeSignature", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractFirPsiCheckLocalVariablesTableTest> {
                model("checkLocalVariablesTable", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractFirPsiWriteFlagsTest> {
                model("writeFlags", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractFirPsiWriteSignatureTest> {
                model("writeSignature", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractFirLightTreeWriteFlagsTest> {
                model("writeFlags", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractFirLightTreeCheckLocalVariablesTableTest> {
                model("checkLocalVariablesTable", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractFirLightTreeWriteSignatureTest> {
                model("writeSignature", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrScriptCodegenTest> {
                model("codegen/script", extension = "kts", targetBackend = TargetBackend.JVM_IR, excludedPattern = excludedCustomTestdataPattern)
            }
        }

        testGroup("compiler/fir/raw-fir/psi2fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractRawFirBuilderTestCase> {
                model("rawBuilder", testMethod = "doRawFirTest", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractRawFirBuilderLazyBodiesByAstTest> {
                model("rawBuilder", testMethod = "doRawFirTest", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractRawFirBuilderLazyBodiesByStubTest> {
                model("rawBuilder", testMethod = "doRawFirTest", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractRawFirBuilderSourceElementMappingTestCase> {
                model("sourceElementMapping", testMethod = "doRawFirTest")
            }
        }

        testGroup("compiler/fir/raw-fir/light-tree2fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractLightTree2FirConverterTestCase> {
                model("rawBuilder")
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
