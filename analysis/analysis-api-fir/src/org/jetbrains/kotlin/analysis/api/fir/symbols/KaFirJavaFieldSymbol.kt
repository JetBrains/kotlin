/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirJavaFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirJavaFieldSymbol(
    override val firSymbol: FirFieldSymbol,
    override val analysisSession: KaFirSession,
) : KaJavaFieldSymbol(), KaFirSymbol<FirFieldSymbol> {

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            KaFirAnnotationListForDeclaration.create(firSymbol)
        }

    override val isVal: Boolean get() = entryPointImplementation { firSymbol.fir.isVal }
    override val name: Name get() = entryPointImplementation { firSymbol.name }
    override val returnType: KaType get() = entryPointImplementation { firSymbol.returnType() }

    override val callableId: CallableId? get() = entryPointImplementation { firSymbol.getCallableId() }

    override val visibility: KaSymbolVisibility get() = entryPointImplementation { firSymbol.visibility.asKaSymbolVisibility }

    override val isStatic: Boolean get() = entryPointImplementation { firSymbol.isStatic }
    override val isCompanion: Boolean get() = entryPointImplementation { firSymbol.isStatic }

    override fun createPointer(): KaSymbolPointer<KaJavaFieldSymbol> = entryPointImplementation {
        KaFirJavaFieldSymbolPointer(createOwnerPointer(), name, firSymbol.isStatic, this)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
