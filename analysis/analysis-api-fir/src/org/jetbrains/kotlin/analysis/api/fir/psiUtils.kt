/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.KtFakePsiSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.SuspiciousFakeSourceCheck
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.diagnostics.KT_DIAGNOSTIC_CONVERTER
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.isTypeAliasedConstructor
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverridesOrDelegated
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes2
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

private val allowedFakeElementKinds = setOf(
    KtFakeSourceElementKind.FromUseSiteTarget,
    KtFakeSourceElementKind.PropertyFromParameter,
    KtFakeSourceElementKind.ItLambdaParameter,
    KtFakeSourceElementKind.EnumGeneratedDeclaration,
    KtFakeSourceElementKind.DataClassGeneratedMembers,
    KtFakeSourceElementKind.ImplicitConstructor,
    KtFakeSourceElementKind.ImplicitJavaAnnotationConstructor,
    KtFakeSourceElementKind.SamConstructor,
    KtFakeSourceElementKind.JavaRecordComponentFunction,
)

@OptIn(SuspiciousFakeSourceCheck::class)
internal fun FirElement.getAllowedPsi() = when (val source = source) {
    null -> null
    is KtRealPsiSourceElement -> source.psi
    is KtFakePsiSourceElement -> if (source.kind in allowedFakeElementKinds) psi else null
    else -> null
}

internal fun FirElement.findPsi(): PsiElement? =
    getAllowedPsi()

@KaImplementationDetail
internal fun KaFirSymbol<*>.findPsi(): PsiElement? {
    return firSymbol.findPsi(analysisSession.analysisScope)
}

@KaImplementationDetail
fun FirBasedSymbol<*>.findPsi(scope: GlobalSearchScope): PsiElement? {
    return if (
        this is FirCallableSymbol<*> &&
        !this.isTypeAliasedConstructor // typealiased constructors should not be unwrapped
    ) {
        fir.unwrapFakeOverridesOrDelegated().findPsi()
    } else {
        fir.findPsi()
    } ?: FirSyntheticFunctionInterfaceSourceProvider.findPsi(fir, scope)
}

/**
 * Finds [PsiElement] which will be used as go-to referenced element for [KtPsiReference]
 * For data classes & enums generated members like `copy` `componentN`, `values` it will return corresponding enum/data class
 * Otherwise, behaves the same way as [findPsi] returns exact PSI declaration corresponding to passed [FirDeclaration]
 */
internal fun FirDeclaration.findReferencePsi(scope: GlobalSearchScope): PsiElement? {
    return if (
        this is FirCallableDeclaration &&
        !this.symbol.isTypeAliasedConstructor // typealiased constructors should not be unwrapped
    ) {
        unwrapFakeOverridesOrDelegated().psi
    } else {
        psi
    } ?: FirSyntheticFunctionInterfaceSourceProvider.findPsi(this, scope)
}

internal val KtNamedFunction.kaSymbolModality: KaSymbolModality?
    get() {
        val modalityByModifiers = kaSymbolModalityByModifiers
        return when {
            modalityByModifiers != null -> when {
                // KT-80178: interface members with no body have implicit ABSTRACT modality
                modalityByModifiers.isOpenFromInterface && !hasBody() -> KaSymbolModality.ABSTRACT
                else -> modalityByModifiers
            }

            isTopLevel || isLocal -> KaSymbolModality.FINAL

            // Green code cannot have those modifiers with other modalities
            hasModifier(KtTokens.INLINE_KEYWORD) || hasModifier(KtTokens.TAILREC_KEYWORD) -> KaSymbolModality.FINAL

            else -> null
        }
    }

internal val KtDestructuringDeclarationEntry.entryName: Name
    get() = if (isSingleUnderscore) SpecialNames.UNDERSCORE_FOR_UNUSED_VAR else nameAsSafeName

internal val KtParameter.parameterName: Name
    get() = when {
        destructuringDeclaration != null -> SpecialNames.DESTRUCT
        name == "_" -> SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
        else -> nameAsSafeName
    }

internal val KtClassOrObject.kaSymbolModality: KaSymbolModality?
    get() = kaSymbolModalityByModifiers ?: when {
        this is KtObjectDeclaration || this is KtEnumEntry -> KaSymbolModality.FINAL
        this !is KtClass -> null
        isAnnotation() || isEnum() -> KaSymbolModality.FINAL
        isInterface() -> KaSymbolModality.ABSTRACT

        // Green code cannot have those modifiers with other modalities
        isValue() || isInline() -> KaSymbolModality.FINAL
        else -> null
    }

internal val KtProperty.kaSymbolModality: KaSymbolModality?
    get() {
        val modalityByModifiers = kaSymbolModalityByModifiers
        return when {
            modalityByModifiers != null -> when {
                // KT-80178: interface members with no body have implicit ABSTRACT modality
                modalityByModifiers.isOpenFromInterface && !hasBody() -> KaSymbolModality.ABSTRACT
                else -> modalityByModifiers
            }

            // Green code cannot have those modifiers with other modalities
            hasModifier(KtTokens.CONST_KEYWORD) -> KaSymbolModality.FINAL

            else -> null
        }
    }

