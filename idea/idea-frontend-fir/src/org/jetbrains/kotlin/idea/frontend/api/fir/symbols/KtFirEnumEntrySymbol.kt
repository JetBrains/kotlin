/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirEnumEntrySymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirEnumEntrySymbol(
    fir: FirEnumEntry,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder
) : KtEnumEntrySymbol(), KtFirSymbol<FirEnumEntry> {
    private val builder by weakRef(_builder)
    override val firRef = firRef(fir, resolveState)

    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.moduleData.session) }

    override val name: Name get() = firRef.withFir { it.name }
    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }

    override val containingEnumClassIdIfNonLocal: ClassId?
        get() = firRef.withFir { it.containingClass()?.classId?.takeUnless { it.isLocal } }

    override val callableIdIfNonLocal: CallableId? get() = getCallableIdIfNonLocal()

    override fun createPointer(): KtSymbolPointer<KtEnumEntrySymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        return firRef.withFir { fir ->
            KtFirEnumEntrySymbolPointer(
                fir.symbol.containingClass()?.classId ?: error("Containing class should present for enum entry"),
                fir.name
            )
        }
    }
}
