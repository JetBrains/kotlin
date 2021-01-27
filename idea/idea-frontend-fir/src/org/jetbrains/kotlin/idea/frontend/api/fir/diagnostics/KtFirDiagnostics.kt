/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtTypeReference

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class KtFirDiagnostic : KtDiagnosticWithPsi {
    abstract class Syntax : KtFirDiagnostic() {
        override val diagnosticClass get() = Syntax::class
    }

    abstract class OtherError : KtFirDiagnostic() {
        override val diagnosticClass get() = OtherError::class
    }

    abstract class IllegalConstExpression : KtFirDiagnostic() {
        override val diagnosticClass get() = IllegalConstExpression::class
    }

    abstract class IllegalUnderscore : KtFirDiagnostic() {
        override val diagnosticClass get() = IllegalUnderscore::class
    }

    abstract class ExpressionRequired : KtFirDiagnostic() {
        override val diagnosticClass get() = ExpressionRequired::class
    }

    abstract class BreakOrContinueOutsideALoop : KtFirDiagnostic() {
        override val diagnosticClass get() = BreakOrContinueOutsideALoop::class
    }

    abstract class NotALoopLabel : KtFirDiagnostic() {
        override val diagnosticClass get() = NotALoopLabel::class
    }

    abstract class VariableExpected : KtFirDiagnostic() {
        override val diagnosticClass get() = VariableExpected::class
    }

    abstract class ReturnNotAllowed : KtFirDiagnostic() {
        override val diagnosticClass get() = ReturnNotAllowed::class
    }

    abstract class DelegationInInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = DelegationInInterface::class
    }

    abstract class Hidden : KtFirDiagnostic() {
        override val diagnosticClass get() = Hidden::class
        abstract val hidden: KtSymbol
    }

    abstract class UnresolvedReference : KtFirDiagnostic() {
        override val diagnosticClass get() = UnresolvedReference::class
        abstract val reference: String
    }

    abstract class UnresolvedLabel : KtFirDiagnostic() {
        override val diagnosticClass get() = UnresolvedLabel::class
    }

    abstract class DeserializationError : KtFirDiagnostic() {
        override val diagnosticClass get() = DeserializationError::class
    }

    abstract class ErrorFromJavaResolution : KtFirDiagnostic() {
        override val diagnosticClass get() = ErrorFromJavaResolution::class
    }

    abstract class UnknownCallableKind : KtFirDiagnostic() {
        override val diagnosticClass get() = UnknownCallableKind::class
    }

    abstract class MissingStdlibClass : KtFirDiagnostic() {
        override val diagnosticClass get() = MissingStdlibClass::class
    }

    abstract class NoThis : KtFirDiagnostic() {
        override val diagnosticClass get() = NoThis::class
    }

    abstract class SuperIsNotAnExpression : KtFirDiagnostic() {
        override val diagnosticClass get() = SuperIsNotAnExpression::class
    }

    abstract class SuperNotAvailable : KtFirDiagnostic() {
        override val diagnosticClass get() = SuperNotAvailable::class
    }

    abstract class AbstractSuperCall : KtFirDiagnostic() {
        override val diagnosticClass get() = AbstractSuperCall::class
    }

    abstract class InstanceAccessBeforeSuperCall : KtFirDiagnostic() {
        override val diagnosticClass get() = InstanceAccessBeforeSuperCall::class
        abstract val target: String
    }

    abstract class TypeParameterAsSupertype : KtFirDiagnostic() {
        override val diagnosticClass get() = TypeParameterAsSupertype::class
    }

    abstract class EnumAsSupertype : KtFirDiagnostic() {
        override val diagnosticClass get() = EnumAsSupertype::class
    }

    abstract class RecursionInSupertypes : KtFirDiagnostic() {
        override val diagnosticClass get() = RecursionInSupertypes::class
    }

    abstract class NotASupertype : KtFirDiagnostic() {
        override val diagnosticClass get() = NotASupertype::class
    }

    abstract class SuperclassNotAccessibleFromInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = SuperclassNotAccessibleFromInterface::class
    }

    abstract class QualifiedSupertypeExtendedByOtherSupertype : KtFirDiagnostic() {
        override val diagnosticClass get() = QualifiedSupertypeExtendedByOtherSupertype::class
        abstract val otherSuperType: KtClassLikeSymbol
    }

    abstract class SupertypeInitializedInInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = SupertypeInitializedInInterface::class
    }

    abstract class InterfaceWithSuperclass : KtFirDiagnostic() {
        override val diagnosticClass get() = InterfaceWithSuperclass::class
    }

    abstract class ClassInSupertypeForEnum : KtFirDiagnostic() {
        override val diagnosticClass get() = ClassInSupertypeForEnum::class
    }

    abstract class SealedSupertype : KtFirDiagnostic() {
        override val diagnosticClass get() = SealedSupertype::class
    }

    abstract class SealedSupertypeInLocalClass : KtFirDiagnostic() {
        override val diagnosticClass get() = SealedSupertypeInLocalClass::class
    }

    abstract class ConstructorInObject : KtFirDiagnostic() {
        override val diagnosticClass get() = ConstructorInObject::class
        abstract override val psi: KtDeclaration
    }

    abstract class ConstructorInInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = ConstructorInInterface::class
        abstract override val psi: KtDeclaration
    }

    abstract class NonPrivateConstructorInEnum : KtFirDiagnostic() {
        override val diagnosticClass get() = NonPrivateConstructorInEnum::class
    }

    abstract class NonPrivateConstructorInSealed : KtFirDiagnostic() {
        override val diagnosticClass get() = NonPrivateConstructorInSealed::class
    }

    abstract class CyclicConstructorDelegationCall : KtFirDiagnostic() {
        override val diagnosticClass get() = CyclicConstructorDelegationCall::class
    }

    abstract class PrimaryConstructorDelegationCallExpected : KtFirDiagnostic() {
        override val diagnosticClass get() = PrimaryConstructorDelegationCallExpected::class
    }

    abstract class SupertypeInitializedWithoutPrimaryConstructor : KtFirDiagnostic() {
        override val diagnosticClass get() = SupertypeInitializedWithoutPrimaryConstructor::class
    }

    abstract class DelegationSuperCallInEnumConstructor : KtFirDiagnostic() {
        override val diagnosticClass get() = DelegationSuperCallInEnumConstructor::class
    }

    abstract class PrimaryConstructorRequiredForDataClass : KtFirDiagnostic() {
        override val diagnosticClass get() = PrimaryConstructorRequiredForDataClass::class
    }

    abstract class ExplicitDelegationCallRequired : KtFirDiagnostic() {
        override val diagnosticClass get() = ExplicitDelegationCallRequired::class
    }

    abstract class SealedClassConstructorCall : KtFirDiagnostic() {
        override val diagnosticClass get() = SealedClassConstructorCall::class
    }

    abstract class AnnotationArgumentKclassLiteralOfTypeParameterError : KtFirDiagnostic() {
        override val diagnosticClass get() = AnnotationArgumentKclassLiteralOfTypeParameterError::class
        abstract override val psi: KtExpression
    }

    abstract class AnnotationArgumentMustBeConst : KtFirDiagnostic() {
        override val diagnosticClass get() = AnnotationArgumentMustBeConst::class
        abstract override val psi: KtExpression
    }

    abstract class AnnotationArgumentMustBeEnumConst : KtFirDiagnostic() {
        override val diagnosticClass get() = AnnotationArgumentMustBeEnumConst::class
        abstract override val psi: KtExpression
    }

    abstract class AnnotationArgumentMustBeKclassLiteral : KtFirDiagnostic() {
        override val diagnosticClass get() = AnnotationArgumentMustBeKclassLiteral::class
        abstract override val psi: KtExpression
    }

    abstract class AnnotationClassMember : KtFirDiagnostic() {
        override val diagnosticClass get() = AnnotationClassMember::class
    }

    abstract class AnnotationParameterDefaultValueMustBeConstant : KtFirDiagnostic() {
        override val diagnosticClass get() = AnnotationParameterDefaultValueMustBeConstant::class
        abstract override val psi: KtExpression
    }

    abstract class InvalidTypeOfAnnotationMember : KtFirDiagnostic() {
        override val diagnosticClass get() = InvalidTypeOfAnnotationMember::class
        abstract override val psi: KtTypeReference
    }

    abstract class LocalAnnotationClassError : KtFirDiagnostic() {
        override val diagnosticClass get() = LocalAnnotationClassError::class
        abstract override val psi: KtClassOrObject
    }

    abstract class MissingValOnAnnotationParameter : KtFirDiagnostic() {
        override val diagnosticClass get() = MissingValOnAnnotationParameter::class
        abstract override val psi: KtParameter
    }

    abstract class NonConstValUsedInConstantExpression : KtFirDiagnostic() {
        override val diagnosticClass get() = NonConstValUsedInConstantExpression::class
        abstract override val psi: KtExpression
    }

    abstract class NotAnAnnotationClass : KtFirDiagnostic() {
        override val diagnosticClass get() = NotAnAnnotationClass::class
        abstract val annotationName: String
    }

    abstract class NullableTypeOfAnnotationMember : KtFirDiagnostic() {
        override val diagnosticClass get() = NullableTypeOfAnnotationMember::class
        abstract override val psi: KtTypeReference
    }

    abstract class VarAnnotationParameter : KtFirDiagnostic() {
        override val diagnosticClass get() = VarAnnotationParameter::class
        abstract override val psi: KtParameter
    }

    abstract class ExposedTypealiasExpandedType : KtFirDiagnostic() {
        override val diagnosticClass get() = ExposedTypealiasExpandedType::class
        abstract override val psi: KtNamedDeclaration
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedFunctionReturnType : KtFirDiagnostic() {
        override val diagnosticClass get() = ExposedFunctionReturnType::class
        abstract override val psi: KtNamedDeclaration
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedReceiverType : KtFirDiagnostic() {
        override val diagnosticClass get() = ExposedReceiverType::class
        abstract override val psi: KtTypeReference
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedPropertyType : KtFirDiagnostic() {
        override val diagnosticClass get() = ExposedPropertyType::class
        abstract override val psi: KtNamedDeclaration
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedParameterType : KtFirDiagnostic() {
        override val diagnosticClass get() = ExposedParameterType::class
        abstract override val psi: KtParameter
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedSuperInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = ExposedSuperInterface::class
        abstract override val psi: KtTypeReference
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedSuperClass : KtFirDiagnostic() {
        override val diagnosticClass get() = ExposedSuperClass::class
        abstract override val psi: KtTypeReference
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedTypeParameterBound : KtFirDiagnostic() {
        override val diagnosticClass get() = ExposedTypeParameterBound::class
        abstract override val psi: KtTypeReference
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class InapplicableInfixModifier : KtFirDiagnostic() {
        override val diagnosticClass get() = InapplicableInfixModifier::class
        abstract val modifier: String
    }

    abstract class RepeatedModifier : KtFirDiagnostic() {
        override val diagnosticClass get() = RepeatedModifier::class
        abstract val modifier: KtModifierKeywordToken
    }

    abstract class RedundantModifier : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantModifier::class
        abstract val redundantModifier: KtModifierKeywordToken
        abstract val conflictingModifier: KtModifierKeywordToken
    }

    abstract class DeprecatedModifierPair : KtFirDiagnostic() {
        override val diagnosticClass get() = DeprecatedModifierPair::class
        abstract val deprecatedModifier: KtModifierKeywordToken
        abstract val conflictingModifier: KtModifierKeywordToken
    }

    abstract class IncompatibleModifiers : KtFirDiagnostic() {
        override val diagnosticClass get() = IncompatibleModifiers::class
        abstract val modifier1: KtModifierKeywordToken
        abstract val modifier2: KtModifierKeywordToken
    }

    abstract class RedundantOpenInInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantOpenInInterface::class
        abstract override val psi: KtModifierListOwner
    }

    abstract class NoneApplicable : KtFirDiagnostic() {
        override val diagnosticClass get() = NoneApplicable::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class InapplicableCandidate : KtFirDiagnostic() {
        override val diagnosticClass get() = InapplicableCandidate::class
        abstract val candidate: KtSymbol
    }

    abstract class InapplicableLateinitModifier : KtFirDiagnostic() {
        override val diagnosticClass get() = InapplicableLateinitModifier::class
        abstract val reason: String
    }

    abstract class Ambiguity : KtFirDiagnostic() {
        override val diagnosticClass get() = Ambiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class AssignOperatorAmbiguity : KtFirDiagnostic() {
        override val diagnosticClass get() = AssignOperatorAmbiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class TypeMismatch : KtFirDiagnostic() {
        override val diagnosticClass get() = TypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
    }

    abstract class RecursionInImplicitTypes : KtFirDiagnostic() {
        override val diagnosticClass get() = RecursionInImplicitTypes::class
    }

    abstract class InferenceError : KtFirDiagnostic() {
        override val diagnosticClass get() = InferenceError::class
    }

    abstract class ProjectionOnNonClassTypeArgument : KtFirDiagnostic() {
        override val diagnosticClass get() = ProjectionOnNonClassTypeArgument::class
    }

    abstract class UpperBoundViolated : KtFirDiagnostic() {
        override val diagnosticClass get() = UpperBoundViolated::class
        abstract val typeParameter: KtTypeParameterSymbol
        abstract val violatedType: KtType
    }

    abstract class TypeArgumentsNotAllowed : KtFirDiagnostic() {
        override val diagnosticClass get() = TypeArgumentsNotAllowed::class
    }

    abstract class WrongNumberOfTypeArguments : KtFirDiagnostic() {
        override val diagnosticClass get() = WrongNumberOfTypeArguments::class
        abstract val expectedCount: Int
        abstract val classifier: KtClassLikeSymbol
    }

    abstract class NoTypeForTypeParameter : KtFirDiagnostic() {
        override val diagnosticClass get() = NoTypeForTypeParameter::class
    }

    abstract class TypeParametersInObject : KtFirDiagnostic() {
        override val diagnosticClass get() = TypeParametersInObject::class
    }

    abstract class IllegalProjectionUsage : KtFirDiagnostic() {
        override val diagnosticClass get() = IllegalProjectionUsage::class
    }

    abstract class TypeParametersInEnum : KtFirDiagnostic() {
        override val diagnosticClass get() = TypeParametersInEnum::class
    }

    abstract class ConflictingProjection : KtFirDiagnostic() {
        override val diagnosticClass get() = ConflictingProjection::class
        abstract val type: String
    }

    abstract class VarianceOnTypeParameterNotAllowed : KtFirDiagnostic() {
        override val diagnosticClass get() = VarianceOnTypeParameterNotAllowed::class
    }

    abstract class ReturnTypeMismatchOnOverride : KtFirDiagnostic() {
        override val diagnosticClass get() = ReturnTypeMismatchOnOverride::class
        abstract val returnType: String
        abstract val superFunction: KtSymbol
    }

    abstract class PropertyTypeMismatchOnOverride : KtFirDiagnostic() {
        override val diagnosticClass get() = PropertyTypeMismatchOnOverride::class
        abstract val propertyType: String
        abstract val targetProperty: KtSymbol
    }

    abstract class VarTypeMismatchOnOverride : KtFirDiagnostic() {
        override val diagnosticClass get() = VarTypeMismatchOnOverride::class
        abstract val variableType: String
        abstract val targetVariable: KtSymbol
    }

    abstract class ManyCompanionObjects : KtFirDiagnostic() {
        override val diagnosticClass get() = ManyCompanionObjects::class
    }

    abstract class ConflictingOverloads : KtFirDiagnostic() {
        override val diagnosticClass get() = ConflictingOverloads::class
        abstract val conflictingOverloads: String
    }

    abstract class Redeclaration : KtFirDiagnostic() {
        override val diagnosticClass get() = Redeclaration::class
        abstract val conflictingDeclaration: String
    }

    abstract class AnyMethodImplementedInInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = AnyMethodImplementedInInterface::class
    }

    abstract class LocalObjectNotAllowed : KtFirDiagnostic() {
        override val diagnosticClass get() = LocalObjectNotAllowed::class
        abstract override val psi: KtNamedDeclaration
        abstract val objectName: Name
    }

    abstract class LocalInterfaceNotAllowed : KtFirDiagnostic() {
        override val diagnosticClass get() = LocalInterfaceNotAllowed::class
        abstract override val psi: KtNamedDeclaration
        abstract val interfaceName: Name
    }

    abstract class AbstractFunctionInNonAbstractClass : KtFirDiagnostic() {
        override val diagnosticClass get() = AbstractFunctionInNonAbstractClass::class
        abstract override val psi: KtFunction
        abstract val function: KtSymbol
        abstract val containingClass: KtSymbol
    }

    abstract class AbstractFunctionWithBody : KtFirDiagnostic() {
        override val diagnosticClass get() = AbstractFunctionWithBody::class
        abstract override val psi: KtFunction
        abstract val function: KtSymbol
    }

    abstract class NonAbstractFunctionWithNoBody : KtFirDiagnostic() {
        override val diagnosticClass get() = NonAbstractFunctionWithNoBody::class
        abstract override val psi: KtFunction
        abstract val function: KtSymbol
    }

    abstract class PrivateFunctionWithNoBody : KtFirDiagnostic() {
        override val diagnosticClass get() = PrivateFunctionWithNoBody::class
        abstract override val psi: KtFunction
        abstract val function: KtSymbol
    }

    abstract class NonMemberFunctionNoBody : KtFirDiagnostic() {
        override val diagnosticClass get() = NonMemberFunctionNoBody::class
        abstract override val psi: KtFunction
        abstract val function: KtSymbol
    }

    abstract class FunctionDeclarationWithNoName : KtFirDiagnostic() {
        override val diagnosticClass get() = FunctionDeclarationWithNoName::class
        abstract override val psi: KtFunction
    }

    abstract class AnonymousFunctionParameterWithDefaultValue : KtFirDiagnostic() {
        override val diagnosticClass get() = AnonymousFunctionParameterWithDefaultValue::class
        abstract override val psi: KtParameter
    }

    abstract class UselessVarargOnParameter : KtFirDiagnostic() {
        override val diagnosticClass get() = UselessVarargOnParameter::class
        abstract override val psi: KtParameter
    }

    abstract class AbstractPropertyInNonAbstractClass : KtFirDiagnostic() {
        override val diagnosticClass get() = AbstractPropertyInNonAbstractClass::class
        abstract override val psi: KtModifierListOwner
        abstract val property: KtSymbol
        abstract val containingClass: KtSymbol
    }

    abstract class PrivatePropertyInInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = PrivatePropertyInInterface::class
        abstract override val psi: KtProperty
    }

    abstract class AbstractPropertyWithInitializer : KtFirDiagnostic() {
        override val diagnosticClass get() = AbstractPropertyWithInitializer::class
        abstract override val psi: KtExpression
    }

    abstract class PropertyInitializerInInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = PropertyInitializerInInterface::class
        abstract override val psi: KtExpression
    }

    abstract class PropertyWithNoTypeNoInitializer : KtFirDiagnostic() {
        override val diagnosticClass get() = PropertyWithNoTypeNoInitializer::class
        abstract override val psi: KtProperty
    }

    abstract class AbstractDelegatedProperty : KtFirDiagnostic() {
        override val diagnosticClass get() = AbstractDelegatedProperty::class
        abstract override val psi: KtPropertyDelegate
    }

    abstract class DelegatedPropertyInInterface : KtFirDiagnostic() {
        override val diagnosticClass get() = DelegatedPropertyInInterface::class
        abstract override val psi: KtPropertyDelegate
    }

    abstract class AbstractPropertyWithGetter : KtFirDiagnostic() {
        override val diagnosticClass get() = AbstractPropertyWithGetter::class
        abstract override val psi: KtPropertyAccessor
    }

    abstract class AbstractPropertyWithSetter : KtFirDiagnostic() {
        override val diagnosticClass get() = AbstractPropertyWithSetter::class
        abstract override val psi: KtPropertyAccessor
    }

    abstract class PrivateSetterForAbstractProperty : KtFirDiagnostic() {
        override val diagnosticClass get() = PrivateSetterForAbstractProperty::class
    }

    abstract class PrivateSetterForOpenProperty : KtFirDiagnostic() {
        override val diagnosticClass get() = PrivateSetterForOpenProperty::class
    }

    abstract class ExpectedPrivateDeclaration : KtFirDiagnostic() {
        override val diagnosticClass get() = ExpectedPrivateDeclaration::class
        abstract override val psi: KtModifierListOwner
    }

    abstract class ExpectedDeclarationWithBody : KtFirDiagnostic() {
        override val diagnosticClass get() = ExpectedDeclarationWithBody::class
        abstract override val psi: KtDeclaration
    }

    abstract class ExpectedPropertyInitializer : KtFirDiagnostic() {
        override val diagnosticClass get() = ExpectedPropertyInitializer::class
        abstract override val psi: KtExpression
    }

    abstract class ExpectedDelegatedProperty : KtFirDiagnostic() {
        override val diagnosticClass get() = ExpectedDelegatedProperty::class
        abstract override val psi: KtPropertyDelegate
    }

    abstract class InitializerRequiredForDestructuringDeclaration : KtFirDiagnostic() {
        override val diagnosticClass get() = InitializerRequiredForDestructuringDeclaration::class
        abstract override val psi: KtDestructuringDeclaration
    }

    abstract class ComponentFunctionMissing : KtFirDiagnostic() {
        override val diagnosticClass get() = ComponentFunctionMissing::class
        abstract val missingFunctionName: Name
        abstract val destructingType: KtType
    }

    abstract class ComponentFunctionAmbiguity : KtFirDiagnostic() {
        override val diagnosticClass get() = ComponentFunctionAmbiguity::class
        abstract val functionWithAmbiguityName: Name
        abstract val candidates: List<KtSymbol>
    }

    abstract class UninitializedVariable : KtFirDiagnostic() {
        override val diagnosticClass get() = UninitializedVariable::class
        abstract val variable: KtVariableSymbol
    }

    abstract class WrongInvocationKind : KtFirDiagnostic() {
        override val diagnosticClass get() = WrongInvocationKind::class
        abstract val declaration: KtSymbol
        abstract val requiredRange: EventOccurrencesRange
        abstract val actualRange: EventOccurrencesRange
    }

    abstract class LeakedInPlaceLambda : KtFirDiagnostic() {
        override val diagnosticClass get() = LeakedInPlaceLambda::class
        abstract val lambda: KtSymbol
    }

    abstract class WrongImpliesCondition : KtFirDiagnostic() {
        override val diagnosticClass get() = WrongImpliesCondition::class
    }

    abstract class RedundantVisibilityModifier : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantVisibilityModifier::class
        abstract override val psi: KtModifierListOwner
    }

    abstract class RedundantModalityModifier : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantModalityModifier::class
        abstract override val psi: KtModifierListOwner
    }

    abstract class RedundantReturnUnitType : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantReturnUnitType::class
        abstract override val psi: PsiTypeElement
    }

    abstract class RedundantExplicitType : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantExplicitType::class
    }

    abstract class RedundantSingleExpressionStringTemplate : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantSingleExpressionStringTemplate::class
    }

    abstract class CanBeVal : KtFirDiagnostic() {
        override val diagnosticClass get() = CanBeVal::class
        abstract override val psi: KtDeclaration
    }

    abstract class CanBeReplacedWithOperatorAssignment : KtFirDiagnostic() {
        override val diagnosticClass get() = CanBeReplacedWithOperatorAssignment::class
        abstract override val psi: KtExpression
    }

    abstract class RedundantCallOfConversionMethod : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantCallOfConversionMethod::class
    }

    abstract class ArrayEqualityOperatorCanBeReplacedWithEquals : KtFirDiagnostic() {
        override val diagnosticClass get() = ArrayEqualityOperatorCanBeReplacedWithEquals::class
        abstract override val psi: KtExpression
    }

    abstract class EmptyRange : KtFirDiagnostic() {
        override val diagnosticClass get() = EmptyRange::class
    }

    abstract class RedundantSetterParameterType : KtFirDiagnostic() {
        override val diagnosticClass get() = RedundantSetterParameterType::class
    }

    abstract class UnusedVariable : KtFirDiagnostic() {
        override val diagnosticClass get() = UnusedVariable::class
        abstract override val psi: KtNamedDeclaration
    }

    abstract class AssignedValueIsNeverRead : KtFirDiagnostic() {
        override val diagnosticClass get() = AssignedValueIsNeverRead::class
    }

    abstract class VariableInitializerIsRedundant : KtFirDiagnostic() {
        override val diagnosticClass get() = VariableInitializerIsRedundant::class
    }

    abstract class VariableNeverRead : KtFirDiagnostic() {
        override val diagnosticClass get() = VariableNeverRead::class
        abstract override val psi: KtNamedDeclaration
    }

    abstract class UselessCallOnNotNull : KtFirDiagnostic() {
        override val diagnosticClass get() = UselessCallOnNotNull::class
    }

}
