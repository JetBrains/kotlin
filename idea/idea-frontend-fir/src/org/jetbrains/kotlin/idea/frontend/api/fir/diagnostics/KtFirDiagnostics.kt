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
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
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
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class KtFirDiagnostic<PSI: PsiElement> : KtDiagnosticWithPsi<PSI> {
    abstract class Unsupported : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Unsupported::class
        abstract val unsupported: String
    }

    abstract class UnsupportedFeature : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnsupportedFeature::class
        abstract val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>
    }

    abstract class Syntax : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Syntax::class
    }

    abstract class OtherError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OtherError::class
    }

    abstract class IllegalConstExpression : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalConstExpression::class
    }

    abstract class IllegalUnderscore : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalUnderscore::class
    }

    abstract class ExpressionRequired : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ExpressionRequired::class
    }

    abstract class BreakOrContinueOutsideALoop : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = BreakOrContinueOutsideALoop::class
    }

    abstract class NotALoopLabel : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NotALoopLabel::class
    }

    abstract class VariableExpected : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = VariableExpected::class
    }

    abstract class DelegationInInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegationInInterface::class
    }

    abstract class NestedClassNotAllowed : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = NestedClassNotAllowed::class
        abstract val declaration: String
    }

    abstract class IncorrectCharacterLiteral : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IncorrectCharacterLiteral::class
    }

    abstract class EmptyCharacterLiteral : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = EmptyCharacterLiteral::class
    }

    abstract class TooManyCharactersInCharacterLiteral : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TooManyCharactersInCharacterLiteral::class
    }

    abstract class IllegalEscape : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalEscape::class
    }

    abstract class Hidden : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Hidden::class
        abstract val hidden: KtSymbol
    }

    abstract class UnresolvedReference : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnresolvedReference::class
        abstract val reference: String
    }

    abstract class UnresolvedLabel : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnresolvedLabel::class
    }

    abstract class DeserializationError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeserializationError::class
    }

    abstract class ErrorFromJavaResolution : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ErrorFromJavaResolution::class
    }

    abstract class UnknownCallableKind : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnknownCallableKind::class
    }

    abstract class MissingStdlibClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = MissingStdlibClass::class
    }

    abstract class NoThis : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NoThis::class
    }

    abstract class SuperIsNotAnExpression : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SuperIsNotAnExpression::class
    }

    abstract class SuperNotAvailable : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SuperNotAvailable::class
    }

    abstract class AbstractSuperCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AbstractSuperCall::class
    }

    abstract class InstanceAccessBeforeSuperCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InstanceAccessBeforeSuperCall::class
        abstract val target: String
    }

    abstract class EnumAsSupertype : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = EnumAsSupertype::class
    }

    abstract class RecursionInSupertypes : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RecursionInSupertypes::class
    }

    abstract class NotASupertype : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NotASupertype::class
    }

    abstract class SuperclassNotAccessibleFromInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SuperclassNotAccessibleFromInterface::class
    }

    abstract class QualifiedSupertypeExtendedByOtherSupertype : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = QualifiedSupertypeExtendedByOtherSupertype::class
        abstract val otherSuperType: KtClassLikeSymbol
    }

    abstract class SupertypeInitializedInInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SupertypeInitializedInInterface::class
    }

    abstract class InterfaceWithSuperclass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InterfaceWithSuperclass::class
    }

    abstract class ClassInSupertypeForEnum : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ClassInSupertypeForEnum::class
    }

    abstract class SealedSupertype : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SealedSupertype::class
    }

    abstract class SealedSupertypeInLocalClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SealedSupertypeInLocalClass::class
    }

    abstract class SupertypeNotAClassOrInterface : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = SupertypeNotAClassOrInterface::class
        abstract val reason: String
    }

    abstract class ConstructorInObject : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ConstructorInObject::class
    }

    abstract class ConstructorInInterface : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ConstructorInInterface::class
    }

    abstract class NonPrivateConstructorInEnum : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonPrivateConstructorInEnum::class
    }

    abstract class NonPrivateOrProtectedConstructorInSealed : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonPrivateOrProtectedConstructorInSealed::class
    }

    abstract class CyclicConstructorDelegationCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = CyclicConstructorDelegationCall::class
    }

    abstract class PrimaryConstructorDelegationCallExpected : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = PrimaryConstructorDelegationCallExpected::class
    }

    abstract class SupertypeNotInitialized : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SupertypeNotInitialized::class
    }

    abstract class SupertypeInitializedWithoutPrimaryConstructor : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SupertypeInitializedWithoutPrimaryConstructor::class
    }

    abstract class DelegationSuperCallInEnumConstructor : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegationSuperCallInEnumConstructor::class
    }

    abstract class PrimaryConstructorRequiredForDataClass : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = PrimaryConstructorRequiredForDataClass::class
    }

    abstract class ExplicitDelegationCallRequired : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ExplicitDelegationCallRequired::class
    }

    abstract class SealedClassConstructorCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SealedClassConstructorCall::class
    }

    abstract class DataClassWithoutParameters : KtFirDiagnostic<KtPrimaryConstructor>() {
        override val diagnosticClass get() = DataClassWithoutParameters::class
    }

    abstract class DataClassVarargParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = DataClassVarargParameter::class
    }

    abstract class DataClassNotPropertyParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = DataClassNotPropertyParameter::class
    }

    abstract class AnnotationArgumentKclassLiteralOfTypeParameterError : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AnnotationArgumentKclassLiteralOfTypeParameterError::class
    }

    abstract class AnnotationArgumentMustBeConst : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AnnotationArgumentMustBeConst::class
    }

    abstract class AnnotationArgumentMustBeEnumConst : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AnnotationArgumentMustBeEnumConst::class
    }

    abstract class AnnotationArgumentMustBeKclassLiteral : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AnnotationArgumentMustBeKclassLiteral::class
    }

    abstract class AnnotationClassMember : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AnnotationClassMember::class
    }

    abstract class AnnotationParameterDefaultValueMustBeConstant : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AnnotationParameterDefaultValueMustBeConstant::class
    }

    abstract class InvalidTypeOfAnnotationMember : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = InvalidTypeOfAnnotationMember::class
    }

    abstract class LocalAnnotationClassError : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = LocalAnnotationClassError::class
    }

    abstract class MissingValOnAnnotationParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = MissingValOnAnnotationParameter::class
    }

    abstract class NonConstValUsedInConstantExpression : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NonConstValUsedInConstantExpression::class
    }

    abstract class NotAnAnnotationClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NotAnAnnotationClass::class
        abstract val annotationName: String
    }

    abstract class NullableTypeOfAnnotationMember : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = NullableTypeOfAnnotationMember::class
    }

    abstract class VarAnnotationParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = VarAnnotationParameter::class
    }

    abstract class SupertypesForAnnotationClass : KtFirDiagnostic<KtClass>() {
        override val diagnosticClass get() = SupertypesForAnnotationClass::class
    }

    abstract class ExposedTypealiasExpandedType : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedTypealiasExpandedType::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedFunctionReturnType : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedFunctionReturnType::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedReceiverType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExposedReceiverType::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedPropertyType : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedPropertyType::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedPropertyTypeInConstructor : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedPropertyTypeInConstructor::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedParameterType : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ExposedParameterType::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedSuperInterface : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExposedSuperInterface::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedSuperClass : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExposedSuperClass::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedTypeParameterBound : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExposedTypeParameterBound::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class InapplicableInfixModifier : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InapplicableInfixModifier::class
    }

    abstract class RepeatedModifier : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RepeatedModifier::class
        abstract val modifier: KtModifierKeywordToken
    }

    abstract class RedundantModifier : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RedundantModifier::class
        abstract val redundantModifier: KtModifierKeywordToken
        abstract val conflictingModifier: KtModifierKeywordToken
    }

    abstract class DeprecatedModifierPair : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedModifierPair::class
        abstract val deprecatedModifier: KtModifierKeywordToken
        abstract val conflictingModifier: KtModifierKeywordToken
    }

    abstract class IncompatibleModifiers : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IncompatibleModifiers::class
        abstract val modifier1: KtModifierKeywordToken
        abstract val modifier2: KtModifierKeywordToken
    }

    abstract class RedundantOpenInInterface : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = RedundantOpenInInterface::class
    }

    abstract class WrongModifierTarget : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = WrongModifierTarget::class
        abstract val modifier: KtModifierKeywordToken
        abstract val target: String
    }

    abstract class InlineClassNotTopLevel : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = InlineClassNotTopLevel::class
    }

    abstract class InlineClassNotFinal : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = InlineClassNotFinal::class
    }

    abstract class AbsenceOfPrimaryConstructorForInlineClass : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = AbsenceOfPrimaryConstructorForInlineClass::class
    }

    abstract class InlineClassConstructorWrongParametersSize : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = InlineClassConstructorWrongParametersSize::class
    }

    abstract class InlineClassConstructorNotFinalReadOnlyParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = InlineClassConstructorNotFinalReadOnlyParameter::class
    }

    abstract class PropertyWithBackingFieldInsideInlineClass : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = PropertyWithBackingFieldInsideInlineClass::class
    }

    abstract class DelegatedPropertyInsideInlineClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegatedPropertyInsideInlineClass::class
    }

    abstract class InlineClassHasInapplicableParameterType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = InlineClassHasInapplicableParameterType::class
        abstract val type: KtType
    }

    abstract class InlineClassCannotImplementInterfaceByDelegation : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InlineClassCannotImplementInterfaceByDelegation::class
    }

    abstract class InlineClassCannotExtendClasses : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = InlineClassCannotExtendClasses::class
    }

    abstract class InlineClassCannotBeRecursive : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = InlineClassCannotBeRecursive::class
    }

    abstract class ReservedMemberInsideInlineClass : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = ReservedMemberInsideInlineClass::class
        abstract val name: String
    }

    abstract class SecondaryConstructorWithBodyInsideInlineClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SecondaryConstructorWithBodyInsideInlineClass::class
    }

    abstract class InnerClassInsideInlineClass : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = InnerClassInsideInlineClass::class
    }

    abstract class ValueClassCannotBeCloneable : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ValueClassCannotBeCloneable::class
    }

    abstract class NoneApplicable : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NoneApplicable::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class InapplicableCandidate : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InapplicableCandidate::class
        abstract val candidate: KtSymbol
    }

    abstract class InapplicableLateinitModifier : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = InapplicableLateinitModifier::class
        abstract val reason: String
    }

    abstract class VarargOutsideParentheses : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = VarargOutsideParentheses::class
    }

    abstract class NamedArgumentsNotAllowed : KtFirDiagnostic<KtValueArgument>() {
        override val diagnosticClass get() = NamedArgumentsNotAllowed::class
        abstract val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget
    }

    abstract class NonVarargSpread : KtFirDiagnostic<LeafPsiElement>() {
        override val diagnosticClass get() = NonVarargSpread::class
    }

    abstract class ArgumentPassedTwice : KtFirDiagnostic<KtValueArgument>() {
        override val diagnosticClass get() = ArgumentPassedTwice::class
    }

    abstract class TooManyArguments : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TooManyArguments::class
        abstract val function: KtCallableSymbol
    }

    abstract class NoValueForParameter : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NoValueForParameter::class
        abstract val violatedParameter: KtSymbol
    }

    abstract class NamedParameterNotFound : KtFirDiagnostic<KtValueArgument>() {
        override val diagnosticClass get() = NamedParameterNotFound::class
        abstract val name: String
    }

    abstract class OverloadResolutionAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OverloadResolutionAmbiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class AssignOperatorAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AssignOperatorAmbiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class TypeMismatch : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
    }

    abstract class RecursionInImplicitTypes : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RecursionInImplicitTypes::class
    }

    abstract class InferenceError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InferenceError::class
    }

    abstract class ProjectionOnNonClassTypeArgument : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ProjectionOnNonClassTypeArgument::class
    }

    abstract class UpperBoundViolated : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UpperBoundViolated::class
        abstract val upperBound: KtType
    }

    abstract class TypeArgumentsNotAllowed : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeArgumentsNotAllowed::class
    }

    abstract class WrongNumberOfTypeArguments : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = WrongNumberOfTypeArguments::class
        abstract val expectedCount: Int
        abstract val classifier: KtClassLikeSymbol
    }

    abstract class NoTypeArgumentsOnRhs : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NoTypeArgumentsOnRhs::class
        abstract val expectedCount: Int
        abstract val classifier: KtClassLikeSymbol
    }

    abstract class TypeParametersInObject : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParametersInObject::class
    }

    abstract class IllegalProjectionUsage : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalProjectionUsage::class
    }

    abstract class TypeParametersInEnum : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParametersInEnum::class
    }

    abstract class ConflictingProjection : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConflictingProjection::class
        abstract val type: String
    }

    abstract class VarianceOnTypeParameterNotAllowed : KtFirDiagnostic<KtTypeParameter>() {
        override val diagnosticClass get() = VarianceOnTypeParameterNotAllowed::class
    }

    abstract class CatchParameterWithDefaultValue : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = CatchParameterWithDefaultValue::class
    }

    abstract class ReifiedTypeInCatchClause : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ReifiedTypeInCatchClause::class
    }

    abstract class TypeParameterInCatchClause : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParameterInCatchClause::class
    }

    abstract class GenericThrowableSubclass : KtFirDiagnostic<KtTypeParameterList>() {
        override val diagnosticClass get() = GenericThrowableSubclass::class
    }

    abstract class InnerClassOfGenericThrowableSubclass : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = InnerClassOfGenericThrowableSubclass::class
    }

    abstract class KclassWithNullableTypeParameterInSignature : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = KclassWithNullableTypeParameterInSignature::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class TypeParameterAsReified : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParameterAsReified::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class FinalUpperBound : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = FinalUpperBound::class
        abstract val type: KtType
    }

    abstract class UpperBoundIsExtensionFunctionType : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UpperBoundIsExtensionFunctionType::class
    }

    abstract class BoundsNotAllowedIfBoundedByTypeParameter : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = BoundsNotAllowedIfBoundedByTypeParameter::class
    }

    abstract class OnlyOneClassBoundAllowed : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OnlyOneClassBoundAllowed::class
    }

    abstract class ExtensionInClassReferenceNotAllowed : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ExtensionInClassReferenceNotAllowed::class
        abstract val referencedDeclaration: KtCallableSymbol
    }

    abstract class CallableReferenceLhsNotAClass : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = CallableReferenceLhsNotAClass::class
    }

    abstract class CallableReferenceToAnnotationConstructor : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = CallableReferenceToAnnotationConstructor::class
    }

    abstract class ClassLiteralLhsNotAClass : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ClassLiteralLhsNotAClass::class
    }

    abstract class NullableTypeInClassLiteralLhs : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NullableTypeInClassLiteralLhs::class
    }

    abstract class ExpressionOfNullableTypeInClassLiteralLhs : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ExpressionOfNullableTypeInClassLiteralLhs::class
        abstract val lhsType: KtType
    }

    abstract class NothingToOverride : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = NothingToOverride::class
        abstract val declaration: KtSymbol
    }

    abstract class CannotWeakenAccessPrivilege : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = CannotWeakenAccessPrivilege::class
        abstract val overridingVisibility: Visibility
        abstract val overridden: KtCallableSymbol
        abstract val containingClassName: Name
    }

    abstract class CannotChangeAccessPrivilege : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = CannotChangeAccessPrivilege::class
        abstract val overridingVisibility: Visibility
        abstract val overridden: KtCallableSymbol
        abstract val containingClassName: Name
    }

    abstract class OverridingFinalMember : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = OverridingFinalMember::class
        abstract val overriddenDeclaration: KtCallableSymbol
        abstract val containingClassName: Name
    }

    abstract class AbstractMemberNotImplemented : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = AbstractMemberNotImplemented::class
        abstract val classOrObject: KtClassLikeSymbol
        abstract val missingDeclaration: KtCallableSymbol
    }

    abstract class AbstractClassMemberNotImplemented : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = AbstractClassMemberNotImplemented::class
        abstract val classOrObject: KtClassLikeSymbol
        abstract val missingDeclaration: KtCallableSymbol
    }

    abstract class InvisibleAbstractMemberFromSuper : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = InvisibleAbstractMemberFromSuper::class
        abstract val classOrObject: KtClassLikeSymbol
        abstract val invisibleDeclaration: KtCallableSymbol
    }

    abstract class InvisibleAbstractMemberFromSuperWarning : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = InvisibleAbstractMemberFromSuperWarning::class
        abstract val classOrObject: KtClassLikeSymbol
        abstract val invisibleDeclaration: KtCallableSymbol
    }

    abstract class ManyImplMemberNotImplemented : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = ManyImplMemberNotImplemented::class
        abstract val classOrObject: KtClassLikeSymbol
        abstract val missingDeclaration: KtCallableSymbol
    }

    abstract class ManyInterfacesMemberNotImplemented : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = ManyInterfacesMemberNotImplemented::class
        abstract val classOrObject: KtClassLikeSymbol
        abstract val missingDeclaration: KtCallableSymbol
    }

    abstract class OverridingFinalMemberByDelegation : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = OverridingFinalMemberByDelegation::class
        abstract val delegatedDeclaration: KtCallableSymbol
        abstract val overriddenDeclaration: KtCallableSymbol
    }

    abstract class DelegatedMemberHidesSupertypeOverride : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = DelegatedMemberHidesSupertypeOverride::class
        abstract val delegatedDeclaration: KtCallableSymbol
        abstract val overriddenDeclaration: KtCallableSymbol
    }

    abstract class ReturnTypeMismatchOnOverride : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ReturnTypeMismatchOnOverride::class
        abstract val function: KtSymbol
        abstract val superFunction: KtSymbol
    }

    abstract class PropertyTypeMismatchOnOverride : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = PropertyTypeMismatchOnOverride::class
        abstract val property: KtSymbol
        abstract val superProperty: KtSymbol
    }

    abstract class VarTypeMismatchOnOverride : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = VarTypeMismatchOnOverride::class
        abstract val variable: KtSymbol
        abstract val superVariable: KtSymbol
    }

    abstract class VarOverriddenByVal : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = VarOverriddenByVal::class
        abstract val overridingDeclaration: KtSymbol
        abstract val overriddenDeclaration: KtSymbol
    }

    abstract class NonFinalMemberInFinalClass : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = NonFinalMemberInFinalClass::class
    }

    abstract class NonFinalMemberInObject : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = NonFinalMemberInObject::class
    }

    abstract class ManyCompanionObjects : KtFirDiagnostic<KtObjectDeclaration>() {
        override val diagnosticClass get() = ManyCompanionObjects::class
    }

    abstract class ConflictingOverloads : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConflictingOverloads::class
        abstract val conflictingOverloads: List<KtSymbol>
    }

    abstract class Redeclaration : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Redeclaration::class
        abstract val conflictingDeclarations: List<KtSymbol>
    }

    abstract class MethodOfAnyImplementedInInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = MethodOfAnyImplementedInInterface::class
    }

    abstract class LocalObjectNotAllowed : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = LocalObjectNotAllowed::class
        abstract val objectName: Name
    }

    abstract class LocalInterfaceNotAllowed : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = LocalInterfaceNotAllowed::class
        abstract val interfaceName: Name
    }

    abstract class AbstractFunctionInNonAbstractClass : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = AbstractFunctionInNonAbstractClass::class
        abstract val function: KtSymbol
        abstract val containingClass: KtSymbol
    }

    abstract class AbstractFunctionWithBody : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = AbstractFunctionWithBody::class
        abstract val function: KtSymbol
    }

    abstract class NonAbstractFunctionWithNoBody : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = NonAbstractFunctionWithNoBody::class
        abstract val function: KtSymbol
    }

    abstract class PrivateFunctionWithNoBody : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = PrivateFunctionWithNoBody::class
        abstract val function: KtSymbol
    }

    abstract class NonMemberFunctionNoBody : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = NonMemberFunctionNoBody::class
        abstract val function: KtSymbol
    }

    abstract class FunctionDeclarationWithNoName : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = FunctionDeclarationWithNoName::class
    }

    abstract class AnonymousFunctionParameterWithDefaultValue : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = AnonymousFunctionParameterWithDefaultValue::class
    }

    abstract class UselessVarargOnParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = UselessVarargOnParameter::class
    }

    abstract class MultipleVarargParameters : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = MultipleVarargParameters::class
    }

    abstract class ForbiddenVarargParameterType : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ForbiddenVarargParameterType::class
        abstract val varargParameterType: KtType
    }

    abstract class ValueParameterWithNoTypeAnnotation : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ValueParameterWithNoTypeAnnotation::class
    }

    abstract class AbstractPropertyInNonAbstractClass : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = AbstractPropertyInNonAbstractClass::class
        abstract val property: KtSymbol
        abstract val containingClass: KtSymbol
    }

    abstract class PrivatePropertyInInterface : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = PrivatePropertyInInterface::class
    }

    abstract class AbstractPropertyWithInitializer : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AbstractPropertyWithInitializer::class
    }

    abstract class PropertyInitializerInInterface : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = PropertyInitializerInInterface::class
    }

    abstract class PropertyWithNoTypeNoInitializer : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = PropertyWithNoTypeNoInitializer::class
    }

    abstract class MustBeInitialized : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = MustBeInitialized::class
    }

    abstract class MustBeInitializedOrBeAbstract : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = MustBeInitializedOrBeAbstract::class
    }

    abstract class ExtensionPropertyMustHaveAccessorsOrBeAbstract : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = ExtensionPropertyMustHaveAccessorsOrBeAbstract::class
    }

    abstract class UnnecessaryLateinit : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = UnnecessaryLateinit::class
    }

    abstract class BackingFieldInInterface : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = BackingFieldInInterface::class
    }

    abstract class ExtensionPropertyWithBackingField : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ExtensionPropertyWithBackingField::class
    }

    abstract class PropertyInitializerNoBackingField : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = PropertyInitializerNoBackingField::class
    }

    abstract class AbstractDelegatedProperty : KtFirDiagnostic<KtPropertyDelegate>() {
        override val diagnosticClass get() = AbstractDelegatedProperty::class
    }

    abstract class DelegatedPropertyInInterface : KtFirDiagnostic<KtPropertyDelegate>() {
        override val diagnosticClass get() = DelegatedPropertyInInterface::class
    }

    abstract class AbstractPropertyWithGetter : KtFirDiagnostic<KtPropertyAccessor>() {
        override val diagnosticClass get() = AbstractPropertyWithGetter::class
    }

    abstract class AbstractPropertyWithSetter : KtFirDiagnostic<KtPropertyAccessor>() {
        override val diagnosticClass get() = AbstractPropertyWithSetter::class
    }

    abstract class PrivateSetterForAbstractProperty : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = PrivateSetterForAbstractProperty::class
    }

    abstract class PrivateSetterForOpenProperty : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = PrivateSetterForOpenProperty::class
    }

    abstract class ExpectedPrivateDeclaration : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = ExpectedPrivateDeclaration::class
    }

    abstract class ValWithSetter : KtFirDiagnostic<KtPropertyAccessor>() {
        override val diagnosticClass get() = ValWithSetter::class
    }

    abstract class ConstValNotTopLevelOrObject : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = ConstValNotTopLevelOrObject::class
    }

    abstract class ConstValWithGetter : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = ConstValWithGetter::class
    }

    abstract class ConstValWithDelegate : KtFirDiagnostic<KtPropertyDelegate>() {
        override val diagnosticClass get() = ConstValWithDelegate::class
    }

    abstract class WrongSetterParameterType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = WrongSetterParameterType::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
    }

    abstract class ExpectedDeclarationWithBody : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ExpectedDeclarationWithBody::class
    }

    abstract class ExpectedPropertyInitializer : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ExpectedPropertyInitializer::class
    }

    abstract class ExpectedDelegatedProperty : KtFirDiagnostic<KtPropertyDelegate>() {
        override val diagnosticClass get() = ExpectedDelegatedProperty::class
    }

    abstract class ExpectedLateinitProperty : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = ExpectedLateinitProperty::class
    }

    abstract class InitializerRequiredForDestructuringDeclaration : KtFirDiagnostic<KtDestructuringDeclaration>() {
        override val diagnosticClass get() = InitializerRequiredForDestructuringDeclaration::class
    }

    abstract class ComponentFunctionMissing : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ComponentFunctionMissing::class
        abstract val missingFunctionName: Name
        abstract val destructingType: KtType
    }

    abstract class ComponentFunctionAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ComponentFunctionAmbiguity::class
        abstract val functionWithAmbiguityName: Name
        abstract val candidates: List<KtSymbol>
    }

    abstract class ComponentFunctionOnNullable : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ComponentFunctionOnNullable::class
        abstract val componentFunctionName: Name
    }

    abstract class ComponentFunctionReturnTypeMismatch : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ComponentFunctionReturnTypeMismatch::class
        abstract val componentFunctionName: Name
        abstract val destructingType: KtType
        abstract val expectedType: KtType
    }

    abstract class UninitializedVariable : KtFirDiagnostic<KtSimpleNameExpression>() {
        override val diagnosticClass get() = UninitializedVariable::class
        abstract val variable: KtVariableSymbol
    }

    abstract class ValReassignment : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ValReassignment::class
        abstract val variable: KtVariableLikeSymbol
    }

    abstract class ValReassignmentViaBackingField : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ValReassignmentViaBackingField::class
        abstract val property: KtVariableSymbol
    }

    abstract class ValReassignmentViaBackingFieldError : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ValReassignmentViaBackingFieldError::class
        abstract val property: KtVariableSymbol
    }

    abstract class WrongInvocationKind : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = WrongInvocationKind::class
        abstract val declaration: KtSymbol
        abstract val requiredRange: EventOccurrencesRange
        abstract val actualRange: EventOccurrencesRange
    }

    abstract class LeakedInPlaceLambda : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = LeakedInPlaceLambda::class
        abstract val lambda: KtSymbol
    }

    abstract class WrongImpliesCondition : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = WrongImpliesCondition::class
    }

    abstract class UnsafeCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnsafeCall::class
        abstract val receiverType: KtType
    }

    abstract class UnsafeImplicitInvokeCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnsafeImplicitInvokeCall::class
        abstract val receiverType: KtType
    }

    abstract class UnsafeInfixCall : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = UnsafeInfixCall::class
        abstract val lhs: KtExpression
        abstract val operator: String
        abstract val rhs: KtExpression
    }

    abstract class UnsafeOperatorCall : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = UnsafeOperatorCall::class
        abstract val lhs: KtExpression
        abstract val operator: String
        abstract val rhs: KtExpression
    }

    abstract class NoElseInWhen : KtFirDiagnostic<KtWhenExpression>() {
        override val diagnosticClass get() = NoElseInWhen::class
        abstract val missingWhenCases: List<WhenMissingCase>
    }

    abstract class InvalidIfAsExpression : KtFirDiagnostic<KtIfExpression>() {
        override val diagnosticClass get() = InvalidIfAsExpression::class
    }

    abstract class TypeParameterIsNotAnExpression : KtFirDiagnostic<KtSimpleNameExpression>() {
        override val diagnosticClass get() = TypeParameterIsNotAnExpression::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class TypeParameterOnLhsOfDot : KtFirDiagnostic<KtSimpleNameExpression>() {
        override val diagnosticClass get() = TypeParameterOnLhsOfDot::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class ErrorInContractDescription : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ErrorInContractDescription::class
        abstract val reason: String
    }

    abstract class NoGetMethod : KtFirDiagnostic<KtArrayAccessExpression>() {
        override val diagnosticClass get() = NoGetMethod::class
    }

    abstract class NoSetMethod : KtFirDiagnostic<KtArrayAccessExpression>() {
        override val diagnosticClass get() = NoSetMethod::class
    }

    abstract class ToplevelTypealiasesOnly : KtFirDiagnostic<KtTypeAlias>() {
        override val diagnosticClass get() = ToplevelTypealiasesOnly::class
    }

    abstract class RedundantVisibilityModifier : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = RedundantVisibilityModifier::class
    }

    abstract class RedundantModalityModifier : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = RedundantModalityModifier::class
    }

    abstract class RedundantReturnUnitType : KtFirDiagnostic<PsiTypeElement>() {
        override val diagnosticClass get() = RedundantReturnUnitType::class
    }

    abstract class RedundantExplicitType : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RedundantExplicitType::class
    }

    abstract class RedundantSingleExpressionStringTemplate : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RedundantSingleExpressionStringTemplate::class
    }

    abstract class CanBeVal : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = CanBeVal::class
    }

    abstract class CanBeReplacedWithOperatorAssignment : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = CanBeReplacedWithOperatorAssignment::class
    }

    abstract class RedundantCallOfConversionMethod : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RedundantCallOfConversionMethod::class
    }

    abstract class ArrayEqualityOperatorCanBeReplacedWithEquals : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ArrayEqualityOperatorCanBeReplacedWithEquals::class
    }

    abstract class EmptyRange : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = EmptyRange::class
    }

    abstract class RedundantSetterParameterType : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RedundantSetterParameterType::class
    }

    abstract class UnusedVariable : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = UnusedVariable::class
    }

    abstract class AssignedValueIsNeverRead : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AssignedValueIsNeverRead::class
    }

    abstract class VariableInitializerIsRedundant : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = VariableInitializerIsRedundant::class
    }

    abstract class VariableNeverRead : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = VariableNeverRead::class
    }

    abstract class UselessCallOnNotNull : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UselessCallOnNotNull::class
    }

    abstract class ReturnNotAllowed : KtFirDiagnostic<KtReturnExpression>() {
        override val diagnosticClass get() = ReturnNotAllowed::class
    }

    abstract class ReturnInFunctionWithExpressionBody : KtFirDiagnostic<KtReturnExpression>() {
        override val diagnosticClass get() = ReturnInFunctionWithExpressionBody::class
    }

}
