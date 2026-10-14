/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.renderToString
import org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.findClassLike
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.type
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Resolves the reference at the caret and renders the types of all [KtTypeReference]s in the PSI of the resolved declaration,
 * analyzed from the use-site session. The types are computed before the text of the declaration is accessed, so the output
 * shows whether type computation loads decompiled text. Afterwards, the declaration text is printed with a caret line under the
 * start of each [KtTypeReference]: the rendered type, followed by `(use)` if its symbol is the same as the one from the use-site
 * session and `(decl)` otherwise.
 *
 * The main purpose is to cover declarations from other modules, e.g., decompiled PSI of binary library dependencies.
 */
abstract class AbstractTypeReferenceInResolvedDeclarationTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val elementAtCaret = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtResolvable>(mainFile)

        val actual = copyAwareAnalyzeForTest(elementAtCaret) { contextElement ->
            val symbol = contextElement.resolveSuccessfulSymbol() ?: error("The element at the caret should be resolved")
            val declaration = symbol.realPsi as? KtDeclaration ?: error("The resolved symbol should have a declaration PSI: $symbol")

            val renderedTypes = declaration.collectTypeReferences().map { typeReference ->
                typeReference to typeReference.type.render(symbol)
            }

            val isDecompiledTextLoaded = declaration.containingKtFile.isDecompiledTextLoaded()

            buildString {
                appendLine("Compiled: ${declaration.containingKtFile.isCompiled}")
                isDecompiledTextLoaded?.let { appendLine("Decompiled text loaded by type computation: $it") }
                appendLine()
                appendTextWithCarets(declaration, renderedTypes)
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    context(_: KaSession)
    private fun KaType.render(resolvedSymbol: KaSymbol): String {
        val renderedType = if (this is KaErrorType) {
            "$presentableText: $errorMessage"
        } else {
            KaRenderer.default.renderToString(this)
        }

        val origin = when (isSymbolConsistentWith(resolvedSymbol)) {
            true -> "(use)"
            false -> "(decl)"
            null -> ""
        }

        return renderedType + origin
    }

    /**
     * Prints the text of [declaration] and, after each of its lines, a caret line per type reference starting on that line, in
     * offset order. A caret line is `//` padded with spaces so that `^` is under the first character of the type reference.
     * For type references starting in the first two columns, `//` doesn't fit, so their caret lines consist of spaces and `^`.
     */
    private fun StringBuilder.appendTextWithCarets(declaration: KtDeclaration, renderedTypes: List<Pair<KtTypeReference, String>>) {
        val declarationStartOffset = declaration.textRange.startOffset
        val carets = renderedTypes
            .map { [typeReference, renderedType] -> typeReference.textRange.startOffset - declarationStartOffset to renderedType }
            .sortedBy { it.first }

        var caretIndex = 0
        var lineStartOffset = 0
        for (line in declaration.text.lines()) {
            appendLine(line)

            val nextLineStartOffset = lineStartOffset + line.length + 1
            while (caretIndex < carets.size && carets[caretIndex].first < nextLineStartOffset) {
                val [offset, renderedType] = carets[caretIndex++]
                val column = offset - lineStartOffset
                val prefix = if (column >= 2) "//".padEnd(column) else " ".repeat(column)
                appendLine("$prefix^$renderedType")
            }

            lineStartOffset = nextLineStartOffset
        }
    }

    /**
     * Checks that the symbol of [this] type is the same as the one provided by the use-site session:
     * a class found by its class ID, or a type parameter of the [resolvedSymbol].
     */
    context(_: KaSession)
    private fun KaType.isSymbolConsistentWith(resolvedSymbol: KaSymbol): Boolean? = when (this) {
        is KaClassType -> symbol == findClassLike(classId)
        is KaTypeParameterType -> {
            val typeParameters = when (resolvedSymbol) {
                is KaCallableSymbol -> resolvedSymbol.typeParameters
                is KaClassLikeSymbol -> resolvedSymbol.typeParameters
                else -> emptyList()
            }

            symbol in typeParameters
        }

        else -> null
    }

    /**
     * Whether the AST of a compiled file was loaded, i.e., its decompiled text was built.
     */
    private fun KtFile.isDecompiledTextLoaded(): Boolean? = if (isCompiled) (this as PsiFileImpl).treeElement != null else null

    /**
     * Collects type references in pre-order. Compiled declarations are traversed via stubs to not load their decompiled text
     * before the types are computed.
     */
    private fun KtDeclaration.collectTypeReferences(): List<KtTypeReference> = buildList {
        val stub = (this@collectTypeReferences as? StubBasedPsiElementBase<*>)?.stub
        if (stub != null) {
            fun visit(stub: StubElement<*>) {
                (stub.psi as? KtTypeReference)?.let(::add)
                stub.childrenStubs.forEach(::visit)
            }

            visit(stub)
        } else {
            accept(object : KtTreeVisitorVoid() {
                override fun visitTypeReference(typeReference: KtTypeReference) {
                    add(typeReference)
                    super.visitTypeReference(typeReference)
                }
            })
        }
    }
}
