/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class UnsupportedImpl(
    override val unsupported: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Unsupported(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsupportedFeatureImpl(
    override val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsupportedFeature(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SyntaxImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Syntax(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class OtherErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OtherError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IllegalConstExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalConstExpression(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IllegalUnderscoreImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalUnderscore(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpressionExpectedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpressionExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AssignmentInExpressionContextImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignmentInExpressionContext(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class BreakOrContinueOutsideALoopImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.BreakOrContinueOutsideALoop(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NotALoopLabelImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotALoopLabel(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VariableExpectedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegationInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationInInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NestedClassNotAllowedImpl(
    override val declaration: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NestedClassNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IncorrectCharacterLiteralImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncorrectCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class EmptyCharacterLiteralImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.EmptyCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TooManyCharactersInCharacterLiteralImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TooManyCharactersInCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IllegalEscapeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalEscape(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IntLiteralOutOfRangeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IntLiteralOutOfRange(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FloatLiteralOutOfRangeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FloatLiteralOutOfRange(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongLongSuffixImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongLongSuffix(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DivisionByZeroImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DivisionByZero(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InvisibleReferenceImpl(
    override val reference: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvisibleReference(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnresolvedReferenceImpl(
    override val reference: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedReference(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnresolvedLabelImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedLabel(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DeserializationErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeserializationError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ErrorFromJavaResolutionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ErrorFromJavaResolution(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnknownCallableKindImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnknownCallableKind(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MissingStdlibClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MissingStdlibClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoThisImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoThis(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CreatingAnInstanceOfAbstractClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CreatingAnInstanceOfAbstractClass(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SuperIsNotAnExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperIsNotAnExpression(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SuperNotAvailableImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperNotAvailable(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractSuperCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractSuperCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InstanceAccessBeforeSuperCallImpl(
    override val target: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InstanceAccessBeforeSuperCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class EnumAsSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.EnumAsSupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RecursionInSupertypesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInSupertypes(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NotASupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotASupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SuperclassNotAccessibleFromInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperclassNotAccessibleFromInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class QualifiedSupertypeExtendedByOtherSupertypeImpl(
    override val otherSuperType: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.QualifiedSupertypeExtendedByOtherSupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SupertypeInitializedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedInInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InterfaceWithSuperclassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InterfaceWithSuperclass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ClassInSupertypeForEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ClassInSupertypeForEnum(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SealedSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedSupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SealedSupertypeInLocalClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedSupertypeInLocalClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SupertypeNotAClassOrInterfaceImpl(
    override val reason: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeNotAClassOrInterface(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstructorInObjectImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstructorInObject(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstructorInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstructorInInterface(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonPrivateConstructorInEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPrivateConstructorInEnum(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonPrivateOrProtectedConstructorInSealedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPrivateOrProtectedConstructorInSealed(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CyclicConstructorDelegationCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CyclicConstructorDelegationCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrimaryConstructorDelegationCallExpectedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrimaryConstructorDelegationCallExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SupertypeNotInitializedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeNotInitialized(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SupertypeInitializedWithoutPrimaryConstructorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedWithoutPrimaryConstructor(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegationSuperCallInEnumConstructorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationSuperCallInEnumConstructor(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrimaryConstructorRequiredForDataClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrimaryConstructorRequiredForDataClass(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExplicitDelegationCallRequiredImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExplicitDelegationCallRequired(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SealedClassConstructorCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedClassConstructorCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DataClassWithoutParametersImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DataClassWithoutParameters(), KtAbstractFirDiagnostic<KtPrimaryConstructor> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DataClassVarargParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DataClassVarargParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DataClassNotPropertyParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DataClassNotPropertyParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentKclassLiteralOfTypeParameterError(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeConstImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeConst(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeEnumConstImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeEnumConst(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeKclassLiteralImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeKclassLiteral(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationClassMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationClassMember(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationParameterDefaultValueMustBeConstantImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationParameterDefaultValueMustBeConstant(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InvalidTypeOfAnnotationMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidTypeOfAnnotationMember(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class LocalAnnotationClassErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalAnnotationClassError(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MissingValOnAnnotationParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MissingValOnAnnotationParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonConstValUsedInConstantExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonConstValUsedInConstantExpression(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NotAnAnnotationClassImpl(
    override val annotationName: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotAnAnnotationClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NullableTypeOfAnnotationMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullableTypeOfAnnotationMember(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarAnnotationParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarAnnotationParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SupertypesForAnnotationClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypesForAnnotationClass(), KtAbstractFirDiagnostic<KtClass> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationUsedAsAnnotationArgumentImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationUsedAsAnnotationArgument(), KtAbstractFirDiagnostic<KtAnnotation> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedTypealiasExpandedTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedTypealiasExpandedType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedFunctionReturnTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedFunctionReturnType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedReceiverTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedReceiverType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedPropertyTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedPropertyType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedPropertyTypeInConstructorImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedPropertyTypeInConstructor(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedParameterTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedParameterType(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedSuperInterfaceImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedSuperInterface(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedSuperClassImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedSuperClass(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedTypeParameterBoundImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedTypeParameterBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InapplicableInfixModifierImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableInfixModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RepeatedModifierImpl(
    override val modifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RepeatedModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantModifierImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DeprecatedModifierPairImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedModifierPair(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IncompatibleModifiersImpl(
    override val modifier1: KtModifierKeywordToken,
    override val modifier2: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncompatibleModifiers(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantOpenInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantOpenInInterface(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongModifierTargetImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongModifierTarget(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class OperatorModifierRequiredImpl(
    override val functionSymbol: KtFunctionLikeSymbol,
    override val name: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OperatorModifierRequired(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InlineClassNotTopLevelImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassNotTopLevel(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InlineClassNotFinalImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassNotFinal(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbsenceOfPrimaryConstructorForInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbsenceOfPrimaryConstructorForInlineClass(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InlineClassConstructorWrongParametersSizeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassConstructorWrongParametersSize(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InlineClassConstructorNotFinalReadOnlyParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassConstructorNotFinalReadOnlyParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyWithBackingFieldInsideInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyWithBackingFieldInsideInlineClass(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegatedPropertyInsideInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegatedPropertyInsideInlineClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InlineClassHasInapplicableParameterTypeImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassHasInapplicableParameterType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InlineClassCannotImplementInterfaceByDelegationImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassCannotImplementInterfaceByDelegation(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InlineClassCannotExtendClassesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassCannotExtendClasses(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InlineClassCannotBeRecursiveImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassCannotBeRecursive(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReservedMemberInsideInlineClassImpl(
    override val name: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReservedMemberInsideInlineClass(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SecondaryConstructorWithBodyInsideInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SecondaryConstructorWithBodyInsideInlineClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InnerClassInsideInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InnerClassInsideInlineClass(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ValueClassCannotBeCloneableImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValueClassCannotBeCloneable(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoneApplicable(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InapplicableCandidateImpl(
    override val candidate: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableCandidate(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ArgumentTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ArgumentTypeMismatch(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NullForNonnullTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullForNonnullType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InapplicableLateinitModifierImpl(
    override val reason: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableLateinitModifier(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarargOutsideParenthesesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarargOutsideParentheses(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NamedArgumentsNotAllowedImpl(
    override val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NamedArgumentsNotAllowed(), KtAbstractFirDiagnostic<KtValueArgument> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonVarargSpreadImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonVarargSpread(), KtAbstractFirDiagnostic<LeafPsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ArgumentPassedTwiceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ArgumentPassedTwice(), KtAbstractFirDiagnostic<KtValueArgument> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TooManyArgumentsImpl(
    override val function: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TooManyArguments(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoValueForParameterImpl(
    override val violatedParameter: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoValueForParameter(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NamedParameterNotFoundImpl(
    override val name: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NamedParameterNotFound(), KtAbstractFirDiagnostic<KtValueArgument> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ManyLambdaExpressionArgumentsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyLambdaExpressionArguments(), KtAbstractFirDiagnostic<KtValueArgument> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class OverloadResolutionAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadResolutionAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AssignOperatorAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignOperatorAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IteratorAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IteratorAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class HasNextFunctionAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.HasNextFunctionAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NextAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NextAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeMismatch(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RecursionInImplicitTypesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInImplicitTypes(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InferenceErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InferenceError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ProjectionOnNonClassTypeArgumentImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProjectionOnNonClassTypeArgument(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UpperBoundViolatedImpl(
    override val upperBound: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UpperBoundViolated(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeArgumentsNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeArgumentsNotAllowed(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongNumberOfTypeArgumentsImpl(
    override val expectedCount: Int,
    override val classifier: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongNumberOfTypeArguments(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoTypeArgumentsOnRhsImpl(
    override val expectedCount: Int,
    override val classifier: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoTypeArgumentsOnRhs(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParametersInObjectImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersInObject(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IllegalProjectionUsageImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalProjectionUsage(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParametersInEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersInEnum(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConflictingProjectionImpl(
    override val type: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingProjection(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarianceOnTypeParameterNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarianceOnTypeParameterNotAllowed(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CatchParameterWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CatchParameterWithDefaultValue(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReifiedTypeInCatchClauseImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReifiedTypeInCatchClause(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParameterInCatchClauseImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterInCatchClause(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class GenericThrowableSubclassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.GenericThrowableSubclass(), KtAbstractFirDiagnostic<KtTypeParameterList> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InnerClassOfGenericThrowableSubclassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InnerClassOfGenericThrowableSubclass(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class KclassWithNullableTypeParameterInSignatureImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.KclassWithNullableTypeParameterInSignature(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParameterAsReifiedImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterAsReified(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FinalUpperBoundImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FinalUpperBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UpperBoundIsExtensionFunctionTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UpperBoundIsExtensionFunctionType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class BoundsNotAllowedIfBoundedByTypeParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.BoundsNotAllowedIfBoundedByTypeParameter(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class OnlyOneClassBoundAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OnlyOneClassBoundAllowed(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RepeatedBoundImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RepeatedBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConflictingUpperBoundsImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingUpperBounds(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NameInConstraintIsNotATypeParameterImpl(
    override val typeParameterName: Name,
    override val typeParametersOwner: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NameInConstraintIsNotATypeParameter(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class BoundOnTypeAliasParameterNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.BoundOnTypeAliasParameterNotAllowed(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReifiedTypeParameterNoInlineImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReifiedTypeParameterNoInline(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParametersNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersNotAllowed(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParameterOfPropertyNotUsedInReceiverImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterOfPropertyNotUsedInReceiver(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReturnTypeMismatchImpl(
    override val expected: KtType,
    override val actual: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CyclicGenericUpperBoundImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CyclicGenericUpperBound(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DeprecatedTypeParameterSyntaxImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedTypeParameterSyntax(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MisplacedTypeParameterConstraintsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MisplacedTypeParameterConstraints(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DynamicUpperBoundImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DynamicUpperBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExtensionInClassReferenceNotAllowedImpl(
    override val referencedDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExtensionInClassReferenceNotAllowed(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CallableReferenceLhsNotAClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CallableReferenceLhsNotAClass(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CallableReferenceToAnnotationConstructorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CallableReferenceToAnnotationConstructor(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ClassLiteralLhsNotAClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ClassLiteralLhsNotAClass(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NullableTypeInClassLiteralLhsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullableTypeInClassLiteralLhs(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpressionOfNullableTypeInClassLiteralLhsImpl(
    override val lhsType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpressionOfNullableTypeInClassLiteralLhs(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NothingToOverrideImpl(
    override val declaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NothingToOverride(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CannotWeakenAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotWeakenAccessPrivilege(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CannotChangeAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotChangeAccessPrivilege(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class OverridingFinalMemberImpl(
    override val overriddenDeclaration: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverridingFinalMember(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractClassMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractClassMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InvisibleAbstractMemberFromSuperImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val invisibleDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvisibleAbstractMemberFromSuper(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InvisibleAbstractMemberFromSuperWarningImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val invisibleDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvisibleAbstractMemberFromSuperWarning(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ManyImplMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyImplMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ManyInterfacesMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyInterfacesMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class OverridingFinalMemberByDelegationImpl(
    override val delegatedDeclaration: KtCallableSymbol,
    override val overriddenDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverridingFinalMemberByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegatedMemberHidesSupertypeOverrideImpl(
    override val delegatedDeclaration: KtCallableSymbol,
    override val overriddenDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegatedMemberHidesSupertypeOverride(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReturnTypeMismatchOnOverrideImpl(
    override val function: KtSymbol,
    override val superFunction: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyTypeMismatchOnOverrideImpl(
    override val property: KtSymbol,
    override val superProperty: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarTypeMismatchOnOverrideImpl(
    override val variable: KtSymbol,
    override val superVariable: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarOverriddenByValImpl(
    override val overridingDeclaration: KtSymbol,
    override val overriddenDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarOverriddenByVal(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonFinalMemberInFinalClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonFinalMemberInFinalClass(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonFinalMemberInObjectImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonFinalMemberInObject(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ManyCompanionObjectsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyCompanionObjects(), KtAbstractFirDiagnostic<KtObjectDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConflictingOverloadsImpl(
    override val conflictingOverloads: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingOverloads(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedeclarationImpl(
    override val conflictingDeclarations: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Redeclaration(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MethodOfAnyImplementedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MethodOfAnyImplementedInInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class LocalObjectNotAllowedImpl(
    override val objectName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalObjectNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class LocalInterfaceNotAllowedImpl(
    override val interfaceName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalInterfaceNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractFunctionInNonAbstractClassImpl(
    override val function: KtSymbol,
    override val containingClass: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractFunctionInNonAbstractClass(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractFunctionWithBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractFunctionWithBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonAbstractFunctionWithNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonAbstractFunctionWithNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivateFunctionWithNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateFunctionWithNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonMemberFunctionNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonMemberFunctionNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FunctionDeclarationWithNoNameImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunctionDeclarationWithNoName(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnonymousFunctionWithNameImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnonymousFunctionWithName(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnonymousFunctionParameterWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnonymousFunctionParameterWithDefaultValue(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UselessVarargOnParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessVarargOnParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MultipleVarargParametersImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MultipleVarargParameters(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ForbiddenVarargParameterTypeImpl(
    override val varargParameterType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ForbiddenVarargParameterType(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ValueParameterWithNoTypeAnnotationImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValueParameterWithNoTypeAnnotation(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CannotInferParameterTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotInferParameterType(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FunInterfaceConstructorReferenceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceConstructorReference(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FunInterfaceWrongCountOfAbstractMembersImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceWrongCountOfAbstractMembers(), KtAbstractFirDiagnostic<KtClass> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FunInterfaceCannotHaveAbstractPropertiesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceCannotHaveAbstractProperties(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FunInterfaceAbstractMethodWithTypeParametersImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceAbstractMethodWithTypeParameters(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FunInterfaceAbstractMethodWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceAbstractMethodWithDefaultValue(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FunInterfaceWithSuspendFunctionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceWithSuspendFunction(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractPropertyInNonAbstractClassImpl(
    override val property: KtSymbol,
    override val containingClass: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyInNonAbstractClass(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivatePropertyInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivatePropertyInInterface(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithInitializer(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyInitializerInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyInitializerInInterface(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyWithNoTypeNoInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyWithNoTypeNoInitializer(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MustBeInitializedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MustBeInitialized(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MustBeInitializedOrBeAbstractImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MustBeInitializedOrBeAbstract(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExtensionPropertyMustHaveAccessorsOrBeAbstractImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExtensionPropertyMustHaveAccessorsOrBeAbstract(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnnecessaryLateinitImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnnecessaryLateinit(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class BackingFieldInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.BackingFieldInInterface(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExtensionPropertyWithBackingFieldImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExtensionPropertyWithBackingField(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyInitializerNoBackingFieldImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyInitializerNoBackingField(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractDelegatedProperty(), KtAbstractFirDiagnostic<KtPropertyDelegate> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegatedPropertyInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegatedPropertyInInterface(), KtAbstractFirDiagnostic<KtPropertyDelegate> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithGetterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithGetter(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithSetterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithSetter(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivateSetterForAbstractPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateSetterForAbstractProperty(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivateSetterForOpenPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateSetterForOpenProperty(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedPrivateDeclarationImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedPrivateDeclaration(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ValWithSetterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValWithSetter(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstValNotTopLevelOrObjectImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValNotTopLevelOrObject(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstValWithGetterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValWithGetter(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstValWithDelegateImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValWithDelegate(), KtAbstractFirDiagnostic<KtPropertyDelegate> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeCantBeUsedForConstValImpl(
    override val constValType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeCantBeUsedForConstVal(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstValWithoutInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValWithoutInitializer(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstValWithNonConstInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValWithNonConstInitializer(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongSetterParameterTypeImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongSetterParameterType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InitializerTypeMismatchImpl(
    override val expected: KtType,
    override val actual: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InitializerTypeMismatch(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class GetterVisibilityDiffersFromPropertyVisibilityImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.GetterVisibilityDiffersFromPropertyVisibility(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongSetterReturnTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongSetterReturnType(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedDeclarationWithBodyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedDeclarationWithBody(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedPropertyInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedPropertyInitializer(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedDelegatedProperty(), KtAbstractFirDiagnostic<KtPropertyDelegate> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedLateinitPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedLateinitProperty(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InitializerRequiredForDestructuringDeclarationImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InitializerRequiredForDestructuringDeclaration(), KtAbstractFirDiagnostic<KtDestructuringDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ComponentFunctionMissingImpl(
    override val missingFunctionName: Name,
    override val destructingType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionMissing(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ComponentFunctionAmbiguityImpl(
    override val functionWithAmbiguityName: Name,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ComponentFunctionOnNullableImpl(
    override val componentFunctionName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionOnNullable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ComponentFunctionReturnTypeMismatchImpl(
    override val componentFunctionName: Name,
    override val destructingType: KtType,
    override val expectedType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UninitializedVariableImpl(
    override val variable: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedVariable(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UninitializedEnumEntryImpl(
    override val enumEntry: KtVariableLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedEnumEntry(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UninitializedEnumCompanionImpl(
    override val enumClass: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedEnumCompanion(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ValReassignmentImpl(
    override val variable: KtVariableLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValReassignment(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ValReassignmentViaBackingFieldImpl(
    override val property: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValReassignmentViaBackingField(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ValReassignmentViaBackingFieldErrorImpl(
    override val property: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValReassignmentViaBackingFieldError(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongInvocationKindImpl(
    override val declaration: KtSymbol,
    override val requiredRange: EventOccurrencesRange,
    override val actualRange: EventOccurrencesRange,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongInvocationKind(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class LeakedInPlaceLambdaImpl(
    override val lambda: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LeakedInPlaceLambda(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongImpliesConditionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongImpliesCondition(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsafeCallImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsafeImplicitInvokeCallImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeImplicitInvokeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsafeInfixCallImpl(
    override val lhs: KtExpression,
    override val operator: String,
    override val rhs: KtExpression,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeInfixCall(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsafeOperatorCallImpl(
    override val lhs: KtExpression,
    override val operator: String,
    override val rhs: KtExpression,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeOperatorCall(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IteratorOnNullableImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IteratorOnNullable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnnecessarySafeCallImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnnecessarySafeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnexpectedSafeCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnexpectedSafeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnnecessaryNotNullAssertionImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnnecessaryNotNullAssertion(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NotNullAssertionOnLambdaExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotNullAssertionOnLambdaExpression(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NotNullAssertionOnCallableReferenceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotNullAssertionOnCallableReference(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UselessElvisImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessElvis(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UselessElvisRightIsNullImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessElvisRightIsNull(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UselessCastImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessCast(), KtAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UselessIsCheckImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessIsCheck(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoElseInWhenImpl(
    override val missingWhenCases: List<WhenMissingCase>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoElseInWhen(), KtAbstractFirDiagnostic<KtWhenExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InvalidIfAsExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidIfAsExpression(), KtAbstractFirDiagnostic<KtIfExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ElseMisplacedInWhenImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ElseMisplacedInWhen(), KtAbstractFirDiagnostic<KtWhenEntry> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParameterIsNotAnExpressionImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterIsNotAnExpression(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParameterOnLhsOfDotImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterOnLhsOfDot(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoCompanionObjectImpl(
    override val klass: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoCompanionObject(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpressionExpectedPackageFoundImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpressionExpectedPackageFound(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ErrorInContractDescriptionImpl(
    override val reason: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ErrorInContractDescription(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoGetMethodImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoGetMethod(), KtAbstractFirDiagnostic<KtArrayAccessExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoSetMethodImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoSetMethod(), KtAbstractFirDiagnostic<KtArrayAccessExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IteratorMissingImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IteratorMissing(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class HasNextMissingImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.HasNextMissing(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NextMissingImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NextMissing(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class HasNextFunctionNoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.HasNextFunctionNoneApplicable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NextNoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NextNoneApplicable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegateSpecialFunctionMissingImpl(
    override val expectedFunctionSignature: String,
    override val delegateType: KtType,
    override val description: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegateSpecialFunctionMissing(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegateSpecialFunctionAmbiguityImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegateSpecialFunctionAmbiguity(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegateSpecialFunctionNoneApplicableImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegateSpecialFunctionNoneApplicable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegateSpecialFunctionReturnTypeMismatchImpl(
    override val delegateFunction: String,
    override val expected: KtType,
    override val actual: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegateSpecialFunctionReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnderscoreIsReservedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnderscoreIsReserved(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnderscoreUsageWithoutBackticksImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnderscoreUsageWithoutBackticks(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ToplevelTypealiasesOnlyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ToplevelTypealiasesOnly(), KtAbstractFirDiagnostic<KtTypeAlias> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantVisibilityModifierImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantVisibilityModifier(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantModalityModifierImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModalityModifier(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantReturnUnitTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantReturnUnitType(), KtAbstractFirDiagnostic<PsiTypeElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantExplicitTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantExplicitType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantSingleExpressionStringTemplateImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantSingleExpressionStringTemplate(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CanBeValImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CanBeVal(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CanBeReplacedWithOperatorAssignmentImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CanBeReplacedWithOperatorAssignment(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantCallOfConversionMethodImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantCallOfConversionMethod(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ArrayEqualityOperatorCanBeReplacedWithEqualsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ArrayEqualityOperatorCanBeReplacedWithEquals(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class EmptyRangeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.EmptyRange(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantSetterParameterTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantSetterParameterType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnusedVariableImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnusedVariable(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AssignedValueIsNeverReadImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignedValueIsNeverRead(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VariableInitializerIsRedundantImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableInitializerIsRedundant(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VariableNeverReadImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableNeverRead(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UselessCallOnNotNullImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessCallOnNotNull(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReturnNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnNotAllowed(), KtAbstractFirDiagnostic<KtReturnExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReturnInFunctionWithExpressionBodyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnInFunctionWithExpressionBody(), KtAbstractFirDiagnostic<KtReturnExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UsageIsNotInlinableImpl(
    override val parameter: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UsageIsNotInlinable(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonLocalReturnNotAllowedImpl(
    override val parameter: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonLocalReturnNotAllowed(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RecursionInInlineImpl(
    override val symbol: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonPublicCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPublicCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ProtectedConstructorCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProtectedConstructorCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ProtectedCallFromPublicInlineErrorImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProtectedCallFromPublicInlineError(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ProtectedCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProtectedCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivateClassMemberFromInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateClassMemberFromInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SuperCallFromPublicInlineImpl(
    override val symbol: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

