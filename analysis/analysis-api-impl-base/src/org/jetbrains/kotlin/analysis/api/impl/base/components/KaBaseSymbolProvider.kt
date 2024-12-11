/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProvider
import org.jetbrains.kotlin.analysis.api.utils.errors.withKaModuleEntry
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments
import org.jetbrains.kotlin.utils.exceptions.buildAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

@KaImplementationDetail
abstract class KaBaseSymbolProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaSymbolProvider {
    override val KtDeclaration.symbol: KaDeclarationSymbol
        get() = withValidityAssertion {
            when (this) {
                is KtParameter -> symbol
                is KtNamedFunction -> symbol
                is KtConstructor<*> -> symbol
                is KtTypeParameter -> symbol
                is KtTypeAlias -> symbol
                is KtEnumEntry -> symbol
                is KtFunctionLiteral -> symbol
                is KtProperty -> symbol
                is KtObjectDeclaration -> symbol
                is KtClassOrObject -> classSymbol!!
                is KtPropertyAccessor -> symbol
                is KtClassInitializer -> symbol
                is KtDestructuringDeclarationEntry -> symbol
                is KtScript -> symbol
                is KtScriptInitializer -> containingDeclaration.symbol
                is KtDestructuringDeclaration -> symbol
                else -> error("Cannot build symbol for ${this::class}")
            }
        }

    protected inline fun <T : PsiElement, R> T.createPsiBasedSymbolWithValidityAssertion(builder: () -> R): R = withValidityAssertion {
        with(analysisSession) {
            if (!canBeAnalysed() && !Registry.`is`("kotlin.analysis.unrelatedSymbolCreation.allowed", false)) {
                throw KaBaseIllegalPsiException(this, this@createPsiBasedSymbolWithValidityAssertion)
            }
        }

        builder()
    }

    @KaImplementationDetail
    class KaBaseIllegalPsiException(session: KaSession, psi: PsiElement) : KotlinIllegalArgumentExceptionWithAttachments(
        "The element cannot be analyzed in the context of the current session.\n" +
                "The call site should be adjusted according to ${KaSymbolProvider::class.simpleName} KDoc."
    ) {
        init {
            with(session) {
                buildAttachment("info.txt") {
                    withKaModuleEntry("useSiteModule", useSiteModule)

                    val psiModule = getModule(psi)
                    withKaModuleEntry("psiModule", psiModule)

                    runCatching {
                        withPsiEntry("psi", psi)
                    }.exceptionOrNull()?.let {
                        withEntry("psiException", it.stackTraceToString())
                    }
                }
            }
        }
    }
}
