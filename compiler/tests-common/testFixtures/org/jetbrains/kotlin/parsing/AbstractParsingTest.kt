/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.parsing

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.TestsCompilerError
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.psi.IfNotParsed
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertSameLinesWithFile
import org.jetbrains.kotlin.utils.rethrow
import org.junit.jupiter.api.assertNotNull
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Paths

@Suppress("UnstableApiUsage")
abstract class AbstractParsingTest : KtParsingTestCase(".", "kt") {
    protected open fun runTest(filePath: String) {
        doBaseTest(filePath, KtNodeTypes.KT_FILE, null)
    }

    protected fun doParsingTest(filePath: String, contentFilter: (String) -> String) {
        doBaseTest(filePath, KtNodeTypes.KT_FILE, contentFilter)
    }

    private fun doBaseTest(filePath: String, fileType: IElementType, contentFilter: ((String) -> String)?) {
        try {
            doBaseTestImpl(filePath, fileType, contentFilter)
        } catch (e: Exception) {
            throw rethrow(e)
        }
    }

    @Throws(Exception::class)
    private fun doBaseTestImpl(filePath: String, fileType: IElementType, contentFilter: ((String) -> String)?) {
        val filePath = ForTestCompileRuntime.transformTestDataPath(filePath).absolutePath
        val fileContent: String?
        if (Paths.get(filePath).isAbsolute()) {
            fileContent = FileUtil.loadFile(File(filePath), CharsetToolkit.UTF8, true).trim { it <= ' ' }
        } else {
            fileContent = loadFile(filePath)
        }

        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath))

        try {
            myFile = createFile(filePath, fileType, (if (contentFilter != null) contentFilter.invoke(fileContent) else fileContent))
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

    private fun createFile(filePath: String, fileType: IElementType, fileContent: String): PsiFile {
        val psiFactory = KtPsiFactory(myProject)

        return if (fileType === KtNodeTypes.EXPRESSION_CODE_FRAGMENT) {
            psiFactory.createExpressionCodeFragment(fileContent, null)
        } else if (fileType === KtNodeTypes.BLOCK_CODE_FRAGMENT) {
            psiFactory.createBlockCodeFragment(fileContent, null)
        } else {
            createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileContent)
        }
    }

    companion object {
        @Throws(Throwable::class)
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
}
