/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirSamConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirSamConstructorSymbol(
    override val firSymbol: FirNamedFunctionSymbol,
    override val analysisSession: KaFirSession,
) : KaSamConstructorSymbol(), KaFirSymbol<FirNamedFunctionSymbol> {

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            KaFirAnnotationListForDeclaration.create(firSymbol)
        }

    override val name: Name get() = entryPointImplementation { firSymbol.name }

    override val returnType: KaType get() = entryPointImplementation { firSymbol.returnType() }
    override val isActual: Boolean get() = entryPointImplementation { firSymbol.isActual }
    override val isExpect: Boolean get() = entryPointImplementation { firSymbol.isExpect }

    override val contextReceivers: List<KaContextReceiver> get() = entryPointImplementation { firSymbol.createContextReceivers() }
    override val modality: KaSymbolModality get() = entryPointImplementation { firSymbol.kaSymbolModality }
    override val visibility: KaSymbolVisibility get() = entryPointImplementation { firSymbol.visibility.asKaSymbolVisibility }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = entryPointImplementation {
            firSymbol.createKtValueParameters()
        }

    override val hasStableParameterNames: Boolean
        get() = entryPointImplementation {
            firSymbol.fir.hasStableParameterNames
        }

    override val isExtension: Boolean get() = entryPointImplementation { firSymbol.isExtension }

    override val callableId: CallableId? get() = entryPointImplementation { firSymbol.getCallableId() }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = entryPointImplementation { firSymbol.createKtTypeParameters() }

    override fun createPointer(): KaSymbolPointer<KaSamConstructorSymbol> = entryPointImplementation {
        val typeAlias = firSymbol.fir.typeAliasConstructorInfo?.typeAliasSymbol
        val ownerPointer: KaSymbolPointer<KaClassLikeSymbol> = if (typeAlias != null) {
            // `functionalInterface` expands typealiased SAM interfaces.
            // To get the typealias symbol, we need to extract it from the FIR
            // See KT-91764
            firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(typeAlias).createPointer()
        } else {
            // Could be replaced with `containingDeclaration` after KT-91765
            functionalInterface.createPointer()
        }
        return KaFirSamConstructorSymbolPointer(ownerPointer, this)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
