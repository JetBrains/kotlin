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
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
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
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtWhenExpression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class KtFirDiagnostic<PSI: PsiElement> : KtDiagnosticWithPsi<PSI> {
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

    abstract class ReturnNotAllowed : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ReturnNotAllowed::class
    }

    abstract class DelegationInInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegationInInterface::class
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

    abstract class TypeParameterAsSupertype : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParameterAsSupertype::class
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

    abstract class ConstructorInObject : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ConstructorInObject::class
    }

    abstract class ConstructorInInterface : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ConstructorInInterface::class
    }

    abstract class NonPrivateConstructorInEnum : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonPrivateConstructorInEnum::class
    }

    abstract class NonPrivateConstructorInSealed : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonPrivateConstructorInSealed::class
    }

    abstract class CyclicConstructorDelegationCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = CyclicConstructorDelegationCall::class
    }

    abstract class PrimaryConstructorDelegationCallExpected : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = PrimaryConstructorDelegationCallExpected::class
    }

    abstract class SupertypeInitializedWithoutPrimaryConstructor : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SupertypeInitializedWithoutPrimaryConstructor::class
    }

    abstract class DelegationSuperCallInEnumConstructor : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegationSuperCallInEnumConstructor::class
    }

    abstract class PrimaryConstructorRequiredForDataClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = PrimaryConstructorRequiredForDataClass::class
    }

    abstract class ExplicitDelegationCallRequired : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ExplicitDelegationCallRequired::class
    }

    abstract class SealedClassConstructorCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SealedClassConstructorCall::class
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

    abstract class ExposedTypealiasExpandedType : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedTypealiasExpandedType::class
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedFunctionReturnType : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedFunctionReturnType::class
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedReceiverType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExposedReceiverType::class
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedPropertyType : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedPropertyType::class
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedParameterType : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ExposedParameterType::class
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedSuperInterface : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExposedSuperInterface::class
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedSuperClass : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExposedSuperClass::class
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
    }

    abstract class ExposedTypeParameterBound : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExposedTypeParameterBound::class
        abstract val elementVisibility: Visibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: Visibility
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

    abstract class NoneApplicable : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NoneApplicable::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class InapplicableCandidate : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InapplicableCandidate::class
        abstract val candidate: KtSymbol
    }

    abstract class InapplicableLateinitModifier : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InapplicableLateinitModifier::class
        abstract val reason: String
    }

    abstract class Ambiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Ambiguity::class
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
        abstract val typeParameter: KtTypeParameterSymbol
        abstract val violatedType: KtType
    }

    abstract class TypeArgumentsNotAllowed : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeArgumentsNotAllowed::class
    }

    abstract class WrongNumberOfTypeArguments : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = WrongNumberOfTypeArguments::class
        abstract val expectedCount: Int
        abstract val classifier: KtClassLikeSymbol
    }

    abstract class NoTypeForTypeParameter : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NoTypeForTypeParameter::class
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

    abstract class ManyCompanionObjects : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ManyCompanionObjects::class
    }

    abstract class ConflictingOverloads : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConflictingOverloads::class
        abstract val conflictingOverloads: String
    }

    abstract class Redeclaration : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Redeclaration::class
        abstract val conflictingDeclaration: String
    }

    abstract class AnyMethodImplementedInInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AnyMethodImplementedInInterface::class
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

    abstract class PrivateSetterForAbstractProperty : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = PrivateSetterForAbstractProperty::class
    }

    abstract class PrivateSetterForOpenProperty : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = PrivateSetterForOpenProperty::class
    }

    abstract class ExpectedPrivateDeclaration : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = ExpectedPrivateDeclaration::class
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

    abstract class UninitializedVariable : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UninitializedVariable::class
        abstract val variable: KtVariableSymbol
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
        abstract val missingWhenCases: List<Any>
    }

    abstract class InvalidIfAsExpression : KtFirDiagnostic<KtIfExpression>() {
        override val diagnosticClass get() = InvalidIfAsExpression::class
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

}
