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
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirMemberFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirTopLevelFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.WrongSymbolForSamConstructor
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.getHasStableParameterNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirFunctionSymbol(
    fir: FirSimpleFunction,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtFunctionSymbol(), KtFirSymbol<FirSimpleFunction> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.moduleData.session) }
    override val name: Name get() = firRef.withFir { it.name }
    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }

    override val valueParameters: List<KtValueParameterSymbol> by firRef.withFirAndCache { fir ->
        fir.valueParameters.map { valueParameter ->
            builder.variableLikeBuilder.buildValueParameterSymbol(valueParameter)
        }
    }

    override val typeParameters by firRef.withFirAndCache { fir ->
        fir.typeParameters.map { typeParameter ->
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol.fir)
        }
    }

    override val hasStableParameterNames: Boolean = firRef.withFir { it.getHasStableParameterNames(it.moduleData.session) }

    override val annotations: List<KtAnnotationCall> by cached { firRef.toAnnotationsList() }
    override fun containsAnnotation(classId: ClassId): Boolean = firRef.containsAnnotation(classId)
    override val annotationClassIds: Collection<ClassId> by cached { firRef.getAnnotationClassIds() }

    override val isSuspend: Boolean get() = firRef.withFir { it.isSuspend }
    override val isOverride: Boolean get() = firRef.withFir { it.isOverride }
    override val isInfix: Boolean get() = firRef.withFir { it.isInfix }
    override val isStatic: Boolean get() = firRef.withFir { it.isStatic }

    override val receiverType: KtTypeAndAnnotations? by cached {
        firRef.receiverTypeAndAnnotations(builder)
    }

    override val isOperator: Boolean get() = firRef.withFir { it.isOperator }
    override val isExternal: Boolean get() = firRef.withFir { it.isExternal }
    override val isInline: Boolean get() = firRef.withFir { it.isInline }
    override val isExtension: Boolean get() = firRef.withFir { it.receiverTypeRef != null }
    override val callableIdIfNonLocal: CallableId? get() = getCallableIdIfNonLocal()

    override val symbolKind: KtSymbolKind
        get() = firRef.withFir { fir ->
            when {
                fir.isLocal -> KtSymbolKind.LOCAL
                fir.containingClass()?.classId == null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.CLASS_MEMBER
            }
        }
    override val modality: Modality get() = getModality()

    override val visibility: Visibility get() = getVisibility()

    override fun createPointer(): KtSymbolPointer<KtFunctionSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        return when (symbolKind) {
            KtSymbolKind.TOP_LEVEL -> firRef.withFir { fir ->
                KtFirTopLevelFunctionSymbolPointer(fir.symbol.callableId, fir.createSignature())
            }
            KtSymbolKind.CLASS_MEMBER -> firRef.withFir { fir ->
                KtFirMemberFunctionSymbolPointer(
                    fir.containingClass()?.classId ?: error("ClassId should not be null for member function"),
                    fir.name,
                    fir.createSignature()
                )
            }
            KtSymbolKind.ACCESSOR -> TODO("Creating symbol for accessors fun is not supported yet")
            KtSymbolKind.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableIdIfNonLocal?.toString() ?: name.asString()
            )
            KtSymbolKind.SAM_CONSTRUCTOR -> throw WrongSymbolForSamConstructor(this::class.java.simpleName)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
