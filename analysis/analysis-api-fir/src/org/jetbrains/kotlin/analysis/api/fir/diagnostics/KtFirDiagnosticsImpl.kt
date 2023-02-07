/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
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
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
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
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Incompatible
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class UnsupportedImpl(
    override val unsupported: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.Unsupported(), KtAbstractFirDiagnostic<PsiElement>

internal class UnsupportedFeatureImpl(
    override val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnsupportedFeature(), KtAbstractFirDiagnostic<PsiElement>

internal class NewInferenceErrorImpl(
    override val error: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NewInferenceError(), KtAbstractFirDiagnostic<PsiElement>

internal class OtherErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OtherError(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalConstExpressionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalConstExpression(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalUnderscoreImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalUnderscore(), KtAbstractFirDiagnostic<PsiElement>

internal class ExpressionExpectedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpressionExpected(), KtAbstractFirDiagnostic<PsiElement>

internal class AssignmentInExpressionContextImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssignmentInExpressionContext(), KtAbstractFirDiagnostic<KtBinaryExpression>

internal class BreakOrContinueOutsideALoopImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.BreakOrContinueOutsideALoop(), KtAbstractFirDiagnostic<PsiElement>

internal class NotALoopLabelImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NotALoopLabel(), KtAbstractFirDiagnostic<PsiElement>

internal class BreakOrContinueJumpsAcrossFunctionBoundaryImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.BreakOrContinueJumpsAcrossFunctionBoundary(), KtAbstractFirDiagnostic<KtExpressionWithLabel>

internal class VariableExpectedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VariableExpected(), KtAbstractFirDiagnostic<PsiElement>

internal class DelegationInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegationInInterface(), KtAbstractFirDiagnostic<PsiElement>

internal class DelegationNotToInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegationNotToInterface(), KtAbstractFirDiagnostic<PsiElement>

internal class NestedClassNotAllowedImpl(
    override val declaration: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NestedClassNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class IncorrectCharacterLiteralImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IncorrectCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement>

internal class EmptyCharacterLiteralImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.EmptyCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement>

internal class TooManyCharactersInCharacterLiteralImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TooManyCharactersInCharacterLiteral(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalEscapeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalEscape(), KtAbstractFirDiagnostic<PsiElement>

internal class IntLiteralOutOfRangeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IntLiteralOutOfRange(), KtAbstractFirDiagnostic<PsiElement>

internal class FloatLiteralOutOfRangeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FloatLiteralOutOfRange(), KtAbstractFirDiagnostic<PsiElement>

internal class WrongLongSuffixImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongLongSuffix(), KtAbstractFirDiagnostic<KtElement>

internal class UnsignedLiteralWithoutDeclarationsOnClasspathImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnsignedLiteralWithoutDeclarationsOnClasspath(), KtAbstractFirDiagnostic<KtElement>

internal class DivisionByZeroImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DivisionByZero(), KtAbstractFirDiagnostic<KtExpression>

internal class ValOrVarOnLoopParameterImpl(
    override val valOrVar: KtKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValOrVarOnLoopParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class ValOrVarOnFunParameterImpl(
    override val valOrVar: KtKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValOrVarOnFunParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class ValOrVarOnCatchParameterImpl(
    override val valOrVar: KtKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValOrVarOnCatchParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class ValOrVarOnSecondaryConstructorParameterImpl(
    override val valOrVar: KtKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValOrVarOnSecondaryConstructorParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class InvisibleSetterImpl(
    override val property: KtVariableSymbol,
    override val visibility: Visibility,
    override val callableId: CallableId,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InvisibleSetter(), KtAbstractFirDiagnostic<PsiElement>

internal class InvisibleReferenceImpl(
    override val reference: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InvisibleReference(), KtAbstractFirDiagnostic<PsiElement>

internal class UnresolvedReferenceImpl(
    override val reference: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnresolvedReference(), KtAbstractFirDiagnostic<PsiElement>

internal class UnresolvedLabelImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnresolvedLabel(), KtAbstractFirDiagnostic<PsiElement>

internal class DeserializationErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeserializationError(), KtAbstractFirDiagnostic<PsiElement>

internal class ErrorFromJavaResolutionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ErrorFromJavaResolution(), KtAbstractFirDiagnostic<PsiElement>

internal class MissingStdlibClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MissingStdlibClass(), KtAbstractFirDiagnostic<PsiElement>

internal class NoThisImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoThis(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecationErrorImpl(
    override val reference: KtSymbol,
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecationError(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecationImpl(
    override val reference: KtSymbol,
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.Deprecation(), KtAbstractFirDiagnostic<PsiElement>

internal class ApiNotAvailableImpl(
    override val sinceKotlinVersion: ApiVersion,
    override val currentVersion: ApiVersion,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ApiNotAvailable(), KtAbstractFirDiagnostic<PsiElement>

internal class UnresolvedReferenceWrongReceiverImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnresolvedReferenceWrongReceiver(), KtAbstractFirDiagnostic<PsiElement>

internal class UnresolvedImportImpl(
    override val reference: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnresolvedImport(), KtAbstractFirDiagnostic<PsiElement>

internal class CreatingAnInstanceOfAbstractClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CreatingAnInstanceOfAbstractClass(), KtAbstractFirDiagnostic<KtExpression>

internal class FunctionCallExpectedImpl(
    override val functionName: String,
    override val hasValueParameters: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunctionCallExpected(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalSelectorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalSelector(), KtAbstractFirDiagnostic<PsiElement>

internal class NoReceiverAllowedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoReceiverAllowed(), KtAbstractFirDiagnostic<PsiElement>

internal class FunctionExpectedImpl(
    override val expression: String,
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunctionExpected(), KtAbstractFirDiagnostic<PsiElement>

internal class ResolutionToClassifierImpl(
    override val classSymbol: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ResolutionToClassifier(), KtAbstractFirDiagnostic<PsiElement>

internal class AmbiguousAlteredAssignImpl(
    override val altererNames: List<String?>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AmbiguousAlteredAssign(), KtAbstractFirDiagnostic<PsiElement>

internal class ForbiddenBinaryModImpl(
    override val forbiddenFunction: KtSymbol,
    override val suggestedFunction: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ForbiddenBinaryMod(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedBinaryModImpl(
    override val forbiddenFunction: KtSymbol,
    override val suggestedFunction: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedBinaryMod(), KtAbstractFirDiagnostic<PsiElement>

internal class SuperIsNotAnExpressionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SuperIsNotAnExpression(), KtAbstractFirDiagnostic<PsiElement>

internal class SuperNotAvailableImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SuperNotAvailable(), KtAbstractFirDiagnostic<PsiElement>

internal class AbstractSuperCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractSuperCall(), KtAbstractFirDiagnostic<PsiElement>

internal class AbstractSuperCallWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractSuperCallWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class InstanceAccessBeforeSuperCallImpl(
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InstanceAccessBeforeSuperCall(), KtAbstractFirDiagnostic<PsiElement>

internal class SuperCallWithDefaultParametersImpl(
    override val name: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SuperCallWithDefaultParameters(), KtAbstractFirDiagnostic<PsiElement>

internal class InterfaceCantCallDefaultMethodViaSuperImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InterfaceCantCallDefaultMethodViaSuper(), KtAbstractFirDiagnostic<PsiElement>

internal class NotASupertypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NotASupertype(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeArgumentsRedundantInSuperQualifierImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeArgumentsRedundantInSuperQualifier(), KtAbstractFirDiagnostic<KtElement>

internal class SuperclassNotAccessibleFromInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SuperclassNotAccessibleFromInterface(), KtAbstractFirDiagnostic<PsiElement>

internal class QualifiedSupertypeExtendedByOtherSupertypeImpl(
    override val otherSuperType: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.QualifiedSupertypeExtendedByOtherSupertype(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SupertypeInitializedInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SupertypeInitializedInInterface(), KtAbstractFirDiagnostic<KtTypeReference>

internal class InterfaceWithSuperclassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InterfaceWithSuperclass(), KtAbstractFirDiagnostic<KtTypeReference>

internal class FinalSupertypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FinalSupertype(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ClassCannotBeExtendedDirectlyImpl(
    override val classSymbol: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ClassCannotBeExtendedDirectly(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SupertypeIsExtensionFunctionTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SupertypeIsExtensionFunctionType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SingletonInSupertypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SingletonInSupertype(), KtAbstractFirDiagnostic<KtTypeReference>

internal class NullableSupertypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NullableSupertype(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ManyClassesInSupertypeListImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ManyClassesInSupertypeList(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SupertypeAppearsTwiceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SupertypeAppearsTwice(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ClassInSupertypeForEnumImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ClassInSupertypeForEnum(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SealedSupertypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SealedSupertype(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SealedSupertypeInLocalClassImpl(
    override val declarationType: String,
    override val sealedClassKind: ClassKind,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SealedSupertypeInLocalClass(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SealedInheritorInDifferentPackageImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SealedInheritorInDifferentPackage(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SealedInheritorInDifferentModuleImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SealedInheritorInDifferentModule(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ClassInheritsJavaSealedClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ClassInheritsJavaSealedClass(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SupertypeNotAClassOrInterfaceImpl(
    override val reason: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SupertypeNotAClassOrInterface(), KtAbstractFirDiagnostic<KtElement>

internal class CyclicInheritanceHierarchyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CyclicInheritanceHierarchy(), KtAbstractFirDiagnostic<PsiElement>

internal class ExpandedTypeCannotBeInheritedImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpandedTypeCannotBeInherited(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ProjectionInImmediateArgumentToSupertypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ProjectionInImmediateArgumentToSupertype(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class InconsistentTypeParameterValuesImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val type: KtClassLikeSymbol,
    override val bounds: List<KtType>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InconsistentTypeParameterValues(), KtAbstractFirDiagnostic<KtClass>

internal class InconsistentTypeParameterBoundsImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val type: KtClassLikeSymbol,
    override val bounds: List<KtType>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InconsistentTypeParameterBounds(), KtAbstractFirDiagnostic<PsiElement>

internal class AmbiguousSuperImpl(
    override val candidates: List<KtType>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AmbiguousSuper(), KtAbstractFirDiagnostic<KtSuperExpression>

internal class WrongMultipleInheritanceImpl(
    override val symbol: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongMultipleInheritance(), KtAbstractFirDiagnostic<KtElement>

internal class ConstructorInObjectImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConstructorInObject(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ConstructorInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConstructorInInterface(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NonPrivateConstructorInEnumImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonPrivateConstructorInEnum(), KtAbstractFirDiagnostic<PsiElement>

internal class NonPrivateOrProtectedConstructorInSealedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonPrivateOrProtectedConstructorInSealed(), KtAbstractFirDiagnostic<PsiElement>

internal class CyclicConstructorDelegationCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CyclicConstructorDelegationCall(), KtAbstractFirDiagnostic<PsiElement>

internal class PrimaryConstructorDelegationCallExpectedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PrimaryConstructorDelegationCallExpected(), KtAbstractFirDiagnostic<PsiElement>

internal class SupertypeNotInitializedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SupertypeNotInitialized(), KtAbstractFirDiagnostic<KtTypeReference>

internal class SupertypeInitializedWithoutPrimaryConstructorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SupertypeInitializedWithoutPrimaryConstructor(), KtAbstractFirDiagnostic<PsiElement>

internal class DelegationSuperCallInEnumConstructorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegationSuperCallInEnumConstructor(), KtAbstractFirDiagnostic<PsiElement>

internal class PrimaryConstructorRequiredForDataClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PrimaryConstructorRequiredForDataClass(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ExplicitDelegationCallRequiredImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExplicitDelegationCallRequired(), KtAbstractFirDiagnostic<PsiElement>

internal class SealedClassConstructorCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SealedClassConstructorCall(), KtAbstractFirDiagnostic<PsiElement>

internal class DataClassWithoutParametersImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DataClassWithoutParameters(), KtAbstractFirDiagnostic<KtPrimaryConstructor>

internal class DataClassVarargParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DataClassVarargParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class DataClassNotPropertyParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DataClassNotPropertyParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationArgumentKclassLiteralOfTypeParameterError(), KtAbstractFirDiagnostic<KtExpression>

internal class AnnotationArgumentMustBeConstImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeConst(), KtAbstractFirDiagnostic<KtExpression>

internal class AnnotationArgumentMustBeEnumConstImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeEnumConst(), KtAbstractFirDiagnostic<KtExpression>

internal class AnnotationArgumentMustBeKclassLiteralImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeKclassLiteral(), KtAbstractFirDiagnostic<KtExpression>

internal class AnnotationClassMemberImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationClassMember(), KtAbstractFirDiagnostic<PsiElement>

internal class AnnotationParameterDefaultValueMustBeConstantImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationParameterDefaultValueMustBeConstant(), KtAbstractFirDiagnostic<KtExpression>

internal class InvalidTypeOfAnnotationMemberImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InvalidTypeOfAnnotationMember(), KtAbstractFirDiagnostic<KtTypeReference>

internal class LocalAnnotationClassErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LocalAnnotationClassError(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class MissingValOnAnnotationParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MissingValOnAnnotationParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class NonConstValUsedInConstantExpressionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonConstValUsedInConstantExpression(), KtAbstractFirDiagnostic<KtExpression>

internal class CycleInAnnotationParameterErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CycleInAnnotationParameterError(), KtAbstractFirDiagnostic<KtParameter>

internal class CycleInAnnotationParameterWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CycleInAnnotationParameterWarning(), KtAbstractFirDiagnostic<KtParameter>

internal class AnnotationClassConstructorCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationClassConstructorCall(), KtAbstractFirDiagnostic<KtCallExpression>

internal class NotAnAnnotationClassImpl(
    override val annotationName: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NotAnAnnotationClass(), KtAbstractFirDiagnostic<PsiElement>

internal class NullableTypeOfAnnotationMemberImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NullableTypeOfAnnotationMember(), KtAbstractFirDiagnostic<KtTypeReference>

internal class VarAnnotationParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VarAnnotationParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class SupertypesForAnnotationClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SupertypesForAnnotationClass(), KtAbstractFirDiagnostic<KtClass>

internal class AnnotationUsedAsAnnotationArgumentImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationUsedAsAnnotationArgument(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class IllegalKotlinVersionStringValueImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalKotlinVersionStringValue(), KtAbstractFirDiagnostic<KtExpression>

internal class NewerVersionInSinceKotlinImpl(
    override val specifiedVersion: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NewerVersionInSinceKotlin(), KtAbstractFirDiagnostic<KtExpression>

internal class DeprecatedSinceKotlinWithUnorderedVersionsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinWithUnorderedVersions(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedSinceKotlinWithoutArgumentsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinWithoutArguments(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedSinceKotlinWithoutDeprecatedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinWithoutDeprecated(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedSinceKotlinWithDeprecatedLevelImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinWithDeprecatedLevel(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedSinceKotlinOutsideKotlinSubpackageImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedSinceKotlinOutsideKotlinSubpackage(), KtAbstractFirDiagnostic<PsiElement>

internal class OverrideDeprecationImpl(
    override val overridenSymbol: KtSymbol,
    override val deprecationInfo: DeprecationInfo,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverrideDeprecation(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class AnnotationOnSuperclassErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationOnSuperclassError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class AnnotationOnSuperclassWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationOnSuperclassWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RestrictedRetentionForExpressionAnnotationErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RestrictedRetentionForExpressionAnnotationError(), KtAbstractFirDiagnostic<PsiElement>

internal class RestrictedRetentionForExpressionAnnotationWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RestrictedRetentionForExpressionAnnotationWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class WrongAnnotationTargetImpl(
    override val actualTarget: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongAnnotationTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class WrongAnnotationTargetWithUseSiteTargetImpl(
    override val actualTarget: String,
    override val useSiteTarget: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongAnnotationTargetWithUseSiteTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class InapplicableTargetOnPropertyImpl(
    override val useSiteDescription: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableTargetOnProperty(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class InapplicableTargetPropertyImmutableImpl(
    override val useSiteDescription: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableTargetPropertyImmutable(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class InapplicableTargetPropertyHasNoDelegateImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableTargetPropertyHasNoDelegate(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class InapplicableTargetPropertyHasNoBackingFieldImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableTargetPropertyHasNoBackingField(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class InapplicableParamTargetImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableParamTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RedundantAnnotationTargetImpl(
    override val useSiteDescription: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantAnnotationTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class InapplicableFileTargetImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableFileTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatedAnnotationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatedAnnotation(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatedAnnotationWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatedAnnotationWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class NotAClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NotAClass(), KtAbstractFirDiagnostic<PsiElement>

internal class WrongExtensionFunctionTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongExtensionFunctionType(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class WrongExtensionFunctionTypeWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongExtensionFunctionTypeWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class AnnotationInWhereClauseErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationInWhereClauseError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class PluginAnnotationAmbiguityImpl(
    override val typeFromCompilerPhase: KtType,
    override val typeFromTypesPhase: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PluginAnnotationAmbiguity(), KtAbstractFirDiagnostic<PsiElement>

internal class WrongJsQualifierImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongJsQualifier(), KtAbstractFirDiagnostic<KtElement>

internal class JsModuleProhibitedOnVarImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JsModuleProhibitedOnVar(), KtAbstractFirDiagnostic<KtElement>

internal class JsModuleProhibitedOnNonNativeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JsModuleProhibitedOnNonNative(), KtAbstractFirDiagnostic<KtElement>

internal class NestedJsModuleProhibitedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NestedJsModuleProhibited(), KtAbstractFirDiagnostic<KtElement>

internal class RuntimeAnnotationNotSupportedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RuntimeAnnotationNotSupported(), KtAbstractFirDiagnostic<PsiElement>

internal class RuntimeAnnotationOnExternalDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RuntimeAnnotationOnExternalDeclaration(), KtAbstractFirDiagnostic<PsiElement>

internal class NativeAnnotationsAllowedOnlyOnMemberOrExtensionFunImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun(), KtAbstractFirDiagnostic<KtElement>

internal class NativeIndexerKeyShouldBeStringOrNumberImpl(
    override val kind: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NativeIndexerKeyShouldBeStringOrNumber(), KtAbstractFirDiagnostic<KtElement>

internal class NativeIndexerWrongParameterCountImpl(
    override val parametersCount: Int,
    override val kind: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NativeIndexerWrongParameterCount(), KtAbstractFirDiagnostic<KtElement>

internal class NativeIndexerCanNotHaveDefaultArgumentsImpl(
    override val kind: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NativeIndexerCanNotHaveDefaultArguments(), KtAbstractFirDiagnostic<KtElement>

internal class NativeGetterReturnTypeShouldBeNullableImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NativeGetterReturnTypeShouldBeNullable(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NativeSetterWrongReturnTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NativeSetterWrongReturnType(), KtAbstractFirDiagnostic<KtDeclaration>

internal class JsNameIsNotOnAllAccessorsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JsNameIsNotOnAllAccessors(), KtAbstractFirDiagnostic<KtElement>

internal class JsNameProhibitedForNamedNativeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JsNameProhibitedForNamedNative(), KtAbstractFirDiagnostic<KtElement>

internal class JsNameProhibitedForOverrideImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JsNameProhibitedForOverride(), KtAbstractFirDiagnostic<KtElement>

internal class JsNameOnPrimaryConstructorProhibitedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JsNameOnPrimaryConstructorProhibited(), KtAbstractFirDiagnostic<KtElement>

internal class JsNameOnAccessorAndPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JsNameOnAccessorAndProperty(), KtAbstractFirDiagnostic<KtElement>

internal class JsNameProhibitedForExtensionPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JsNameProhibitedForExtensionProperty(), KtAbstractFirDiagnostic<KtElement>

internal class OptInUsageImpl(
    override val optInMarkerFqName: FqName,
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInUsage(), KtAbstractFirDiagnostic<PsiElement>

internal class OptInUsageErrorImpl(
    override val optInMarkerFqName: FqName,
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInUsageError(), KtAbstractFirDiagnostic<PsiElement>

internal class OptInOverrideImpl(
    override val optInMarkerFqName: FqName,
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInOverride(), KtAbstractFirDiagnostic<PsiElement>

internal class OptInOverrideErrorImpl(
    override val optInMarkerFqName: FqName,
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInOverrideError(), KtAbstractFirDiagnostic<PsiElement>

internal class OptInIsNotEnabledImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInIsNotEnabled(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OptInCanOnlyBeUsedAsAnnotationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInCanOnlyBeUsedAsAnnotation(), KtAbstractFirDiagnostic<PsiElement>

internal class OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptInImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn(), KtAbstractFirDiagnostic<PsiElement>

internal class OptInWithoutArgumentsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInWithoutArguments(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OptInArgumentIsNotMarkerImpl(
    override val notMarkerFqName: FqName,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInArgumentIsNotMarker(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OptInMarkerWithWrongTargetImpl(
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInMarkerWithWrongTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OptInMarkerWithWrongRetentionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInMarkerWithWrongRetention(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OptInMarkerOnWrongTargetImpl(
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInMarkerOnWrongTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OptInMarkerOnOverrideImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInMarkerOnOverride(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OptInMarkerOnOverrideWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OptInMarkerOnOverrideWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class SubclassOptInInapplicableImpl(
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SubclassOptInInapplicable(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class ExposedTypealiasExpandedTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedTypealiasExpandedType(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ExposedFunctionReturnTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedFunctionReturnType(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ExposedReceiverTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedReceiverType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ExposedPropertyTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedPropertyType(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ExposedPropertyTypeInConstructorErrorImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedPropertyTypeInConstructorError(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ExposedPropertyTypeInConstructorWarningImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedPropertyTypeInConstructorWarning(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ExposedParameterTypeImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedParameterType(), KtAbstractFirDiagnostic<KtParameter>

internal class ExposedSuperInterfaceImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedSuperInterface(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ExposedSuperClassImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedSuperClass(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ExposedTypeParameterBoundImpl(
    override val elementVisibility: EffectiveVisibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: EffectiveVisibility,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExposedTypeParameterBound(), KtAbstractFirDiagnostic<KtTypeReference>

internal class InapplicableInfixModifierImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableInfixModifier(), KtAbstractFirDiagnostic<PsiElement>

internal class RepeatedModifierImpl(
    override val modifier: KtModifierKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatedModifier(), KtAbstractFirDiagnostic<PsiElement>

internal class RedundantModifierImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantModifier(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedModifierImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val actualModifier: KtModifierKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedModifier(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedModifierPairImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedModifierPair(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedModifierForTargetImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedModifierForTarget(), KtAbstractFirDiagnostic<PsiElement>

internal class RedundantModifierForTargetImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantModifierForTarget(), KtAbstractFirDiagnostic<PsiElement>

internal class IncompatibleModifiersImpl(
    override val modifier1: KtModifierKeywordToken,
    override val modifier2: KtModifierKeywordToken,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IncompatibleModifiers(), KtAbstractFirDiagnostic<PsiElement>

internal class RedundantOpenInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantOpenInInterface(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class WrongModifierTargetImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongModifierTarget(), KtAbstractFirDiagnostic<PsiElement>

internal class OperatorModifierRequiredImpl(
    override val functionSymbol: KtFunctionLikeSymbol,
    override val name: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OperatorModifierRequired(), KtAbstractFirDiagnostic<PsiElement>

internal class InfixModifierRequiredImpl(
    override val functionSymbol: KtFunctionLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InfixModifierRequired(), KtAbstractFirDiagnostic<PsiElement>

internal class WrongModifierContainingDeclarationImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongModifierContainingDeclaration(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedModifierContainingDeclarationImpl(
    override val modifier: KtModifierKeywordToken,
    override val target: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedModifierContainingDeclaration(), KtAbstractFirDiagnostic<PsiElement>

internal class InapplicableOperatorModifierImpl(
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableOperatorModifier(), KtAbstractFirDiagnostic<PsiElement>

internal class NoExplicitVisibilityInApiModeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoExplicitVisibilityInApiMode(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NoExplicitVisibilityInApiModeWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoExplicitVisibilityInApiModeWarning(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NoExplicitReturnTypeInApiModeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoExplicitReturnTypeInApiMode(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NoExplicitReturnTypeInApiModeWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoExplicitReturnTypeInApiModeWarning(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ValueClassNotTopLevelImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassNotTopLevel(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ValueClassNotFinalImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassNotFinal(), KtAbstractFirDiagnostic<KtDeclaration>

internal class AbsenceOfPrimaryConstructorForValueClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbsenceOfPrimaryConstructorForValueClass(), KtAbstractFirDiagnostic<KtDeclaration>

internal class InlineClassConstructorWrongParametersSizeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InlineClassConstructorWrongParametersSize(), KtAbstractFirDiagnostic<KtElement>

internal class ValueClassEmptyConstructorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassEmptyConstructor(), KtAbstractFirDiagnostic<KtElement>

internal class ValueClassConstructorNotFinalReadOnlyParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassConstructorNotFinalReadOnlyParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class PropertyWithBackingFieldInsideValueClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyWithBackingFieldInsideValueClass(), KtAbstractFirDiagnostic<KtProperty>

internal class DelegatedPropertyInsideValueClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegatedPropertyInsideValueClass(), KtAbstractFirDiagnostic<PsiElement>

internal class ValueClassHasInapplicableParameterTypeImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassHasInapplicableParameterType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ValueClassCannotImplementInterfaceByDelegationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassCannotImplementInterfaceByDelegation(), KtAbstractFirDiagnostic<PsiElement>

internal class ValueClassCannotExtendClassesImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassCannotExtendClasses(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ValueClassCannotBeRecursiveImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassCannotBeRecursive(), KtAbstractFirDiagnostic<KtTypeReference>

internal class MultiFieldValueClassPrimaryConstructorDefaultParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MultiFieldValueClassPrimaryConstructorDefaultParameter(), KtAbstractFirDiagnostic<KtExpression>

internal class SecondaryConstructorWithBodyInsideValueClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SecondaryConstructorWithBodyInsideValueClass(), KtAbstractFirDiagnostic<PsiElement>

internal class ReservedMemberInsideValueClassImpl(
    override val name: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReservedMemberInsideValueClass(), KtAbstractFirDiagnostic<KtFunction>

internal class TypeArgumentOnTypedValueClassEqualsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeArgumentOnTypedValueClassEquals(), KtAbstractFirDiagnostic<KtTypeReference>

internal class InnerClassInsideValueClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InnerClassInsideValueClass(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ValueClassCannotBeCloneableImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassCannotBeCloneable(), KtAbstractFirDiagnostic<KtDeclaration>

internal class AnnotationOnIllegalMultiFieldValueClassTypedTargetImpl(
    override val name: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnnotationOnIllegalMultiFieldValueClassTypedTarget(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class NoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoneApplicable(), KtAbstractFirDiagnostic<PsiElement>

internal class InapplicableCandidateImpl(
    override val candidate: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableCandidate(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val isMismatchDueToNullability: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeMismatch(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeInferenceOnlyInputTypesErrorImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeInferenceOnlyInputTypesError(), KtAbstractFirDiagnostic<PsiElement>

internal class ThrowableTypeMismatchImpl(
    override val actualType: KtType,
    override val isMismatchDueToNullability: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ThrowableTypeMismatch(), KtAbstractFirDiagnostic<PsiElement>

internal class ConditionTypeMismatchImpl(
    override val actualType: KtType,
    override val isMismatchDueToNullability: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConditionTypeMismatch(), KtAbstractFirDiagnostic<PsiElement>

internal class ArgumentTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val isMismatchDueToNullability: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ArgumentTypeMismatch(), KtAbstractFirDiagnostic<PsiElement>

internal class NullForNonnullTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NullForNonnullType(), KtAbstractFirDiagnostic<PsiElement>

internal class InapplicableLateinitModifierImpl(
    override val reason: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableLateinitModifier(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class VarargOutsideParenthesesImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VarargOutsideParentheses(), KtAbstractFirDiagnostic<KtElement>

internal class NamedArgumentsNotAllowedImpl(
    override val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NamedArgumentsNotAllowed(), KtAbstractFirDiagnostic<KtValueArgument>

internal class NonVarargSpreadImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonVarargSpread(), KtAbstractFirDiagnostic<LeafPsiElement>

internal class ArgumentPassedTwiceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ArgumentPassedTwice(), KtAbstractFirDiagnostic<KtValueArgument>

internal class TooManyArgumentsImpl(
    override val function: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TooManyArguments(), KtAbstractFirDiagnostic<PsiElement>

internal class NoValueForParameterImpl(
    override val violatedParameter: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoValueForParameter(), KtAbstractFirDiagnostic<KtElement>

internal class NamedParameterNotFoundImpl(
    override val name: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NamedParameterNotFound(), KtAbstractFirDiagnostic<KtValueArgument>

internal class NameForAmbiguousParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NameForAmbiguousParameter(), KtAbstractFirDiagnostic<KtValueArgument>

internal class AssignmentTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val isMismatchDueToNullability: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssignmentTypeMismatch(), KtAbstractFirDiagnostic<KtExpression>

internal class ResultTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ResultTypeMismatch(), KtAbstractFirDiagnostic<KtExpression>

internal class ManyLambdaExpressionArgumentsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ManyLambdaExpressionArguments(), KtAbstractFirDiagnostic<KtValueArgument>

internal class NewInferenceNoInformationForParameterImpl(
    override val name: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NewInferenceNoInformationForParameter(), KtAbstractFirDiagnostic<KtElement>

internal class SpreadOfNullableImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SpreadOfNullable(), KtAbstractFirDiagnostic<PsiElement>

internal class AssigningSingleElementToVarargInNamedFormFunctionErrorImpl(
    override val expectedArrayType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssigningSingleElementToVarargInNamedFormFunctionError(), KtAbstractFirDiagnostic<KtExpression>

internal class AssigningSingleElementToVarargInNamedFormFunctionWarningImpl(
    override val expectedArrayType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssigningSingleElementToVarargInNamedFormFunctionWarning(), KtAbstractFirDiagnostic<KtExpression>

internal class AssigningSingleElementToVarargInNamedFormAnnotationErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssigningSingleElementToVarargInNamedFormAnnotationError(), KtAbstractFirDiagnostic<KtExpression>

internal class AssigningSingleElementToVarargInNamedFormAnnotationWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssigningSingleElementToVarargInNamedFormAnnotationWarning(), KtAbstractFirDiagnostic<KtExpression>

internal class RedundantSpreadOperatorInNamedFormInAnnotationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantSpreadOperatorInNamedFormInAnnotation(), KtAbstractFirDiagnostic<KtExpression>

internal class RedundantSpreadOperatorInNamedFormInFunctionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantSpreadOperatorInNamedFormInFunction(), KtAbstractFirDiagnostic<KtExpression>

internal class InferenceUnsuccessfulForkImpl(
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InferenceUnsuccessfulFork(), KtAbstractFirDiagnostic<PsiElement>

internal class OverloadResolutionAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverloadResolutionAmbiguity(), KtAbstractFirDiagnostic<PsiElement>

internal class AssignOperatorAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssignOperatorAmbiguity(), KtAbstractFirDiagnostic<PsiElement>

internal class IteratorAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IteratorAmbiguity(), KtAbstractFirDiagnostic<PsiElement>

internal class HasNextFunctionAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.HasNextFunctionAmbiguity(), KtAbstractFirDiagnostic<PsiElement>

internal class NextAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NextAmbiguity(), KtAbstractFirDiagnostic<PsiElement>

internal class AmbiguousFunctionTypeKindImpl(
    override val kinds: List<FunctionTypeKind>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AmbiguousFunctionTypeKind(), KtAbstractFirDiagnostic<PsiElement>

internal class NoContextReceiverImpl(
    override val contextReceiverRepresentation: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoContextReceiver(), KtAbstractFirDiagnostic<KtElement>

internal class MultipleArgumentsApplicableForContextReceiverImpl(
    override val contextReceiverRepresentation: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MultipleArgumentsApplicableForContextReceiver(), KtAbstractFirDiagnostic<KtElement>

internal class AmbiguousCallWithImplicitContextReceiverImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AmbiguousCallWithImplicitContextReceiver(), KtAbstractFirDiagnostic<KtElement>

internal class UnsupportedContextualDeclarationCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnsupportedContextualDeclarationCall(), KtAbstractFirDiagnostic<KtElement>

internal class RecursionInImplicitTypesImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RecursionInImplicitTypes(), KtAbstractFirDiagnostic<PsiElement>

internal class InferenceErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InferenceError(), KtAbstractFirDiagnostic<PsiElement>

internal class ProjectionOnNonClassTypeArgumentImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ProjectionOnNonClassTypeArgument(), KtAbstractFirDiagnostic<PsiElement>

internal class UpperBoundViolatedImpl(
    override val expectedUpperBound: KtType,
    override val actualUpperBound: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UpperBoundViolated(), KtAbstractFirDiagnostic<PsiElement>

internal class UpperBoundViolatedInTypealiasExpansionImpl(
    override val expectedUpperBound: KtType,
    override val actualUpperBound: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UpperBoundViolatedInTypealiasExpansion(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeArgumentsNotAllowedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeArgumentsNotAllowed(), KtAbstractFirDiagnostic<PsiElement>

internal class WrongNumberOfTypeArgumentsImpl(
    override val expectedCount: Int,
    override val classifier: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongNumberOfTypeArguments(), KtAbstractFirDiagnostic<PsiElement>

internal class NoTypeArgumentsOnRhsImpl(
    override val expectedCount: Int,
    override val classifier: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoTypeArgumentsOnRhs(), KtAbstractFirDiagnostic<PsiElement>

internal class OuterClassArgumentsRequiredImpl(
    override val outer: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OuterClassArgumentsRequired(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeParametersInObjectImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParametersInObject(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeParametersInAnonymousObjectImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParametersInAnonymousObject(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalProjectionUsageImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalProjectionUsage(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeParametersInEnumImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParametersInEnum(), KtAbstractFirDiagnostic<PsiElement>

internal class ConflictingProjectionImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConflictingProjection(), KtAbstractFirDiagnostic<KtTypeProjection>

internal class ConflictingProjectionInTypealiasExpansionImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConflictingProjectionInTypealiasExpansion(), KtAbstractFirDiagnostic<KtElement>

internal class RedundantProjectionImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantProjection(), KtAbstractFirDiagnostic<KtTypeProjection>

internal class VarianceOnTypeParameterNotAllowedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VarianceOnTypeParameterNotAllowed(), KtAbstractFirDiagnostic<KtTypeParameter>

internal class CatchParameterWithDefaultValueImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CatchParameterWithDefaultValue(), KtAbstractFirDiagnostic<PsiElement>

internal class ReifiedTypeInCatchClauseImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReifiedTypeInCatchClause(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeParameterInCatchClauseImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParameterInCatchClause(), KtAbstractFirDiagnostic<PsiElement>

internal class GenericThrowableSubclassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.GenericThrowableSubclass(), KtAbstractFirDiagnostic<KtTypeParameter>

internal class InnerClassOfGenericThrowableSubclassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InnerClassOfGenericThrowableSubclass(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class KclassWithNullableTypeParameterInSignatureImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.KclassWithNullableTypeParameterInSignature(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class TypeParameterAsReifiedImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParameterAsReified(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeParameterAsReifiedArrayErrorImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParameterAsReifiedArrayError(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeParameterAsReifiedArrayWarningImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParameterAsReifiedArrayWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class ReifiedTypeForbiddenSubstitutionImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReifiedTypeForbiddenSubstitution(), KtAbstractFirDiagnostic<PsiElement>

internal class FinalUpperBoundImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FinalUpperBound(), KtAbstractFirDiagnostic<KtTypeReference>

internal class UpperBoundIsExtensionFunctionTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UpperBoundIsExtensionFunctionType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class BoundsNotAllowedIfBoundedByTypeParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.BoundsNotAllowedIfBoundedByTypeParameter(), KtAbstractFirDiagnostic<KtElement>

internal class OnlyOneClassBoundAllowedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OnlyOneClassBoundAllowed(), KtAbstractFirDiagnostic<KtTypeReference>

internal class RepeatedBoundImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatedBound(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ConflictingUpperBoundsImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConflictingUpperBounds(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class NameInConstraintIsNotATypeParameterImpl(
    override val typeParameterName: Name,
    override val typeParametersOwner: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NameInConstraintIsNotATypeParameter(), KtAbstractFirDiagnostic<KtSimpleNameExpression>

internal class BoundOnTypeAliasParameterNotAllowedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.BoundOnTypeAliasParameterNotAllowed(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ReifiedTypeParameterNoInlineImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReifiedTypeParameterNoInline(), KtAbstractFirDiagnostic<KtTypeParameter>

internal class TypeParametersNotAllowedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParametersNotAllowed(), KtAbstractFirDiagnostic<KtDeclaration>

internal class TypeParameterOfPropertyNotUsedInReceiverImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParameterOfPropertyNotUsedInReceiver(), KtAbstractFirDiagnostic<KtTypeParameter>

internal class ReturnTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val targetFunction: KtSymbol,
    override val isMismatchDueToNullability: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression>

internal class ImplicitNothingReturnTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ImplicitNothingReturnType(), KtAbstractFirDiagnostic<PsiElement>

internal class ImplicitNothingPropertyTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ImplicitNothingPropertyType(), KtAbstractFirDiagnostic<PsiElement>

internal class CyclicGenericUpperBoundImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CyclicGenericUpperBound(), KtAbstractFirDiagnostic<PsiElement>

internal class DeprecatedTypeParameterSyntaxImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedTypeParameterSyntax(), KtAbstractFirDiagnostic<KtDeclaration>

internal class MisplacedTypeParameterConstraintsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MisplacedTypeParameterConstraints(), KtAbstractFirDiagnostic<KtTypeParameter>

internal class DynamicSupertypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DynamicSupertype(), KtAbstractFirDiagnostic<KtTypeReference>

internal class DynamicUpperBoundImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DynamicUpperBound(), KtAbstractFirDiagnostic<KtTypeReference>

internal class DynamicReceiverNotAllowedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DynamicReceiverNotAllowed(), KtAbstractFirDiagnostic<KtElement>

internal class IncompatibleTypesImpl(
    override val typeA: KtType,
    override val typeB: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IncompatibleTypes(), KtAbstractFirDiagnostic<KtElement>

internal class IncompatibleTypesWarningImpl(
    override val typeA: KtType,
    override val typeB: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IncompatibleTypesWarning(), KtAbstractFirDiagnostic<KtElement>

internal class TypeVarianceConflictErrorImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val typeParameterVariance: Variance,
    override val variance: Variance,
    override val containingType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeVarianceConflictError(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeVarianceConflictInExpandedTypeImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val typeParameterVariance: Variance,
    override val variance: Variance,
    override val containingType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeVarianceConflictInExpandedType(), KtAbstractFirDiagnostic<PsiElement>

internal class SmartcastImpossibleImpl(
    override val desiredType: KtType,
    override val subject: KtExpression,
    override val description: String,
    override val isCastToNotNull: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SmartcastImpossible(), KtAbstractFirDiagnostic<KtExpression>

internal class RedundantNullableImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantNullable(), KtAbstractFirDiagnostic<KtTypeReference>

internal class PlatformClassMappedToKotlinImpl(
    override val kotlinClass: FqName,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PlatformClassMappedToKotlin(), KtAbstractFirDiagnostic<PsiElement>

internal class InferredTypeVariableIntoEmptyIntersectionImpl(
    override val typeVariableDescription: String,
    override val incompatibleTypes: List<KtType>,
    override val description: String,
    override val causingTypes: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InferredTypeVariableIntoEmptyIntersection(), KtAbstractFirDiagnostic<PsiElement>

internal class InferredTypeVariableIntoPossibleEmptyIntersectionImpl(
    override val typeVariableDescription: String,
    override val incompatibleTypes: List<KtType>,
    override val description: String,
    override val causingTypes: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InferredTypeVariableIntoPossibleEmptyIntersection(), KtAbstractFirDiagnostic<PsiElement>

internal class IncorrectLeftComponentOfIntersectionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IncorrectLeftComponentOfIntersection(), KtAbstractFirDiagnostic<KtTypeReference>

internal class IncorrectRightComponentOfIntersectionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IncorrectRightComponentOfIntersection(), KtAbstractFirDiagnostic<KtTypeReference>

internal class NullableOnDefinitelyNotNullableImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NullableOnDefinitelyNotNullable(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ExtensionInClassReferenceNotAllowedImpl(
    override val referencedDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExtensionInClassReferenceNotAllowed(), KtAbstractFirDiagnostic<KtExpression>

internal class CallableReferenceLhsNotAClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CallableReferenceLhsNotAClass(), KtAbstractFirDiagnostic<KtExpression>

internal class CallableReferenceToAnnotationConstructorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CallableReferenceToAnnotationConstructor(), KtAbstractFirDiagnostic<KtExpression>

internal class ClassLiteralLhsNotAClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ClassLiteralLhsNotAClass(), KtAbstractFirDiagnostic<KtExpression>

internal class NullableTypeInClassLiteralLhsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NullableTypeInClassLiteralLhs(), KtAbstractFirDiagnostic<KtExpression>

internal class ExpressionOfNullableTypeInClassLiteralLhsImpl(
    override val lhsType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpressionOfNullableTypeInClassLiteralLhs(), KtAbstractFirDiagnostic<PsiElement>

internal class NothingToOverrideImpl(
    override val declaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NothingToOverride(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class CannotOverrideInvisibleMemberImpl(
    override val overridingMember: KtCallableSymbol,
    override val baseMember: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CannotOverrideInvisibleMember(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class DataClassOverrideConflictImpl(
    override val overridingMember: KtCallableSymbol,
    override val baseMember: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DataClassOverrideConflict(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class CannotWeakenAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KtCallableSymbol,
    override val containingClassName: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CannotWeakenAccessPrivilege(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class CannotChangeAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KtCallableSymbol,
    override val containingClassName: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CannotChangeAccessPrivilege(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class OverridingFinalMemberImpl(
    override val overriddenDeclaration: KtCallableSymbol,
    override val containingClassName: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverridingFinalMember(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ReturnTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KtCallableSymbol,
    override val conflictingDeclaration2: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReturnTypeMismatchOnInheritance(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class PropertyTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KtCallableSymbol,
    override val conflictingDeclaration2: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyTypeMismatchOnInheritance(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class VarTypeMismatchOnInheritanceImpl(
    override val conflictingDeclaration1: KtCallableSymbol,
    override val conflictingDeclaration2: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VarTypeMismatchOnInheritance(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class ReturnTypeMismatchByDelegationImpl(
    override val delegateDeclaration: KtCallableSymbol,
    override val baseDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReturnTypeMismatchByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class PropertyTypeMismatchByDelegationImpl(
    override val delegateDeclaration: KtCallableSymbol,
    override val baseDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyTypeMismatchByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class VarOverriddenByValByDelegationImpl(
    override val delegateDeclaration: KtCallableSymbol,
    override val baseDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VarOverriddenByValByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class ConflictingInheritedMembersImpl(
    override val owner: KtClassLikeSymbol,
    override val conflictingDeclarations: List<KtCallableSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConflictingInheritedMembers(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class AbstractMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class AbstractClassMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractClassMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class InvisibleAbstractMemberFromSuperErrorImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val invisibleDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InvisibleAbstractMemberFromSuperError(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class InvisibleAbstractMemberFromSuperWarningImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val invisibleDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InvisibleAbstractMemberFromSuperWarning(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class AmbiguousAnonymousTypeInferredImpl(
    override val superTypes: List<KtType>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AmbiguousAnonymousTypeInferred(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ManyImplMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ManyImplMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class ManyInterfacesMemberNotImplementedImpl(
    override val classOrObject: KtClassLikeSymbol,
    override val missingDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ManyInterfacesMemberNotImplemented(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class OverridingFinalMemberByDelegationImpl(
    override val delegatedDeclaration: KtCallableSymbol,
    override val overriddenDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverridingFinalMemberByDelegation(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class DelegatedMemberHidesSupertypeOverrideImpl(
    override val delegatedDeclaration: KtCallableSymbol,
    override val overriddenDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegatedMemberHidesSupertypeOverride(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class ReturnTypeMismatchOnOverrideImpl(
    override val function: KtCallableSymbol,
    override val superFunction: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReturnTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class PropertyTypeMismatchOnOverrideImpl(
    override val property: KtCallableSymbol,
    override val superProperty: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class VarTypeMismatchOnOverrideImpl(
    override val variable: KtCallableSymbol,
    override val superVariable: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VarTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class VarOverriddenByValImpl(
    override val overridingDeclaration: KtCallableSymbol,
    override val overriddenDeclaration: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VarOverriddenByVal(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class NonFinalMemberInFinalClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonFinalMemberInFinalClass(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class NonFinalMemberInObjectImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonFinalMemberInObject(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class VirtualMemberHiddenImpl(
    override val declared: KtCallableSymbol,
    override val overriddenContainer: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VirtualMemberHidden(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ManyCompanionObjectsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ManyCompanionObjects(), KtAbstractFirDiagnostic<KtObjectDeclaration>

internal class ConflictingOverloadsImpl(
    override val conflictingOverloads: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConflictingOverloads(), KtAbstractFirDiagnostic<PsiElement>

internal class RedeclarationImpl(
    override val conflictingDeclarations: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.Redeclaration(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class PackageOrClassifierRedeclarationImpl(
    override val conflictingDeclarations: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PackageOrClassifierRedeclaration(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class MethodOfAnyImplementedInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MethodOfAnyImplementedInInterface(), KtAbstractFirDiagnostic<PsiElement>

internal class LocalObjectNotAllowedImpl(
    override val objectName: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LocalObjectNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class LocalInterfaceNotAllowedImpl(
    override val interfaceName: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LocalInterfaceNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class AbstractFunctionInNonAbstractClassImpl(
    override val function: KtCallableSymbol,
    override val containingClass: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractFunctionInNonAbstractClass(), KtAbstractFirDiagnostic<KtFunction>

internal class AbstractFunctionWithBodyImpl(
    override val function: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractFunctionWithBody(), KtAbstractFirDiagnostic<KtFunction>

internal class NonAbstractFunctionWithNoBodyImpl(
    override val function: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonAbstractFunctionWithNoBody(), KtAbstractFirDiagnostic<KtFunction>

internal class PrivateFunctionWithNoBodyImpl(
    override val function: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PrivateFunctionWithNoBody(), KtAbstractFirDiagnostic<KtFunction>

internal class NonMemberFunctionNoBodyImpl(
    override val function: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonMemberFunctionNoBody(), KtAbstractFirDiagnostic<KtFunction>

internal class FunctionDeclarationWithNoNameImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunctionDeclarationWithNoName(), KtAbstractFirDiagnostic<KtFunction>

internal class AnonymousFunctionWithNameImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnonymousFunctionWithName(), KtAbstractFirDiagnostic<KtFunction>

internal class AnonymousFunctionParameterWithDefaultValueImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnonymousFunctionParameterWithDefaultValue(), KtAbstractFirDiagnostic<KtParameter>

internal class UselessVarargOnParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UselessVarargOnParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class MultipleVarargParametersImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MultipleVarargParameters(), KtAbstractFirDiagnostic<KtParameter>

internal class ForbiddenVarargParameterTypeImpl(
    override val varargParameterType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ForbiddenVarargParameterType(), KtAbstractFirDiagnostic<KtParameter>

internal class ValueParameterWithNoTypeAnnotationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueParameterWithNoTypeAnnotation(), KtAbstractFirDiagnostic<KtParameter>

internal class CannotInferParameterTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CannotInferParameterType(), KtAbstractFirDiagnostic<KtElement>

internal class NoTailCallsFoundImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoTailCallsFound(), KtAbstractFirDiagnostic<KtNamedFunction>

internal class TailrecOnVirtualMemberErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TailrecOnVirtualMemberError(), KtAbstractFirDiagnostic<KtNamedFunction>

internal class NonTailRecursiveCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonTailRecursiveCall(), KtAbstractFirDiagnostic<PsiElement>

internal class TailRecursionInTryIsNotSupportedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TailRecursionInTryIsNotSupported(), KtAbstractFirDiagnostic<PsiElement>

internal class DataObjectCustomEqualsOrHashCodeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DataObjectCustomEqualsOrHashCode(), KtAbstractFirDiagnostic<KtNamedFunction>

internal class FunInterfaceConstructorReferenceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunInterfaceConstructorReference(), KtAbstractFirDiagnostic<KtExpression>

internal class FunInterfaceWrongCountOfAbstractMembersImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunInterfaceWrongCountOfAbstractMembers(), KtAbstractFirDiagnostic<KtClass>

internal class FunInterfaceCannotHaveAbstractPropertiesImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunInterfaceCannotHaveAbstractProperties(), KtAbstractFirDiagnostic<KtDeclaration>

internal class FunInterfaceAbstractMethodWithTypeParametersImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunInterfaceAbstractMethodWithTypeParameters(), KtAbstractFirDiagnostic<KtDeclaration>

internal class FunInterfaceAbstractMethodWithDefaultValueImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunInterfaceAbstractMethodWithDefaultValue(), KtAbstractFirDiagnostic<KtDeclaration>

internal class FunInterfaceWithSuspendFunctionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunInterfaceWithSuspendFunction(), KtAbstractFirDiagnostic<KtDeclaration>

internal class AbstractPropertyInNonAbstractClassImpl(
    override val property: KtCallableSymbol,
    override val containingClass: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractPropertyInNonAbstractClass(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class PrivatePropertyInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PrivatePropertyInInterface(), KtAbstractFirDiagnostic<KtProperty>

internal class AbstractPropertyWithInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractPropertyWithInitializer(), KtAbstractFirDiagnostic<KtExpression>

internal class PropertyInitializerInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyInitializerInInterface(), KtAbstractFirDiagnostic<KtExpression>

internal class PropertyWithNoTypeNoInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyWithNoTypeNoInitializer(), KtAbstractFirDiagnostic<KtProperty>

internal class MustBeInitializedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MustBeInitialized(), KtAbstractFirDiagnostic<KtProperty>

internal class MustBeInitializedOrBeAbstractImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.MustBeInitializedOrBeAbstract(), KtAbstractFirDiagnostic<KtProperty>

internal class ExtensionPropertyMustHaveAccessorsOrBeAbstractImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExtensionPropertyMustHaveAccessorsOrBeAbstract(), KtAbstractFirDiagnostic<KtProperty>

internal class UnnecessaryLateinitImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnnecessaryLateinit(), KtAbstractFirDiagnostic<KtProperty>

internal class BackingFieldInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.BackingFieldInInterface(), KtAbstractFirDiagnostic<KtProperty>

internal class ExtensionPropertyWithBackingFieldImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExtensionPropertyWithBackingField(), KtAbstractFirDiagnostic<KtExpression>

internal class PropertyInitializerNoBackingFieldImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyInitializerNoBackingField(), KtAbstractFirDiagnostic<KtExpression>

internal class AbstractDelegatedPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractDelegatedProperty(), KtAbstractFirDiagnostic<KtExpression>

internal class DelegatedPropertyInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegatedPropertyInInterface(), KtAbstractFirDiagnostic<KtExpression>

internal class AbstractPropertyWithGetterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractPropertyWithGetter(), KtAbstractFirDiagnostic<KtPropertyAccessor>

internal class AbstractPropertyWithSetterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractPropertyWithSetter(), KtAbstractFirDiagnostic<KtPropertyAccessor>

internal class PrivateSetterForAbstractPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PrivateSetterForAbstractProperty(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class PrivateSetterForOpenPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PrivateSetterForOpenProperty(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class ValWithSetterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValWithSetter(), KtAbstractFirDiagnostic<KtPropertyAccessor>

internal class ConstValNotTopLevelOrObjectImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConstValNotTopLevelOrObject(), KtAbstractFirDiagnostic<KtElement>

internal class ConstValWithGetterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConstValWithGetter(), KtAbstractFirDiagnostic<KtElement>

internal class ConstValWithDelegateImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConstValWithDelegate(), KtAbstractFirDiagnostic<KtExpression>

internal class TypeCantBeUsedForConstValImpl(
    override val constValType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeCantBeUsedForConstVal(), KtAbstractFirDiagnostic<KtProperty>

internal class ConstValWithoutInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConstValWithoutInitializer(), KtAbstractFirDiagnostic<KtProperty>

internal class ConstValWithNonConstInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConstValWithNonConstInitializer(), KtAbstractFirDiagnostic<KtExpression>

internal class WrongSetterParameterTypeImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongSetterParameterType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class DelegateUsesExtensionPropertyTypeParameterErrorImpl(
    override val usedTypeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegateUsesExtensionPropertyTypeParameterError(), KtAbstractFirDiagnostic<KtProperty>

internal class DelegateUsesExtensionPropertyTypeParameterWarningImpl(
    override val usedTypeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegateUsesExtensionPropertyTypeParameterWarning(), KtAbstractFirDiagnostic<KtProperty>

internal class InitializerTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val isMismatchDueToNullability: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InitializerTypeMismatch(), KtAbstractFirDiagnostic<KtProperty>

internal class GetterVisibilityDiffersFromPropertyVisibilityImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.GetterVisibilityDiffersFromPropertyVisibility(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class SetterVisibilityInconsistentWithPropertyVisibilityImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SetterVisibilityInconsistentWithPropertyVisibility(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class WrongSetterReturnTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongSetterReturnType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class WrongGetterReturnTypeImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongGetterReturnType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class AccessorForDelegatedPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AccessorForDelegatedProperty(), KtAbstractFirDiagnostic<KtPropertyAccessor>

internal class PropertyInitializerWithExplicitFieldDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyInitializerWithExplicitFieldDeclaration(), KtAbstractFirDiagnostic<KtExpression>

internal class PropertyFieldDeclarationMissingInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyFieldDeclarationMissingInitializer(), KtAbstractFirDiagnostic<KtBackingField>

internal class LateinitPropertyFieldDeclarationWithInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LateinitPropertyFieldDeclarationWithInitializer(), KtAbstractFirDiagnostic<KtBackingField>

internal class LateinitFieldInValPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LateinitFieldInValProperty(), KtAbstractFirDiagnostic<KtBackingField>

internal class LateinitNullableBackingFieldImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LateinitNullableBackingField(), KtAbstractFirDiagnostic<KtBackingField>

internal class BackingFieldForDelegatedPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.BackingFieldForDelegatedProperty(), KtAbstractFirDiagnostic<KtBackingField>

internal class PropertyMustHaveGetterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyMustHaveGetter(), KtAbstractFirDiagnostic<KtProperty>

internal class PropertyMustHaveSetterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyMustHaveSetter(), KtAbstractFirDiagnostic<KtProperty>

internal class ExplicitBackingFieldInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExplicitBackingFieldInInterface(), KtAbstractFirDiagnostic<KtBackingField>

internal class ExplicitBackingFieldInAbstractPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExplicitBackingFieldInAbstractProperty(), KtAbstractFirDiagnostic<KtBackingField>

internal class ExplicitBackingFieldInExtensionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExplicitBackingFieldInExtension(), KtAbstractFirDiagnostic<KtBackingField>

internal class RedundantExplicitBackingFieldImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantExplicitBackingField(), KtAbstractFirDiagnostic<KtBackingField>

internal class AbstractPropertyInPrimaryConstructorParametersImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AbstractPropertyInPrimaryConstructorParameters(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class LocalVariableWithTypeParametersWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LocalVariableWithTypeParametersWarning(), KtAbstractFirDiagnostic<KtProperty>

internal class LocalVariableWithTypeParametersImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LocalVariableWithTypeParameters(), KtAbstractFirDiagnostic<KtProperty>

internal class ExplicitTypeArgumentsInPropertyAccessImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExplicitTypeArgumentsInPropertyAccess(), KtAbstractFirDiagnostic<KtExpression>

internal class LateinitIntrinsicCallOnNonLiteralImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LateinitIntrinsicCallOnNonLiteral(), KtAbstractFirDiagnostic<PsiElement>

internal class LateinitIntrinsicCallOnNonLateinitImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LateinitIntrinsicCallOnNonLateinit(), KtAbstractFirDiagnostic<PsiElement>

internal class LateinitIntrinsicCallInInlineFunctionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LateinitIntrinsicCallInInlineFunction(), KtAbstractFirDiagnostic<PsiElement>

internal class LateinitIntrinsicCallOnNonAccessiblePropertyImpl(
    override val declaration: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LateinitIntrinsicCallOnNonAccessibleProperty(), KtAbstractFirDiagnostic<PsiElement>

internal class LocalExtensionPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LocalExtensionProperty(), KtAbstractFirDiagnostic<PsiElement>

internal class ExpectedDeclarationWithBodyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedDeclarationWithBody(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ExpectedClassConstructorDelegationCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedClassConstructorDelegationCall(), KtAbstractFirDiagnostic<KtConstructorDelegationCall>

internal class ExpectedClassConstructorPropertyParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedClassConstructorPropertyParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class ExpectedEnumConstructorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedEnumConstructor(), KtAbstractFirDiagnostic<KtConstructor<*>>

internal class ExpectedEnumEntryWithBodyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedEnumEntryWithBody(), KtAbstractFirDiagnostic<KtEnumEntry>

internal class ExpectedPropertyInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedPropertyInitializer(), KtAbstractFirDiagnostic<KtExpression>

internal class ExpectedDelegatedPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedDelegatedProperty(), KtAbstractFirDiagnostic<KtExpression>

internal class ExpectedLateinitPropertyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedLateinitProperty(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class SupertypeInitializedInExpectedClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SupertypeInitializedInExpectedClass(), KtAbstractFirDiagnostic<PsiElement>

internal class ExpectedPrivateDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedPrivateDeclaration(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class ImplementationByDelegationInExpectClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ImplementationByDelegationInExpectClass(), KtAbstractFirDiagnostic<KtDelegatedSuperTypeEntry>

internal class ActualTypeAliasNotToClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ActualTypeAliasNotToClass(), KtAbstractFirDiagnostic<KtTypeAlias>

internal class ActualTypeAliasToClassWithDeclarationSiteVarianceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ActualTypeAliasToClassWithDeclarationSiteVariance(), KtAbstractFirDiagnostic<KtTypeAlias>

internal class ActualTypeAliasWithUseSiteVarianceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ActualTypeAliasWithUseSiteVariance(), KtAbstractFirDiagnostic<KtTypeAlias>

internal class ActualTypeAliasWithComplexSubstitutionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ActualTypeAliasWithComplexSubstitution(), KtAbstractFirDiagnostic<KtTypeAlias>

internal class ActualFunctionWithDefaultArgumentsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ActualFunctionWithDefaultArguments(), KtAbstractFirDiagnostic<PsiElement>

internal class ActualAnnotationConflictingDefaultArgumentValueImpl(
    override val parameter: KtVariableLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ActualAnnotationConflictingDefaultArgumentValue(), KtAbstractFirDiagnostic<PsiElement>

internal class ExpectedFunctionSourceWithDefaultArgumentsNotFoundImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedFunctionSourceWithDefaultArgumentsNotFound(), KtAbstractFirDiagnostic<PsiElement>

internal class NoActualForExpectImpl(
    override val declaration: KtSymbol,
    override val module: FirModuleData,
    override val compatibility: Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoActualForExpect(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ActualWithoutExpectImpl(
    override val declaration: KtSymbol,
    override val compatibility: Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ActualWithoutExpect(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class AmbiguousActualsImpl(
    override val declaration: KtSymbol,
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AmbiguousActuals(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class AmbiguousExpectsImpl(
    override val declaration: KtSymbol,
    override val modules: List<FirModuleData>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AmbiguousExpects(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class NoActualClassMemberForExpectedClassImpl(
    override val declaration: KtSymbol,
    override val members: List<Pair<KtSymbol, Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>>>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoActualClassMemberForExpectedClass(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class ActualMissingImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ActualMissing(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class InitializerRequiredForDestructuringDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InitializerRequiredForDestructuringDeclaration(), KtAbstractFirDiagnostic<KtDestructuringDeclaration>

internal class ComponentFunctionMissingImpl(
    override val missingFunctionName: Name,
    override val destructingType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ComponentFunctionMissing(), KtAbstractFirDiagnostic<PsiElement>

internal class ComponentFunctionAmbiguityImpl(
    override val functionWithAmbiguityName: Name,
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ComponentFunctionAmbiguity(), KtAbstractFirDiagnostic<PsiElement>

internal class ComponentFunctionOnNullableImpl(
    override val componentFunctionName: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ComponentFunctionOnNullable(), KtAbstractFirDiagnostic<KtExpression>

internal class ComponentFunctionReturnTypeMismatchImpl(
    override val componentFunctionName: Name,
    override val destructingType: KtType,
    override val expectedType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ComponentFunctionReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression>

internal class UninitializedVariableImpl(
    override val variable: KtVariableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UninitializedVariable(), KtAbstractFirDiagnostic<KtExpression>

internal class UninitializedParameterImpl(
    override val parameter: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UninitializedParameter(), KtAbstractFirDiagnostic<KtSimpleNameExpression>

internal class UninitializedEnumEntryImpl(
    override val enumEntry: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UninitializedEnumEntry(), KtAbstractFirDiagnostic<KtExpression>

internal class UninitializedEnumCompanionImpl(
    override val enumClass: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UninitializedEnumCompanion(), KtAbstractFirDiagnostic<KtExpression>

internal class ValReassignmentImpl(
    override val variable: KtVariableLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValReassignment(), KtAbstractFirDiagnostic<KtExpression>

internal class ValReassignmentViaBackingFieldErrorImpl(
    override val property: KtVariableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValReassignmentViaBackingFieldError(), KtAbstractFirDiagnostic<KtExpression>

internal class ValReassignmentViaBackingFieldWarningImpl(
    override val property: KtVariableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValReassignmentViaBackingFieldWarning(), KtAbstractFirDiagnostic<KtExpression>

internal class CapturedValInitializationImpl(
    override val property: KtVariableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CapturedValInitialization(), KtAbstractFirDiagnostic<KtExpression>

internal class CapturedMemberValInitializationImpl(
    override val property: KtVariableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CapturedMemberValInitialization(), KtAbstractFirDiagnostic<KtExpression>

internal class SetterProjectedOutImpl(
    override val property: KtVariableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SetterProjectedOut(), KtAbstractFirDiagnostic<KtBinaryExpression>

internal class WrongInvocationKindImpl(
    override val declaration: KtSymbol,
    override val requiredRange: EventOccurrencesRange,
    override val actualRange: EventOccurrencesRange,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongInvocationKind(), KtAbstractFirDiagnostic<PsiElement>

internal class LeakedInPlaceLambdaImpl(
    override val lambda: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LeakedInPlaceLambda(), KtAbstractFirDiagnostic<PsiElement>

internal class WrongImpliesConditionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongImpliesCondition(), KtAbstractFirDiagnostic<PsiElement>

internal class VariableWithNoTypeNoInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VariableWithNoTypeNoInitializer(), KtAbstractFirDiagnostic<KtVariableDeclaration>

internal class InitializationBeforeDeclarationImpl(
    override val property: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InitializationBeforeDeclaration(), KtAbstractFirDiagnostic<KtExpression>

internal class UnreachableCodeImpl(
    override val reachable: List<PsiElement>,
    override val unreachable: List<PsiElement>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnreachableCode(), KtAbstractFirDiagnostic<KtElement>

internal class SenselessComparisonImpl(
    override val expression: KtExpression,
    override val compareResult: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SenselessComparison(), KtAbstractFirDiagnostic<KtExpression>

internal class SenselessNullInWhenImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SenselessNullInWhen(), KtAbstractFirDiagnostic<KtElement>

internal class TypecheckerHasRunIntoRecursiveProblemImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypecheckerHasRunIntoRecursiveProblem(), KtAbstractFirDiagnostic<KtExpression>

internal class UnsafeCallImpl(
    override val receiverType: KtType,
    override val receiverExpression: KtExpression?,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnsafeCall(), KtAbstractFirDiagnostic<PsiElement>

internal class UnsafeImplicitInvokeCallImpl(
    override val receiverType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnsafeImplicitInvokeCall(), KtAbstractFirDiagnostic<PsiElement>

internal class UnsafeInfixCallImpl(
    override val receiverExpression: KtExpression,
    override val operator: String,
    override val argumentExpression: KtExpression,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnsafeInfixCall(), KtAbstractFirDiagnostic<KtExpression>

internal class UnsafeOperatorCallImpl(
    override val receiverExpression: KtExpression,
    override val operator: String,
    override val argumentExpression: KtExpression,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnsafeOperatorCall(), KtAbstractFirDiagnostic<KtExpression>

internal class IteratorOnNullableImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IteratorOnNullable(), KtAbstractFirDiagnostic<KtExpression>

internal class UnnecessarySafeCallImpl(
    override val receiverType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnnecessarySafeCall(), KtAbstractFirDiagnostic<PsiElement>

internal class SafeCallWillChangeNullabilityImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SafeCallWillChangeNullability(), KtAbstractFirDiagnostic<KtSafeQualifiedExpression>

internal class UnexpectedSafeCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnexpectedSafeCall(), KtAbstractFirDiagnostic<PsiElement>

internal class UnnecessaryNotNullAssertionImpl(
    override val receiverType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnnecessaryNotNullAssertion(), KtAbstractFirDiagnostic<KtExpression>

internal class NotNullAssertionOnLambdaExpressionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NotNullAssertionOnLambdaExpression(), KtAbstractFirDiagnostic<KtExpression>

internal class NotNullAssertionOnCallableReferenceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NotNullAssertionOnCallableReference(), KtAbstractFirDiagnostic<KtExpression>

internal class UselessElvisImpl(
    override val receiverType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UselessElvis(), KtAbstractFirDiagnostic<KtBinaryExpression>

internal class UselessElvisRightIsNullImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UselessElvisRightIsNull(), KtAbstractFirDiagnostic<KtBinaryExpression>

internal class CannotCheckForErasedImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CannotCheckForErased(), KtAbstractFirDiagnostic<PsiElement>

internal class CastNeverSucceedsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CastNeverSucceeds(), KtAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>

internal class UselessCastImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UselessCast(), KtAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>

internal class UncheckedCastImpl(
    override val originalType: KtType,
    override val targetType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UncheckedCast(), KtAbstractFirDiagnostic<KtBinaryExpressionWithTypeRHS>

internal class UselessIsCheckImpl(
    override val compileTimeCheckResult: Boolean,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UselessIsCheck(), KtAbstractFirDiagnostic<KtElement>

internal class IsEnumEntryImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IsEnumEntry(), KtAbstractFirDiagnostic<KtTypeReference>

internal class EnumEntryAsTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.EnumEntryAsType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class ExpectedConditionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpectedCondition(), KtAbstractFirDiagnostic<KtWhenCondition>

internal class NoElseInWhenImpl(
    override val missingWhenCases: List<WhenMissingCase>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoElseInWhen(), KtAbstractFirDiagnostic<KtWhenExpression>

internal class NonExhaustiveWhenStatementImpl(
    override val type: String,
    override val missingWhenCases: List<WhenMissingCase>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonExhaustiveWhenStatement(), KtAbstractFirDiagnostic<KtWhenExpression>

internal class InvalidIfAsExpressionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InvalidIfAsExpression(), KtAbstractFirDiagnostic<KtIfExpression>

internal class ElseMisplacedInWhenImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ElseMisplacedInWhen(), KtAbstractFirDiagnostic<KtWhenEntry>

internal class IllegalDeclarationInWhenSubjectImpl(
    override val illegalReason: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalDeclarationInWhenSubject(), KtAbstractFirDiagnostic<KtElement>

internal class CommaInWhenConditionWithoutArgumentImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CommaInWhenConditionWithoutArgument(), KtAbstractFirDiagnostic<PsiElement>

internal class DuplicateLabelInWhenImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DuplicateLabelInWhen(), KtAbstractFirDiagnostic<KtElement>

internal class ConfusingBranchConditionErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConfusingBranchConditionError(), KtAbstractFirDiagnostic<PsiElement>

internal class ConfusingBranchConditionWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConfusingBranchConditionWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class TypeParameterIsNotAnExpressionImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParameterIsNotAnExpression(), KtAbstractFirDiagnostic<KtSimpleNameExpression>

internal class TypeParameterOnLhsOfDotImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypeParameterOnLhsOfDot(), KtAbstractFirDiagnostic<KtSimpleNameExpression>

internal class NoCompanionObjectImpl(
    override val klass: KtClassLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoCompanionObject(), KtAbstractFirDiagnostic<KtExpression>

internal class ExpressionExpectedPackageFoundImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExpressionExpectedPackageFound(), KtAbstractFirDiagnostic<KtExpression>

internal class ErrorInContractDescriptionImpl(
    override val reason: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ErrorInContractDescription(), KtAbstractFirDiagnostic<KtElement>

internal class NoGetMethodImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoGetMethod(), KtAbstractFirDiagnostic<KtArrayAccessExpression>

internal class NoSetMethodImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoSetMethod(), KtAbstractFirDiagnostic<KtArrayAccessExpression>

internal class IteratorMissingImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IteratorMissing(), KtAbstractFirDiagnostic<KtExpression>

internal class HasNextMissingImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.HasNextMissing(), KtAbstractFirDiagnostic<KtExpression>

internal class NextMissingImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NextMissing(), KtAbstractFirDiagnostic<KtExpression>

internal class HasNextFunctionNoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.HasNextFunctionNoneApplicable(), KtAbstractFirDiagnostic<KtExpression>

internal class NextNoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NextNoneApplicable(), KtAbstractFirDiagnostic<KtExpression>

internal class DelegateSpecialFunctionMissingImpl(
    override val expectedFunctionSignature: String,
    override val delegateType: KtType,
    override val description: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegateSpecialFunctionMissing(), KtAbstractFirDiagnostic<KtExpression>

internal class DelegateSpecialFunctionAmbiguityImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegateSpecialFunctionAmbiguity(), KtAbstractFirDiagnostic<KtExpression>

internal class DelegateSpecialFunctionNoneApplicableImpl(
    override val expectedFunctionSignature: String,
    override val candidates: List<KtSymbol>,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegateSpecialFunctionNoneApplicable(), KtAbstractFirDiagnostic<KtExpression>

internal class DelegateSpecialFunctionReturnTypeMismatchImpl(
    override val delegateFunction: String,
    override val expectedType: KtType,
    override val actualType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegateSpecialFunctionReturnTypeMismatch(), KtAbstractFirDiagnostic<KtExpression>

internal class UnderscoreIsReservedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnderscoreIsReserved(), KtAbstractFirDiagnostic<PsiElement>

internal class UnderscoreUsageWithoutBackticksImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnderscoreUsageWithoutBackticks(), KtAbstractFirDiagnostic<PsiElement>

internal class ResolvedToUnderscoreNamedCatchParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ResolvedToUnderscoreNamedCatchParameter(), KtAbstractFirDiagnostic<KtNameReferenceExpression>

internal class InvalidCharactersImpl(
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InvalidCharacters(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class DangerousCharactersImpl(
    override val characters: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DangerousCharacters(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class EqualityNotApplicableImpl(
    override val operator: String,
    override val leftType: KtType,
    override val rightType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.EqualityNotApplicable(), KtAbstractFirDiagnostic<KtBinaryExpression>

internal class EqualityNotApplicableWarningImpl(
    override val operator: String,
    override val leftType: KtType,
    override val rightType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.EqualityNotApplicableWarning(), KtAbstractFirDiagnostic<KtBinaryExpression>

internal class IncompatibleEnumComparisonErrorImpl(
    override val leftType: KtType,
    override val rightType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IncompatibleEnumComparisonError(), KtAbstractFirDiagnostic<KtElement>

internal class IncDecShouldNotReturnUnitImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IncDecShouldNotReturnUnit(), KtAbstractFirDiagnostic<KtExpression>

internal class AssignmentOperatorShouldReturnUnitImpl(
    override val functionSymbol: KtFunctionLikeSymbol,
    override val operator: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssignmentOperatorShouldReturnUnit(), KtAbstractFirDiagnostic<KtExpression>

internal class PropertyAsOperatorImpl(
    override val property: KtVariableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PropertyAsOperator(), KtAbstractFirDiagnostic<PsiElement>

internal class DslScopeViolationImpl(
    override val calleeSymbol: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DslScopeViolation(), KtAbstractFirDiagnostic<PsiElement>

internal class ToplevelTypealiasesOnlyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ToplevelTypealiasesOnly(), KtAbstractFirDiagnostic<KtTypeAlias>

internal class RecursiveTypealiasExpansionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RecursiveTypealiasExpansion(), KtAbstractFirDiagnostic<KtElement>

internal class TypealiasShouldExpandToClassImpl(
    override val expandedType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.TypealiasShouldExpandToClass(), KtAbstractFirDiagnostic<KtElement>

internal class RedundantVisibilityModifierImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantVisibilityModifier(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class RedundantModalityModifierImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantModalityModifier(), KtAbstractFirDiagnostic<KtModifierListOwner>

internal class RedundantReturnUnitTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantReturnUnitType(), KtAbstractFirDiagnostic<KtTypeReference>

internal class RedundantExplicitTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantExplicitType(), KtAbstractFirDiagnostic<PsiElement>

internal class RedundantSingleExpressionStringTemplateImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantSingleExpressionStringTemplate(), KtAbstractFirDiagnostic<PsiElement>

internal class CanBeValImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CanBeVal(), KtAbstractFirDiagnostic<KtDeclaration>

internal class CanBeReplacedWithOperatorAssignmentImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CanBeReplacedWithOperatorAssignment(), KtAbstractFirDiagnostic<KtExpression>

internal class RedundantCallOfConversionMethodImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantCallOfConversionMethod(), KtAbstractFirDiagnostic<PsiElement>

internal class ArrayEqualityOperatorCanBeReplacedWithEqualsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ArrayEqualityOperatorCanBeReplacedWithEquals(), KtAbstractFirDiagnostic<KtExpression>

internal class EmptyRangeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.EmptyRange(), KtAbstractFirDiagnostic<PsiElement>

internal class RedundantSetterParameterTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantSetterParameterType(), KtAbstractFirDiagnostic<PsiElement>

internal class UnusedVariableImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UnusedVariable(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class AssignedValueIsNeverReadImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AssignedValueIsNeverRead(), KtAbstractFirDiagnostic<PsiElement>

internal class VariableInitializerIsRedundantImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VariableInitializerIsRedundant(), KtAbstractFirDiagnostic<PsiElement>

internal class VariableNeverReadImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VariableNeverRead(), KtAbstractFirDiagnostic<KtNamedDeclaration>

internal class UselessCallOnNotNullImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UselessCallOnNotNull(), KtAbstractFirDiagnostic<PsiElement>

internal class ReturnNotAllowedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReturnNotAllowed(), KtAbstractFirDiagnostic<KtReturnExpression>

internal class NotAFunctionLabelImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NotAFunctionLabel(), KtAbstractFirDiagnostic<KtReturnExpression>

internal class ReturnInFunctionWithExpressionBodyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReturnInFunctionWithExpressionBody(), KtAbstractFirDiagnostic<KtReturnExpression>

internal class NoReturnInFunctionWithBlockBodyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NoReturnInFunctionWithBlockBody(), KtAbstractFirDiagnostic<KtDeclarationWithBody>

internal class AnonymousInitializerInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.AnonymousInitializerInInterface(), KtAbstractFirDiagnostic<KtAnonymousInitializer>

internal class UsageIsNotInlinableImpl(
    override val parameter: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UsageIsNotInlinable(), KtAbstractFirDiagnostic<KtElement>

internal class NonLocalReturnNotAllowedImpl(
    override val parameter: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonLocalReturnNotAllowed(), KtAbstractFirDiagnostic<KtElement>

internal class NotYetSupportedInInlineImpl(
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NotYetSupportedInInline(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NothingToInlineImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NothingToInline(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NullableInlineParameterImpl(
    override val parameter: KtSymbol,
    override val function: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NullableInlineParameter(), KtAbstractFirDiagnostic<KtDeclaration>

internal class RecursionInInlineImpl(
    override val symbol: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RecursionInInline(), KtAbstractFirDiagnostic<KtElement>

internal class NonPublicCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonPublicCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement>

internal class ProtectedConstructorCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ProtectedConstructorCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement>

internal class ProtectedCallFromPublicInlineErrorImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ProtectedCallFromPublicInlineError(), KtAbstractFirDiagnostic<KtElement>

internal class ProtectedCallFromPublicInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ProtectedCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement>

internal class PrivateClassMemberFromInlineImpl(
    override val inlineDeclaration: KtSymbol,
    override val referencedDeclaration: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PrivateClassMemberFromInline(), KtAbstractFirDiagnostic<KtElement>

internal class SuperCallFromPublicInlineImpl(
    override val symbol: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SuperCallFromPublicInline(), KtAbstractFirDiagnostic<KtElement>

internal class DeclarationCantBeInlinedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeclarationCantBeInlined(), KtAbstractFirDiagnostic<KtDeclaration>

internal class OverrideByInlineImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverrideByInline(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NonInternalPublishedApiImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonInternalPublishedApi(), KtAbstractFirDiagnostic<KtElement>

internal class InvalidDefaultFunctionalParameterForInlineImpl(
    override val defaultValue: KtExpression,
    override val parameter: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InvalidDefaultFunctionalParameterForInline(), KtAbstractFirDiagnostic<KtElement>

internal class ReifiedTypeParameterInOverrideImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReifiedTypeParameterInOverride(), KtAbstractFirDiagnostic<KtElement>

internal class InlinePropertyWithBackingFieldImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InlinePropertyWithBackingField(), KtAbstractFirDiagnostic<KtDeclaration>

internal class IllegalInlineParameterModifierImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalInlineParameterModifier(), KtAbstractFirDiagnostic<KtElement>

internal class InlineSuspendFunctionTypeUnsupportedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InlineSuspendFunctionTypeUnsupported(), KtAbstractFirDiagnostic<KtParameter>

internal class RedundantInlineSuspendFunctionTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantInlineSuspendFunctionType(), KtAbstractFirDiagnostic<KtElement>

internal class InefficientEqualsOverridingInValueClassImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InefficientEqualsOverridingInValueClass(), KtAbstractFirDiagnostic<KtNamedFunction>

internal class CannotAllUnderImportFromSingletonImpl(
    override val objectName: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CannotAllUnderImportFromSingleton(), KtAbstractFirDiagnostic<KtImportDirective>

internal class PackageCannotBeImportedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PackageCannotBeImported(), KtAbstractFirDiagnostic<KtImportDirective>

internal class CannotBeImportedImpl(
    override val name: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CannotBeImported(), KtAbstractFirDiagnostic<KtImportDirective>

internal class ConflictingImportImpl(
    override val name: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConflictingImport(), KtAbstractFirDiagnostic<KtImportDirective>

internal class OperatorRenamedOnImportImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OperatorRenamedOnImport(), KtAbstractFirDiagnostic<KtImportDirective>

internal class IllegalSuspendFunctionCallImpl(
    override val suspendCallable: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalSuspendFunctionCall(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalSuspendPropertyAccessImpl(
    override val suspendCallable: KtSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalSuspendPropertyAccess(), KtAbstractFirDiagnostic<PsiElement>

internal class NonLocalSuspensionPointImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonLocalSuspensionPoint(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalRestrictedSuspendingFunctionCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalRestrictedSuspendingFunctionCall(), KtAbstractFirDiagnostic<PsiElement>

internal class NonModifierFormForBuiltInSuspendImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonModifierFormForBuiltInSuspend(), KtAbstractFirDiagnostic<PsiElement>

internal class ModifierFormForNonBuiltInSuspendImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ModifierFormForNonBuiltInSuspend(), KtAbstractFirDiagnostic<PsiElement>

internal class ModifierFormForNonBuiltInSuspendFunErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ModifierFormForNonBuiltInSuspendFunError(), KtAbstractFirDiagnostic<PsiElement>

internal class ModifierFormForNonBuiltInSuspendFunWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ModifierFormForNonBuiltInSuspendFunWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class ReturnForBuiltInSuspendImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ReturnForBuiltInSuspend(), KtAbstractFirDiagnostic<KtReturnExpression>

internal class RedundantLabelWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantLabelWarning(), KtAbstractFirDiagnostic<KtLabelReferenceExpression>

internal class ConflictingJvmDeclarationsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConflictingJvmDeclarations(), KtAbstractFirDiagnostic<PsiElement>

internal class OverrideCannotBeStaticImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverrideCannotBeStatic(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmStaticNotInObjectOrClassCompanionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmStaticNotInObjectOrClassCompanion(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmStaticNotInObjectOrCompanionImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmStaticNotInObjectOrCompanion(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmStaticOnNonPublicMemberImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmStaticOnNonPublicMember(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmStaticOnConstOrJvmFieldImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmStaticOnConstOrJvmField(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmStaticOnExternalInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmStaticOnExternalInInterface(), KtAbstractFirDiagnostic<PsiElement>

internal class InapplicableJvmNameImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableJvmName(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalJvmNameImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalJvmName(), KtAbstractFirDiagnostic<PsiElement>

internal class FunctionDelegateMemberNameClashImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FunctionDelegateMemberNameClash(), KtAbstractFirDiagnostic<PsiElement>

internal class ValueClassWithoutJvmInlineAnnotationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ValueClassWithoutJvmInlineAnnotation(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmInlineWithoutValueClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmInlineWithoutValueClass(), KtAbstractFirDiagnostic<PsiElement>

internal class JavaTypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JavaTypeMismatch(), KtAbstractFirDiagnostic<KtExpression>

internal class UpperBoundCannotBeArrayImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.UpperBoundCannotBeArray(), KtAbstractFirDiagnostic<PsiElement>

internal class StrictfpOnClassImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.StrictfpOnClass(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class VolatileOnValueImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VolatileOnValue(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class VolatileOnDelegateImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.VolatileOnDelegate(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class SynchronizedOnAbstractImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SynchronizedOnAbstract(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class SynchronizedInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SynchronizedInInterface(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class SynchronizedOnInlineImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SynchronizedOnInline(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class SynchronizedOnSuspendErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SynchronizedOnSuspendError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class SynchronizedOnSuspendWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SynchronizedOnSuspendWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OverloadsWithoutDefaultArgumentsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverloadsWithoutDefaultArguments(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OverloadsAbstractImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverloadsAbstract(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OverloadsInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverloadsInterface(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OverloadsLocalImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverloadsLocal(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OverloadsAnnotationClassConstructorErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverloadsAnnotationClassConstructorError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OverloadsAnnotationClassConstructorWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverloadsAnnotationClassConstructorWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class OverloadsPrivateImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverloadsPrivate(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class DeprecatedJavaAnnotationImpl(
    override val kotlinName: FqName,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DeprecatedJavaAnnotation(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class JvmPackageNameCannotBeEmptyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmPackageNameCannotBeEmpty(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class JvmPackageNameMustBeValidNameImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmPackageNameMustBeValidName(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class JvmPackageNameNotSupportedInFilesWithClassesImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmPackageNameNotSupportedInFilesWithClasses(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class PositionedValueArgumentForJavaAnnotationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.PositionedValueArgumentForJavaAnnotation(), KtAbstractFirDiagnostic<KtExpression>

internal class RedundantRepeatableAnnotationImpl(
    override val kotlinRepeatable: FqName,
    override val javaRepeatable: FqName,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RedundantRepeatableAnnotation(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class LocalJvmRecordImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.LocalJvmRecord(), KtAbstractFirDiagnostic<PsiElement>

internal class NonFinalJvmRecordImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonFinalJvmRecord(), KtAbstractFirDiagnostic<PsiElement>

internal class EnumJvmRecordImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.EnumJvmRecord(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmRecordWithoutPrimaryConstructorParametersImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmRecordWithoutPrimaryConstructorParameters(), KtAbstractFirDiagnostic<PsiElement>

internal class NonDataClassJvmRecordImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonDataClassJvmRecord(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmRecordNotValParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmRecordNotValParameter(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmRecordNotLastVarargParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmRecordNotLastVarargParameter(), KtAbstractFirDiagnostic<PsiElement>

internal class InnerJvmRecordImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InnerJvmRecord(), KtAbstractFirDiagnostic<PsiElement>

internal class FieldInJvmRecordImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.FieldInJvmRecord(), KtAbstractFirDiagnostic<PsiElement>

internal class DelegationByInJvmRecordImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegationByInJvmRecord(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmRecordExtendsClassImpl(
    override val superType: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmRecordExtendsClass(), KtAbstractFirDiagnostic<PsiElement>

internal class IllegalJavaLangRecordSupertypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.IllegalJavaLangRecordSupertype(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmDefaultNotInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmDefaultNotInInterface(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmDefaultInJvm6TargetImpl(
    override val annotation: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmDefaultInJvm6Target(), KtAbstractFirDiagnostic<PsiElement>

internal class JvmDefaultRequiredForOverrideImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmDefaultRequiredForOverride(), KtAbstractFirDiagnostic<KtDeclaration>

internal class JvmDefaultInDeclarationImpl(
    override val annotation: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmDefaultInDeclaration(), KtAbstractFirDiagnostic<KtElement>

internal class JvmDefaultWithCompatibilityInDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmDefaultWithCompatibilityInDeclaration(), KtAbstractFirDiagnostic<KtElement>

internal class JvmDefaultWithCompatibilityNotOnInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmDefaultWithCompatibilityNotOnInterface(), KtAbstractFirDiagnostic<KtElement>

internal class NonJvmDefaultOverridesJavaDefaultImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonJvmDefaultOverridesJavaDefault(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ExternalDeclarationCannotBeAbstractImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalDeclarationCannotBeAbstract(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ExternalDeclarationCannotHaveBodyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalDeclarationCannotHaveBody(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ExternalDeclarationInInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalDeclarationInInterface(), KtAbstractFirDiagnostic<KtDeclaration>

internal class ExternalDeclarationCannotBeInlinedImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalDeclarationCannotBeInlined(), KtAbstractFirDiagnostic<KtDeclaration>

internal class NonSourceRepeatedAnnotationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonSourceRepeatedAnnotation(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatedAnnotationTarget6Impl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatedAnnotationTarget6(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatedAnnotationWithContainerImpl(
    override val name: ClassId,
    override val explicitContainerName: ClassId,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatedAnnotationWithContainer(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableContainerMustHaveValueArrayErrorImpl(
    override val container: ClassId,
    override val annotation: ClassId,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableContainerMustHaveValueArrayError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableContainerMustHaveValueArrayWarningImpl(
    override val container: ClassId,
    override val annotation: ClassId,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableContainerMustHaveValueArrayWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableContainerHasNonDefaultParameterErrorImpl(
    override val container: ClassId,
    override val nonDefault: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableContainerHasNonDefaultParameterError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableContainerHasNonDefaultParameterWarningImpl(
    override val container: ClassId,
    override val nonDefault: Name,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableContainerHasNonDefaultParameterWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableContainerHasShorterRetentionErrorImpl(
    override val container: ClassId,
    override val retention: String,
    override val annotation: ClassId,
    override val annotationRetention: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableContainerHasShorterRetentionError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableContainerHasShorterRetentionWarningImpl(
    override val container: ClassId,
    override val retention: String,
    override val annotation: ClassId,
    override val annotationRetention: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableContainerHasShorterRetentionWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableContainerTargetSetNotASubsetErrorImpl(
    override val container: ClassId,
    override val annotation: ClassId,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableContainerTargetSetNotASubsetError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableContainerTargetSetNotASubsetWarningImpl(
    override val container: ClassId,
    override val annotation: ClassId,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableContainerTargetSetNotASubsetWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableAnnotationHasNestedClassNamedContainerErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableAnnotationHasNestedClassNamedContainerError(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class RepeatableAnnotationHasNestedClassNamedContainerWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.RepeatableAnnotationHasNestedClassNamedContainerWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class SuspensionPointInsideCriticalSectionImpl(
    override val function: KtCallableSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SuspensionPointInsideCriticalSection(), KtAbstractFirDiagnostic<PsiElement>

internal class InapplicableJvmFieldImpl(
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableJvmField(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class InapplicableJvmFieldWarningImpl(
    override val message: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InapplicableJvmFieldWarning(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class JvmSyntheticOnDelegateImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JvmSyntheticOnDelegate(), KtAbstractFirDiagnostic<KtAnnotationEntry>

internal class DefaultMethodCallFromJava6TargetErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DefaultMethodCallFromJava6TargetError(), KtAbstractFirDiagnostic<PsiElement>

internal class DefaultMethodCallFromJava6TargetWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DefaultMethodCallFromJava6TargetWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class InterfaceStaticMethodCallFromJava6TargetErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InterfaceStaticMethodCallFromJava6TargetError(), KtAbstractFirDiagnostic<PsiElement>

internal class InterfaceStaticMethodCallFromJava6TargetWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InterfaceStaticMethodCallFromJava6TargetWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class SubclassCantCallCompanionProtectedNonStaticImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SubclassCantCallCompanionProtectedNonStatic(), KtAbstractFirDiagnostic<PsiElement>

internal class ConcurrentHashMapContainsOperatorErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConcurrentHashMapContainsOperatorError(), KtAbstractFirDiagnostic<PsiElement>

internal class ConcurrentHashMapContainsOperatorWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ConcurrentHashMapContainsOperatorWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class SpreadOnSignaturePolymorphicCallErrorImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SpreadOnSignaturePolymorphicCallError(), KtAbstractFirDiagnostic<PsiElement>

internal class SpreadOnSignaturePolymorphicCallWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.SpreadOnSignaturePolymorphicCallWarning(), KtAbstractFirDiagnostic<PsiElement>

internal class JavaSamInterfaceConstructorReferenceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JavaSamInterfaceConstructorReference(), KtAbstractFirDiagnostic<PsiElement>

internal class JavaShadowedProtectedFieldReferenceImpl(
    override val containerClass: ClassId,
    override val shadowingClass: ClassId,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.JavaShadowedProtectedFieldReference(), KtAbstractFirDiagnostic<PsiElement>

internal class ImplementingFunctionInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ImplementingFunctionInterface(), KtAbstractFirDiagnostic<KtClassOrObject>

internal class OverridingExternalFunWithOptionalParamsImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverridingExternalFunWithOptionalParams(), KtAbstractFirDiagnostic<KtElement>

internal class OverridingExternalFunWithOptionalParamsWithFakeImpl(
    override val function: KtFunctionLikeSymbol,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.OverridingExternalFunWithOptionalParamsWithFake(), KtAbstractFirDiagnostic<KtElement>

internal class CallToDefinedExternallyFromNonExternalDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.CallToDefinedExternallyFromNonExternalDeclaration(), KtAbstractFirDiagnostic<PsiElement>

internal class ExternalClassConstructorPropertyParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalClassConstructorPropertyParameter(), KtAbstractFirDiagnostic<KtParameter>

internal class ExternalEnumEntryWithBodyImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalEnumEntryWithBody(), KtAbstractFirDiagnostic<KtElement>

internal class ExternalAnonymousInitializerImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalAnonymousInitializer(), KtAbstractFirDiagnostic<KtAnonymousInitializer>

internal class ExternalDelegationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalDelegation(), KtAbstractFirDiagnostic<KtElement>

internal class ExternalDelegatedConstructorCallImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalDelegatedConstructorCall(), KtAbstractFirDiagnostic<KtElement>

internal class WrongBodyOfExternalDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongBodyOfExternalDeclaration(), KtAbstractFirDiagnostic<KtElement>

internal class WrongInitializerOfExternalDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongInitializerOfExternalDeclaration(), KtAbstractFirDiagnostic<KtElement>

internal class WrongDefaultValueForExternalFunParameterImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongDefaultValueForExternalFunParameter(), KtAbstractFirDiagnostic<KtElement>

internal class NestedExternalDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NestedExternalDeclaration(), KtAbstractFirDiagnostic<KtExpression>

internal class WrongExternalDeclarationImpl(
    override val classKind: String,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.WrongExternalDeclaration(), KtAbstractFirDiagnostic<KtExpression>

internal class NestedClassInExternalInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NestedClassInExternalInterface(), KtAbstractFirDiagnostic<KtExpression>

internal class ExternalTypeExtendsNonExternalTypeImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExternalTypeExtendsNonExternalType(), KtAbstractFirDiagnostic<KtElement>

internal class InlineExternalDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InlineExternalDeclaration(), KtAbstractFirDiagnostic<KtDeclaration>

internal class InlineClassInExternalDeclarationWarningImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InlineClassInExternalDeclarationWarning(), KtAbstractFirDiagnostic<KtElement>

internal class InlineClassInExternalDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.InlineClassInExternalDeclaration(), KtAbstractFirDiagnostic<KtElement>

internal class ExtensionFunctionInExternalDeclarationImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.ExtensionFunctionInExternalDeclaration(), KtAbstractFirDiagnostic<KtElement>

internal class NonAbstractMemberOfExternalInterfaceImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonAbstractMemberOfExternalInterface(), KtAbstractFirDiagnostic<KtExpression>

internal class NonExternalDeclarationInInappropriateFileImpl(
    override val type: KtType,
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.NonExternalDeclarationInInappropriateFile(), KtAbstractFirDiagnostic<KtElement>

internal class DelegationByDynamicImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.DelegationByDynamic(), KtAbstractFirDiagnostic<KtElement>

internal class SyntaxImpl(
    override val firDiagnostic: KtPsiDiagnostic,
    override val token: KtLifetimeToken,
) : KtFirDiagnostic.Syntax(), KtAbstractFirDiagnostic<PsiElement>

