/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.TestGenerator
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.impl.SimpleTestClassModelTestAllFilesPresentMethodGenerator
import org.jetbrains.kotlin.generators.impl.SimpleTestMethodGenerator
import org.jetbrains.kotlin.generators.impl.SingleClassTestModelAllFilesPresentedMethodGenerator
import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.Printer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.util.*

private const val TEST_GENERATOR_NAME = "GenerateNewCompilerTests.kt"

private val METHOD_GENERATORS = listOf(
    SimpleTestClassModelTestAllFilesPresentMethodGenerator,
    SimpleTestMethodGenerator,
    SingleClassTestModelAllFilesPresentedMethodGenerator
)

object NewTestGeneratorImpl : TestGenerator(METHOD_GENERATORS) {
    private val GENERATED_FILES = HashSet<String>()

    private fun Printer.generateMetadata(testDataSource: TestEntityModel) {
        val dataString = testDataSource.dataString
        if (dataString != null) {
            println("@TestMetadata(\"", dataString, "\")")
        }
    }

    private fun Printer.generateTestAnnotation() {
        println("@Test")
    }

    private fun Printer.generateNestedAnnotation(isNested: Boolean) {
        if (isNested) {
            println("@Nested")
        }
    }

    private fun Printer.generateTestDataPath(testClassModel: TestClassModel) {
        val dataPathRoot = testClassModel.dataPathRoot
        if (dataPathRoot != null) {
            println("@TestDataPath(\"", dataPathRoot, "\")")
        }
    }

    private fun Printer.generateParameterAnnotations(testClassModel: TestClassModel) {
        for (annotationModel in testClassModel.annotations) {
            annotationModel.generate(this)
            println()
        }
    }

    private fun Printer.generateSuppressAllWarnings() {
        println("@SuppressWarnings(\"all\")")
    }

    override fun generateAndSave(testClass: TestGroup.TestClass, dryRun: Boolean): GenerationResult {
        val generatorInstance = TestGeneratorInstance(
            testClass.baseDir,
            testClass.suiteTestClassName,
            testClass.baseTestClassName,
            testClass.testModels,
            methodGenerators
        )
        return generatorInstance.generateAndSave(dryRun)
    }

