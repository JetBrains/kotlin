/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirKotlinPropertySymbol(
    override val firSymbol: FirPropertySymbol,
    override val analysisSession: KaFirSession,
) : KaKotlinPropertySymbol(), KaFirSymbol<FirPropertySymbol> {
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

    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.receiver(builder) }

    override val contextReceivers: List<KaContextReceiver> by cached { firSymbol.createContextReceivers(builder) }

    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }

    override val initializer: KaInitializerValue? by cached { firSymbol.getKtConstantInitializer(builder) }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            if (firSymbol.origin == FirDeclarationOrigin.DynamicScope) {
                return@withValidityAssertion KaSymbolLocation.CLASS
            }
            when (firSymbol.containingClassLookupTag()?.classId) {
                null -> KaSymbolLocation.TOP_LEVEL
                else -> KaSymbolLocation.CLASS
            }
        }

    override val modality: KaSymbolModality get() = withValidityAssertion { firSymbol.kaSymbolModality }
    override val compilerVisibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override val annotations by cached {
        KaFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    override val callableId: CallableId? get() = withValidityAssertion { firSymbol.getCallableId() }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    override val getter: KaPropertyGetterSymbol?
        get() = withValidityAssertion {
            firSymbol.getterSymbol?.let { builder.functionBuilder.buildPropertyAccessorSymbol(it) } as? KaPropertyGetterSymbol
        }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            firSymbol.setterSymbol?.let { builder.functionBuilder.buildPropertyAccessorSymbol(it) } as? KaPropertySetterSymbol
        }
    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = withValidityAssertion {
            firSymbol.backingFieldSymbol?.let { builder.variableBuilder.buildBackingFieldSymbol(it) }
        }

    // NB: `field` in accessors indicates the property should have a backing field. To see that, though, we need BODY_RESOLVE.
    override val hasBackingField: Boolean
        get() = withValidityAssertion {
            firSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
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
    override val isActual: Boolean get() = withValidityAssertion { firSymbol.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { firSymbol.isExpect }

    override val hasGetter: Boolean get() = withValidityAssertion { firSymbol.getterSymbol != null }
    override val hasSetter: Boolean get() = withValidityAssertion { firSymbol.setterSymbol != null }

    override fun createPointer(): KaSymbolPointer<KaKotlinPropertySymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaVariableSymbol>(this)?.let { psiPointer ->
            return KaFirPsiBasedPropertySymbolPointer(psiPointer)
        }

        return when (val kind = location) {
            KaSymbolLocation.TOP_LEVEL -> {
                if (firSymbol.fir.origin is FirDeclarationOrigin.ScriptCustomization.ResultProperty) {
                    KaFirResultPropertySymbolPointer(analysisSession.createOwnerPointer(this))
                } else {
                    KaFirTopLevelPropertySymbolPointer(
                        firSymbol.callableId,
                        FirCallableSignature.createSignature(firSymbol),
                    )
                }
            }

            KaSymbolLocation.CLASS ->
                KaFirMemberPropertySymbolPointer(
                    ownerPointer = analysisSession.createOwnerPointer(this),
                    name = firSymbol.name,
                    signature = FirCallableSignature.createSignature(firSymbol),
                    isStatic = firSymbol.isStatic,
                )

            else -> throw KaUnsupportedSymbolLocation(this::class, kind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}

