/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.TestGenerator
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.Printer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import java.io.File
import java.io.IOException

private val METHOD_GENERATORS = listOf(
    RunTestMethodGenerator,
    SimpleTestClassModelTestAllFilesPresentMethodGenerator,
    SimpleTestMethodGenerator,
    SingleClassTestModelAllFilesPresentedMethodGenerator,
    TransformingTestMethodGenerator,
)

object TestGeneratorImpl : TestGenerator(METHOD_GENERATORS) {
    override fun generateAndSave(testClass: TestGroup.TestClass, dryRun: Boolean, mainClassName: String?): GenerationResult {
        val generatorInstance = TestGeneratorImplInstance(
            testClass.baseDir,
            testClass.suiteTestClassName,
            testClass.baseTestClassName,
            testClass.testModels,
            testClass.useJunit4,
            methodGenerators,
            mainClassName,
        )
        return generatorInstance.generateAndSave(dryRun)
    }
}

private class TestGeneratorImplInstance(
    baseDir: String,
    suiteTestClassFqName: String,
    baseTestClassFqName: String,
    private val testClassModels: Collection<TestClassModel>,
    private val useJunit4: Boolean,
    private val methodGenerators: Map<MethodModel.Kind, MethodGenerator<*>>,
    private val mainClassName: String?
) {
    companion object {
        private val GENERATED_FILES = HashSet<String>()
        private val RUNNER = JUnit3RunnerWithInners::class.java
        private val JUNIT4_RUNNER = BlockJUnit4ClassRunner::class.java

        private fun generateMetadata(p: Printer, testDataSource: TestEntityModel) {
            val dataString = testDataSource.dataString
            if (dataString != null) {
                p.println("@TestMetadata(\"", dataString, "\")")
            }
        }

        private fun generateTestDataPath(p: Printer, testClassModel: TestClassModel) {
            val dataPathRoot = testClassModel.dataPathRoot
            if (dataPathRoot != null) {
                p.println("@TestDataPath(\"", dataPathRoot, "\")")
            }
        }

        private fun generateParameterAnnotations(p: Printer, testClassModel: TestClassModel) {
            for (annotationModel in testClassModel.annotations) {
                annotationModel.generate(p)
                p.println()
            }
        }

        private fun generateSuppressAllWarnings(p: Printer) {
            p.println("@SuppressWarnings(\"all\")")
        }
    }

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
    fun generateAndSave(dryRun: Boolean): TestGenerator.GenerationResult {
        val generatedCode = generate()

        val testSourceFile = File(testSourceFilePath)
        val changed =
            GeneratorsFileUtil.isFileContentChangedIgnoringLineSeparators(testSourceFile, generatedCode)
        if (!dryRun) {
            GeneratorsFileUtil.writeFileIfContentChanged(testSourceFile, generatedCode, false)
        }
        return TestGenerator.GenerationResult(changed, testSourceFilePath)
    }

    private fun generate(): String {
        val out = StringBuilder()
        val p = Printer(out, indentUnit = Printer.TWO_SPACE_INDENT)

        val copyright = File("license/COPYRIGHT_HEADER.txt").readText()
        p.println(copyright)
        p.println()
        p.println("package ", suiteClassPackage, ";")
        p.println()
        p.println("import com.intellij.testFramework.TestDataPath;")
        if (!useJunit4) {
            p.println("import ", RUNNER.canonicalName, ";")
        }
        p.println("import " + KotlinTestUtils::class.java.canonicalName + ";")
        p.println("import " + KtTestUtil::class.java.canonicalName + ";")

        for (clazz in testClassModels.flatMapTo(mutableSetOf()) { classModel -> classModel.imports }) {
            val realName = when (clazz) {
                TransformingTestMethodModel.TransformerFunctionsClassPlaceHolder::class.java ->
                    "org.jetbrains.kotlin.test.utils.TransformersFunctions"
                else -> clazz.canonicalName
            }
            p.println("import $realName;")
        }

        if (suiteClassPackage != baseTestClassPackage) {
            p.println("import $baseTestClassPackage.$baseTestClassName;")
        }

        p.println("import " + TestMetadata::class.java.canonicalName + ";")
        p.println("import " + RunWith::class.java.canonicalName + ";")
        if (useJunit4) {
            p.println("import " + BlockJUnit4ClassRunner::class.java.canonicalName + ";")
            p.println("import " + Test::class.java.canonicalName + ";")
        }
        p.println()
        p.println("import java.io.File;")
        p.println("import java.util.regex.Pattern;")
        p.println()
        p.println("/** This class is generated by {@link ", mainClassName, "}. DO NOT MODIFY MANUALLY */")

        generateSuppressAllWarnings(p)

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

                override val tags: List<String>
                    get() = emptyList()
            }
        }

        generateTestClass(p, model, false)
        return out.toString()
    }

    private fun generateTestClass(p: Printer, testClassModel: TestClassModel, isStatic: Boolean) {
        val staticModifier = if (isStatic) "static " else ""

        generateMetadata(p, testClassModel)
        generateTestDataPath(p, testClassModel)
        generateParameterAnnotations(p, testClassModel)

        p.println("@RunWith(", if (useJunit4) JUNIT4_RUNNER.simpleName else RUNNER.simpleName, ".class)")

        p.println("public " + staticModifier + "class ", testClassModel.name, " extends ", baseTestClassName, " {")
        p.pushIndent()

        val testMethods = testClassModel.methods
        val innerTestClasses = testClassModel.innerTestClasses

        var first = true

        for (methodModel in testMethods) {
            if (!methodModel.shouldBeGenerated()) continue

            if (first) {
                first = false
            } else {
                p.println()
            }

            generateTestMethod(p, methodModel, useJunit4)
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

    private fun generateTestMethod(p: Printer, methodModel: MethodModel, useJunit4: Boolean) {
        if (useJunit4 && (methodModel !is RunTestMethodModel)) {
            p.println("@Test")
        }

        val generator = methodGenerators.getValue(methodModel.kind)

        generateMetadata(p, methodModel)
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
