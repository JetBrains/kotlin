/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirConstructorSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId

internal class KtFirConstructorSymbol(
    fir: FirConstructor,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtConstructorSymbol(), KtFirSymbol<FirConstructor> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { it.findPsi(fir.session) }

    override val type: KtType by firRef.withFirAndCache(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { builder.buildKtType(it.returnTypeRef) }
    override val valueParameters: List<KtConstructorParameterSymbol> by firRef.withFirAndCache { fir ->
        fir.valueParameters.map { valueParameter ->
            check(valueParameter is FirValueParameterImpl)
            builder.buildFirConstructorParameter(valueParameter)
        }
    }

    override val isPrimary: Boolean get() = firRef.withFir { it.isPrimary }
    override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    override val ownerClassId: ClassId
        get() = firRef.withFir {
            it.symbol.callableId.classId ?: error("ClassID should present for constructor")
        }

    override val owner: KtClassOrObjectSymbol by firRef.withFirAndCache { fir ->
        val session = fir.session
        val classId = ownerClassId
        val firClass = session.firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir
            ?: error("Class with id $classId id was not found")
        check(firClass is FirRegularClass) { "Owner class for constructor should be FirRegularClass, but ${firClass::class} was met" }
        builder.buildClassSymbol(firClass)
    }

    override fun createPointer(): KtSymbolPointer<KtConstructorSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        if (symbolKind == KtSymbolKind.LOCAL) {
            throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException("${ownerClassId.asString()}.constructor")
        }
        return KtFirConstructorSymbolPointer(ownerClassId, isPrimary, firRef.withFir { it.createSignature() })
    }
}