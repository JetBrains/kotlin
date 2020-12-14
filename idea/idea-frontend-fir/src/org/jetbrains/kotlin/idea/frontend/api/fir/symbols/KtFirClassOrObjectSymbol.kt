/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirClassOrObjectInLibrarySymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.convertAnnotation
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolVisibility
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirClassOrObjectSymbol(
    fir: FirRegularClass,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtClassOrObjectSymbol(), KtFirSymbol<FirRegularClass> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.session) }
    override val name: Name get() = firRef.withFir { it.symbol.classId.shortClassName }
    override val classIdIfNonLocal: ClassId?
        get() = firRef.withFir { fir ->
            fir.symbol.classId.takeUnless { it.isLocal }
        }

    override val modality: KtSymbolModality get() = getModality()

    override val visibility: KtSymbolVisibility get() = getVisibility()

    override val annotations: List<KtAnnotationCall> by firRef.withFirAndCache(FirResolvePhase.TYPES) {
        convertAnnotation(it)
    }

    override val isInner: Boolean get() = firRef.withFir(FirResolvePhase.STATUS) { it.isInner }

    override val companionObject: KtClassOrObjectSymbol? by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { fir ->
        fir.companionObject?.let { builder.buildClassSymbol(it) }
    }

    override val superTypes: List<KtType> by firRef.withFirAndCache(FirResolvePhase.SUPER_TYPES) { fir ->
        fir.superConeTypes.map {
            builder.buildKtType(it)
        }
    }
    override val primaryConstructor: KtConstructorSymbol? by firRef.withFirAndCache(FirResolvePhase.RAW_FIR) { fir ->
        fir.getPrimaryConstructorIfAny()?.let { builder.buildConstructorSymbol(it) }
    }

    override val typeParameters by firRef.withFirAndCache { fir ->
        fir.typeParameters.map { typeParameter ->
            builder.buildTypeParameterSymbol(typeParameter.symbol.fir)
        }
    }

    override val classKind: KtClassKind
        get() = firRef.withFir { fir ->
            when (fir.classKind) {
                ClassKind.INTERFACE -> KtClassKind.INTERFACE
                ClassKind.ENUM_CLASS -> KtClassKind.ENUM_CLASS
                ClassKind.ENUM_ENTRY -> KtClassKind.ENUM_ENTRY
                ClassKind.ANNOTATION_CLASS -> KtClassKind.ANNOTATION_CLASS
                ClassKind.CLASS -> KtClassKind.CLASS
                ClassKind.OBJECT -> if (fir.isCompanion) KtClassKind.COMPANION_OBJECT else KtClassKind.OBJECT
            }
        }
    override val symbolKind: KtSymbolKind
        get() = firRef.withFir { fir ->
            when {
                fir.isLocal -> KtSymbolKind.LOCAL
                fir.symbol.classId.isNestedClass -> KtSymbolKind.MEMBER
                else -> KtSymbolKind.TOP_LEVEL
            }
        }

    override fun createPointer(): KtSymbolPointer<KtClassOrObjectSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        if (symbolKind == KtSymbolKind.LOCAL) {
            throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(classIdIfNonLocal?.asString().orEmpty())
        }
        return KtFirClassOrObjectInLibrarySymbol(classIdIfNonLocal!!)
    }
}