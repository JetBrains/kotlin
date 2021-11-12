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
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.getHasStableParameterNames
import org.jetbrains.kotlin.name.ClassId

internal class KtFirConstructorSymbol(
    fir: FirConstructor,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtConstructorSymbol(), KtFirSymbol<FirConstructor> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.moduleData.session) }

    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }

    override val valueParameters: List<KtValueParameterSymbol> by firRef.withFirAndCache { fir ->
        fir.valueParameters.map { valueParameter ->
            builder.variableLikeBuilder.buildValueParameterSymbol(valueParameter)
        }
    }

    override val hasStableParameterNames: Boolean = firRef.withFir { it.getHasStableParameterNames(it.moduleData.session) }

    override val visibility: Visibility get() = getVisibility()

    override val annotations: List<KtAnnotationCall> by cached { firRef.toAnnotationsList() }
    override fun containsAnnotation(classId: ClassId): Boolean = firRef.containsAnnotation(classId)
    override val annotationClassIds: Collection<ClassId> by cached { firRef.getAnnotationClassIds() }

    override val containingClassIdIfNonLocal: ClassId?
        get() = firRef.withFir { fir -> fir.containingClass()?.classId /* TODO check if local */ }

    override val isPrimary: Boolean get() = firRef.withFir { it.isPrimary }

    override val typeParameters by firRef.withFirAndCache { fir ->
        fir.typeParameters.map { typeParameter ->
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol.fir)
        }
    }

    override fun createPointer(): KtSymbolPointer<KtConstructorSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        if (symbolKind == KtSymbolKind.LOCAL) {
            throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException("constructor")
        }
        val ownerClassId = containingClassIdIfNonLocal
            ?: error("ClassId should present for member declaration")
        return KtFirConstructorSymbolPointer(ownerClassId, isPrimary, firRef.withFir { it.createSignature() })
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
