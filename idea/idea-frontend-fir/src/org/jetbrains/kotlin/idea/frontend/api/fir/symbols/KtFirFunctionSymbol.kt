/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirMemberFunctionSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtCommonSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirFunctionSymbol(
    fir: FirSimpleFunction,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtFunctionSymbol(), KtFirSymbol<FirSimpleFunction> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { it.findPsi(fir.session) }
    override val name: Name get() = firRef.withFir { it.name }
    override val type: KtType by firRef.withFirAndCache(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { fir -> builder.buildKtType(fir.returnTypeRef) }
    override val valueParameters: List<KtFirFunctionValueParameterSymbol> by firRef.withFirAndCache { fir ->
        fir.valueParameters.map { valueParameter ->
            check(valueParameter is FirValueParameterImpl)
            builder.buildParameterSymbol(valueParameter)
        }
    }
    override val typeParameters by firRef.withFirAndCache { fir ->
        fir.typeParameters.map { typeParameter ->
            builder.buildTypeParameterSymbol(typeParameter.symbol.fir)
        }
    }

    override val isSuspend: Boolean get() = firRef.withFir { it.isSuspend }
    override val receiverType: KtType? by firRef.withFirAndCache(FirResolvePhase.TYPES) { fir -> fir.receiverTypeRef?.let(builder::buildKtType) }
    override val isOperator: Boolean get() = firRef.withFir { it.isOperator }
    override val isExtension: Boolean get() = firRef.withFir { it.receiverTypeRef != null }
    override val callableIdIfNonLocal: FqName?
        get() = firRef.withFir { fir ->
            fir.symbol.callableId.takeUnless { fir.isLocal }?.asFqNameForDebugInfo()
        }
    override val symbolKind: KtSymbolKind
        get() = firRef.withFir { fir ->
            when {
                fir.isLocal -> KtSymbolKind.LOCAL
                fir.containingClass()?.classId == null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.MEMBER
            }
        }
    override val modality: KtCommonSymbolModality get() = getModality()

    override fun createPointer(): KtSymbolPointer<KtFunctionSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        return when (symbolKind) {
            KtSymbolKind.TOP_LEVEL -> TODO("Creating symbol for top level fun is not supported yet")
            KtSymbolKind.MEMBER -> KtFirMemberFunctionSymbolPointer(
                firRef.withFir { it.containingClass()?.classId ?: error("ClassId should not be null for member function") },
                firRef.withFir { it.createSignature() }
            )
            KtSymbolKind.NON_PROPERTY_PARAMETER -> error("KtFunction could not be a parameter")
            KtSymbolKind.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableIdIfNonLocal?.asString() ?: name.asString()
            )
        }
    }
}
