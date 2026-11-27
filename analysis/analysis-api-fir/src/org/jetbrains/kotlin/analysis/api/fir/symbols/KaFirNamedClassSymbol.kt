/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.isInlineClass
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isActualDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirNamedClassSymbol private constructor(
    override val backingPsi: KtClassOrObject?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirRegularClassSymbol>,
) : KaFirNamedClassSymbolBase<KtClassOrObject>(), KaFirKtBasedSymbol<KtClassOrObject, FirRegularClassSymbol> {
    init {
        requireWithAttachment(
            backingPsi == null || backingPsi !is KtEnumEntry && !backingPsi.isObjectLiteral(),
            { (if (backingPsi is KtEnumEntry) "Enum entry" else "Object literal") + " is not expected here" },
        ) {
            withPsiEntry("classOrObject", backingPsi)
        }
    }

    constructor(declaration: KtClassOrObject, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, analysisSession = session),
        analysisSession = session,
    )

    constructor(symbol: FirRegularClassSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtClassOrObject,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val name: Name
        get() = entryPointImplementation { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val superTypes: List<KaType>
        get() = entryPointImplementation { createSuperTypes() }

    override val classId: ClassId?
        get() = entryPointImplementation {
            if (backingPsi != null) {
                backingPsi.getClassId()
            } else {
                firSymbol.getClassId()
            }
        }

    override val modality: KaSymbolModality
        get() = entryPointImplementation {
            backingPsi?.kaSymbolModality ?: firSymbol.modality.asKaSymbolModality
        }

    override val visibility: KaSymbolVisibility
        get() = entryPointImplementation { (backingPsi?.visibility ?: firSymbol.visibility).asKaSymbolVisibility }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            psiOrSymbolAnnotationList()
        }

    override val isInner: Boolean
        get() = entryPointImplementation { backingPsi?.hasModifier(KtTokens.INNER_KEYWORD) ?: firSymbol.isInner }

    override val isData: Boolean
        get() = entryPointImplementation { backingPsi?.hasModifier(KtTokens.DATA_KEYWORD) ?: firSymbol.isData }

    override val isInline: Boolean
        get() = entryPointImplementation {
            when {
                backingPsi == null -> firSymbol.isInlineClass
                backingPsi.hasModifier(KtTokens.INLINE_KEYWORD) -> true
                backingPsi.hasModifier(KtTokens.VALUE_KEYWORD) -> firSymbol.isInlineClass
                else -> false
            }
        }

    override val isValue: Boolean
        get() = entryPointImplementation {
            if (backingPsi != null) {
                backingPsi.hasModifier(KtTokens.VALUE_KEYWORD) || backingPsi.hasModifier(KtTokens.INLINE_KEYWORD)
            } else {
                firSymbol.isValue
            }
        }

    override val isExternal: Boolean
        get() = entryPointImplementation {
            backingPsi?.isExternalDeclaration ?: firSymbol.isEffectivelyExternal(analysisSession.firSession)
        }

    override val isActual: Boolean
        get() = entryPointImplementation { backingPsi?.isActualDeclaration() ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = entryPointImplementation { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override val contextReceivers: List<KaContextReceiver>
        get() = entryPointImplementation {
            if (backingPsi != null && backingPsi.modifierList?.contextParameterList == null)
                emptyList()
            else
                firSymbol.createContextReceivers()
        }

    override val companionObject: KaNamedClassSymbol?
        get() = entryPointImplementation {
            if (backingPsi != null) {
                backingPsi.companionObjects.firstOrNull()?.let {
                    return KaFirNamedClassSymbol(it, analysisSession)
                }

                // No compiler plugin – no new companion object possibility
                if (!hasDeclarationGeneratorCompilerPlugin(backingPsi, analysisSession = analysisSession)) {
                    return null
                }
            }

            firSymbol.resolvedCompanionObjectSymbol?.let {
                firSymbolBuilder.classifierBuilder.buildNamedClassSymbol(it)
            }
        }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = entryPointImplementation {
            createKaTypeParameters() ?: firSymbol.createRegularKtTypeParameters()
        }

    override val classKind: KaClassKind
        get() = entryPointImplementation {
            when (backingPsi) {
                null -> firSymbol.classKind.toKtClassKind(isCompanionObject = firSymbol.isCompanion)
                is KtObjectDeclaration -> if (backingPsi.isCompanion()) KaClassKind.COMPANION_OBJECT else KaClassKind.OBJECT
                is KtClass -> when {
                    backingPsi.isInterface() -> KaClassKind.INTERFACE
                    backingPsi.isEnum() -> KaClassKind.ENUM_CLASS
                    backingPsi.isAnnotation() -> KaClassKind.ANNOTATION_CLASS
                    else -> KaClassKind.CLASS
                }

                else -> throw AssertionError("Unexpected class or object: ${backingPsi::class.simpleName}")
            }
        }

    override val location: KaSymbolLocation
        get() = entryPointImplementation { backingPsi?.location ?: getSymbolKind() }
}
