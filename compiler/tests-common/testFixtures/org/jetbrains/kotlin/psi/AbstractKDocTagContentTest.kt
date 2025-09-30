package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class AbstractKDocTagContentTest : KtParsingTestCase(
    "",
    "kt",
    KotlinParserDefinition()
) {

    override fun getTestDataPath(): String = KtTestUtil.getHomeDirectory()

    fun doTest(filePath: String) {
        val fileText = loadFile(filePath)
        val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileText) as KtFile

        val actual = buildString {
            ktFile.acceptChildren(
                object : KtTreeVisitorVoid() {
                    override fun visitElement(element: PsiElement) {
                        element.acceptChildren(this)

                        if (element !is KDocTag) {
                            return
                        }
                        appendLine("ORIGINAL KDOC:")
                        appendLine(element.text)
                        appendLine("CONTENT:")
                        appendLine(element.getContent())
                        appendLine()
                    }
                }
            )
        }


        val outputFilePath = FileUtil.getNameWithoutExtension(filePath) + ".tagContent.txt"
        TestDataAssertions.assertEqualsToFile(File(outputFilePath), actual)
    }
}