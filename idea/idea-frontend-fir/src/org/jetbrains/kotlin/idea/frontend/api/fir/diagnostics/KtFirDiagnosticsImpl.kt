/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.KtWhenCondition
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Incompatible
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class UnsupportedImpl(
    override val unsupported: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.Unsupported(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnsupportedFeatureImpl(
    override val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsupportedFeature(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NewInferenceErrorImpl(
    override val error: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NewInferenceError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SyntaxImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.Syntax(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OtherErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OtherError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalConstExpressionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalConstExpression(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalUnderscoreImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalUnderscore(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpressionExpectedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpressionExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssignmentInExpressionContextImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignmentInExpressionContext(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class BreakOrContinueOutsideALoopImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.BreakOrContinueOutsideALoop(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NotALoopLabelImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotALoopLabel(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class BreakOrContinueJumpsAcrossFunctionBoundaryImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.BreakOrContinueJumpsAcrossFunctionBoundary(), KtAbstractFirDiagnostic<KtExpressionWithLabel> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VariableExpectedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegationInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationInInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegationNotToInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationNotToInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NestedClassNotAllowedImpl(
    override val declaration: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NestedClassNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IncorrectCharacterLiteralImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncorrectCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class EmptyCharacterLiteralImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.EmptyCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TooManyCharactersInCharacterLiteralImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TooManyCharactersInCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalEscapeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalEscape(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IntLiteralOutOfRangeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IntLiteralOutOfRange(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FloatLiteralOutOfRangeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FloatLiteralOutOfRange(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongLongSuffixImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongLongSuffix(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DivisionByZeroImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DivisionByZero(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValOrVarOnLoopParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValOrVarOnLoopParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValOrVarOnFunParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValOrVarOnFunParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValOrVarOnCatchParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValOrVarOnCatchParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValOrVarOnSecondaryConstructorParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValOrVarOnSecondaryConstructorParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InvisibleReferenceImpl(
    override val reference: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvisibleReference(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnresolvedReferenceImpl(
    override val reference: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedReference(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnresolvedLabelImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedLabel(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeserializationErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeserializationError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ErrorFromJavaResolutionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ErrorFromJavaResolution(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class MissingStdlibClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.MissingStdlibClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoThisImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoThis(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecationErrorImpl(
    override val reference: KtSymbol,
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecationError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecationImpl(
    override val reference: KtSymbol,
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.Deprecation(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnresolvedReferenceWrongReceiverImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedReferenceWrongReceiver(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CreatingAnInstanceOfAbstractClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CreatingAnInstanceOfAbstractClass(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunctionCallExpectedImpl(
    override val functionName: String,
    override val hasValueParameters: Boolean,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunctionCallExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalSelectorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalSelector(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoReceiverAllowedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoReceiverAllowed(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunctionExpectedImpl(
    override val expression: String,
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunctionExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ResolutionToClassifierImpl(
    override val classSymbol: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ResolutionToClassifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SuperCallWithDefaultParametersImpl(
    override val name: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperCallWithDefaultParameters(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NotASupertypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotASupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeArgumentsRedundantInSuperQualifierImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeArgumentsRedundantInSuperQualifier(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SuperclassNotAccessibleFromInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperclassNotAccessibleFromInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class QualifiedSupertypeExtendedByOtherSupertypeImpl(
    override val otherSuperType: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.QualifiedSupertypeExtendedByOtherSupertype(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SupertypeInitializedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedInInterface(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InterfaceWithSuperclassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InterfaceWithSuperclass(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FinalSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FinalSupertype(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ClassCannotBeExtendedDirectlyImpl(
    override val classSymbol: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ClassCannotBeExtendedDirectly(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SupertypeIsExtensionFunctionTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeIsExtensionFunctionType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SingletonInSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SingletonInSupertype(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NullableSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullableSupertype(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ManyClassesInSupertypeListImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyClassesInSupertypeList(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SupertypeAppearsTwiceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeAppearsTwice(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ClassInSupertypeForEnumImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ClassInSupertypeForEnum(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SealedSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedSupertype(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SealedSupertypeInLocalClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedSupertypeInLocalClass(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SealedInheritorInDifferentPackageImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedInheritorInDifferentPackage(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SealedInheritorInDifferentModuleImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedInheritorInDifferentModule(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ClassInheritsJavaSealedClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ClassInheritsJavaSealedClass(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SupertypeNotAClassOrInterfaceImpl(
    override val reason: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeNotAClassOrInterface(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CyclicInheritanceHierarchyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CyclicInheritanceHierarchy(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpandedTypeCannotBeInheritedImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpandedTypeCannotBeInherited(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ProjectionInImmediateArgumentToSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProjectionInImmediateArgumentToSupertype(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InconsistentTypeParameterValuesImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val type: KtClassLikeSymbol,
    override val bounds: List<KtType>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InconsistentTypeParameterValues(), KtAbstractFirDiagnostic<KtClass> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InconsistentTypeParameterBoundsImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val type: KtClassLikeSymbol,
    override val bounds: List<KtType>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InconsistentTypeParameterBounds(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AmbiguousSuperImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AmbiguousSuper(), KtAbstractFirDiagnostic<KtSuperExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConstructorInObjectImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstructorInObject(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConstructorInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstructorInInterface(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonPrivateConstructorInEnumImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPrivateConstructorInEnum(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonPrivateOrProtectedConstructorInSealedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPrivateOrProtectedConstructorInSealed(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CyclicConstructorDelegationCallImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CyclicConstructorDelegationCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PrimaryConstructorDelegationCallExpectedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrimaryConstructorDelegationCallExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SupertypeNotInitializedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeNotInitialized(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SupertypeInitializedWithoutPrimaryConstructorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedWithoutPrimaryConstructor(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegationSuperCallInEnumConstructorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationSuperCallInEnumConstructor(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PrimaryConstructorRequiredForDataClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrimaryConstructorRequiredForDataClass(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExplicitDelegationCallRequiredImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExplicitDelegationCallRequired(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SealedClassConstructorCallImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedClassConstructorCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DataClassWithoutParametersImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DataClassWithoutParameters(), KtAbstractFirDiagnostic<KtPrimaryConstructor> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DataClassVarargParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DataClassVarargParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DataClassNotPropertyParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DataClassNotPropertyParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentKclassLiteralOfTypeParameterError(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeConstImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeConst(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeEnumConstImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeEnumConst(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeKclassLiteralImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeKclassLiteral(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationClassMemberImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationClassMember(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationParameterDefaultValueMustBeConstantImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationParameterDefaultValueMustBeConstant(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InvalidTypeOfAnnotationMemberImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidTypeOfAnnotationMember(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class LocalAnnotationClassErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalAnnotationClassError(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class MissingValOnAnnotationParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.MissingValOnAnnotationParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonConstValUsedInConstantExpressionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonConstValUsedInConstantExpression(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationClassConstructorCallImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationClassConstructorCall(), KtAbstractFirDiagnostic<KtCallExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NotAnAnnotationClassImpl(
    override val annotationName: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotAnAnnotationClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NullableTypeOfAnnotationMemberImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullableTypeOfAnnotationMember(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VarAnnotationParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarAnnotationParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SupertypesForAnnotationClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypesForAnnotationClass(), KtAbstractFirDiagnostic<KtClass> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationUsedAsAnnotationArgumentImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationUsedAsAnnotationArgument(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalKotlinVersionStringValueImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalKotlinVersionStringValue(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NewerVersionInSinceKotlinImpl(
    override val specifiedVersion: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NewerVersionInSinceKotlin(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedSinceKotlinWithUnorderedVersionsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinWithUnorderedVersions(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedSinceKotlinWithoutArgumentsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinWithoutArguments(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedSinceKotlinWithoutDeprecatedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinWithoutDeprecated(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedSinceKotlinWithDeprecatedLevelImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinWithDeprecatedLevel(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedSinceKotlinOutsideKotlinSubpackageImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinOutsideKotlinSubpackage(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationOnSuperclassErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationOnSuperclassError(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnnotationOnSuperclassWarningImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationOnSuperclassWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RestrictedRetentionForExpressionAnnotationErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RestrictedRetentionForExpressionAnnotationError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RestrictedRetentionForExpressionAnnotationWarningImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RestrictedRetentionForExpressionAnnotationWarning(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongAnnotationTargetImpl(
    override val actualTarget: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongAnnotationTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongAnnotationTargetWithUseSiteTargetImpl(
    override val actualTarget: String,
    override val useSiteTarget: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongAnnotationTargetWithUseSiteTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableTargetOnPropertyImpl(
    override val useSiteDescription: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableTargetOnProperty(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableTargetPropertyImmutableImpl(
    override val useSiteDescription: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableTargetPropertyImmutable(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableTargetPropertyHasNoDelegateImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableTargetPropertyHasNoDelegate(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableTargetPropertyHasNoBackingFieldImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableTargetPropertyHasNoBackingField(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableParamTargetImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableParamTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantAnnotationTargetImpl(
    override val useSiteDescription: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantAnnotationTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableFileTargetImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableFileTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalApiUsageImpl(
    override val optInMarkerFqName: FqName,
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalApiUsage(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalApiUsageErrorImpl(
    override val optInMarkerFqName: FqName,
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalApiUsageError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalOverrideImpl(
    override val optInMarkerFqName: FqName,
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalOverride(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalOverrideErrorImpl(
    override val optInMarkerFqName: FqName,
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalOverrideError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalIsNotEnabledImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalIsNotEnabled(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalCanOnlyBeUsedAsAnnotationImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalCanOnlyBeUsedAsAnnotation(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalMarkerCanOnlyBeUsedAsAnnotationOrArgumentInUseExperimentalImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalMarkerCanOnlyBeUsedAsAnnotationOrArgumentInUseExperimental(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UseExperimentalWithoutArgumentsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UseExperimentalWithoutArguments(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UseExperimentalArgumentIsNotMarkerImpl(
    override val notMarkerFqName: FqName,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UseExperimentalArgumentIsNotMarker(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalAnnotationWithWrongTargetImpl(
    override val target: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalAnnotationWithWrongTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExperimentalAnnotationWithWrongRetentionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExperimentalAnnotationWithWrongRetention(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedTypealiasExpandedTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedTypealiasExpandedType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedFunctionReturnTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedFunctionReturnType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedReceiverTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedReceiverType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedPropertyTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedPropertyType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedPropertyTypeInConstructorImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedPropertyTypeInConstructor(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedParameterTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedParameterType(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedSuperInterfaceImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedSuperInterface(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedSuperClassImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedSuperClass(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExposedTypeParameterBoundImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedTypeParameterBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableInfixModifierImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableInfixModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RepeatedModifierImpl(
    override val modifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RepeatedModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantModifierImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedModifierImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val actualModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedModifierPairImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedModifierPair(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedModifierForTargetImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedModifierForTarget(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantModifierForTargetImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModifierForTarget(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IncompatibleModifiersImpl(
    override val modifier1: KtModifierKeywordToken,
    override val modifier2: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncompatibleModifiers(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantOpenInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantOpenInInterface(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongModifierTargetImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongModifierTarget(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OperatorModifierRequiredImpl(
    override val functionSymbol: KtFunctionLikeSymbol,
    override val name: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OperatorModifierRequired(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InfixModifierRequiredImpl(
    override val functionSymbol: KtFunctionLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InfixModifierRequired(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongModifierContainingDeclarationImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongModifierContainingDeclaration(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedModifierContainingDeclarationImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedModifierContainingDeclaration(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableOperatorModifierImpl(
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableOperatorModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineClassNotTopLevelImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassNotTopLevel(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineClassNotFinalImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassNotFinal(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbsenceOfPrimaryConstructorForInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbsenceOfPrimaryConstructorForInlineClass(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineClassConstructorWrongParametersSizeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassConstructorWrongParametersSize(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineClassConstructorNotFinalReadOnlyParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassConstructorNotFinalReadOnlyParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PropertyWithBackingFieldInsideInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyWithBackingFieldInsideInlineClass(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegatedPropertyInsideInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegatedPropertyInsideInlineClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineClassHasInapplicableParameterTypeImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassHasInapplicableParameterType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineClassCannotImplementInterfaceByDelegationImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassCannotImplementInterfaceByDelegation(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineClassCannotExtendClassesImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassCannotExtendClasses(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineClassCannotBeRecursiveImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineClassCannotBeRecursive(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReservedMemberInsideInlineClassImpl(
    override val name: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReservedMemberInsideInlineClass(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SecondaryConstructorWithBodyInsideInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SecondaryConstructorWithBodyInsideInlineClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InnerClassInsideInlineClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InnerClassInsideInlineClass(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValueClassCannotBeCloneableImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValueClassCannotBeCloneable(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoneApplicable(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableCandidateImpl(
    override val candidate: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableCandidate(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeMismatch(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ThrowableTypeMismatchImpl(
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ThrowableTypeMismatch(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConditionTypeMismatchImpl(
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConditionTypeMismatch(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ArgumentTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ArgumentTypeMismatch(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NullForNonnullTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullForNonnullType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InapplicableLateinitModifierImpl(
    override val reason: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableLateinitModifier(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VarargOutsideParenthesesImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarargOutsideParentheses(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NamedArgumentsNotAllowedImpl(
    override val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NamedArgumentsNotAllowed(), KtAbstractFirDiagnostic<KtValueArgument> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonVarargSpreadImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonVarargSpread(), KtAbstractFirDiagnostic<LeafPsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ArgumentPassedTwiceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ArgumentPassedTwice(), KtAbstractFirDiagnostic<KtValueArgument> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TooManyArgumentsImpl(
    override val function: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TooManyArguments(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoValueForParameterImpl(
    override val violatedParameter: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoValueForParameter(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NamedParameterNotFoundImpl(
    override val name: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NamedParameterNotFound(), KtAbstractFirDiagnostic<KtValueArgument> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssignmentTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignmentTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ResultTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ResultTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ManyLambdaExpressionArgumentsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyLambdaExpressionArguments(), KtAbstractFirDiagnostic<KtValueArgument> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NewInferenceNoInformationForParameterImpl(
    override val name: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NewInferenceNoInformationForParameter(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SpreadOfNullableImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SpreadOfNullable(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssigningSingleElementToVarargInNamedFormFunctionErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssigningSingleElementToVarargInNamedFormFunctionError(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssigningSingleElementToVarargInNamedFormFunctionWarningImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssigningSingleElementToVarargInNamedFormFunctionWarning(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssigningSingleElementToVarargInNamedFormAnnotationErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssigningSingleElementToVarargInNamedFormAnnotationError(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssigningSingleElementToVarargInNamedFormAnnotationWarningImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssigningSingleElementToVarargInNamedFormAnnotationWarning(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverloadResolutionAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadResolutionAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssignOperatorAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignOperatorAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IteratorAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IteratorAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class HasNextFunctionAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.HasNextFunctionAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NextAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NextAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RecursionInImplicitTypesImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInImplicitTypes(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InferenceErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InferenceError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ProjectionOnNonClassTypeArgumentImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProjectionOnNonClassTypeArgument(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UpperBoundViolatedImpl(
    override val expectedUpperBound: KtType,
    override val actualUpperBound: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UpperBoundViolated(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UpperBoundViolatedInTypealiasExpansionImpl(
    override val expectedUpperBound: KtType,
    override val actualUpperBound: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UpperBoundViolatedInTypealiasExpansion(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeArgumentsNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeArgumentsNotAllowed(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongNumberOfTypeArgumentsImpl(
    override val expectedCount: Int,
    override val classifier: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongNumberOfTypeArguments(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoTypeArgumentsOnRhsImpl(
    override val expectedCount: Int,
    override val classifier: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoTypeArgumentsOnRhs(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OuterClassArgumentsRequiredImpl(
    override val outer: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OuterClassArgumentsRequired(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParametersInObjectImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersInObject(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalProjectionUsageImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalProjectionUsage(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParametersInEnumImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersInEnum(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConflictingProjectionImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingProjection(), KtAbstractFirDiagnostic<KtTypeProjection> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConflictingProjectionInTypealiasExpansionImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingProjectionInTypealiasExpansion(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantProjectionImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantProjection(), KtAbstractFirDiagnostic<KtTypeProjection> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VarianceOnTypeParameterNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarianceOnTypeParameterNotAllowed(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CatchParameterWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CatchParameterWithDefaultValue(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReifiedTypeInCatchClauseImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReifiedTypeInCatchClause(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParameterInCatchClauseImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterInCatchClause(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class GenericThrowableSubclassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.GenericThrowableSubclass(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InnerClassOfGenericThrowableSubclassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InnerClassOfGenericThrowableSubclass(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class KclassWithNullableTypeParameterInSignatureImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.KclassWithNullableTypeParameterInSignature(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParameterAsReifiedImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterAsReified(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParameterAsReifiedArrayErrorImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterAsReifiedArrayError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParameterAsReifiedArrayWarningImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterAsReifiedArrayWarning(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReifiedTypeForbiddenSubstitutionImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReifiedTypeForbiddenSubstitution(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FinalUpperBoundImpl(
    override val type: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FinalUpperBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UpperBoundIsExtensionFunctionTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UpperBoundIsExtensionFunctionType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class BoundsNotAllowedIfBoundedByTypeParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.BoundsNotAllowedIfBoundedByTypeParameter(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OnlyOneClassBoundAllowedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OnlyOneClassBoundAllowed(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RepeatedBoundImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RepeatedBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConflictingUpperBoundsImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingUpperBounds(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NameInConstraintIsNotATypeParameterImpl(
    override val typeParameterName: Name,
    override val typeParametersOwner: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NameInConstraintIsNotATypeParameter(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class BoundOnTypeAliasParameterNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.BoundOnTypeAliasParameterNotAllowed(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReifiedTypeParameterNoInlineImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReifiedTypeParameterNoInline(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParametersNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersNotAllowed(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParameterOfPropertyNotUsedInReceiverImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterOfPropertyNotUsedInReceiver(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReturnTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val targetFunction: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CyclicGenericUpperBoundImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CyclicGenericUpperBound(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedTypeParameterSyntaxImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedTypeParameterSyntax(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class MisplacedTypeParameterConstraintsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.MisplacedTypeParameterConstraints(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DynamicUpperBoundImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DynamicUpperBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IncompatibleTypesImpl(
    override val typeA: KtType,
    override val typeB: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncompatibleTypes(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IncompatibleTypesWarningImpl(
    override val typeA: KtType,
    override val typeB: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncompatibleTypesWarning(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeVarianceConflictImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val typeParameterVariance: Variance,
    override val variance: Variance,
    override val containingType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeVarianceConflict(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeVarianceConflictInExpandedTypeImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val typeParameterVariance: Variance,
    override val variance: Variance,
    override val containingType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeVarianceConflictInExpandedType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SmartcastImpossibleImpl(
    override val desiredType: KtType,
    override val subject: KtExpression,
    override val description: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SmartcastImpossible(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExtensionInClassReferenceNotAllowedImpl(
    override val referencedDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExtensionInClassReferenceNotAllowed(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CallableReferenceLhsNotAClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CallableReferenceLhsNotAClass(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CallableReferenceToAnnotationConstructorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CallableReferenceToAnnotationConstructor(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ClassLiteralLhsNotAClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ClassLiteralLhsNotAClass(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NullableTypeInClassLiteralLhsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullableTypeInClassLiteralLhs(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpressionOfNullableTypeInClassLiteralLhsImpl(
    override val lhsType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpressionOfNullableTypeInClassLiteralLhs(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NothingToOverrideImpl(
    override val declaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NothingToOverride(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CannotOverrideInvisibleMemberImpl(
    override val overridingMember: KtCallableSymbol,
    override val baseMember: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotOverrideInvisibleMember(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DataClassOverrideConflictImpl(
    override val overridingMember: KtCallableSymbol,
    override val baseMember: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DataClassOverrideConflict(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CannotWeakenAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotWeakenAccessPrivilege(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CannotChangeAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotChangeAccessPrivilege(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverridingFinalMemberImpl(
    override val overriddenDeclaration: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverridingFinalMember(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReturnTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KtCallableSymbol,
    override val conflictingDeclaration2: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnTypeMismatchOnInheritance(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PropertyTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KtCallableSymbol,
    override val conflictingDeclaration2: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyTypeMismatchOnInheritance(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VarTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KtCallableSymbol,
    override val conflictingDeclaration2: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarTypeMismatchOnInheritance(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReturnTypeMismatchByDelegationImpl(
    override val delegateDeclaration: KtCallableSymbol,
    override val baseDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnTypeMismatchByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PropertyTypeMismatchByDelegationImpl(
    override val delegateDeclaration: KtCallableSymbol,
    override val baseDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyTypeMismatchByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VarOverriddenByValByDelegationImpl(
    override val delegateDeclaration: KtCallableSymbol,
    override val baseDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarOverriddenByValByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConflictingInheritedMembersImpl(
    override val conflictingDeclarations: List<KtCallableSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingInheritedMembers(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractClassMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractClassMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InvisibleAbstractMemberFromSuperErrorImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val invisibleDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvisibleAbstractMemberFromSuperError(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InvisibleAbstractMemberFromSuperWarningImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val invisibleDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvisibleAbstractMemberFromSuperWarning(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ManyImplMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyImplMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ManyInterfacesMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyInterfacesMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverridingFinalMemberByDelegationImpl(
    override val delegatedDeclaration: KtCallableSymbol,
    override val overriddenDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverridingFinalMemberByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegatedMemberHidesSupertypeOverrideImpl(
    override val delegatedDeclaration: KtCallableSymbol,
    override val overriddenDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegatedMemberHidesSupertypeOverride(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReturnTypeMismatchOnOverrideImpl(
    override val function: KtCallableSymbol,
    override val superFunction: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PropertyTypeMismatchOnOverrideImpl(
    override val property: KtCallableSymbol,
    override val superProperty: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VarTypeMismatchOnOverrideImpl(
    override val variable: KtCallableSymbol,
    override val superVariable: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VarOverriddenByValImpl(
    override val overridingDeclaration: KtCallableSymbol,
    override val overriddenDeclaration: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarOverriddenByVal(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonFinalMemberInFinalClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonFinalMemberInFinalClass(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonFinalMemberInObjectImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonFinalMemberInObject(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VirtualMemberHiddenImpl(
    override val declared: KtCallableSymbol,
    override val overriddenContainer: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VirtualMemberHidden(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ManyCompanionObjectsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyCompanionObjects(), KtAbstractFirDiagnostic<KtObjectDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConflictingOverloadsImpl(
    override val conflictingOverloads: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingOverloads(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedeclarationImpl(
    override val conflictingDeclarations: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.Redeclaration(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PackageOrClassifierRedeclarationImpl(
    override val conflictingDeclarations: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PackageOrClassifierRedeclaration(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class MethodOfAnyImplementedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.MethodOfAnyImplementedInInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class LocalObjectNotAllowedImpl(
    override val objectName: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalObjectNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class LocalInterfaceNotAllowedImpl(
    override val interfaceName: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalInterfaceNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractFunctionInNonAbstractClassImpl(
    override val function: KtCallableSymbol,
    override val containingClass: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractFunctionInNonAbstractClass(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractFunctionWithBodyImpl(
    override val function: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractFunctionWithBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonAbstractFunctionWithNoBodyImpl(
    override val function: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonAbstractFunctionWithNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PrivateFunctionWithNoBodyImpl(
    override val function: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateFunctionWithNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonMemberFunctionNoBodyImpl(
    override val function: KtCallableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonMemberFunctionNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunctionDeclarationWithNoNameImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunctionDeclarationWithNoName(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnonymousFunctionWithNameImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnonymousFunctionWithName(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnonymousFunctionParameterWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnonymousFunctionParameterWithDefaultValue(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UselessVarargOnParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessVarargOnParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class MultipleVarargParametersImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.MultipleVarargParameters(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ForbiddenVarargParameterTypeImpl(
    override val varargParameterType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ForbiddenVarargParameterType(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValueParameterWithNoTypeAnnotationImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValueParameterWithNoTypeAnnotation(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CannotInferParameterTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotInferParameterType(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunInterfaceConstructorReferenceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceConstructorReference(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunInterfaceWrongCountOfAbstractMembersImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceWrongCountOfAbstractMembers(), KtAbstractFirDiagnostic<KtClass> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunInterfaceCannotHaveAbstractPropertiesImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceCannotHaveAbstractProperties(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunInterfaceAbstractMethodWithTypeParametersImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceAbstractMethodWithTypeParameters(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunInterfaceAbstractMethodWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceAbstractMethodWithDefaultValue(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FunInterfaceWithSuspendFunctionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunInterfaceWithSuspendFunction(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractPropertyInNonAbstractClassImpl(
    override val property: KtCallableSymbol,
    override val containingClass: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyInNonAbstractClass(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PrivatePropertyInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivatePropertyInInterface(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithInitializerImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithInitializer(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PropertyInitializerInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyInitializerInInterface(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PropertyWithNoTypeNoInitializerImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyWithNoTypeNoInitializer(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class MustBeInitializedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.MustBeInitialized(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class MustBeInitializedOrBeAbstractImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.MustBeInitializedOrBeAbstract(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExtensionPropertyMustHaveAccessorsOrBeAbstractImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExtensionPropertyMustHaveAccessorsOrBeAbstract(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnnecessaryLateinitImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnnecessaryLateinit(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class BackingFieldInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.BackingFieldInInterface(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExtensionPropertyWithBackingFieldImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExtensionPropertyWithBackingField(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PropertyInitializerNoBackingFieldImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyInitializerNoBackingField(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractDelegatedProperty(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegatedPropertyInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegatedPropertyInInterface(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithGetterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithGetter(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithSetterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithSetter(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PrivateSetterForAbstractPropertyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateSetterForAbstractProperty(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PrivateSetterForOpenPropertyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateSetterForOpenProperty(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValWithSetterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValWithSetter(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConstValNotTopLevelOrObjectImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValNotTopLevelOrObject(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConstValWithGetterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValWithGetter(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConstValWithDelegateImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValWithDelegate(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeCantBeUsedForConstValImpl(
    override val constValType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeCantBeUsedForConstVal(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConstValWithoutInitializerImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValWithoutInitializer(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConstValWithNonConstInitializerImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstValWithNonConstInitializer(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongSetterParameterTypeImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongSetterParameterType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InitializerTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InitializerTypeMismatch(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class GetterVisibilityDiffersFromPropertyVisibilityImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.GetterVisibilityDiffersFromPropertyVisibility(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SetterVisibilityInconsistentWithPropertyVisibilityImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SetterVisibilityInconsistentWithPropertyVisibility(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongSetterReturnTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongSetterReturnType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongGetterReturnTypeImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongGetterReturnType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AccessorForDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AccessorForDelegatedProperty(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AbstractPropertyInPrimaryConstructorParametersImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyInPrimaryConstructorParameters(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedDeclarationWithBodyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedDeclarationWithBody(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedClassConstructorDelegationCallImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedClassConstructorDelegationCall(), KtAbstractFirDiagnostic<KtConstructorDelegationCall> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedClassConstructorPropertyParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedClassConstructorPropertyParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedEnumConstructorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedEnumConstructor(), KtAbstractFirDiagnostic<KtConstructor<*>> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedEnumEntryWithBodyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedEnumEntryWithBody(), KtAbstractFirDiagnostic<KtEnumEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedPropertyInitializerImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedPropertyInitializer(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedDelegatedProperty(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedLateinitPropertyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedLateinitProperty(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SupertypeInitializedInExpectedClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedInExpectedClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedPrivateDeclarationImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedPrivateDeclaration(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ImplementationByDelegationInExpectClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ImplementationByDelegationInExpectClass(), KtAbstractFirDiagnostic<KtDelegatedSuperTypeEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ActualTypeAliasNotToClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ActualTypeAliasNotToClass(), KtAbstractFirDiagnostic<KtTypeAlias> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ActualTypeAliasToClassWithDeclarationSiteVarianceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ActualTypeAliasToClassWithDeclarationSiteVariance(), KtAbstractFirDiagnostic<KtTypeAlias> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ActualTypeAliasWithUseSiteVarianceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ActualTypeAliasWithUseSiteVariance(), KtAbstractFirDiagnostic<KtTypeAlias> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ActualTypeAliasWithComplexSubstitutionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ActualTypeAliasWithComplexSubstitution(), KtAbstractFirDiagnostic<KtTypeAlias> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ActualFunctionWithDefaultArgumentsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ActualFunctionWithDefaultArguments(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ActualAnnotationConflictingDefaultArgumentValueImpl(
    override val parameter: KtVariableLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ActualAnnotationConflictingDefaultArgumentValue(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedFunctionSourceWithDefaultArgumentsNotFoundImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedFunctionSourceWithDefaultArgumentsNotFound(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoActualForExpectImpl(
    override val declaration: KtSymbol,
    override val module: FirModuleData,
    override val compatibility: Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoActualForExpect(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ActualWithoutExpectImpl(
    override val declaration: KtSymbol,
    override val compatibility: Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ActualWithoutExpect(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AmbiguousActualsImpl(
    override val declaration: KtSymbol,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AmbiguousActuals(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AmbiguousExpectsImpl(
    override val declaration: KtSymbol,
    override val modules: List<FirModuleData>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AmbiguousExpects(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoActualClassMemberForExpectedClassImpl(
    override val declaration: KtSymbol,
    override val members: List<Pair<KtSymbol, Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>>>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoActualClassMemberForExpectedClass(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ActualMissingImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ActualMissing(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InitializerRequiredForDestructuringDeclarationImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InitializerRequiredForDestructuringDeclaration(), KtAbstractFirDiagnostic<KtDestructuringDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ComponentFunctionMissingImpl(
    override val missingFunctionName: Name,
    override val destructingType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionMissing(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ComponentFunctionAmbiguityImpl(
    override val functionWithAmbiguityName: Name,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ComponentFunctionOnNullableImpl(
    override val componentFunctionName: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionOnNullable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ComponentFunctionReturnTypeMismatchImpl(
    override val componentFunctionName: Name,
    override val destructingType: KtType,
    override val expectedType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UninitializedVariableImpl(
    override val variable: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedVariable(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UninitializedParameterImpl(
    override val parameter: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedParameter(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UninitializedEnumEntryImpl(
    override val enumEntry: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedEnumEntry(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UninitializedEnumCompanionImpl(
    override val enumClass: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedEnumCompanion(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValReassignmentImpl(
    override val variable: KtVariableLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValReassignment(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValReassignmentViaBackingFieldErrorImpl(
    override val property: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValReassignmentViaBackingFieldError(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ValReassignmentViaBackingFieldWarningImpl(
    override val property: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ValReassignmentViaBackingFieldWarning(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CapturedValInitializationImpl(
    override val property: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CapturedValInitialization(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CapturedMemberValInitializationImpl(
    override val property: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CapturedMemberValInitialization(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SetterProjectedOutImpl(
    override val property: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SetterProjectedOut(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongInvocationKindImpl(
    override val declaration: KtSymbol,
    override val requiredRange: EventOccurrencesRange,
    override val actualRange: EventOccurrencesRange,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongInvocationKind(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class LeakedInPlaceLambdaImpl(
    override val lambda: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.LeakedInPlaceLambda(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class WrongImpliesConditionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongImpliesCondition(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VariableWithNoTypeNoInitializerImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableWithNoTypeNoInitializer(), KtAbstractFirDiagnostic<KtVariableDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InitializationBeforeDeclarationImpl(
    override val property: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InitializationBeforeDeclaration(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnreachableCodeImpl(
    override val reachable: List<PsiElement>,
    override val unreachable: List<PsiElement>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnreachableCode(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SenselessComparisonImpl(
    override val expression: KtExpression,
    override val compareResult: Boolean,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SenselessComparison(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SenselessNullInWhenImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SenselessNullInWhen(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnsafeCallImpl(
    override val receiverType: KtType,
    override val receiverExpression: KtExpression?,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnsafeImplicitInvokeCallImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeImplicitInvokeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnsafeInfixCallImpl(
    override val receiverExpression: KtExpression,
    override val operator: String,
    override val argumentExpression: KtExpression,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeInfixCall(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnsafeOperatorCallImpl(
    override val receiverExpression: KtExpression,
    override val operator: String,
    override val argumentExpression: KtExpression,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeOperatorCall(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IteratorOnNullableImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IteratorOnNullable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnnecessarySafeCallImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnnecessarySafeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnexpectedSafeCallImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnexpectedSafeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnnecessaryNotNullAssertionImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnnecessaryNotNullAssertion(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NotNullAssertionOnLambdaExpressionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotNullAssertionOnLambdaExpression(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NotNullAssertionOnCallableReferenceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotNullAssertionOnCallableReference(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UselessElvisImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessElvis(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UselessElvisRightIsNullImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessElvisRightIsNull(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UselessCastImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessCast(), KtAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UselessIsCheckImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessIsCheck(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IsEnumEntryImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IsEnumEntry(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class EnumEntryAsTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.EnumEntryAsType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpectedConditionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedCondition(), KtAbstractFirDiagnostic<KtWhenCondition> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoElseInWhenImpl(
    override val missingWhenCases: List<WhenMissingCase>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoElseInWhen(), KtAbstractFirDiagnostic<KtWhenExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonExhaustiveWhenStatementImpl(
    override val type: String,
    override val missingWhenCases: List<WhenMissingCase>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonExhaustiveWhenStatement(), KtAbstractFirDiagnostic<KtWhenExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InvalidIfAsExpressionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidIfAsExpression(), KtAbstractFirDiagnostic<KtIfExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ElseMisplacedInWhenImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ElseMisplacedInWhen(), KtAbstractFirDiagnostic<KtWhenEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalDeclarationInWhenSubjectImpl(
    override val illegalReason: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalDeclarationInWhenSubject(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CommaInWhenConditionWithoutArgumentImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CommaInWhenConditionWithoutArgument(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParameterIsNotAnExpressionImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterIsNotAnExpression(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypeParameterOnLhsOfDotImpl(
    override val typeParameter: KtTypeParameterSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterOnLhsOfDot(), KtAbstractFirDiagnostic<KtSimpleNameExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoCompanionObjectImpl(
    override val klass: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoCompanionObject(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ExpressionExpectedPackageFoundImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpressionExpectedPackageFound(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ErrorInContractDescriptionImpl(
    override val reason: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ErrorInContractDescription(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoGetMethodImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoGetMethod(), KtAbstractFirDiagnostic<KtArrayAccessExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoSetMethodImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoSetMethod(), KtAbstractFirDiagnostic<KtArrayAccessExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IteratorMissingImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IteratorMissing(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class HasNextMissingImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.HasNextMissing(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NextMissingImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NextMissing(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class HasNextFunctionNoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.HasNextFunctionNoneApplicable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NextNoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NextNoneApplicable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegateSpecialFunctionMissingImpl(
    override val expectedFunctionSignature: String,
    override val delegateType: KtType,
    override val description: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegateSpecialFunctionMissing(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegateSpecialFunctionAmbiguityImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegateSpecialFunctionAmbiguity(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegateSpecialFunctionNoneApplicableImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegateSpecialFunctionNoneApplicable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegateSpecialFunctionReturnTypeMismatchImpl(
    override val delegateFunction: String,
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegateSpecialFunctionReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnderscoreIsReservedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnderscoreIsReserved(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnderscoreUsageWithoutBackticksImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnderscoreUsageWithoutBackticks(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ResolvedToUnderscoreNamedCatchParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ResolvedToUnderscoreNamedCatchParameter(), KtAbstractFirDiagnostic<KtNameReferenceExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InvalidCharactersImpl(
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidCharacters(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DangerousCharactersImpl(
    override val characters: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DangerousCharacters(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class EqualityNotApplicableImpl(
    override val operator: String,
    override val leftType: KtType,
    override val rightType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.EqualityNotApplicable(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class EqualityNotApplicableWarningImpl(
    override val operator: String,
    override val leftType: KtType,
    override val rightType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.EqualityNotApplicableWarning(), KtAbstractFirDiagnostic<KtBinaryExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IncompatibleEnumComparisonErrorImpl(
    override val leftType: KtType,
    override val rightType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncompatibleEnumComparisonError(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IncDecShouldNotReturnUnitImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncDecShouldNotReturnUnit(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssignmentOperatorShouldReturnUnitImpl(
    override val functionSymbol: KtFunctionLikeSymbol,
    override val operator: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignmentOperatorShouldReturnUnit(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ToplevelTypealiasesOnlyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ToplevelTypealiasesOnly(), KtAbstractFirDiagnostic<KtTypeAlias> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RecursiveTypealiasExpansionImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursiveTypealiasExpansion(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class TypealiasShouldExpandToClassImpl(
    override val expandedType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypealiasShouldExpandToClass(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantVisibilityModifierImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantVisibilityModifier(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantModalityModifierImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModalityModifier(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantReturnUnitTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantReturnUnitType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantExplicitTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantExplicitType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantSingleExpressionStringTemplateImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantSingleExpressionStringTemplate(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CanBeValImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CanBeVal(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CanBeReplacedWithOperatorAssignmentImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CanBeReplacedWithOperatorAssignment(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantCallOfConversionMethodImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantCallOfConversionMethod(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ArrayEqualityOperatorCanBeReplacedWithEqualsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ArrayEqualityOperatorCanBeReplacedWithEquals(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class EmptyRangeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.EmptyRange(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantSetterParameterTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantSetterParameterType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UnusedVariableImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnusedVariable(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AssignedValueIsNeverReadImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignedValueIsNeverRead(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VariableInitializerIsRedundantImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableInitializerIsRedundant(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VariableNeverReadImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableNeverRead(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UselessCallOnNotNullImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessCallOnNotNull(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReturnNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnNotAllowed(), KtAbstractFirDiagnostic<KtReturnExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReturnInFunctionWithExpressionBodyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnInFunctionWithExpressionBody(), KtAbstractFirDiagnostic<KtReturnExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NoReturnInFunctionWithBlockBodyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoReturnInFunctionWithBlockBody(), KtAbstractFirDiagnostic<KtDeclarationWithBody> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class AnonymousInitializerInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnonymousInitializerInInterface(), KtAbstractFirDiagnostic<KtAnonymousInitializer> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UsageIsNotInlinableImpl(
    override val parameter: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UsageIsNotInlinable(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonLocalReturnNotAllowedImpl(
    override val parameter: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonLocalReturnNotAllowed(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NotYetSupportedInInlineImpl(
    override val message: String,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotYetSupportedInInline(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NothingToInlineImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NothingToInline(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NullableInlineParameterImpl(
    override val parameter: KtSymbol,
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullableInlineParameter(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RecursionInInlineImpl(
    override val symbol: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonPublicCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPublicCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ProtectedConstructorCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProtectedConstructorCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ProtectedCallFromPublicInlineErrorImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProtectedCallFromPublicInlineError(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ProtectedCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProtectedCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PrivateClassMemberFromInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateClassMemberFromInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SuperCallFromPublicInlineImpl(
    override val symbol: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeclarationCantBeInlinedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeclarationCantBeInlined(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverrideByInlineImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverrideByInline(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonInternalPublishedApiImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonInternalPublishedApi(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InvalidDefaultFunctionalParameterForInlineImpl(
    override val defaultValue: KtExpression,
    override val parameter: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidDefaultFunctionalParameterForInline(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReifiedTypeParameterInOverrideImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReifiedTypeParameterInOverride(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlinePropertyWithBackingFieldImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlinePropertyWithBackingField(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalInlineParameterModifierImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalInlineParameterModifier(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InlineSuspendFunctionTypeUnsupportedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InlineSuspendFunctionTypeUnsupported(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class RedundantInlineSuspendFunctionTypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantInlineSuspendFunctionType(), KtAbstractFirDiagnostic<KtElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CannotAllUnderImportFromSingletonImpl(
    override val objectName: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotAllUnderImportFromSingleton(), KtAbstractFirDiagnostic<KtImportDirective> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class PackageCannotBeImportedImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.PackageCannotBeImported(), KtAbstractFirDiagnostic<KtImportDirective> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class CannotBeImportedImpl(
    override val name: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotBeImported(), KtAbstractFirDiagnostic<KtImportDirective> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConflictingImportImpl(
    override val name: Name,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingImport(), KtAbstractFirDiagnostic<KtImportDirective> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OperatorRenamedOnImportImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OperatorRenamedOnImport(), KtAbstractFirDiagnostic<KtImportDirective> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalSuspendFunctionCallImpl(
    override val suspendCallable: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalSuspendFunctionCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalSuspendPropertyAccessImpl(
    override val suspendCallable: KtSymbol,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalSuspendPropertyAccess(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonLocalSuspensionPointImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonLocalSuspensionPoint(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalRestrictedSuspendingFunctionCallImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalRestrictedSuspendingFunctionCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonModifierFormForBuiltInSuspendImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonModifierFormForBuiltInSuspend(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ModifierFormForNonBuiltInSuspendImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ModifierFormForNonBuiltInSuspend(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ReturnForBuiltInSuspendImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnForBuiltInSuspend(), KtAbstractFirDiagnostic<KtReturnExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class ConflictingJvmDeclarationsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingJvmDeclarations(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class JavaTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.JavaTypeMismatch(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class UpperBoundCannotBeArrayImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.UpperBoundCannotBeArray(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class StrictfpOnClassImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.StrictfpOnClass(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VolatileOnValueImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VolatileOnValue(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class VolatileOnDelegateImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.VolatileOnDelegate(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SynchronizedOnAbstractImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SynchronizedOnAbstract(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SynchronizedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SynchronizedInInterface(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class SynchronizedOnInlineImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.SynchronizedOnInline(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverloadsWithoutDefaultArgumentsImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadsWithoutDefaultArguments(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverloadsAbstractImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadsAbstract(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverloadsInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadsInterface(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverloadsLocalImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadsLocal(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverloadsAnnotationClassConstructorErrorImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadsAnnotationClassConstructorError(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverloadsAnnotationClassConstructorWarningImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadsAnnotationClassConstructorWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class OverloadsPrivateImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverloadsPrivate(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DeprecatedJavaAnnotationImpl(
    override val kotlinName: FqName,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedJavaAnnotation(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class JvmPackageNameCannotBeEmptyImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.JvmPackageNameCannotBeEmpty(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class JvmPackageNameMustBeValidNameImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.JvmPackageNameMustBeValidName(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class JvmPackageNameNotSupportedInFilesWithClassesImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.JvmPackageNameNotSupportedInFilesWithClasses(), KtAbstractFirDiagnostic<KtAnnotationEntry> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class LocalJvmRecordImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalJvmRecord(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonFinalJvmRecordImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonFinalJvmRecord(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class EnumJvmRecordImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.EnumJvmRecord(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class JvmRecordWithoutPrimaryConstructorParametersImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.JvmRecordWithoutPrimaryConstructorParameters(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class NonDataClassJvmRecordImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonDataClassJvmRecord(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class JvmRecordNotValParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.JvmRecordNotValParameter(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class JvmRecordNotLastVarargParameterImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.JvmRecordNotLastVarargParameter(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class InnerJvmRecordImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.InnerJvmRecord(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class FieldInJvmRecordImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.FieldInJvmRecord(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class DelegationByInJvmRecordImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationByInJvmRecord(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class JvmRecordExtendsClassImpl(
    override val superType: KtType,
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.JvmRecordExtendsClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

internal class IllegalJavaLangRecordSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalJavaLangRecordSupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic by weakRef(firDiagnostic)
}

