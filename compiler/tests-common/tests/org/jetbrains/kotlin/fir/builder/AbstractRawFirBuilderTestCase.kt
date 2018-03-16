/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.util.Getter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.cli.common.script.CliScriptDefinitionProvider
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import java.io.File

abstract class AbstractRawFirBuilderTestCase : KtParsingTestCase(
    ".",
    "kt",
    KotlinParserDefinition()
) {
    override fun setUp() {
        super.setUp()
        project.registerService(ScriptDefinitionProvider::class.java, CliScriptDefinitionProvider::class.java)
    }

    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()

    private fun createFile(filePath: String, fileType: IElementType): PsiFile {
        val psiFactory = KtPsiFactory(myProject)
        return when (fileType) {
            KtNodeTypes.EXPRESSION_CODE_FRAGMENT ->
                psiFactory.createExpressionCodeFragment(loadFile(filePath), null)
            KtNodeTypes.BLOCK_CODE_FRAGMENT ->
                psiFactory.createBlockCodeFragment(loadFile(filePath), null)
            else ->
                createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), loadFile(filePath))
        }
    }

    protected fun doRawFirTest(filePath: String) {
        doRawFirTest(filePath, checkNeeded = true)
    }

    protected fun doRawFirTest(filePath: String, checkNeeded: Boolean) {
        val file = createKtFile(filePath)
        val firFile = file.toFirFile()
        if (checkNeeded) {
            val firFileDump = StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
            val expectedPath = filePath.replace(".kt", ".txt")
            KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
        }
    }

    protected fun createKtFile(filePath: String): KtFile {
        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath))
        return (createFile(filePath, KtNodeTypes.KT_FILE) as KtFile).apply {
            myFile = this
        }
    }

    protected fun KtFile.toFirFile(): FirFile =
        RawFirBuilder(object : FirSessionBase() {}).buildFirFile(this)

    override fun tearDown() {
        super.tearDown()
        FileTypeRegistry.ourInstanceGetter = Getter<FileTypeRegistry> { FileTypeManager.getInstance() }
    }

}