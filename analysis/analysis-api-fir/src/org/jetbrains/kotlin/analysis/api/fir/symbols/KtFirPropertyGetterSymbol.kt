/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.annotations.containsAnnotation
import org.jetbrains.kotlin.analysis.api.fir.symbols.annotations.getAnnotationClassIds
import org.jetbrains.kotlin.analysis.api.fir.symbols.annotations.toAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.resolve.getHasStableParameterNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

internal class KtFirPropertyGetterSymbol(
    fir: FirPropertyAccessor,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtPropertyGetterSymbol(), KtFirSymbol<FirPropertyAccessor> {
    init {
        require(fir.isGetter)
    }

    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.moduleData.session) }

    override val isDefault: Boolean get() = firRef.withFir { it is FirDefaultPropertyAccessor }
    override val isInline: Boolean get() = firRef.withFir { it.isInline }
    override val isOverride: Boolean get() = firRef.withFir { it.isOverride }
    override val hasBody: Boolean get() = firRef.withFir { it.body != null }

    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }
    override val modality: Modality get() = getModality()
    override val visibility: Visibility get() = getVisibility()
    override val annotations: List<KtAnnotationCall> by cached { firRef.toAnnotationsList() }
    override fun containsAnnotation(classId: ClassId): Boolean = firRef.containsAnnotation(classId)
    override val annotationClassIds: Collection<ClassId> by cached { firRef.getAnnotationClassIds() }

    /**
     * Returns [CallableId] of the delegated Java method if the corresponding property of this getter is a synthetic Java property.
     * Otherwise, returns `null`
     */
    override val callableIdIfNonLocal: CallableId? by firRef.withFirAndCache { fir ->
        if (fir is FirSyntheticPropertyAccessor) {
            fir.delegate.symbol.callableId
        } else null
    }

    override val receiverType: KtTypeAndAnnotations? by cached {
        firRef.receiverTypeAndAnnotations(builder)
    }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = emptyList()

    override val hasStableParameterNames: Boolean = firRef.withFir { it.getHasStableParameterNames(it.moduleData.session) }

    override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        TODO("Creating pointers for getters from library is not supported yet")
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
