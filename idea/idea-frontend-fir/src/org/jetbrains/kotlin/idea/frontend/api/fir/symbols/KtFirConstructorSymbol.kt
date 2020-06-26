/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion

internal class KtFirConstructorSymbol(
    fir: FirConstructor,
    override val token: ValidityOwner,
    private val builder: KtSymbolByFirBuilder
) : KtConstructorSymbol(), KtFirSymbol<FirConstructor> {
    override val fir: FirConstructor by weakRef(fir)
    override val psi: PsiElement? by cached { fir.findPsi(fir.session) }

    override val type: KtType by cached { builder.buildKtType(fir.returnTypeRef) }
    override val valueParameters: List<KtConstructorParameterSymbol> by cached {
        fir.valueParameters.map { valueParameter ->
            check(valueParameter is FirValueParameterImpl)
            builder.buildFirConstructorParameter(valueParameter)
        }
    }

    override val isPrimary: Boolean get() = withValidityAssertion { fir.isPrimary }
    override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    override val owner: KtClassOrObjectSymbol by cached {
        val session = fir.session
        val classId = fir.symbol.callableId.classId ?: error("ClassID should present for constructor")
        val firClass = session.firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir
            ?: error("Class with id $classId id was not found")
        check(firClass is FirRegularClass) { "Owner class for constructor should be FirRegularClass, but ${firClass::class} was met" }
        builder.buildClassSymbol(firClass)
    }
}