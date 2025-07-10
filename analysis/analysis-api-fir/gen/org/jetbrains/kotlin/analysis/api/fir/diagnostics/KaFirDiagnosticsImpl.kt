/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeprecationInfo
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement.Version
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBackingField
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContextReceiver
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
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
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
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility.Mismatch
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class UnsupportedImpl(
    override val unsupported: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.Unsupported

internal class UnsupportedFeatureImpl(
    override val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedFeature

internal class UnsupportedSuspendTestImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedSuspendTest

internal class NewInferenceErrorImpl(
    override val error: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NewInferenceError

internal class OtherErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OtherError

internal class OtherErrorWithReasonImpl(
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OtherErrorWithReason

internal class IllegalConstExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalConstExpression

internal class IllegalUnderscoreImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalUnderscore

internal class ExpressionExpectedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpressionExpected

internal class AssignmentInExpressionContextImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.AssignmentInExpressionContext

internal class BreakOrContinueOutsideALoopImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.BreakOrContinueOutsideALoop

internal class NotALoopLabelImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotALoopLabel

internal class BreakOrContinueJumpsAcrossFunctionBoundaryImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpressionWithLabel>(firDiagnostic, token), KaFirDiagnostic.BreakOrContinueJumpsAcrossFunctionBoundary

internal class VariableExpectedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VariableExpected

internal class DelegationInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegationInInterface

internal class DelegationNotToInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegationNotToInterface

internal class NestedClassNotAllowedImpl(
    override val declaration: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NestedClassNotAllowed

internal class NestedClassNotAllowedInLocalErrorImpl(
    override val declaration: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NestedClassNotAllowedInLocalError

internal class NestedClassNotAllowedInLocalWarningImpl(
    override val declaration: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NestedClassNotAllowedInLocalWarning

internal class IncorrectCharacterLiteralImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IncorrectCharacterLiteral

internal class EmptyCharacterLiteralImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.EmptyCharacterLiteral

internal class TooManyCharactersInCharacterLiteralImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TooManyCharactersInCharacterLiteral

internal class IllegalEscapeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalEscape

internal class IntLiteralOutOfRangeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IntLiteralOutOfRange

internal class FloatLiteralOutOfRangeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FloatLiteralOutOfRange

internal class WrongLongSuffixImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongLongSuffix

internal class UnsignedLiteralWithoutDeclarationsOnClasspathImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnsignedLiteralWithoutDeclarationsOnClasspath

internal class DivisionByZeroImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DivisionByZero

internal class ValOrVarOnLoopParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValOrVarOnLoopParameter

internal class ValOrVarOnFunParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValOrVarOnFunParameter

internal class ValOrVarOnCatchParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValOrVarOnCatchParameter

internal class ValOrVarOnSecondaryConstructorParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValOrVarOnSecondaryConstructorParameter

internal class InvisibleSetterImpl(
    override val property: KaVariableSymbol,
    override val visibility: Visibility,
    override val callableId: CallableId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvisibleSetter

internal class InnerOnTopLevelScriptClassErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InnerOnTopLevelScriptClassError

internal class InnerOnTopLevelScriptClassWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InnerOnTopLevelScriptClassWarning

internal class ErrorSuppressionImpl(
    override val diagnosticName: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ErrorSuppression

internal class MissingConstructorKeywordImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingConstructorKeyword

internal class RedundantInterpolationPrefixImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantInterpolationPrefix

internal class WrappedLhsInAssignmentErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrappedLhsInAssignmentError

internal class WrappedLhsInAssignmentWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrappedLhsInAssignmentWarning

internal class InvisibleReferenceImpl(
    override val reference: KaSymbol,
    override val visible: Visibility,
    override val containingDeclaration: ClassId?,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvisibleReference

internal class UnresolvedReferenceImpl(
    override val reference: String,
    override val operator: String?,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnresolvedReference

internal class UnresolvedLabelImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnresolvedLabel

internal class AmbiguousLabelImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousLabel

internal class LabelNameClashImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LabelNameClash

internal class DeserializationErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeserializationError

internal class ErrorFromJavaResolutionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ErrorFromJavaResolution

internal class MissingStdlibClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingStdlibClass

internal class NoThisImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoThis

internal class DeprecationErrorImpl(
    override val reference: KaSymbol,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecationError

internal class DeprecationImpl(
    override val reference: KaSymbol,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.Deprecation

internal class VersionRequirementDeprecationErrorImpl(
    override val reference: KaSymbol,
    override val version: Version,
    override val currentVersion: String,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VersionRequirementDeprecationError

internal class VersionRequirementDeprecationImpl(
    override val reference: KaSymbol,
    override val version: Version,
    override val currentVersion: String,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VersionRequirementDeprecation

internal class TypealiasExpansionDeprecationErrorImpl(
    override val alias: KaSymbol,
    override val reference: KaSymbol,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpansionDeprecationError

internal class TypealiasExpansionDeprecationImpl(
    override val alias: KaSymbol,
    override val reference: KaSymbol,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpansionDeprecation

internal class ApiNotAvailableImpl(
    override val sinceKotlinVersion: ApiVersion,
    override val currentVersion: ApiVersion,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ApiNotAvailable

internal class UnresolvedReferenceWrongReceiverImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnresolvedReferenceWrongReceiver

internal class UnresolvedImportImpl(
    override val reference: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnresolvedImport

internal class PlaceholderProjectionInQualifierImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PlaceholderProjectionInQualifier

internal class DuplicateParameterNameInFunctionTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DuplicateParameterNameInFunctionType

internal class MissingDependencyClassImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClass

internal class MissingDependencyClassInExpressionTypeImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClassInExpressionType

internal class MissingDependencySuperclassImpl(
    override val missingType: KaType,
    override val declarationType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencySuperclass

internal class MissingDependencySuperclassWarningImpl(
    override val missingType: KaType,
    override val declarationType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencySuperclassWarning

internal class MissingDependencySuperclassInTypeArgumentImpl(
    override val missingType: KaType,
    override val declarationType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencySuperclassInTypeArgument

internal class MissingDependencyClassInLambdaParameterImpl(
    override val type: KaType,
    override val parameterName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClassInLambdaParameter

internal class MissingDependencyClassInLambdaReceiverImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClassInLambdaReceiver

internal class CreatingAnInstanceOfAbstractClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CreatingAnInstanceOfAbstractClass

internal class NoConstructorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoConstructor

internal class FunctionCallExpectedImpl(
    override val functionName: String,
    override val hasValueParameters: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FunctionCallExpected

internal class IllegalSelectorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalSelector

internal class NoReceiverAllowedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoReceiverAllowed

internal class FunctionExpectedImpl(
    override val expression: String,
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FunctionExpected

internal class InterfaceAsFunctionImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InterfaceAsFunction

internal class ExpectClassAsFunctionImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpectClassAsFunction

internal class InnerClassConstructorNoReceiverImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InnerClassConstructorNoReceiver

internal class PluginAmbiguousInterceptedSymbolImpl(
    override val names: List<String>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PluginAmbiguousInterceptedSymbol

internal class ResolutionToClassifierImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ResolutionToClassifier

internal class AmbiguousAlteredAssignImpl(
    override val altererNames: List<String?>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousAlteredAssign

internal class SelfCallInNestedObjectConstructorErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SelfCallInNestedObjectConstructorError

internal class SuperIsNotAnExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuperIsNotAnExpression

internal class SuperNotAvailableImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuperNotAvailable

internal class AbstractSuperCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AbstractSuperCall

internal class AbstractSuperCallWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AbstractSuperCallWarning

internal class InstanceAccessBeforeSuperCallImpl(
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InstanceAccessBeforeSuperCall

internal class SuperCallWithDefaultParametersImpl(
    override val name: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuperCallWithDefaultParameters

internal class InterfaceCantCallDefaultMethodViaSuperImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InterfaceCantCallDefaultMethodViaSuper

internal class JavaClassInheritsKtPrivateClassImpl(
    override val javaClassId: ClassId,
    override val privateKotlinType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JavaClassInheritsKtPrivateClass

internal class NotASupertypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotASupertype

internal class TypeArgumentsRedundantInSuperQualifierImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentsRedundantInSuperQualifier

internal class SuperclassNotAccessibleFromInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuperclassNotAccessibleFromInterface

internal class SupertypeInitializedInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeInitializedInInterface

internal class InterfaceWithSuperclassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InterfaceWithSuperclass

internal class FinalSupertypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.FinalSupertype

internal class ClassCannotBeExtendedDirectlyImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ClassCannotBeExtendedDirectly

internal class SupertypeIsExtensionOrContextFunctionTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeIsExtensionOrContextFunctionType

internal class SingletonInSupertypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SingletonInSupertype

internal class NullableSupertypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableSupertype

internal class NullableSupertypeThroughTypealiasErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableSupertypeThroughTypealiasError

internal class NullableSupertypeThroughTypealiasWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableSupertypeThroughTypealiasWarning

internal class ManyClassesInSupertypeListImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ManyClassesInSupertypeList

internal class SupertypeAppearsTwiceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeAppearsTwice

internal class ClassInSupertypeForEnumImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ClassInSupertypeForEnum

internal class SealedSupertypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SealedSupertype

internal class SealedSupertypeInLocalClassImpl(
    override val declarationType: String,
    override val sealedClassKind: ClassKind,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SealedSupertypeInLocalClass

internal class SealedInheritorInDifferentPackageImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SealedInheritorInDifferentPackage

internal class SealedInheritorInDifferentModuleImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SealedInheritorInDifferentModule

internal class ClassInheritsJavaSealedClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ClassInheritsJavaSealedClass

internal class UnsupportedSealedFunInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedSealedFunInterface

internal class SupertypeNotAClassOrInterfaceImpl(
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeNotAClassOrInterface

internal class UnsupportedInheritanceFromJavaMemberReferencingKotlinFunctionImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction

internal class CyclicInheritanceHierarchyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CyclicInheritanceHierarchy

internal class ExpandedTypeCannotBeInheritedImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ExpandedTypeCannotBeInherited

internal class ProjectionInImmediateArgumentToSupertypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ProjectionInImmediateArgumentToSupertype

internal class InconsistentTypeParameterValuesImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val type: KaClassLikeSymbol,
    override val bounds: List<KaType>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.InconsistentTypeParameterValues

internal class InconsistentTypeParameterBoundsImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val type: KaClassLikeSymbol,
    override val bounds: List<KaType>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InconsistentTypeParameterBounds

internal class AmbiguousSuperImpl(
    override val candidates: List<KaType>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSuperExpression>(firDiagnostic, token), KaFirDiagnostic.AmbiguousSuper

internal class WrongMultipleInheritanceImpl(
    override val symbol: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongMultipleInheritance

internal class ConstructorInObjectImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ConstructorInObject

internal class ConstructorInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ConstructorInInterface

internal class NonPrivateConstructorInEnumImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonPrivateConstructorInEnum

internal class NonPrivateOrProtectedConstructorInSealedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonPrivateOrProtectedConstructorInSealed

internal class CyclicConstructorDelegationCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CyclicConstructorDelegationCall

internal class PrimaryConstructorDelegationCallExpectedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PrimaryConstructorDelegationCallExpected

internal class ProtectedConstructorNotInSuperCallImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ProtectedConstructorNotInSuperCall

internal class SupertypeNotInitializedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeNotInitialized

internal class SupertypeInitializedWithoutPrimaryConstructorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeInitializedWithoutPrimaryConstructor

internal class DelegationSuperCallInEnumConstructorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegationSuperCallInEnumConstructor

internal class ExplicitDelegationCallRequiredImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExplicitDelegationCallRequired

internal class SealedClassConstructorCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SealedClassConstructorCall

internal class DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotationsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations

internal class DataClassConsistentCopyWrongAnnotationTargetImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DataClassConsistentCopyWrongAnnotationTarget

internal class DataClassCopyVisibilityWillBeChangedErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPrimaryConstructor>(firDiagnostic, token), KaFirDiagnostic.DataClassCopyVisibilityWillBeChangedError

internal class DataClassCopyVisibilityWillBeChangedWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPrimaryConstructor>(firDiagnostic, token), KaFirDiagnostic.DataClassCopyVisibilityWillBeChangedWarning

internal class DataClassInvisibleCopyUsageErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNameReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.DataClassInvisibleCopyUsageError

internal class DataClassInvisibleCopyUsageWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNameReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.DataClassInvisibleCopyUsageWarning

internal class DataClassWithoutParametersImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.DataClassWithoutParameters

internal class DataClassVarargParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.DataClassVarargParameter

internal class DataClassNotPropertyParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.DataClassNotPropertyParameter

internal class AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationArgumentKclassLiteralOfTypeParameterError

internal class AnnotationArgumentMustBeConstImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationArgumentMustBeConst

internal class AnnotationArgumentMustBeEnumConstImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationArgumentMustBeEnumConst

internal class AnnotationArgumentMustBeKclassLiteralImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationArgumentMustBeKclassLiteral

internal class AnnotationClassMemberImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AnnotationClassMember

internal class AnnotationParameterDefaultValueMustBeConstantImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationParameterDefaultValueMustBeConstant

internal class InvalidTypeOfAnnotationMemberImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InvalidTypeOfAnnotationMember

internal class ProjectionInTypeOfAnnotationMemberErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ProjectionInTypeOfAnnotationMemberError

internal class ProjectionInTypeOfAnnotationMemberWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ProjectionInTypeOfAnnotationMemberWarning

internal class LocalAnnotationClassErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.LocalAnnotationClassError

internal class MissingValOnAnnotationParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.MissingValOnAnnotationParameter

internal class NonConstValUsedInConstantExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NonConstValUsedInConstantExpression

internal class CycleInAnnotationParameterErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.CycleInAnnotationParameterError

internal class CycleInAnnotationParameterWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.CycleInAnnotationParameterWarning

internal class AnnotationClassConstructorCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.AnnotationClassConstructorCall

internal class EnumClassConstructorCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.EnumClassConstructorCall

internal class NotAnAnnotationClassImpl(
    override val annotationName: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotAnAnnotationClass

internal class NullableTypeOfAnnotationMemberImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableTypeOfAnnotationMember

internal class VarAnnotationParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.VarAnnotationParameter

internal class SupertypesForAnnotationClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClass>(firDiagnostic, token), KaFirDiagnostic.SupertypesForAnnotationClass

internal class AnnotationUsedAsAnnotationArgumentImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationUsedAsAnnotationArgument

internal class AnnotationOnAnnotationArgumentImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationOnAnnotationArgument

internal class IllegalKotlinVersionStringValueImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.IllegalKotlinVersionStringValue

internal class NewerVersionInSinceKotlinImpl(
    override val specifiedVersion: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NewerVersionInSinceKotlin

internal class DeprecatedSinceKotlinWithUnorderedVersionsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinWithUnorderedVersions

internal class DeprecatedSinceKotlinWithoutArgumentsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinWithoutArguments

internal class DeprecatedSinceKotlinWithoutDeprecatedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinWithoutDeprecated

internal class DeprecatedSinceKotlinWithDeprecatedLevelImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinWithDeprecatedLevel

internal class DeprecatedSinceKotlinOutsideKotlinSubpackageImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinOutsideKotlinSubpackage

internal class KotlinActualAnnotationHasNoEffectInKotlinImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.KotlinActualAnnotationHasNoEffectInKotlin

internal class OverrideDeprecationImpl(
    override val overridenSymbol: KaSymbol,
    override val deprecationInfo: FirDeprecationInfo,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.OverrideDeprecation

internal class RedundantAnnotationImpl(
    override val annotation: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RedundantAnnotation

internal class AnnotationOnSuperclassErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationOnSuperclassError

internal class RestrictedRetentionForExpressionAnnotationErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RestrictedRetentionForExpressionAnnotationError

internal class WrongAnnotationTargetImpl(
    override val actualTarget: String,
    override val allowedTargets: List<KotlinTarget>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongAnnotationTarget

internal class WrongAnnotationTargetWarningImpl(
    override val actualTarget: String,
    override val allowedTargets: List<KotlinTarget>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongAnnotationTargetWarning

internal class WrongAnnotationTargetWithUseSiteTargetImpl(
    override val actualTarget: String,
    override val useSiteTarget: String,
    override val allowedTargets: List<KotlinTarget>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongAnnotationTargetWithUseSiteTarget

internal class AnnotationWithUseSiteTargetOnExpressionErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationWithUseSiteTargetOnExpressionError

internal class AnnotationWithUseSiteTargetOnExpressionWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationWithUseSiteTargetOnExpressionWarning

internal class InapplicableTargetOnPropertyImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetOnProperty

internal class InapplicableTargetOnPropertyWarningImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetOnPropertyWarning

internal class InapplicableTargetPropertyImmutableImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetPropertyImmutable

internal class InapplicableTargetPropertyHasNoDelegateImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetPropertyHasNoDelegate

internal class InapplicableTargetPropertyHasNoBackingFieldImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetPropertyHasNoBackingField

internal class InapplicableParamTargetImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableParamTarget

internal class RedundantAnnotationTargetImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RedundantAnnotationTarget

internal class InapplicableFileTargetImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableFileTarget

internal class InapplicableAllTargetImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableAllTarget

internal class InapplicableAllTargetInMultiAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableAllTargetInMultiAnnotation

internal class RepeatedAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RepeatedAnnotation

internal class RepeatedAnnotationWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RepeatedAnnotationWarning

internal class NotAClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotAClass

internal class WrongExtensionFunctionTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongExtensionFunctionType

internal class WrongExtensionFunctionTypeWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongExtensionFunctionTypeWarning

internal class AnnotationInWhereClauseErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationInWhereClauseError

internal class AnnotationInContractErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.AnnotationInContractError

internal class CompilerRequiredAnnotationAmbiguityImpl(
    override val typeFromCompilerPhase: KaType,
    override val typeFromTypesPhase: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompilerRequiredAnnotationAmbiguity

internal class AmbiguousAnnotationArgumentImpl(
    override val symbols: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousAnnotationArgument

internal class VolatileOnValueImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.VolatileOnValue

internal class VolatileOnDelegateImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.VolatileOnDelegate

internal class NonSourceAnnotationOnInlinedLambdaExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.NonSourceAnnotationOnInlinedLambdaExpression

internal class PotentiallyNonReportedAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.PotentiallyNonReportedAnnotation

internal class AnnotationWillBeAppliedAlsoToPropertyOrFieldImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationWillBeAppliedAlsoToPropertyOrField

internal class AnnotationsOnBlockLevelExpressionOnTheSameLineImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AnnotationsOnBlockLevelExpressionOnTheSameLine

internal class IgnorabilityAnnotationsWithCheckerDisabledImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.IgnorabilityAnnotationsWithCheckerDisabled

internal class DslMarkerPropagatesToManyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DslMarkerPropagatesToMany

internal class JsModuleProhibitedOnVarImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsModuleProhibitedOnVar

internal class JsModuleProhibitedOnNonNativeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsModuleProhibitedOnNonNative

internal class NestedJsModuleProhibitedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NestedJsModuleProhibited

internal class CallFromUmdMustBeJsModuleAndJsNonModuleImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallFromUmdMustBeJsModuleAndJsNonModule

internal class CallToJsModuleWithoutModuleSystemImpl(
    override val callee: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallToJsModuleWithoutModuleSystem

internal class CallToJsNonModuleWithModuleSystemImpl(
    override val callee: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallToJsNonModuleWithModuleSystem

internal class RuntimeAnnotationNotSupportedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RuntimeAnnotationNotSupported

internal class RuntimeAnnotationOnExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RuntimeAnnotationOnExternalDeclaration

internal class NativeAnnotationsAllowedOnlyOnMemberOrExtensionFunImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun

internal class NativeIndexerKeyShouldBeStringOrNumberImpl(
    override val kind: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NativeIndexerKeyShouldBeStringOrNumber

internal class NativeIndexerWrongParameterCountImpl(
    override val parametersCount: Int,
    override val kind: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NativeIndexerWrongParameterCount

internal class NativeIndexerCanNotHaveDefaultArgumentsImpl(
    override val kind: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NativeIndexerCanNotHaveDefaultArguments

internal class NativeGetterReturnTypeShouldBeNullableImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NativeGetterReturnTypeShouldBeNullable

internal class NativeSetterWrongReturnTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NativeSetterWrongReturnType

internal class JsNameIsNotOnAllAccessorsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameIsNotOnAllAccessors

internal class JsNameProhibitedForNamedNativeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameProhibitedForNamedNative

internal class JsNameProhibitedForOverrideImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameProhibitedForOverride

internal class JsNameOnPrimaryConstructorProhibitedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameOnPrimaryConstructorProhibited

internal class JsNameOnAccessorAndPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameOnAccessorAndProperty

internal class JsNameProhibitedForExtensionPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameProhibitedForExtensionProperty

internal class JsBuiltinNameClashImpl(
    override val name: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsBuiltinNameClash

internal class NameContainsIllegalCharsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NameContainsIllegalChars

internal class JsNameClashImpl(
    override val name: String,
    override val existing: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameClash

internal class JsFakeNameClashImpl(
    override val name: String,
    override val override: KaSymbol,
    override val existing: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsFakeNameClash

internal class WrongJsQualifierImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongJsQualifier

internal class OptInUsageImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInUsage

internal class OptInUsageErrorImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInUsageError

internal class OptInToInheritanceImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInToInheritance

internal class OptInToInheritanceErrorImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInToInheritanceError

internal class OptInOverrideImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInOverride

internal class OptInOverrideErrorImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInOverrideError

internal class OptInCanOnlyBeUsedAsAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInCanOnlyBeUsedAsAnnotation

internal class OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptInImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn

internal class OptInWithoutArgumentsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInWithoutArguments

internal class OptInArgumentIsNotMarkerImpl(
    override val notMarkerClassId: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassLiteralExpression>(firDiagnostic, token), KaFirDiagnostic.OptInArgumentIsNotMarker

internal class OptInMarkerWithWrongTargetImpl(
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerWithWrongTarget

internal class OptInMarkerWithWrongRetentionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerWithWrongRetention

internal class OptInMarkerOnWrongTargetImpl(
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerOnWrongTarget

internal class OptInMarkerOnOverrideImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerOnOverride

internal class OptInMarkerOnOverrideWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerOnOverrideWarning

internal class SubclassOptInInapplicableImpl(
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SubclassOptInInapplicable

internal class SubclassOptInArgumentIsNotMarkerImpl(
    override val notMarkerClassId: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassLiteralExpression>(firDiagnostic, token), KaFirDiagnostic.SubclassOptInArgumentIsNotMarker

internal class ExposedTypealiasExpandedTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExposedTypealiasExpandedType

internal class ExposedFunctionReturnTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExposedFunctionReturnType

internal class ExposedReceiverTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedReceiverType

internal class ExposedPropertyTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExposedPropertyType

internal class ExposedPropertyTypeInConstructorErrorImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExposedPropertyTypeInConstructorError

internal class ExposedParameterTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ExposedParameterType

internal class ExposedSuperInterfaceImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedSuperInterface

internal class ExposedSuperClassImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedSuperClass

internal class ExposedTypeParameterBoundImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedTypeParameterBound

internal class ExposedTypeParameterBoundDeprecationWarningImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedTypeParameterBoundDeprecationWarning

internal class ExposedPackagePrivateTypeFromInternalWarningImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedPackagePrivateTypeFromInternalWarning

internal class InapplicableInfixModifierImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableInfixModifier

internal class RepeatedModifierImpl(
    override val modifier: KtModifierKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RepeatedModifier

internal class RedundantModifierImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantModifier

internal class DeprecatedModifierImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val actualModifier: KtModifierKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedModifier

internal class DeprecatedModifierPairImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedModifierPair

internal class DeprecatedModifierForTargetImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedModifierForTarget

internal class RedundantModifierForTargetImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantModifierForTarget

internal class IncompatibleModifiersImpl(
    override val modifier1: KtModifierKeywordToken,
    override val modifier2: KtModifierKeywordToken,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleModifiers

internal class RedundantOpenInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.RedundantOpenInInterface

internal class WrongModifierTargetImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongModifierTarget

internal class OperatorModifierRequiredImpl(
    override val functionSymbol: KaFunctionSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OperatorModifierRequired

internal class OperatorCallOnConstructorImpl(
    override val name: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OperatorCallOnConstructor

internal class InfixModifierRequiredImpl(
    override val functionSymbol: KaFunctionSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InfixModifierRequired

internal class WrongModifierContainingDeclarationImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongModifierContainingDeclaration

internal class DeprecatedModifierContainingDeclarationImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedModifierContainingDeclaration

internal class InapplicableOperatorModifierImpl(
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableOperatorModifier

internal class InapplicableOperatorModifierWarningImpl(
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableOperatorModifierWarning

internal class NoExplicitVisibilityInApiModeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoExplicitVisibilityInApiMode

internal class NoExplicitVisibilityInApiModeWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoExplicitVisibilityInApiModeWarning

internal class NoExplicitReturnTypeInApiModeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoExplicitReturnTypeInApiMode

internal class NoExplicitReturnTypeInApiModeWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoExplicitReturnTypeInApiModeWarning

internal class AnonymousSuspendFunctionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.AnonymousSuspendFunction

internal class ValueClassNotTopLevelImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ValueClassNotTopLevel

internal class ValueClassNotFinalImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ValueClassNotFinal

internal class AbsenceOfPrimaryConstructorForValueClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.AbsenceOfPrimaryConstructorForValueClass

internal class InlineClassConstructorWrongParametersSizeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InlineClassConstructorWrongParametersSize

internal class ValueClassEmptyConstructorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassEmptyConstructor

internal class ValueClassConstructorNotFinalReadOnlyParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValueClassConstructorNotFinalReadOnlyParameter

internal class PropertyWithBackingFieldInsideValueClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.PropertyWithBackingFieldInsideValueClass

internal class DelegatedPropertyInsideValueClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegatedPropertyInsideValueClass

internal class ValueClassHasInapplicableParameterTypeImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassHasInapplicableParameterType

internal class ValueClassCannotImplementInterfaceByDelegationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotImplementInterfaceByDelegation

internal class ValueClassCannotExtendClassesImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotExtendClasses

internal class ValueClassCannotBeRecursiveImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotBeRecursive

internal class MultiFieldValueClassPrimaryConstructorDefaultParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.MultiFieldValueClassPrimaryConstructorDefaultParameter

internal class SecondaryConstructorWithBodyInsideValueClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SecondaryConstructorWithBodyInsideValueClass

internal class ReservedMemberInsideValueClassImpl(
    override val name: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.ReservedMemberInsideValueClass

internal class ReservedMemberFromInterfaceInsideValueClassImpl(
    override val interfaceName: String,
    override val methodName: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClass>(firDiagnostic, token), KaFirDiagnostic.ReservedMemberFromInterfaceInsideValueClass

internal class TypeArgumentOnTypedValueClassEqualsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentOnTypedValueClassEquals

internal class InnerClassInsideValueClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InnerClassInsideValueClass

internal class ValueClassCannotBeCloneableImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotBeCloneable

internal class ValueClassCannotHaveContextReceiversImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotHaveContextReceivers

internal class AnnotationOnIllegalMultiFieldValueClassTypedTargetImpl(
    override val name: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationOnIllegalMultiFieldValueClassTypedTarget

internal class NoneApplicableImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoneApplicable

internal class InapplicableCandidateImpl(
    override val candidate: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableCandidate

internal class TypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeMismatch

internal class TypeInferenceOnlyInputTypesErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeInferenceOnlyInputTypesError

internal class ThrowableTypeMismatchImpl(
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ThrowableTypeMismatch

internal class ConditionTypeMismatchImpl(
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConditionTypeMismatch

internal class ArgumentTypeMismatchImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ArgumentTypeMismatch

internal class MemberProjectedOutImpl(
    override val receiver: KaType,
    override val projection: String,
    override val symbol: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MemberProjectedOut

internal class NullForNonnullTypeImpl(
    override val expectedType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NullForNonnullType

internal class InapplicableLateinitModifierImpl(
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.InapplicableLateinitModifier

internal class VarargOutsideParenthesesImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.VarargOutsideParentheses

internal class NamedArgumentsNotAllowedImpl(
    override val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtValueArgument>(firDiagnostic, token), KaFirDiagnostic.NamedArgumentsNotAllowed

internal class NonVarargSpreadImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<LeafPsiElement>(firDiagnostic, token), KaFirDiagnostic.NonVarargSpread

internal class ArgumentPassedTwiceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtValueArgument>(firDiagnostic, token), KaFirDiagnostic.ArgumentPassedTwice

internal class TooManyArgumentsImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TooManyArguments

internal class UnexpectedTrailingLambdaOnANewLineImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnexpectedTrailingLambdaOnANewLine

internal class NoValueForParameterImpl(
    override val violatedParameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NoValueForParameter

internal class NamedParameterNotFoundImpl(
    override val name: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtValueArgument>(firDiagnostic, token), KaFirDiagnostic.NamedParameterNotFound

internal class NameForAmbiguousParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtValueArgument>(firDiagnostic, token), KaFirDiagnostic.NameForAmbiguousParameter

internal class MixingNamedAndPositionalArgumentsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MixingNamedAndPositionalArguments

internal class AssignmentTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssignmentTypeMismatch

internal class ResultTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ResultTypeMismatch

internal class ManyLambdaExpressionArgumentsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtLambdaExpression>(firDiagnostic, token), KaFirDiagnostic.ManyLambdaExpressionArguments

internal class SpreadOfNullableImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SpreadOfNullable

internal class AssigningSingleElementToVarargInNamedFormFunctionErrorImpl(
    override val expectedArrayType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssigningSingleElementToVarargInNamedFormFunctionError

internal class AssigningSingleElementToVarargInNamedFormFunctionWarningImpl(
    override val expectedArrayType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssigningSingleElementToVarargInNamedFormFunctionWarning

internal class AssigningSingleElementToVarargInNamedFormAnnotationErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssigningSingleElementToVarargInNamedFormAnnotationError

internal class AssigningSingleElementToVarargInNamedFormAnnotationWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssigningSingleElementToVarargInNamedFormAnnotationWarning

internal class RedundantSpreadOperatorInNamedFormInAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.RedundantSpreadOperatorInNamedFormInAnnotation

internal class RedundantSpreadOperatorInNamedFormInFunctionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.RedundantSpreadOperatorInNamedFormInFunction

internal class NestedClassAccessedViaInstanceReferenceImpl(
    override val symbol: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NestedClassAccessedViaInstanceReference

internal class CompareToTypeMismatchImpl(
    override val actualType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CompareToTypeMismatch

internal class HasNextFunctionTypeMismatchImpl(
    override val actualType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.HasNextFunctionTypeMismatch

internal class IllegalTypeArgumentForVarargParameterWarningImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IllegalTypeArgumentForVarargParameterWarning

internal class OverloadResolutionAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OverloadResolutionAmbiguity

internal class AssignOperatorAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AssignOperatorAmbiguity

internal class IteratorAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IteratorAmbiguity

internal class HasNextFunctionAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.HasNextFunctionAmbiguity

internal class NextAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NextAmbiguity

internal class AmbiguousFunctionTypeKindImpl(
    override val kinds: List<FunctionTypeKind>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousFunctionTypeKind

internal class NoContextArgumentImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NoContextArgument

internal class AmbiguousContextArgumentImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousContextArgument

internal class AmbiguousCallWithImplicitContextReceiverImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousCallWithImplicitContextReceiver

internal class UnsupportedContextualDeclarationCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedContextualDeclarationCall

internal class SubtypingBetweenContextReceiversImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SubtypingBetweenContextReceivers

internal class ContextParametersWithBackingFieldImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextParametersWithBackingField

internal class ContextReceiversDeprecatedImpl(
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextReceiversDeprecated

internal class ContextClassOrConstructorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextClassOrConstructor

internal class ContextParameterWithoutNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtContextReceiver>(firDiagnostic, token), KaFirDiagnostic.ContextParameterWithoutName

internal class ContextParameterWithDefaultImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextParameterWithDefault

internal class CallableReferenceToContextualDeclarationImpl(
    override val symbol: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceToContextualDeclaration

internal class MultipleContextListsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleContextLists

internal class NamedContextParameterInFunctionTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NamedContextParameterInFunctionType

internal class ContextualOverloadShadowedImpl(
    override val symbols: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextualOverloadShadowed

internal class RecursionInImplicitTypesImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RecursionInImplicitTypes

internal class InferenceErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InferenceError

internal class ProjectionOnNonClassTypeArgumentImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ProjectionOnNonClassTypeArgument

internal class UpperBoundViolatedImpl(
    override val expectedUpperBound: KaType,
    override val actualUpperBound: KaType,
    override val extraMessage: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolated

internal class UpperBoundViolatedDeprecationWarningImpl(
    override val expectedUpperBound: KaType,
    override val actualUpperBound: KaType,
    override val extraMessage: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedDeprecationWarning

internal class UpperBoundViolatedInTypealiasExpansionImpl(
    override val expectedUpperBound: KaType,
    override val actualUpperBound: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInTypealiasExpansion

internal class UpperBoundViolatedInTypealiasExpansionDeprecationWarningImpl(
    override val expectedUpperBound: KaType,
    override val actualUpperBound: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInTypealiasExpansionDeprecationWarning

internal class TypeArgumentsNotAllowedImpl(
    override val place: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentsNotAllowed

internal class TypeArgumentsForOuterClassWhenNestedReferencedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentsForOuterClassWhenNestedReferenced

internal class WrongNumberOfTypeArgumentsImpl(
    override val expectedCount: Int,
    override val owner: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongNumberOfTypeArguments

internal class NoTypeArgumentsOnRhsImpl(
    override val expectedCount: Int,
    override val classifier: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoTypeArgumentsOnRhs

internal class OuterClassArgumentsRequiredImpl(
    override val outer: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OuterClassArgumentsRequired

internal class TypeParametersInObjectImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParametersInObject

internal class TypeParametersInAnonymousObjectImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParametersInAnonymousObject

internal class IllegalProjectionUsageImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalProjectionUsage

internal class TypeParametersInEnumImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParametersInEnum

internal class ConflictingProjectionImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeProjection>(firDiagnostic, token), KaFirDiagnostic.ConflictingProjection

internal class ConflictingProjectionInTypealiasExpansionImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConflictingProjectionInTypealiasExpansion

internal class RedundantProjectionImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeProjection>(firDiagnostic, token), KaFirDiagnostic.RedundantProjection

internal class VarianceOnTypeParameterNotAllowedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.VarianceOnTypeParameterNotAllowed

internal class CatchParameterWithDefaultValueImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CatchParameterWithDefaultValue

internal class TypeParameterInCatchClauseImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParameterInCatchClause

internal class GenericThrowableSubclassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.GenericThrowableSubclass

internal class InnerClassOfGenericThrowableSubclassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.InnerClassOfGenericThrowableSubclass

internal class KclassWithNullableTypeParameterInSignatureImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.KclassWithNullableTypeParameterInSignature

internal class TypeParameterAsReifiedImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParameterAsReified

internal class TypeParameterAsReifiedArrayErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParameterAsReifiedArrayError

internal class ReifiedTypeForbiddenSubstitutionImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeForbiddenSubstitution

internal class DefinitelyNonNullableAsReifiedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DefinitelyNonNullableAsReified

internal class TypeIntersectionAsReifiedErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val types: List<KaType>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeIntersectionAsReifiedError

internal class TypeIntersectionAsReifiedWarningImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val types: List<KaType>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeIntersectionAsReifiedWarning

internal class FinalUpperBoundImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.FinalUpperBound

internal class UpperBoundIsExtensionOrContextFunctionTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundIsExtensionOrContextFunctionType

internal class BoundsNotAllowedIfBoundedByTypeParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.BoundsNotAllowedIfBoundedByTypeParameter

internal class OnlyOneClassBoundAllowedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.OnlyOneClassBoundAllowed

internal class RepeatedBoundImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RepeatedBound

internal class ConflictingUpperBoundsImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ConflictingUpperBounds

internal class NameInConstraintIsNotATypeParameterImpl(
    override val typeParameterName: Name,
    override val typeParametersOwner: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSimpleNameExpression>(firDiagnostic, token), KaFirDiagnostic.NameInConstraintIsNotATypeParameter

internal class BoundOnTypeAliasParameterNotAllowedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.BoundOnTypeAliasParameterNotAllowed

internal class ReifiedTypeParameterNoInlineImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeParameterNoInline

internal class ReifiedTypeParameterOnAliasErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeParameterOnAliasError

internal class ReifiedTypeParameterOnAliasWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeParameterOnAliasWarning

internal class TypeParametersNotAllowedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.TypeParametersNotAllowed

internal class TypeParameterOfPropertyNotUsedInReceiverImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.TypeParameterOfPropertyNotUsedInReceiver

internal class ReturnTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val targetFunction: KaSymbol,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatch

internal class ImplicitNothingReturnTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ImplicitNothingReturnType

internal class ImplicitNothingPropertyTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ImplicitNothingPropertyType

internal class AbbreviatedNothingReturnTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AbbreviatedNothingReturnType

internal class AbbreviatedNothingPropertyTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AbbreviatedNothingPropertyType

internal class CyclicGenericUpperBoundImpl(
    override val typeParameters: List<KaTypeParameterSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CyclicGenericUpperBound

internal class FiniteBoundsViolationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FiniteBoundsViolation

internal class FiniteBoundsViolationInJavaImpl(
    override val containingTypes: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FiniteBoundsViolationInJava

internal class ExpansiveInheritanceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpansiveInheritance

internal class ExpansiveInheritanceInJavaImpl(
    override val containingTypes: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpansiveInheritanceInJava

internal class DeprecatedTypeParameterSyntaxImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.DeprecatedTypeParameterSyntax

internal class MisplacedTypeParameterConstraintsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.MisplacedTypeParameterConstraints

internal class DynamicSupertypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DynamicSupertype

internal class DynamicUpperBoundImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DynamicUpperBound

internal class DynamicReceiverNotAllowedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DynamicReceiverNotAllowed

internal class DynamicReceiverExpectedButWasNonDynamicImpl(
    override val actualType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DynamicReceiverExpectedButWasNonDynamic

internal class IncompatibleTypesImpl(
    override val typeA: KaType,
    override val typeB: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleTypes

internal class IncompatibleTypesWarningImpl(
    override val typeA: KaType,
    override val typeB: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleTypesWarning

internal class TypeVarianceConflictErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeParameterVariance: Variance,
    override val variance: Variance,
    override val containingType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeVarianceConflictError

internal class TypeVarianceConflictInExpandedTypeImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeParameterVariance: Variance,
    override val variance: Variance,
    override val containingType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeVarianceConflictInExpandedType

internal class SmartcastImpossibleImpl(
    override val desiredType: KaType,
    override val subject: KtExpression,
    override val description: String,
    override val isCastToNotNull: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.SmartcastImpossible

internal class SmartcastImpossibleOnImplicitInvokeReceiverImpl(
    override val desiredType: KaType,
    override val subject: KtExpression,
    override val description: String,
    override val isCastToNotNull: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.SmartcastImpossibleOnImplicitInvokeReceiver

internal class DeprecatedSmartcastOnDelegatedPropertyImpl(
    override val desiredType: KaType,
    override val property: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSmartcastOnDelegatedProperty

internal class RedundantNullableImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RedundantNullable

internal class PlatformClassMappedToKotlinImpl(
    override val kotlinClass: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PlatformClassMappedToKotlin

internal class InferredTypeVariableIntoEmptyIntersectionErrorImpl(
    override val typeVariableDescription: String,
    override val incompatibleTypes: List<KaType>,
    override val description: String,
    override val causingTypes: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InferredTypeVariableIntoEmptyIntersectionError

internal class InferredTypeVariableIntoEmptyIntersectionWarningImpl(
    override val typeVariableDescription: String,
    override val incompatibleTypes: List<KaType>,
    override val description: String,
    override val causingTypes: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InferredTypeVariableIntoEmptyIntersectionWarning

internal class InferredTypeVariableIntoPossibleEmptyIntersectionImpl(
    override val typeVariableDescription: String,
    override val incompatibleTypes: List<KaType>,
    override val description: String,
    override val causingTypes: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InferredTypeVariableIntoPossibleEmptyIntersection

internal class IncorrectLeftComponentOfIntersectionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncorrectLeftComponentOfIntersection

internal class IncorrectRightComponentOfIntersectionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncorrectRightComponentOfIntersection

internal class NullableOnDefinitelyNotNullableImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableOnDefinitelyNotNullable

internal class InferredInvisibleReifiedTypeArgumentErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeArgumentType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleReifiedTypeArgumentError

internal class InferredInvisibleReifiedTypeArgumentWarningImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeArgumentType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleReifiedTypeArgumentWarning

internal class InferredInvisibleVarargTypeArgumentErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeArgumentType: KaType,
    override val valueParameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleVarargTypeArgumentError

internal class InferredInvisibleVarargTypeArgumentWarningImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeArgumentType: KaType,
    override val valueParameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleVarargTypeArgumentWarning

internal class InferredInvisibleReturnTypeErrorImpl(
    override val calleeSymbol: KaSymbol,
    override val returnType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleReturnTypeError

internal class InferredInvisibleReturnTypeWarningImpl(
    override val calleeSymbol: KaSymbol,
    override val returnType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleReturnTypeWarning

internal class GenericQualifierOnConstructorCallErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.GenericQualifierOnConstructorCallError

internal class GenericQualifierOnConstructorCallWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.GenericQualifierOnConstructorCallWarning

internal class AtomicRefWithoutConsistentIdentityImpl(
    override val atomicRef: ClassId,
    override val argumentType: KaType,
    override val suggestedType: ClassId?,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AtomicRefWithoutConsistentIdentity

internal class ExtensionInClassReferenceNotAllowedImpl(
    override val referencedDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExtensionInClassReferenceNotAllowed

internal class CallableReferenceLhsNotAClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceLhsNotAClass

internal class CallableReferenceToAnnotationConstructorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceToAnnotationConstructor

internal class AdaptedCallableReferenceAgainstReflectionTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AdaptedCallableReferenceAgainstReflectionType

internal class ClassLiteralLhsNotAClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ClassLiteralLhsNotAClass

internal class NullableTypeInClassLiteralLhsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NullableTypeInClassLiteralLhs

internal class ExpressionOfNullableTypeInClassLiteralLhsImpl(
    override val lhsType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpressionOfNullableTypeInClassLiteralLhs

internal class UnsupportedClassLiteralsWithEmptyLhsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedClassLiteralsWithEmptyLhs

internal class MutablePropertyWithCapturedTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MutablePropertyWithCapturedType

internal class UnsupportedReflectionApiImpl(
    override val unsupportedReflectionAPI: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedReflectionApi

internal class NothingToOverrideImpl(
    override val declaration: KaCallableSymbol,
    override val candidates: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.NothingToOverride

internal class CannotOverrideInvisibleMemberImpl(
    override val overridingMember: KaCallableSymbol,
    override val baseMember: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.CannotOverrideInvisibleMember

internal class DataClassOverrideConflictImpl(
    override val overridingMember: KaCallableSymbol,
    override val baseMember: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.DataClassOverrideConflict

internal class DataClassOverrideDefaultValuesImpl(
    override val overridingMember: KaCallableSymbol,
    override val baseType: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DataClassOverrideDefaultValues

internal class CannotWeakenAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.CannotWeakenAccessPrivilege

internal class CannotWeakenAccessPrivilegeWarningImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.CannotWeakenAccessPrivilegeWarning

internal class CannotChangeAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.CannotChangeAccessPrivilege

internal class CannotChangeAccessPrivilegeWarningImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.CannotChangeAccessPrivilegeWarning

internal class CannotInferVisibilityImpl(
    override val callable: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CannotInferVisibility

internal class CannotInferVisibilityWarningImpl(
    override val callable: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CannotInferVisibilityWarning

internal class MultipleDefaultsInheritedFromSupertypesImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypes

internal class MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride

internal class MultipleDefaultsInheritedFromSupertypesDeprecationErrorImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesDeprecationError

internal class MultipleDefaultsInheritedFromSupertypesDeprecationWarningImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesDeprecationWarning

internal class MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationErrorImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError

internal class MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarningImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning

internal class TypealiasExpandsToArrayOfNothingsImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpandsToArrayOfNothings

internal class OverridingFinalMemberImpl(
    override val overriddenDeclaration: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.OverridingFinalMember

internal class ReturnTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KaCallableSymbol,
    override val conflictingDeclaration2: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatchOnInheritance

internal class PropertyTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KaCallableSymbol,
    override val conflictingDeclaration2: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.PropertyTypeMismatchOnInheritance

internal class VarTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KaCallableSymbol,
    override val conflictingDeclaration2: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.VarTypeMismatchOnInheritance

internal class ReturnTypeMismatchByDelegationImpl(
    override val delegateDeclaration: KaCallableSymbol,
    override val baseDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatchByDelegation

internal class PropertyTypeMismatchByDelegationImpl(
    override val delegateDeclaration: KaCallableSymbol,
    override val baseDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.PropertyTypeMismatchByDelegation

internal class VarOverriddenByValByDelegationImpl(
    override val delegateDeclaration: KaCallableSymbol,
    override val baseDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.VarOverriddenByValByDelegation

internal class ConflictingInheritedMembersImpl(
    override val owner: KaClassLikeSymbol,
    override val conflictingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ConflictingInheritedMembers

internal class AbstractMemberNotImplementedImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.AbstractMemberNotImplemented

internal class AbstractMemberIncorrectlyDelegatedErrorImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.AbstractMemberIncorrectlyDelegatedError

internal class AbstractMemberIncorrectlyDelegatedWarningImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.AbstractMemberIncorrectlyDelegatedWarning

internal class AbstractMemberNotImplementedByEnumEntryImpl(
    override val enumEntry: KaSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtEnumEntry>(firDiagnostic, token), KaFirDiagnostic.AbstractMemberNotImplementedByEnumEntry

internal class AbstractClassMemberNotImplementedImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.AbstractClassMemberNotImplemented

internal class InvisibleAbstractMemberFromSuperErrorImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val invisibleDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.InvisibleAbstractMemberFromSuperError

internal class AmbiguousAnonymousTypeInferredImpl(
    override val superTypes: List<KaType>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.AmbiguousAnonymousTypeInferred

internal class ManyImplMemberNotImplementedImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ManyImplMemberNotImplemented

internal class ManyInterfacesMemberNotImplementedImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ManyInterfacesMemberNotImplemented

internal class OverridingFinalMemberByDelegationImpl(
    override val delegatedDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.OverridingFinalMemberByDelegation

internal class DelegatedMemberHidesSupertypeOverrideImpl(
    override val delegatedDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.DelegatedMemberHidesSupertypeOverride

internal class ReturnTypeMismatchOnOverrideImpl(
    override val function: KaCallableSymbol,
    override val superFunction: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatchOnOverride

internal class PropertyTypeMismatchOnOverrideImpl(
    override val property: KaCallableSymbol,
    override val superProperty: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.PropertyTypeMismatchOnOverride

internal class VarTypeMismatchOnOverrideImpl(
    override val variable: KaCallableSymbol,
    override val superVariable: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VarTypeMismatchOnOverride

internal class VarOverriddenByValImpl(
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VarOverriddenByVal

internal class VarImplementedByInheritedValErrorImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VarImplementedByInheritedValError

internal class VarImplementedByInheritedValWarningImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VarImplementedByInheritedValWarning

internal class NonFinalMemberInFinalClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NonFinalMemberInFinalClass

internal class NonFinalMemberInObjectImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NonFinalMemberInObject

internal class VirtualMemberHiddenImpl(
    override val declared: KaCallableSymbol,
    override val overriddenContainer: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VirtualMemberHidden

internal class ParameterNameChangedOnOverrideImpl(
    override val superType: KaClassLikeSymbol,
    override val conflictingParameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ParameterNameChangedOnOverride

internal class DifferentNamesForTheSameParameterInSupertypesImpl(
    override val currentParameter: KaSymbol,
    override val conflictingParameter: KaSymbol,
    override val parameterNumber: Int,
    override val conflictingFunctions: List<KaFunctionSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.DifferentNamesForTheSameParameterInSupertypes

internal class SuspendOverriddenByNonSuspendImpl(
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtCallableDeclaration>(firDiagnostic, token), KaFirDiagnostic.SuspendOverriddenByNonSuspend

internal class NonSuspendOverriddenBySuspendImpl(
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtCallableDeclaration>(firDiagnostic, token), KaFirDiagnostic.NonSuspendOverriddenBySuspend

internal class ManyCompanionObjectsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtObjectDeclaration>(firDiagnostic, token), KaFirDiagnostic.ManyCompanionObjects

internal class ConflictingOverloadsImpl(
    override val conflictingOverloads: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConflictingOverloads

internal class RedeclarationImpl(
    override val conflictingDeclarations: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.Redeclaration

internal class ClassifierRedeclarationImpl(
    override val conflictingDeclarations: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ClassifierRedeclaration

internal class PackageConflictsWithClassifierImpl(
    override val conflictingClassId: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPackageDirective>(firDiagnostic, token), KaFirDiagnostic.PackageConflictsWithClassifier

internal class ExpectAndActualInTheSameModuleImpl(
    override val declaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectAndActualInTheSameModule

internal class MethodOfAnyImplementedInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MethodOfAnyImplementedInInterface

internal class ExtensionShadowedByMemberImpl(
    override val member: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExtensionShadowedByMember

internal class ExtensionFunctionShadowedByMemberPropertyWithInvokeImpl(
    override val member: KaCallableSymbol,
    override val invokeOperator: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExtensionFunctionShadowedByMemberPropertyWithInvoke

internal class LocalObjectNotAllowedImpl(
    override val objectName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.LocalObjectNotAllowed

internal class LocalInterfaceNotAllowedImpl(
    override val interfaceName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.LocalInterfaceNotAllowed

internal class AbstractFunctionInNonAbstractClassImpl(
    override val function: KaCallableSymbol,
    override val containingClass: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.AbstractFunctionInNonAbstractClass

internal class AbstractFunctionWithBodyImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.AbstractFunctionWithBody

internal class NonAbstractFunctionWithNoBodyImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.NonAbstractFunctionWithNoBody

internal class PrivateFunctionWithNoBodyImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.PrivateFunctionWithNoBody

internal class NonMemberFunctionNoBodyImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.NonMemberFunctionNoBody

internal class FunctionDeclarationWithNoNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.FunctionDeclarationWithNoName

internal class AnonymousFunctionWithNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.AnonymousFunctionWithName

internal class SingleAnonymousFunctionWithNameErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.SingleAnonymousFunctionWithNameError

internal class SingleAnonymousFunctionWithNameWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.SingleAnonymousFunctionWithNameWarning

internal class AnonymousFunctionParameterWithDefaultValueImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.AnonymousFunctionParameterWithDefaultValue

internal class UselessVarargOnParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.UselessVarargOnParameter

internal class MultipleVarargParametersImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.MultipleVarargParameters

internal class ForbiddenVarargParameterTypeImpl(
    override val varargParameterType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ForbiddenVarargParameterType

internal class ValueParameterWithoutExplicitTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValueParameterWithoutExplicitType

internal class CannotInferParameterTypeImpl(
    override val parameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotInferParameterType

internal class CannotInferValueParameterTypeImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotInferValueParameterType

internal class CannotInferItParameterTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotInferItParameterType

internal class CannotInferReceiverParameterTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotInferReceiverParameterType

internal class NoTailCallsFoundImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.NoTailCallsFound

internal class TailrecOnVirtualMemberErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.TailrecOnVirtualMemberError

internal class NonTailRecursiveCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonTailRecursiveCall

internal class TailRecursionInTryIsNotSupportedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TailRecursionInTryIsNotSupported

internal class DataObjectCustomEqualsOrHashCodeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.DataObjectCustomEqualsOrHashCode

internal class DefaultValueNotAllowedInOverrideImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DefaultValueNotAllowedInOverride

internal class FunInterfaceWrongCountOfAbstractMembersImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClass>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceWrongCountOfAbstractMembers

internal class FunInterfaceCannotHaveAbstractPropertiesImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceCannotHaveAbstractProperties

internal class FunInterfaceAbstractMethodWithTypeParametersImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceAbstractMethodWithTypeParameters

internal class FunInterfaceAbstractMethodWithDefaultValueImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceAbstractMethodWithDefaultValue

internal class FunInterfaceWithSuspendFunctionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceWithSuspendFunction

internal class AbstractPropertyInNonAbstractClassImpl(
    override val property: KaCallableSymbol,
    override val containingClass: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyInNonAbstractClass

internal class PrivatePropertyInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.PrivatePropertyInInterface

internal class AbstractPropertyWithInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyWithInitializer

internal class PropertyInitializerInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.PropertyInitializerInInterface

internal class PropertyWithNoTypeNoInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.PropertyWithNoTypeNoInitializer

internal class AbstractPropertyWithoutTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyWithoutType

internal class LateinitPropertyWithoutTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.LateinitPropertyWithoutType

internal class MustBeInitializedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitialized

internal class MustBeInitializedWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedWarning

internal class MustBeInitializedOrBeFinalImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrBeFinal

internal class MustBeInitializedOrBeFinalWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrBeFinalWarning

internal class MustBeInitializedOrBeAbstractImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrBeAbstract

internal class MustBeInitializedOrBeAbstractWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrBeAbstractWarning

internal class MustBeInitializedOrFinalOrAbstractImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrFinalOrAbstract

internal class MustBeInitializedOrFinalOrAbstractWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrFinalOrAbstractWarning

internal class ExtensionPropertyMustHaveAccessorsOrBeAbstractImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.ExtensionPropertyMustHaveAccessorsOrBeAbstract

internal class UnnecessaryLateinitImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.UnnecessaryLateinit

internal class BackingFieldInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.BackingFieldInInterface

internal class ExtensionPropertyWithBackingFieldImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExtensionPropertyWithBackingField

internal class PropertyInitializerNoBackingFieldImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.PropertyInitializerNoBackingField

internal class AbstractDelegatedPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AbstractDelegatedProperty

internal class DelegatedPropertyInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegatedPropertyInInterface

internal class AbstractPropertyWithGetterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPropertyAccessor>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyWithGetter

internal class AbstractPropertyWithSetterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPropertyAccessor>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyWithSetter

internal class PrivateSetterForAbstractPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.PrivateSetterForAbstractProperty

internal class PrivateSetterForOpenPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.PrivateSetterForOpenProperty

internal class ValWithSetterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPropertyAccessor>(firDiagnostic, token), KaFirDiagnostic.ValWithSetter

internal class ConstValNotTopLevelOrObjectImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConstValNotTopLevelOrObject

internal class ConstValWithGetterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConstValWithGetter

internal class ConstValWithDelegateImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ConstValWithDelegate

internal class TypeCantBeUsedForConstValImpl(
    override val constValType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.TypeCantBeUsedForConstVal

internal class ConstValWithoutInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.ConstValWithoutInitializer

internal class ConstValWithNonConstInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ConstValWithNonConstInitializer

internal class WrongSetterParameterTypeImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongSetterParameterType

internal class DelegateUsesExtensionPropertyTypeParameterErrorImpl(
    override val usedTypeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.DelegateUsesExtensionPropertyTypeParameterError

internal class DelegateUsesExtensionPropertyTypeParameterWarningImpl(
    override val usedTypeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.DelegateUsesExtensionPropertyTypeParameterWarning

internal class InitializerTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.InitializerTypeMismatch

internal class GetterVisibilityDiffersFromPropertyVisibilityImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.GetterVisibilityDiffersFromPropertyVisibility

internal class SetterVisibilityInconsistentWithPropertyVisibilityImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.SetterVisibilityInconsistentWithPropertyVisibility

internal class WrongSetterReturnTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongSetterReturnType

internal class WrongGetterReturnTypeImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongGetterReturnType

internal class AccessorForDelegatedPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPropertyAccessor>(firDiagnostic, token), KaFirDiagnostic.AccessorForDelegatedProperty

internal class PropertyInitializerWithExplicitFieldDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.PropertyInitializerWithExplicitFieldDeclaration

internal class PropertyFieldDeclarationMissingInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.PropertyFieldDeclarationMissingInitializer

internal class LateinitPropertyFieldDeclarationWithInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.LateinitPropertyFieldDeclarationWithInitializer

internal class LateinitFieldInValPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.LateinitFieldInValProperty

internal class LateinitNullableBackingFieldImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.LateinitNullableBackingField

internal class BackingFieldForDelegatedPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.BackingFieldForDelegatedProperty

internal class PropertyMustHaveGetterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.PropertyMustHaveGetter

internal class PropertyMustHaveSetterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.PropertyMustHaveSetter

internal class ExplicitBackingFieldInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.ExplicitBackingFieldInInterface

internal class ExplicitBackingFieldInAbstractPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.ExplicitBackingFieldInAbstractProperty

internal class ExplicitBackingFieldInExtensionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.ExplicitBackingFieldInExtension

internal class RedundantExplicitBackingFieldImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.RedundantExplicitBackingField

internal class AbstractPropertyInPrimaryConstructorParametersImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyInPrimaryConstructorParameters

internal class LocalVariableWithTypeParametersWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.LocalVariableWithTypeParametersWarning

internal class LocalVariableWithTypeParametersImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.LocalVariableWithTypeParameters

internal class ExplicitTypeArgumentsInPropertyAccessImpl(
    override val kind: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExplicitTypeArgumentsInPropertyAccess

internal class SafeCallableReferenceCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.SafeCallableReferenceCall

internal class LateinitIntrinsicCallOnNonLiteralImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LateinitIntrinsicCallOnNonLiteral

internal class LateinitIntrinsicCallOnNonLateinitImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LateinitIntrinsicCallOnNonLateinit

internal class LateinitIntrinsicCallInInlineFunctionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LateinitIntrinsicCallInInlineFunction

internal class LateinitIntrinsicCallOnNonAccessiblePropertyImpl(
    override val declaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LateinitIntrinsicCallOnNonAccessibleProperty

internal class LocalExtensionPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LocalExtensionProperty

internal class UnnamedVarPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnnamedVarProperty

internal class UnnamedDelegatedPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnnamedDelegatedProperty

internal class ExpectedDeclarationWithBodyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectedDeclarationWithBody

internal class ExpectedClassConstructorDelegationCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtConstructorDelegationCall>(firDiagnostic, token), KaFirDiagnostic.ExpectedClassConstructorDelegationCall

internal class ExpectedClassConstructorPropertyParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ExpectedClassConstructorPropertyParameter

internal class ExpectedEnumConstructorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtConstructor<*>>(firDiagnostic, token), KaFirDiagnostic.ExpectedEnumConstructor

internal class ExpectedEnumEntryWithBodyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtEnumEntry>(firDiagnostic, token), KaFirDiagnostic.ExpectedEnumEntryWithBody

internal class ExpectedPropertyInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExpectedPropertyInitializer

internal class ExpectedDelegatedPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExpectedDelegatedProperty

internal class ExpectedLateinitPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ExpectedLateinitProperty

internal class SupertypeInitializedInExpectedClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeInitializedInExpectedClass

internal class ExpectedPrivateDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ExpectedPrivateDeclaration

internal class ExpectedExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ExpectedExternalDeclaration

internal class ExpectedTailrecFunctionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ExpectedTailrecFunction

internal class ImplementationByDelegationInExpectClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDelegatedSuperTypeEntry>(firDiagnostic, token), KaFirDiagnostic.ImplementationByDelegationInExpectClass

internal class ActualTypeAliasNotToClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasNotToClass

internal class ActualTypeAliasToClassWithDeclarationSiteVarianceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasToClassWithDeclarationSiteVariance

internal class ActualTypeAliasWithUseSiteVarianceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasWithUseSiteVariance

internal class ActualTypeAliasWithComplexSubstitutionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasWithComplexSubstitution

internal class ActualTypeAliasToNullableTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasToNullableType

internal class ActualTypeAliasToNothingImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasToNothing

internal class ActualFunctionWithDefaultArgumentsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.ActualFunctionWithDefaultArguments

internal class DefaultArgumentsInExpectWithActualTypealiasImpl(
    override val expectClassSymbol: KaClassLikeSymbol,
    override val members: List<KaCallableSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.DefaultArgumentsInExpectWithActualTypealias

internal class DefaultArgumentsInExpectActualizedByFakeOverrideImpl(
    override val expectClassSymbol: KaClassLikeSymbol,
    override val members: List<KaFunctionSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClass>(firDiagnostic, token), KaFirDiagnostic.DefaultArgumentsInExpectActualizedByFakeOverride

internal class ExpectedFunctionSourceWithDefaultArgumentsNotFoundImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpectedFunctionSourceWithDefaultArgumentsNotFound

internal class ActualWithoutExpectImpl(
    override val declaration: KaSymbol,
    override val compatibility: Map<ExpectActualMatchingCompatibility, List<KaSymbol>>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ActualWithoutExpect

internal class ExpectActualIncompatibleClassTypeParameterCountImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassTypeParameterCount

internal class ExpectActualIncompatibleReturnTypeImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleReturnType

internal class ExpectActualIncompatibleParameterNamesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleParameterNames

internal class ExpectActualIncompatibleContextParameterNamesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleContextParameterNames

internal class ExpectActualIncompatibleTypeParameterNamesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleTypeParameterNames

internal class ExpectActualIncompatibleValueParameterVarargImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleValueParameterVararg

internal class ExpectActualIncompatibleValueParameterNoinlineImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleValueParameterNoinline

internal class ExpectActualIncompatibleValueParameterCrossinlineImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleValueParameterCrossinline

internal class ExpectActualIncompatibleFunctionModifiersDifferentImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleFunctionModifiersDifferent

internal class ExpectActualIncompatibleFunctionModifiersNotSubsetImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleFunctionModifiersNotSubset

internal class ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverrideImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride

internal class ExpectActualIncompatiblePropertyKindImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatiblePropertyKind

internal class ExpectActualIncompatiblePropertyLateinitModifierImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatiblePropertyLateinitModifier

internal class ExpectActualIncompatiblePropertyConstModifierImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatiblePropertyConstModifier

internal class ExpectActualIncompatiblePropertySetterVisibilityImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatiblePropertySetterVisibility

internal class ExpectActualIncompatibleClassKindImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassKind

internal class ExpectActualIncompatibleClassModifiersImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassModifiers

internal class ExpectActualIncompatibleFunInterfaceModifierImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleFunInterfaceModifier

internal class ExpectActualIncompatibleSupertypesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleSupertypes

internal class ExpectActualIncompatibleNestedTypeAliasImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleNestedTypeAlias

internal class ExpectActualIncompatibleEnumEntriesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleEnumEntries

internal class ExpectActualIncompatibleIllegalRequiresOptInImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleIllegalRequiresOptIn

internal class ExpectActualIncompatibleModalityImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleModality

internal class ExpectActualIncompatibleVisibilityImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleVisibility

internal class ExpectActualIncompatibleClassTypeParameterUpperBoundsImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassTypeParameterUpperBounds

internal class ExpectActualIncompatibleTypeParameterVarianceImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleTypeParameterVariance

internal class ExpectActualIncompatibleTypeParameterReifiedImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleTypeParameterReified

internal class ExpectActualIncompatibleClassScopeImpl(
    override val actualClass: KaSymbol,
    override val expectMemberDeclaration: KaSymbol,
    override val actualMemberDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassScope

internal class ExpectRefinementAnnotationWrongTargetImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectRefinementAnnotationWrongTarget

internal class AmbiguousExpectsImpl(
    override val declaration: KaSymbol,
    override val modules: List<FirModuleData>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.AmbiguousExpects

internal class NoActualClassMemberForExpectedClassImpl(
    override val declaration: KaSymbol,
    override val members: List<Pair<KaSymbol, Map<Mismatch, List<KaSymbol>>>>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoActualClassMemberForExpectedClass

internal class ActualMissingImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ActualMissing

internal class ExpectRefinementAnnotationMissingImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectRefinementAnnotationMissing

internal class ExpectActualClassifiersAreInBetaWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassLikeDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualClassifiersAreInBetaWarning

internal class NotAMultiplatformCompilationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotAMultiplatformCompilation

internal class ExpectActualOptInAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualOptInAnnotation

internal class ActualTypealiasToSpecialAnnotationImpl(
    override val typealiasedClassId: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypealiasToSpecialAnnotation

internal class ActualAnnotationsNotMatchExpectImpl(
    override val expectSymbol: KaSymbol,
    override val actualSymbol: KaSymbol,
    override val actualAnnotationTargetSourceElement: PsiElement?,
    override val incompatibilityType: ExpectActualAnnotationsIncompatibilityType<FirAnnotation>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ActualAnnotationsNotMatchExpect

internal class OptionalDeclarationOutsideOfAnnotationEntryImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptionalDeclarationOutsideOfAnnotationEntry

internal class OptionalDeclarationUsageInNonCommonSourceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptionalDeclarationUsageInNonCommonSource

internal class OptionalExpectationNotOnExpectedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptionalExpectationNotOnExpected

internal class InitializerRequiredForDestructuringDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDestructuringDeclaration>(firDiagnostic, token), KaFirDiagnostic.InitializerRequiredForDestructuringDeclaration

internal class ComponentFunctionMissingImpl(
    override val missingFunctionName: Name,
    override val destructingType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ComponentFunctionMissing

internal class ComponentFunctionAmbiguityImpl(
    override val functionWithAmbiguityName: Name,
    override val candidates: List<KaSymbol>,
    override val destructingType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ComponentFunctionAmbiguity

internal class ComponentFunctionOnNullableImpl(
    override val componentFunctionName: Name,
    override val destructingType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ComponentFunctionOnNullable

internal class ComponentFunctionReturnTypeMismatchImpl(
    override val componentFunctionName: Name,
    override val destructingType: KaType,
    override val expectedType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ComponentFunctionReturnTypeMismatch

internal class UninitializedVariableImpl(
    override val variable: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UninitializedVariable

internal class UninitializedParameterImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSimpleNameExpression>(firDiagnostic, token), KaFirDiagnostic.UninitializedParameter

internal class UninitializedEnumEntryImpl(
    override val enumEntry: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UninitializedEnumEntry

internal class UninitializedEnumCompanionImpl(
    override val enumClass: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UninitializedEnumCompanion

internal class ValReassignmentImpl(
    override val variable: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ValReassignment

internal class ValReassignmentViaBackingFieldErrorImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ValReassignmentViaBackingFieldError

internal class CapturedValInitializationImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CapturedValInitialization

internal class CapturedMemberValInitializationImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CapturedMemberValInitialization

internal class NonInlineMemberValInitializationImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NonInlineMemberValInitialization

internal class SetterProjectedOutImpl(
    override val receiverType: KaType,
    override val projection: String,
    override val property: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.SetterProjectedOut

internal class WrongInvocationKindImpl(
    override val declaration: KaSymbol,
    override val requiredRange: EventOccurrencesRange,
    override val actualRange: EventOccurrencesRange,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongInvocationKind

internal class LeakedInPlaceLambdaImpl(
    override val lambda: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LeakedInPlaceLambda

internal class VariableWithNoTypeNoInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtVariableDeclaration>(firDiagnostic, token), KaFirDiagnostic.VariableWithNoTypeNoInitializer

internal class InitializationBeforeDeclarationImpl(
    override val property: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.InitializationBeforeDeclaration

internal class InitializationBeforeDeclarationWarningImpl(
    override val property: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.InitializationBeforeDeclarationWarning

internal class UnreachableCodeImpl(
    override val reachable: List<PsiElement>,
    override val unreachable: List<PsiElement>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnreachableCode

internal class SenselessComparisonImpl(
    override val compareResult: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.SenselessComparison

internal class SenselessNullInWhenImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SenselessNullInWhen

internal class TypecheckerHasRunIntoRecursiveProblemImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.TypecheckerHasRunIntoRecursiveProblem

internal class ReturnValueNotUsedImpl(
    override val functionName: Name?,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ReturnValueNotUsed

internal class UnsafeCallImpl(
    override val receiverType: KaType,
    override val receiverExpression: KtExpression?,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsafeCall

internal class UnsafeImplicitInvokeCallImpl(
    override val receiverType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsafeImplicitInvokeCall

internal class UnsafeInfixCallImpl(
    override val receiverType: KaType,
    override val receiverExpression: KtExpression,
    override val operator: String,
    override val argumentExpression: KtExpression?,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UnsafeInfixCall

internal class UnsafeOperatorCallImpl(
    override val receiverType: KaType,
    override val receiverExpression: KtExpression,
    override val operator: String,
    override val argumentExpression: KtExpression?,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UnsafeOperatorCall

internal class IteratorOnNullableImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.IteratorOnNullable

internal class UnnecessarySafeCallImpl(
    override val receiverType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnnecessarySafeCall

internal class UnexpectedSafeCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnexpectedSafeCall

internal class UnnecessaryNotNullAssertionImpl(
    override val receiverType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UnnecessaryNotNullAssertion

internal class NotNullAssertionOnLambdaExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NotNullAssertionOnLambdaExpression

internal class NotNullAssertionOnCallableReferenceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NotNullAssertionOnCallableReference

internal class UselessElvisImpl(
    override val receiverType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.UselessElvis

internal class UselessElvisRightIsNullImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.UselessElvisRightIsNull

internal class CannotCheckForErasedImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CannotCheckForErased

internal class CastNeverSucceedsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.CastNeverSucceeds

internal class UselessCastImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.UselessCast

internal class UncheckedCastImpl(
    override val originalType: KaType,
    override val targetType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.UncheckedCast

internal class UselessIsCheckImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UselessIsCheck

internal class IsEnumEntryImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IsEnumEntry

internal class DynamicNotAllowedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DynamicNotAllowed

internal class EnumEntryAsTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.EnumEntryAsType

internal class ExpectedConditionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenCondition>(firDiagnostic, token), KaFirDiagnostic.ExpectedCondition

internal class NoElseInWhenImpl(
    override val missingWhenCases: List<WhenMissingCase>,
    override val description: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenExpression>(firDiagnostic, token), KaFirDiagnostic.NoElseInWhen

internal class InvalidIfAsExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtIfExpression>(firDiagnostic, token), KaFirDiagnostic.InvalidIfAsExpression

internal class ElseMisplacedInWhenImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenEntry>(firDiagnostic, token), KaFirDiagnostic.ElseMisplacedInWhen

internal class RedundantElseInWhenImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenEntry>(firDiagnostic, token), KaFirDiagnostic.RedundantElseInWhen

internal class IllegalDeclarationInWhenSubjectImpl(
    override val illegalReason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IllegalDeclarationInWhenSubject

internal class CommaInWhenConditionWithoutArgumentImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CommaInWhenConditionWithoutArgument

internal class DuplicateBranchConditionInWhenImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DuplicateBranchConditionInWhen

internal class ConfusingBranchConditionErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConfusingBranchConditionError

internal class ConfusingBranchConditionWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConfusingBranchConditionWarning

internal class WrongConditionSuggestGuardImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongConditionSuggestGuard

internal class CommaInWhenConditionWithWhenGuardImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CommaInWhenConditionWithWhenGuard

internal class WhenGuardWithoutSubjectImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WhenGuardWithoutSubject

internal class InferredInvisibleWhenTypeErrorImpl(
    override val whenType: KaType,
    override val syntaxConstructionName: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleWhenTypeError

internal class InferredInvisibleWhenTypeWarningImpl(
    override val whenType: KaType,
    override val syntaxConstructionName: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleWhenTypeWarning

internal class TypeParameterIsNotAnExpressionImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSimpleNameExpression>(firDiagnostic, token), KaFirDiagnostic.TypeParameterIsNotAnExpression

internal class TypeParameterOnLhsOfDotImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSimpleNameExpression>(firDiagnostic, token), KaFirDiagnostic.TypeParameterOnLhsOfDot

internal class NoCompanionObjectImpl(
    override val klass: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NoCompanionObject

internal class ExpressionExpectedPackageFoundImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExpressionExpectedPackageFound

internal class ErrorInContractDescriptionImpl(
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ErrorInContractDescription

internal class ContractNotAllowedImpl(
    override val reason: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContractNotAllowed

internal class NoGetMethodImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtArrayAccessExpression>(firDiagnostic, token), KaFirDiagnostic.NoGetMethod

internal class NoSetMethodImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtArrayAccessExpression>(firDiagnostic, token), KaFirDiagnostic.NoSetMethod

internal class IteratorMissingImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.IteratorMissing

internal class HasNextMissingImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.HasNextMissing

internal class NextMissingImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NextMissing

internal class HasNextFunctionNoneApplicableImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.HasNextFunctionNoneApplicable

internal class NextNoneApplicableImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NextNoneApplicable

internal class DelegateSpecialFunctionMissingImpl(
    override val expectedFunctionSignature: String,
    override val delegateType: KaType,
    override val description: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegateSpecialFunctionMissing

internal class DelegateSpecialFunctionAmbiguityImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegateSpecialFunctionAmbiguity

internal class DelegateSpecialFunctionNoneApplicableImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegateSpecialFunctionNoneApplicable

internal class DelegateSpecialFunctionReturnTypeMismatchImpl(
    override val delegateFunction: String,
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegateSpecialFunctionReturnTypeMismatch

internal class UnderscoreIsReservedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnderscoreIsReserved

internal class UnderscoreUsageWithoutBackticksImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnderscoreUsageWithoutBackticks

internal class ResolvedToUnderscoreNamedCatchParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNameReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.ResolvedToUnderscoreNamedCatchParameter

internal class InvalidCharactersImpl(
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidCharacters

internal class EqualityNotApplicableImpl(
    override val operator: String,
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.EqualityNotApplicable

internal class EqualityNotApplicableWarningImpl(
    override val operator: String,
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.EqualityNotApplicableWarning

internal class IncompatibleEnumComparisonErrorImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleEnumComparisonError

internal class IncompatibleEnumComparisonImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleEnumComparison

internal class ForbiddenIdentityEqualsImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ForbiddenIdentityEquals

internal class ForbiddenIdentityEqualsWarningImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ForbiddenIdentityEqualsWarning

internal class DeprecatedIdentityEqualsImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedIdentityEquals

internal class ImplicitBoxingInIdentityEqualsImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImplicitBoxingInIdentityEquals

internal class IncDecShouldNotReturnUnitImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.IncDecShouldNotReturnUnit

internal class AssignmentOperatorShouldReturnUnitImpl(
    override val functionSymbol: KaFunctionSymbol,
    override val operator: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssignmentOperatorShouldReturnUnit

internal class NotFunctionAsOperatorImpl(
    override val elementName: String,
    override val elementSymbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotFunctionAsOperator

internal class DslScopeViolationImpl(
    override val calleeSymbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DslScopeViolation

internal class ToplevelTypealiasesOnlyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ToplevelTypealiasesOnly

internal class RecursiveTypealiasExpansionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RecursiveTypealiasExpansion

internal class TypealiasShouldExpandToClassImpl(
    override val expandedType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasShouldExpandToClass

internal class ConstructorOrSupertypeOnTypealiasWithTypeProjectionErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConstructorOrSupertypeOnTypealiasWithTypeProjectionError

internal class ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning

internal class TypealiasExpansionCapturesOuterTypeParametersImpl(
    override val outerTypeParameters: List<KaTypeParameterSymbol>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpansionCapturesOuterTypeParameters

internal class RedundantVisibilityModifierImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.RedundantVisibilityModifier

internal class RedundantModalityModifierImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.RedundantModalityModifier

internal class RedundantReturnUnitTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RedundantReturnUnitType

internal class RedundantSingleExpressionStringTemplateImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantSingleExpressionStringTemplate

internal class CanBeValImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CanBeVal

internal class CanBeValLateinitImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CanBeValLateinit

internal class CanBeValDelayedInitializationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CanBeValDelayedInitialization

internal class RedundantCallOfConversionMethodImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantCallOfConversionMethod

internal class ArrayEqualityOperatorCanBeReplacedWithContentEqualsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ArrayEqualityOperatorCanBeReplacedWithContentEquals

internal class EmptyRangeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.EmptyRange

internal class RedundantSetterParameterTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantSetterParameterType

internal class UnusedVariableImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.UnusedVariable

internal class AssignedValueIsNeverReadImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AssignedValueIsNeverRead

internal class VariableInitializerIsRedundantImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VariableInitializerIsRedundant

internal class VariableNeverReadImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VariableNeverRead

internal class UselessCallOnNotNullImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UselessCallOnNotNull

internal class UnusedAnonymousParameterImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnusedAnonymousParameter

internal class UnusedExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnusedExpression

internal class UnusedLambdaExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnusedLambdaExpression

internal class ReturnNotAllowedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnNotAllowed

internal class NotAFunctionLabelImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.NotAFunctionLabel

internal class ReturnInFunctionWithExpressionBodyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnInFunctionWithExpressionBody

internal class ReturnInFunctionWithExpressionBodyWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnInFunctionWithExpressionBodyWarning

internal class ReturnInFunctionWithExpressionBodyAndImplicitTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnInFunctionWithExpressionBodyAndImplicitType

internal class NoReturnInFunctionWithBlockBodyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclarationWithBody>(firDiagnostic, token), KaFirDiagnostic.NoReturnInFunctionWithBlockBody

internal class RedundantReturnImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.RedundantReturn

internal class AnonymousInitializerInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnonymousInitializer>(firDiagnostic, token), KaFirDiagnostic.AnonymousInitializerInInterface

internal class UsageIsNotInlinableImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UsageIsNotInlinable

internal class NonLocalReturnNotAllowedImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonLocalReturnNotAllowed

internal class NotYetSupportedInInlineImpl(
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NotYetSupportedInInline

internal class NothingToInlineImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NothingToInline

internal class NullableInlineParameterImpl(
    override val parameter: KaSymbol,
    override val function: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NullableInlineParameter

internal class RecursionInInlineImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RecursionInInline

internal class NonPublicCallFromPublicInlineImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicCallFromPublicInline

internal class NonPublicInlineCallFromPublicInlineImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicInlineCallFromPublicInline

internal class NonPublicCallFromPublicInlineDeprecationImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicCallFromPublicInlineDeprecation

internal class NonPublicDataCopyCallFromPublicInlineErrorImpl(
    override val inlineDeclaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicDataCopyCallFromPublicInlineError

internal class NonPublicDataCopyCallFromPublicInlineWarningImpl(
    override val inlineDeclaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicDataCopyCallFromPublicInlineWarning

internal class ProtectedConstructorCallFromPublicInlineImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ProtectedConstructorCallFromPublicInline

internal class ProtectedCallFromPublicInlineErrorImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ProtectedCallFromPublicInlineError

internal class PrivateClassMemberFromInlineImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.PrivateClassMemberFromInline

internal class SuperCallFromPublicInlineImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SuperCallFromPublicInline

internal class DeclarationCantBeInlinedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.DeclarationCantBeInlined

internal class DeclarationCantBeInlinedDeprecationErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.DeclarationCantBeInlinedDeprecationError

internal class DeclarationCantBeInlinedDeprecationWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.DeclarationCantBeInlinedDeprecationWarning

internal class OverrideByInlineImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.OverrideByInline

internal class NonInternalPublishedApiImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonInternalPublishedApi

internal class InvalidDefaultFunctionalParameterForInlineImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InvalidDefaultFunctionalParameterForInline

internal class NotSupportedInlineParameterInInlineParameterDefaultValueImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NotSupportedInlineParameterInInlineParameterDefaultValue

internal class ReifiedTypeParameterInOverrideImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeParameterInOverride

internal class InlinePropertyWithBackingFieldImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InlinePropertyWithBackingField

internal class InlinePropertyWithBackingFieldDeprecationErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InlinePropertyWithBackingFieldDeprecationError

internal class InlinePropertyWithBackingFieldDeprecationWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InlinePropertyWithBackingFieldDeprecationWarning

internal class IllegalInlineParameterModifierImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IllegalInlineParameterModifier

internal class InlineSuspendFunctionTypeUnsupportedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.InlineSuspendFunctionTypeUnsupported

internal class InefficientEqualsOverridingInValueClassImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.InefficientEqualsOverridingInValueClass

internal class InlineClassDeprecatedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InlineClassDeprecated

internal class LessVisibleTypeAccessInInlineErrorImpl(
    override val typeVisibility: EffectiveVisibility,
    override val type: KaType,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleTypeAccessInInlineError

internal class LessVisibleTypeAccessInInlineWarningImpl(
    override val typeVisibility: EffectiveVisibility,
    override val type: KaType,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleTypeAccessInInlineWarning

internal class LessVisibleTypeInInlineAccessedSignatureErrorImpl(
    override val symbol: KaSymbol,
    override val typeVisibility: EffectiveVisibility,
    override val type: KaType,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleTypeInInlineAccessedSignatureError

internal class LessVisibleTypeInInlineAccessedSignatureWarningImpl(
    override val symbol: KaSymbol,
    override val typeVisibility: EffectiveVisibility,
    override val type: KaType,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleTypeInInlineAccessedSignatureWarning

internal class LessVisibleContainingClassInInlineErrorImpl(
    override val symbol: KaSymbol,
    override val visibility: EffectiveVisibility,
    override val containingClass: KaClassLikeSymbol,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleContainingClassInInlineError

internal class LessVisibleContainingClassInInlineWarningImpl(
    override val symbol: KaSymbol,
    override val visibility: EffectiveVisibility,
    override val containingClass: KaClassLikeSymbol,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleContainingClassInInlineWarning

internal class CallableReferenceToLessVisibleDeclarationInInlineErrorImpl(
    override val symbol: KaSymbol,
    override val visibility: EffectiveVisibility,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceToLessVisibleDeclarationInInlineError

internal class CallableReferenceToLessVisibleDeclarationInInlineWarningImpl(
    override val symbol: KaSymbol,
    override val visibility: EffectiveVisibility,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceToLessVisibleDeclarationInInlineWarning

internal class InlineFromHigherPlatformImpl(
    override val inlinedBytecodeVersion: String,
    override val currentModuleBytecodeVersion: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InlineFromHigherPlatform

internal class CannotAllUnderImportFromSingletonImpl(
    override val objectName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.CannotAllUnderImportFromSingleton

internal class PackageCannotBeImportedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.PackageCannotBeImported

internal class CannotBeImportedImpl(
    override val name: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.CannotBeImported

internal class ConflictingImportImpl(
    override val name: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.ConflictingImport

internal class OperatorRenamedOnImportImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.OperatorRenamedOnImport

internal class TypealiasAsCallableQualifierInImportErrorImpl(
    override val typealiasName: Name,
    override val originalClassName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.TypealiasAsCallableQualifierInImportError

internal class TypealiasAsCallableQualifierInImportWarningImpl(
    override val typealiasName: Name,
    override val originalClassName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.TypealiasAsCallableQualifierInImportWarning

internal class IllegalSuspendFunctionCallImpl(
    override val suspendCallable: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalSuspendFunctionCall

internal class IllegalSuspendPropertyAccessImpl(
    override val suspendCallable: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalSuspendPropertyAccess

internal class NonLocalSuspensionPointImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonLocalSuspensionPoint

internal class IllegalRestrictedSuspendingFunctionCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalRestrictedSuspendingFunctionCall

internal class NonModifierFormForBuiltInSuspendImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonModifierFormForBuiltInSuspend

internal class ModifierFormForNonBuiltInSuspendImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ModifierFormForNonBuiltInSuspend

internal class ModifierFormForNonBuiltInSuspendFunErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ModifierFormForNonBuiltInSuspendFunError

internal class ModifierFormForNonBuiltInSuspendFunWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ModifierFormForNonBuiltInSuspendFunWarning

internal class ReturnForBuiltInSuspendImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnForBuiltInSuspend

internal class MixingSuspendAndNonSuspendSupertypesImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MixingSuspendAndNonSuspendSupertypes

internal class MixingFunctionalKindsInSupertypesImpl(
    override val kinds: List<FunctionTypeKind>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MixingFunctionalKindsInSupertypes

internal class RedundantLabelWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtLabelReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.RedundantLabelWarning

internal class MultipleLabelsAreForbiddenImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtLabelReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.MultipleLabelsAreForbidden

internal class DeprecatedAccessToEnumEntryCompanionPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEnumEntryCompanionProperty

internal class DeprecatedAccessToEntryPropertyFromEnumImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEntryPropertyFromEnum

internal class DeprecatedAccessToEntriesPropertyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEntriesProperty

internal class DeprecatedAccessToEnumEntryPropertyAsReferenceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEnumEntryPropertyAsReference

internal class DeprecatedAccessToEntriesAsQualifierImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEntriesAsQualifier

internal class DeclarationOfEnumEntryEntriesErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtEnumEntry>(firDiagnostic, token), KaFirDiagnostic.DeclarationOfEnumEntryEntriesError

internal class DeclarationOfEnumEntryEntriesWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtEnumEntry>(firDiagnostic, token), KaFirDiagnostic.DeclarationOfEnumEntryEntriesWarning

internal class IncompatibleClassImpl(
    override val presentableString: String,
    override val incompatibility: IncompatibleVersionErrorData<*>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleClass

internal class PreReleaseClassImpl(
    override val presentableString: String,
    override val poisoningFeatures: List<String>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PreReleaseClass

internal class IrWithUnstableAbiCompiledClassImpl(
    override val presentableString: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IrWithUnstableAbiCompiledClass

internal class BuilderInferenceStubReceiverImpl(
    override val typeParameterName: Name,
    override val containingDeclarationName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.BuilderInferenceStubReceiver

internal class BuilderInferenceMultiLambdaRestrictionImpl(
    override val typeParameterName: Name,
    override val containingDeclarationName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.BuilderInferenceMultiLambdaRestriction

internal class OverrideCannotBeStaticImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OverrideCannotBeStatic

internal class JvmStaticNotInObjectOrClassCompanionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticNotInObjectOrClassCompanion

internal class JvmStaticNotInObjectOrCompanionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticNotInObjectOrCompanion

internal class JvmStaticOnNonPublicMemberImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticOnNonPublicMember

internal class JvmStaticOnConstOrJvmFieldImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticOnConstOrJvmField

internal class JvmStaticOnExternalInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticOnExternalInInterface

internal class InapplicableJvmNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableJvmName

internal class IllegalJvmNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalJvmName

internal class FunctionDelegateMemberNameClashImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FunctionDelegateMemberNameClash

internal class ValueClassWithoutJvmInlineAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassWithoutJvmInlineAnnotation

internal class JvmInlineWithoutValueClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmInlineWithoutValueClass

internal class InapplicableJvmExposeBoxedWithNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableJvmExposeBoxedWithName

internal class UselessJvmExposeBoxedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UselessJvmExposeBoxed

internal class JvmExposeBoxedCannotExposeSuspendImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeSuspend

internal class JvmExposeBoxedRequiresNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedRequiresName

internal class JvmExposeBoxedCannotBeTheSameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotBeTheSame

internal class JvmExposeBoxedCannotBeTheSameAsJvmNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotBeTheSameAsJvmName

internal class JvmExposeBoxedCannotExposeOpenAbstractImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeOpenAbstract

internal class JvmExposeBoxedCannotExposeSyntheticImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeSynthetic

internal class JvmExposeBoxedCannotExposeLocalsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeLocals

internal class JvmExposeBoxedCannotExposeReifiedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeReified

internal class WrongNullabilityForJavaOverrideImpl(
    override val override: KaCallableSymbol,
    override val base: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongNullabilityForJavaOverride

internal class AccidentalOverrideClashByJvmSignatureImpl(
    override val hidden: KaFunctionSymbol,
    override val overrideDescription: String,
    override val regular: KaFunctionSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.AccidentalOverrideClashByJvmSignature

internal class ImplementationByDelegationWithDifferentGenericSignatureErrorImpl(
    override val base: KaFunctionSymbol,
    override val override: KaFunctionSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ImplementationByDelegationWithDifferentGenericSignatureError

internal class ImplementationByDelegationWithDifferentGenericSignatureWarningImpl(
    override val base: KaFunctionSymbol,
    override val override: KaFunctionSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ImplementationByDelegationWithDifferentGenericSignatureWarning

internal class NotYetSupportedLocalInlineFunctionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NotYetSupportedLocalInlineFunction

internal class PropertyHidesJavaFieldImpl(
    override val hidden: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtCallableDeclaration>(firDiagnostic, token), KaFirDiagnostic.PropertyHidesJavaField

internal class JavaTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.JavaTypeMismatch

internal class ReceiverNullabilityMismatchBasedOnJavaAnnotationsImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    override val messageSuffix: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ReceiverNullabilityMismatchBasedOnJavaAnnotations

internal class NullabilityMismatchBasedOnJavaAnnotationsImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    override val messageSuffix: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NullabilityMismatchBasedOnJavaAnnotations

internal class NullabilityMismatchBasedOnExplicitTypeArgumentsForJavaImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    override val messageSuffix: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NullabilityMismatchBasedOnExplicitTypeArgumentsForJava

internal class TypeMismatchWhenFlexibilityChangesImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeMismatchWhenFlexibilityChanges

internal class JavaClassOnCompanionImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaClassOnCompanion

internal class UpperBoundCannotBeArrayImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundCannotBeArray

internal class UpperBoundViolatedBasedOnJavaAnnotationsImpl(
    override val expectedUpperBound: KaType,
    override val actualUpperBound: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedBasedOnJavaAnnotations

internal class UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotationsImpl(
    override val expectedUpperBound: KaType,
    override val actualUpperBound: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations

internal class StrictfpOnClassImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.StrictfpOnClass

internal class SynchronizedOnAbstractImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnAbstract

internal class SynchronizedInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedInInterface

internal class SynchronizedInAnnotationErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedInAnnotationError

internal class SynchronizedInAnnotationWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedInAnnotationWarning

internal class SynchronizedOnInlineImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnInline

internal class SynchronizedOnValueClassErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnValueClassError

internal class SynchronizedOnValueClassWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnValueClassWarning

internal class SynchronizedOnSuspendErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnSuspendError

internal class SynchronizedOnSuspendWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnSuspendWarning

internal class OverloadsWithoutDefaultArgumentsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsWithoutDefaultArguments

internal class OverloadsAbstractImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsAbstract

internal class OverloadsInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsInterface

internal class OverloadsLocalImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsLocal

internal class OverloadsAnnotationClassConstructorErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsAnnotationClassConstructorError

internal class OverloadsPrivateImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsPrivate

internal class DeprecatedJavaAnnotationImpl(
    override val kotlinName: FqName,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DeprecatedJavaAnnotation

internal class JvmPackageNameCannotBeEmptyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmPackageNameCannotBeEmpty

internal class JvmPackageNameMustBeValidNameImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmPackageNameMustBeValidName

internal class JvmPackageNameNotSupportedInFilesWithClassesImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmPackageNameNotSupportedInFilesWithClasses

internal class PositionedValueArgumentForJavaAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.PositionedValueArgumentForJavaAnnotation

internal class RedundantRepeatableAnnotationImpl(
    override val kotlinRepeatable: FqName,
    override val javaRepeatable: FqName,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RedundantRepeatableAnnotation

internal class ThrowsInAnnotationErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.ThrowsInAnnotationError

internal class ThrowsInAnnotationWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.ThrowsInAnnotationWarning

internal class JvmSerializableLambdaOnInlinedFunctionLiteralsErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmSerializableLambdaOnInlinedFunctionLiteralsError

internal class JvmSerializableLambdaOnInlinedFunctionLiteralsWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmSerializableLambdaOnInlinedFunctionLiteralsWarning

internal class IncompatibleAnnotationTargetsImpl(
    override val missingJavaTargets: List<String>,
    override val correspondingKotlinTargets: List<String>,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.IncompatibleAnnotationTargets

internal class AnnotationTargetsOnlyInJavaImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationTargetsOnlyInJava

internal class LocalJvmRecordImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LocalJvmRecord

internal class NonFinalJvmRecordImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonFinalJvmRecord

internal class EnumJvmRecordImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.EnumJvmRecord

internal class JvmRecordWithoutPrimaryConstructorParametersImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordWithoutPrimaryConstructorParameters

internal class NonDataClassJvmRecordImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonDataClassJvmRecord

internal class JvmRecordNotValParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordNotValParameter

internal class JvmRecordNotLastVarargParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordNotLastVarargParameter

internal class InnerJvmRecordImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InnerJvmRecord

internal class FieldInJvmRecordImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FieldInJvmRecord

internal class DelegationByInJvmRecordImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegationByInJvmRecord

internal class JvmRecordExtendsClassImpl(
    override val superType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordExtendsClass

internal class IllegalJavaLangRecordSupertypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalJavaLangRecordSupertype

internal class JavaModuleDoesNotDependOnModuleImpl(
    override val moduleName: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaModuleDoesNotDependOnModule

internal class JavaModuleDoesNotReadUnnamedModuleImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaModuleDoesNotReadUnnamedModule

internal class JavaModuleDoesNotExportPackageImpl(
    override val moduleName: String,
    override val packageName: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaModuleDoesNotExportPackage

internal class JvmDefaultWithoutCompatibilityNotInEnableModeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JvmDefaultWithoutCompatibilityNotInEnableMode

internal class JvmDefaultWithCompatibilityNotInNoCompatibilityModeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JvmDefaultWithCompatibilityNotInNoCompatibilityMode

internal class ExternalDeclarationCannotBeAbstractImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExternalDeclarationCannotBeAbstract

internal class ExternalDeclarationCannotHaveBodyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExternalDeclarationCannotHaveBody

internal class ExternalDeclarationInInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExternalDeclarationInInterface

internal class ExternalDeclarationCannotBeInlinedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExternalDeclarationCannotBeInlined

internal class NonSourceRepeatedAnnotationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.NonSourceRepeatedAnnotation

internal class RepeatedAnnotationWithContainerImpl(
    override val name: ClassId,
    override val explicitContainerName: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatedAnnotationWithContainer

internal class RepeatableContainerMustHaveValueArrayErrorImpl(
    override val container: ClassId,
    override val annotation: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableContainerMustHaveValueArrayError

internal class RepeatableContainerHasNonDefaultParameterErrorImpl(
    override val container: ClassId,
    override val nonDefault: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableContainerHasNonDefaultParameterError

internal class RepeatableContainerHasShorterRetentionErrorImpl(
    override val container: ClassId,
    override val retention: String,
    override val annotation: ClassId,
    override val annotationRetention: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableContainerHasShorterRetentionError

internal class RepeatableContainerTargetSetNotASubsetErrorImpl(
    override val container: ClassId,
    override val annotation: ClassId,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableContainerTargetSetNotASubsetError

internal class RepeatableAnnotationHasNestedClassNamedContainerErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableAnnotationHasNestedClassNamedContainerError

internal class SuspensionPointInsideCriticalSectionImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuspensionPointInsideCriticalSection

internal class InapplicableJvmFieldImpl(
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableJvmField

internal class InapplicableJvmFieldWarningImpl(
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableJvmFieldWarning

internal class IdentitySensitiveOperationsWithValueTypeImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IdentitySensitiveOperationsWithValueType

internal class SynchronizedBlockOnJavaValueBasedClassImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SynchronizedBlockOnJavaValueBasedClass

internal class SynchronizedBlockOnValueClassOrPrimitiveErrorImpl(
    override val valueClassOrPrimitive: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SynchronizedBlockOnValueClassOrPrimitiveError

internal class SynchronizedBlockOnValueClassOrPrimitiveWarningImpl(
    override val valueClassOrPrimitive: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SynchronizedBlockOnValueClassOrPrimitiveWarning

internal class JvmSyntheticOnDelegateImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmSyntheticOnDelegate

internal class SubclassCantCallCompanionProtectedNonStaticImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SubclassCantCallCompanionProtectedNonStatic

internal class ConcurrentHashMapContainsOperatorErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConcurrentHashMapContainsOperatorError

internal class SpreadOnSignaturePolymorphicCallErrorImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SpreadOnSignaturePolymorphicCallError

internal class JavaSamInterfaceConstructorReferenceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaSamInterfaceConstructorReference

internal class NoReflectionInClassPathImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoReflectionInClassPath

internal class SyntheticPropertyWithoutJavaOriginImpl(
    override val originalSymbol: KaFunctionSymbol,
    override val functionName: Name,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SyntheticPropertyWithoutJavaOrigin

internal class JavaFieldShadowedByKotlinPropertyImpl(
    override val kotlinProperty: KaVariableSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaFieldShadowedByKotlinProperty

internal class MissingBuiltInDeclarationImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingBuiltInDeclaration

internal class DangerousCharactersImpl(
    override val characters: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.DangerousCharacters

internal class ImplementingFunctionInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ImplementingFunctionInterface

internal class OverridingExternalFunWithOptionalParamsImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.OverridingExternalFunWithOptionalParams

internal class OverridingExternalFunWithOptionalParamsWithFakeImpl(
    override val function: KaFunctionSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.OverridingExternalFunWithOptionalParamsWithFake

internal class CallToDefinedExternallyFromNonExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CallToDefinedExternallyFromNonExternalDeclaration

internal class ExternalEnumEntryWithBodyImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalEnumEntryWithBody

internal class ExternalTypeExtendsNonExternalTypeImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalTypeExtendsNonExternalType

internal class EnumClassInExternalDeclarationWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.EnumClassInExternalDeclarationWarning

internal class InlineClassInExternalDeclarationWarningImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InlineClassInExternalDeclarationWarning

internal class InlineClassInExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InlineClassInExternalDeclaration

internal class ExtensionFunctionInExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExtensionFunctionInExternalDeclaration

internal class NonExternalDeclarationInInappropriateFileImpl(
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonExternalDeclarationInInappropriateFile

internal class JsExternalInheritorsOnlyImpl(
    override val parent: KaClassLikeSymbol,
    override val kid: KaClassLikeSymbol,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.JsExternalInheritorsOnly

internal class JsExternalArgumentImpl(
    override val argType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.JsExternalArgument

internal class WrongExportedDeclarationImpl(
    override val kind: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongExportedDeclaration

internal class NonExportableTypeImpl(
    override val kind: String,
    override val type: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonExportableType

internal class NonConsumableExportedIdentifierImpl(
    override val name: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonConsumableExportedIdentifier

internal class NamedCompanionInExportedInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NamedCompanionInExportedInterface

internal class NotExportedActualDeclarationWhileExpectIsExportedImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NotExportedActualDeclarationWhileExpectIsExported

internal class NestedJsExportImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NestedJsExport

internal class DelegationByDynamicImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DelegationByDynamic

internal class PropertyDelegationByDynamicImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.PropertyDelegationByDynamic

internal class SpreadOperatorInDynamicCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SpreadOperatorInDynamicCall

internal class WrongOperationWithDynamicImpl(
    override val operation: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongOperationWithDynamic

internal class JsStaticNotInClassCompanionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JsStaticNotInClassCompanion

internal class JsStaticOnNonPublicMemberImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JsStaticOnNonPublicMember

internal class JsStaticOnConstImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JsStaticOnConst

internal class SyntaxImpl(
    override val message: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.Syntax

internal class NestedExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NestedExternalDeclaration

internal class WrongExternalDeclarationImpl(
    override val classKind: String,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.WrongExternalDeclaration

internal class NestedClassInExternalInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NestedClassInExternalInterface

internal class InlineExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InlineExternalDeclaration

internal class NonAbstractMemberOfExternalInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NonAbstractMemberOfExternalInterface

internal class ExternalClassConstructorPropertyParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ExternalClassConstructorPropertyParameter

internal class ExternalAnonymousInitializerImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnonymousInitializer>(firDiagnostic, token), KaFirDiagnostic.ExternalAnonymousInitializer

internal class ExternalDelegationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalDelegation

internal class ExternalDelegatedConstructorCallImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalDelegatedConstructorCall

internal class WrongBodyOfExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongBodyOfExternalDeclaration

internal class WrongInitializerOfExternalDeclarationImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongInitializerOfExternalDeclaration

internal class WrongDefaultValueForExternalFunParameterImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongDefaultValueForExternalFunParameter

internal class CannotCheckForExternalInterfaceImpl(
    override val targetType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotCheckForExternalInterface

internal class UncheckedCastToExternalInterfaceImpl(
    override val sourceType: KaType,
    override val targetType: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UncheckedCastToExternalInterface

internal class ExternalInterfaceAsClassLiteralImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalInterfaceAsClassLiteral

internal class ExternalInterfaceAsReifiedTypeArgumentImpl(
    override val typeArgument: KaType,
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalInterfaceAsReifiedTypeArgument

internal class NamedCompanionInExternalInterfaceImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NamedCompanionInExternalInterface

internal class JscodeArgumentNonConstExpressionImpl(
    firDiagnostic: KtPsiDiagnostic,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JscodeArgumentNonConstExpression

