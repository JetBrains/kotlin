/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirJavaFieldSymbol(
    fir: FirField,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder
) : KtJavaFieldSymbol(), KtFirSymbol<FirField> {
    private val builder by weakRef(_builder)
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.session) }

    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.TYPES, builder)
    }
    override val isVal: Boolean get() = firRef.withFir { it.isVal }
    override val name: Name get() = firRef.withFir { it.name }

    override val callableIdIfNonLocal: CallableId? get() = getCallableIdIfNonLocal()

    override val modality: Modality get() = getModality()

    override val visibility: Visibility get() = getVisibility()

    override fun createPointer(): KtSymbolPointer<KtJavaFieldSymbol> {
        TODO("Creating pointers for java fields is not supported yet")
    }
}