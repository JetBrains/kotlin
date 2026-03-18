/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.dsl.junit4

import org.jetbrains.kotlin.generators.AbstractTestGenerator
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.Printer
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

object TestGeneratorForJUnit4 : AbstractTestGenerator() {
    override fun generateAndSave(
        testClass: TestGroup.TestClass,
        dryRun: Boolean,
        allowGenerationOnTeamCity: Boolean,
        mainClassName: String?,
    ): GenerationResult {
        val generatorInstance = TestGeneratorForJUnit4Instance(
            testClass.baseDir,
            testClass.suiteTestClassName,
            testClass.baseTestClassName,
            testClass.testModels,
            mainClassName,
        )
        return generatorInstance.generateAndSave(dryRun, allowGenerationOnTeamCity)
    }
}

private class TestGeneratorForJUnit4Instance(
    baseDir: String,
    suiteTestClassFqName: String,
    baseTestClassFqName: String,
    private val testClassModels: Collection<TestClassModel>,
    private val mainClassName: String?
) {
    companion object {
        private val JUNIT3_RUNNER = Class.forName("org.jetbrains.kotlin.test.JUnit3RunnerWithInners")

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

    @Throws(IOException::class)
    fun generateAndSave(dryRun: Boolean, allowGenerationOnTeamCity: Boolean): AbstractTestGenerator.GenerationResult {
        val generatedCode = generate()

        val testSourceFile = File(testSourceFilePath)
        val changed = if (!dryRun) {
            GeneratorsFileUtil.writeFileIfContentChanged(
                testSourceFile,
                generatedCode,
                logNotChanged = false,
                forbidGenerationOnTeamcity = !allowGenerationOnTeamCity
            )
        } else {
            GeneratorsFileUtil.isFileContentChangedIgnoringLineSeparators(testSourceFile, generatedCode)
        }
        return AbstractTestGenerator.GenerationResult(changed, testSourceFilePath)
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
        p.println("import ", JUNIT3_RUNNER.canonicalName, ";")
        p.println("import org.jetbrains.kotlin.test.KotlinTestUtils;")
        p.println("import org.jetbrains.kotlin.test.util.KtTestUtil;")

        for (clazz in testClassModels.flatMapTo(mutableSetOf()) { classModel -> classModel.imports }) {
            p.println("import ${clazz.canonicalName};")
        }

        if (suiteClassPackage != baseTestClassPackage) {
            p.println("import $baseTestClassPackage.$baseTestClassName;")
        }

        p.println("import " + TestMetadata::class.java.canonicalName + ";")
        p.println("import " + RunWith::class.java.canonicalName + ";")
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

                override val methods: Collection<MethodModel<*>>
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

        p.println("@RunWith(${JUNIT3_RUNNER.simpleName}.class)")

        p.println("public " + staticModifier + "class ", testClassModel.name, " extends ", baseTestClassName, " {")
        p.pushIndent()

        val testMethods = testClassModel.methods
        val innerTestClasses = testClassModel.innerTestClasses

        var first = true

        for (methodModel in testMethods) {
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

    private fun generateTestMethod(p: Printer, methodModel: MethodModel<*>) {
        generateMetadata(p, methodModel)
        methodModel.generateSignature(p)
        p.printWithNoIndent(" {")
        p.println()

        p.pushIndent()

        methodModel.generateBody(p)

        p.popIndent()
        p.println("}")
    }
}
