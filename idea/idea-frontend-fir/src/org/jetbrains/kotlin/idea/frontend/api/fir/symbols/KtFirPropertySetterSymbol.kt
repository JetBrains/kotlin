/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.containsAnnotation
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.getAnnotationClassIds
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.toAnnotationsList
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSetterParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

internal class KtFirPropertySetterSymbol(
    fir: FirPropertyAccessor,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder,
) : KtPropertySetterSymbol(), KtFirSymbol<FirPropertyAccessor> {
    init {
        require(fir.isSetter)
    }

    private val builder by weakRef(_builder)
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.session) }

    override val isDefault: Boolean get() = firRef.withFir { it is FirDefaultPropertyAccessor }
    override val isInline: Boolean get() = firRef.withFir { it.isInline }
    override val isOverride: Boolean get() = firRef.withFir { it.isOverride }
    override val hasBody: Boolean get() = firRef.withFir { it.body != null }

    override val modality: KtCommonSymbolModality get() = firRef.withFir(FirResolvePhase.STATUS) { it.modality.getSymbolModality() }
    override val visibility: KtSymbolVisibility get() = firRef.withFir(FirResolvePhase.STATUS) { it.visibility.getSymbolVisibility() }

    override val annotations: List<KtAnnotationCall> by cached { firRef.toAnnotationsList() }
    override fun containsAnnotation(classId: ClassId): Boolean = firRef.containsAnnotation(classId)
    override val annotationClassIds: Collection<ClassId> by cached { firRef.getAnnotationClassIds() }

    override val parameter: KtSetterParameterSymbol by firRef.withFirAndCache { fir ->
        builder.buildFirSetterParameter(fir.valueParameters.single())
    }

    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }

    override val symbolKind: KtSymbolKind
        get() = firRef.withFir { fir ->
            when (fir.symbol.callableId.classId) {
                null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.MEMBER
            }
        }

    override val dispatchType: KtType? by cached {
        firRef.dispatchReceiverTypeAndAnnotations(builder)
    }

    override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        TODO("Creating pointers for setters from library is not supported yet")
    }
}