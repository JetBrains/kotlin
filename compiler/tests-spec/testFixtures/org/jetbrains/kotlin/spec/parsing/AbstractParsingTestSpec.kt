/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.parsing

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.TestExceptionsComparator
import org.jetbrains.kotlin.TestsCompilerError
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.psi.IfNotParsed
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser
import org.jetbrains.kotlin.spec.utils.validators.ParsingTestTypeValidator
import org.jetbrains.kotlin.spec.utils.validators.SpecTestValidationException
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertSameLinesWithFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertNotNull
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Paths

@Suppress("UnstableApiUsage")
abstract class AbstractParsingTestSpec : KtParsingTestCase(".", "kt") {
    fun runTest(filePath: String) {
        val file = ForTestCompileRuntime.transformTestDataPath(filePath)
        val [specTest, testLinkedType] = CommonParser.parseSpecTest(
            file.canonicalPath,
            mapOf("main.kt" to FileUtil.loadFile(file, true))
        )

        println(specTest)

        if (specTest.exception == null) {
            doBaseTest(filePath) { it: String -> CommonParser.testInfoFilter(it) }
        } else {
            TestExceptionsComparator(file).run(specTest.exception) {
                doBaseTest(filePath) { it: String -> CommonParser.testInfoFilter(it) }
            }
        }

        try {
            val psiTestValidator = ParsingTestTypeValidator(myFile, File(filePath), specTest)
            psiTestValidator.validatePathConsistency(testLinkedType)
            psiTestValidator.validateTestType()
        } catch (e: SpecTestValidationException) {
            Assertions.fail(e.description)
        }
    }

    private fun doBaseTest(filePath: String, contentFilter: ((String) -> String)?) {
        val filePath = ForTestCompileRuntime.transformTestDataPath(filePath).absolutePath
        val fileContent = if (Paths.get(filePath).isAbsolute) {
            FileUtil.loadFile(File(filePath), CharsetToolkit.UTF8, true).trim { it <= ' ' }
        } else {
            loadFile(filePath)
        }

        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath))

        try {
            myFile = createFile(filePath, contentFilter?.invoke(fileContent) ?: fileContent)
            myFile.acceptChildren(object : KtVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    element.acceptChildren(this)
                    try {
                        checkPsiGetters(element)
                    } catch (throwable: Throwable) {
                        throw TestsCompilerError(throwable)
                    }
                }
            })
        } catch (throwable: Throwable) {
            throw TestsCompilerError(throwable)
        }

        val actual = toParseTreeText(myFile, skipSpaces = false, printRanges = false).trim { it <= ' ' }
        if (Paths.get(filePath).isAbsolute) {
            assertSameLinesWithFile(filePath.replace("\\.kts?".toRegex(), ".txt"), actual)
        } else {
            doCheckResult(myFullDataPath, filePath.replace("\\.kts?".toRegex(), ".txt"), actual)
        }
    }

    private fun createFile(filePath: String, fileContent: String): PsiFile {
        return createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileContent)
    }

    private fun checkPsiGetters(elem: KtElement) {
        val methods = elem.javaClass.getDeclaredMethods()
        for (method in methods) {
            val methodName = method.name
            if (!methodName.startsWith("get") && !methodName.startsWith("find") ||
                methodName == "getReference" ||
                methodName == "getReferences" ||
                methodName == "getUseScope" ||
                methodName == "getPresentation"
            ) {
                continue
            }

            if (!Modifier.isPublic(method.modifiers)) continue
            if (method.parameterTypes.size > 0) continue

            val declaringClass = method.declaringClass
            if (!declaringClass.getName().startsWith("org.jetbrains.kotlin")) continue

            val result = method.invoke(elem)
            if (result == null) {
                for (annotation in method.declaredAnnotations) {
                    if (annotation is IfNotParsed) {
                        assertNotNull(PsiTreeUtil.findChildOfType(elem, PsiErrorElement::class.java)) {
                            "Incomplete operation in parsed OK test, method $methodName in ${declaringClass.getSimpleName()} returns null. Element text: \n${elem.text}"
                        }
                    }
                }
            }
        }
    }
}
