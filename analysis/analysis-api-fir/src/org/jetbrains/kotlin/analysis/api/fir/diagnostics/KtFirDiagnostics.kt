/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
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

sealed class KtFirDiagnostic<PSI : PsiElement> : KtDiagnosticWithPsi<PSI> {
    abstract class Unsupported : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Unsupported::class
        abstract val unsupported: String
    }

    abstract class UnsupportedFeature : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnsupportedFeature::class
        abstract val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>
    }

    abstract class NewInferenceError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NewInferenceError::class
        abstract val error: String
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

    abstract class ExpressionExpected : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ExpressionExpected::class
    }

    abstract class AssignmentInExpressionContext : KtFirDiagnostic<KtBinaryExpression>() {
        override val diagnosticClass get() = AssignmentInExpressionContext::class
    }

    abstract class BreakOrContinueOutsideALoop : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = BreakOrContinueOutsideALoop::class
    }

    abstract class NotALoopLabel : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NotALoopLabel::class
    }

    abstract class BreakOrContinueJumpsAcrossFunctionBoundary : KtFirDiagnostic<KtExpressionWithLabel>() {
        override val diagnosticClass get() = BreakOrContinueJumpsAcrossFunctionBoundary::class
    }

    abstract class VariableExpected : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = VariableExpected::class
    }

    abstract class DelegationInInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegationInInterface::class
    }

    abstract class DelegationNotToInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegationNotToInterface::class
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

    abstract class IntLiteralOutOfRange : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IntLiteralOutOfRange::class
    }

    abstract class FloatLiteralOutOfRange : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = FloatLiteralOutOfRange::class
    }

    abstract class WrongLongSuffix : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = WrongLongSuffix::class
    }

    abstract class UnsignedLiteralWithoutDeclarationsOnClasspath : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = UnsignedLiteralWithoutDeclarationsOnClasspath::class
    }

    abstract class DivisionByZero : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = DivisionByZero::class
    }

    abstract class ValOrVarOnLoopParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ValOrVarOnLoopParameter::class
        abstract val valOrVar: KtKeywordToken
    }

    abstract class ValOrVarOnFunParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ValOrVarOnFunParameter::class
        abstract val valOrVar: KtKeywordToken
    }

    abstract class ValOrVarOnCatchParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ValOrVarOnCatchParameter::class
        abstract val valOrVar: KtKeywordToken
    }

    abstract class ValOrVarOnSecondaryConstructorParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ValOrVarOnSecondaryConstructorParameter::class
        abstract val valOrVar: KtKeywordToken
    }

    abstract class InvisibleSetter : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InvisibleSetter::class
        abstract val property: KtVariableSymbol
        abstract val visibility: Visibility
        abstract val callableId: CallableId
    }

    abstract class InvisibleReference : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InvisibleReference::class
        abstract val reference: KtSymbol
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

    abstract class MissingStdlibClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = MissingStdlibClass::class
    }

    abstract class NoThis : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NoThis::class
    }

    abstract class DeprecationError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecationError::class
        abstract val reference: KtSymbol
        abstract val message: String
    }

    abstract class Deprecation : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Deprecation::class
        abstract val reference: KtSymbol
        abstract val message: String
    }

    abstract class ApiNotAvailable : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ApiNotAvailable::class
        abstract val sinceKotlinVersion: ApiVersion
        abstract val currentVersion: ApiVersion
    }

    abstract class UnresolvedReferenceWrongReceiver : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnresolvedReferenceWrongReceiver::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class UnresolvedImport : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnresolvedImport::class
        abstract val reference: String
    }

    abstract class CreatingAnInstanceOfAbstractClass : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = CreatingAnInstanceOfAbstractClass::class
    }

    abstract class FunctionCallExpected : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = FunctionCallExpected::class
        abstract val functionName: String
        abstract val hasValueParameters: Boolean
    }

    abstract class IllegalSelector : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalSelector::class
    }

    abstract class NoReceiverAllowed : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NoReceiverAllowed::class
    }

    abstract class FunctionExpected : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = FunctionExpected::class
        abstract val expression: String
        abstract val type: KtType
    }

    abstract class ResolutionToClassifier : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ResolutionToClassifier::class
        abstract val classSymbol: KtClassLikeSymbol
    }

    abstract class AmbiguousAlteredAssign : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AmbiguousAlteredAssign::class
        abstract val altererNames: List<String?>
    }

    abstract class ForbiddenBinaryMod : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ForbiddenBinaryMod::class
        abstract val forbiddenFunction: KtSymbol
        abstract val suggestedFunction: String
    }

    abstract class DeprecatedBinaryMod : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedBinaryMod::class
        abstract val forbiddenFunction: KtSymbol
        abstract val suggestedFunction: String
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

    abstract class AbstractSuperCallWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AbstractSuperCallWarning::class
    }

    abstract class InstanceAccessBeforeSuperCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InstanceAccessBeforeSuperCall::class
        abstract val target: String
    }

    abstract class SuperCallWithDefaultParameters : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SuperCallWithDefaultParameters::class
        abstract val name: String
    }

    abstract class InterfaceCantCallDefaultMethodViaSuper : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InterfaceCantCallDefaultMethodViaSuper::class
    }

    abstract class NotASupertype : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NotASupertype::class
    }

    abstract class TypeArgumentsRedundantInSuperQualifier : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = TypeArgumentsRedundantInSuperQualifier::class
    }

    abstract class SuperclassNotAccessibleFromInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SuperclassNotAccessibleFromInterface::class
    }

    abstract class QualifiedSupertypeExtendedByOtherSupertype : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = QualifiedSupertypeExtendedByOtherSupertype::class
        abstract val otherSuperType: KtSymbol
    }

    abstract class SupertypeInitializedInInterface : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SupertypeInitializedInInterface::class
    }

    abstract class InterfaceWithSuperclass : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = InterfaceWithSuperclass::class
    }

    abstract class FinalSupertype : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = FinalSupertype::class
    }

    abstract class ClassCannotBeExtendedDirectly : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ClassCannotBeExtendedDirectly::class
        abstract val classSymbol: KtClassLikeSymbol
    }

    abstract class SupertypeIsExtensionFunctionType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SupertypeIsExtensionFunctionType::class
    }

    abstract class SingletonInSupertype : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SingletonInSupertype::class
    }

    abstract class NullableSupertype : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = NullableSupertype::class
    }

    abstract class ManyClassesInSupertypeList : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ManyClassesInSupertypeList::class
    }

    abstract class SupertypeAppearsTwice : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SupertypeAppearsTwice::class
    }

    abstract class ClassInSupertypeForEnum : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ClassInSupertypeForEnum::class
    }

    abstract class SealedSupertype : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SealedSupertype::class
    }

    abstract class SealedSupertypeInLocalClass : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SealedSupertypeInLocalClass::class
        abstract val declarationType: String
        abstract val sealedClassKind: ClassKind
    }

    abstract class SealedInheritorInDifferentPackage : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SealedInheritorInDifferentPackage::class
    }

    abstract class SealedInheritorInDifferentModule : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = SealedInheritorInDifferentModule::class
    }

    abstract class ClassInheritsJavaSealedClass : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ClassInheritsJavaSealedClass::class
    }

    abstract class SupertypeNotAClassOrInterface : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = SupertypeNotAClassOrInterface::class
        abstract val reason: String
    }

    abstract class CyclicInheritanceHierarchy : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = CyclicInheritanceHierarchy::class
    }

    abstract class ExpandedTypeCannotBeInherited : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ExpandedTypeCannotBeInherited::class
        abstract val type: KtType
    }

    abstract class ProjectionInImmediateArgumentToSupertype : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = ProjectionInImmediateArgumentToSupertype::class
    }

    abstract class InconsistentTypeParameterValues : KtFirDiagnostic<KtClass>() {
        override val diagnosticClass get() = InconsistentTypeParameterValues::class
        abstract val typeParameter: KtTypeParameterSymbol
        abstract val type: KtClassLikeSymbol
        abstract val bounds: List<KtType>
    }

    abstract class InconsistentTypeParameterBounds : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InconsistentTypeParameterBounds::class
        abstract val typeParameter: KtTypeParameterSymbol
        abstract val type: KtClassLikeSymbol
        abstract val bounds: List<KtType>
    }

    abstract class AmbiguousSuper : KtFirDiagnostic<KtSuperExpression>() {
        override val diagnosticClass get() = AmbiguousSuper::class
        abstract val candidates: List<KtType>
    }

    abstract class WrongMultipleInheritance : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = WrongMultipleInheritance::class
        abstract val symbol: KtCallableSymbol
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

    abstract class CycleInAnnotationParameterError : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = CycleInAnnotationParameterError::class
    }

    abstract class CycleInAnnotationParameterWarning : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = CycleInAnnotationParameterWarning::class
    }

    abstract class AnnotationClassConstructorCall : KtFirDiagnostic<KtCallExpression>() {
        override val diagnosticClass get() = AnnotationClassConstructorCall::class
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

    abstract class AnnotationUsedAsAnnotationArgument : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = AnnotationUsedAsAnnotationArgument::class
    }

    abstract class IllegalKotlinVersionStringValue : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = IllegalKotlinVersionStringValue::class
    }

    abstract class NewerVersionInSinceKotlin : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NewerVersionInSinceKotlin::class
        abstract val specifiedVersion: String
    }

    abstract class DeprecatedSinceKotlinWithUnorderedVersions : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithUnorderedVersions::class
    }

    abstract class DeprecatedSinceKotlinWithoutArguments : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithoutArguments::class
    }

    abstract class DeprecatedSinceKotlinWithoutDeprecated : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithoutDeprecated::class
    }

    abstract class DeprecatedSinceKotlinWithDeprecatedLevel : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithDeprecatedLevel::class
    }

    abstract class DeprecatedSinceKotlinOutsideKotlinSubpackage : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedSinceKotlinOutsideKotlinSubpackage::class
    }

    abstract class OverrideDeprecation : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = OverrideDeprecation::class
        abstract val overridenSymbol: KtSymbol
        abstract val deprecationInfo: DeprecationInfo
    }

    abstract class AnnotationOnSuperclassError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = AnnotationOnSuperclassError::class
    }

    abstract class AnnotationOnSuperclassWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = AnnotationOnSuperclassWarning::class
    }

    abstract class RestrictedRetentionForExpressionAnnotationError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RestrictedRetentionForExpressionAnnotationError::class
    }

    abstract class RestrictedRetentionForExpressionAnnotationWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RestrictedRetentionForExpressionAnnotationWarning::class
    }

    abstract class WrongAnnotationTarget : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = WrongAnnotationTarget::class
        abstract val actualTarget: String
    }

    abstract class WrongAnnotationTargetWithUseSiteTarget : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = WrongAnnotationTargetWithUseSiteTarget::class
        abstract val actualTarget: String
        abstract val useSiteTarget: String
    }

    abstract class InapplicableTargetOnProperty : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = InapplicableTargetOnProperty::class
        abstract val useSiteDescription: String
    }

    abstract class InapplicableTargetPropertyImmutable : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = InapplicableTargetPropertyImmutable::class
        abstract val useSiteDescription: String
    }

    abstract class InapplicableTargetPropertyHasNoDelegate : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = InapplicableTargetPropertyHasNoDelegate::class
    }

    abstract class InapplicableTargetPropertyHasNoBackingField : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = InapplicableTargetPropertyHasNoBackingField::class
    }

    abstract class InapplicableParamTarget : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = InapplicableParamTarget::class
    }

    abstract class RedundantAnnotationTarget : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RedundantAnnotationTarget::class
        abstract val useSiteDescription: String
    }

    abstract class InapplicableFileTarget : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = InapplicableFileTarget::class
    }

    abstract class RepeatedAnnotation : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatedAnnotation::class
    }

    abstract class RepeatedAnnotationWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatedAnnotationWarning::class
    }

    abstract class NotAClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NotAClass::class
    }

    abstract class WrongExtensionFunctionType : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = WrongExtensionFunctionType::class
    }

    abstract class WrongExtensionFunctionTypeWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = WrongExtensionFunctionTypeWarning::class
    }

    abstract class AnnotationInWhereClauseError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = AnnotationInWhereClauseError::class
    }

    abstract class PluginAnnotationAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = PluginAnnotationAmbiguity::class
        abstract val typeFromCompilerPhase: KtType
        abstract val typeFromTypesPhase: KtType
    }

    abstract class WrongJsQualifier : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = WrongJsQualifier::class
    }

    abstract class JsModuleProhibitedOnVar : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JsModuleProhibitedOnVar::class
    }

    abstract class JsModuleProhibitedOnNonNative : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JsModuleProhibitedOnNonNative::class
    }

    abstract class NestedJsModuleProhibited : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NestedJsModuleProhibited::class
    }

    abstract class RuntimeAnnotationNotSupported : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RuntimeAnnotationNotSupported::class
    }

    abstract class RuntimeAnnotationOnExternalDeclaration : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RuntimeAnnotationOnExternalDeclaration::class
    }

    abstract class NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun::class
        abstract val type: KtType
    }

    abstract class NativeIndexerKeyShouldBeStringOrNumber : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NativeIndexerKeyShouldBeStringOrNumber::class
        abstract val kind: String
    }

    abstract class NativeIndexerWrongParameterCount : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NativeIndexerWrongParameterCount::class
        abstract val parametersCount: Int
        abstract val kind: String
    }

    abstract class NativeIndexerCanNotHaveDefaultArguments : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NativeIndexerCanNotHaveDefaultArguments::class
        abstract val kind: String
    }

    abstract class NativeGetterReturnTypeShouldBeNullable : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NativeGetterReturnTypeShouldBeNullable::class
    }

    abstract class NativeSetterWrongReturnType : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NativeSetterWrongReturnType::class
    }

    abstract class JsNameIsNotOnAllAccessors : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JsNameIsNotOnAllAccessors::class
    }

    abstract class JsNameProhibitedForNamedNative : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JsNameProhibitedForNamedNative::class
    }

    abstract class JsNameProhibitedForOverride : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JsNameProhibitedForOverride::class
    }

    abstract class JsNameOnPrimaryConstructorProhibited : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JsNameOnPrimaryConstructorProhibited::class
    }

    abstract class JsNameOnAccessorAndProperty : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JsNameOnAccessorAndProperty::class
    }

    abstract class JsNameProhibitedForExtensionProperty : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JsNameProhibitedForExtensionProperty::class
    }

    abstract class OptInUsage : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OptInUsage::class
        abstract val optInMarkerFqName: FqName
        abstract val message: String
    }

    abstract class OptInUsageError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OptInUsageError::class
        abstract val optInMarkerFqName: FqName
        abstract val message: String
    }

    abstract class OptInOverride : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OptInOverride::class
        abstract val optInMarkerFqName: FqName
        abstract val message: String
    }

    abstract class OptInOverrideError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OptInOverrideError::class
        abstract val optInMarkerFqName: FqName
        abstract val message: String
    }

    abstract class OptInIsNotEnabled : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OptInIsNotEnabled::class
    }

    abstract class OptInCanOnlyBeUsedAsAnnotation : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OptInCanOnlyBeUsedAsAnnotation::class
    }

    abstract class OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn::class
    }

    abstract class OptInWithoutArguments : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OptInWithoutArguments::class
    }

    abstract class OptInArgumentIsNotMarker : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OptInArgumentIsNotMarker::class
        abstract val notMarkerFqName: FqName
    }

    abstract class OptInMarkerWithWrongTarget : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OptInMarkerWithWrongTarget::class
        abstract val target: String
    }

    abstract class OptInMarkerWithWrongRetention : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OptInMarkerWithWrongRetention::class
    }

    abstract class OptInMarkerOnWrongTarget : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OptInMarkerOnWrongTarget::class
        abstract val target: String
    }

    abstract class OptInMarkerOnOverride : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OptInMarkerOnOverride::class
    }

    abstract class OptInMarkerOnOverrideWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OptInMarkerOnOverrideWarning::class
    }

    abstract class SubclassOptInInapplicable : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = SubclassOptInInapplicable::class
        abstract val target: String
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

    abstract class ExposedPropertyTypeInConstructorError : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedPropertyTypeInConstructorError::class
        abstract val elementVisibility: EffectiveVisibility
        abstract val restrictingDeclaration: KtSymbol
        abstract val restrictingVisibility: EffectiveVisibility
    }

    abstract class ExposedPropertyTypeInConstructorWarning : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ExposedPropertyTypeInConstructorWarning::class
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

    abstract class DeprecatedModifier : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedModifier::class
        abstract val deprecatedModifier: KtModifierKeywordToken
        abstract val actualModifier: KtModifierKeywordToken
    }

    abstract class DeprecatedModifierPair : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedModifierPair::class
        abstract val deprecatedModifier: KtModifierKeywordToken
        abstract val conflictingModifier: KtModifierKeywordToken
    }

    abstract class DeprecatedModifierForTarget : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedModifierForTarget::class
        abstract val deprecatedModifier: KtModifierKeywordToken
        abstract val target: String
    }

    abstract class RedundantModifierForTarget : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = RedundantModifierForTarget::class
        abstract val redundantModifier: KtModifierKeywordToken
        abstract val target: String
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

    abstract class OperatorModifierRequired : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OperatorModifierRequired::class
        abstract val functionSymbol: KtFunctionLikeSymbol
        abstract val name: String
    }

    abstract class InfixModifierRequired : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InfixModifierRequired::class
        abstract val functionSymbol: KtFunctionLikeSymbol
    }

    abstract class WrongModifierContainingDeclaration : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = WrongModifierContainingDeclaration::class
        abstract val modifier: KtModifierKeywordToken
        abstract val target: String
    }

    abstract class DeprecatedModifierContainingDeclaration : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DeprecatedModifierContainingDeclaration::class
        abstract val modifier: KtModifierKeywordToken
        abstract val target: String
    }

    abstract class InapplicableOperatorModifier : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InapplicableOperatorModifier::class
        abstract val message: String
    }

    abstract class NoExplicitVisibilityInApiMode : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NoExplicitVisibilityInApiMode::class
    }

    abstract class NoExplicitVisibilityInApiModeWarning : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NoExplicitVisibilityInApiModeWarning::class
    }

    abstract class NoExplicitReturnTypeInApiMode : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NoExplicitReturnTypeInApiMode::class
    }

    abstract class NoExplicitReturnTypeInApiModeWarning : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NoExplicitReturnTypeInApiModeWarning::class
    }

    abstract class ValueClassNotTopLevel : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ValueClassNotTopLevel::class
    }

    abstract class ValueClassNotFinal : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ValueClassNotFinal::class
    }

    abstract class AbsenceOfPrimaryConstructorForValueClass : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = AbsenceOfPrimaryConstructorForValueClass::class
    }

    abstract class InlineClassConstructorWrongParametersSize : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = InlineClassConstructorWrongParametersSize::class
    }

    abstract class ValueClassEmptyConstructor : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ValueClassEmptyConstructor::class
    }

    abstract class ValueClassConstructorNotFinalReadOnlyParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ValueClassConstructorNotFinalReadOnlyParameter::class
    }

    abstract class PropertyWithBackingFieldInsideValueClass : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = PropertyWithBackingFieldInsideValueClass::class
    }

    abstract class DelegatedPropertyInsideValueClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegatedPropertyInsideValueClass::class
    }

    abstract class ValueClassHasInapplicableParameterType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ValueClassHasInapplicableParameterType::class
        abstract val type: KtType
    }

    abstract class ValueClassCannotImplementInterfaceByDelegation : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ValueClassCannotImplementInterfaceByDelegation::class
    }

    abstract class ValueClassCannotExtendClasses : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ValueClassCannotExtendClasses::class
    }

    abstract class ValueClassCannotBeRecursive : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = ValueClassCannotBeRecursive::class
    }

    abstract class MultiFieldValueClassPrimaryConstructorDefaultParameter : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = MultiFieldValueClassPrimaryConstructorDefaultParameter::class
    }

    abstract class SecondaryConstructorWithBodyInsideValueClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SecondaryConstructorWithBodyInsideValueClass::class
    }

    abstract class ReservedMemberInsideValueClass : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = ReservedMemberInsideValueClass::class
        abstract val name: String
    }

    abstract class TypeArgumentOnTypedValueClassEquals : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = TypeArgumentOnTypedValueClassEquals::class
    }

    abstract class InnerClassInsideValueClass : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = InnerClassInsideValueClass::class
    }

    abstract class ValueClassCannotBeCloneable : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ValueClassCannotBeCloneable::class
    }

    abstract class AnnotationOnIllegalMultiFieldValueClassTypedTarget : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = AnnotationOnIllegalMultiFieldValueClassTypedTarget::class
        abstract val name: String
    }

    abstract class NoneApplicable : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NoneApplicable::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class InapplicableCandidate : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InapplicableCandidate::class
        abstract val candidate: KtSymbol
    }

    abstract class TypeMismatch : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
        abstract val isMismatchDueToNullability: Boolean
    }

    abstract class TypeInferenceOnlyInputTypesError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeInferenceOnlyInputTypesError::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class ThrowableTypeMismatch : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ThrowableTypeMismatch::class
        abstract val actualType: KtType
        abstract val isMismatchDueToNullability: Boolean
    }

    abstract class ConditionTypeMismatch : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConditionTypeMismatch::class
        abstract val actualType: KtType
        abstract val isMismatchDueToNullability: Boolean
    }

    abstract class ArgumentTypeMismatch : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ArgumentTypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
        abstract val isMismatchDueToNullability: Boolean
    }

    abstract class NullForNonnullType : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NullForNonnullType::class
    }

    abstract class InapplicableLateinitModifier : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = InapplicableLateinitModifier::class
        abstract val reason: String
    }

    abstract class VarargOutsideParentheses : KtFirDiagnostic<KtElement>() {
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

    abstract class NameForAmbiguousParameter : KtFirDiagnostic<KtValueArgument>() {
        override val diagnosticClass get() = NameForAmbiguousParameter::class
    }

    abstract class AssignmentTypeMismatch : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AssignmentTypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
        abstract val isMismatchDueToNullability: Boolean
    }

    abstract class ResultTypeMismatch : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ResultTypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
    }

    abstract class ManyLambdaExpressionArguments : KtFirDiagnostic<KtValueArgument>() {
        override val diagnosticClass get() = ManyLambdaExpressionArguments::class
    }

    abstract class NewInferenceNoInformationForParameter : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NewInferenceNoInformationForParameter::class
        abstract val name: String
    }

    abstract class SpreadOfNullable : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SpreadOfNullable::class
    }

    abstract class AssigningSingleElementToVarargInNamedFormFunctionError : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormFunctionError::class
        abstract val expectedArrayType: KtType
    }

    abstract class AssigningSingleElementToVarargInNamedFormFunctionWarning : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormFunctionWarning::class
        abstract val expectedArrayType: KtType
    }

    abstract class AssigningSingleElementToVarargInNamedFormAnnotationError : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormAnnotationError::class
    }

    abstract class AssigningSingleElementToVarargInNamedFormAnnotationWarning : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormAnnotationWarning::class
    }

    abstract class RedundantSpreadOperatorInNamedFormInAnnotation : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = RedundantSpreadOperatorInNamedFormInAnnotation::class
    }

    abstract class RedundantSpreadOperatorInNamedFormInFunction : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = RedundantSpreadOperatorInNamedFormInFunction::class
    }

    abstract class InferenceUnsuccessfulFork : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InferenceUnsuccessfulFork::class
        abstract val message: String
    }

    abstract class OverloadResolutionAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OverloadResolutionAmbiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class AssignOperatorAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AssignOperatorAmbiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class IteratorAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IteratorAmbiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class HasNextFunctionAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = HasNextFunctionAmbiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class NextAmbiguity : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NextAmbiguity::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class AmbiguousFunctionTypeKind : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = AmbiguousFunctionTypeKind::class
        abstract val kinds: List<FunctionTypeKind>
    }

    abstract class NoContextReceiver : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NoContextReceiver::class
        abstract val contextReceiverRepresentation: KtType
    }

    abstract class MultipleArgumentsApplicableForContextReceiver : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = MultipleArgumentsApplicableForContextReceiver::class
        abstract val contextReceiverRepresentation: KtType
    }

    abstract class AmbiguousCallWithImplicitContextReceiver : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = AmbiguousCallWithImplicitContextReceiver::class
    }

    abstract class UnsupportedContextualDeclarationCall : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = UnsupportedContextualDeclarationCall::class
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
        abstract val expectedUpperBound: KtType
        abstract val actualUpperBound: KtType
    }

    abstract class UpperBoundViolatedInTypealiasExpansion : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UpperBoundViolatedInTypealiasExpansion::class
        abstract val expectedUpperBound: KtType
        abstract val actualUpperBound: KtType
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

    abstract class OuterClassArgumentsRequired : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OuterClassArgumentsRequired::class
        abstract val outer: KtClassLikeSymbol
    }

    abstract class TypeParametersInObject : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParametersInObject::class
    }

    abstract class TypeParametersInAnonymousObject : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParametersInAnonymousObject::class
    }

    abstract class IllegalProjectionUsage : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalProjectionUsage::class
    }

    abstract class TypeParametersInEnum : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParametersInEnum::class
    }

    abstract class ConflictingProjection : KtFirDiagnostic<KtTypeProjection>() {
        override val diagnosticClass get() = ConflictingProjection::class
        abstract val type: KtType
    }

    abstract class ConflictingProjectionInTypealiasExpansion : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ConflictingProjectionInTypealiasExpansion::class
        abstract val type: KtType
    }

    abstract class RedundantProjection : KtFirDiagnostic<KtTypeProjection>() {
        override val diagnosticClass get() = RedundantProjection::class
        abstract val type: KtType
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

    abstract class GenericThrowableSubclass : KtFirDiagnostic<KtTypeParameter>() {
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

    abstract class TypeParameterAsReifiedArrayError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParameterAsReifiedArrayError::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class TypeParameterAsReifiedArrayWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeParameterAsReifiedArrayWarning::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class ReifiedTypeForbiddenSubstitution : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ReifiedTypeForbiddenSubstitution::class
        abstract val type: KtType
    }

    abstract class FinalUpperBound : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = FinalUpperBound::class
        abstract val type: KtType
    }

    abstract class UpperBoundIsExtensionFunctionType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = UpperBoundIsExtensionFunctionType::class
    }

    abstract class BoundsNotAllowedIfBoundedByTypeParameter : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = BoundsNotAllowedIfBoundedByTypeParameter::class
    }

    abstract class OnlyOneClassBoundAllowed : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = OnlyOneClassBoundAllowed::class
    }

    abstract class RepeatedBound : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = RepeatedBound::class
    }

    abstract class ConflictingUpperBounds : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ConflictingUpperBounds::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class NameInConstraintIsNotATypeParameter : KtFirDiagnostic<KtSimpleNameExpression>() {
        override val diagnosticClass get() = NameInConstraintIsNotATypeParameter::class
        abstract val typeParameterName: Name
        abstract val typeParametersOwner: KtSymbol
    }

    abstract class BoundOnTypeAliasParameterNotAllowed : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = BoundOnTypeAliasParameterNotAllowed::class
    }

    abstract class ReifiedTypeParameterNoInline : KtFirDiagnostic<KtTypeParameter>() {
        override val diagnosticClass get() = ReifiedTypeParameterNoInline::class
    }

    abstract class TypeParametersNotAllowed : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = TypeParametersNotAllowed::class
    }

    abstract class TypeParameterOfPropertyNotUsedInReceiver : KtFirDiagnostic<KtTypeParameter>() {
        override val diagnosticClass get() = TypeParameterOfPropertyNotUsedInReceiver::class
    }

    abstract class ReturnTypeMismatch : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ReturnTypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
        abstract val targetFunction: KtSymbol
        abstract val isMismatchDueToNullability: Boolean
    }

    abstract class ImplicitNothingReturnType : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ImplicitNothingReturnType::class
    }

    abstract class ImplicitNothingPropertyType : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ImplicitNothingPropertyType::class
    }

    abstract class CyclicGenericUpperBound : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = CyclicGenericUpperBound::class
    }

    abstract class DeprecatedTypeParameterSyntax : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = DeprecatedTypeParameterSyntax::class
    }

    abstract class MisplacedTypeParameterConstraints : KtFirDiagnostic<KtTypeParameter>() {
        override val diagnosticClass get() = MisplacedTypeParameterConstraints::class
    }

    abstract class DynamicSupertype : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = DynamicSupertype::class
    }

    abstract class DynamicUpperBound : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = DynamicUpperBound::class
    }

    abstract class DynamicReceiverNotAllowed : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = DynamicReceiverNotAllowed::class
    }

    abstract class IncompatibleTypes : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = IncompatibleTypes::class
        abstract val typeA: KtType
        abstract val typeB: KtType
    }

    abstract class IncompatibleTypesWarning : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = IncompatibleTypesWarning::class
        abstract val typeA: KtType
        abstract val typeB: KtType
    }

    abstract class TypeVarianceConflictError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeVarianceConflictError::class
        abstract val typeParameter: KtTypeParameterSymbol
        abstract val typeParameterVariance: Variance
        abstract val variance: Variance
        abstract val containingType: KtType
    }

    abstract class TypeVarianceConflictInExpandedType : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TypeVarianceConflictInExpandedType::class
        abstract val typeParameter: KtTypeParameterSymbol
        abstract val typeParameterVariance: Variance
        abstract val variance: Variance
        abstract val containingType: KtType
    }

    abstract class SmartcastImpossible : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = SmartcastImpossible::class
        abstract val desiredType: KtType
        abstract val subject: KtExpression
        abstract val description: String
        abstract val isCastToNotNull: Boolean
    }

    abstract class RedundantNullable : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = RedundantNullable::class
    }

    abstract class PlatformClassMappedToKotlin : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = PlatformClassMappedToKotlin::class
        abstract val kotlinClass: FqName
    }

    abstract class InferredTypeVariableIntoEmptyIntersection : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InferredTypeVariableIntoEmptyIntersection::class
        abstract val typeVariableDescription: String
        abstract val incompatibleTypes: List<KtType>
        abstract val description: String
        abstract val causingTypes: String
    }

    abstract class InferredTypeVariableIntoPossibleEmptyIntersection : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InferredTypeVariableIntoPossibleEmptyIntersection::class
        abstract val typeVariableDescription: String
        abstract val incompatibleTypes: List<KtType>
        abstract val description: String
        abstract val causingTypes: String
    }

    abstract class IncorrectLeftComponentOfIntersection : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = IncorrectLeftComponentOfIntersection::class
    }

    abstract class IncorrectRightComponentOfIntersection : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = IncorrectRightComponentOfIntersection::class
    }

    abstract class NullableOnDefinitelyNotNullable : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = NullableOnDefinitelyNotNullable::class
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
        abstract val declaration: KtCallableSymbol
    }

    abstract class CannotOverrideInvisibleMember : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = CannotOverrideInvisibleMember::class
        abstract val overridingMember: KtCallableSymbol
        abstract val baseMember: KtCallableSymbol
    }

    abstract class DataClassOverrideConflict : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = DataClassOverrideConflict::class
        abstract val overridingMember: KtCallableSymbol
        abstract val baseMember: KtCallableSymbol
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

    abstract class ReturnTypeMismatchOnInheritance : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = ReturnTypeMismatchOnInheritance::class
        abstract val conflictingDeclaration1: KtCallableSymbol
        abstract val conflictingDeclaration2: KtCallableSymbol
    }

    abstract class PropertyTypeMismatchOnInheritance : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = PropertyTypeMismatchOnInheritance::class
        abstract val conflictingDeclaration1: KtCallableSymbol
        abstract val conflictingDeclaration2: KtCallableSymbol
    }

    abstract class VarTypeMismatchOnInheritance : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = VarTypeMismatchOnInheritance::class
        abstract val conflictingDeclaration1: KtCallableSymbol
        abstract val conflictingDeclaration2: KtCallableSymbol
    }

    abstract class ReturnTypeMismatchByDelegation : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = ReturnTypeMismatchByDelegation::class
        abstract val delegateDeclaration: KtCallableSymbol
        abstract val baseDeclaration: KtCallableSymbol
    }

    abstract class PropertyTypeMismatchByDelegation : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = PropertyTypeMismatchByDelegation::class
        abstract val delegateDeclaration: KtCallableSymbol
        abstract val baseDeclaration: KtCallableSymbol
    }

    abstract class VarOverriddenByValByDelegation : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = VarOverriddenByValByDelegation::class
        abstract val delegateDeclaration: KtCallableSymbol
        abstract val baseDeclaration: KtCallableSymbol
    }

    abstract class ConflictingInheritedMembers : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = ConflictingInheritedMembers::class
        abstract val owner: KtClassLikeSymbol
        abstract val conflictingDeclarations: List<KtCallableSymbol>
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

    abstract class InvisibleAbstractMemberFromSuperError : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = InvisibleAbstractMemberFromSuperError::class
        abstract val classOrObject: KtClassLikeSymbol
        abstract val invisibleDeclaration: KtCallableSymbol
    }

    abstract class InvisibleAbstractMemberFromSuperWarning : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = InvisibleAbstractMemberFromSuperWarning::class
        abstract val classOrObject: KtClassLikeSymbol
        abstract val invisibleDeclaration: KtCallableSymbol
    }

    abstract class AmbiguousAnonymousTypeInferred : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = AmbiguousAnonymousTypeInferred::class
        abstract val superTypes: List<KtType>
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
        abstract val function: KtCallableSymbol
        abstract val superFunction: KtCallableSymbol
    }

    abstract class PropertyTypeMismatchOnOverride : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = PropertyTypeMismatchOnOverride::class
        abstract val property: KtCallableSymbol
        abstract val superProperty: KtCallableSymbol
    }

    abstract class VarTypeMismatchOnOverride : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = VarTypeMismatchOnOverride::class
        abstract val variable: KtCallableSymbol
        abstract val superVariable: KtCallableSymbol
    }

    abstract class VarOverriddenByVal : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = VarOverriddenByVal::class
        abstract val overridingDeclaration: KtCallableSymbol
        abstract val overriddenDeclaration: KtCallableSymbol
    }

    abstract class NonFinalMemberInFinalClass : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = NonFinalMemberInFinalClass::class
    }

    abstract class NonFinalMemberInObject : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = NonFinalMemberInObject::class
    }

    abstract class VirtualMemberHidden : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = VirtualMemberHidden::class
        abstract val declared: KtCallableSymbol
        abstract val overriddenContainer: KtClassLikeSymbol
    }

    abstract class ManyCompanionObjects : KtFirDiagnostic<KtObjectDeclaration>() {
        override val diagnosticClass get() = ManyCompanionObjects::class
    }

    abstract class ConflictingOverloads : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConflictingOverloads::class
        abstract val conflictingOverloads: List<KtSymbol>
    }

    abstract class Redeclaration : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = Redeclaration::class
        abstract val conflictingDeclarations: List<KtSymbol>
    }

    abstract class PackageOrClassifierRedeclaration : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = PackageOrClassifierRedeclaration::class
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
        abstract val function: KtCallableSymbol
        abstract val containingClass: KtClassLikeSymbol
    }

    abstract class AbstractFunctionWithBody : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = AbstractFunctionWithBody::class
        abstract val function: KtCallableSymbol
    }

    abstract class NonAbstractFunctionWithNoBody : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = NonAbstractFunctionWithNoBody::class
        abstract val function: KtCallableSymbol
    }

    abstract class PrivateFunctionWithNoBody : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = PrivateFunctionWithNoBody::class
        abstract val function: KtCallableSymbol
    }

    abstract class NonMemberFunctionNoBody : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = NonMemberFunctionNoBody::class
        abstract val function: KtCallableSymbol
    }

    abstract class FunctionDeclarationWithNoName : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = FunctionDeclarationWithNoName::class
    }

    abstract class AnonymousFunctionWithName : KtFirDiagnostic<KtFunction>() {
        override val diagnosticClass get() = AnonymousFunctionWithName::class
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

    abstract class CannotInferParameterType : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = CannotInferParameterType::class
    }

    abstract class NoTailCallsFound : KtFirDiagnostic<KtNamedFunction>() {
        override val diagnosticClass get() = NoTailCallsFound::class
    }

    abstract class TailrecOnVirtualMemberError : KtFirDiagnostic<KtNamedFunction>() {
        override val diagnosticClass get() = TailrecOnVirtualMemberError::class
    }

    abstract class NonTailRecursiveCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonTailRecursiveCall::class
    }

    abstract class TailRecursionInTryIsNotSupported : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = TailRecursionInTryIsNotSupported::class
    }

    abstract class DataObjectCustomEqualsOrHashCode : KtFirDiagnostic<KtNamedFunction>() {
        override val diagnosticClass get() = DataObjectCustomEqualsOrHashCode::class
    }

    abstract class FunInterfaceConstructorReference : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = FunInterfaceConstructorReference::class
    }

    abstract class FunInterfaceWrongCountOfAbstractMembers : KtFirDiagnostic<KtClass>() {
        override val diagnosticClass get() = FunInterfaceWrongCountOfAbstractMembers::class
    }

    abstract class FunInterfaceCannotHaveAbstractProperties : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = FunInterfaceCannotHaveAbstractProperties::class
    }

    abstract class FunInterfaceAbstractMethodWithTypeParameters : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = FunInterfaceAbstractMethodWithTypeParameters::class
    }

    abstract class FunInterfaceAbstractMethodWithDefaultValue : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = FunInterfaceAbstractMethodWithDefaultValue::class
    }

    abstract class FunInterfaceWithSuspendFunction : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = FunInterfaceWithSuspendFunction::class
    }

    abstract class AbstractPropertyInNonAbstractClass : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = AbstractPropertyInNonAbstractClass::class
        abstract val property: KtCallableSymbol
        abstract val containingClass: KtClassLikeSymbol
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

    abstract class AbstractDelegatedProperty : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AbstractDelegatedProperty::class
    }

    abstract class DelegatedPropertyInInterface : KtFirDiagnostic<KtExpression>() {
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

    abstract class ValWithSetter : KtFirDiagnostic<KtPropertyAccessor>() {
        override val diagnosticClass get() = ValWithSetter::class
    }

    abstract class ConstValNotTopLevelOrObject : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ConstValNotTopLevelOrObject::class
    }

    abstract class ConstValWithGetter : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ConstValWithGetter::class
    }

    abstract class ConstValWithDelegate : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ConstValWithDelegate::class
    }

    abstract class TypeCantBeUsedForConstVal : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = TypeCantBeUsedForConstVal::class
        abstract val constValType: KtType
    }

    abstract class ConstValWithoutInitializer : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = ConstValWithoutInitializer::class
    }

    abstract class ConstValWithNonConstInitializer : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ConstValWithNonConstInitializer::class
    }

    abstract class WrongSetterParameterType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = WrongSetterParameterType::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
    }

    abstract class DelegateUsesExtensionPropertyTypeParameterError : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = DelegateUsesExtensionPropertyTypeParameterError::class
        abstract val usedTypeParameter: KtTypeParameterSymbol
    }

    abstract class DelegateUsesExtensionPropertyTypeParameterWarning : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = DelegateUsesExtensionPropertyTypeParameterWarning::class
        abstract val usedTypeParameter: KtTypeParameterSymbol
    }

    abstract class InitializerTypeMismatch : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = InitializerTypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
        abstract val isMismatchDueToNullability: Boolean
    }

    abstract class GetterVisibilityDiffersFromPropertyVisibility : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = GetterVisibilityDiffersFromPropertyVisibility::class
    }

    abstract class SetterVisibilityInconsistentWithPropertyVisibility : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = SetterVisibilityInconsistentWithPropertyVisibility::class
    }

    abstract class WrongSetterReturnType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = WrongSetterReturnType::class
    }

    abstract class WrongGetterReturnType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = WrongGetterReturnType::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
    }

    abstract class AccessorForDelegatedProperty : KtFirDiagnostic<KtPropertyAccessor>() {
        override val diagnosticClass get() = AccessorForDelegatedProperty::class
    }

    abstract class PropertyInitializerWithExplicitFieldDeclaration : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = PropertyInitializerWithExplicitFieldDeclaration::class
    }

    abstract class PropertyFieldDeclarationMissingInitializer : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = PropertyFieldDeclarationMissingInitializer::class
    }

    abstract class LateinitPropertyFieldDeclarationWithInitializer : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = LateinitPropertyFieldDeclarationWithInitializer::class
    }

    abstract class LateinitFieldInValProperty : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = LateinitFieldInValProperty::class
    }

    abstract class LateinitNullableBackingField : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = LateinitNullableBackingField::class
    }

    abstract class BackingFieldForDelegatedProperty : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = BackingFieldForDelegatedProperty::class
    }

    abstract class PropertyMustHaveGetter : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = PropertyMustHaveGetter::class
    }

    abstract class PropertyMustHaveSetter : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = PropertyMustHaveSetter::class
    }

    abstract class ExplicitBackingFieldInInterface : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = ExplicitBackingFieldInInterface::class
    }

    abstract class ExplicitBackingFieldInAbstractProperty : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = ExplicitBackingFieldInAbstractProperty::class
    }

    abstract class ExplicitBackingFieldInExtension : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = ExplicitBackingFieldInExtension::class
    }

    abstract class RedundantExplicitBackingField : KtFirDiagnostic<KtBackingField>() {
        override val diagnosticClass get() = RedundantExplicitBackingField::class
    }

    abstract class AbstractPropertyInPrimaryConstructorParameters : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = AbstractPropertyInPrimaryConstructorParameters::class
    }

    abstract class LocalVariableWithTypeParametersWarning : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = LocalVariableWithTypeParametersWarning::class
    }

    abstract class LocalVariableWithTypeParameters : KtFirDiagnostic<KtProperty>() {
        override val diagnosticClass get() = LocalVariableWithTypeParameters::class
    }

    abstract class ExplicitTypeArgumentsInPropertyAccess : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ExplicitTypeArgumentsInPropertyAccess::class
    }

    abstract class LateinitIntrinsicCallOnNonLiteral : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonLiteral::class
    }

    abstract class LateinitIntrinsicCallOnNonLateinit : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonLateinit::class
    }

    abstract class LateinitIntrinsicCallInInlineFunction : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = LateinitIntrinsicCallInInlineFunction::class
    }

    abstract class LateinitIntrinsicCallOnNonAccessibleProperty : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonAccessibleProperty::class
        abstract val declaration: KtSymbol
    }

    abstract class LocalExtensionProperty : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = LocalExtensionProperty::class
    }

    abstract class ExpectedDeclarationWithBody : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ExpectedDeclarationWithBody::class
    }

    abstract class ExpectedClassConstructorDelegationCall : KtFirDiagnostic<KtConstructorDelegationCall>() {
        override val diagnosticClass get() = ExpectedClassConstructorDelegationCall::class
    }

    abstract class ExpectedClassConstructorPropertyParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ExpectedClassConstructorPropertyParameter::class
    }

    abstract class ExpectedEnumConstructor : KtFirDiagnostic<KtConstructor<*>>() {
        override val diagnosticClass get() = ExpectedEnumConstructor::class
    }

    abstract class ExpectedEnumEntryWithBody : KtFirDiagnostic<KtEnumEntry>() {
        override val diagnosticClass get() = ExpectedEnumEntryWithBody::class
    }

    abstract class ExpectedPropertyInitializer : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ExpectedPropertyInitializer::class
    }

    abstract class ExpectedDelegatedProperty : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ExpectedDelegatedProperty::class
    }

    abstract class ExpectedLateinitProperty : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = ExpectedLateinitProperty::class
    }

    abstract class SupertypeInitializedInExpectedClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SupertypeInitializedInExpectedClass::class
    }

    abstract class ExpectedPrivateDeclaration : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = ExpectedPrivateDeclaration::class
    }

    abstract class ImplementationByDelegationInExpectClass : KtFirDiagnostic<KtDelegatedSuperTypeEntry>() {
        override val diagnosticClass get() = ImplementationByDelegationInExpectClass::class
    }

    abstract class ActualTypeAliasNotToClass : KtFirDiagnostic<KtTypeAlias>() {
        override val diagnosticClass get() = ActualTypeAliasNotToClass::class
    }

    abstract class ActualTypeAliasToClassWithDeclarationSiteVariance : KtFirDiagnostic<KtTypeAlias>() {
        override val diagnosticClass get() = ActualTypeAliasToClassWithDeclarationSiteVariance::class
    }

    abstract class ActualTypeAliasWithUseSiteVariance : KtFirDiagnostic<KtTypeAlias>() {
        override val diagnosticClass get() = ActualTypeAliasWithUseSiteVariance::class
    }

    abstract class ActualTypeAliasWithComplexSubstitution : KtFirDiagnostic<KtTypeAlias>() {
        override val diagnosticClass get() = ActualTypeAliasWithComplexSubstitution::class
    }

    abstract class ActualFunctionWithDefaultArguments : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ActualFunctionWithDefaultArguments::class
    }

    abstract class ActualAnnotationConflictingDefaultArgumentValue : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ActualAnnotationConflictingDefaultArgumentValue::class
        abstract val parameter: KtVariableLikeSymbol
    }

    abstract class ExpectedFunctionSourceWithDefaultArgumentsNotFound : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ExpectedFunctionSourceWithDefaultArgumentsNotFound::class
    }

    abstract class NoActualForExpect : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = NoActualForExpect::class
        abstract val declaration: KtSymbol
        abstract val module: FirModuleData
        abstract val compatibility: Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>
    }

    abstract class ActualWithoutExpect : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ActualWithoutExpect::class
        abstract val declaration: KtSymbol
        abstract val compatibility: Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>
    }

    abstract class AmbiguousActuals : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = AmbiguousActuals::class
        abstract val declaration: KtSymbol
        abstract val candidates: List<KtSymbol>
    }

    abstract class AmbiguousExpects : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = AmbiguousExpects::class
        abstract val declaration: KtSymbol
        abstract val modules: List<FirModuleData>
    }

    abstract class NoActualClassMemberForExpectedClass : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = NoActualClassMemberForExpectedClass::class
        abstract val declaration: KtSymbol
        abstract val members: List<Pair<KtSymbol, Map<Incompatible<FirBasedSymbol<*>>, List<KtSymbol>>>>
    }

    abstract class ActualMissing : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = ActualMissing::class
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

    abstract class UninitializedVariable : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = UninitializedVariable::class
        abstract val variable: KtVariableSymbol
    }

    abstract class UninitializedParameter : KtFirDiagnostic<KtSimpleNameExpression>() {
        override val diagnosticClass get() = UninitializedParameter::class
        abstract val parameter: KtSymbol
    }

    abstract class UninitializedEnumEntry : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = UninitializedEnumEntry::class
        abstract val enumEntry: KtSymbol
    }

    abstract class UninitializedEnumCompanion : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = UninitializedEnumCompanion::class
        abstract val enumClass: KtClassLikeSymbol
    }

    abstract class ValReassignment : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ValReassignment::class
        abstract val variable: KtVariableLikeSymbol
    }

    abstract class ValReassignmentViaBackingFieldError : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ValReassignmentViaBackingFieldError::class
        abstract val property: KtVariableSymbol
    }

    abstract class ValReassignmentViaBackingFieldWarning : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ValReassignmentViaBackingFieldWarning::class
        abstract val property: KtVariableSymbol
    }

    abstract class CapturedValInitialization : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = CapturedValInitialization::class
        abstract val property: KtVariableSymbol
    }

    abstract class CapturedMemberValInitialization : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = CapturedMemberValInitialization::class
        abstract val property: KtVariableSymbol
    }

    abstract class SetterProjectedOut : KtFirDiagnostic<KtBinaryExpression>() {
        override val diagnosticClass get() = SetterProjectedOut::class
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

    abstract class VariableWithNoTypeNoInitializer : KtFirDiagnostic<KtVariableDeclaration>() {
        override val diagnosticClass get() = VariableWithNoTypeNoInitializer::class
    }

    abstract class InitializationBeforeDeclaration : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = InitializationBeforeDeclaration::class
        abstract val property: KtSymbol
    }

    abstract class UnreachableCode : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = UnreachableCode::class
        abstract val reachable: List<PsiElement>
        abstract val unreachable: List<PsiElement>
    }

    abstract class SenselessComparison : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = SenselessComparison::class
        abstract val expression: KtExpression
        abstract val compareResult: Boolean
    }

    abstract class SenselessNullInWhen : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = SenselessNullInWhen::class
    }

    abstract class TypecheckerHasRunIntoRecursiveProblem : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = TypecheckerHasRunIntoRecursiveProblem::class
    }

    abstract class UnsafeCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnsafeCall::class
        abstract val receiverType: KtType
        abstract val receiverExpression: KtExpression?
    }

    abstract class UnsafeImplicitInvokeCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnsafeImplicitInvokeCall::class
        abstract val receiverType: KtType
    }

    abstract class UnsafeInfixCall : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = UnsafeInfixCall::class
        abstract val receiverExpression: KtExpression
        abstract val operator: String
        abstract val argumentExpression: KtExpression
    }

    abstract class UnsafeOperatorCall : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = UnsafeOperatorCall::class
        abstract val receiverExpression: KtExpression
        abstract val operator: String
        abstract val argumentExpression: KtExpression
    }

    abstract class IteratorOnNullable : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = IteratorOnNullable::class
    }

    abstract class UnnecessarySafeCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnnecessarySafeCall::class
        abstract val receiverType: KtType
    }

    abstract class SafeCallWillChangeNullability : KtFirDiagnostic<KtSafeQualifiedExpression>() {
        override val diagnosticClass get() = SafeCallWillChangeNullability::class
    }

    abstract class UnexpectedSafeCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnexpectedSafeCall::class
    }

    abstract class UnnecessaryNotNullAssertion : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = UnnecessaryNotNullAssertion::class
        abstract val receiverType: KtType
    }

    abstract class NotNullAssertionOnLambdaExpression : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NotNullAssertionOnLambdaExpression::class
    }

    abstract class NotNullAssertionOnCallableReference : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NotNullAssertionOnCallableReference::class
    }

    abstract class UselessElvis : KtFirDiagnostic<KtBinaryExpression>() {
        override val diagnosticClass get() = UselessElvis::class
        abstract val receiverType: KtType
    }

    abstract class UselessElvisRightIsNull : KtFirDiagnostic<KtBinaryExpression>() {
        override val diagnosticClass get() = UselessElvisRightIsNull::class
    }

    abstract class CannotCheckForErased : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = CannotCheckForErased::class
        abstract val type: KtType
    }

    abstract class CastNeverSucceeds : KtFirDiagnostic<KtBinaryExpressionWithTypeRHS>() {
        override val diagnosticClass get() = CastNeverSucceeds::class
    }

    abstract class UselessCast : KtFirDiagnostic<KtBinaryExpressionWithTypeRHS>() {
        override val diagnosticClass get() = UselessCast::class
    }

    abstract class UncheckedCast : KtFirDiagnostic<KtBinaryExpressionWithTypeRHS>() {
        override val diagnosticClass get() = UncheckedCast::class
        abstract val originalType: KtType
        abstract val targetType: KtType
    }

    abstract class UselessIsCheck : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = UselessIsCheck::class
        abstract val compileTimeCheckResult: Boolean
    }

    abstract class IsEnumEntry : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = IsEnumEntry::class
    }

    abstract class EnumEntryAsType : KtFirDiagnostic<KtTypeReference>() {
        override val diagnosticClass get() = EnumEntryAsType::class
    }

    abstract class ExpectedCondition : KtFirDiagnostic<KtWhenCondition>() {
        override val diagnosticClass get() = ExpectedCondition::class
    }

    abstract class NoElseInWhen : KtFirDiagnostic<KtWhenExpression>() {
        override val diagnosticClass get() = NoElseInWhen::class
        abstract val missingWhenCases: List<WhenMissingCase>
    }

    abstract class NonExhaustiveWhenStatement : KtFirDiagnostic<KtWhenExpression>() {
        override val diagnosticClass get() = NonExhaustiveWhenStatement::class
        abstract val type: String
        abstract val missingWhenCases: List<WhenMissingCase>
    }

    abstract class InvalidIfAsExpression : KtFirDiagnostic<KtIfExpression>() {
        override val diagnosticClass get() = InvalidIfAsExpression::class
    }

    abstract class ElseMisplacedInWhen : KtFirDiagnostic<KtWhenEntry>() {
        override val diagnosticClass get() = ElseMisplacedInWhen::class
    }

    abstract class IllegalDeclarationInWhenSubject : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = IllegalDeclarationInWhenSubject::class
        abstract val illegalReason: String
    }

    abstract class CommaInWhenConditionWithoutArgument : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = CommaInWhenConditionWithoutArgument::class
    }

    abstract class DuplicateLabelInWhen : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = DuplicateLabelInWhen::class
    }

    abstract class ConfusingBranchConditionError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConfusingBranchConditionError::class
    }

    abstract class ConfusingBranchConditionWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConfusingBranchConditionWarning::class
    }

    abstract class TypeParameterIsNotAnExpression : KtFirDiagnostic<KtSimpleNameExpression>() {
        override val diagnosticClass get() = TypeParameterIsNotAnExpression::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class TypeParameterOnLhsOfDot : KtFirDiagnostic<KtSimpleNameExpression>() {
        override val diagnosticClass get() = TypeParameterOnLhsOfDot::class
        abstract val typeParameter: KtTypeParameterSymbol
    }

    abstract class NoCompanionObject : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NoCompanionObject::class
        abstract val klass: KtClassLikeSymbol
    }

    abstract class ExpressionExpectedPackageFound : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = ExpressionExpectedPackageFound::class
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

    abstract class IteratorMissing : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = IteratorMissing::class
    }

    abstract class HasNextMissing : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = HasNextMissing::class
    }

    abstract class NextMissing : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NextMissing::class
    }

    abstract class HasNextFunctionNoneApplicable : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = HasNextFunctionNoneApplicable::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class NextNoneApplicable : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NextNoneApplicable::class
        abstract val candidates: List<KtSymbol>
    }

    abstract class DelegateSpecialFunctionMissing : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = DelegateSpecialFunctionMissing::class
        abstract val expectedFunctionSignature: String
        abstract val delegateType: KtType
        abstract val description: String
    }

    abstract class DelegateSpecialFunctionAmbiguity : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = DelegateSpecialFunctionAmbiguity::class
        abstract val expectedFunctionSignature: String
        abstract val candidates: List<KtSymbol>
    }

    abstract class DelegateSpecialFunctionNoneApplicable : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = DelegateSpecialFunctionNoneApplicable::class
        abstract val expectedFunctionSignature: String
        abstract val candidates: List<KtSymbol>
    }

    abstract class DelegateSpecialFunctionReturnTypeMismatch : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = DelegateSpecialFunctionReturnTypeMismatch::class
        abstract val delegateFunction: String
        abstract val expectedType: KtType
        abstract val actualType: KtType
    }

    abstract class UnderscoreIsReserved : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnderscoreIsReserved::class
    }

    abstract class UnderscoreUsageWithoutBackticks : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UnderscoreUsageWithoutBackticks::class
    }

    abstract class ResolvedToUnderscoreNamedCatchParameter : KtFirDiagnostic<KtNameReferenceExpression>() {
        override val diagnosticClass get() = ResolvedToUnderscoreNamedCatchParameter::class
    }

    abstract class InvalidCharacters : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = InvalidCharacters::class
        abstract val message: String
    }

    abstract class DangerousCharacters : KtFirDiagnostic<KtNamedDeclaration>() {
        override val diagnosticClass get() = DangerousCharacters::class
        abstract val characters: String
    }

    abstract class EqualityNotApplicable : KtFirDiagnostic<KtBinaryExpression>() {
        override val diagnosticClass get() = EqualityNotApplicable::class
        abstract val operator: String
        abstract val leftType: KtType
        abstract val rightType: KtType
    }

    abstract class EqualityNotApplicableWarning : KtFirDiagnostic<KtBinaryExpression>() {
        override val diagnosticClass get() = EqualityNotApplicableWarning::class
        abstract val operator: String
        abstract val leftType: KtType
        abstract val rightType: KtType
    }

    abstract class IncompatibleEnumComparisonError : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = IncompatibleEnumComparisonError::class
        abstract val leftType: KtType
        abstract val rightType: KtType
    }

    abstract class IncDecShouldNotReturnUnit : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = IncDecShouldNotReturnUnit::class
    }

    abstract class AssignmentOperatorShouldReturnUnit : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = AssignmentOperatorShouldReturnUnit::class
        abstract val functionSymbol: KtFunctionLikeSymbol
        abstract val operator: String
    }

    abstract class PropertyAsOperator : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = PropertyAsOperator::class
        abstract val property: KtVariableSymbol
    }

    abstract class DslScopeViolation : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DslScopeViolation::class
        abstract val calleeSymbol: KtSymbol
    }

    abstract class ToplevelTypealiasesOnly : KtFirDiagnostic<KtTypeAlias>() {
        override val diagnosticClass get() = ToplevelTypealiasesOnly::class
    }

    abstract class RecursiveTypealiasExpansion : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = RecursiveTypealiasExpansion::class
    }

    abstract class TypealiasShouldExpandToClass : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = TypealiasShouldExpandToClass::class
        abstract val expandedType: KtType
    }

    abstract class RedundantVisibilityModifier : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = RedundantVisibilityModifier::class
    }

    abstract class RedundantModalityModifier : KtFirDiagnostic<KtModifierListOwner>() {
        override val diagnosticClass get() = RedundantModalityModifier::class
    }

    abstract class RedundantReturnUnitType : KtFirDiagnostic<KtTypeReference>() {
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

    abstract class NotAFunctionLabel : KtFirDiagnostic<KtReturnExpression>() {
        override val diagnosticClass get() = NotAFunctionLabel::class
    }

    abstract class ReturnInFunctionWithExpressionBody : KtFirDiagnostic<KtReturnExpression>() {
        override val diagnosticClass get() = ReturnInFunctionWithExpressionBody::class
    }

    abstract class NoReturnInFunctionWithBlockBody : KtFirDiagnostic<KtDeclarationWithBody>() {
        override val diagnosticClass get() = NoReturnInFunctionWithBlockBody::class
    }

    abstract class AnonymousInitializerInInterface : KtFirDiagnostic<KtAnonymousInitializer>() {
        override val diagnosticClass get() = AnonymousInitializerInInterface::class
    }

    abstract class UsageIsNotInlinable : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = UsageIsNotInlinable::class
        abstract val parameter: KtSymbol
    }

    abstract class NonLocalReturnNotAllowed : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NonLocalReturnNotAllowed::class
        abstract val parameter: KtSymbol
    }

    abstract class NotYetSupportedInInline : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NotYetSupportedInInline::class
        abstract val message: String
    }

    abstract class NothingToInline : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NothingToInline::class
    }

    abstract class NullableInlineParameter : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NullableInlineParameter::class
        abstract val parameter: KtSymbol
        abstract val function: KtSymbol
    }

    abstract class RecursionInInline : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = RecursionInInline::class
        abstract val symbol: KtSymbol
    }

    abstract class NonPublicCallFromPublicInline : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NonPublicCallFromPublicInline::class
        abstract val inlineDeclaration: KtSymbol
        abstract val referencedDeclaration: KtSymbol
    }

    abstract class ProtectedConstructorCallFromPublicInline : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ProtectedConstructorCallFromPublicInline::class
        abstract val inlineDeclaration: KtSymbol
        abstract val referencedDeclaration: KtSymbol
    }

    abstract class ProtectedCallFromPublicInlineError : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ProtectedCallFromPublicInlineError::class
        abstract val inlineDeclaration: KtSymbol
        abstract val referencedDeclaration: KtSymbol
    }

    abstract class ProtectedCallFromPublicInline : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ProtectedCallFromPublicInline::class
        abstract val inlineDeclaration: KtSymbol
        abstract val referencedDeclaration: KtSymbol
    }

    abstract class PrivateClassMemberFromInline : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = PrivateClassMemberFromInline::class
        abstract val inlineDeclaration: KtSymbol
        abstract val referencedDeclaration: KtSymbol
    }

    abstract class SuperCallFromPublicInline : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = SuperCallFromPublicInline::class
        abstract val symbol: KtSymbol
    }

    abstract class DeclarationCantBeInlined : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = DeclarationCantBeInlined::class
    }

    abstract class OverrideByInline : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = OverrideByInline::class
    }

    abstract class NonInternalPublishedApi : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NonInternalPublishedApi::class
    }

    abstract class InvalidDefaultFunctionalParameterForInline : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = InvalidDefaultFunctionalParameterForInline::class
        abstract val defaultValue: KtExpression
        abstract val parameter: KtSymbol
    }

    abstract class ReifiedTypeParameterInOverride : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ReifiedTypeParameterInOverride::class
    }

    abstract class InlinePropertyWithBackingField : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = InlinePropertyWithBackingField::class
    }

    abstract class IllegalInlineParameterModifier : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = IllegalInlineParameterModifier::class
    }

    abstract class InlineSuspendFunctionTypeUnsupported : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = InlineSuspendFunctionTypeUnsupported::class
    }

    abstract class RedundantInlineSuspendFunctionType : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = RedundantInlineSuspendFunctionType::class
    }

    abstract class InefficientEqualsOverridingInValueClass : KtFirDiagnostic<KtNamedFunction>() {
        override val diagnosticClass get() = InefficientEqualsOverridingInValueClass::class
        abstract val type: KtType
    }

    abstract class CannotAllUnderImportFromSingleton : KtFirDiagnostic<KtImportDirective>() {
        override val diagnosticClass get() = CannotAllUnderImportFromSingleton::class
        abstract val objectName: Name
    }

    abstract class PackageCannotBeImported : KtFirDiagnostic<KtImportDirective>() {
        override val diagnosticClass get() = PackageCannotBeImported::class
    }

    abstract class CannotBeImported : KtFirDiagnostic<KtImportDirective>() {
        override val diagnosticClass get() = CannotBeImported::class
        abstract val name: Name
    }

    abstract class ConflictingImport : KtFirDiagnostic<KtImportDirective>() {
        override val diagnosticClass get() = ConflictingImport::class
        abstract val name: Name
    }

    abstract class OperatorRenamedOnImport : KtFirDiagnostic<KtImportDirective>() {
        override val diagnosticClass get() = OperatorRenamedOnImport::class
    }

    abstract class IllegalSuspendFunctionCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalSuspendFunctionCall::class
        abstract val suspendCallable: KtSymbol
    }

    abstract class IllegalSuspendPropertyAccess : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalSuspendPropertyAccess::class
        abstract val suspendCallable: KtSymbol
    }

    abstract class NonLocalSuspensionPoint : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonLocalSuspensionPoint::class
    }

    abstract class IllegalRestrictedSuspendingFunctionCall : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalRestrictedSuspendingFunctionCall::class
    }

    abstract class NonModifierFormForBuiltInSuspend : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonModifierFormForBuiltInSuspend::class
    }

    abstract class ModifierFormForNonBuiltInSuspend : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspend::class
    }

    abstract class ModifierFormForNonBuiltInSuspendFunError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspendFunError::class
    }

    abstract class ModifierFormForNonBuiltInSuspendFunWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspendFunWarning::class
    }

    abstract class ReturnForBuiltInSuspend : KtFirDiagnostic<KtReturnExpression>() {
        override val diagnosticClass get() = ReturnForBuiltInSuspend::class
    }

    abstract class RedundantLabelWarning : KtFirDiagnostic<KtLabelReferenceExpression>() {
        override val diagnosticClass get() = RedundantLabelWarning::class
    }

    abstract class ConflictingJvmDeclarations : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConflictingJvmDeclarations::class
    }

    abstract class OverrideCannotBeStatic : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = OverrideCannotBeStatic::class
    }

    abstract class JvmStaticNotInObjectOrClassCompanion : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmStaticNotInObjectOrClassCompanion::class
    }

    abstract class JvmStaticNotInObjectOrCompanion : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmStaticNotInObjectOrCompanion::class
    }

    abstract class JvmStaticOnNonPublicMember : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmStaticOnNonPublicMember::class
    }

    abstract class JvmStaticOnConstOrJvmField : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmStaticOnConstOrJvmField::class
    }

    abstract class JvmStaticOnExternalInInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmStaticOnExternalInInterface::class
    }

    abstract class InapplicableJvmName : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InapplicableJvmName::class
    }

    abstract class IllegalJvmName : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalJvmName::class
    }

    abstract class FunctionDelegateMemberNameClash : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = FunctionDelegateMemberNameClash::class
    }

    abstract class ValueClassWithoutJvmInlineAnnotation : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ValueClassWithoutJvmInlineAnnotation::class
    }

    abstract class JvmInlineWithoutValueClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmInlineWithoutValueClass::class
    }

    abstract class JavaTypeMismatch : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = JavaTypeMismatch::class
        abstract val expectedType: KtType
        abstract val actualType: KtType
    }

    abstract class UpperBoundCannotBeArray : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = UpperBoundCannotBeArray::class
    }

    abstract class StrictfpOnClass : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = StrictfpOnClass::class
    }

    abstract class VolatileOnValue : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = VolatileOnValue::class
    }

    abstract class VolatileOnDelegate : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = VolatileOnDelegate::class
    }

    abstract class SynchronizedOnAbstract : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = SynchronizedOnAbstract::class
    }

    abstract class SynchronizedInInterface : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = SynchronizedInInterface::class
    }

    abstract class SynchronizedOnInline : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = SynchronizedOnInline::class
    }

    abstract class SynchronizedOnSuspendError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = SynchronizedOnSuspendError::class
    }

    abstract class SynchronizedOnSuspendWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = SynchronizedOnSuspendWarning::class
    }

    abstract class OverloadsWithoutDefaultArguments : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OverloadsWithoutDefaultArguments::class
    }

    abstract class OverloadsAbstract : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OverloadsAbstract::class
    }

    abstract class OverloadsInterface : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OverloadsInterface::class
    }

    abstract class OverloadsLocal : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OverloadsLocal::class
    }

    abstract class OverloadsAnnotationClassConstructorError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OverloadsAnnotationClassConstructorError::class
    }

    abstract class OverloadsAnnotationClassConstructorWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OverloadsAnnotationClassConstructorWarning::class
    }

    abstract class OverloadsPrivate : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = OverloadsPrivate::class
    }

    abstract class DeprecatedJavaAnnotation : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = DeprecatedJavaAnnotation::class
        abstract val kotlinName: FqName
    }

    abstract class JvmPackageNameCannotBeEmpty : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = JvmPackageNameCannotBeEmpty::class
    }

    abstract class JvmPackageNameMustBeValidName : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = JvmPackageNameMustBeValidName::class
    }

    abstract class JvmPackageNameNotSupportedInFilesWithClasses : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = JvmPackageNameNotSupportedInFilesWithClasses::class
    }

    abstract class PositionedValueArgumentForJavaAnnotation : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = PositionedValueArgumentForJavaAnnotation::class
    }

    abstract class RedundantRepeatableAnnotation : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RedundantRepeatableAnnotation::class
        abstract val kotlinRepeatable: FqName
        abstract val javaRepeatable: FqName
    }

    abstract class LocalJvmRecord : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = LocalJvmRecord::class
    }

    abstract class NonFinalJvmRecord : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonFinalJvmRecord::class
    }

    abstract class EnumJvmRecord : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = EnumJvmRecord::class
    }

    abstract class JvmRecordWithoutPrimaryConstructorParameters : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmRecordWithoutPrimaryConstructorParameters::class
    }

    abstract class NonDataClassJvmRecord : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = NonDataClassJvmRecord::class
    }

    abstract class JvmRecordNotValParameter : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmRecordNotValParameter::class
    }

    abstract class JvmRecordNotLastVarargParameter : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmRecordNotLastVarargParameter::class
    }

    abstract class InnerJvmRecord : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InnerJvmRecord::class
    }

    abstract class FieldInJvmRecord : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = FieldInJvmRecord::class
    }

    abstract class DelegationByInJvmRecord : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DelegationByInJvmRecord::class
    }

    abstract class JvmRecordExtendsClass : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmRecordExtendsClass::class
        abstract val superType: KtType
    }

    abstract class IllegalJavaLangRecordSupertype : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = IllegalJavaLangRecordSupertype::class
    }

    abstract class JvmDefaultNotInInterface : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmDefaultNotInInterface::class
    }

    abstract class JvmDefaultInJvm6Target : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JvmDefaultInJvm6Target::class
        abstract val annotation: String
    }

    abstract class JvmDefaultRequiredForOverride : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = JvmDefaultRequiredForOverride::class
    }

    abstract class JvmDefaultInDeclaration : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JvmDefaultInDeclaration::class
        abstract val annotation: String
    }

    abstract class JvmDefaultWithCompatibilityInDeclaration : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JvmDefaultWithCompatibilityInDeclaration::class
    }

    abstract class JvmDefaultWithCompatibilityNotOnInterface : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = JvmDefaultWithCompatibilityNotOnInterface::class
    }

    abstract class NonJvmDefaultOverridesJavaDefault : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = NonJvmDefaultOverridesJavaDefault::class
    }

    abstract class ExternalDeclarationCannotBeAbstract : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ExternalDeclarationCannotBeAbstract::class
    }

    abstract class ExternalDeclarationCannotHaveBody : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ExternalDeclarationCannotHaveBody::class
    }

    abstract class ExternalDeclarationInInterface : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ExternalDeclarationInInterface::class
    }

    abstract class ExternalDeclarationCannotBeInlined : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = ExternalDeclarationCannotBeInlined::class
    }

    abstract class NonSourceRepeatedAnnotation : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = NonSourceRepeatedAnnotation::class
    }

    abstract class RepeatedAnnotationTarget6 : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatedAnnotationTarget6::class
    }

    abstract class RepeatedAnnotationWithContainer : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatedAnnotationWithContainer::class
        abstract val name: ClassId
        abstract val explicitContainerName: ClassId
    }

    abstract class RepeatableContainerMustHaveValueArrayError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableContainerMustHaveValueArrayError::class
        abstract val container: ClassId
        abstract val annotation: ClassId
    }

    abstract class RepeatableContainerMustHaveValueArrayWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableContainerMustHaveValueArrayWarning::class
        abstract val container: ClassId
        abstract val annotation: ClassId
    }

    abstract class RepeatableContainerHasNonDefaultParameterError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableContainerHasNonDefaultParameterError::class
        abstract val container: ClassId
        abstract val nonDefault: Name
    }

    abstract class RepeatableContainerHasNonDefaultParameterWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableContainerHasNonDefaultParameterWarning::class
        abstract val container: ClassId
        abstract val nonDefault: Name
    }

    abstract class RepeatableContainerHasShorterRetentionError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableContainerHasShorterRetentionError::class
        abstract val container: ClassId
        abstract val retention: String
        abstract val annotation: ClassId
        abstract val annotationRetention: String
    }

    abstract class RepeatableContainerHasShorterRetentionWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableContainerHasShorterRetentionWarning::class
        abstract val container: ClassId
        abstract val retention: String
        abstract val annotation: ClassId
        abstract val annotationRetention: String
    }

    abstract class RepeatableContainerTargetSetNotASubsetError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableContainerTargetSetNotASubsetError::class
        abstract val container: ClassId
        abstract val annotation: ClassId
    }

    abstract class RepeatableContainerTargetSetNotASubsetWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableContainerTargetSetNotASubsetWarning::class
        abstract val container: ClassId
        abstract val annotation: ClassId
    }

    abstract class RepeatableAnnotationHasNestedClassNamedContainerError : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableAnnotationHasNestedClassNamedContainerError::class
    }

    abstract class RepeatableAnnotationHasNestedClassNamedContainerWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = RepeatableAnnotationHasNestedClassNamedContainerWarning::class
    }

    abstract class SuspensionPointInsideCriticalSection : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SuspensionPointInsideCriticalSection::class
        abstract val function: KtCallableSymbol
    }

    abstract class InapplicableJvmField : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = InapplicableJvmField::class
        abstract val message: String
    }

    abstract class InapplicableJvmFieldWarning : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = InapplicableJvmFieldWarning::class
        abstract val message: String
    }

    abstract class JvmSyntheticOnDelegate : KtFirDiagnostic<KtAnnotationEntry>() {
        override val diagnosticClass get() = JvmSyntheticOnDelegate::class
    }

    abstract class DefaultMethodCallFromJava6TargetError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DefaultMethodCallFromJava6TargetError::class
    }

    abstract class DefaultMethodCallFromJava6TargetWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = DefaultMethodCallFromJava6TargetWarning::class
    }

    abstract class InterfaceStaticMethodCallFromJava6TargetError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InterfaceStaticMethodCallFromJava6TargetError::class
    }

    abstract class InterfaceStaticMethodCallFromJava6TargetWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = InterfaceStaticMethodCallFromJava6TargetWarning::class
    }

    abstract class SubclassCantCallCompanionProtectedNonStatic : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SubclassCantCallCompanionProtectedNonStatic::class
    }

    abstract class ConcurrentHashMapContainsOperatorError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConcurrentHashMapContainsOperatorError::class
    }

    abstract class ConcurrentHashMapContainsOperatorWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = ConcurrentHashMapContainsOperatorWarning::class
    }

    abstract class SpreadOnSignaturePolymorphicCallError : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SpreadOnSignaturePolymorphicCallError::class
    }

    abstract class SpreadOnSignaturePolymorphicCallWarning : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = SpreadOnSignaturePolymorphicCallWarning::class
    }

    abstract class JavaSamInterfaceConstructorReference : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JavaSamInterfaceConstructorReference::class
    }

    abstract class JavaShadowedProtectedFieldReference : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = JavaShadowedProtectedFieldReference::class
        abstract val containerClass: ClassId
        abstract val shadowingClass: ClassId
    }

    abstract class ImplementingFunctionInterface : KtFirDiagnostic<KtClassOrObject>() {
        override val diagnosticClass get() = ImplementingFunctionInterface::class
    }

    abstract class OverridingExternalFunWithOptionalParams : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = OverridingExternalFunWithOptionalParams::class
    }

    abstract class OverridingExternalFunWithOptionalParamsWithFake : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = OverridingExternalFunWithOptionalParamsWithFake::class
        abstract val function: KtFunctionLikeSymbol
    }

    abstract class CallToDefinedExternallyFromNonExternalDeclaration : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = CallToDefinedExternallyFromNonExternalDeclaration::class
    }

    abstract class ExternalClassConstructorPropertyParameter : KtFirDiagnostic<KtParameter>() {
        override val diagnosticClass get() = ExternalClassConstructorPropertyParameter::class
    }

    abstract class ExternalEnumEntryWithBody : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ExternalEnumEntryWithBody::class
    }

    abstract class ExternalAnonymousInitializer : KtFirDiagnostic<KtAnonymousInitializer>() {
        override val diagnosticClass get() = ExternalAnonymousInitializer::class
    }

    abstract class ExternalDelegation : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ExternalDelegation::class
    }

    abstract class ExternalDelegatedConstructorCall : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ExternalDelegatedConstructorCall::class
    }

    abstract class WrongBodyOfExternalDeclaration : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = WrongBodyOfExternalDeclaration::class
    }

    abstract class WrongInitializerOfExternalDeclaration : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = WrongInitializerOfExternalDeclaration::class
    }

    abstract class WrongDefaultValueForExternalFunParameter : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = WrongDefaultValueForExternalFunParameter::class
    }

    abstract class NestedExternalDeclaration : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NestedExternalDeclaration::class
    }

    abstract class WrongExternalDeclaration : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = WrongExternalDeclaration::class
        abstract val classKind: String
    }

    abstract class NestedClassInExternalInterface : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NestedClassInExternalInterface::class
    }

    abstract class ExternalTypeExtendsNonExternalType : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ExternalTypeExtendsNonExternalType::class
    }

    abstract class InlineExternalDeclaration : KtFirDiagnostic<KtDeclaration>() {
        override val diagnosticClass get() = InlineExternalDeclaration::class
    }

    abstract class InlineClassInExternalDeclarationWarning : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = InlineClassInExternalDeclarationWarning::class
    }

    abstract class InlineClassInExternalDeclaration : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = InlineClassInExternalDeclaration::class
    }

    abstract class ExtensionFunctionInExternalDeclaration : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = ExtensionFunctionInExternalDeclaration::class
    }

    abstract class NonAbstractMemberOfExternalInterface : KtFirDiagnostic<KtExpression>() {
        override val diagnosticClass get() = NonAbstractMemberOfExternalInterface::class
    }

    abstract class NonExternalDeclarationInInappropriateFile : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = NonExternalDeclarationInInappropriateFile::class
        abstract val type: KtType
    }

    abstract class DelegationByDynamic : KtFirDiagnostic<KtElement>() {
        override val diagnosticClass get() = DelegationByDynamic::class
    }

    abstract class Syntax : KtFirDiagnostic<PsiElement>() {
        override val diagnosticClass get() = Syntax::class
    }

}