internal val KtDeclaration.kaSymbolModalityByModifiers: KaSymbolModality?
    get() = when {
        hasModifier(KtTokens.FINAL_KEYWORD) -> KaSymbolModality.FINAL
        hasModifier(KtTokens.ABSTRACT_KEYWORD) -> KaSymbolModality.ABSTRACT
        hasModifier(KtTokens.OPEN_KEYWORD) -> KaSymbolModality.OPEN
        this is KtClassOrObject && hasModifier(KtTokens.SEALED_KEYWORD) -> KaSymbolModality.SEALED
        else -> null
    }

internal val KtNamedFunction.visibility: Visibility?
    get() = when {
        isLocal -> Visibilities.Local
        else -> visibilityByModifiers
    }

/**
 * The compiler forces the class-like declarations to have proper visibility right
 * away during their constructions and forbids its changes later, so the visibility might be
 * computed from the PSI directly
 */
internal val KtClassLikeDeclaration.visibility: Visibility
    get() = when (this) {
        // TODO: KT-80716 replaced with native isLocal check
        is KtTypeAlias if getClassId() == null -> Visibilities.Local
        is KtClassOrObject if isLocal -> Visibilities.Local
        else -> visibilityByModifiers ?: Visibilities.Public
    }

internal val KtProperty.visibility: Visibility?
    get() = when {
        isLocal -> Visibilities.Local
        else -> visibilityByModifiers
    }

internal val KtDeclaration.visibilityByModifiers: Visibility?
    get() = when {
        hasModifier(KtTokens.PRIVATE_KEYWORD) -> Visibilities.Private
        hasModifier(KtTokens.INTERNAL_KEYWORD) -> Visibilities.Internal
        hasModifier(KtTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
        hasModifier(KtTokens.PUBLIC_KEYWORD) -> Visibilities.Public
        else -> null
    }

internal val KtDeclaration.location: KaSymbolLocation
    get() {
        // Note: a declaration can be nested inside a modifier list (for example, in the case of dangling annotations or context parameters)
        val parent = getParentOfTypes2<KtDeclaration, KtModifierList>()

        if (this is KtTypeParameter) {
            return if (parent is KtClassOrObject) KaSymbolLocation.CLASS else KaSymbolLocation.LOCAL
        }

        return when (parent) {
            null, is KtScript -> KaSymbolLocation.TOP_LEVEL

            is KtClassOrObject -> KaSymbolLocation.CLASS

            is KtDeclarationWithBody,
            is KtDeclarationWithInitializer,
            is KtAnonymousInitializer,
            is KtModifierList,
            is KtParameter,
                -> KaSymbolLocation.LOCAL

            else -> errorWithAttachment("Unexpected parent declaration: ${parent::class.simpleName}") {
                withPsiEntry("parentDeclaration", parent)
                withPsiEntry("psi", this@location)
            }
        }
    }

internal fun KtAnnotated.hasAnnotation(useSiteTarget: AnnotationUseSiteTarget): Boolean = annotationEntries.any {
    it.useSiteTarget?.getAnnotationUseSiteTarget() == useSiteTarget
}

/**
 * If [this] is a part of a tag section subject, returns the corresponding [KDocTag].
 * Otherwise, returns null.
 */
internal fun KDocName.getTagIfSubject(): KDocTag? {
    return getStrictParentOfType<KDocLink>()?.getTagIfSubject()
}

/**
 * **true** if for [this] property should be created [KaFirPropertySetterSymbol]
 * instead of [KaFirDefaultPropertySetterSymbol][org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirDefaultPropertySetterSymbol].
 *
 * The implementation is aligned with [PsiRawFirBuilder][org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.toFirPropertyAccessor]
 */
internal val KtProperty.hasRegularSetter: Boolean
    get() = isVar && hasDelegate() || setter?.hasBody() == true

/**
 * **true** if for [this] property should be created [KaFirPropertyGetterSymbol][org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPropertyGetterSymbol]
 * instead of [KaFirDefaultPropertyGetterSymbol][org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirDefaultPropertyGetterSymbol].
 *
 * The implementation is aligned with [PsiRawFirBuilder][org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.toFirPropertyAccessor]
 */
internal val KtProperty.hasRegularGetter: Boolean
    get() = hasDelegate() || getter?.hasBody() == true

context(callable: KtCallableDeclaration)
private val KaSymbolModality.isOpenFromInterface: Boolean
    get() = this == KaSymbolModality.OPEN && (callable.containingClassOrObject as? KtClass)?.isInterface() == true

context(analysisSession: KaFirSession)
internal fun KtPsiDiagnostic.asKaDiagnostic(): KaDiagnosticWithPsi<*> = asKaDiagnostic(analysisSession)

internal fun KtPsiDiagnostic.asKaDiagnostic(analysisSession: KaFirSession): KaDiagnosticWithPsi<*> {
    return KT_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as KtDiagnostic)
}