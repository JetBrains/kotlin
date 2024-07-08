/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclarationUtil
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

object TestReferenceResolveResultRenderer {
    private const val UNRESOLVED_REFERENCE_RESULT = "Nothing (Unresolved reference)"

    fun PsiElement.renderResolvedPsi(testServices: TestServices): String {
        val containingFqn = when (this) {
            is PsiMember -> (containingClass ?: this as? PsiClass)?.qualifiedName
            is KtNamedDeclaration -> KtNamedDeclarationUtil.getParentFqName(this)
            else -> testServices.assertions.fail { "Could not get containing class" }
        }
        return """
                Resolved to:
                    (in ${containingFqn}) (psi: ${text.substringBefore('{')})
        """.trimIndent()
    }

    /**
     * Empty [symbols] list equals to unresolved reference.
     */
    fun KaSession.renderResolvedTo(
        symbols: Collection<KaSymbol>,
        renderPsiClassName: Boolean = false,
        renderer: KaDeclarationRenderer = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES,
        additionalInfo: KaSession.(KaSymbol) -> String? = { null }
    ): String {
        if (symbols.isEmpty()) return UNRESOLVED_REFERENCE_RESULT

        val sortedSymbols = symbols.map { renderResolveResult(it, renderPsiClassName, renderer, additionalInfo) }.sorted()
        if (sortedSymbols.size == 1) {
            return sortedSymbols.single()
        }

        return sortedSymbols
            .withIndex()
            .joinToString(separator = "\n") { "${it.index}: ${it.value}" }
    }

    private fun KaSession.renderResolveResult(
        symbol: KaSymbol,
        renderPsiClassName: Boolean,
        renderer: KaDeclarationRenderer,
        additionalInfo: KaSession.(KaSymbol) -> String?
    ): String {
        return buildString {
            symbolContainerFqName(symbol)?.let { fqName ->
                append("(in $fqName) ")
            }
            when (symbol) {
                is KaReceiverParameterSymbol -> {
                    append("extension receiver with type ")
                    append(symbol.returnType.render(renderer.typeRenderer, position = Variance.INVARIANT))
                }
                is KaDeclarationSymbol -> {
                    append(symbol.render(renderer))
                    if (renderPsiClassName) {
                        append(" (psi: ${symbol.psi?.let { it::class.simpleName }})")
                    }
                }
                is KaPackageSymbol -> append("package ${symbol.fqName}")
                else -> error("Unexpected symbol ${symbol::class}")
            }
            additionalInfo(symbol)?.let { append(" [$it]") }
        }
    }

    @Suppress("unused")// KaSession receiver
    private fun KaSession.symbolContainerFqName(symbol: KaSymbol): String? {
        if (symbol is KaPackageSymbol || symbol is KaValueParameterSymbol) return null
        val nonLocalFqName = when (symbol) {
            is KaConstructorSymbol -> symbol.containingClassId?.asSingleFqName()
            is KaCallableSymbol -> symbol.callableId?.asSingleFqName()?.parent()
            is KaClassLikeSymbol -> symbol.classId?.asSingleFqName()?.parent()
            else -> null
        }
        when (nonLocalFqName) {
            null -> Unit
            FqName.ROOT -> return "ROOT"
            else -> return nonLocalFqName.asString()
        }
        val container = symbol.containingDeclaration ?: return null
        val parents = generateSequence(container) { it.containingDeclaration }.toList().asReversed()
        return "<local>: " + parents.joinToString(separator = ".") { it.name?.asString() ?: "<no name>" }
    }
}
