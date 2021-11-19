/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.evaluate.KtFirConstantValueConverter
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirMemberPropertySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.analysis.api.fir.utils.asKtInitializerValue
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.WrongSymbolForSamConstructor
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KtFirKotlinPropertySymbol(
    fir: FirProperty,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtKotlinPropertySymbol(), KtFirSymbol<FirProperty> {
    init {
        assert(!fir.isLocal)
        check(fir !is FirSyntheticProperty)
    }

    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.moduleData.session) }

    override val isVal: Boolean get() = firRef.withFir { it.isVal }
    override val name: Name get() = firRef.withFir { it.name }

    override val returnType: KtType by cached {
        firRef.returnType(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }

    override val typeParameters: List<KtTypeParameterSymbol> by cached {
        fir.typeParameters.map { builder.classifierBuilder.buildTypeParameterSymbol(it) }
    }

    override val receiverType: KtType? by cached {
        firRef.receiverType(builder)
    }

    override val isExtension: Boolean get() = firRef.withFir { it.receiverTypeRef != null }
    override val initializer by firRef.withFirAndCache(FirResolvePhase.BODY_RESOLVE) { fir ->
        fir.initializer?.asKtInitializerValue()
    }
    override val symbolKind: KtSymbolKind
        get() = firRef.withFir { fir ->
            when (fir.containingClass()?.classId) {
                null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.CLASS_MEMBER
            }
        }
    override val modality: Modality get() = getModality()

    override val visibility: Visibility get() = getVisibility()

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firRef, resolveState.rootModuleSession, token) }

    override val callableIdIfNonLocal: CallableId? get() = getCallableIdIfNonLocal()

    override val getter: KtPropertyGetterSymbol? by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { property ->
        property.getter?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertyGetterSymbol
    }

    override val setter: KtPropertySetterSymbol? by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { property ->
        property.setter?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertySetterSymbol
    }

    // NB: `field` in accessors indicates the property should have a backing field. To see that, though, we need BODY_RESOLVE.
    override val hasBackingField: Boolean get() = firRef.withFir(FirResolvePhase.BODY_RESOLVE) { it.hasBackingField }

    override val isDelegatedProperty: Boolean get() = firRef.withFir { it.delegateFieldSymbol != null }

    override val isLateInit: Boolean get() = firRef.withFir { it.isLateInit }

    override val isConst: Boolean get() = firRef.withFir { it.isConst }

    override val isFromPrimaryConstructor: Boolean
        get() = firRef.withFir {
            it.fromPrimaryConstructor == true || it.source?.kind == KtFakeSourceElementKind.PropertyFromParameter
        }
    override val isOverride: Boolean get() = firRef.withFir { it.isOverride }
    override val isStatic: Boolean get() = firRef.withFir { it.isStatic }

    override val hasGetter: Boolean get() = firRef.withFir { it.getter != null }
    override val hasSetter: Boolean get() = firRef.withFir { it.setter != null }

    override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        return when (symbolKind) {
            KtSymbolKind.TOP_LEVEL -> TODO("Creating symbol for top level properties is not supported yet")
            KtSymbolKind.CLASS_MEMBER -> firRef.withFir { fir ->
                KtFirMemberPropertySymbolPointer(
                    fir.containingClass()?.classId ?: error("ClassId should not be null for member property"),
                    fir.name,
                    fir.createSignature()
                )
            }
            KtSymbolKind.ACCESSOR -> TODO("Creating symbol for accessors is not supported yet")
            KtSymbolKind.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(name.asString())
            KtSymbolKind.SAM_CONSTRUCTOR -> throw WrongSymbolForSamConstructor(this::class.java.simpleName)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
