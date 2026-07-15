/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.hasAnnotation
import org.jetbrains.kotlin.analysis.api.fir.parameterName
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirValueParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.asKaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.impl.base.util.requireIsInstance
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.varargElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPropertyAccessor

internal class KaFirValueParameterSymbol private constructor(
    override val backingPsi: KtParameter?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirValueParameterSymbol>,
) : KaValueParameterSymbol(), KaFirKtBasedSymbol<KtParameter, FirValueParameterSymbol> {
    constructor(declaration: KtParameter, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirValueParameterSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtParameter,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val name: Name
        get() = withValidityAssertion { backingPsi?.parameterName ?: firSymbol.name }

    override val isVararg: Boolean
        get() = withValidityAssertion { backingPsi?.isVarArg ?: firSymbol.isVararg }

    override val isImplicitLambdaParameter: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                false
            else
                firSymbol.source?.kind == KtFakeSourceElementKind.ItLambdaParameter
        }

    override val isCrossinline: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.CROSSINLINE_KEYWORD) ?: firSymbol.isCrossinline }

    override val visibility: KaSymbolVisibility
        get() = withValidityAssertion {
            FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility.asKaSymbolVisibility
        }

    @Deprecated("Use 'visibility' instead", level = DeprecationLevel.HIDDEN)
    override val compilerVisibility: Visibility
        get() = withValidityAssertion { FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility }

    override val isNoinline: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.NOINLINE_KEYWORD) ?: firSymbol.isNoinline }

    override val returnType: KaType
        get() = withValidityAssertion {
            val returnType = firSymbol.resolvedReturnType
            if (firSymbol.isVararg) {
                builder.typeBuilder.buildKtType(returnType.varargElementType())
            } else {
                builder.typeBuilder.buildKtType(returnType)
            }
        }

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion {
            with(analysisSession) {
                if (hasDeclaredDefaultValue) {
                    return true
                }

                val parameterIndex = index
                val ownerFunction = containingDeclaration as? KaFunctionSymbol ?: return false

                // Checks the effective (possibly inherited) default value of the matching parameter, not just the declared one. The
                // recursion into `hasDefaultValue` lets a default propagate across several hops at once, e.g., from the `expect`
                // counterpart of the base an `actual` override inherits from.
                fun KaDeclarationSymbol.hasMatchingDefaultParameter(): Boolean =
                    (this as? KaFunctionSymbol)?.valueParameters?.getOrNull(parameterIndex)?.hasDefaultValue == true

                // An implicit default value can only be inherited from an overridden declaration (for a named function) or from the
                // matched `expect` declaration (for a named function or a constructor). Other function kinds cannot have one.
                when (ownerFunction) {
                    is KaNamedFunctionSymbol ->
                        ownerFunction.isOverride && ownerFunction.directlyOverriddenSymbols.any { it.hasMatchingDefaultParameter() } ||
                                ownerFunction.isActual && ownerFunction.getExpectsForActual().any { it.hasMatchingDefaultParameter() }
                    is KaConstructorSymbol ->
                        ownerFunction.isActual && ownerFunction.getExpectsForActual().any { it.hasMatchingDefaultParameter() }
                    else -> false
                }
            }
        }

    override val hasDeclaredDefaultValue: Boolean
        get() = withValidityAssertion { backingPsi?.hasDefaultValue() ?: firSymbol.hasDefaultValue }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            if (backingPsi != null && backingPsi.annotationEntries.isEmpty()) {
                val property = (backingPsi.ownerDeclaration as? KtPropertyAccessor)?.property
                if (property?.hasAnnotation(AnnotationUseSiteTarget.SETTER_PARAMETER) != true) {
                    return@withValidityAssertion KaBaseEmptyAnnotationList(token)
                }
            }

            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val primaryConstructorProperty: KaKotlinPropertySymbol?
        get() = withValidityAssertion {
            if (backingPsi != null) {
                return if (backingPsi.hasValOrVar() && backingPsi.ownerFunction is KtPrimaryConstructor) {
                    KaFirKotlinPropertySymbol.create(backingPsi, analysisSession)
                } else {
                    null
                }
            }

            val propertySymbol = firSymbol.fir.correspondingProperty?.symbol ?: return null
            return KaFirKotlinPropertySymbol.create(propertySymbol, analysisSession)
        }

    override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaValueParameterSymbol>()?.let { return it }
        return KaFirValueParameterSymbolPointer(
            ownerPointer = analysisSession.createOwnerPointer(this),
            name = name,
            index = index,
            originalSymbol = this
        )
    }

    private val index: Int
        get() {
            val ownerSymbol = with(analysisSession) { containingDeclaration }
                ?: error("Containing function is expected for a value parameter symbol")
            requireIsInstance<KaFunctionSymbol>(ownerSymbol)

            return (ownerSymbol.firSymbol.fir as FirFunction).valueParameters.indexOf(firSymbol.fir)
        }

    override fun equals(other: Any?): Boolean {
        return psiOrSymbolEquals(other)
    }

    override fun hashCode(): Int {
        return psiOrSymbolHashCode()
    }
}
