/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.contracts.coneEffectDeclarationToAnalysisApi
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.fir.utils.callableId
import org.jetbrains.kotlin.analysis.api.fir.utils.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isActualDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isCompanion
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

internal class KaFirNamedFunctionSymbol private constructor(
    override val backingPsi: KtNamedFunction?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirNamedFunctionSymbol>,
) : KaNamedFunctionSymbol(), KaFirKtBasedSymbol<KtNamedFunction, FirNamedFunctionSymbol> {
    init {
        require(backingPsi?.isAnonymous != true)
    }

    constructor(declaration: KtNamedFunction, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, analysisSession = session),
        analysisSession = session,
    )

    constructor(symbol: FirNamedFunctionSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtNamedFunction,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val name: Name get() = entryPointImplementation { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val isBuiltinFunctionInvoke: Boolean
        get() = entryPointImplementation { callableId in kotlinFunctionInvokeCallableIds }

    override val contractEffects: List<KaContractEffectDeclaration>
        get() = entryPointImplementation {
            if (backingPsi != null && !backingPsi.mayHaveContract() && !backingPsi.hasContractEffectList()) {
                emptyList()
            } else {
                firSymbol.resolvedContractDescription?.effects
                    ?.map(FirEffectDeclaration::effect)
                    ?.map { it.coneEffectDeclarationToAnalysisApi(this) }
                    .orEmpty()
            }
        }

    override val returnType: KaType
        get() = entryPointImplementation { createReturnType() }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = entryPointImplementation {
            KaFirReceiverParameterSymbol.create(backingPsi, this, analysisSession = analysisSession)
        }

    override val contextReceivers: List<KaContextReceiver>
        get() = entryPointImplementation { createContextReceivers() }

    override val contextParameters: List<KaContextParameterSymbol>
        get() = entryPointImplementation {
            createKaContextParameters() ?: firSymbol.createKaContextParameters()
        }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = entryPointImplementation {
            createKaTypeParameters() ?: firSymbol.createKtTypeParameters()
        }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = entryPointImplementation {
            createKaValueParameters() ?: firSymbol.createKtValueParameters()
        }

    override val hasStableParameterNames: Boolean
        get() = entryPointImplementation {
            if (backingPsi != null)
                true
            else
                firSymbol.fir.hasStableParameterNames
        }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation { psiOrSymbolAnnotationList() }

    override val isSuspend: Boolean
        get() = entryPointImplementation { backingPsi?.hasModifier(KtTokens.SUSPEND_KEYWORD) ?: firSymbol.isSuspend }

    /**
     * Some modifiers can be inherited, so we cannot guarantee **false** value for them by PSI in this case.
     *
     * Returns not null output of [org.jetbrains.kotlin.psi.KtModifierListOwnerStub.hasModifier]
     * if [backingPsi] is not null and the modifier is not affected by [isOverride] check.
     */
    private fun psiHasModifierConsideringInheritance(modifierToken: KtModifierKeywordToken): Boolean? {
        if (backingPsi == null) return null

        val hasModifier = backingPsi.hasModifier(modifierToken)
        return when {
            // The modifier is explicitly declared, so it shouldn't be changed
            hasModifier -> true
            // The modifier is inherited, so it might be changed
            isOverride -> null
            // The modifier is not explicitly declared and not inherited, so it should be false
            else -> false
        }
    }

    override val isOverride: Boolean
        get() = entryPointImplementation {
            isOverrideWithWorkaround
        }

    override val isInfix: Boolean
        get() = entryPointImplementation {
            psiHasModifierConsideringInheritance(KtTokens.INFIX_KEYWORD) ?: firSymbol.isInfix
        }

    override val isStatic: Boolean
        get() = entryPointImplementation {
            // Kotlin doesn't have static functions
            if (backingPsi != null || origin == KaSymbolOrigin.SOURCE)
                false
            else
                firSymbol.isStatic
        }

    @OptIn(KtExperimentalApi::class)
    override val isCompanion: Boolean
        get() = entryPointImplementation {
            backingPsi?.isCompanion ?: firSymbol.isStatic
        }

    override val isTailRec: Boolean
        get() = entryPointImplementation { backingPsi?.hasModifier(KtTokens.TAILREC_KEYWORD) ?: firSymbol.isTailRec }

    override val isOperator: Boolean
        get() = entryPointImplementation {
            psiHasModifierConsideringInheritance(KtTokens.OPERATOR_KEYWORD) ?: firSymbol.isOperator
        }

    override val isExternal: Boolean
        get() = entryPointImplementation {
            backingPsi?.isExternalDeclaration ?: firSymbol.isEffectivelyExternal(analysisSession.firSession)
        }

    override val isInline: Boolean
        get() = entryPointImplementation { backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) ?: firSymbol.isInline }

    override val isExtension: Boolean
        get() = entryPointImplementation { backingPsi?.isExtensionDeclaration() ?: firSymbol.isExtension }

    override val isActual: Boolean
        get() = entryPointImplementation { backingPsi?.isActualDeclaration() ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = entryPointImplementation { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override val callableId: CallableId?
        get() = entryPointImplementation {
            if (backingPsi != null)
                backingPsi.callableId
            else
                firSymbol.getCallableId()
        }

    override val location: KaSymbolLocation
        get() = entryPointImplementation {
            when {
                backingPsi != null -> backingPsi.location
                firSymbol.origin == FirDeclarationOrigin.DynamicScope -> KaSymbolLocation.CLASS
                firSymbol.rawStatus.visibility == Visibilities.Local -> KaSymbolLocation.LOCAL
                firSymbol.containingClassLookupTag()?.classId == null -> KaSymbolLocation.TOP_LEVEL
                else -> KaSymbolLocation.CLASS
            }
        }

    override val modality: KaSymbolModality
        get() = entryPointImplementation {
            val psiBasedModality = backingPsi?.run {
                val modalityByModifiers = kaSymbolModalityByModifiers
                when {
                    modalityByModifiers != null -> when {
                        // KT-80178: interface members with no body have implicit ABSTRACT modality
                        modalityByModifiers.isOpenFromInterface && !hasBody() -> KaSymbolModality.ABSTRACT
                        else -> modalityByModifiers
                    }

                    isTopLevel || isLocal -> KaSymbolModality.FINAL

                    // Green code cannot have those modifiers with other modalities
                    hasModifier(KtTokens.INLINE_KEYWORD) || hasModifier(KtTokens.TAILREC_KEYWORD) -> KaSymbolModality.FINAL

                    else -> psiBasedDefaultKaModality(::isOverride)
                }
            }

            psiBasedModality ?: firSymbol.kaSymbolModality
        }

    override val visibility: KaSymbolVisibility
        get() = entryPointImplementation {
            val compilerVisibility = backingPsi?.psiBasedVisibility(::isOverride) ?: firSymbol.visibility
            compilerVisibility.asKaSymbolVisibility
        }

    override fun createPointer(): KaSymbolPointer<KaNamedFunctionSymbol> = entryPointImplementation {
        psiBasedSymbolPointerOfTypeIfSource<KaNamedFunctionSymbol>()?.let { return it }

        when (val kind = location) {
            KaSymbolLocation.TOP_LEVEL -> KaFirTopLevelFunctionSymbolPointer(
                firSymbol.callableId,
                FirCallableSignature.createSignature(firSymbol),
                this
            )

            KaSymbolLocation.CLASS -> createMemberFunctionPointer()

            KaSymbolLocation.LOCAL -> throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableId?.toString() ?: name.asString()
            )

            else -> throw KaUnsupportedSymbolLocation(this::class, kind)
        }
    }

    context(analysisSession: KaFirSession)
    private fun createMemberFunctionPointer(): KaSymbolPointer<KaNamedFunctionSymbol> {
        if (origin == KaSymbolOrigin.JS_DYNAMIC) {
            return KaFirDynamicFunctionSymbolPointer(name, this)
        }

        if (origin.isFromJava) {
            createJavaSyntheticPropertyAccessorPointerIfApplicable()?.let { return it }
        }

        return KaFirMemberFunctionSymbolPointer(
            ownerPointer = createOwnerPointer(),
            name = name,
            signature = FirCallableSignature.createSignature(firSymbol),
            isStatic = firSymbol.isStatic,
            originalSymbol = this
        )
    }

    /**
     * Creates a symbol pointer for Java accessor methods (getFoo/setFoo) that implement Kotlin properties.
     *
     * When a Java class implements a Kotlin interface with a property, the Java getter/setter methods
     * are exposed as a [KaSyntheticJavaPropertySymbol]. The underlying Java methods can be accessed
     * via [KaSyntheticJavaPropertySymbol.javaGetterSymbol] and [KaSyntheticJavaPropertySymbol.javaSetterSymbol].
     *
     * These accessor functions cannot be restored via the normal member function pointer because they're
     * not directly found in the class's member scope - they're only accessible through the synthetic property.
     *
     * @return A pointer for synthetic property accessor functions, or null if not applicable.
     */
    context(analysisSession: KaFirSession)
    private fun createJavaSyntheticPropertyAccessorPointerIfApplicable(): KaSymbolPointer<KaNamedFunctionSymbol>? {
        val accessorKind = KaSyntheticJavaPropertyAccessorKind.fromAccessorName(name) ?: return null

        val containingClassSymbol = firSymbol.containingClassLookupTag()?.toSymbol(analysisSession.firSession)
                as? FirClassSymbol<*> ?: return null

        val scope = containingClassSymbol.unsubstitutedScope(
            useSiteSession = analysisSession.firSession,
            scopeSession = analysisSession.getScopeSessionFor(analysisSession.firSession),
            withForcedTypeCalculator = false,
            memberRequiredPhase = FirResolvePhase.STATUS,
        )

        val propertyNameCandidates = getPropertyNamesCandidatesByAccessorName(name)
        for (propertyName in propertyNameCandidates) {
            for (propertySymbol in scope.getProperties(propertyName)) {
                val syntheticProperty = propertySymbol.fir as? FirSyntheticProperty ?: continue
                val matchesAccessor = when (accessorKind) {
                    KaSyntheticJavaPropertyAccessorKind.GETTER -> syntheticProperty.getter.delegate.symbol == firSymbol
                    KaSyntheticJavaPropertyAccessorKind.SETTER -> syntheticProperty.setter?.delegate?.symbol == firSymbol
                }

                if (matchesAccessor) {
                    return KaFirJavaSyntheticPropertyAccessorFunctionSymbolPointer(
                        ownerPointer = createOwnerPointer(),
                        propertyName = propertyName,
                        isGetter = accessorKind == KaSyntheticJavaPropertyAccessorKind.GETTER,
                        originalSymbol = this
                    )
                }
            }
        }

        return null
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
