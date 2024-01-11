/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isNonLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionWithoutNameSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.isExtensionFunctionAnnotationCall
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.addToStdlib.joinToWithBuffer
import java.io.File
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

abstract class AbstractRawFirBuilderTestCase : KtParsingTestCase(
    "",
    "kt",
    KotlinParserDefinition()
) {
    override fun getTestDataPath() = KtTestUtil.getHomeDirectory()

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

    protected open fun doRawFirTest(filePath: String) {
        val file = createKtFile(filePath)
        val firFile = file.toFirFile(BodyBuildingMode.NORMAL)
        val firFileDump = FirRenderer.withDeclarationAttributes().renderElementAsString(firFile)
        val expectedPath = expectedPath(filePath, ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
        checkAnnotationOwners(filePath, firFile)
    }

    protected fun expectedPath(originalPath: String, newExtension: String): String {
        return originalPath.replace(".${myFileExt}", newExtension)
    }

    protected fun checkAnnotationOwners(filePath: String, firFile: FirFile) {
        val expectedPath = expectedPath(filePath, ".annotationOwners.txt")
        val expectedFile = File(expectedPath)
        val annotations = firFile.collectAnnotations()
        if (annotations.isEmpty() && !expectedFile.exists()) {
            return
        }

        val actual = annotations.groupBy(AnnotationWithContext::annotation)
            .entries
            .joinToString(separator = "\n\n") { (annotation, contexts) ->
                buildString {
                    appendLine(annotation.render().trim())
                    append("owner -> ")
                    appendLine(annotation.containingDeclarationSymbol.let {
                        if (it is FirValueParameterSymbol) {
                            "$it from ${it.containingFunctionSymbol}"
                        } else {
                            it
                        }
                    })

                    contexts.joinToWithBuffer(buffer = this, separator = "\n") {
                        append("context -> ")
                        append(it.context)
                    }
                }
            }

        KotlinTestUtils.assertEqualsToFile(expectedFile, actual)
    }

    private fun FirElementWithResolveState.collectAnnotations(): Collection<AnnotationWithContext> {
        val result = mutableListOf<AnnotationWithContext>()
        val contextStack = ContextStack()

        this.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                contextStack.withStack(element) {
                    if (element is FirAnnotationCall) {
                        result += AnnotationWithContext(element, contextStack.dumpContext())
                    }

                    element.acceptChildren(this)
                }
            }
        })

        return result
    }

    private class AnnotationWithContext(val annotation: FirAnnotationCall, val context: String)

    private class ContextStack {
        val stack = mutableListOf<FirDeclaration>()

        inline fun withStack(element: FirElement, action: () -> Unit) {
            if (element !is FirDeclaration) {
                action()
                return
            }

            stack += element
            try {
                action()
            } finally {
                val last = stack.removeLast()
                if (last != element) {
                    error("Stack is corrupted")
                }
            }
        }

        fun dumpContext(): String {
            val reversedStack = stack.asReversed().iterator()
            return buildString {
                var declaration = reversedStack.next()
                append(declaration.symbol)

                while (declaration.shouldAddParentContext() && reversedStack.hasNext()) {
                    declaration = reversedStack.next()
                    append(" from ")
                    append(declaration.symbol)
                }
            }
        }

        private fun FirDeclaration.shouldAddParentContext(): Boolean = symbol is FirFunctionWithoutNameSymbol || !isNonLocal
    }

    protected open fun createKtFile(filePath: String): KtFile {
        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath))
        return (createFile(filePath, KtNodeTypes.KT_FILE) as KtFile).apply {
            myFile = this
        }
    }

    protected fun KtFile.toFirFile(bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL): FirFile {
        val session = FirSessionFactoryHelper.createEmptySession()
        return PsiRawFirBuilder(
            session,
            StubFirScopeProvider,
            bodyBuildingMode = bodyBuildingMode
        ).buildFirFile(this)
    }

    private fun FirElement.traverseChildren(result: MutableSet<FirElement> = hashSetOf()): MutableSet<FirElement> {
        if (!result.add(this)) {
            return result
        }
        for (property in this::class.memberProperties) {
            if (hasNoAcceptAndTransform(this::class.simpleName, property.name)) continue

            when (val childElement = property.getter.apply { isAccessible = true }.call(this)) {
                is FirElement -> childElement.traverseChildren(result)
                is List<*> -> childElement.filterIsInstance<FirElement>().forEach { it.traverseChildren(result) }
                else -> continue
            }

        }
        return result
    }

    private val firImplClassPropertiesWithNoAcceptAndTransform = mapOf(
        "FirResolvedImportImpl" to "delegate",
        "FirErrorTypeRefImpl" to "delegatedTypeRef",
        "FirResolvedTypeRefImpl" to "delegatedTypeRef"
    )

    private fun hasNoAcceptAndTransform(className: String?, propertyName: String): Boolean {
        if (className == null) return false
        return firImplClassPropertiesWithNoAcceptAndTransform[className] == propertyName
    }

    private fun FirFile.visitChildren(): Set<FirElement> {
        val result = HashSet<FirElement>()
        val processor = ConsistencyProcessor(result)
        accept(ConsistencyVisitor(processor))
        return result
    }

    private fun FirFile.transformChildren(): Set<FirElement> {
        val result = HashSet<FirElement>()
        val processor = ConsistencyProcessor(result)
        transform<FirFile, Unit>(ConsistencyTransformer(processor), Unit)
        return result
    }

    protected fun FirFile.checkChildren() {
        val children = traverseChildren()
        val visitedChildren = visitChildren()
        children.removeAll(visitedChildren)
        if (children.isNotEmpty()) {
            val element = children.firstOrNull { it !is FirAnnotationArgumentMapping } ?: return
            val elementDump = element.render()
            throw AssertionError("FirElement ${element.javaClass} is not visited: $elementDump")
        }
    }

    protected fun FirFile.checkTransformedChildren() {
        val children = traverseChildren()
        val transformedChildren = transformChildren()
        children.removeAll(transformedChildren)
        if (children.isNotEmpty()) {
            val element = children.firstOrNull { it !is FirAnnotationArgumentMapping } ?: return
            val elementDump = element.render()
            throw AssertionError("FirElement ${element.javaClass} is not transformed: $elementDump")
        }
    }

    private class ConsistencyVisitor(private val processor: ConsistencyProcessor) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            processor.process(element) { it.acceptChildren(this@ConsistencyVisitor) }
        }
    }

    private class ConsistencyTransformer(private val processor: ConsistencyProcessor) : FirTransformer<Unit>() {
        override fun <E : FirElement> transformElement(element: E, data: Unit): E {
            processor.process(element) { it.transformChildren(this@ConsistencyTransformer, Unit) }
            return element
        }
    }

    private class ConsistencyProcessor(private val result: MutableSet<FirElement>) {
        private var parent: FirElement? = null

        fun process(element: FirElement, processChildren: (FirElement) -> Unit) {
            if (!result.add(element)) {
                throwTwiceVisitingError(element, parent)
            } else {
                val oldParent = parent
                try {
                    parent = element
                    processChildren(element)
                } finally {
                    parent = oldParent
                }
            }
        }
    }
}

private fun throwTwiceVisitingError(element: FirElement, parent: FirElement?) {
    if (element is FirTypeRef || element is FirTypeParameter ||
        element is FirTypeProjection || element is FirValueParameter || element is FirAnnotation || element is FirFunctionTypeParameter ||
        element is FirEmptyContractDescription ||
        element is FirStubReference || element.isExtensionFunctionAnnotation || element is FirEmptyArgumentList ||
        element === FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS ||
        ((parent is FirContractCallBlock || parent is FirContractDescription) && element is FirFunctionCall)
    ) {
        return
    }
    if (element is FirExpression) {
        val psiParent = element.source?.psi?.parent
        if (psiParent is KtPropertyDelegate || psiParent?.parent is KtPropertyDelegate) return
    }

    val elementDump = FirRenderer().renderElementAsString(element)
    throw AssertionError("FirElement ${element.javaClass} is visited twice: $elementDump")
}


private val FirElement.isExtensionFunctionAnnotation: Boolean
    get() = (this as? FirAnnotation)?.isExtensionFunctionAnnotationCall == true
