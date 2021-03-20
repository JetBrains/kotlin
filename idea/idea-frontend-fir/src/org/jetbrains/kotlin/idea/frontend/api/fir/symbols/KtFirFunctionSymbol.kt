/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.containsAnnotation
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.getAnnotationClassIds
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.toAnnotationsList
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirMemberFunctionSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirMemberPropertySymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirTopLevelFunctionSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirFunctionSymbol(
    fir: FirSimpleFunction,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder
) : KtFunctionSymbol(), KtFirSymbol<FirSimpleFunction> {
    private val builder by weakRef(_builder)
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.session) }
    override val name: Name get() = firRef.withFir { it.name }
    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }
    override val valueParameters: List<KtFirFunctionValueParameterSymbol> by firRef.withFirAndCache { fir ->
        fir.valueParameters.map { valueParameter ->
            builder.variableLikeBuilder.buildFunctionParameterSymbol(valueParameter)
        }
    }
    override val typeParameters by firRef.withFirAndCache { fir ->
        fir.typeParameters.map { typeParameter ->
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol.fir)
        }
    }

    override val annotations: List<KtAnnotationCall> by cached { firRef.toAnnotationsList() }
    override fun containsAnnotation(classId: ClassId): Boolean = firRef.containsAnnotation(classId)
    override val annotationClassIds: Collection<ClassId> by cached { firRef.getAnnotationClassIds() }

    override val isSuspend: Boolean get() = firRef.withFir { it.isSuspend }
    override val isOverride: Boolean get() = firRef.withFir { it.isOverride }

    override val dispatchType: KtType? by cached {
        firRef.dispatchReceiverTypeAndAnnotations(builder)
    }

    override val receiverType: KtTypeAndAnnotations? by cached {
        firRef.receiverTypeAndAnnotations(builder)
    }

    override val isOperator: Boolean get() = firRef.withFir { it.isOperator }
    override val isExternal: Boolean get() = firRef.withFir { it.isExternal }
    override val isInline: Boolean get() = firRef.withFir { it.isInline }
    override val isExtension: Boolean get() = firRef.withFir { it.receiverTypeRef != null }
    override val callableIdIfNonLocal: FqName?
        get() = firRef.withFir { fir ->
            fir.symbol.callableId.takeUnless { fir.isLocal }?.asFqNameForDebugInfo()
        }
    override val symbolKind: KtSymbolKind
        get() = firRef.withFir { fir ->
            when {
                fir.isLocal -> KtSymbolKind.LOCAL
                fir.containingClass()?.classId == null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.MEMBER
            }
        }
    override val modality: KtCommonSymbolModality get() = getModality()

    override val visibility: KtSymbolVisibility get() = getVisibility()

    override fun createPointer(): KtSymbolPointer<KtFunctionSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        return when (symbolKind) {
            KtSymbolKind.TOP_LEVEL -> firRef.withFir { fir ->
                KtFirTopLevelFunctionSymbolPointer(fir.symbol.callableId, fir.createSignature())
            }
            KtSymbolKind.MEMBER ->  firRef.withFir { fir ->
                KtFirMemberFunctionSymbolPointer(
                    fir.containingClass()?.classId ?: error("ClassId should not be null for member function"),
                    fir.name,
                    fir.createSignature()
                )
            }
            KtSymbolKind.NON_PROPERTY_PARAMETER -> error("KtFunction could not be a parameter")
            KtSymbolKind.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableIdIfNonLocal?.asString() ?: name.asString()
            )
        }
    }
}
