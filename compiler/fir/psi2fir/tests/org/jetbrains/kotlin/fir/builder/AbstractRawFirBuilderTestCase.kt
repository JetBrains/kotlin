/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
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
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import java.io.File
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

abstract class AbstractRawFirBuilderTestCase : KtParsingTestCase(
    "",
    "kt",
    KotlinParserDefinition()
) {
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
        val file = createKtFile(filePath)
        val firFile = file.toFirFile(stubMode = false)
        val firFileDump = StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
        val expectedPath = filePath.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
    }

    protected fun createKtFile(filePath: String): KtFile {
        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath))
        return (createFile(filePath, KtNodeTypes.KT_FILE) as KtFile).apply {
            myFile = this
        }
    }

    protected fun KtFile.toFirFile(stubMode: Boolean): FirFile =
        RawFirBuilder(object : FirSessionBase() {}, stubMode).buildFirFile(this)

    private fun FirElement.traverseChildren(result: MutableSet<FirElement> = hashSetOf()): MutableSet<FirElement> {
        if (!result.add(this)) {
            return result
        }
        propertyLoop@ for (property in this::class.memberProperties) {
            val childElement = property.getter.apply { isAccessible = true }.call(this)

            when (childElement) {
                is FirElement -> childElement.traverseChildren(result)
                is List<*> -> childElement.filterIsInstance<FirElement>().forEach { it.traverseChildren(result) }
                else -> continue@propertyLoop
            }

        }
        return result
    }

    private fun FirFile.visitChildren(): Set<FirElement> =
        ConsistencyVisitor().let {
            this@visitChildren.accept(it)
            it.result
        }

    private fun FirFile.transformChildren(): Set<FirElement> =
        ConsistencyTransformer().let {
            this@transformChildren.transform<FirFile, Unit>(it, Unit)
            it.result
        }

    protected fun FirFile.checkChildren() {
        val children = traverseChildren()
        val visitedChildren = visitChildren()
        children.removeAll(visitedChildren)
        if (children.isNotEmpty()) {
            val element = children.first()
            val elementDump = element.render()
            throw AssertionError("FirElement ${element.javaClass} is not visited: $elementDump")
        }
    }

    protected fun FirFile.checkTransformedChildren() {
        val children = traverseChildren()
        val transformedChildren = transformChildren()
        children.removeAll(transformedChildren)
        if (children.isNotEmpty()) {
            val element = children.first()
            val elementDump = element.render()
            throw AssertionError("FirElement ${element.javaClass} is not transformed: $elementDump")
        }
    }

    private class ConsistencyVisitor : FirVisitorVoid() {
        var result = hashSetOf<FirElement>()

        override fun visitElement(element: FirElement) {
            // NB: types are reused sometimes (e.g. in accessors)
            if (!result.add(element)) {
                if (element !is FirTypeRef) {
                    val elementDump = StringBuilder().also { element.accept(FirRenderer(it)) }.toString()
                    throw AssertionError("FirElement ${element.javaClass} is visited twice: $elementDump")
                }
            } else {
                element.acceptChildren(this)
            }
        }
    }

    private class ConsistencyTransformer : FirTransformer<Unit>() {
        var result = hashSetOf<FirElement>()

        override fun <E : FirElement> transformElement(element: E, data: Unit): CompositeTransformResult<E> {
            if (!result.add(element)) {
                if (element !is FirTypeRef) {
                    val elementDump = StringBuilder().also { element.accept(FirRenderer(it)) }.toString()
                    throw AssertionError("FirElement ${element.javaClass} is visited twice: $elementDump")
                }
            } else {
                element.transformChildren(this, Unit)
            }
            return CompositeTransformResult(element)
        }
    }

    override fun tearDown() {
        super.tearDown()
        FileTypeRegistry.ourInstanceGetter = Getter<FileTypeRegistry> { FileTypeManager.getInstance() }
    }

}