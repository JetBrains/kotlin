/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirMemberPropertySymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.convertAnnotation
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.convertConstantExpression
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirPropertySymbol(
    fir: FirProperty,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtPropertySymbol(), KtFirSymbol<FirProperty> {
    init {
        assert(!fir.isLocal)
    }

    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.session) }

    override val isVal: Boolean get() = firRef.withFir { it.isVal }
    override val name: Name get() = firRef.withFir { it.name }
    override val type: KtType by firRef.withFirAndCache(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { fir -> builder.buildKtType(fir.returnTypeRef) }
    override val receiverType: KtType? by firRef.withFirAndCache(FirResolvePhase.TYPES) { fir -> fir.receiverTypeRef?.let(builder::buildKtType) }
    override val isExtension: Boolean get() = firRef.withFir { it.receiverTypeRef != null }
    override val initializer: KtConstantValue? by firRef.withFirAndCache(FirResolvePhase.BODY_RESOLVE) { fir -> fir.initializer?.convertConstantExpression() }
    override val symbolKind: KtSymbolKind
        get() = firRef.withFir { fir ->
            when (fir.containingClass()?.classId) {
                null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.MEMBER
            }
        }
    override val modality: KtCommonSymbolModality get() = getModality()

    override val visibility: KtSymbolVisibility get() = getVisibility()


    override val annotations: List<KtAnnotationCall> by firRef.withFirAndCache(FirResolvePhase.TYPES) {
        convertAnnotation(it)
    }

    override val callableIdIfNonLocal: FqName?
        get() = firRef.withFir { fir ->
            fir.symbol.callableId.takeUnless { fir.isLocal }?.asFqNameForDebugInfo()
        }

    override val getter: KtPropertyGetterSymbol? by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { property ->
        property.getter?.let { builder.buildPropertyAccessorSymbol(it) } as? KtPropertyGetterSymbol
    }

    override val setter: KtPropertySetterSymbol? by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { property ->
        property.setter?.let { builder.buildPropertyAccessorSymbol(it) } as? KtPropertySetterSymbol
    }

    override val hasBackingField: Boolean get() = firRef.withFir { it.hasBackingField }

    override val isLateInit: Boolean get() = firRef.withFir { it.isLateInit }

    override val isConst: Boolean get() = firRef.withFir { it.isConst }

    override val isOverride: Boolean get() = firRef.withFir { it.isOverride }

    override fun createPointer(): KtSymbolPointer<KtPropertySymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        return when (symbolKind) {
            KtSymbolKind.TOP_LEVEL -> TODO("Creating symbol for top level fun is not supported yet")
            KtSymbolKind.NON_PROPERTY_PARAMETER -> TODO("Creating symbol for top level parameters is not supported yet")
            KtSymbolKind.MEMBER -> KtFirMemberPropertySymbolPointer(
                firRef.withFir { it.containingClass()?.classId ?: error("ClassId should not be null for member property") },
                firRef.withFir { it.createSignature() }
            )
            KtSymbolKind.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(name.asString())
        }
    }
}
