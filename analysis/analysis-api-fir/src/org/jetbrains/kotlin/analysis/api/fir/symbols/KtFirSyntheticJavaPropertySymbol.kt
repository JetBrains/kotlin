/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.evaluate.KtFirConstantValueConverter
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.annotations.containsAnnotation
import org.jetbrains.kotlin.analysis.api.fir.symbols.annotations.getAnnotationClassIds
import org.jetbrains.kotlin.analysis.api.fir.symbols.annotations.toAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirJavaSyntheticPropertySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirSyntheticJavaPropertySymbol(
    fir: FirSyntheticProperty,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtSyntheticJavaPropertySymbol(), KtFirSymbol<FirSyntheticProperty> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.moduleData.session) }

    override val isVal: Boolean get() = firRef.withFir { it.isVal }
    override val name: Name get() = firRef.withFir { it.name }
    override val annotatedType: KtTypeAndAnnotations by cached {
        firRef.returnTypeAndAnnotations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }

    override val receiverType: KtTypeAndAnnotations? by cached {
        firRef.receiverTypeAndAnnotations(builder)
    }
    override val isExtension: Boolean get() = firRef.withFir { it.receiverTypeRef != null }
    override val initializer: KtConstantValue? by firRef.withFirAndCache(FirResolvePhase.BODY_RESOLVE) { fir ->
        fir.initializer?.let { KtFirConstantValueConverter.toConstantValue(it, resolveState.rootModuleSession) }
    }

    override val modality: Modality get() = getModality()
    override val visibility: Visibility get() = getVisibility()

    override val annotations: List<KtAnnotationCall> by cached { firRef.toAnnotationsList() }
    override fun containsAnnotation(classId: ClassId): Boolean = firRef.containsAnnotation(classId)
    override val annotationClassIds: Collection<ClassId> by cached { firRef.getAnnotationClassIds() }

    override val callableIdIfNonLocal: CallableId? get() = getCallableIdIfNonLocal()

    override val getter: KtPropertyGetterSymbol by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { property ->
        property.getter.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as KtPropertyGetterSymbol
    }
    override val javaGetterSymbol: KtFunctionSymbol
        get() {
            return firRef.withFir { builder.functionLikeBuilder.buildFunctionSymbol(it.getter.delegate) }
        }
    override val javaSetterSymbol: KtFunctionSymbol?
        get() {
            return firRef.withFir { fir ->
                fir.setter?.delegate?.let { setter -> builder.functionLikeBuilder.buildFunctionSymbol(setter) }
            }
        }

    override val setter: KtPropertySetterSymbol? by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { property ->
        property.setter?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertySetterSymbol
    }

    override val isFromPrimaryConstructor: Boolean get() = false
    override val isOverride: Boolean get() = firRef.withFir { it.isOverride }
    override val isStatic: Boolean get() = firRef.withFir { it.isStatic }

    override val hasSetter: Boolean get() = firRef.withFir { it.setter != null }

    override val origin: KtSymbolOrigin get() = withValidityAssertion { KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY }

    override fun createPointer(): KtSymbolPointer<KtSyntheticJavaPropertySymbol> {
        val containingClassId = firRef.withFir { it.containingClass()?.classId }
            ?: error("Cannot find parent class for synthetic java property $callableIdIfNonLocal")

        return KtFirJavaSyntheticPropertySymbolPointer(containingClassId, name)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
