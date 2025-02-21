/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.asKaSymbolModality
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.toKtClassKind
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.statusTransformerExtensions
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration
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
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirRegularClassSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtClassOrObject,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val superTypes: List<KaType>
        get() = withValidityAssertion { createSuperTypes() }

    override val classId: ClassId?
        get() = withValidityAssertion {
            if (backingPsi != null) {
                backingPsi.getClassId()
            } else {
                firSymbol.getClassId()
            }
        }

    override val modality: KaSymbolModality
        get() = withValidityAssertion {
            backingPsi?.kaSymbolModality ?: firSymbol.modality.asKaSymbolModality
        }

    override val visibility: KaSymbolVisibility
        get() = withValidityAssertion {
            backingPsi?.visibility?.asKaSymbolVisibility ?: when (val possiblyRawVisibility = firSymbol.fir.visibility) {
                Visibilities.Unknown -> if (firSymbol.fir.isLocal) KaSymbolVisibility.LOCAL else KaSymbolVisibility.PUBLIC
                else -> possiblyRawVisibility.asKaSymbolVisibility
            }
        }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { backingPsi?.visibility ?: firSymbol.visibility }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            psiOrSymbolAnnotationList()
        }

    override val isInner: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.INNER_KEYWORD) ?: firSymbol.isInner }

    override val isData: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.DATA_KEYWORD) ?: firSymbol.isData }

    override val isInline: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null) {
                backingPsi.hasModifier(KtTokens.VALUE_KEYWORD) || backingPsi.hasModifier(KtTokens.INLINE_KEYWORD)
            } else {
                firSymbol.isInlineOrValue
            }
        }

    override val isFun: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.FUN_KEYWORD) ?: firSymbol.isFun }

    override val isExternal: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.EXTERNAL_KEYWORD) ?: firSymbol.isExternal }

    override val isActual: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.ACTUAL_KEYWORD) ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion {
            if (backingPsi != null && backingPsi.contextReceiverList == null)
                emptyList()
            else
                firSymbol.createContextReceivers(builder)
        }

    override val companionObject: KaNamedClassSymbol?
        get() = withValidityAssertion {
            firSymbol.companionObjectSymbol?.let {
                builder.classifierBuilder.buildNamedClassSymbol(it)
            }
        }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            createKaTypeParameters() ?: firSymbol.createRegularKtTypeParameters(builder)
        }

    override val classKind: KaClassKind
        get() = withValidityAssertion {
            val classKind = when (backingPsi) {
                null -> firSymbol.classKind
                is KtObjectDeclaration -> ClassKind.OBJECT
                is KtClass -> when {
                    backingPsi.isInterface() -> ClassKind.INTERFACE
                    backingPsi.isEnum() -> ClassKind.ENUM_CLASS
                    backingPsi.isAnnotation() -> ClassKind.ANNOTATION_CLASS
                    else -> ClassKind.CLASS
                }
                else -> throw AssertionError("Unexpected class or object: ${backingPsi::class.simpleName}")
            }

            val isCompanionObject = (backingPsi as? KtObjectDeclaration)?.isCompanion() ?: firSymbol.isCompanion
            classKind.toKtClassKind(isCompanionObject = isCompanionObject)
        }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { backingPsi?.location ?: getSymbolKind() }
}
