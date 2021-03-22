/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.containsAnnotation
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.getAnnotationClassIds
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations.toAnnotationsList
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirMemberPropertySymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.convertConstantExpression
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirSyntheticJavaPropertySymbol(
    fir: FirSyntheticProperty,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder
) : KtSyntheticJavaPropertySymbol(), KtFirSymbol<FirSyntheticProperty> {
    private val builder by weakRef(_builder)
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.session) }

    override val isVal: Boolean get() = firRef.withFir { it.isVal }
    override val name: Name get() = firRef.withFir { it.name }
    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }
    override val dispatchType: KtType? by cached {
        firRef.dispatchReceiverTypeAndAnnotations(builder)
    }

    override val receiverType: KtTypeAndAnnotations? by cached {
        firRef.receiverTypeAndAnnotations(builder)
    }
    override val isExtension: Boolean get() = firRef.withFir { it.receiverTypeRef != null }
    override val initializer: KtConstantValue? by firRef.withFirAndCache(FirResolvePhase.BODY_RESOLVE) { fir -> fir.initializer?.convertConstantExpression() }

    override val modality: KtCommonSymbolModality get() = getModality()
    override val visibility: Visibility get() = getVisibility()

    override val annotations: List<KtAnnotationCall> by cached { firRef.toAnnotationsList() }
    override fun containsAnnotation(classId: ClassId): Boolean = firRef.containsAnnotation(classId)
    override val annotationClassIds: Collection<ClassId> by cached { firRef.getAnnotationClassIds() }

    override val callableIdIfNonLocal: CallableId? get() = getCallableIdIfNonLocal()


    override val getter: KtPropertyGetterSymbol by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { property ->
        property.getter.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as KtPropertyGetterSymbol
    }

    override val setter: KtPropertySetterSymbol? by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { property ->
        property.setter?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertySetterSymbol
    }

    override val javaGetterName: Name get() = firRef.withFir { it.getter.delegate.name }
    override val javaSetterName: Name? get() = firRef.withFir { it.setter?.delegate?.name }

    override val isOverride: Boolean get() = firRef.withFir { it.isOverride }

    override val hasSetter: Boolean get() = firRef.withFir { it.setter != null }

    override val origin: KtSymbolOrigin get() = withValidityAssertion { KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY }

    override fun createPointer(): KtSymbolPointer<KtSyntheticJavaPropertySymbol> {
        TODO("pointers to KtSyntheticJavaPropertySymbol is not supported yet")
    }
}
