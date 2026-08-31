/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaUnstableDiagnosticApi::class)

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.api.components.KaWhenMissingCase
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
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
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
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
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
import org.jetbrains.kotlin.resolve.ReturnValueStatus
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
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.Unsupported

internal class UnsupportedFeatureImpl(
    override val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedFeature

internal class UnsupportedSuspendTestImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedSuspendTest

internal class NewInferenceErrorImpl(
    override val error: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NewInferenceError

internal class EscapingCapturedVariableImpl(
    override val variable: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.EscapingCapturedVariable

internal class OtherErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OtherError

internal class OtherErrorWithReasonImpl(
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OtherErrorWithReason

internal class IllegalConstExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalConstExpression

internal class IllegalUnderscoreImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalUnderscore

internal class ExpressionExpectedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpressionExpected

internal class AssignmentInExpressionContextImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.AssignmentInExpressionContext

internal class BreakOrContinueOutsideALoopImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.BreakOrContinueOutsideALoop

internal class NotALoopLabelImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotALoopLabel

internal class BreakOrContinueJumpsAcrossFunctionBoundaryImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpressionWithLabel>(firDiagnostic, token), KaFirDiagnostic.BreakOrContinueJumpsAcrossFunctionBoundary

internal class VariableExpectedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VariableExpected

internal class DelegationInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegationInInterface

internal class DelegationNotToInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegationNotToInterface

internal class NestedClassNotAllowedImpl(
    override val declaration: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NestedClassNotAllowed

internal class NestedClassNotAllowedInLocalErrorImpl(
    override val declaration: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NestedClassNotAllowedInLocalError

internal class NestedClassNotAllowedInLocalWarningImpl(
    override val declaration: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NestedClassNotAllowedInLocalWarning

internal class IncorrectCharacterLiteralImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IncorrectCharacterLiteral

internal class EmptyCharacterLiteralImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.EmptyCharacterLiteral

internal class TooManyCharactersInCharacterLiteralImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TooManyCharactersInCharacterLiteral

internal class IllegalEscapeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalEscape

internal class IntLiteralOutOfRangeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IntLiteralOutOfRange

internal class IntLiteralWithLeadingZerosImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IntLiteralWithLeadingZeros

internal class FloatLiteralOutOfRangeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FloatLiteralOutOfRange

internal class WrongLongSuffixImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongLongSuffix

internal class UnsignedLiteralWithoutDeclarationsOnClasspathImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnsignedLiteralWithoutDeclarationsOnClasspath

internal class DivisionByZeroImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DivisionByZero

internal class TrimMarginBlankPrefixImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.TrimMarginBlankPrefix

internal class ValOrVarOnLoopParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValOrVarOnLoopParameter

internal class ValOrVarOnFunParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValOrVarOnFunParameter

internal class ValOrVarOnCatchParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValOrVarOnCatchParameter

internal class ValOrVarOnSecondaryConstructorParameterImpl(
    override val valOrVar: KtKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValOrVarOnSecondaryConstructorParameter

internal class InnerOnTopLevelScriptClassErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InnerOnTopLevelScriptClassError

internal class InnerOnTopLevelScriptClassWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InnerOnTopLevelScriptClassWarning

internal class ErrorSuppressionImpl(
    override val diagnosticName: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ErrorSuppression

internal class MissingConstructorKeywordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingConstructorKeyword

internal class RedundantInterpolationPrefixImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantInterpolationPrefix

internal class WrappedLhsInAssignmentErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrappedLhsInAssignmentError

internal class WrappedLhsInAssignmentWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrappedLhsInAssignmentWarning

internal class ParenthesizedPackageQualifierErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ParenthesizedPackageQualifierError

internal class ParenthesizedPackageQualifierWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ParenthesizedPackageQualifierWarning

internal class KotlinPackageUsageImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.KotlinPackageUsage

internal class UnsupportedArrayLiteralOutsideOfAnnotationErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedArrayLiteralOutsideOfAnnotationError

internal class UnsupportedArrayLiteralOutsideOfAnnotationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedArrayLiteralOutsideOfAnnotationWarning

internal class UnresolvedReferenceImpl(
    override val reference: String,
    override val operator: String?,
    override val receiverType: KaType?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnresolvedReference

internal class UnresolvedReferenceWrongReceiverImpl(
    override val candidate: KaSymbol,
    override val operator: String?,
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnresolvedReferenceWrongReceiver

internal class InaccessibleOuterClassReceiverImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InaccessibleOuterClassReceiver

internal class UnresolvedImportImpl(
    override val reference: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnresolvedImport

internal class InvisibleReferenceImpl(
    override val reference: KaSymbol,
    override val visible: Visibility,
    override val containingDeclaration: ClassId?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvisibleReference

internal class InvisibleReferenceWarningImpl(
    override val reference: KaSymbol,
    override val visible: Visibility,
    override val containingDeclaration: ClassId?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvisibleReferenceWarning

internal class InvisibleSetterImpl(
    override val property: KaVariableSymbol,
    override val visibility: Visibility,
    override val callableId: CallableId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvisibleSetter

internal class UnresolvedLabelImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnresolvedLabel

internal class AmbiguousLabelImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousLabel

internal class LabelNameClashImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LabelNameClash

internal class DeserializationErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeserializationError

internal class ErrorFromJavaResolutionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ErrorFromJavaResolution

internal class MissingStdlibClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingStdlibClass

internal class NoThisImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoThis

internal class ApiNotAvailableImpl(
    override val sinceKotlinVersion: ApiVersion,
    override val currentVersion: ApiVersion,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ApiNotAvailable

internal class PlaceholderProjectionInQualifierImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PlaceholderProjectionInQualifier

internal class PlaceholderProjectionInTyperefImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PlaceholderProjectionInTyperef

internal class DuplicateParameterNameInFunctionTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DuplicateParameterNameInFunctionType

internal class MissingDependencyClassImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClass

internal class MissingDependencyClassInExpressionTypeImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClassInExpressionType

internal class MissingDependencySuperclassImpl(
    override val missingTypeConstructorName: FqName,
    override val declarationTypeConstructorName: FqName,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencySuperclass

internal class MissingDependencySuperclassWarningImpl(
    override val missingTypeConstructorName: FqName,
    override val declarationTypeConstructorName: FqName,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencySuperclassWarning

internal class MissingDependencyClassInLambdaParameterImpl(
    override val type: KaType,
    override val parameterName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClassInLambdaParameter

internal class MissingDependencyClassInLambdaReceiverImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClassInLambdaReceiver

internal class MissingDependencyClassInTypealiasImpl(
    override val missingType: KaType,
    override val declarationType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyClassInTypealias

internal class MissingDependencyInInferredTypeAnnotationErrorImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyInInferredTypeAnnotationError

internal class MissingDependencyInInferredTypeAnnotationWarningImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingDependencyInInferredTypeAnnotationWarning

internal class RootIdePackageDeprecatedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RootIdePackageDeprecated

internal class SmartcastToTypeVariableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SmartcastToTypeVariable

internal class CreatingAnInstanceOfAbstractClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CreatingAnInstanceOfAbstractClass

internal class NoConstructorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoConstructor

internal class NoImplicitDefaultConstructorOnExpectClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoImplicitDefaultConstructorOnExpectClass

internal class FunctionCallExpectedImpl(
    override val functionName: String,
    override val hasValueParameters: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FunctionCallExpected

internal class IllegalSelectorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalSelector

internal class NoReceiverAllowedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoReceiverAllowed

internal class FunctionExpectedImpl(
    override val expression: String,
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FunctionExpected

internal class InterfaceAsFunctionImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InterfaceAsFunction

internal class ExpectClassAsFunctionImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpectClassAsFunction

internal class InnerClassConstructorNoReceiverImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InnerClassConstructorNoReceiver

internal class PluginAmbiguousInterceptedSymbolImpl(
    override val names: List<String>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PluginAmbiguousInterceptedSymbol

internal class ResolutionToClassifierImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ResolutionToClassifier

internal class AmbiguousAlteredAssignImpl(
    override val altererNames: List<String?>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousAlteredAssign

internal class SelfCallInNestedObjectConstructorErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SelfCallInNestedObjectConstructorError

internal class AmbiguousCollectionLiteralImpl(
    override val candidatesWithOf: List<KaClassLikeSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtCollectionLiteralExpression>(firDiagnostic, token), KaFirDiagnostic.AmbiguousCollectionLiteral

internal class UnresolvedCollectionLiteralImpl(
    override val incompatibleBound: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtCollectionLiteralExpression>(firDiagnostic, token), KaFirDiagnostic.UnresolvedCollectionLiteral

internal class ImplicitPropertyTypeMakesBehaviorOrderDependantImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ImplicitPropertyTypeMakesBehaviorOrderDependant

internal class ImplicitPropertyTypeMakesBehaviorOrderDependantErrorImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ImplicitPropertyTypeMakesBehaviorOrderDependantError

internal class SuperIsNotAnExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuperIsNotAnExpression

internal class SuperNotAvailableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuperNotAvailable

internal class AbstractSuperCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AbstractSuperCall

internal class InstanceAccessBeforeSuperCallImpl(
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InstanceAccessBeforeSuperCall

internal class SuperCallWithDefaultParametersImpl(
    override val name: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuperCallWithDefaultParameters

internal class InterfaceCantCallDefaultMethodViaSuperImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InterfaceCantCallDefaultMethodViaSuper

internal class JavaClassInheritsKtPrivateClassImpl(
    override val javaClassId: ClassId,
    override val privateKotlinType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JavaClassInheritsKtPrivateClass

internal class NotASupertypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotASupertype

internal class TypeArgumentsRedundantInSuperQualifierImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentsRedundantInSuperQualifier

internal class SuperclassNotAccessibleFromInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuperclassNotAccessibleFromInterface

internal class SupertypeInitializedInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeInitializedInInterface

internal class InterfaceWithSuperclassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InterfaceWithSuperclass

internal class FinalSupertypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.FinalSupertype

internal class ClassCannotBeExtendedDirectlyImpl(
    override val classSymbol: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ClassCannotBeExtendedDirectly

internal class SupertypeIsExtensionOrContextFunctionTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeIsExtensionOrContextFunctionType

internal class SingletonInSupertypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SingletonInSupertype

internal class NullableSupertypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableSupertype

internal class NullableSupertypeThroughTypealiasErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableSupertypeThroughTypealiasError

internal class NullableSupertypeThroughTypealiasWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableSupertypeThroughTypealiasWarning

internal class ManyClassesInSupertypeListImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ManyClassesInSupertypeList

internal class SupertypeAppearsTwiceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeAppearsTwice

internal class ClassInSupertypeForEnumImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ClassInSupertypeForEnum

internal class SealedSupertypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SealedSupertype

internal class SealedSupertypeInLocalClassImpl(
    override val declarationType: String,
    override val sealedClassKind: ClassKind,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SealedSupertypeInLocalClass

internal class SealedInheritorInDifferentPackageImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SealedInheritorInDifferentPackage

internal class SealedInheritorInDifferentModuleImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SealedInheritorInDifferentModule

internal class ClassInheritsJavaSealedClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ClassInheritsJavaSealedClass

internal class UnsupportedSealedFunInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedSealedFunInterface

internal class SupertypeNotAClassOrInterfaceImpl(
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeNotAClassOrInterface

internal class UnsupportedInheritanceFromJavaMemberReferencingKotlinFunctionImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction

internal class CyclicInheritanceHierarchyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CyclicInheritanceHierarchy

internal class ProjectionInImmediateArgumentToSupertypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ProjectionInImmediateArgumentToSupertype

internal class InconsistentTypeParameterValuesImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val type: KaClassLikeSymbol,
    override val bounds: List<KaType>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.InconsistentTypeParameterValues

internal class InconsistentTypeParameterBoundsImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val type: KaClassLikeSymbol,
    override val bounds: List<KaType>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InconsistentTypeParameterBounds

internal class AmbiguousSuperImpl(
    override val candidates: List<KaType>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSuperExpression>(firDiagnostic, token), KaFirDiagnostic.AmbiguousSuper

internal class WrongMultipleInheritanceImpl(
    override val symbol: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongMultipleInheritance

internal class ConstructorInObjectImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ConstructorInObject

internal class ConstructorInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ConstructorInInterface

internal class NonPrivateConstructorInEnumImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonPrivateConstructorInEnum

internal class NonPrivateOrProtectedConstructorInSealedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonPrivateOrProtectedConstructorInSealed

internal class CyclicConstructorDelegationCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CyclicConstructorDelegationCall

internal class PrimaryConstructorDelegationCallExpectedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PrimaryConstructorDelegationCallExpected

internal class ProtectedConstructorNotInSuperCallImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ProtectedConstructorNotInSuperCall

internal class SupertypeNotInitializedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeNotInitialized

internal class SupertypeInitializedWithoutPrimaryConstructorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeInitializedWithoutPrimaryConstructor

internal class DelegationSuperCallInEnumConstructorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegationSuperCallInEnumConstructor

internal class ExplicitDelegationCallRequiredImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExplicitDelegationCallRequired

internal class SealedClassConstructorCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SealedClassConstructorCall

internal class DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotationsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations

internal class DataClassConsistentCopyWrongAnnotationTargetImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DataClassConsistentCopyWrongAnnotationTarget

internal class DataClassCopyVisibilityWillBeChangedErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPrimaryConstructor>(firDiagnostic, token), KaFirDiagnostic.DataClassCopyVisibilityWillBeChangedError

internal class DataClassCopyVisibilityWillBeChangedWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPrimaryConstructor>(firDiagnostic, token), KaFirDiagnostic.DataClassCopyVisibilityWillBeChangedWarning

internal class DataClassInvisibleCopyUsageErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNameReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.DataClassInvisibleCopyUsageError

internal class DataClassInvisibleCopyUsageWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNameReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.DataClassInvisibleCopyUsageWarning

internal class DataClassWithoutParametersImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.DataClassWithoutParameters

internal class DataClassVarargParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.DataClassVarargParameter

internal class DataClassNotPropertyParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.DataClassNotPropertyParameter

internal class DataClassCopyJsExportabilityWillBeChangedErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DataClassCopyJsExportabilityWillBeChangedError

internal class DataClassCopyJsExportabilityWillBeChangedWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DataClassCopyJsExportabilityWillBeChangedWarning

internal class AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationArgumentKclassLiteralOfTypeParameterError

internal class AnnotationArgumentMustBeConstImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationArgumentMustBeConst

internal class AnnotationArgumentMustBeEnumConstImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationArgumentMustBeEnumConst

internal class AnnotationArgumentMustBeKclassLiteralImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationArgumentMustBeKclassLiteral

internal class AnnotationClassMemberImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AnnotationClassMember

internal class AnnotationParameterDefaultValueMustBeConstantImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AnnotationParameterDefaultValueMustBeConstant

internal class InvalidTypeOfAnnotationMemberImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InvalidTypeOfAnnotationMember

internal class ProjectionInTypeOfAnnotationMemberErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ProjectionInTypeOfAnnotationMemberError

internal class ProjectionInTypeOfAnnotationMemberWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ProjectionInTypeOfAnnotationMemberWarning

internal class LocalAnnotationClassErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.LocalAnnotationClassError

internal class MissingValOnAnnotationParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.MissingValOnAnnotationParameter

internal class NonConstValUsedInConstantExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NonConstValUsedInConstantExpression

internal class CycleInAnnotationParameterErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.CycleInAnnotationParameterError

internal class AnnotationClassConstructorCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.AnnotationClassConstructorCall

internal class EnumClassConstructorCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.EnumClassConstructorCall

internal class NotAnAnnotationClassImpl(
    override val annotationName: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotAnAnnotationClass

internal class NullableTypeOfAnnotationMemberImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableTypeOfAnnotationMember

internal class VarAnnotationParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.VarAnnotationParameter

internal class SupertypesForAnnotationClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClass>(firDiagnostic, token), KaFirDiagnostic.SupertypesForAnnotationClass

internal class AnnotationUsedAsAnnotationArgumentImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationUsedAsAnnotationArgument

internal class AnnotationOnAnnotationArgumentImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationOnAnnotationArgument

internal class IllegalKotlinVersionStringValueImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.IllegalKotlinVersionStringValue

internal class NewerVersionInSinceKotlinImpl(
    override val specifiedVersion: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NewerVersionInSinceKotlin

internal class DeprecatedSinceKotlinWithUnorderedVersionsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinWithUnorderedVersions

internal class DeprecatedSinceKotlinWithoutArgumentsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinWithoutArguments

internal class DeprecatedSinceKotlinWithoutDeprecatedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinWithoutDeprecated

internal class DeprecatedSinceKotlinWithDeprecatedLevelImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinWithDeprecatedLevel

internal class DeprecatedSinceKotlinOutsideKotlinSubpackageImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSinceKotlinOutsideKotlinSubpackage

internal class KotlinActualAnnotationHasNoEffectInKotlinImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.KotlinActualAnnotationHasNoEffectInKotlin

internal class DeprecationErrorImpl(
    override val reference: KaSymbol,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecationError

internal class DeprecationImpl(
    override val reference: KaSymbol,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.Deprecation

internal class DeprecationErrorMigrationPeriodWarningImpl(
    override val reference: KaSymbol,
    override val message: String,
    override val migrationLanguageFeature: LanguageFeature,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecationErrorMigrationPeriodWarning

internal class OverrideDeprecationImpl(
    override val overridenSymbol: KaSymbol,
    override val deprecationInfo: FirDeprecationInfo,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.OverrideDeprecation

internal class ExtendingAnAnnotationClassErrorImpl(
    override val annotationSymbol: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExtendingAnAnnotationClassError

internal class ExtendingAnAnnotationClassWarningImpl(
    override val annotationSymbol: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExtendingAnAnnotationClassWarning

internal class TypealiasExpansionDeprecationErrorImpl(
    override val alias: KaSymbol,
    override val reference: KaSymbol,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpansionDeprecationError

internal class TypealiasExpansionDeprecationImpl(
    override val alias: KaSymbol,
    override val reference: KaSymbol,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpansionDeprecation

internal class VersionRequirementDeprecationErrorImpl(
    override val reference: KaSymbol,
    override val version: Version,
    override val currentVersion: String,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VersionRequirementDeprecationError

internal class VersionRequirementDeprecationImpl(
    override val reference: KaSymbol,
    override val version: Version,
    override val currentVersion: String,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VersionRequirementDeprecation

internal class RedundantAnnotationImpl(
    override val annotation: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RedundantAnnotation

internal class AnnotationOnSuperclassErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationOnSuperclassError

internal class RestrictedRetentionForExpressionAnnotationErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RestrictedRetentionForExpressionAnnotationError

internal class WrongAnnotationTargetImpl(
    override val actualTarget: String,
    override val allowedTargets: List<KotlinTarget>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongAnnotationTarget

internal class WrongAnnotationTargetWarningImpl(
    override val actualTarget: String,
    override val allowedTargets: List<KotlinTarget>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongAnnotationTargetWarning

internal class WrongAnnotationTargetWithUseSiteTargetImpl(
    override val actualTarget: String,
    override val useSiteTarget: String,
    override val allowedTargets: List<KotlinTarget>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongAnnotationTargetWithUseSiteTarget

internal class AnnotationWithUseSiteTargetOnExpressionErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationWithUseSiteTargetOnExpressionError

internal class AnnotationWithUseSiteTargetOnExpressionWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationWithUseSiteTargetOnExpressionWarning

internal class InapplicableTargetOnPropertyImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetOnProperty

internal class InapplicableTargetOnPropertyWarningImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetOnPropertyWarning

internal class InapplicableTargetPropertyImmutableImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetPropertyImmutable

internal class InapplicableTargetPropertyHasNoDelegateImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetPropertyHasNoDelegate

internal class InapplicableTargetPropertyHasNoBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableTargetPropertyHasNoBackingField

internal class InapplicableParamTargetImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableParamTarget

internal class InapplicableFileTargetImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableFileTarget

internal class InapplicableAllTargetImpl(
    override val inapplicableTargetDescription: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableAllTarget

internal class InapplicableAllTargetInMultiAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableAllTargetInMultiAnnotation

internal class RepeatedAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RepeatedAnnotation

internal class RepeatedAnnotationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RepeatedAnnotationWarning

internal class RedundantAnnotationTargetImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RedundantAnnotationTarget

internal class NotAClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotAClass

internal class WrongExtensionFunctionTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.WrongExtensionFunctionType

internal class AnnotationInWhereClauseErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationInWhereClauseError

internal class AnnotationInContractErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.AnnotationInContractError

internal class AmbiguousAnnotationArgumentImpl(
    override val symbols: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousAnnotationArgument

internal class VolatileOnValueImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.VolatileOnValue

internal class VolatileOnDelegateImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.VolatileOnDelegate

internal class NonInternalPublishedApiImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonInternalPublishedApi

internal class NonSourceAnnotationOnInlinedLambdaExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.NonSourceAnnotationOnInlinedLambdaExpression

internal class PotentiallyNonReportedAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.PotentiallyNonReportedAnnotation

internal class AnnotationWillBeAppliedAlsoToPropertyOrFieldImpl(
    override val useSiteDescription: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationWillBeAppliedAlsoToPropertyOrField

internal class AnnotationsOnBlockLevelExpressionOnTheSameLineImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AnnotationsOnBlockLevelExpressionOnTheSameLine

internal class IgnorabilityAnnotationsWithCheckerDisabledImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.IgnorabilityAnnotationsWithCheckerDisabled

internal class DslMarkerPropagatesToManyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DslMarkerPropagatesToMany

internal class DslMarkerAppliedToWrongTargetImpl(
    override val dslMarkerSymbol: KaClassLikeSymbol,
    override val actualTarget: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DslMarkerAppliedToWrongTarget

internal class JsModuleProhibitedOnNonNativeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsModuleProhibitedOnNonNative

internal class CallFromUmdMustBeJsModuleAndJsNonModuleImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallFromUmdMustBeJsModuleAndJsNonModule

internal class CallToJsModuleWithoutModuleSystemImpl(
    override val callee: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallToJsModuleWithoutModuleSystem

internal class CallToJsNonModuleWithModuleSystemImpl(
    override val callee: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallToJsNonModuleWithModuleSystem

internal class RuntimeAnnotationOnExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RuntimeAnnotationOnExternalDeclaration

internal class NativeAnnotationsAllowedOnlyOnMemberOrExtensionFunImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun

internal class NativeIndexerKeyShouldBeStringOrNumberImpl(
    override val kind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NativeIndexerKeyShouldBeStringOrNumber

internal class NativeIndexerWrongParameterCountImpl(
    override val parametersCount: Int,
    override val kind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NativeIndexerWrongParameterCount

internal class NativeIndexerCanNotHaveDefaultArgumentsImpl(
    override val kind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NativeIndexerCanNotHaveDefaultArguments

internal class NativeGetterReturnTypeShouldBeNullableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NativeGetterReturnTypeShouldBeNullable

internal class NativeSetterWrongReturnTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NativeSetterWrongReturnType

internal class JsNameIsNotOnAllAccessorsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameIsNotOnAllAccessors

internal class JsNameProhibitedForNamedNativeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameProhibitedForNamedNative

internal class JsNameProhibitedForOverrideImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameProhibitedForOverride

internal class JsNameOnPrimaryConstructorProhibitedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameOnPrimaryConstructorProhibited

internal class JsNameOnAccessorAndPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameOnAccessorAndProperty

internal class JsNameProhibitedForExtensionPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameProhibitedForExtensionProperty

internal class JsBuiltinNameClashImpl(
    override val name: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsBuiltinNameClash

internal class NameContainsIllegalCharsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NameContainsIllegalChars

internal class JsNameClashImpl(
    override val name: String,
    override val existing: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNameClash

internal class JsFakeNameClashImpl(
    override val name: String,
    override val override: KaSymbol,
    override val existing: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsFakeNameClash

internal class JsSymbolOnTopLevelDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsSymbolOnTopLevelDeclaration

internal class JsSymbolProhibitedForOverrideImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsSymbolProhibitedForOverride

internal class WrongJsQualifierImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongJsQualifier

internal class JsModuleProhibitedOnVarImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsModuleProhibitedOnVar

internal class NestedJsModuleProhibitedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NestedJsModuleProhibited

internal class UnresolvedEqualityBoundArgumentImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UnresolvedEqualityBoundArgument

internal class AmbiguouslyResolvedEqualityBoundArgumentImpl(
    override val candidates: List<KaType>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AmbiguouslyResolvedEqualityBoundArgument

internal class EqualityBoundArgumentExpandsToNonStarProjectedImpl(
    override val expandedType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.EqualityBoundArgumentExpandsToNonStarProjected

internal class EqualityBoundMismatchOnInheritanceImpl(
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.EqualityBoundMismatchOnInheritance

internal class EqualityBoundMismatchByDelegationImpl(
    override val delegateDeclaration: KaCallableSymbol,
    override val baseDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.EqualityBoundMismatchByDelegation

internal class InheritedIntersectionEqualityBoundImpl(
    override val declaration: KaCallableSymbol,
    override val candidates: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InheritedIntersectionEqualityBound

internal class EqualityBoundNotSupertypeOfContainingClassImpl(
    override val equalityBoundType: KaType,
    override val receiverType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.EqualityBoundNotSupertypeOfContainingClass

internal class EqualityNotApplicableByEqualityBoundsImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    override val leftIsEqualityBound: String,
    override val rightIsEqualityBound: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.EqualityNotApplicableByEqualityBounds

internal class EqualitySuspiciousByEqualityBoundsImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    override val leftEqualityBound: KaType,
    override val rightEqualityBound: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.EqualitySuspiciousByEqualityBounds

internal class OptInUsageImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInUsage

internal class OptInUsageErrorImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInUsageError

internal class OptInToInheritanceImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInToInheritance

internal class OptInToInheritanceErrorImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInToInheritanceError

internal class OptInOverrideImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInOverride

internal class OptInOverrideErrorImpl(
    override val optInMarkerClassId: ClassId,
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInOverrideError

internal class OptInCanOnlyBeUsedAsAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInCanOnlyBeUsedAsAnnotation

internal class OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptInImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn

internal class OptInWithoutArgumentsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInWithoutArguments

internal class OptInArgumentIsNotMarkerImpl(
    override val notMarkerClassId: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassLiteralExpression>(firDiagnostic, token), KaFirDiagnostic.OptInArgumentIsNotMarker

internal class OptInMarkerWithWrongTargetImpl(
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerWithWrongTarget

internal class OptInMarkerWithWrongRetentionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerWithWrongRetention

internal class OptInMarkerOnWrongTargetImpl(
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerOnWrongTarget

internal class OptInMarkerOnOverrideImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerOnOverride

internal class OptInMarkerOnOverrideWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OptInMarkerOnOverrideWarning

internal class SubclassOptInInapplicableImpl(
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SubclassOptInInapplicable

internal class SubclassOptInArgumentIsNotMarkerImpl(
    override val notMarkerClassId: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassLiteralExpression>(firDiagnostic, token), KaFirDiagnostic.SubclassOptInArgumentIsNotMarker

internal class ExposedTypealiasExpandedTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExposedTypealiasExpandedType

internal class ExposedFunctionReturnTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExposedFunctionReturnType

internal class ExposedReceiverTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedReceiverType

internal class ExposedPropertyTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExposedPropertyType

internal class ExposedPropertyTypeInConstructorErrorImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExposedPropertyTypeInConstructorError

internal class ExposedParameterTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ExposedParameterType

internal class ExposedSuperInterfaceImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedSuperInterface

internal class ExposedSuperClassImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedSuperClass

internal class ExposedTypeParameterBoundImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedTypeParameterBound

internal class ExposedTypeParameterBoundDeprecationWarningImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KaClassLikeSymbol,
    override val relationToType: RelationToType,
    override val restrictingVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedTypeParameterBoundDeprecationWarning

internal class RepeatedModifierImpl(
    override val modifier: KtModifierKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RepeatedModifier

internal class WrongModifierTargetImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongModifierTarget

internal class WrongModifierContainingDeclarationImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongModifierContainingDeclaration

internal class DeprecatedModifierImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val actualModifier: KtModifierKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedModifier

internal class DeprecatedModifierForTargetImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedModifierForTarget

internal class DeprecatedModifierContainingDeclarationImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedModifierContainingDeclaration

internal class IncompatibleModifiersImpl(
    override val modifier1: KtModifierKeywordToken,
    override val modifier2: KtModifierKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleModifiers

internal class DeprecatedModifierPairImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedModifierPair

internal class RedundantModifierImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantModifier

internal class RedundantModifierForTargetImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantModifierForTarget

internal class InfixModifierRequiredImpl(
    override val functionSymbol: KaFunctionSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InfixModifierRequired

internal class OperatorModifierRequiredImpl(
    override val functionSymbol: KaFunctionSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OperatorModifierRequired

internal class InapplicableInfixModifierImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableInfixModifier

internal class InapplicableOperatorModifierImpl(
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableOperatorModifier

internal class InapplicableOperatorModifierWarningImpl(
    override val message: String,
    override val deprecatingFeature: LanguageFeature,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableOperatorModifierWarning

internal class InapplicableLateinitModifierImpl(
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.InapplicableLateinitModifier

internal class PotentiallyNullableReturnTypeOfOperatorOfImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.PotentiallyNullableReturnTypeOfOperatorOf

internal class NullableReturnTypeOfOperatorOfImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.NullableReturnTypeOfOperatorOf

internal class ReturnTypeMismatchOfOperatorOfImpl(
    override val outerClass: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatchOfOperatorOf

internal class NoVarargOverloadOfOperatorOfImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.NoVarargOverloadOfOperatorOf

internal class MultipleVarargOverloadsOfOperatorOfImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.MultipleVarargOverloadsOfOperatorOf

internal class InconsistentReturnTypesInOfOverloadsImpl(
    override val mainOverloadType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.InconsistentReturnTypesInOfOverloads

internal class InconsistentParameterTypesInOfOverloadsImpl(
    override val mainParameterType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InconsistentParameterTypesInOfOverloads

internal class InconsistentVisibilityInOfOverloadsImpl(
    override val mainVisibility: Visibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.InconsistentVisibilityInOfOverloads

internal class InconsistentSuspendInOfOverloadsImpl(
    override val overloadSuspendability: String,
    override val mainOverloadSuspendability: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.InconsistentSuspendInOfOverloads

internal class OfOverloadsInBlockAndObjectImpl(
    override val overloadOrigin: String,
    override val mainOrigin: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.OfOverloadsInBlockAndObject

internal class InconsistentTypeParametersInOfOverloadsImpl(
    override val mainOverload: KaFunctionSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.InconsistentTypeParametersInOfOverloads

internal class RedundantOpenInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.RedundantOpenInInterface

internal class OperatorCallOnConstructorImpl(
    override val name: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OperatorCallOnConstructor

internal class NoExplicitVisibilityInApiModeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoExplicitVisibilityInApiMode

internal class NoExplicitVisibilityInApiModeWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoExplicitVisibilityInApiModeWarning

internal class NoExplicitReturnTypeInApiModeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoExplicitReturnTypeInApiMode

internal class NoExplicitReturnTypeInApiModeWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoExplicitReturnTypeInApiModeWarning

internal class AnonymousSuspendFunctionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.AnonymousSuspendFunction

internal class ValueClassNotTopLevelImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ValueClassNotTopLevel

internal class ValueClassNotFinalImpl(
    override val prefix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ValueClassNotFinal

internal class ValueClassOpenImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ValueClassOpen

internal class AbsenceOfPrimaryConstructorForValueClassImpl(
    override val modifier: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.AbsenceOfPrimaryConstructorForValueClass

internal class ExpectValueClassWithNoPrimaryConstructorHasSecondaryImpl(
    override val modifier: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectValueClassWithNoPrimaryConstructorHasSecondary

internal class InlineClassConstructorWrongParametersSizeImpl(
    override val prefix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InlineClassConstructorWrongParametersSize

internal class ValueClassEmptyConstructorImpl(
    override val prefix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassEmptyConstructor

internal class ValueClassConstructorNotFinalReadOnlyParameterImpl(
    override val prefix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValueClassConstructorNotFinalReadOnlyParameter

internal class AbstractValueClassConstructorPropertyParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.AbstractValueClassConstructorPropertyParameter

internal class SealedValueClassConstructorPropertyParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.SealedValueClassConstructorPropertyParameter

internal class PropertyWithBackingFieldInsideValueClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.PropertyWithBackingFieldInsideValueClass

internal class DelegatedPropertyInsideValueClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegatedPropertyInsideValueClass

internal class ValueClassHasInapplicableParameterTypeImpl(
    override val type: KaType,
    override val prefix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassHasInapplicableParameterType

internal class ValueClassCannotImplementInterfaceByDelegationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotImplementInterfaceByDelegation

internal class ValueClassCannotExtendClassesImpl(
    override val prefix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotExtendClasses

internal class ValueClassCannotExtendIdentityClassesImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotExtendIdentityClasses

internal class ValueClassCannotBeRecursiveImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotBeRecursive

internal class ValueClassCannotBeRecursiveViaTypeParametersErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotBeRecursiveViaTypeParametersError

internal class ValueClassCannotBeRecursiveViaTypeParametersWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotBeRecursiveViaTypeParametersWarning

internal class SecondaryConstructorWithBodyInsideValueClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SecondaryConstructorWithBodyInsideValueClass

internal class ReservedMemberInsideValueClassImpl(
    override val name: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.ReservedMemberInsideValueClass

internal class ReservedMemberFromInterfaceInsideValueClassImpl(
    override val interfaceName: String,
    override val methodName: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClass>(firDiagnostic, token), KaFirDiagnostic.ReservedMemberFromInterfaceInsideValueClass

internal class TypeArgumentOnTypedValueClassEqualsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentOnTypedValueClassEquals

internal class InnerClassInsideValueClassImpl(
    override val prefix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InnerClassInsideValueClass

internal class ValueClassCannotBeCloneableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ValueClassCannotBeCloneable

internal class NoneApplicableImpl(
    override val candidates: List<Pair<KaSymbol, List<String>>>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoneApplicable

internal class InapplicableCandidateImpl(
    override val candidate: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableCandidate

internal class HasNextFunctionNoneApplicableImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.HasNextFunctionNoneApplicable

internal class NextNoneApplicableImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NextNoneApplicable

internal class DelegateSpecialFunctionNoneApplicableImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegateSpecialFunctionNoneApplicable

internal class TypeInferenceOnlyInputTypesErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeInferenceOnlyInputTypesError

internal class MemberProjectedOutImpl(
    override val receiver: KaType,
    override val projection: String,
    override val symbol: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MemberProjectedOut

internal class NoValueForParameterImpl(
    override val violatedParameter: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NoValueForParameter

internal class TooManyArgumentsImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TooManyArguments

internal class NamedParameterNotFoundImpl(
    override val name: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtValueArgument>(firDiagnostic, token), KaFirDiagnostic.NamedParameterNotFound

internal class NameForAmbiguousParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtValueArgument>(firDiagnostic, token), KaFirDiagnostic.NameForAmbiguousParameter

internal class ArgumentPassedTwiceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtValueArgument>(firDiagnostic, token), KaFirDiagnostic.ArgumentPassedTwice

internal class NamedArgumentsNotAllowedImpl(
    override val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtValueArgument>(firDiagnostic, token), KaFirDiagnostic.NamedArgumentsNotAllowed

internal class MixingNamedAndPositionalArgumentsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MixingNamedAndPositionalArguments

internal class VarargOutsideParenthesesImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.VarargOutsideParentheses

internal class NonVarargSpreadImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<LeafPsiElement>(firDiagnostic, token), KaFirDiagnostic.NonVarargSpread

internal class SpreadOfNullableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SpreadOfNullable

internal class UnexpectedTrailingLambdaOnANewLineImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnexpectedTrailingLambdaOnANewLine

internal class ManyLambdaExpressionArgumentsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtLambdaExpression>(firDiagnostic, token), KaFirDiagnostic.ManyLambdaExpressionArguments

internal class AssigningSingleElementToVarargInNamedFormFunctionErrorImpl(
    override val expectedArrayType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssigningSingleElementToVarargInNamedFormFunctionError

internal class AssigningSingleElementToVarargInNamedFormFunctionWarningImpl(
    override val expectedArrayType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssigningSingleElementToVarargInNamedFormFunctionWarning

internal class AssigningSingleElementToVarargInNamedFormAnnotationErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssigningSingleElementToVarargInNamedFormAnnotationError

internal class AssigningSingleElementToVarargInNamedFormAnnotationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssigningSingleElementToVarargInNamedFormAnnotationWarning

internal class RedundantSpreadOperatorInNamedFormInFunctionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RedundantSpreadOperatorInNamedFormInFunction

internal class RedundantSpreadOperatorInNamedFormInAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RedundantSpreadOperatorInNamedFormInAnnotation

internal class IllegalTypeArgumentForVarargParameterWarningImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IllegalTypeArgumentForVarargParameterWarning

internal class NestedClassAccessedViaInstanceReferenceImpl(
    override val symbol: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NestedClassAccessedViaInstanceReference

internal class TypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeMismatch

internal class ArgumentTypeMismatchImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ArgumentTypeMismatch

internal class ReturnTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val targetFunction: KaSymbol,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatch

internal class ExpectedParameterTypeMismatchImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpectedParameterTypeMismatch

internal class InitializerTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.InitializerTypeMismatch

internal class FieldInitializerTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.FieldInitializerTypeMismatch

internal class AssignmentTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssignmentTypeMismatch

internal class ConditionTypeMismatchImpl(
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConditionTypeMismatch

internal class ThrowableTypeMismatchImpl(
    override val actualType: KaType,
    override val isMismatchDueToNullability: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ThrowableTypeMismatch

internal class ResultTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ResultTypeMismatch

internal class CompareToTypeMismatchImpl(
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CompareToTypeMismatch

internal class HasNextFunctionTypeMismatchImpl(
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.HasNextFunctionTypeMismatch

internal class ComponentFunctionReturnTypeMismatchImpl(
    override val componentFunctionName: Name,
    override val destructingType: KaType,
    override val expectedType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ComponentFunctionReturnTypeMismatch

internal class DelegateSpecialFunctionReturnTypeMismatchImpl(
    override val delegateFunction: String,
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegateSpecialFunctionReturnTypeMismatch

internal class OverloadResolutionAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OverloadResolutionAmbiguity

internal class AssignOperatorAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AssignOperatorAmbiguity

internal class IteratorAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IteratorAmbiguity

internal class HasNextFunctionAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.HasNextFunctionAmbiguity

internal class NextAmbiguityImpl(
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NextAmbiguity

internal class ComponentFunctionAmbiguityImpl(
    override val functionWithAmbiguityName: Name,
    override val candidates: List<KaSymbol>,
    override val destructingType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ComponentFunctionAmbiguity

internal class DelegateSpecialFunctionAmbiguityImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegateSpecialFunctionAmbiguity

internal class CompilerRequiredAnnotationAmbiguityImpl(
    override val typeFromCompilerPhase: KaType,
    override val typeFromTypesPhase: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompilerRequiredAnnotationAmbiguity

internal class AmbiguousFunctionTypeKindImpl(
    override val kinds: List<FunctionTypeKind>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousFunctionTypeKind

internal class ContextSensitiveResolutionAmbiguityImpl(
    override val resolvedCandidate: KaSymbol,
    override val contextSensitiveCandidates: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextSensitiveResolutionAmbiguity

internal class NoContextArgumentImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NoContextArgument

internal class AmbiguousContextArgumentImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousContextArgument

internal class ContextualOverloadShadowedImpl(
    override val symbols: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextualOverloadShadowed

internal class MultipleContextListsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleContextLists

internal class ContextParameterWithoutNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtContextReceiver>(firDiagnostic, token), KaFirDiagnostic.ContextParameterWithoutName

internal class ContextParametersWithBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextParametersWithBackingField

internal class CallableReferenceToContextualDeclarationImpl(
    override val symbol: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceToContextualDeclaration

internal class NamedContextParameterInFunctionTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NamedContextParameterInFunctionType

internal class ContextParameterWithDefaultImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContextParameterWithDefault

internal class UnsupportedContextualDeclarationCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedContextualDeclarationCall

internal class AmbiguousCallWithImplicitContextReceiverImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.AmbiguousCallWithImplicitContextReceiver

internal class CoroutineContextAsContextParameterIsReservedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CoroutineContextAsContextParameterIsReserved

internal class RecursionInImplicitTypesImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RecursionInImplicitTypes

internal class InferenceErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InferenceError

internal class ProjectionOnNonClassTypeArgumentImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ProjectionOnNonClassTypeArgument

internal class UpperBoundViolatedImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    override val extraMessage: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolated

internal class UpperBoundViolatedDeprecationWarningImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    override val extraMessage: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedDeprecationWarning

internal class UpperBoundViolatedInTypeOperatorOrParameterBoundsErrorImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    override val extraMessage: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInTypeOperatorOrParameterBoundsError

internal class UpperBoundViolatedInTypeOperatorOrParameterBoundsWarningImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    override val extraMessage: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInTypeOperatorOrParameterBoundsWarning

internal class UpperBoundViolatedInTypealiasExpansionImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInTypealiasExpansion

internal class UpperBoundViolatedInTypealiasExpansionDeprecationWarningImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInTypealiasExpansionDeprecationWarning

internal class UpperBoundViolatedInLhsOfClassLiteralWarningImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInLhsOfClassLiteralWarning

internal class TypeArgumentsNotAllowedImpl(
    override val place: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentsNotAllowed

internal class TypeArgumentsNotAllowedWarningImpl(
    override val place: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentsNotAllowedWarning

internal class TypeArgumentsNotAllowedInPackageQualifierWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentsNotAllowedInPackageQualifierWarning

internal class TypeArgumentsForOuterClassWhenNestedReferencedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeArgumentsForOuterClassWhenNestedReferenced

internal class WrongNumberOfTypeArgumentsImpl(
    override val expectedCount: Int,
    override val owner: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongNumberOfTypeArguments

internal class WrongNumberOfTypeArgumentsWarningImpl(
    override val expectedCount: Int,
    override val owner: KaSymbol,
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongNumberOfTypeArgumentsWarning

internal class WrongNumberOfTypeArgumentsInLocalClassInLhsWarningImpl(
    override val expectedCount: Int,
    override val owner: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongNumberOfTypeArgumentsInLocalClassInLhsWarning

internal class WrongNumberOfTypeArgumentsInGetClassWarningImpl(
    override val expectedCount: Int,
    override val owner: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongNumberOfTypeArgumentsInGetClassWarning

internal class InvalidQualifierInLhsOfCallableReferenceToStaticErrorImpl(
    override val kind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidQualifierInLhsOfCallableReferenceToStaticError

internal class InvalidQualifierInLhsOfCallableReferenceToStaticWarningImpl(
    override val kind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidQualifierInLhsOfCallableReferenceToStaticWarning

internal class NoTypeArgumentsOnRhsImpl(
    override val expectedCount: Int,
    override val classifier: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoTypeArgumentsOnRhs

internal class OuterClassArgumentsRequiredImpl(
    override val outer: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OuterClassArgumentsRequired

internal class TypeParametersInObjectImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParametersInObject

internal class TypeParametersInAnonymousObjectImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParametersInAnonymousObject

internal class IllegalProjectionUsageImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalProjectionUsage

internal class TypeParametersInEnumImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParametersInEnum

internal class ConflictingProjectionImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeProjection>(firDiagnostic, token), KaFirDiagnostic.ConflictingProjection

internal class ConflictingProjectionInTypealiasExpansionImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConflictingProjectionInTypealiasExpansion

internal class ConflictingProjectionInCallableReferenceWarningImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeProjection>(firDiagnostic, token), KaFirDiagnostic.ConflictingProjectionInCallableReferenceWarning

internal class RedundantProjectionImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeProjection>(firDiagnostic, token), KaFirDiagnostic.RedundantProjection

internal class VarianceOnTypeParameterNotAllowedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.VarianceOnTypeParameterNotAllowed

internal class CatchParameterWithDefaultValueImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CatchParameterWithDefaultValue

internal class TypeParameterInCatchClauseImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParameterInCatchClause

internal class GenericThrowableSubclassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.GenericThrowableSubclass

internal class InnerClassOfGenericThrowableSubclassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.InnerClassOfGenericThrowableSubclass

internal class KclassWithNullableTypeParameterInSignatureImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.KclassWithNullableTypeParameterInSignature

internal class TypeParameterAsReifiedImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParameterAsReified

internal class TypeParameterAsReifiedDeprecationWarningImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParameterAsReifiedDeprecationWarning

internal class TypeParameterAsReifiedArrayErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeParameterAsReifiedArrayError

internal class ReifiedTypeForbiddenSubstitutionImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeForbiddenSubstitution

internal class DefinitelyNonNullableAsReifiedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DefinitelyNonNullableAsReified

internal class TypeIntersectionAsReifiedErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val types: List<KaType>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeIntersectionAsReifiedError

internal class TypeIntersectionAsReifiedWarningImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val types: List<KaType>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeIntersectionAsReifiedWarning

internal class TypeIntersectionAsReifiedDeprecationWarningImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val types: List<KaType>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeIntersectionAsReifiedDeprecationWarning

internal class FinalUpperBoundImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.FinalUpperBound

internal class UpperBoundIsExtensionOrContextFunctionTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundIsExtensionOrContextFunctionType

internal class BoundsNotAllowedIfBoundedByTypeParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.BoundsNotAllowedIfBoundedByTypeParameter

internal class OnlyOneClassBoundAllowedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.OnlyOneClassBoundAllowed

internal class RepeatedBoundImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RepeatedBound

internal class ConflictingUpperBoundsImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ConflictingUpperBounds

internal class NameInConstraintIsNotATypeParameterImpl(
    override val typeParameterName: Name,
    override val typeParametersOwner: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSimpleNameExpression>(firDiagnostic, token), KaFirDiagnostic.NameInConstraintIsNotATypeParameter

internal class BoundOnTypeAliasParameterNotAllowedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.BoundOnTypeAliasParameterNotAllowed

internal class ReifiedTypeParameterNoInlineImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeParameterNoInline

internal class ReifiedTypeParameterOnAliasErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeParameterOnAliasError

internal class ReifiedTypeParameterOnAliasWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeParameterOnAliasWarning

internal class TypeParametersNotAllowedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.TypeParametersNotAllowed

internal class IncorrectTypeParameterOfPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.IncorrectTypeParameterOfProperty

internal class ImplicitNothingReturnTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ImplicitNothingReturnType

internal class ImplicitNothingPropertyTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ImplicitNothingPropertyType

internal class AbbreviatedNothingReturnTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AbbreviatedNothingReturnType

internal class AbbreviatedNothingPropertyTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AbbreviatedNothingPropertyType

internal class CyclicGenericUpperBoundImpl(
    override val typeParameters: List<KaTypeParameterSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CyclicGenericUpperBound

internal class FiniteBoundsViolationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FiniteBoundsViolation

internal class FiniteBoundsViolationInJavaImpl(
    override val containingTypes: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FiniteBoundsViolationInJava

internal class ExpansiveInheritanceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpansiveInheritance

internal class ExpansiveInheritanceInJavaImpl(
    override val containingTypes: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpansiveInheritanceInJava

internal class DeprecatedTypeParameterSyntaxImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.DeprecatedTypeParameterSyntax

internal class MisplacedTypeParameterConstraintsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeParameter>(firDiagnostic, token), KaFirDiagnostic.MisplacedTypeParameterConstraints

internal class DynamicSupertypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DynamicSupertype

internal class DynamicUpperBoundImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DynamicUpperBound

internal class DynamicReceiverNotAllowedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DynamicReceiverNotAllowed

internal class DynamicReceiverExpectedButWasNonDynamicImpl(
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DynamicReceiverExpectedButWasNonDynamic

internal class IncompatibleTypesImpl(
    override val typeA: KaType,
    override val typeB: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleTypes

internal class IncompatibleTypesWarningImpl(
    override val typeA: KaType,
    override val typeB: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleTypesWarning

internal class TypeVarianceConflictErrorImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeParameterVariance: Variance,
    override val variance: Variance,
    override val containingType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeVarianceConflictError

internal class TypeVarianceConflictInExpandedTypeImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeParameterVariance: Variance,
    override val variance: Variance,
    override val containingType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeVarianceConflictInExpandedType

internal class SmartcastImpossibleImpl(
    override val desiredType: KaType,
    override val subject: KtExpression,
    override val description: String,
    override val isCastToNotNull: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.SmartcastImpossible

internal class SmartcastImpossibleOnImplicitInvokeReceiverImpl(
    override val desiredType: KaType,
    override val subject: KtExpression,
    override val description: String,
    override val isCastToNotNull: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.SmartcastImpossibleOnImplicitInvokeReceiver

internal class DeprecatedSmartcastOnDelegatedPropertyImpl(
    override val desiredType: KaType,
    override val property: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DeprecatedSmartcastOnDelegatedProperty

internal class PlatformClassMappedToKotlinImpl(
    override val kotlinClass: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PlatformClassMappedToKotlin

internal class InferredTypeVariableIntoEmptyIntersectionErrorImpl(
    override val typeVariableDescription: String,
    override val incompatibleTypes: List<KaType>,
    override val description: String,
    override val causingTypes: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InferredTypeVariableIntoEmptyIntersectionError

internal class InferredTypeVariableIntoEmptyIntersectionWarningImpl(
    override val typeVariableDescription: String,
    override val incompatibleTypes: List<KaType>,
    override val description: String,
    override val causingTypes: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InferredTypeVariableIntoEmptyIntersectionWarning

internal class InferredTypeVariableIntoPossibleEmptyIntersectionImpl(
    override val typeVariableDescription: String,
    override val incompatibleTypes: List<KaType>,
    override val description: String,
    override val causingTypes: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InferredTypeVariableIntoPossibleEmptyIntersection

internal class IncorrectLeftComponentOfIntersectionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncorrectLeftComponentOfIntersection

internal class IncorrectRightComponentOfIntersectionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncorrectRightComponentOfIntersection

internal class NullableOnDefinitelyNotNullableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NullableOnDefinitelyNotNullable

internal class RedundantNullableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RedundantNullable

internal class InferredInvisibleReifiedTypeArgumentWarningImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeArgumentType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleReifiedTypeArgumentWarning

internal class InferredInvisibleVarargTypeArgumentWarningImpl(
    override val typeParameter: KaTypeParameterSymbol,
    override val typeArgumentType: KaType,
    override val valueParameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleVarargTypeArgumentWarning

internal class InferredInvisibleReturnTypeWarningImpl(
    override val calleeSymbol: KaSymbol,
    override val returnType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleReturnTypeWarning

internal class GenericQualifierOnConstructorCallErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.GenericQualifierOnConstructorCallError

internal class GenericQualifierOnConstructorCallWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.GenericQualifierOnConstructorCallWarning

internal class AtomicRefWithoutConsistentIdentityImpl(
    override val atomicRef: ClassId,
    override val argumentType: KaType,
    override val suggestedType: ClassId?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AtomicRefWithoutConsistentIdentity

internal class AtomicRefCallArgumentWithoutConsistentIdentityImpl(
    override val argumentType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AtomicRefCallArgumentWithoutConsistentIdentity

internal class ExtensionInClassReferenceNotAllowedImpl(
    override val referencedDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExtensionInClassReferenceNotAllowed

internal class CallableReferenceLhsNotAClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceLhsNotAClass

internal class CallableReferenceToAnnotationConstructorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceToAnnotationConstructor

internal class AdaptedCallableReferenceAgainstReflectionTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AdaptedCallableReferenceAgainstReflectionType

internal class ClassLiteralLhsNotAClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ClassLiteralLhsNotAClass

internal class ClassLiteralLhsNotAClassWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ClassLiteralLhsNotAClassWarning

internal class NullableTypeInClassLiteralLhsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NullableTypeInClassLiteralLhs

internal class ExpressionOfNullableTypeInClassLiteralLhsImpl(
    override val lhsType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpressionOfNullableTypeInClassLiteralLhs

internal class ExpressionOfNullableTypeInClassLiteralLhsWarningImpl(
    override val lhsType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpressionOfNullableTypeInClassLiteralLhsWarning

internal class UnsupportedClassLiteralsWithEmptyLhsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedClassLiteralsWithEmptyLhs

internal class UnsupportedArrayOfNothingInClassLiteralLhsImpl(
    override val unsupported: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedArrayOfNothingInClassLiteralLhs

internal class MutablePropertyWithCapturedTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MutablePropertyWithCapturedType

internal class UnsupportedReflectionApiImpl(
    override val unsupportedReflectionAPI: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnsupportedReflectionApi

internal class NothingToOverrideImpl(
    override val declaration: KaCallableSymbol,
    override val candidates: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.NothingToOverride

internal class CannotOverrideInvisibleMemberImpl(
    override val overridingMember: KaCallableSymbol,
    override val baseMember: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.CannotOverrideInvisibleMember

internal class DataClassOverrideConflictImpl(
    override val overridingMember: KaCallableSymbol,
    override val baseMember: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.DataClassOverrideConflict

internal class DataClassOverrideDefaultValuesImpl(
    override val overridingMember: KaCallableSymbol,
    override val baseType: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DataClassOverrideDefaultValues

internal class CannotWeakenAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.CannotWeakenAccessPrivilege

internal class CannotWeakenAccessPrivilegeWarningImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.CannotWeakenAccessPrivilegeWarning

internal class CannotChangeAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.CannotChangeAccessPrivilege

internal class CannotChangeAccessPrivilegeWarningImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.CannotChangeAccessPrivilegeWarning

internal class CannotInferVisibilityImpl(
    override val callable: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CannotInferVisibility

internal class CannotInferVisibilityWarningImpl(
    override val callable: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CannotInferVisibilityWarning

internal class MultipleDefaultsInheritedFromSupertypesImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypes

internal class MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride

internal class MultipleDefaultsInheritedFromSupertypesDeprecationErrorImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesDeprecationError

internal class MultipleDefaultsInheritedFromSupertypesDeprecationWarningImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesDeprecationWarning

internal class MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationErrorImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError

internal class MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarningImpl(
    override val name: Name,
    override val valueParameter: KaSymbol,
    override val baseFunctions: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning

internal class TypealiasExpandsToArrayOfNothingsImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpandsToArrayOfNothings

internal class OverridingFinalMemberImpl(
    override val overriddenDeclaration: KaCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.OverridingFinalMember

internal class ReturnTypeMismatchOnOverrideImpl(
    override val function: KaCallableSymbol,
    override val superFunction: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatchOnOverride

internal class PropertyTypeMismatchOnOverrideImpl(
    override val property: KaCallableSymbol,
    override val superProperty: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.PropertyTypeMismatchOnOverride

internal class VarTypeMismatchOnOverrideImpl(
    override val variable: KaCallableSymbol,
    override val superVariable: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VarTypeMismatchOnOverride

internal class ReturnTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KaCallableSymbol,
    override val conflictingDeclaration2: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatchOnInheritance

internal class PropertyTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KaCallableSymbol,
    override val conflictingDeclaration2: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.PropertyTypeMismatchOnInheritance

internal class VarTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KaCallableSymbol,
    override val conflictingDeclaration2: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.VarTypeMismatchOnInheritance

internal class ReturnTypeMismatchByDelegationImpl(
    override val delegateDeclaration: KaCallableSymbol,
    override val baseDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ReturnTypeMismatchByDelegation

internal class PropertyTypeMismatchByDelegationImpl(
    override val delegateDeclaration: KaCallableSymbol,
    override val baseDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.PropertyTypeMismatchByDelegation

internal class VarOverriddenByValByDelegationImpl(
    override val delegateDeclaration: KaCallableSymbol,
    override val baseDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.VarOverriddenByValByDelegation

internal class ConflictingInheritedMembersImpl(
    override val owner: KaClassLikeSymbol,
    override val conflictingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ConflictingInheritedMembers

internal class AbstractMemberNotImplementedImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.AbstractMemberNotImplemented

internal class AbstractMemberIncorrectlyDelegatedErrorImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.AbstractMemberIncorrectlyDelegatedError

internal class AbstractMemberIncorrectlyDelegatedWarningImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.AbstractMemberIncorrectlyDelegatedWarning

internal class AbstractMemberNotImplementedByEnumEntryImpl(
    override val enumEntry: KaSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtEnumEntry>(firDiagnostic, token), KaFirDiagnostic.AbstractMemberNotImplementedByEnumEntry

internal class AbstractClassMemberNotImplementedImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.AbstractClassMemberNotImplemented

internal class InvisibleAbstractMemberFromSuperErrorImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val invisibleDeclarations: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.InvisibleAbstractMemberFromSuperError

internal class AmbiguousAnonymousTypeInferredImpl(
    override val superTypes: List<KaType>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.AmbiguousAnonymousTypeInferred

internal class ManyImplMemberNotImplementedImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ManyImplMemberNotImplemented

internal class ManyInterfacesMemberNotImplementedImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val missingDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ManyInterfacesMemberNotImplemented

internal class OverridingFinalMemberByDelegationImpl(
    override val delegatedDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.OverridingFinalMemberByDelegation

internal class DelegatedMemberHidesSupertypeOverrideImpl(
    override val delegatedDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.DelegatedMemberHidesSupertypeOverride

internal class VarOverriddenByValImpl(
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VarOverriddenByVal

internal class VarImplementedByInheritedValErrorImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VarImplementedByInheritedValError

internal class VarImplementedByInheritedValWarningImpl(
    override val classOrObject: KaClassLikeSymbol,
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VarImplementedByInheritedValWarning

internal class NonFinalMemberInFinalClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NonFinalMemberInFinalClass

internal class NonFinalMemberInObjectImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NonFinalMemberInObject

internal class VirtualMemberHiddenImpl(
    override val declared: KaCallableSymbol,
    override val overriddenContainer: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VirtualMemberHidden

internal class ParameterNameChangedOnOverrideImpl(
    override val superType: KaClassLikeSymbol,
    override val conflictingParameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ParameterNameChangedOnOverride

internal class DifferentNamesForTheSameParameterInSupertypesImpl(
    override val currentParameter: KaSymbol,
    override val conflictingParameter: KaSymbol,
    override val parameterNumber: Int,
    override val conflictingFunctions: List<KaFunctionSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.DifferentNamesForTheSameParameterInSupertypes

internal class SuspendOverriddenByNonSuspendImpl(
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtCallableDeclaration>(firDiagnostic, token), KaFirDiagnostic.SuspendOverriddenByNonSuspend

internal class NonSuspendOverriddenBySuspendImpl(
    override val overridingDeclaration: KaCallableSymbol,
    override val overriddenDeclaration: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtCallableDeclaration>(firDiagnostic, token), KaFirDiagnostic.NonSuspendOverriddenBySuspend

internal class OverridingIgnorableWithMustUseImpl(
    override val method: KaCallableSymbol,
    override val parentClass: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.OverridingIgnorableWithMustUse

internal class ManyCompanionObjectsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtObjectDeclaration>(firDiagnostic, token), KaFirDiagnostic.ManyCompanionObjects

internal class ConflictingOverloadsImpl(
    override val conflictingOverloads: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConflictingOverloads

internal class RedeclarationImpl(
    override val conflictingDeclarations: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.Redeclaration

internal class ClassifierRedeclarationImpl(
    override val conflictingDeclarations: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ClassifierRedeclaration

internal class PackageConflictsWithClassifierImpl(
    override val conflictingClassId: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPackageDirective>(firDiagnostic, token), KaFirDiagnostic.PackageConflictsWithClassifier

internal class ExpectAndActualInTheSameModuleImpl(
    override val declaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectAndActualInTheSameModule

internal class MethodOfAnyImplementedInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MethodOfAnyImplementedInInterface

internal class ExtensionShadowedByMemberImpl(
    override val member: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExtensionShadowedByMember

internal class ExtensionFunctionShadowedByMemberPropertyWithInvokeImpl(
    override val member: KaCallableSymbol,
    override val invokeOperator: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExtensionFunctionShadowedByMemberPropertyWithInvoke

internal class LocalObjectNotAllowedImpl(
    override val objectName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.LocalObjectNotAllowed

internal class LocalInterfaceNotAllowedImpl(
    override val interfaceName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.LocalInterfaceNotAllowed

internal class AbstractFunctionInNonAbstractClassImpl(
    override val function: KaCallableSymbol,
    override val containingClass: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.AbstractFunctionInNonAbstractClass

internal class AbstractFunctionWithBodyImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.AbstractFunctionWithBody

internal class NonAbstractFunctionWithNoBodyImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.NonAbstractFunctionWithNoBody

internal class PrivateFunctionWithNoBodyImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.PrivateFunctionWithNoBody

internal class NonMemberFunctionNoBodyImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.NonMemberFunctionNoBody

internal class FunctionDeclarationWithNoNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.FunctionDeclarationWithNoName

internal class AnonymousFunctionWithNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.AnonymousFunctionWithName

internal class SingleAnonymousFunctionWithNameErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.SingleAnonymousFunctionWithNameError

internal class SingleAnonymousFunctionWithNameWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.SingleAnonymousFunctionWithNameWarning

internal class AnonymousFunctionParameterWithDefaultValueImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.AnonymousFunctionParameterWithDefaultValue

internal class UselessVarargOnParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.UselessVarargOnParameter

internal class MultipleVarargParametersImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.MultipleVarargParameters

internal class ForbiddenVarargParameterTypeImpl(
    override val varargParameterType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ForbiddenVarargParameterType

internal class ValueParameterWithoutExplicitTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ValueParameterWithoutExplicitType

internal class CannotInferParameterTypeImpl(
    override val parameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotInferParameterType

internal class CannotInferValueParameterTypeImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotInferValueParameterType

internal class CannotInferItParameterTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotInferItParameterType

internal class CannotInferReceiverParameterTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotInferReceiverParameterType

internal class NoTailCallsFoundImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.NoTailCallsFound

internal class TailrecOnVirtualMemberErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.TailrecOnVirtualMemberError

internal class NonTailRecursiveCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonTailRecursiveCall

internal class TailRecursionInTryIsNotSupportedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TailRecursionInTryIsNotSupported

internal class DataObjectCustomEqualsOrHashCodeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.DataObjectCustomEqualsOrHashCode

internal class DefaultValueNotAllowedInOverrideImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DefaultValueNotAllowedInOverride

internal class FunInterfaceWrongCountOfAbstractMembersImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClass>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceWrongCountOfAbstractMembers

internal class FunInterfaceCannotHaveAbstractPropertiesImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceCannotHaveAbstractProperties

internal class FunInterfaceAbstractMethodWithTypeParametersImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceAbstractMethodWithTypeParameters

internal class FunInterfaceAbstractMethodWithDefaultValueImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceAbstractMethodWithDefaultValue

internal class FunInterfaceWithSuspendFunctionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.FunInterfaceWithSuspendFunction

internal class AbstractPropertyInNonAbstractClassImpl(
    override val property: KaCallableSymbol,
    override val containingClass: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyInNonAbstractClass

internal class PrivatePropertyInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.PrivatePropertyInInterface

internal class AbstractPropertyWithInitializerImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyWithInitializer

internal class PropertyInitializerInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.PropertyInitializerInInterface

internal class PropertyWithNoTypeNoInitializerImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.PropertyWithNoTypeNoInitializer

internal class AbstractPropertyWithoutTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyWithoutType

internal class LateinitPropertyWithoutTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.LateinitPropertyWithoutType

internal class MustBeInitializedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitialized

internal class MustBeInitializedWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedWarning

internal class MustBeInitializedOrBeFinalImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrBeFinal

internal class MustBeInitializedOrBeFinalWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrBeFinalWarning

internal class MustBeInitializedOrBeAbstractImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrBeAbstract

internal class MustBeInitializedOrBeAbstractWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrBeAbstractWarning

internal class MustBeInitializedOrFinalOrAbstractImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrFinalOrAbstract

internal class MustBeInitializedOrFinalOrAbstractWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.MustBeInitializedOrFinalOrAbstractWarning

internal class ExplicitFieldMustBeInitializedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.ExplicitFieldMustBeInitialized

internal class ExtensionPropertyMustHaveAccessorsOrBeAbstractImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.ExtensionPropertyMustHaveAccessorsOrBeAbstract

internal class UnnecessaryLateinitImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.UnnecessaryLateinit

internal class BackingFieldInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.BackingFieldInInterface

internal class ExtensionPropertyWithBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExtensionPropertyWithBackingField

internal class PropertyInitializerNoBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.PropertyInitializerNoBackingField

internal class AbstractDelegatedPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AbstractDelegatedProperty

internal class DelegatedPropertyInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegatedPropertyInInterface

internal class AbstractPropertyWithGetterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPropertyAccessor>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyWithGetter

internal class AbstractPropertyWithSetterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPropertyAccessor>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyWithSetter

internal class PrivateSetterForAbstractPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.PrivateSetterForAbstractProperty

internal class PrivateSetterForOpenPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.PrivateSetterForOpenProperty

internal class ValWithSetterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPropertyAccessor>(firDiagnostic, token), KaFirDiagnostic.ValWithSetter

internal class ConstValNotTopLevelOrObjectImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConstValNotTopLevelOrObject

internal class ConstValWithGetterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConstValWithGetter

internal class ConstValWithDelegateImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ConstValWithDelegate

internal class TypeCantBeUsedForConstValImpl(
    override val constValType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.TypeCantBeUsedForConstVal

internal class ConstValWithoutInitializerImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.ConstValWithoutInitializer

internal class ConstValWithEbfImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.ConstValWithEbf

internal class ConstValWithNonConstInitializerImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ConstValWithNonConstInitializer

internal class DelegateUsesExtensionPropertyTypeParameterErrorImpl(
    override val usedTypeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.DelegateUsesExtensionPropertyTypeParameterError

internal class GetterVisibilityDiffersFromPropertyVisibilityImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.GetterVisibilityDiffersFromPropertyVisibility

internal class SetterVisibilityInconsistentWithPropertyVisibilityImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.SetterVisibilityInconsistentWithPropertyVisibility

internal class WrongGetterReturnTypeImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongGetterReturnType

internal class WrongSetterReturnTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongSetterReturnType

internal class WrongSetterParameterTypeImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongSetterParameterType

internal class AccessorForDelegatedPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtPropertyAccessor>(firDiagnostic, token), KaFirDiagnostic.AccessorForDelegatedProperty

internal class PropertyInitializerWithExplicitFieldDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.PropertyInitializerWithExplicitFieldDeclaration

internal class PropertyFieldDeclarationMissingInitializerImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.PropertyFieldDeclarationMissingInitializer

internal class LateinitNullableBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.LateinitNullableBackingField

internal class BackingFieldForDelegatedPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.BackingFieldForDelegatedProperty

internal class VarPropertyWithExplicitBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VarPropertyWithExplicitBackingField

internal class NonFinalPropertyWithExplicitBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.NonFinalPropertyWithExplicitBackingField

internal class ExpectPropertyWithExplicitBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExpectPropertyWithExplicitBackingField

internal class InconsistentBackingFieldTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.InconsistentBackingFieldType

internal class ExplicitFieldVisibilityMustBeLessPermissiveImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.ExplicitFieldVisibilityMustBeLessPermissive

internal class PropertyWithExplicitFieldAndAccessorsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PropertyWithExplicitFieldAndAccessors

internal class ExplicitBackingFieldInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.ExplicitBackingFieldInInterface

internal class ExplicitBackingFieldInAbstractPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.ExplicitBackingFieldInAbstractProperty

internal class ExplicitBackingFieldInExtensionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.ExplicitBackingFieldInExtension

internal class RedundantExplicitBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBackingField>(firDiagnostic, token), KaFirDiagnostic.RedundantExplicitBackingField

internal class AbstractPropertyInPrimaryConstructorParametersImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.AbstractPropertyInPrimaryConstructorParameters

internal class LocalVariableWithTypeParametersWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.LocalVariableWithTypeParametersWarning

internal class LocalVariableWithTypeParametersImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtProperty>(firDiagnostic, token), KaFirDiagnostic.LocalVariableWithTypeParameters

internal class ExplicitTypeArgumentsInPropertyAccessImpl(
    override val kind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExplicitTypeArgumentsInPropertyAccess

internal class ExplicitTypeArgumentsInPropertyAccessWarningImpl(
    override val kind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExplicitTypeArgumentsInPropertyAccessWarning

internal class SafeCallableReferenceCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.SafeCallableReferenceCall

internal class LateinitIntrinsicCallOnNonLiteralImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LateinitIntrinsicCallOnNonLiteral

internal class LateinitIntrinsicCallOnNonLateinitImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LateinitIntrinsicCallOnNonLateinit

internal class LateinitIntrinsicCallInInlineFunctionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LateinitIntrinsicCallInInlineFunction

internal class LateinitIntrinsicCallOnNonAccessiblePropertyImpl(
    override val declaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LateinitIntrinsicCallOnNonAccessibleProperty

internal class LocalExtensionPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LocalExtensionProperty

internal class UnnamedVarPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnnamedVarProperty

internal class UnnamedDelegatedPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnnamedDelegatedProperty

internal class UnnamedPropertyWithImplicitIgnorableTypeImpl(
    override val ignorableType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnnamedPropertyWithImplicitIgnorableType

internal class DestructuringShortFormNameMismatchImpl(
    override val destructuredName: Name,
    override val propertyName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DestructuringShortFormNameMismatch

internal class DestructuringShortFormOfNonDataClassImpl(
    override val rhsType: KaType,
    override val destructuredName: Name,
    override val target: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DestructuringShortFormOfNonDataClass

internal class DestructuringShortFormUnderscoreImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DestructuringShortFormUnderscore

internal class NameBasedDestructuringUnderscoreWithoutRenamingImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NameBasedDestructuringUnderscoreWithoutRenaming

internal class ExpectedDeclarationWithBodyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectedDeclarationWithBody

internal class ExpectedClassConstructorDelegationCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtConstructorDelegationCall>(firDiagnostic, token), KaFirDiagnostic.ExpectedClassConstructorDelegationCall

internal class ExpectedClassConstructorPropertyParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ExpectedClassConstructorPropertyParameter

internal class ExpectedEnumConstructorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtConstructor<*>>(firDiagnostic, token), KaFirDiagnostic.ExpectedEnumConstructor

internal class ExpectedEnumEntryWithBodyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtEnumEntry>(firDiagnostic, token), KaFirDiagnostic.ExpectedEnumEntryWithBody

internal class ExpectedPropertyInitializerImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExpectedPropertyInitializer

internal class ExpectedDelegatedPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExpectedDelegatedProperty

internal class ExpectedLateinitPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ExpectedLateinitProperty

internal class SupertypeInitializedInExpectedClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SupertypeInitializedInExpectedClass

internal class ExpectedPrivateDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ExpectedPrivateDeclaration

internal class ExpectedExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ExpectedExternalDeclaration

internal class ExpectedTailrecFunctionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.ExpectedTailrecFunction

internal class ImplementationByDelegationInExpectClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDelegatedSuperTypeEntry>(firDiagnostic, token), KaFirDiagnostic.ImplementationByDelegationInExpectClass

internal class ActualTypeAliasNotToClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasNotToClass

internal class ActualTypeAliasToClassWithDeclarationSiteVarianceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasToClassWithDeclarationSiteVariance

internal class ActualTypeAliasWithUseSiteVarianceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasWithUseSiteVariance

internal class ActualTypeAliasWithComplexSubstitutionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasWithComplexSubstitution

internal class ActualTypeAliasToNullableTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasToNullableType

internal class ActualTypeAliasToNothingImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypeAliasToNothing

internal class ActualFunctionWithDefaultArgumentsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtFunction>(firDiagnostic, token), KaFirDiagnostic.ActualFunctionWithDefaultArguments

internal class DefaultArgumentsInExpectWithActualTypealiasImpl(
    override val expectClassSymbol: KaClassLikeSymbol,
    override val members: List<KaCallableSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.DefaultArgumentsInExpectWithActualTypealias

internal class DefaultArgumentsInExpectActualizedByFakeOverrideImpl(
    override val expectClassSymbol: KaClassLikeSymbol,
    override val members: List<KaFunctionSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClass>(firDiagnostic, token), KaFirDiagnostic.DefaultArgumentsInExpectActualizedByFakeOverride

internal class ExpectedFunctionSourceWithDefaultArgumentsNotFoundImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ExpectedFunctionSourceWithDefaultArgumentsNotFound

internal class ActualWithoutExpectImpl(
    override val declaration: KaSymbol,
    override val compatibility: Map<ExpectActualMatchingCompatibility, List<KaSymbol>>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ActualWithoutExpect

internal class ExpectActualIncompatibleClassTypeParameterCountImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassTypeParameterCount

internal class ExpectActualIncompatibleReturnTypeImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleReturnType

internal class ExpectActualIncompatibleEqualityBoundsImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleEqualityBounds

internal class ExpectActualIncompatibleParameterNamesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleParameterNames

internal class ExpectActualIncompatibleContextParameterNamesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleContextParameterNames

internal class ExpectActualIncompatibleTypeParameterNamesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleTypeParameterNames

internal class ExpectActualIncompatibleValueParameterVarargImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleValueParameterVararg

internal class ExpectActualIncompatibleValueParameterNoinlineImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleValueParameterNoinline

internal class ExpectActualIncompatibleValueParameterCrossinlineImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleValueParameterCrossinline

internal class ExpectActualIncompatibleFunctionModifiersDifferentImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleFunctionModifiersDifferent

internal class ExpectActualIncompatibleFunctionModifiersNotSubsetImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleFunctionModifiersNotSubset

internal class ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverrideImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride

internal class ExpectActualIncompatiblePropertyKindImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatiblePropertyKind

internal class ExpectActualIncompatiblePropertyLateinitModifierImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatiblePropertyLateinitModifier

internal class ExpectActualIncompatiblePropertyConstModifierImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatiblePropertyConstModifier

internal class ExpectActualIncompatiblePropertySetterVisibilityImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatiblePropertySetterVisibility

internal class ExpectActualIncompatibleClassKindImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassKind

internal class ExpectActualIncompatibleClassModifiersImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassModifiers

internal class ExpectActualIncompatibleFunInterfaceModifierImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleFunInterfaceModifier

internal class ExpectActualIncompatibleSupertypesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleSupertypes

internal class ExpectActualIncompatibleNestedTypeAliasImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleNestedTypeAlias

internal class ExpectActualIncompatibleEnumEntriesImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleEnumEntries

internal class ExpectActualIncompatibleIllegalRequiresOptInImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleIllegalRequiresOptIn

internal class ExpectActualIncompatibleModalityImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleModality

internal class ExpectActualIncompatibleVisibilityImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleVisibility

internal class ExpectActualIncompatibleClassTypeParameterUpperBoundsImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassTypeParameterUpperBounds

internal class ExpectActualIncompatibleTypeParameterVarianceImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleTypeParameterVariance

internal class ExpectActualIncompatibleTypeParameterReifiedImpl(
    override val expectDeclaration: KaSymbol,
    override val actualDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleTypeParameterReified

internal class ExpectActualIncompatibleClassScopeImpl(
    override val actualClass: KaSymbol,
    override val expectMemberDeclaration: KaSymbol,
    override val actualMemberDeclaration: KaSymbol,
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualIncompatibleClassScope

internal class ExpectRefinementAnnotationWrongTargetImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectRefinementAnnotationWrongTarget

internal class AmbiguousExpectsImpl(
    override val declaration: KaSymbol,
    override val modules: List<FirModuleData>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.AmbiguousExpects

internal class NoActualClassMemberForExpectedClassImpl(
    override val declaration: KaSymbol,
    override val members: List<Pair<KaSymbol, Map<Mismatch, List<KaSymbol>>>>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.NoActualClassMemberForExpectedClass

internal class ActualMissingImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ActualMissing

internal class ExpectRefinementAnnotationMissingImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectRefinementAnnotationMissing

internal class ExpectActualClassifiersAreInBetaWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassLikeDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualClassifiersAreInBetaWarning

internal class NotAMultiplatformCompilationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotAMultiplatformCompilation

internal class ExpectActualOptInAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExpectActualOptInAnnotation

internal class ActualTypealiasToSpecialAnnotationImpl(
    override val typealiasedClassId: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeAlias>(firDiagnostic, token), KaFirDiagnostic.ActualTypealiasToSpecialAnnotation

internal class ActualAnnotationsNotMatchExpectImpl(
    override val expectSymbol: KaSymbol,
    override val actualSymbol: KaSymbol,
    override val actualAnnotationTargetSourceElement: PsiElement?,
    override val incompatibilityType: ExpectActualAnnotationsIncompatibilityType<FirAnnotation>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ActualAnnotationsNotMatchExpect

internal class ActualIgnorabilityNotMatchExpectImpl(
    override val expectDeclaration: KaSymbol,
    override val expectIgnorability: ReturnValueStatus,
    override val actualDeclaration: KaSymbol,
    override val actualIgnorability: ReturnValueStatus,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.ActualIgnorabilityNotMatchExpect

internal class OptionalDeclarationOutsideOfAnnotationEntryImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptionalDeclarationOutsideOfAnnotationEntry

internal class OptionalDeclarationUsageInNonCommonSourceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptionalDeclarationUsageInNonCommonSource

internal class OptionalExpectationNotOnExpectedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OptionalExpectationNotOnExpected

internal class UninitializedVariableImpl(
    override val variable: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UninitializedVariable

internal class UninitializedParameterImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSimpleNameExpression>(firDiagnostic, token), KaFirDiagnostic.UninitializedParameter

internal class UninitializedEnumEntryImpl(
    override val enumEntry: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UninitializedEnumEntry

internal class UninitializedEnumCompanionImpl(
    override val enumClass: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UninitializedEnumCompanion

internal class ValReassignmentImpl(
    override val variable: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ValReassignment

internal class ValReassignmentViaBackingFieldErrorImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ValReassignmentViaBackingFieldError

internal class CapturedValInitializationImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CapturedValInitialization

internal class CapturedMemberValInitializationImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CapturedMemberValInitialization

internal class NonInlineMemberValInitializationImpl(
    override val property: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NonInlineMemberValInitialization

internal class SetterProjectedOutImpl(
    override val receiverType: KaType,
    override val projection: String,
    override val property: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.SetterProjectedOut

internal class WrongInvocationKindImpl(
    override val declaration: KaSymbol,
    override val requiredRange: EventOccurrencesRange,
    override val actualRange: EventOccurrencesRange,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongInvocationKind

internal class LeakedInPlaceLambdaImpl(
    override val lambda: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LeakedInPlaceLambda

internal class VariableWithNoTypeNoInitializerImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtVariableDeclaration>(firDiagnostic, token), KaFirDiagnostic.VariableWithNoTypeNoInitializer

internal class InitializationBeforeDeclarationImpl(
    override val property: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.InitializationBeforeDeclaration

internal class InitializationBeforeDeclarationWarningImpl(
    override val property: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.InitializationBeforeDeclarationWarning

internal class UnreachableCodeImpl(
    override val reachable: List<PsiElement>,
    override val unreachable: List<PsiElement>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnreachableCode

internal class SenselessComparisonImpl(
    override val compareResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.SenselessComparison

internal class SenselessNullInWhenImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SenselessNullInWhen

internal class TypecheckerHasRunIntoRecursiveProblemImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.TypecheckerHasRunIntoRecursiveProblem

internal class ReturnValueNotUsedImpl(
    override val functionName: Name?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ReturnValueNotUsed

internal class ReturnValueNotUsedCoercionImpl(
    override val functionName: Name?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ReturnValueNotUsedCoercion

internal class NullForNonnullTypeImpl(
    override val expectedType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NullForNonnullType

internal class UnsafeCallImpl(
    override val receiverType: KaType,
    override val receiverExpression: KtExpression?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsafeCall

internal class UnsafeImplicitInvokeCallImpl(
    override val receiverType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsafeImplicitInvokeCall

internal class UnsafeInfixCallImpl(
    override val receiverType: KaType,
    override val receiverExpression: KtExpression,
    override val operator: String,
    override val argumentExpression: KtExpression?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UnsafeInfixCall

internal class UnsafeOperatorCallImpl(
    override val receiverType: KaType,
    override val receiverExpression: KtExpression,
    override val operator: String,
    override val argumentExpression: KtExpression?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UnsafeOperatorCall

internal class UnsafeCallableReferenceImpl(
    override val receiverType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnsafeCallableReference

internal class IteratorOnNullableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.IteratorOnNullable

internal class ComponentFunctionOnNullableImpl(
    override val componentFunctionName: Name,
    override val destructingType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ComponentFunctionOnNullable

internal class UnexpectedSafeCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnexpectedSafeCall

internal class UnnecessarySafeCallImpl(
    override val receiverType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnnecessarySafeCall

internal class UnnecessaryNotNullAssertionImpl(
    override val receiverType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.UnnecessaryNotNullAssertion

internal class NotNullAssertionOnLambdaExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NotNullAssertionOnLambdaExpression

internal class NotNullAssertionOnCallableReferenceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NotNullAssertionOnCallableReference

internal class UselessElvisImpl(
    override val receiverType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.UselessElvis

internal class UselessElvisRightIsNullImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.UselessElvisRightIsNull

internal class UselessElvisLeftIsNullImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.UselessElvisLeftIsNull

internal class CannotCheckForErasedImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CannotCheckForErased

internal class UnsafeCastRelyingOnNullImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.UnsafeCastRelyingOnNull

internal class SafeCastRelyingOnNullImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.SafeCastRelyingOnNull

internal class CastNeverSucceedsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.CastNeverSucceeds

internal class UselessCastImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.UselessCast

internal class UncheckedCastImpl(
    override val originalType: KaType,
    override val targetType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.UncheckedCast

internal class NumericCastNeverSucceedsButCanBeReplacedWithToCallImpl(
    override val targetType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.NumericCastNeverSucceedsButCanBeReplacedWithToCall

internal class IntegerLiteralCastInsteadOfToCallImpl(
    override val targetType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>(firDiagnostic, token), KaFirDiagnostic.IntegerLiteralCastInsteadOfToCall

internal class ImpossibleIsCheckErrorImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImpossibleIsCheckError

internal class ImpossibleIsCheckWarningImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImpossibleIsCheckWarning

internal class ImpossibleIsCheckDeprecationErrorImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImpossibleIsCheckDeprecationError

internal class ImpossibleIsCheckDeprecationWarningImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImpossibleIsCheckDeprecationWarning

internal class ImpossibleIsCheckRelyingOnNullErrorImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImpossibleIsCheckRelyingOnNullError

internal class ImpossibleIsCheckRelyingOnNullWarningImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImpossibleIsCheckRelyingOnNullWarning

internal class ImpossibleIsCheckRelyingOnNullDeprecationErrorImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImpossibleIsCheckRelyingOnNullDeprecationError

internal class ImpossibleIsCheckRelyingOnNullDeprecationWarningImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImpossibleIsCheckRelyingOnNullDeprecationWarning

internal class UselessIsCheckImpl(
    override val compileTimeCheckResult: Boolean,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UselessIsCheck

internal class IsEnumEntryImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IsEnumEntry

internal class DynamicNotAllowedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DynamicNotAllowed

internal class EnumEntryAsTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.EnumEntryAsType

internal class ExpectedConditionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenCondition>(firDiagnostic, token), KaFirDiagnostic.ExpectedCondition

internal class NoElseInWhenImpl(
    override val missingWhenCases: List<KaWhenMissingCase>,
    override val description: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenExpression>(firDiagnostic, token), KaFirDiagnostic.NoElseInWhen

internal class MissingBranchForNonAbstractSealedClassImpl(
    override val missingWhenCases: List<KaWhenMissingCase>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenExpression>(firDiagnostic, token), KaFirDiagnostic.MissingBranchForNonAbstractSealedClass

internal class InvalidIfAsExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtIfExpression>(firDiagnostic, token), KaFirDiagnostic.InvalidIfAsExpression

internal class ElseMisplacedInWhenImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenEntry>(firDiagnostic, token), KaFirDiagnostic.ElseMisplacedInWhen

internal class RedundantElseInWhenImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtWhenEntry>(firDiagnostic, token), KaFirDiagnostic.RedundantElseInWhen

internal class IllegalDeclarationInWhenSubjectImpl(
    override val illegalReason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IllegalDeclarationInWhenSubject

internal class CommaInWhenConditionWithoutArgumentImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CommaInWhenConditionWithoutArgument

internal class DuplicateBranchConditionInWhenImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DuplicateBranchConditionInWhen

internal class ConfusingBranchConditionErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConfusingBranchConditionError

internal class WrongConditionSuggestGuardImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongConditionSuggestGuard

internal class CommaInWhenConditionWithWhenGuardImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CommaInWhenConditionWithWhenGuard

internal class WhenGuardWithoutSubjectImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WhenGuardWithoutSubject

internal class InferredInvisibleWhenTypeWarningImpl(
    override val whenType: KaType,
    override val syntaxConstructionName: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InferredInvisibleWhenTypeWarning

internal class TypeParameterIsNotAnExpressionImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSimpleNameExpression>(firDiagnostic, token), KaFirDiagnostic.TypeParameterIsNotAnExpression

internal class TypeParameterOnLhsOfDotImpl(
    override val typeParameter: KaTypeParameterSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtSimpleNameExpression>(firDiagnostic, token), KaFirDiagnostic.TypeParameterOnLhsOfDot

internal class NoCompanionObjectImpl(
    override val klass: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NoCompanionObject

internal class ExpressionExpectedPackageFoundImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ExpressionExpectedPackageFound

internal class ErrorInContractDescriptionImpl(
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ErrorInContractDescription

internal class ContractNotAllowedImpl(
    override val reason: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ContractNotAllowed

internal class NoGetMethodImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtArrayAccessExpression>(firDiagnostic, token), KaFirDiagnostic.NoGetMethod

internal class NoSetMethodImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtArrayAccessExpression>(firDiagnostic, token), KaFirDiagnostic.NoSetMethod

internal class IteratorMissingImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.IteratorMissing

internal class HasNextMissingImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.HasNextMissing

internal class NextMissingImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NextMissing

internal class ComponentFunctionMissingImpl(
    override val missingFunctionName: Name,
    override val destructingType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ComponentFunctionMissing

internal class DelegateSpecialFunctionMissingImpl(
    override val expectedFunctionSignature: String,
    override val delegateType: KaType,
    override val description: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.DelegateSpecialFunctionMissing

internal class UnderscoreIsReservedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnderscoreIsReserved

internal class UnderscoreUsageWithoutBackticksImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnderscoreUsageWithoutBackticks

internal class ResolvedToUnderscoreNamedCatchParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNameReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.ResolvedToUnderscoreNamedCatchParameter

internal class InvalidCharactersImpl(
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidCharacters

internal class EqualityNotApplicableImpl(
    override val operator: String,
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.EqualityNotApplicable

internal class EqualityNotApplicableWarningImpl(
    override val operator: String,
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtBinaryExpression>(firDiagnostic, token), KaFirDiagnostic.EqualityNotApplicableWarning

internal class IncompatibleEnumComparisonErrorImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleEnumComparisonError

internal class IncompatibleEnumComparisonImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleEnumComparison

internal class ForbiddenIdentityEqualsImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ForbiddenIdentityEquals

internal class ForbiddenIdentityEqualsWarningImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ForbiddenIdentityEqualsWarning

internal class DeprecatedIdentityEqualsImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedIdentityEquals

internal class ImplicitBoxingInIdentityEqualsImpl(
    override val leftType: KaType,
    override val rightType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ImplicitBoxingInIdentityEquals

internal class IncDecShouldNotReturnUnitImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.IncDecShouldNotReturnUnit

internal class AssignmentOperatorShouldReturnUnitImpl(
    override val functionSymbol: KaFunctionSymbol,
    override val operator: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.AssignmentOperatorShouldReturnUnit

internal class InitializerRequiredForDestructuringDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDestructuringDeclaration>(firDiagnostic, token), KaFirDiagnostic.InitializerRequiredForDestructuringDeclaration

internal class NotFunctionAsOperatorImpl(
    override val elementName: String,
    override val elementSymbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NotFunctionAsOperator

internal class DslScopeViolationImpl(
    override val calleeSymbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DslScopeViolation

internal class ReceiverShadowedByContextParameterImpl(
    override val calleeSymbol: KaSymbol,
    override val isDispatchOfMemberExtension: Boolean,
    override val contextParameterSymbols: List<KaSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ReceiverShadowedByContextParameter

internal class RecursiveTypealiasExpansionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RecursiveTypealiasExpansion

internal class TypealiasShouldExpandToClassImpl(
    override val expandedType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasShouldExpandToClass

internal class ConstructorOrSupertypeOnTypealiasWithTypeProjectionErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConstructorOrSupertypeOnTypealiasWithTypeProjectionError

internal class ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning

internal class TypealiasExpansionCapturesOuterTypeParametersImpl(
    override val outerTypeParameters: List<KaTypeParameterSymbol>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpansionCapturesOuterTypeParameters

internal class TypealiasExpandsToCompilerRequiredAnnotationErrorImpl(
    override val annotation: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpandsToCompilerRequiredAnnotationError

internal class TypealiasExpandsToCompilerRequiredAnnotationWarningImpl(
    override val annotation: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.TypealiasExpandsToCompilerRequiredAnnotationWarning

internal class ExpectedTypealiasImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExpectedTypealias

internal class RedundantVisibilityModifierImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.RedundantVisibilityModifier

internal class RedundantModalityModifierImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtModifierListOwner>(firDiagnostic, token), KaFirDiagnostic.RedundantModalityModifier

internal class RedundantReturnUnitTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RedundantReturnUnitType

internal class RedundantSingleExpressionStringTemplateImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantSingleExpressionStringTemplate

internal class CanBeValImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CanBeVal

internal class CanBeValLateinitImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CanBeValLateinit

internal class CanBeValDelayedInitializationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.CanBeValDelayedInitialization

internal class RedundantCallOfConversionMethodImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantCallOfConversionMethod

internal class ArrayEqualityOperatorCanBeReplacedWithContentEqualsImpl(
    override val operator: String,
    override val replacementPrefix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.ArrayEqualityOperatorCanBeReplacedWithContentEquals

internal class EmptyRangeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.EmptyRange

internal class RedundantSetterParameterTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.RedundantSetterParameterType

internal class UnusedVariableImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.UnusedVariable

internal class AssignedValueIsNeverReadImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.AssignedValueIsNeverRead

internal class VariableInitializerIsRedundantImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.VariableInitializerIsRedundant

internal class VariableNeverReadImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.VariableNeverRead

internal class UselessCallOnNotNullImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UselessCallOnNotNull

internal class UnusedAnonymousParameterImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UnusedAnonymousParameter

internal class UnusedExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnusedExpression

internal class UnusedLambdaExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnusedLambdaExpression

internal class ReturnNotAllowedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnNotAllowed

internal class NotAFunctionLabelImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.NotAFunctionLabel

internal class ReturnInFunctionWithExpressionBodyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnInFunctionWithExpressionBody

internal class ReturnInFunctionWithExpressionBodyWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnInFunctionWithExpressionBodyWarning

internal class ReturnInFunctionWithExpressionBodyAndImplicitTypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnInFunctionWithExpressionBodyAndImplicitType

internal class NoReturnInFunctionWithBlockBodyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclarationWithBody>(firDiagnostic, token), KaFirDiagnostic.NoReturnInFunctionWithBlockBody

internal class RedundantReturnImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.RedundantReturn

internal class AnonymousInitializerInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnonymousInitializer>(firDiagnostic, token), KaFirDiagnostic.AnonymousInitializerInInterface

internal class UsageIsNotInlinableImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UsageIsNotInlinable

internal class NonLocalReturnNotAllowedImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonLocalReturnNotAllowed

internal class NotYetSupportedInInlineImpl(
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NotYetSupportedInInline

internal class NotYetSupportedInInlineWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NotYetSupportedInInlineWarning

internal class NothingToInlineImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NothingToInline

internal class NullableInlineParameterImpl(
    override val parameter: KaSymbol,
    override val function: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NullableInlineParameter

internal class RecursionInInlineImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.RecursionInInline

internal class NonPublicCallFromPublicInlineImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicCallFromPublicInline

internal class NonPublicInlineCallFromPublicInlineImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicInlineCallFromPublicInline

internal class NonPublicCallFromPublicInlineDeprecationImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicCallFromPublicInlineDeprecation

internal class NonPublicDataCopyCallFromPublicInlineErrorImpl(
    override val inlineDeclaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicDataCopyCallFromPublicInlineError

internal class NonPublicDataCopyCallFromPublicInlineWarningImpl(
    override val inlineDeclaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonPublicDataCopyCallFromPublicInlineWarning

internal class ProtectedConstructorCallFromPublicInlineImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ProtectedConstructorCallFromPublicInline

internal class ProtectedCallFromPublicInlineErrorImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ProtectedCallFromPublicInlineError

internal class PrivateClassMemberFromInlineImpl(
    override val inlineDeclaration: KaSymbol,
    override val referencedDeclaration: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.PrivateClassMemberFromInline

internal class SuperCallFromPublicInlineImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SuperCallFromPublicInline

internal class DeclarationCantBeInlinedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.DeclarationCantBeInlined

internal class DeclarationCantBeInlinedDeprecationErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.DeclarationCantBeInlinedDeprecationError

internal class DeclarationCantBeInlinedDeprecationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.DeclarationCantBeInlinedDeprecationWarning

internal class OverrideByInlineImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.OverrideByInline

internal class InvalidDefaultFunctionalParameterForInlineImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InvalidDefaultFunctionalParameterForInline

internal class NotSupportedInlineParameterInInlineParameterDefaultValueImpl(
    override val parameter: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NotSupportedInlineParameterInInlineParameterDefaultValue

internal class ReifiedTypeParameterInOverrideImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ReifiedTypeParameterInOverride

internal class InlinePropertyWithBackingFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InlinePropertyWithBackingField

internal class InlinePropertyWithBackingFieldDeprecationErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InlinePropertyWithBackingFieldDeprecationError

internal class InlinePropertyWithBackingFieldDeprecationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InlinePropertyWithBackingFieldDeprecationWarning

internal class IllegalInlineParameterModifierImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IllegalInlineParameterModifier

internal class InlineSuspendFunctionTypeUnsupportedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.InlineSuspendFunctionTypeUnsupported

internal class InefficientEqualsOverridingInValueClassImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.InefficientEqualsOverridingInValueClass

internal class InlineClassDeprecatedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InlineClassDeprecated

internal class LessVisibleTypeAccessInInlineErrorImpl(
    override val typeVisibility: EffectiveVisibility,
    override val type: KaType,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleTypeAccessInInlineError

internal class LessVisibleTypeAccessInInlineWarningImpl(
    override val typeVisibility: EffectiveVisibility,
    override val type: KaType,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleTypeAccessInInlineWarning

internal class LessVisibleTypeInInlineAccessedSignatureErrorImpl(
    override val symbol: KaSymbol,
    override val typeVisibility: EffectiveVisibility,
    override val type: KaType,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleTypeInInlineAccessedSignatureError

internal class LessVisibleTypeInInlineAccessedSignatureWarningImpl(
    override val symbol: KaSymbol,
    override val typeVisibility: EffectiveVisibility,
    override val type: KaType,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.LessVisibleTypeInInlineAccessedSignatureWarning

internal class CallableReferenceToLessVisibleDeclarationInInlineErrorImpl(
    override val symbol: KaSymbol,
    override val visibility: EffectiveVisibility,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceToLessVisibleDeclarationInInlineError

internal class CallableReferenceToLessVisibleDeclarationInInlineWarningImpl(
    override val symbol: KaSymbol,
    override val visibility: EffectiveVisibility,
    override val inlineVisibility: EffectiveVisibility,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CallableReferenceToLessVisibleDeclarationInInlineWarning

internal class ContextParameterMustBeNoinlineImpl(
    override val parameter: KaSymbol,
    override val function: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ContextParameterMustBeNoinline

internal class InlineFromHigherPlatformImpl(
    override val inlinedBytecodeVersion: String,
    override val currentModuleBytecodeVersion: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InlineFromHigherPlatform

internal class CannotAllUnderImportFromSingletonImpl(
    override val objectName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.CannotAllUnderImportFromSingleton

internal class PackageCannotBeImportedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.PackageCannotBeImported

internal class CannotBeImportedImpl(
    override val name: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.CannotBeImported

internal class ConflictingImportImpl(
    override val name: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.ConflictingImport

internal class FunctionTypeOfTooLargeArityImpl(
    override val classId: ClassId,
    override val maxArity: Int,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.FunctionTypeOfTooLargeArity

internal class KSuspendFunctionTypeOfDangerouslyLargeArityImpl(
    override val classId: ClassId,
    override val maxArity: Int,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.KSuspendFunctionTypeOfDangerouslyLargeArity

internal class OperatorRenamedOnImportImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.OperatorRenamedOnImport

internal class TypealiasAsCallableQualifierInImportErrorImpl(
    override val typealiasName: Name,
    override val originalClassName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.TypealiasAsCallableQualifierInImportError

internal class TypealiasAsCallableQualifierInImportWarningImpl(
    override val typealiasName: Name,
    override val originalClassName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtImportDirective>(firDiagnostic, token), KaFirDiagnostic.TypealiasAsCallableQualifierInImportWarning

internal class IllegalSuspendFunctionCallImpl(
    override val suspendCallable: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalSuspendFunctionCall

internal class IllegalSuspendPropertyAccessImpl(
    override val suspendCallable: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalSuspendPropertyAccess

internal class NonLocalSuspensionPointImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonLocalSuspensionPoint

internal class IllegalRestrictedSuspendingFunctionCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalRestrictedSuspendingFunctionCall

internal class NonModifierFormForBuiltInSuspendImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonModifierFormForBuiltInSuspend

internal class ModifierFormForNonBuiltInSuspendImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ModifierFormForNonBuiltInSuspend

internal class ModifierFormForNonBuiltInSuspendFunErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ModifierFormForNonBuiltInSuspendFunError

internal class ReturnForBuiltInSuspendImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtReturnExpression>(firDiagnostic, token), KaFirDiagnostic.ReturnForBuiltInSuspend

internal class MixingSuspendAndNonSuspendSupertypesImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MixingSuspendAndNonSuspendSupertypes

internal class MixingFunctionalKindsInSupertypesImpl(
    override val kinds: List<FunctionTypeKind>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MixingFunctionalKindsInSupertypes

internal class RedundantLabelWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtLabelReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.RedundantLabelWarning

internal class MultipleLabelsAreForbiddenImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtLabelReferenceExpression>(firDiagnostic, token), KaFirDiagnostic.MultipleLabelsAreForbidden

internal class DeprecatedAccessToEnumEntryCompanionPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEnumEntryCompanionProperty

internal class DeprecatedAccessToEntryPropertyFromEnumImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEntryPropertyFromEnum

internal class DeprecatedAccessToEntriesPropertyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEntriesProperty

internal class DeprecatedAccessToEnumEntryPropertyAsReferenceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEnumEntryPropertyAsReference

internal class DeprecatedAccessToEntriesAsQualifierImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DeprecatedAccessToEntriesAsQualifier

internal class DeclarationOfEnumEntryEntriesErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtEnumEntry>(firDiagnostic, token), KaFirDiagnostic.DeclarationOfEnumEntryEntriesError

internal class DeclarationOfEnumEntryEntriesWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtEnumEntry>(firDiagnostic, token), KaFirDiagnostic.DeclarationOfEnumEntryEntriesWarning

internal class IncompatibleClassImpl(
    override val presentableString: String,
    override val incompatibility: IncompatibleVersionErrorData<*>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IncompatibleClass

internal class PreReleaseClassImpl(
    override val presentableString: String,
    override val poisoningFeatures: List<String>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PreReleaseClass

internal class IrWithUnstableAbiCompiledClassImpl(
    override val presentableString: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IrWithUnstableAbiCompiledClass

internal class BuilderInferenceStubReceiverImpl(
    override val typeParameterName: Name,
    override val containingDeclarationName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.BuilderInferenceStubReceiver

internal class BuilderInferenceMultiLambdaRestrictionImpl(
    override val typeParameterName: Name,
    override val containingDeclarationName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.BuilderInferenceMultiLambdaRestriction

internal class InvalidVersioningOnNonOptionalImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidVersioningOnNonOptional

internal class InvalidVersioningOnNonfinalClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidVersioningOnNonfinalClass

internal class InvalidVersioningOnLocalFunctionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidVersioningOnLocalFunction

internal class InvalidVersioningOnAnnotationClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidVersioningOnAnnotationClass

internal class InvalidDefaultValueDependencyImpl(
    override val lowestVersion: MavenComparableVersion?,
    override val highestVersion: MavenComparableVersion?,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidDefaultValueDependency

internal class InvalidNonOptionalParameterPositionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidNonOptionalParameterPosition

internal class InvalidVersioningOnReceiverOrContextParameterPositionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidVersioningOnReceiverOrContextParameterPosition

internal class InvalidVersioningOnVarargImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidVersioningOnVararg

internal class InvalidVersioningOnValueClassParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InvalidVersioningOnValueClassParameter

internal class NonAscendingVersionAnnotationImpl(
    override val lowestVersion: MavenComparableVersion?,
    override val highestVersion: MavenComparableVersion?,
    override val sourceOfHighestVersion: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonAscendingVersionAnnotation

internal class CompanionBlockMemberExtensionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompanionBlockMemberExtension

internal class PrivateConstInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.PrivateConstInInterface

internal class IllegalCompanionBlockImpl(
    override val parent: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalCompanionBlock

internal class CompanionBlockNestedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompanionBlockNested

internal class IllegalCompanionBlockMemberImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalCompanionBlockMember

internal class CompanionExtensionReceiverWithTypeArgumentsImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompanionExtensionReceiverWithTypeArguments

internal class CompanionExtensionReceiverIsObjectImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompanionExtensionReceiverIsObject

internal class CompanionExtensionReceiverIsTypeParameterImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompanionExtensionReceiverIsTypeParameter

internal class CompanionExtensionReceiverAnnotatedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompanionExtensionReceiverAnnotated

internal class CompanionExtensionNullableReceiverImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CompanionExtensionNullableReceiver

internal class OverrideCannotBeStaticImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.OverrideCannotBeStatic

internal class JvmStaticNotInObjectOrClassCompanionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticNotInObjectOrClassCompanion

internal class JvmStaticNotInObjectOrCompanionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticNotInObjectOrCompanion

internal class JvmStaticOnNonPublicMemberImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticOnNonPublicMember

internal class JvmStaticOnConstOrJvmFieldImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticOnConstOrJvmField

internal class JvmStaticOnExternalInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmStaticOnExternalInInterface

internal class InapplicableJvmNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableJvmName

internal class IllegalJvmNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalJvmName

internal class FunctionDelegateMemberNameClashImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FunctionDelegateMemberNameClash

internal class ValueClassWithoutJvmInlineAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ValueClassWithoutJvmInlineAnnotation

internal class JvmInlineWithoutValueClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmInlineWithoutValueClass

internal class InapplicableJvmExposeBoxedWithNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InapplicableJvmExposeBoxedWithName

internal class UselessJvmExposeBoxedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UselessJvmExposeBoxed

internal class JvmExposeBoxedCannotExposeSuspendImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeSuspend

internal class JvmExposeBoxedRequiresNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedRequiresName

internal class JvmExposeBoxedCannotBeTheSameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotBeTheSame

internal class JvmExposeBoxedCannotBeTheSameAsJvmNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotBeTheSameAsJvmName

internal class JvmExposeBoxedCannotExposeOpenAbstractImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeOpenAbstract

internal class JvmExposeBoxedCannotExposeSyntheticImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeSynthetic

internal class JvmExposeBoxedCannotExposeLocalsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeLocals

internal class JvmExposeBoxedCannotExposeReifiedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeReified

internal class JvmExposeBoxedCannotExposePrivateImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposePrivate

internal class JvmExposeBoxedCannotExposeSealedConstructorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCannotExposeSealedConstructor

internal class JvmExposeBoxedCanBeReplacedWithJvmNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmExposeBoxedCanBeReplacedWithJvmName

internal class WrongTypeForJavaOverrideImpl(
    override val override: KaCallableSymbol,
    override val base: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WrongTypeForJavaOverride

internal class AccidentalOverrideClashByJvmSignatureImpl(
    override val hidden: KaFunctionSymbol,
    override val overrideDescription: String,
    override val regular: KaFunctionSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedFunction>(firDiagnostic, token), KaFirDiagnostic.AccidentalOverrideClashByJvmSignature

internal class ImplementationByDelegationWithDifferentGenericSignatureErrorImpl(
    override val base: KaFunctionSymbol,
    override val override: KaFunctionSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ImplementationByDelegationWithDifferentGenericSignatureError

internal class ImplementationByDelegationWithDifferentGenericSignatureWarningImpl(
    override val base: KaFunctionSymbol,
    override val override: KaFunctionSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtTypeReference>(firDiagnostic, token), KaFirDiagnostic.ImplementationByDelegationWithDifferentGenericSignatureWarning

internal class NotYetSupportedLocalInlineFunctionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.NotYetSupportedLocalInlineFunction

internal class PropertyHidesJavaFieldImpl(
    override val hidden: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtCallableDeclaration>(firDiagnostic, token), KaFirDiagnostic.PropertyHidesJavaField

internal class ConflictVersionAndJvmOverloadsAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConflictVersionAndJvmOverloadsAnnotation

internal class JavaTypeMismatchImpl(
    override val expectedType: KaType,
    override val actualType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.JavaTypeMismatch

internal class ReceiverNullabilityMismatchBasedOnJavaAnnotationsImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    override val messageSuffix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ReceiverNullabilityMismatchBasedOnJavaAnnotations

internal class ReceiverMutabilityMismatchBasedOnJavaAnnotationsImpl(
    override val actualType: KaType,
    override val expectedType: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ReceiverMutabilityMismatchBasedOnJavaAnnotations

internal class TypeMismatchBasedOnJavaAnnotationsImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    override val messageSuffix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeMismatchBasedOnJavaAnnotations

internal class NullabilityMismatchBasedOnExplicitTypeArgumentsForJavaImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    override val messageSuffix: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NullabilityMismatchBasedOnExplicitTypeArgumentsForJava

internal class TypeMismatchWhenFlexibilityChangesImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.TypeMismatchWhenFlexibilityChanges

internal class JavaClassOnCompanionImpl(
    override val actualType: KaType,
    override val expectedType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaClassOnCompanion

internal class JavaClassPropertyReferenceErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaClassPropertyReferenceError

internal class JavaClassPropertyReferenceWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaClassPropertyReferenceWarning

internal class UnexhaustiveWhenBasedOnJavaAnnotationsImpl(
    override val subjectType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UnexhaustiveWhenBasedOnJavaAnnotations

internal class WhenSubjectCanBeNullInJavaImpl(
    override val subjectType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.WhenSubjectCanBeNullInJava

internal class UpperBoundCannotBeArrayImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundCannotBeArray

internal class UpperBoundViolatedBasedOnJavaAnnotationsImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedBasedOnJavaAnnotations

internal class UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotationsImpl(
    override val expectedUpperBound: KaType,
    override val actualType: KaType,
    override val onTypeParameter: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations

internal class StrictfpOnClassImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.StrictfpOnClass

internal class SynchronizedOnAbstractImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnAbstract

internal class SynchronizedInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedInInterface

internal class SynchronizedInAnnotationErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedInAnnotationError

internal class SynchronizedInAnnotationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedInAnnotationWarning

internal class SynchronizedOnInlineImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnInline

internal class SynchronizedOnValueClassErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnValueClassError

internal class SynchronizedOnValueClassWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnValueClassWarning

internal class SynchronizedOnSuspendErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.SynchronizedOnSuspendError

internal class OverloadsWithoutDefaultArgumentsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsWithoutDefaultArguments

internal class OverloadsAbstractImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsAbstract

internal class OverloadsInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsInterface

internal class OverloadsLocalImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsLocal

internal class OverloadsAnnotationClassConstructorErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsAnnotationClassConstructorError

internal class OverloadsPrivateImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.OverloadsPrivate

internal class DeprecatedJavaAnnotationImpl(
    override val kotlinName: FqName,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.DeprecatedJavaAnnotation

internal class JvmPackageNameCannotBeEmptyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmPackageNameCannotBeEmpty

internal class JvmPackageNameMustBeValidNameImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmPackageNameMustBeValidName

internal class JvmPackageNameNotSupportedInFilesWithClassesImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmPackageNameNotSupportedInFilesWithClasses

internal class PositionedValueArgumentForJavaAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.PositionedValueArgumentForJavaAnnotation

internal class RedundantRepeatableAnnotationImpl(
    override val kotlinRepeatable: FqName,
    override val javaRepeatable: FqName,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RedundantRepeatableAnnotation

internal class ThrowsInAnnotationErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.ThrowsInAnnotationError

internal class ThrowsInAnnotationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.ThrowsInAnnotationWarning

internal class JvmSerializableLambdaOnInlinedFunctionLiteralsErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmSerializableLambdaOnInlinedFunctionLiteralsError

internal class JvmSerializableLambdaOnInlinedFunctionLiteralsWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmSerializableLambdaOnInlinedFunctionLiteralsWarning

internal class IncompatibleAnnotationTargetsImpl(
    override val missingJavaTargets: List<String>,
    override val correspondingKotlinTargets: List<String>,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.IncompatibleAnnotationTargets

internal class AnnotationTargetsOnlyInJavaImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.AnnotationTargetsOnlyInJava

internal class RuntimeAnnotationOnLambdaIsNotRetainedImpl(
    override val annotationClass: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RuntimeAnnotationOnLambdaIsNotRetained

internal class LocalJvmRecordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.LocalJvmRecord

internal class NonFinalJvmRecordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonFinalJvmRecord

internal class EnumJvmRecordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.EnumJvmRecord

internal class JvmRecordWithoutPrimaryConstructorParametersImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordWithoutPrimaryConstructorParameters

internal class NonDataClassJvmRecordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonDataClassJvmRecord

internal class NonDataValueClassJvmRecordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NonDataValueClassJvmRecord

internal class JvmRecordNotValParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordNotValParameter

internal class JvmRecordNotLastVarargParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordNotLastVarargParameter

internal class InnerJvmRecordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.InnerJvmRecord

internal class FieldInJvmRecordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.FieldInJvmRecord

internal class DelegationByInJvmRecordImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.DelegationByInJvmRecord

internal class JvmRecordExtendsClassImpl(
    override val superType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordExtendsClass

internal class IllegalJavaLangRecordSupertypeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.IllegalJavaLangRecordSupertype

internal class JvmRecordsIllegalBytecodeTargetImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JvmRecordsIllegalBytecodeTarget

internal class JavaModuleDoesNotDependOnModuleImpl(
    override val moduleName: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaModuleDoesNotDependOnModule

internal class JavaModuleDoesNotReadUnnamedModuleImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaModuleDoesNotReadUnnamedModule

internal class JavaModuleDoesNotExportPackageImpl(
    override val moduleName: String,
    override val packageName: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaModuleDoesNotExportPackage

internal class JvmDefaultWithoutCompatibilityNotInEnableModeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JvmDefaultWithoutCompatibilityNotInEnableMode

internal class JvmDefaultWithCompatibilityNotInNoCompatibilityModeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JvmDefaultWithCompatibilityNotInNoCompatibilityMode

internal class ExternalDeclarationCannotBeAbstractImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExternalDeclarationCannotBeAbstract

internal class ExternalDeclarationCannotHaveBodyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExternalDeclarationCannotHaveBody

internal class ExternalDeclarationInInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExternalDeclarationInInterface

internal class ExternalDeclarationCannotBeInlinedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.ExternalDeclarationCannotBeInlined

internal class NonSourceRepeatedAnnotationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.NonSourceRepeatedAnnotation

internal class RepeatedAnnotationWithContainerImpl(
    override val name: ClassId,
    override val explicitContainerName: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatedAnnotationWithContainer

internal class RepeatableContainerMustHaveValueArrayErrorImpl(
    override val container: ClassId,
    override val annotation: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableContainerMustHaveValueArrayError

internal class RepeatableContainerHasNonDefaultParameterErrorImpl(
    override val container: ClassId,
    override val nonDefault: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableContainerHasNonDefaultParameterError

internal class RepeatableContainerHasShorterRetentionErrorImpl(
    override val container: ClassId,
    override val retention: String,
    override val annotation: ClassId,
    override val annotationRetention: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableContainerHasShorterRetentionError

internal class RepeatableContainerTargetSetNotASubsetErrorImpl(
    override val container: ClassId,
    override val annotation: ClassId,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableContainerTargetSetNotASubsetError

internal class RepeatableAnnotationHasNestedClassNamedContainerErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.RepeatableAnnotationHasNestedClassNamedContainerError

internal class SuspensionPointInsideCriticalSectionImpl(
    override val function: KaCallableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SuspensionPointInsideCriticalSection

internal class InapplicableJvmFieldImpl(
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableJvmField

internal class InapplicableJvmFieldWarningImpl(
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.InapplicableJvmFieldWarning

internal class IdentitySensitiveOperationsWithValueTypeImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.IdentitySensitiveOperationsWithValueType

internal class SynchronizedBlockOnJavaValueBasedClassImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SynchronizedBlockOnJavaValueBasedClass

internal class SynchronizedBlockOnValueClassOrPrimitiveErrorImpl(
    override val valueClassOrPrimitive: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SynchronizedBlockOnValueClassOrPrimitiveError

internal class SynchronizedBlockOnValueClassOrPrimitiveWarningImpl(
    override val valueClassOrPrimitive: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SynchronizedBlockOnValueClassOrPrimitiveWarning

internal class JvmSyntheticOnDelegateImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnnotationEntry>(firDiagnostic, token), KaFirDiagnostic.JvmSyntheticOnDelegate

internal class SubclassCantCallCompanionProtectedNonStaticImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SubclassCantCallCompanionProtectedNonStatic

internal class SubclassCantCallCompanionProtectedNonStaticWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SubclassCantCallCompanionProtectedNonStaticWarning

internal class ConcurrentHashMapContainsOperatorErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.ConcurrentHashMapContainsOperatorError

internal class SpreadOnSignaturePolymorphicCallErrorImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SpreadOnSignaturePolymorphicCallError

internal class JavaSamInterfaceConstructorReferenceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaSamInterfaceConstructorReference

internal class NoReflectionInClassPathImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.NoReflectionInClassPath

internal class SyntheticPropertyWithoutJavaOriginImpl(
    override val originalSymbol: KaFunctionSymbol,
    override val functionName: Name,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.SyntheticPropertyWithoutJavaOrigin

internal class JavaFieldShadowedByKotlinPropertyImpl(
    override val kotlinProperty: KaVariableSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JavaFieldShadowedByKotlinProperty

internal class MissingBuiltInDeclarationImpl(
    override val symbol: KaSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.MissingBuiltInDeclaration

internal class DangerousCharactersImpl(
    override val characters: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.DangerousCharacters

internal class ImplementingFunctionInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtClassOrObject>(firDiagnostic, token), KaFirDiagnostic.ImplementingFunctionInterface

internal class OverridingExternalFunWithOptionalParamsImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.OverridingExternalFunWithOptionalParams

internal class OverridingExternalFunWithOptionalParamsWithFakeImpl(
    override val function: KaFunctionSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.OverridingExternalFunWithOptionalParamsWithFake

internal class ExternalEnumEntryWithBodyImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalEnumEntryWithBody

internal class EnumClassInExternalDeclarationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.EnumClassInExternalDeclarationWarning

internal class InlineClassInExternalDeclarationWarningImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InlineClassInExternalDeclarationWarning

internal class InlineClassInExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.InlineClassInExternalDeclaration

internal class ExtensionFunctionInExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExtensionFunctionInExternalDeclaration

internal class JsExternalInheritorsOnlyImpl(
    override val parent: KaClassLikeSymbol,
    override val kid: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.JsExternalInheritorsOnly

internal class JsExternalArgumentImpl(
    override val argType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.JsExternalArgument

internal class WrongExportedDeclarationImpl(
    override val kind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongExportedDeclaration

internal class NonExportableTypeImpl(
    override val kind: String,
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonExportableType

internal class NonExportableTypeInSyntheticCopyFunctionWithExposedCopyVisibilityImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonExportableTypeInSyntheticCopyFunctionWithExposedCopyVisibility

internal class NonExportableTypeInSyntheticCopyWithoutConsistentVisibilityImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonExportableTypeInSyntheticCopyWithoutConsistentVisibility

internal class NonConsumableExportedIdentifierImpl(
    override val name: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonConsumableExportedIdentifier

internal class NamedCompanionInExportedInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NamedCompanionInExportedInterface

internal class NotExportedOrExternalActualDeclarationWhileExpectIsExportedImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NotExportedOrExternalActualDeclarationWhileExpectIsExported

internal class ExposedNotExportedSuperInterfaceErrorImpl(
    override val restrictingDeclaration: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedNotExportedSuperInterfaceError

internal class ExposedNotExportedSuperInterfaceWarningImpl(
    override val restrictingDeclaration: KaClassLikeSymbol,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExposedNotExportedSuperInterfaceWarning

internal class NestedJsExportImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NestedJsExport

internal class MultipleJsExportDefaultInOneFileImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.MultipleJsExportDefaultInOneFile

internal class WrongJsExportTargetVisibilityImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongJsExportTargetVisibility

internal class DelegationByDynamicImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.DelegationByDynamic

internal class PropertyDelegationByDynamicImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.PropertyDelegationByDynamic

internal class SpreadOperatorInDynamicCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.SpreadOperatorInDynamicCall

internal class WrongOperationWithDynamicImpl(
    override val operation: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongOperationWithDynamic

internal class JsStaticNotInObjectImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JsStaticNotInObject

internal class JsStaticOnNonPublicMemberImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JsStaticOnNonPublicMember

internal class JsStaticOnConstImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.JsStaticOnConst

internal class JsNoRuntimeWrongTargetImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNoRuntimeWrongTarget

internal class JsNoRuntimeForbiddenIsCheckImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNoRuntimeForbiddenIsCheck

internal class JsNoRuntimeForbiddenAsCastImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNoRuntimeForbiddenAsCast

internal class JsNoRuntimeForbiddenClassReferenceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNoRuntimeForbiddenClassReference

internal class JsNoRuntimeUselessOnExternalInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNoRuntimeUselessOnExternalInterface

internal class JsNoRuntimeInterfaceAsReifiedTypeArgumentImpl(
    override val typeArgument: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JsNoRuntimeInterfaceAsReifiedTypeArgument

internal class JsActualExternalInterfaceWhileExpectWithoutJsNoRuntimeImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.JsActualExternalInterfaceWhileExpectWithoutJsNoRuntime

internal class JsNoRuntimeActualAnnotationsNotMatchExpectImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtNamedDeclaration>(firDiagnostic, token), KaFirDiagnostic.JsNoRuntimeActualAnnotationsNotMatchExpect

internal class SyntaxImpl(
    override val message: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.Syntax

internal class NestedExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NestedExternalDeclaration

internal class WrongExternalDeclarationImpl(
    override val classKind: String,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.WrongExternalDeclaration

internal class NestedClassInExternalInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NestedClassInExternalInterface

internal class CompanionObjectInExternalInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.CompanionObjectInExternalInterface

internal class InlineExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtDeclaration>(firDiagnostic, token), KaFirDiagnostic.InlineExternalDeclaration

internal class NonAbstractMemberOfExternalInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtExpression>(firDiagnostic, token), KaFirDiagnostic.NonAbstractMemberOfExternalInterface

internal class ExternalClassConstructorPropertyParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtParameter>(firDiagnostic, token), KaFirDiagnostic.ExternalClassConstructorPropertyParameter

internal class ExternalAnonymousInitializerImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtAnonymousInitializer>(firDiagnostic, token), KaFirDiagnostic.ExternalAnonymousInitializer

internal class ExternalDelegationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalDelegation

internal class ExternalDelegatedConstructorCallImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalDelegatedConstructorCall

internal class WrongBodyOfExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongBodyOfExternalDeclaration

internal class WrongInitializerOfExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongInitializerOfExternalDeclaration

internal class WrongDefaultValueForExternalFunParameterImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.WrongDefaultValueForExternalFunParameter

internal class CannotCheckForExternalInterfaceImpl(
    override val targetType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.CannotCheckForExternalInterface

internal class UncheckedCastToExternalInterfaceImpl(
    override val sourceType: KaType,
    override val targetType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.UncheckedCastToExternalInterface

internal class ExternalInterfaceAsClassLiteralImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalInterfaceAsClassLiteral

internal class ExternalInterfaceAsReifiedTypeArgumentImpl(
    override val typeArgument: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalInterfaceAsReifiedTypeArgument

internal class NamedCompanionInExternalInterfaceImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NamedCompanionInExternalInterface

internal class CallToDefinedExternallyFromNonExternalDeclarationImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaFirDiagnostic.CallToDefinedExternallyFromNonExternalDeclaration

internal class ExternalTypeExtendsNonExternalTypeImpl(
    override val superType: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.ExternalTypeExtendsNonExternalType

internal class NonExternalDeclarationInInappropriateFileImpl(
    override val type: KaType,
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.NonExternalDeclarationInInappropriateFile

internal class JscodeArgumentNonConstExpressionImpl(
    firDiagnostic: KtDiagnosticWithSource,
    token: KaLifetimeToken,
) : KaAbstractFirDiagnostic<KtElement>(firDiagnostic, token), KaFirDiagnostic.JscodeArgumentNonConstExpression

