/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirJavaSyntheticPropertySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirSyntheticJavaPropertySymbol(
    override val firSymbol: FirSyntheticPropertySymbol,
    override val analysisSession: KaFirSession,
) : KaSyntheticJavaPropertySymbol(), KaFirSymbol<FirSyntheticPropertySymbol> {
    override val isVal: Boolean get() = entryPointImplementation { firSymbol.isVal }
    override val name: Name get() = entryPointImplementation { firSymbol.name }
    override val isActual: Boolean get() = entryPointImplementation { firSymbol.isActual }
    override val isExpect: Boolean get() = entryPointImplementation { firSymbol.isExpect }
    override val returnType: KaType get() = entryPointImplementation { firSymbol.returnType() }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = entryPointImplementation { KaFirReceiverParameterSymbol.create(null, this, analysisSession = analysisSession) }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = entryPointImplementation { firSymbol.createKtTypeParameters() }

    override val isExtension: Boolean
        get() = entryPointImplementation { false }

    override val origin: KaSymbolOrigin
        get() = entryPointImplementation { KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY }

    override val initializer: KaInitializerValue? get() = entryPointImplementation { firSymbol.getKtConstantInitializer() }

    override val modality: KaSymbolModality get() = entryPointImplementation { firSymbol.kaSymbolModality }
    override val visibility: KaSymbolVisibility get() = entryPointImplementation { firSymbol.visibility.asKaSymbolVisibility }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            KaFirAnnotationListForDeclaration.create(firSymbol)
        }

    override val callableId: CallableId?
        get() = entryPointImplementation { firSymbol.getCallableId() }

    override val getter: KaPropertyGetterSymbol
        get() = entryPointImplementation {
            KaFirSyntheticPropertyGetterSymbol(this)
        }

    override val javaGetterSymbol: KaNamedFunctionSymbol
        get() = entryPointImplementation {
            val fir = firSymbol.fir as FirSyntheticProperty
            return firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(fir.getter.delegate.symbol)
        }

    override val javaSetterSymbol: KaNamedFunctionSymbol?
        get() = entryPointImplementation {
            val fir = firSymbol.fir as FirSyntheticProperty
            return fir.setter?.delegate?.let { firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(it.symbol) }
        }

    override val setter: KaPropertySetterSymbol?
        get() = entryPointImplementation {
            firSymbol.setterSymbol?.let {
                KaFirSyntheticPropertySetterSymbol(this)
            }
        }

    override val isOverride: Boolean get() = entryPointImplementation { firSymbol.isOverride }
    override val isStatic: Boolean get() = entryPointImplementation { firSymbol.isStatic }
    override val isCompanion: Boolean get() = entryPointImplementation { firSymbol.isStatic }

    override val isExternal: Boolean get() = entryPointImplementation { firSymbol.isEffectivelyExternal(analysisSession.firSession) }

    override val hasSetter: Boolean get() = entryPointImplementation { firSymbol.setterSymbol != null }

    override fun createPointer(): KaSymbolPointer<KaSyntheticJavaPropertySymbol> = entryPointImplementation {
        KaFirJavaSyntheticPropertySymbolPointer(
            ownerPointer = createOwnerPointer(),
            propertyName = name,
            isSynthetic = firSymbol is SyntheticSymbol,
            originalSymbol = this
        )
    }

    /**
     * Synthetic properties are built anew on each scope lookup, so two symbols of the same property usually have different
     * [firSymbol]s. The property is defined by its name and its Java accessors, which are stable.
     *
     * This is a workaround for KT-91296: once the compiler reuses synthetic properties, [symbolEquals] is enough.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KaFirSyntheticJavaPropertySymbol) return false
        if (firSymbol == other.firSymbol) return true

        return firSymbol.name == other.firSymbol.name &&
                firSymbol.getterSymbol?.delegateFunctionSymbol == other.firSymbol.getterSymbol?.delegateFunctionSymbol &&
                firSymbol.setterSymbol?.delegateFunctionSymbol == other.firSymbol.setterSymbol?.delegateFunctionSymbol
    }

    override fun hashCode(): Int = firSymbol.getterSymbol?.delegateFunctionSymbol.hashCode()
}