    private class TestGeneratorInstance(
        baseDir: String,
        suiteTestClassFqName: String,
        baseTestClassFqName: String,
        private val testClassModels: Collection<TestClassModel>,
        private val methodGenerators: Map<MethodModel.Kind, MethodGenerator<*>>
    ) {
        private val baseTestClassPackage: String = baseTestClassFqName.substringBeforeLast('.', "")
        private val baseTestClassName: String = baseTestClassFqName.substringAfterLast('.', baseTestClassFqName)
        private val suiteClassPackage: String = suiteTestClassFqName.substringBeforeLast('.', baseTestClassPackage)
        private val suiteClassName: String = suiteTestClassFqName.substringAfterLast('.', suiteTestClassFqName)
        private val testSourceFilePath: String = baseDir + "/" + this.suiteClassPackage.replace(".", "/") + "/" + this.suiteClassName + ".java"

        init {
            if (!GENERATED_FILES.add(testSourceFilePath)) {
                throw IllegalArgumentException("Same test file already generated in current session: $testSourceFilePath")
            }
        }

        @Throws(IOException::class)
        fun generateAndSave(dryRun: Boolean): GenerationResult {
            val generatedCode = generate()

            val testSourceFile = File(testSourceFilePath)
            val changed =
                GeneratorsFileUtil.isFileContentChangedIgnoringLineSeparators(testSourceFile, generatedCode)
            if (!dryRun) {
                GeneratorsFileUtil.writeFileIfContentChanged(testSourceFile, generatedCode, false)
            }
            return GenerationResult(changed, testSourceFilePath)
        }

        private fun generate(): String {
            val out = StringBuilder()
            val p = Printer(out)

            val copyright = File("license/COPYRIGHT_HEADER.txt").readText()
            p.println(copyright)
            p.println()
            p.println("package $suiteClassPackage;")
            p.println()
            p.println("import com.intellij.testFramework.TestDataPath;")
            p.println("import ${KtTestUtil::class.java.canonicalName};")

            for (clazz in testClassModels.flatMapTo(mutableSetOf()) { classModel -> classModel.imports }) {
                p.println("import ${clazz.name};")
            }

            if (suiteClassPackage != baseTestClassPackage) {
                p.println("import $baseTestClassPackage.$baseTestClassName;")
            }

            p.println("import ${TestMetadata::class.java.canonicalName};")
            p.println("import ${Nested::class.java.canonicalName};")
            p.println("import ${Test::class.java.canonicalName};")
            p.println()
            p.println("import java.io.File;")
            p.println("import java.util.regex.Pattern;")
            p.println()
            p.println("/** This class is generated by {@link ", TEST_GENERATOR_NAME, "}. DO NOT MODIFY MANUALLY */")

            p.generateSuppressAllWarnings()

            val model: TestClassModel
            if (testClassModels.size == 1) {
                model = object : DelegatingTestClassModel(testClassModels.single()) {
                    override val name: String
                        get() = suiteClassName
                }
            } else {
                model = object : TestClassModel() {
                    override val innerTestClasses: Collection<TestClassModel>
                        get() = testClassModels

                    override val methods: Collection<MethodModel>
                        get() = emptyList()

                    override val isEmpty: Boolean
                        get() = false

                    override val name: String
                        get() = suiteClassName

                    override val dataString: String?
                        get() = null

                    override val dataPathRoot: String?
                        get() = null

                    override val annotations: Collection<AnnotationModel>
                        get() = emptyList()

                    override val imports: Set<Class<*>>
                        get() = super.imports
                }
            }

            generateTestClass(p, model, false)
            return out.toString()
        }

        private fun generateTestClass(p: Printer, testClassModel: TestClassModel, isNested: Boolean) {
            p.generateNestedAnnotation(isNested)
            p.generateMetadata(testClassModel)
            p.generateTestDataPath(testClassModel)
            p.generateParameterAnnotations(testClassModel)

            val extendsClause = if (!isNested) " extends $baseTestClassName" else ""

            p.println("public class ${testClassModel.name}$extendsClause {")
            p.pushIndent()

            val testMethods = testClassModel.methods
            val innerTestClasses = testClassModel.innerTestClasses

            var first = true

            for (methodModel in testMethods) {
                if (methodModel is RunTestMethodModel) continue
                if (!methodModel.shouldBeGenerated()) continue

                if (first) {
                    first = false
                } else {
                    p.println()
                }

                generateTestMethod(p, methodModel)
            }

            for (innerTestClass in innerTestClasses) {
                if (!innerTestClass.isEmpty) {
                    if (first) {
                        first = false
                    } else {
                        p.println()
                    }

                    generateTestClass(p, innerTestClass, true)
                }
            }

            p.popIndent()
            p.println("}")
        }

        private fun generateTestMethod(p: Printer, methodModel: MethodModel) {
            val generator = methodGenerators.getValue(methodModel.kind)

            p.generateTestAnnotation()
            p.generateMetadata(methodModel)
            generator.hackyGenerateSignature(methodModel, p)
            p.printWithNoIndent(" {")
            p.println()

            p.pushIndent()

            generator.hackyGenerateBody(methodModel, p)

            p.popIndent()
            p.println("}")
        }

        private fun <T : MethodModel> MethodGenerator<T>.hackyGenerateBody(method: MethodModel, p: Printer) {
            @Suppress("UNCHECKED_CAST")
            generateBody(method as T, p)
        }

        private fun <T : MethodModel> MethodGenerator<T>.hackyGenerateSignature(method: MethodModel, p: Printer) {
            @Suppress("UNCHECKED_CAST")
            generateSignature(method as T, p)
        }
    }
}
