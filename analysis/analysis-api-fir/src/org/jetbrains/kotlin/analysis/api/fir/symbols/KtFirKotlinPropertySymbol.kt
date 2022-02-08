/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirMemberPropertySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.WrongSymbolForSamConstructor
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KtFirKotlinPropertySymbol(
    override val firSymbol: FirPropertySymbol,
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtKotlinPropertySymbol(), KtFirSymbol<FirPropertySymbol> {
    init {
        assert(!firSymbol.isLocal)
        check(firSymbol !is FirSyntheticPropertySymbol)
        check(firSymbol.fir !is FirSyntheticProperty)
    }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion { firSymbol.delegateFieldSymbol != null }

    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val isVal: Boolean get() = withValidityAssertion { firSymbol.isVal }
    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverType: KtType? get() = withValidityAssertion { firSymbol.receiverType(builder) }
    

    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }
    override val initializer: KtInitializerValue? by cached { firSymbol.getKtConstantInitializer() }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            when (firSymbol.containingClass()?.classId) {
                null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.CLASS_MEMBER
            }
        }
    override val modality: Modality get() = withValidityAssertion { firSymbol.modalityOrFinal }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, resolveState.rootModuleSession, token) }

    override val callableIdIfNonLocal: CallableId? get() = withValidityAssertion { firSymbol.getCallableIdIfNonLocal() }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    override val getter: KtPropertyGetterSymbol?
        get() = withValidityAssertion {
            firSymbol.getterSymbol?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertyGetterSymbol
        }

    override val setter: KtPropertySetterSymbol?
        get() = withValidityAssertion {
            firSymbol.setterSymbol?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertySetterSymbol
        }

    // NB: `field` in accessors indicates the property should have a backing field. To see that, though, we need BODY_RESOLVE.
    override val hasBackingField: Boolean
        get() = withValidityAssertion {
            firSymbol.ensureResolved(FirResolvePhase.BODY_RESOLVE)
            firSymbol.fir.hasBackingField
        }
    override val isLateInit: Boolean get() = withValidityAssertion { firSymbol.isLateInit }


    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion {
            firSymbol.fir.fromPrimaryConstructor == true || firSymbol.source?.kind == KtFakeSourceElementKind.PropertyFromParameter
        }

    override val isOverride: Boolean get() = withValidityAssertion { firSymbol.isOverride }
    override val isConst: Boolean get() = withValidityAssertion { firSymbol.isConst }
    override val isStatic: Boolean get() = withValidityAssertion { firSymbol.isStatic }

    override val hasGetter: Boolean get() = withValidityAssertion { firSymbol.getterSymbol != null }
    override val hasSetter: Boolean get() = withValidityAssertion { firSymbol.setterSymbol != null }

    override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol> {
        if (firSymbol.fir.origin != FirDeclarationOrigin.SubstitutionOverride) {
            KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        }

        return when (symbolKind) {
            KtSymbolKind.TOP_LEVEL -> TODO("Creating symbol for top level properties is not supported yet")
            KtSymbolKind.CLASS_MEMBER ->
                KtFirMemberPropertySymbolPointer(
                    firSymbol.containingClass()?.classId ?: error("ClassId should not be null for member property"),
                    firSymbol.name,
                    firSymbol.createSignature()
                )

            KtSymbolKind.ACCESSOR -> TODO("Creating symbol for accessors is not supported yet")
            KtSymbolKind.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(name.asString())
            KtSymbolKind.SAM_CONSTRUCTOR -> throw WrongSymbolForSamConstructor(this::class.java.simpleName)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
