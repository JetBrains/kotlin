/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
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
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.MismatchOrIncompatible
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed interface KtFirDiagnostic<PSI : PsiElement> : KtDiagnosticWithPsi<PSI> {
    interface Unsupported : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = Unsupported::class
        val unsupported: String
    }

    interface UnsupportedFeature : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedFeature::class
        val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>
    }

    interface UnsupportedSuspendTest : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedSuspendTest::class
    }

    interface NewInferenceError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NewInferenceError::class
        val error: String
    }

    interface OtherError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OtherError::class
    }

    interface IllegalConstExpression : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalConstExpression::class
    }

    interface IllegalUnderscore : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalUnderscore::class
    }

    interface ExpressionExpected : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpressionExpected::class
    }

    interface AssignmentInExpressionContext : KtFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = AssignmentInExpressionContext::class
    }

    interface BreakOrContinueOutsideALoop : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = BreakOrContinueOutsideALoop::class
    }

    interface NotALoopLabel : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotALoopLabel::class
    }

    interface BreakOrContinueJumpsAcrossFunctionBoundary : KtFirDiagnostic<KtExpressionWithLabel> {
        override val diagnosticClass get() = BreakOrContinueJumpsAcrossFunctionBoundary::class
    }

    interface VariableExpected : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VariableExpected::class
    }

    interface DelegationInInterface : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegationInInterface::class
    }

    interface DelegationNotToInterface : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegationNotToInterface::class
    }

    interface NestedClassNotAllowed : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NestedClassNotAllowed::class
        val declaration: String
    }

    interface IncorrectCharacterLiteral : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IncorrectCharacterLiteral::class
    }

    interface EmptyCharacterLiteral : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EmptyCharacterLiteral::class
    }

    interface TooManyCharactersInCharacterLiteral : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TooManyCharactersInCharacterLiteral::class
    }

    interface IllegalEscape : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalEscape::class
    }

    interface IntLiteralOutOfRange : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IntLiteralOutOfRange::class
    }

    interface FloatLiteralOutOfRange : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FloatLiteralOutOfRange::class
    }

    interface WrongLongSuffix : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongLongSuffix::class
    }

    interface UnsignedLiteralWithoutDeclarationsOnClasspath : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnsignedLiteralWithoutDeclarationsOnClasspath::class
    }

    interface DivisionByZero : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DivisionByZero::class
    }

    interface ValOrVarOnLoopParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValOrVarOnLoopParameter::class
        val valOrVar: KtKeywordToken
    }

    interface ValOrVarOnFunParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValOrVarOnFunParameter::class
        val valOrVar: KtKeywordToken
    }

    interface ValOrVarOnCatchParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValOrVarOnCatchParameter::class
        val valOrVar: KtKeywordToken
    }

    interface ValOrVarOnSecondaryConstructorParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValOrVarOnSecondaryConstructorParameter::class
        val valOrVar: KtKeywordToken
    }

    interface InvisibleSetter : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvisibleSetter::class
        val property: KtVariableSymbol
        val visibility: Visibility
        val callableId: CallableId
    }

    interface InnerOnTopLevelScriptClassError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InnerOnTopLevelScriptClassError::class
    }

    interface InnerOnTopLevelScriptClassWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InnerOnTopLevelScriptClassWarning::class
    }

    interface ErrorSuppression : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ErrorSuppression::class
        val diagnosticName: String
    }

    interface MissingConstructorKeyword : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingConstructorKeyword::class
    }

    interface InvisibleReference : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvisibleReference::class
        val reference: KtSymbol
        val visible: Visibility
        val containingDeclaration: ClassId?
    }

    interface UnresolvedReference : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedReference::class
        val reference: String
        val operator: String?
    }

    interface UnresolvedLabel : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedLabel::class
    }

    interface DeserializationError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeserializationError::class
    }

    interface ErrorFromJavaResolution : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ErrorFromJavaResolution::class
    }

    interface MissingStdlibClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingStdlibClass::class
    }

    interface NoThis : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoThis::class
    }

    interface DeprecationError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecationError::class
        val reference: KtSymbol
        val message: String
    }

    interface Deprecation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = Deprecation::class
        val reference: KtSymbol
        val message: String
    }

    interface VersionRequirementDeprecationError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VersionRequirementDeprecationError::class
        val reference: KtSymbol
        val version: Version
        val currentVersion: String
        val message: String
    }

    interface VersionRequirementDeprecation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VersionRequirementDeprecation::class
        val reference: KtSymbol
        val version: Version
        val currentVersion: String
        val message: String
    }

    interface TypealiasExpansionDeprecationError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypealiasExpansionDeprecationError::class
        val alias: KtSymbol
        val reference: KtSymbol
        val message: String
    }

    interface TypealiasExpansionDeprecation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypealiasExpansionDeprecation::class
        val alias: KtSymbol
        val reference: KtSymbol
        val message: String
    }

    interface ApiNotAvailable : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ApiNotAvailable::class
        val sinceKotlinVersion: ApiVersion
        val currentVersion: ApiVersion
    }

    interface UnresolvedReferenceWrongReceiver : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedReferenceWrongReceiver::class
        val candidates: List<KtSymbol>
    }

    interface UnresolvedImport : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedImport::class
        val reference: String
    }

    interface DuplicateParameterNameInFunctionType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DuplicateParameterNameInFunctionType::class
    }

    interface MissingDependencyClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencyClass::class
        val type: KtType
    }

    interface MissingDependencySuperclass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencySuperclass::class
        val missingType: KtType
        val declarationType: KtType
    }

    interface MissingDependencyClassInLambdaParameter : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencyClassInLambdaParameter::class
        val type: KtType
    }

    interface CreatingAnInstanceOfAbstractClass : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CreatingAnInstanceOfAbstractClass::class
    }

    interface NoConstructor : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoConstructor::class
    }

    interface FunctionCallExpected : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FunctionCallExpected::class
        val functionName: String
        val hasValueParameters: Boolean
    }

    interface IllegalSelector : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalSelector::class
    }

    interface NoReceiverAllowed : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoReceiverAllowed::class
    }

    interface FunctionExpected : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FunctionExpected::class
        val expression: String
        val type: KtType
    }

    interface InterfaceAsFunction : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InterfaceAsFunction::class
        val classSymbol: KtClassLikeSymbol
    }

    interface ExpectClassAsFunction : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpectClassAsFunction::class
        val classSymbol: KtClassLikeSymbol
    }

    interface InnerClassConstructorNoReceiver : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InnerClassConstructorNoReceiver::class
        val classSymbol: KtClassLikeSymbol
    }

    interface ResolutionToClassifier : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ResolutionToClassifier::class
        val classSymbol: KtClassLikeSymbol
    }

    interface AmbiguousAlteredAssign : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousAlteredAssign::class
        val altererNames: List<String?>
    }

    interface ForbiddenBinaryMod : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ForbiddenBinaryMod::class
        val forbiddenFunction: KtSymbol
        val suggestedFunction: String
    }

    interface DeprecatedBinaryMod : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedBinaryMod::class
        val forbiddenFunction: KtSymbol
        val suggestedFunction: String
    }

    interface SuperIsNotAnExpression : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuperIsNotAnExpression::class
    }

    interface SuperNotAvailable : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuperNotAvailable::class
    }

    interface AbstractSuperCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AbstractSuperCall::class
    }

    interface AbstractSuperCallWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AbstractSuperCallWarning::class
    }

    interface InstanceAccessBeforeSuperCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InstanceAccessBeforeSuperCall::class
        val target: String
    }

    interface SuperCallWithDefaultParameters : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuperCallWithDefaultParameters::class
        val name: String
    }

    interface InterfaceCantCallDefaultMethodViaSuper : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InterfaceCantCallDefaultMethodViaSuper::class
    }

    interface NotASupertype : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotASupertype::class
    }

    interface TypeArgumentsRedundantInSuperQualifier : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = TypeArgumentsRedundantInSuperQualifier::class
    }

    interface SuperclassNotAccessibleFromInterface : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuperclassNotAccessibleFromInterface::class
    }

    interface QualifiedSupertypeExtendedByOtherSupertype : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = QualifiedSupertypeExtendedByOtherSupertype::class
        val otherSuperType: KtSymbol
    }

    interface SupertypeInitializedInInterface : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SupertypeInitializedInInterface::class
    }

    interface InterfaceWithSuperclass : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = InterfaceWithSuperclass::class
    }

    interface FinalSupertype : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = FinalSupertype::class
    }

    interface ClassCannotBeExtendedDirectly : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ClassCannotBeExtendedDirectly::class
        val classSymbol: KtClassLikeSymbol
    }

    interface SupertypeIsExtensionFunctionType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SupertypeIsExtensionFunctionType::class
    }

    interface SingletonInSupertype : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SingletonInSupertype::class
    }

    interface NullableSupertype : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = NullableSupertype::class
    }

    interface ManyClassesInSupertypeList : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ManyClassesInSupertypeList::class
    }

    interface SupertypeAppearsTwice : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SupertypeAppearsTwice::class
    }

    interface ClassInSupertypeForEnum : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ClassInSupertypeForEnum::class
    }

    interface SealedSupertype : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SealedSupertype::class
    }

    interface SealedSupertypeInLocalClass : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SealedSupertypeInLocalClass::class
        val declarationType: String
        val sealedClassKind: ClassKind
    }

    interface SealedInheritorInDifferentPackage : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SealedInheritorInDifferentPackage::class
    }

    interface SealedInheritorInDifferentModule : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SealedInheritorInDifferentModule::class
    }

    interface ClassInheritsJavaSealedClass : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ClassInheritsJavaSealedClass::class
    }

    interface UnsupportedSealedFunInterface : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedSealedFunInterface::class
    }

    interface SupertypeNotAClassOrInterface : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SupertypeNotAClassOrInterface::class
        val reason: String
    }

    interface UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction::class
        val symbol: KtSymbol
    }

    interface CyclicInheritanceHierarchy : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CyclicInheritanceHierarchy::class
    }

    interface ExpandedTypeCannotBeInherited : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ExpandedTypeCannotBeInherited::class
        val type: KtType
    }

    interface ProjectionInImmediateArgumentToSupertype : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ProjectionInImmediateArgumentToSupertype::class
    }

    interface InconsistentTypeParameterValues : KtFirDiagnostic<KtClass> {
        override val diagnosticClass get() = InconsistentTypeParameterValues::class
        val typeParameter: KtTypeParameterSymbol
        val type: KtClassLikeSymbol
        val bounds: List<KtType>
    }

    interface InconsistentTypeParameterBounds : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InconsistentTypeParameterBounds::class
        val typeParameter: KtTypeParameterSymbol
        val type: KtClassLikeSymbol
        val bounds: List<KtType>
    }

    interface AmbiguousSuper : KtFirDiagnostic<KtSuperExpression> {
        override val diagnosticClass get() = AmbiguousSuper::class
        val candidates: List<KtType>
    }

    interface WrongMultipleInheritance : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongMultipleInheritance::class
        val symbol: KtCallableSymbol
    }

    interface ConstructorInObject : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ConstructorInObject::class
    }

    interface ConstructorInInterface : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ConstructorInInterface::class
    }

    interface NonPrivateConstructorInEnum : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonPrivateConstructorInEnum::class
    }

    interface NonPrivateOrProtectedConstructorInSealed : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonPrivateOrProtectedConstructorInSealed::class
    }

    interface CyclicConstructorDelegationCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CyclicConstructorDelegationCall::class
    }

    interface PrimaryConstructorDelegationCallExpected : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PrimaryConstructorDelegationCallExpected::class
    }

    interface ProtectedConstructorNotInSuperCall : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ProtectedConstructorNotInSuperCall::class
        val symbol: KtSymbol
    }

    interface SupertypeNotInitialized : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = SupertypeNotInitialized::class
    }

    interface SupertypeInitializedWithoutPrimaryConstructor : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SupertypeInitializedWithoutPrimaryConstructor::class
    }

    interface DelegationSuperCallInEnumConstructor : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegationSuperCallInEnumConstructor::class
    }

    interface ExplicitDelegationCallRequired : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExplicitDelegationCallRequired::class
    }

    interface SealedClassConstructorCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SealedClassConstructorCall::class
    }

    interface DataClassWithoutParameters : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = DataClassWithoutParameters::class
    }

    interface DataClassVarargParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = DataClassVarargParameter::class
    }

    interface DataClassNotPropertyParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = DataClassNotPropertyParameter::class
    }

    interface AnnotationArgumentKclassLiteralOfTypeParameterError : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationArgumentKclassLiteralOfTypeParameterError::class
    }

    interface AnnotationArgumentMustBeConst : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationArgumentMustBeConst::class
    }

    interface AnnotationArgumentMustBeEnumConst : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationArgumentMustBeEnumConst::class
    }

    interface AnnotationArgumentMustBeKclassLiteral : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationArgumentMustBeKclassLiteral::class
    }

    interface AnnotationClassMember : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationClassMember::class
    }

    interface AnnotationParameterDefaultValueMustBeConstant : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationParameterDefaultValueMustBeConstant::class
    }

    interface InvalidTypeOfAnnotationMember : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = InvalidTypeOfAnnotationMember::class
    }

    interface LocalAnnotationClassError : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = LocalAnnotationClassError::class
    }

    interface MissingValOnAnnotationParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = MissingValOnAnnotationParameter::class
    }

    interface NonConstValUsedInConstantExpression : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NonConstValUsedInConstantExpression::class
    }

    interface CycleInAnnotationParameterError : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = CycleInAnnotationParameterError::class
    }

    interface CycleInAnnotationParameterWarning : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = CycleInAnnotationParameterWarning::class
    }

    interface AnnotationClassConstructorCall : KtFirDiagnostic<KtCallExpression> {
        override val diagnosticClass get() = AnnotationClassConstructorCall::class
    }

    interface EnumClassConstructorCall : KtFirDiagnostic<KtCallExpression> {
        override val diagnosticClass get() = EnumClassConstructorCall::class
    }

    interface NotAnAnnotationClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotAnAnnotationClass::class
        val annotationName: String
    }

    interface NullableTypeOfAnnotationMember : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = NullableTypeOfAnnotationMember::class
    }

    interface VarAnnotationParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = VarAnnotationParameter::class
    }

    interface SupertypesForAnnotationClass : KtFirDiagnostic<KtClass> {
        override val diagnosticClass get() = SupertypesForAnnotationClass::class
    }

    interface AnnotationUsedAsAnnotationArgument : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationUsedAsAnnotationArgument::class
    }

    interface IllegalKotlinVersionStringValue : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = IllegalKotlinVersionStringValue::class
    }

    interface NewerVersionInSinceKotlin : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NewerVersionInSinceKotlin::class
        val specifiedVersion: String
    }

    interface DeprecatedSinceKotlinWithUnorderedVersions : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithUnorderedVersions::class
    }

    interface DeprecatedSinceKotlinWithoutArguments : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithoutArguments::class
    }

    interface DeprecatedSinceKotlinWithoutDeprecated : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithoutDeprecated::class
    }

    interface DeprecatedSinceKotlinWithDeprecatedLevel : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithDeprecatedLevel::class
    }

    interface DeprecatedSinceKotlinOutsideKotlinSubpackage : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinOutsideKotlinSubpackage::class
    }

    interface OverrideDeprecation : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = OverrideDeprecation::class
        val overridenSymbol: KtSymbol
        val deprecationInfo: DeprecationInfo
    }

    interface AnnotationOnSuperclassError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationOnSuperclassError::class
    }

    interface AnnotationOnSuperclassWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationOnSuperclassWarning::class
    }

    interface RestrictedRetentionForExpressionAnnotationError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RestrictedRetentionForExpressionAnnotationError::class
    }

    interface RestrictedRetentionForExpressionAnnotationWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RestrictedRetentionForExpressionAnnotationWarning::class
    }

    interface WrongAnnotationTarget : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongAnnotationTarget::class
        val actualTarget: String
    }

    interface WrongAnnotationTargetWithUseSiteTarget : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongAnnotationTargetWithUseSiteTarget::class
        val actualTarget: String
        val useSiteTarget: String
    }

    interface InapplicableTargetOnProperty : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetOnProperty::class
        val useSiteDescription: String
    }

    interface InapplicableTargetOnPropertyWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetOnPropertyWarning::class
        val useSiteDescription: String
    }

    interface InapplicableTargetPropertyImmutable : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetPropertyImmutable::class
        val useSiteDescription: String
    }

    interface InapplicableTargetPropertyHasNoDelegate : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetPropertyHasNoDelegate::class
    }

    interface InapplicableTargetPropertyHasNoBackingField : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetPropertyHasNoBackingField::class
    }

    interface InapplicableParamTarget : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableParamTarget::class
    }

    interface RedundantAnnotationTarget : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RedundantAnnotationTarget::class
        val useSiteDescription: String
    }

    interface InapplicableFileTarget : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableFileTarget::class
    }

    interface RepeatedAnnotation : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatedAnnotation::class
    }

    interface RepeatedAnnotationWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatedAnnotationWarning::class
    }

    interface NotAClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotAClass::class
    }

    interface WrongExtensionFunctionType : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongExtensionFunctionType::class
    }

    interface WrongExtensionFunctionTypeWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongExtensionFunctionTypeWarning::class
    }

    interface AnnotationInWhereClauseError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationInWhereClauseError::class
    }

    interface CompilerRequiredAnnotationAmbiguity : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CompilerRequiredAnnotationAmbiguity::class
        val typeFromCompilerPhase: KtType
        val typeFromTypesPhase: KtType
    }

    interface AmbiguousAnnotationArgument : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousAnnotationArgument::class
        val symbols: List<KtSymbol>
    }

    interface VolatileOnValue : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = VolatileOnValue::class
    }

    interface VolatileOnDelegate : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = VolatileOnDelegate::class
    }

    interface NonSourceAnnotationOnInlinedLambdaExpression : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = NonSourceAnnotationOnInlinedLambdaExpression::class
    }

    interface PotentiallyNonReportedAnnotation : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = PotentiallyNonReportedAnnotation::class
    }

    interface JsModuleProhibitedOnVar : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsModuleProhibitedOnVar::class
    }

    interface JsModuleProhibitedOnNonNative : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsModuleProhibitedOnNonNative::class
    }

    interface NestedJsModuleProhibited : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NestedJsModuleProhibited::class
    }

    interface CallFromUmdMustBeJsModuleAndJsNonModule : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallFromUmdMustBeJsModuleAndJsNonModule::class
    }

    interface CallToJsModuleWithoutModuleSystem : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallToJsModuleWithoutModuleSystem::class
        val callee: KtSymbol
    }

    interface CallToJsNonModuleWithModuleSystem : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallToJsNonModuleWithModuleSystem::class
        val callee: KtSymbol
    }

    interface RuntimeAnnotationNotSupported : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RuntimeAnnotationNotSupported::class
    }

    interface RuntimeAnnotationOnExternalDeclaration : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RuntimeAnnotationOnExternalDeclaration::class
    }

    interface NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun::class
        val type: KtType
    }

    interface NativeIndexerKeyShouldBeStringOrNumber : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NativeIndexerKeyShouldBeStringOrNumber::class
        val kind: String
    }

    interface NativeIndexerWrongParameterCount : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NativeIndexerWrongParameterCount::class
        val parametersCount: Int
        val kind: String
    }

    interface NativeIndexerCanNotHaveDefaultArguments : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NativeIndexerCanNotHaveDefaultArguments::class
        val kind: String
    }

    interface NativeGetterReturnTypeShouldBeNullable : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NativeGetterReturnTypeShouldBeNullable::class
    }

    interface NativeSetterWrongReturnType : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NativeSetterWrongReturnType::class
    }

    interface JsNameIsNotOnAllAccessors : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameIsNotOnAllAccessors::class
    }

    interface JsNameProhibitedForNamedNative : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameProhibitedForNamedNative::class
    }

    interface JsNameProhibitedForOverride : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameProhibitedForOverride::class
    }

    interface JsNameOnPrimaryConstructorProhibited : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameOnPrimaryConstructorProhibited::class
    }

    interface JsNameOnAccessorAndProperty : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameOnAccessorAndProperty::class
    }

    interface JsNameProhibitedForExtensionProperty : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameProhibitedForExtensionProperty::class
    }

    interface JsBuiltinNameClash : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsBuiltinNameClash::class
        val name: String
    }

    interface NameContainsIllegalChars : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NameContainsIllegalChars::class
    }

    interface JsNameClash : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameClash::class
        val name: String
        val existing: List<KtSymbol>
    }

    interface JsFakeNameClash : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsFakeNameClash::class
        val name: String
        val override: KtSymbol
        val existing: List<KtSymbol>
    }

    interface WrongJsQualifier : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongJsQualifier::class
    }

    interface OptInUsage : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInUsage::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInUsageError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInUsageError::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInOverride : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInOverride::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInOverrideError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInOverrideError::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInIsNotEnabled : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInIsNotEnabled::class
    }

    interface OptInCanOnlyBeUsedAsAnnotation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInCanOnlyBeUsedAsAnnotation::class
    }

    interface OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn::class
    }

    interface OptInWithoutArguments : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInWithoutArguments::class
    }

    interface OptInArgumentIsNotMarker : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInArgumentIsNotMarker::class
        val notMarkerClassId: ClassId
    }

    interface OptInMarkerWithWrongTarget : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerWithWrongTarget::class
        val target: String
    }

    interface OptInMarkerWithWrongRetention : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerWithWrongRetention::class
    }

    interface OptInMarkerOnWrongTarget : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerOnWrongTarget::class
        val target: String
    }

    interface OptInMarkerOnOverride : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerOnOverride::class
    }

    interface OptInMarkerOnOverrideWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerOnOverrideWarning::class
    }

    interface SubclassOptInInapplicable : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SubclassOptInInapplicable::class
        val target: String
    }

    interface ExposedTypealiasExpandedType : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedTypealiasExpandedType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedFunctionReturnType : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedFunctionReturnType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedReceiverType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ExposedReceiverType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedPropertyType : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedPropertyType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedPropertyTypeInConstructorError : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedPropertyTypeInConstructorError::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedPropertyTypeInConstructorWarning : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedPropertyTypeInConstructorWarning::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedParameterType : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ExposedParameterType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedSuperInterface : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ExposedSuperInterface::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedSuperClass : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ExposedSuperClass::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedTypeParameterBound : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ExposedTypeParameterBound::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KtSymbol
        val restrictingVisibility: EffectiveVisibility
    }

    interface InapplicableInfixModifier : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableInfixModifier::class
    }

    interface RepeatedModifier : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RepeatedModifier::class
        val modifier: KtModifierKeywordToken
    }

    interface RedundantModifier : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantModifier::class
        val redundantModifier: KtModifierKeywordToken
        val conflictingModifier: KtModifierKeywordToken
    }

    interface DeprecatedModifier : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifier::class
        val deprecatedModifier: KtModifierKeywordToken
        val actualModifier: KtModifierKeywordToken
    }

    interface DeprecatedModifierPair : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierPair::class
        val deprecatedModifier: KtModifierKeywordToken
        val conflictingModifier: KtModifierKeywordToken
    }

    interface DeprecatedModifierForTarget : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierForTarget::class
        val deprecatedModifier: KtModifierKeywordToken
        val target: String
    }

    interface RedundantModifierForTarget : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantModifierForTarget::class
        val redundantModifier: KtModifierKeywordToken
        val target: String
    }

    interface IncompatibleModifiers : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IncompatibleModifiers::class
        val modifier1: KtModifierKeywordToken
        val modifier2: KtModifierKeywordToken
    }

    interface RedundantOpenInInterface : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = RedundantOpenInInterface::class
    }

    interface WrongModifierTarget : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongModifierTarget::class
        val modifier: KtModifierKeywordToken
        val target: String
    }

    interface OperatorModifierRequired : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OperatorModifierRequired::class
        val functionSymbol: KtFunctionLikeSymbol
        val name: String
    }

    interface OperatorCallOnConstructor : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OperatorCallOnConstructor::class
        val name: String
    }

    interface InfixModifierRequired : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InfixModifierRequired::class
        val functionSymbol: KtFunctionLikeSymbol
    }

    interface WrongModifierContainingDeclaration : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongModifierContainingDeclaration::class
        val modifier: KtModifierKeywordToken
        val target: String
    }

    interface DeprecatedModifierContainingDeclaration : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierContainingDeclaration::class
        val modifier: KtModifierKeywordToken
        val target: String
    }

    interface InapplicableOperatorModifier : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableOperatorModifier::class
        val message: String
    }

    interface NoExplicitVisibilityInApiMode : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NoExplicitVisibilityInApiMode::class
    }

    interface NoExplicitVisibilityInApiModeWarning : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NoExplicitVisibilityInApiModeWarning::class
    }

    interface NoExplicitReturnTypeInApiMode : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NoExplicitReturnTypeInApiMode::class
    }

    interface NoExplicitReturnTypeInApiModeWarning : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NoExplicitReturnTypeInApiModeWarning::class
    }

    interface AnonymousSuspendFunction : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = AnonymousSuspendFunction::class
    }

    interface ValueClassNotTopLevel : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ValueClassNotTopLevel::class
    }

    interface ValueClassNotFinal : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ValueClassNotFinal::class
    }

    interface AbsenceOfPrimaryConstructorForValueClass : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = AbsenceOfPrimaryConstructorForValueClass::class
    }

    interface InlineClassConstructorWrongParametersSize : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InlineClassConstructorWrongParametersSize::class
    }

    interface ValueClassEmptyConstructor : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ValueClassEmptyConstructor::class
    }

    interface ValueClassConstructorNotFinalReadOnlyParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValueClassConstructorNotFinalReadOnlyParameter::class
    }

    interface PropertyWithBackingFieldInsideValueClass : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PropertyWithBackingFieldInsideValueClass::class
    }

    interface DelegatedPropertyInsideValueClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegatedPropertyInsideValueClass::class
    }

    interface ValueClassHasInapplicableParameterType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ValueClassHasInapplicableParameterType::class
        val type: KtType
    }

    interface ValueClassCannotImplementInterfaceByDelegation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ValueClassCannotImplementInterfaceByDelegation::class
    }

    interface ValueClassCannotExtendClasses : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ValueClassCannotExtendClasses::class
    }

    interface ValueClassCannotBeRecursive : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ValueClassCannotBeRecursive::class
    }

    interface MultiFieldValueClassPrimaryConstructorDefaultParameter : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = MultiFieldValueClassPrimaryConstructorDefaultParameter::class
    }

    interface SecondaryConstructorWithBodyInsideValueClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SecondaryConstructorWithBodyInsideValueClass::class
    }

    interface ReservedMemberInsideValueClass : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = ReservedMemberInsideValueClass::class
        val name: String
    }

    interface TypeArgumentOnTypedValueClassEquals : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = TypeArgumentOnTypedValueClassEquals::class
    }

    interface InnerClassInsideValueClass : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InnerClassInsideValueClass::class
    }

    interface ValueClassCannotBeCloneable : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ValueClassCannotBeCloneable::class
    }

    interface ValueClassCannotHaveContextReceivers : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ValueClassCannotHaveContextReceivers::class
    }

    interface AnnotationOnIllegalMultiFieldValueClassTypedTarget : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationOnIllegalMultiFieldValueClassTypedTarget::class
        val name: String
    }

    interface NoneApplicable : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoneApplicable::class
        val candidates: List<KtSymbol>
    }

    interface InapplicableCandidate : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableCandidate::class
        val candidate: KtSymbol
    }

    interface TypeMismatch : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeMismatch::class
        val expectedType: KtType
        val actualType: KtType
        val isMismatchDueToNullability: Boolean
    }

    interface TypeInferenceOnlyInputTypesError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeInferenceOnlyInputTypesError::class
        val typeParameter: KtTypeParameterSymbol
    }

    interface ThrowableTypeMismatch : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ThrowableTypeMismatch::class
        val actualType: KtType
        val isMismatchDueToNullability: Boolean
    }

    interface ConditionTypeMismatch : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConditionTypeMismatch::class
        val actualType: KtType
        val isMismatchDueToNullability: Boolean
    }

    interface ArgumentTypeMismatch : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ArgumentTypeMismatch::class
        val expectedType: KtType
        val actualType: KtType
        val isMismatchDueToNullability: Boolean
    }

    interface NullForNonnullType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NullForNonnullType::class
        val expectedType: KtType
    }

    interface InapplicableLateinitModifier : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = InapplicableLateinitModifier::class
        val reason: String
    }

    interface VarargOutsideParentheses : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = VarargOutsideParentheses::class
    }

    interface NamedArgumentsNotAllowed : KtFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = NamedArgumentsNotAllowed::class
        val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget
    }

    interface NonVarargSpread : KtFirDiagnostic<LeafPsiElement> {
        override val diagnosticClass get() = NonVarargSpread::class
    }

    interface ArgumentPassedTwice : KtFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = ArgumentPassedTwice::class
    }

    interface TooManyArguments : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TooManyArguments::class
        val function: KtCallableSymbol
    }

    interface NoValueForParameter : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NoValueForParameter::class
        val violatedParameter: KtSymbol
    }

    interface NamedParameterNotFound : KtFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = NamedParameterNotFound::class
        val name: String
    }

    interface NameForAmbiguousParameter : KtFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = NameForAmbiguousParameter::class
    }

    interface AssignmentTypeMismatch : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssignmentTypeMismatch::class
        val expectedType: KtType
        val actualType: KtType
        val isMismatchDueToNullability: Boolean
    }

    interface ResultTypeMismatch : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ResultTypeMismatch::class
        val expectedType: KtType
        val actualType: KtType
    }

    interface ManyLambdaExpressionArguments : KtFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = ManyLambdaExpressionArguments::class
    }

    interface NewInferenceNoInformationForParameter : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NewInferenceNoInformationForParameter::class
        val name: String
    }

    interface SpreadOfNullable : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpreadOfNullable::class
    }

    interface AssigningSingleElementToVarargInNamedFormFunctionError : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormFunctionError::class
        val expectedArrayType: KtType
    }

    interface AssigningSingleElementToVarargInNamedFormFunctionWarning : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormFunctionWarning::class
        val expectedArrayType: KtType
    }

    interface AssigningSingleElementToVarargInNamedFormAnnotationError : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormAnnotationError::class
    }

    interface AssigningSingleElementToVarargInNamedFormAnnotationWarning : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormAnnotationWarning::class
    }

    interface RedundantSpreadOperatorInNamedFormInAnnotation : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = RedundantSpreadOperatorInNamedFormInAnnotation::class
    }

    interface RedundantSpreadOperatorInNamedFormInFunction : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = RedundantSpreadOperatorInNamedFormInFunction::class
    }

    interface InferenceUnsuccessfulFork : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferenceUnsuccessfulFork::class
        val message: String
    }

    interface NestedClassAccessedViaInstanceReference : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NestedClassAccessedViaInstanceReference::class
        val symbol: KtClassLikeSymbol
    }

    interface OverloadResolutionAmbiguity : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OverloadResolutionAmbiguity::class
        val candidates: List<KtSymbol>
    }

    interface AssignOperatorAmbiguity : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AssignOperatorAmbiguity::class
        val candidates: List<KtSymbol>
    }

    interface IteratorAmbiguity : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IteratorAmbiguity::class
        val candidates: List<KtSymbol>
    }

    interface HasNextFunctionAmbiguity : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = HasNextFunctionAmbiguity::class
        val candidates: List<KtSymbol>
    }

    interface NextAmbiguity : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NextAmbiguity::class
        val candidates: List<KtSymbol>
    }

    interface AmbiguousFunctionTypeKind : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousFunctionTypeKind::class
        val kinds: List<FunctionTypeKind>
    }

    interface NoContextReceiver : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NoContextReceiver::class
        val contextReceiverRepresentation: KtType
    }

    interface MultipleArgumentsApplicableForContextReceiver : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleArgumentsApplicableForContextReceiver::class
        val contextReceiverRepresentation: KtType
    }

    interface AmbiguousCallWithImplicitContextReceiver : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = AmbiguousCallWithImplicitContextReceiver::class
    }

    interface UnsupportedContextualDeclarationCall : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnsupportedContextualDeclarationCall::class
    }

    interface SubtypingBetweenContextReceivers : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SubtypingBetweenContextReceivers::class
    }

    interface ContextReceiversWithBackingField : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ContextReceiversWithBackingField::class
    }

    interface RecursionInImplicitTypes : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RecursionInImplicitTypes::class
    }

    interface InferenceError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferenceError::class
    }

    interface ProjectionOnNonClassTypeArgument : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ProjectionOnNonClassTypeArgument::class
    }

    interface UpperBoundViolated : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolated::class
        val expectedUpperBound: KtType
        val actualUpperBound: KtType
        val extraMessage: String
    }

    interface UpperBoundViolatedInTypealiasExpansion : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolatedInTypealiasExpansion::class
        val expectedUpperBound: KtType
        val actualUpperBound: KtType
    }

    interface TypeArgumentsNotAllowed : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeArgumentsNotAllowed::class
        val place: String
    }

    interface TypeArgumentsForOuterClassWhenNestedReferenced : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeArgumentsForOuterClassWhenNestedReferenced::class
    }

    interface WrongNumberOfTypeArguments : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongNumberOfTypeArguments::class
        val expectedCount: Int
        val classifier: KtClassLikeSymbol
    }

    interface NoTypeArgumentsOnRhs : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoTypeArgumentsOnRhs::class
        val expectedCount: Int
        val classifier: KtClassLikeSymbol
    }

    interface OuterClassArgumentsRequired : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OuterClassArgumentsRequired::class
        val outer: KtClassLikeSymbol
    }

    interface TypeParametersInObject : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParametersInObject::class
    }

    interface TypeParametersInAnonymousObject : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParametersInAnonymousObject::class
    }

    interface IllegalProjectionUsage : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalProjectionUsage::class
    }

    interface TypeParametersInEnum : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParametersInEnum::class
    }

    interface ConflictingProjection : KtFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass get() = ConflictingProjection::class
        val type: KtType
    }

    interface ConflictingProjectionInTypealiasExpansion : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConflictingProjectionInTypealiasExpansion::class
        val type: KtType
    }

    interface RedundantProjection : KtFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass get() = RedundantProjection::class
        val type: KtType
    }

    interface VarianceOnTypeParameterNotAllowed : KtFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = VarianceOnTypeParameterNotAllowed::class
    }

    interface CatchParameterWithDefaultValue : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CatchParameterWithDefaultValue::class
    }

    interface ReifiedTypeInCatchClause : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ReifiedTypeInCatchClause::class
    }

    interface TypeParameterInCatchClause : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParameterInCatchClause::class
    }

    interface GenericThrowableSubclass : KtFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = GenericThrowableSubclass::class
    }

    interface InnerClassOfGenericThrowableSubclass : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = InnerClassOfGenericThrowableSubclass::class
    }

    interface KclassWithNullableTypeParameterInSignature : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = KclassWithNullableTypeParameterInSignature::class
        val typeParameter: KtTypeParameterSymbol
    }

    interface TypeParameterAsReified : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParameterAsReified::class
        val typeParameter: KtTypeParameterSymbol
    }

    interface TypeParameterAsReifiedArrayError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParameterAsReifiedArrayError::class
        val typeParameter: KtTypeParameterSymbol
    }

    interface TypeParameterAsReifiedArrayWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParameterAsReifiedArrayWarning::class
        val typeParameter: KtTypeParameterSymbol
    }

    interface ReifiedTypeForbiddenSubstitution : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ReifiedTypeForbiddenSubstitution::class
        val type: KtType
    }

    interface DefinitelyNonNullableAsReified : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DefinitelyNonNullableAsReified::class
    }

    interface FinalUpperBound : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = FinalUpperBound::class
        val type: KtType
    }

    interface UpperBoundIsExtensionFunctionType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = UpperBoundIsExtensionFunctionType::class
    }

    interface BoundsNotAllowedIfBoundedByTypeParameter : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = BoundsNotAllowedIfBoundedByTypeParameter::class
    }

    interface OnlyOneClassBoundAllowed : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = OnlyOneClassBoundAllowed::class
    }

    interface RepeatedBound : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = RepeatedBound::class
    }

    interface ConflictingUpperBounds : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ConflictingUpperBounds::class
        val typeParameter: KtTypeParameterSymbol
    }

    interface NameInConstraintIsNotATypeParameter : KtFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass get() = NameInConstraintIsNotATypeParameter::class
        val typeParameterName: Name
        val typeParametersOwner: KtSymbol
    }

    interface BoundOnTypeAliasParameterNotAllowed : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = BoundOnTypeAliasParameterNotAllowed::class
    }

    interface ReifiedTypeParameterNoInline : KtFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = ReifiedTypeParameterNoInline::class
    }

    interface TypeParametersNotAllowed : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = TypeParametersNotAllowed::class
    }

    interface TypeParameterOfPropertyNotUsedInReceiver : KtFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = TypeParameterOfPropertyNotUsedInReceiver::class
    }

    interface ReturnTypeMismatch : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ReturnTypeMismatch::class
        val expectedType: KtType
        val actualType: KtType
        val targetFunction: KtSymbol
        val isMismatchDueToNullability: Boolean
    }

    interface ImplicitNothingReturnType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ImplicitNothingReturnType::class
    }

    interface ImplicitNothingPropertyType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ImplicitNothingPropertyType::class
    }

    interface AbbreviatedNothingReturnType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AbbreviatedNothingReturnType::class
    }

    interface AbbreviatedNothingPropertyType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AbbreviatedNothingPropertyType::class
    }

    interface CyclicGenericUpperBound : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CyclicGenericUpperBound::class
    }

    interface FiniteBoundsViolation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FiniteBoundsViolation::class
    }

    interface FiniteBoundsViolationInJava : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FiniteBoundsViolationInJava::class
        val containingTypes: List<KtSymbol>
    }

    interface ExpansiveInheritance : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpansiveInheritance::class
    }

    interface ExpansiveInheritanceInJava : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpansiveInheritanceInJava::class
        val containingTypes: List<KtSymbol>
    }

    interface DeprecatedTypeParameterSyntax : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = DeprecatedTypeParameterSyntax::class
    }

    interface MisplacedTypeParameterConstraints : KtFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = MisplacedTypeParameterConstraints::class
    }

    interface DynamicSupertype : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = DynamicSupertype::class
    }

    interface DynamicUpperBound : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = DynamicUpperBound::class
    }

    interface DynamicReceiverNotAllowed : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DynamicReceiverNotAllowed::class
    }

    interface DynamicReceiverExpectedButWasNonDynamic : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DynamicReceiverExpectedButWasNonDynamic::class
        val actualType: KtType
    }

    interface IncompatibleTypes : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncompatibleTypes::class
        val typeA: KtType
        val typeB: KtType
    }

    interface IncompatibleTypesWarning : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncompatibleTypesWarning::class
        val typeA: KtType
        val typeB: KtType
    }

    interface TypeVarianceConflictError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeVarianceConflictError::class
        val typeParameter: KtTypeParameterSymbol
        val typeParameterVariance: Variance
        val variance: Variance
        val containingType: KtType
    }

    interface TypeVarianceConflictInExpandedType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeVarianceConflictInExpandedType::class
        val typeParameter: KtTypeParameterSymbol
        val typeParameterVariance: Variance
        val variance: Variance
        val containingType: KtType
    }

    interface SmartcastImpossible : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = SmartcastImpossible::class
        val desiredType: KtType
        val subject: KtExpression
        val description: String
        val isCastToNotNull: Boolean
    }

    interface RedundantNullable : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = RedundantNullable::class
    }

    interface PlatformClassMappedToKotlin : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PlatformClassMappedToKotlin::class
        val kotlinClass: ClassId
    }

    interface InferredTypeVariableIntoEmptyIntersectionError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferredTypeVariableIntoEmptyIntersectionError::class
        val typeVariableDescription: String
        val incompatibleTypes: List<KtType>
        val description: String
        val causingTypes: String
    }

    interface InferredTypeVariableIntoEmptyIntersectionWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferredTypeVariableIntoEmptyIntersectionWarning::class
        val typeVariableDescription: String
        val incompatibleTypes: List<KtType>
        val description: String
        val causingTypes: String
    }

    interface InferredTypeVariableIntoPossibleEmptyIntersection : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferredTypeVariableIntoPossibleEmptyIntersection::class
        val typeVariableDescription: String
        val incompatibleTypes: List<KtType>
        val description: String
        val causingTypes: String
    }

    interface IncorrectLeftComponentOfIntersection : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = IncorrectLeftComponentOfIntersection::class
    }

    interface IncorrectRightComponentOfIntersection : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = IncorrectRightComponentOfIntersection::class
    }

    interface NullableOnDefinitelyNotNullable : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = NullableOnDefinitelyNotNullable::class
    }

    interface ExtensionInClassReferenceNotAllowed : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExtensionInClassReferenceNotAllowed::class
        val referencedDeclaration: KtCallableSymbol
    }

    interface CallableReferenceLhsNotAClass : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CallableReferenceLhsNotAClass::class
    }

    interface CallableReferenceToAnnotationConstructor : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CallableReferenceToAnnotationConstructor::class
    }

    interface AdaptedCallableReferenceAgainstReflectionType : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AdaptedCallableReferenceAgainstReflectionType::class
    }

    interface ClassLiteralLhsNotAClass : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ClassLiteralLhsNotAClass::class
    }

    interface NullableTypeInClassLiteralLhs : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NullableTypeInClassLiteralLhs::class
    }

    interface ExpressionOfNullableTypeInClassLiteralLhs : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpressionOfNullableTypeInClassLiteralLhs::class
        val lhsType: KtType
    }

    interface UnsupportedClassLiteralsWithEmptyLhs : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnsupportedClassLiteralsWithEmptyLhs::class
    }

    interface MutablePropertyWithCapturedType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MutablePropertyWithCapturedType::class
    }

    interface NothingToOverride : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = NothingToOverride::class
        val declaration: KtCallableSymbol
    }

    interface CannotOverrideInvisibleMember : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = CannotOverrideInvisibleMember::class
        val overridingMember: KtCallableSymbol
        val baseMember: KtCallableSymbol
    }

    interface DataClassOverrideConflict : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = DataClassOverrideConflict::class
        val overridingMember: KtCallableSymbol
        val baseMember: KtCallableSymbol
    }

    interface DataClassOverrideDefaultValues : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DataClassOverrideDefaultValues::class
        val overridingMember: KtCallableSymbol
        val baseType: KtClassLikeSymbol
    }

    interface CannotWeakenAccessPrivilege : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = CannotWeakenAccessPrivilege::class
        val overridingVisibility: Visibility
        val overridden: KtCallableSymbol
        val containingClassName: Name
    }

    interface CannotChangeAccessPrivilege : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = CannotChangeAccessPrivilege::class
        val overridingVisibility: Visibility
        val overridden: KtCallableSymbol
        val containingClassName: Name
    }

    interface CannotInferVisibility : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = CannotInferVisibility::class
        val callable: KtCallableSymbol
    }

    interface MultipleDefaultsInheritedFromSupertypes : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypes::class
        val name: Name
        val valueParameter: KtSymbol
        val baseFunctions: List<KtCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride::class
        val name: Name
        val valueParameter: KtSymbol
        val baseFunctions: List<KtCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesDeprecationError : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesDeprecationError::class
        val name: Name
        val valueParameter: KtSymbol
        val baseFunctions: List<KtCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesDeprecationWarning : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesDeprecationWarning::class
        val name: Name
        val valueParameter: KtSymbol
        val baseFunctions: List<KtCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError::class
        val name: Name
        val valueParameter: KtSymbol
        val baseFunctions: List<KtCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning::class
        val name: Name
        val valueParameter: KtSymbol
        val baseFunctions: List<KtCallableSymbol>
    }

    interface TypealiasExpandsToArrayOfNothings : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = TypealiasExpandsToArrayOfNothings::class
        val type: KtType
    }

    interface OverridingFinalMember : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = OverridingFinalMember::class
        val overriddenDeclaration: KtCallableSymbol
        val containingClassName: Name
    }

    interface ReturnTypeMismatchOnInheritance : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ReturnTypeMismatchOnInheritance::class
        val conflictingDeclaration1: KtCallableSymbol
        val conflictingDeclaration2: KtCallableSymbol
    }

    interface PropertyTypeMismatchOnInheritance : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = PropertyTypeMismatchOnInheritance::class
        val conflictingDeclaration1: KtCallableSymbol
        val conflictingDeclaration2: KtCallableSymbol
    }

    interface VarTypeMismatchOnInheritance : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = VarTypeMismatchOnInheritance::class
        val conflictingDeclaration1: KtCallableSymbol
        val conflictingDeclaration2: KtCallableSymbol
    }

    interface ReturnTypeMismatchByDelegation : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ReturnTypeMismatchByDelegation::class
        val delegateDeclaration: KtCallableSymbol
        val baseDeclaration: KtCallableSymbol
    }

    interface PropertyTypeMismatchByDelegation : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = PropertyTypeMismatchByDelegation::class
        val delegateDeclaration: KtCallableSymbol
        val baseDeclaration: KtCallableSymbol
    }

    interface VarOverriddenByValByDelegation : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = VarOverriddenByValByDelegation::class
        val delegateDeclaration: KtCallableSymbol
        val baseDeclaration: KtCallableSymbol
    }

    interface ConflictingInheritedMembers : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ConflictingInheritedMembers::class
        val owner: KtClassLikeSymbol
        val conflictingDeclarations: List<KtCallableSymbol>
    }

    interface AbstractMemberNotImplemented : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = AbstractMemberNotImplemented::class
        val classOrObject: KtClassLikeSymbol
        val missingDeclaration: KtCallableSymbol
    }

    interface AbstractMemberNotImplementedByEnumEntry : KtFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass get() = AbstractMemberNotImplementedByEnumEntry::class
        val enumEntry: KtSymbol
        val missingDeclarations: List<KtCallableSymbol>
    }

    interface AbstractClassMemberNotImplemented : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = AbstractClassMemberNotImplemented::class
        val classOrObject: KtClassLikeSymbol
        val missingDeclaration: KtCallableSymbol
    }

    interface InvisibleAbstractMemberFromSuperError : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = InvisibleAbstractMemberFromSuperError::class
        val classOrObject: KtClassLikeSymbol
        val invisibleDeclaration: KtCallableSymbol
    }

    interface InvisibleAbstractMemberFromSuperWarning : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = InvisibleAbstractMemberFromSuperWarning::class
        val classOrObject: KtClassLikeSymbol
        val invisibleDeclaration: KtCallableSymbol
    }

    interface AmbiguousAnonymousTypeInferred : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = AmbiguousAnonymousTypeInferred::class
        val superTypes: List<KtType>
    }

    interface ManyImplMemberNotImplemented : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ManyImplMemberNotImplemented::class
        val classOrObject: KtClassLikeSymbol
        val missingDeclaration: KtCallableSymbol
    }

    interface ManyInterfacesMemberNotImplemented : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ManyInterfacesMemberNotImplemented::class
        val classOrObject: KtClassLikeSymbol
        val missingDeclaration: KtCallableSymbol
    }

    interface OverridingFinalMemberByDelegation : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = OverridingFinalMemberByDelegation::class
        val delegatedDeclaration: KtCallableSymbol
        val overriddenDeclaration: KtCallableSymbol
    }

    interface DelegatedMemberHidesSupertypeOverride : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = DelegatedMemberHidesSupertypeOverride::class
        val delegatedDeclaration: KtCallableSymbol
        val overriddenDeclaration: KtCallableSymbol
    }

    interface ReturnTypeMismatchOnOverride : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ReturnTypeMismatchOnOverride::class
        val function: KtCallableSymbol
        val superFunction: KtCallableSymbol
    }

    interface PropertyTypeMismatchOnOverride : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = PropertyTypeMismatchOnOverride::class
        val property: KtCallableSymbol
        val superProperty: KtCallableSymbol
    }

    interface VarTypeMismatchOnOverride : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VarTypeMismatchOnOverride::class
        val variable: KtCallableSymbol
        val superVariable: KtCallableSymbol
    }

    interface VarOverriddenByVal : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VarOverriddenByVal::class
        val overridingDeclaration: KtCallableSymbol
        val overriddenDeclaration: KtCallableSymbol
    }

    interface VarImplementedByInheritedValError : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VarImplementedByInheritedValError::class
        val classOrObject: KtClassLikeSymbol
        val overridingDeclaration: KtCallableSymbol
        val overriddenDeclaration: KtCallableSymbol
    }

    interface VarImplementedByInheritedValWarning : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VarImplementedByInheritedValWarning::class
        val classOrObject: KtClassLikeSymbol
        val overridingDeclaration: KtCallableSymbol
        val overriddenDeclaration: KtCallableSymbol
    }

    interface NonFinalMemberInFinalClass : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NonFinalMemberInFinalClass::class
    }

    interface NonFinalMemberInObject : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NonFinalMemberInObject::class
    }

    interface VirtualMemberHidden : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VirtualMemberHidden::class
        val declared: KtCallableSymbol
        val overriddenContainer: KtClassLikeSymbol
    }

    interface ManyCompanionObjects : KtFirDiagnostic<KtObjectDeclaration> {
        override val diagnosticClass get() = ManyCompanionObjects::class
    }

    interface ConflictingOverloads : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConflictingOverloads::class
        val conflictingOverloads: List<KtSymbol>
    }

    interface Redeclaration : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = Redeclaration::class
        val conflictingDeclarations: List<KtSymbol>
    }

    interface PackageOrClassifierRedeclaration : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = PackageOrClassifierRedeclaration::class
        val conflictingDeclarations: List<KtSymbol>
    }

    interface ExpectAndActualInTheSameModule : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectAndActualInTheSameModule::class
        val declaration: KtSymbol
    }

    interface MethodOfAnyImplementedInInterface : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MethodOfAnyImplementedInInterface::class
    }

    interface LocalObjectNotAllowed : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = LocalObjectNotAllowed::class
        val objectName: Name
    }

    interface LocalInterfaceNotAllowed : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = LocalInterfaceNotAllowed::class
        val interfaceName: Name
    }

    interface AbstractFunctionInNonAbstractClass : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = AbstractFunctionInNonAbstractClass::class
        val function: KtCallableSymbol
        val containingClass: KtClassLikeSymbol
    }

    interface AbstractFunctionWithBody : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = AbstractFunctionWithBody::class
        val function: KtCallableSymbol
    }

    interface NonAbstractFunctionWithNoBody : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = NonAbstractFunctionWithNoBody::class
        val function: KtCallableSymbol
    }

    interface PrivateFunctionWithNoBody : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = PrivateFunctionWithNoBody::class
        val function: KtCallableSymbol
    }

    interface NonMemberFunctionNoBody : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = NonMemberFunctionNoBody::class
        val function: KtCallableSymbol
    }

    interface FunctionDeclarationWithNoName : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = FunctionDeclarationWithNoName::class
    }

    interface AnonymousFunctionWithName : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = AnonymousFunctionWithName::class
    }

    interface SingleAnonymousFunctionWithNameError : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = SingleAnonymousFunctionWithNameError::class
    }

    interface SingleAnonymousFunctionWithNameWarning : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = SingleAnonymousFunctionWithNameWarning::class
    }

    interface AnonymousFunctionParameterWithDefaultValue : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = AnonymousFunctionParameterWithDefaultValue::class
    }

    interface UselessVarargOnParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = UselessVarargOnParameter::class
    }

    interface MultipleVarargParameters : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = MultipleVarargParameters::class
    }

    interface ForbiddenVarargParameterType : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ForbiddenVarargParameterType::class
        val varargParameterType: KtType
    }

    interface ValueParameterWithNoTypeAnnotation : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValueParameterWithNoTypeAnnotation::class
    }

    interface CannotInferParameterType : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CannotInferParameterType::class
    }

    interface NoTailCallsFound : KtFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = NoTailCallsFound::class
    }

    interface TailrecOnVirtualMemberError : KtFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = TailrecOnVirtualMemberError::class
    }

    interface NonTailRecursiveCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonTailRecursiveCall::class
    }

    interface TailRecursionInTryIsNotSupported : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TailRecursionInTryIsNotSupported::class
    }

    interface DataObjectCustomEqualsOrHashCode : KtFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = DataObjectCustomEqualsOrHashCode::class
    }

    interface DefaultValueNotAllowedInOverride : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DefaultValueNotAllowedInOverride::class
    }

    interface FunInterfaceConstructorReference : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = FunInterfaceConstructorReference::class
    }

    interface FunInterfaceWrongCountOfAbstractMembers : KtFirDiagnostic<KtClass> {
        override val diagnosticClass get() = FunInterfaceWrongCountOfAbstractMembers::class
    }

    interface FunInterfaceCannotHaveAbstractProperties : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = FunInterfaceCannotHaveAbstractProperties::class
    }

    interface FunInterfaceAbstractMethodWithTypeParameters : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = FunInterfaceAbstractMethodWithTypeParameters::class
    }

    interface FunInterfaceAbstractMethodWithDefaultValue : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = FunInterfaceAbstractMethodWithDefaultValue::class
    }

    interface FunInterfaceWithSuspendFunction : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = FunInterfaceWithSuspendFunction::class
    }

    interface AbstractPropertyInNonAbstractClass : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = AbstractPropertyInNonAbstractClass::class
        val property: KtCallableSymbol
        val containingClass: KtClassLikeSymbol
    }

    interface PrivatePropertyInInterface : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PrivatePropertyInInterface::class
    }

    interface AbstractPropertyWithInitializer : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AbstractPropertyWithInitializer::class
    }

    interface PropertyInitializerInInterface : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = PropertyInitializerInInterface::class
    }

    interface PropertyWithNoTypeNoInitializer : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PropertyWithNoTypeNoInitializer::class
    }

    interface MustBeInitialized : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitialized::class
    }

    interface MustBeInitializedWarning : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedWarning::class
    }

    interface MustBeInitializedOrBeFinal : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrBeFinal::class
    }

    interface MustBeInitializedOrBeFinalWarning : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrBeFinalWarning::class
    }

    interface MustBeInitializedOrBeAbstract : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrBeAbstract::class
    }

    interface MustBeInitializedOrBeAbstractWarning : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrBeAbstractWarning::class
    }

    interface MustBeInitializedOrFinalOrAbstract : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrFinalOrAbstract::class
    }

    interface MustBeInitializedOrFinalOrAbstractWarning : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrFinalOrAbstractWarning::class
    }

    interface ExtensionPropertyMustHaveAccessorsOrBeAbstract : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = ExtensionPropertyMustHaveAccessorsOrBeAbstract::class
    }

    interface UnnecessaryLateinit : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = UnnecessaryLateinit::class
    }

    interface BackingFieldInInterface : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = BackingFieldInInterface::class
    }

    interface ExtensionPropertyWithBackingField : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExtensionPropertyWithBackingField::class
    }

    interface PropertyInitializerNoBackingField : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = PropertyInitializerNoBackingField::class
    }

    interface AbstractDelegatedProperty : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AbstractDelegatedProperty::class
    }

    interface DelegatedPropertyInInterface : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegatedPropertyInInterface::class
    }

    interface AbstractPropertyWithGetter : KtFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass get() = AbstractPropertyWithGetter::class
    }

    interface AbstractPropertyWithSetter : KtFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass get() = AbstractPropertyWithSetter::class
    }

    interface PrivateSetterForAbstractProperty : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = PrivateSetterForAbstractProperty::class
    }

    interface PrivateSetterForOpenProperty : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = PrivateSetterForOpenProperty::class
    }

    interface ValWithSetter : KtFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass get() = ValWithSetter::class
    }

    interface ConstValNotTopLevelOrObject : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConstValNotTopLevelOrObject::class
    }

    interface ConstValWithGetter : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConstValWithGetter::class
    }

    interface ConstValWithDelegate : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ConstValWithDelegate::class
    }

    interface TypeCantBeUsedForConstVal : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = TypeCantBeUsedForConstVal::class
        val constValType: KtType
    }

    interface ConstValWithoutInitializer : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = ConstValWithoutInitializer::class
    }

    interface ConstValWithNonConstInitializer : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ConstValWithNonConstInitializer::class
    }

    interface WrongSetterParameterType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = WrongSetterParameterType::class
        val expectedType: KtType
        val actualType: KtType
    }

    interface DelegateUsesExtensionPropertyTypeParameterError : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = DelegateUsesExtensionPropertyTypeParameterError::class
        val usedTypeParameter: KtTypeParameterSymbol
    }

    interface DelegateUsesExtensionPropertyTypeParameterWarning : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = DelegateUsesExtensionPropertyTypeParameterWarning::class
        val usedTypeParameter: KtTypeParameterSymbol
    }

    interface InitializerTypeMismatch : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = InitializerTypeMismatch::class
        val expectedType: KtType
        val actualType: KtType
        val isMismatchDueToNullability: Boolean
    }

    interface GetterVisibilityDiffersFromPropertyVisibility : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = GetterVisibilityDiffersFromPropertyVisibility::class
    }

    interface SetterVisibilityInconsistentWithPropertyVisibility : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = SetterVisibilityInconsistentWithPropertyVisibility::class
    }

    interface WrongSetterReturnType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = WrongSetterReturnType::class
    }

    interface WrongGetterReturnType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = WrongGetterReturnType::class
        val expectedType: KtType
        val actualType: KtType
    }

    interface AccessorForDelegatedProperty : KtFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass get() = AccessorForDelegatedProperty::class
    }

    interface PropertyInitializerWithExplicitFieldDeclaration : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = PropertyInitializerWithExplicitFieldDeclaration::class
    }

    interface PropertyFieldDeclarationMissingInitializer : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = PropertyFieldDeclarationMissingInitializer::class
    }

    interface LateinitPropertyFieldDeclarationWithInitializer : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = LateinitPropertyFieldDeclarationWithInitializer::class
    }

    interface LateinitFieldInValProperty : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = LateinitFieldInValProperty::class
    }

    interface LateinitNullableBackingField : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = LateinitNullableBackingField::class
    }

    interface BackingFieldForDelegatedProperty : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = BackingFieldForDelegatedProperty::class
    }

    interface PropertyMustHaveGetter : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PropertyMustHaveGetter::class
    }

    interface PropertyMustHaveSetter : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PropertyMustHaveSetter::class
    }

    interface ExplicitBackingFieldInInterface : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = ExplicitBackingFieldInInterface::class
    }

    interface ExplicitBackingFieldInAbstractProperty : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = ExplicitBackingFieldInAbstractProperty::class
    }

    interface ExplicitBackingFieldInExtension : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = ExplicitBackingFieldInExtension::class
    }

    interface RedundantExplicitBackingField : KtFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = RedundantExplicitBackingField::class
    }

    interface AbstractPropertyInPrimaryConstructorParameters : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = AbstractPropertyInPrimaryConstructorParameters::class
    }

    interface LocalVariableWithTypeParametersWarning : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = LocalVariableWithTypeParametersWarning::class
    }

    interface LocalVariableWithTypeParameters : KtFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = LocalVariableWithTypeParameters::class
    }

    interface ExplicitTypeArgumentsInPropertyAccess : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExplicitTypeArgumentsInPropertyAccess::class
        val kind: String
    }

    interface SafeCallableReferenceCall : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = SafeCallableReferenceCall::class
    }

    interface LateinitIntrinsicCallOnNonLiteral : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonLiteral::class
    }

    interface LateinitIntrinsicCallOnNonLateinit : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonLateinit::class
    }

    interface LateinitIntrinsicCallInInlineFunction : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LateinitIntrinsicCallInInlineFunction::class
    }

    interface LateinitIntrinsicCallOnNonAccessibleProperty : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonAccessibleProperty::class
        val declaration: KtSymbol
    }

    interface LocalExtensionProperty : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LocalExtensionProperty::class
    }

    interface ExpectedDeclarationWithBody : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExpectedDeclarationWithBody::class
    }

    interface ExpectedClassConstructorDelegationCall : KtFirDiagnostic<KtConstructorDelegationCall> {
        override val diagnosticClass get() = ExpectedClassConstructorDelegationCall::class
    }

    interface ExpectedClassConstructorPropertyParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ExpectedClassConstructorPropertyParameter::class
    }

    interface ExpectedEnumConstructor : KtFirDiagnostic<KtConstructor<*>> {
        override val diagnosticClass get() = ExpectedEnumConstructor::class
    }

    interface ExpectedEnumEntryWithBody : KtFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass get() = ExpectedEnumEntryWithBody::class
    }

    interface ExpectedPropertyInitializer : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExpectedPropertyInitializer::class
    }

    interface ExpectedDelegatedProperty : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExpectedDelegatedProperty::class
    }

    interface ExpectedLateinitProperty : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ExpectedLateinitProperty::class
    }

    interface SupertypeInitializedInExpectedClass : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SupertypeInitializedInExpectedClass::class
    }

    interface ExpectedPrivateDeclaration : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ExpectedPrivateDeclaration::class
    }

    interface ExpectedExternalDeclaration : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ExpectedExternalDeclaration::class
    }

    interface ExpectedTailrecFunction : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ExpectedTailrecFunction::class
    }

    interface ImplementationByDelegationInExpectClass : KtFirDiagnostic<KtDelegatedSuperTypeEntry> {
        override val diagnosticClass get() = ImplementationByDelegationInExpectClass::class
    }

    interface ActualTypeAliasNotToClass : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasNotToClass::class
    }

    interface ActualTypeAliasToClassWithDeclarationSiteVariance : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasToClassWithDeclarationSiteVariance::class
    }

    interface ActualTypeAliasWithUseSiteVariance : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasWithUseSiteVariance::class
    }

    interface ActualTypeAliasWithComplexSubstitution : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasWithComplexSubstitution::class
    }

    interface ActualTypeAliasToNullableType : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasToNullableType::class
    }

    interface ActualTypeAliasToNothing : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasToNothing::class
    }

    interface ActualFunctionWithDefaultArguments : KtFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = ActualFunctionWithDefaultArguments::class
    }

    interface DefaultArgumentsInExpectWithActualTypealias : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = DefaultArgumentsInExpectWithActualTypealias::class
        val expectClassSymbol: KtClassLikeSymbol
        val members: List<KtCallableSymbol>
    }

    interface DefaultArgumentsInExpectActualizedByFakeOverride : KtFirDiagnostic<KtClass> {
        override val diagnosticClass get() = DefaultArgumentsInExpectActualizedByFakeOverride::class
        val expectClassSymbol: KtClassLikeSymbol
        val members: List<KtFunctionLikeSymbol>
    }

    interface ExpectedFunctionSourceWithDefaultArgumentsNotFound : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpectedFunctionSourceWithDefaultArgumentsNotFound::class
    }

    interface NoActualForExpect : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NoActualForExpect::class
        val declaration: KtSymbol
        val module: FirModuleData
        val compatibility: Map<ExpectActualCompatibility<FirBasedSymbol<*>>, List<KtSymbol>>
    }

    interface ActualWithoutExpect : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ActualWithoutExpect::class
        val declaration: KtSymbol
        val compatibility: Map<ExpectActualCompatibility<FirBasedSymbol<*>>, List<KtSymbol>>
    }

    interface AmbiguousExpects : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = AmbiguousExpects::class
        val declaration: KtSymbol
        val modules: List<FirModuleData>
    }

    interface NoActualClassMemberForExpectedClass : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NoActualClassMemberForExpectedClass::class
        val declaration: KtSymbol
        val members: List<Pair<KtSymbol, Map<MismatchOrIncompatible<FirBasedSymbol<*>>, List<KtSymbol>>>>
    }

    interface ActualMissing : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ActualMissing::class
    }

    interface ExpectActualClassifiersAreInBetaWarning : KtFirDiagnostic<KtClassLikeDeclaration> {
        override val diagnosticClass get() = ExpectActualClassifiersAreInBetaWarning::class
    }

    interface NotAMultiplatformCompilation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotAMultiplatformCompilation::class
    }

    interface ExpectActualOptInAnnotation : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualOptInAnnotation::class
    }

    interface ActualTypealiasToSpecialAnnotation : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypealiasToSpecialAnnotation::class
        val typealiasedClassId: ClassId
    }

    interface ActualAnnotationsNotMatchExpect : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ActualAnnotationsNotMatchExpect::class
        val expectSymbol: KtSymbol
        val actualSymbol: KtSymbol
        val actualAnnotationTargetSourceElement: PsiElement?
        val incompatibilityType: ExpectActualAnnotationsIncompatibilityType<FirAnnotation>
    }

    interface OptionalDeclarationOutsideOfAnnotationEntry : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptionalDeclarationOutsideOfAnnotationEntry::class
    }

    interface OptionalDeclarationUsageInNonCommonSource : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptionalDeclarationUsageInNonCommonSource::class
    }

    interface OptionalExpectationNotOnExpected : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptionalExpectationNotOnExpected::class
    }

    interface InitializerRequiredForDestructuringDeclaration : KtFirDiagnostic<KtDestructuringDeclaration> {
        override val diagnosticClass get() = InitializerRequiredForDestructuringDeclaration::class
    }

    interface ComponentFunctionMissing : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ComponentFunctionMissing::class
        val missingFunctionName: Name
        val destructingType: KtType
    }

    interface ComponentFunctionAmbiguity : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ComponentFunctionAmbiguity::class
        val functionWithAmbiguityName: Name
        val candidates: List<KtSymbol>
    }

    interface ComponentFunctionOnNullable : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ComponentFunctionOnNullable::class
        val componentFunctionName: Name
    }

    interface ComponentFunctionReturnTypeMismatch : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ComponentFunctionReturnTypeMismatch::class
        val componentFunctionName: Name
        val destructingType: KtType
        val expectedType: KtType
    }

    interface UninitializedVariable : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UninitializedVariable::class
        val variable: KtVariableSymbol
    }

    interface UninitializedParameter : KtFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass get() = UninitializedParameter::class
        val parameter: KtSymbol
    }

    interface UninitializedEnumEntry : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UninitializedEnumEntry::class
        val enumEntry: KtSymbol
    }

    interface UninitializedEnumCompanion : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UninitializedEnumCompanion::class
        val enumClass: KtClassLikeSymbol
    }

    interface ValReassignment : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ValReassignment::class
        val variable: KtVariableLikeSymbol
    }

    interface ValReassignmentViaBackingFieldError : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ValReassignmentViaBackingFieldError::class
        val property: KtVariableSymbol
    }

    interface ValReassignmentViaBackingFieldWarning : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ValReassignmentViaBackingFieldWarning::class
        val property: KtVariableSymbol
    }

    interface CapturedValInitialization : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CapturedValInitialization::class
        val property: KtVariableSymbol
    }

    interface CapturedMemberValInitialization : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CapturedMemberValInitialization::class
        val property: KtVariableSymbol
    }

    interface SetterProjectedOut : KtFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = SetterProjectedOut::class
        val property: KtVariableSymbol
    }

    interface WrongInvocationKind : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongInvocationKind::class
        val declaration: KtSymbol
        val requiredRange: EventOccurrencesRange
        val actualRange: EventOccurrencesRange
    }

    interface LeakedInPlaceLambda : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LeakedInPlaceLambda::class
        val lambda: KtSymbol
    }

    interface WrongImpliesCondition : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongImpliesCondition::class
    }

    interface VariableWithNoTypeNoInitializer : KtFirDiagnostic<KtVariableDeclaration> {
        override val diagnosticClass get() = VariableWithNoTypeNoInitializer::class
    }

    interface InitializationBeforeDeclaration : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = InitializationBeforeDeclaration::class
        val property: KtSymbol
    }

    interface UnreachableCode : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnreachableCode::class
        val reachable: List<PsiElement>
        val unreachable: List<PsiElement>
    }

    interface SenselessComparison : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = SenselessComparison::class
        val compareResult: Boolean
    }

    interface SenselessNullInWhen : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SenselessNullInWhen::class
    }

    interface TypecheckerHasRunIntoRecursiveProblem : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = TypecheckerHasRunIntoRecursiveProblem::class
    }

    interface UnsafeCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsafeCall::class
        val receiverType: KtType
        val receiverExpression: KtExpression?
    }

    interface UnsafeImplicitInvokeCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsafeImplicitInvokeCall::class
        val receiverType: KtType
    }

    interface UnsafeInfixCall : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UnsafeInfixCall::class
        val receiverType: KtType
        val receiverExpression: KtExpression
        val operator: String
        val argumentExpression: KtExpression?
    }

    interface UnsafeOperatorCall : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UnsafeOperatorCall::class
        val receiverType: KtType
        val receiverExpression: KtExpression
        val operator: String
        val argumentExpression: KtExpression?
    }

    interface IteratorOnNullable : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = IteratorOnNullable::class
    }

    interface UnnecessarySafeCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnnecessarySafeCall::class
        val receiverType: KtType
    }

    interface SafeCallWillChangeNullability : KtFirDiagnostic<KtSafeQualifiedExpression> {
        override val diagnosticClass get() = SafeCallWillChangeNullability::class
    }

    interface UnexpectedSafeCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnexpectedSafeCall::class
    }

    interface UnnecessaryNotNullAssertion : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UnnecessaryNotNullAssertion::class
        val receiverType: KtType
    }

    interface NotNullAssertionOnLambdaExpression : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NotNullAssertionOnLambdaExpression::class
    }

    interface NotNullAssertionOnCallableReference : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NotNullAssertionOnCallableReference::class
    }

    interface UselessElvis : KtFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = UselessElvis::class
        val receiverType: KtType
    }

    interface UselessElvisRightIsNull : KtFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = UselessElvisRightIsNull::class
    }

    interface CannotCheckForErased : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotCheckForErased::class
        val type: KtType
    }

    interface CastNeverSucceeds : KtFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass get() = CastNeverSucceeds::class
    }

    interface UselessCast : KtFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass get() = UselessCast::class
    }

    interface UncheckedCast : KtFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass get() = UncheckedCast::class
        val originalType: KtType
        val targetType: KtType
    }

    interface UselessIsCheck : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UselessIsCheck::class
        val compileTimeCheckResult: Boolean
    }

    interface IsEnumEntry : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = IsEnumEntry::class
    }

    interface DynamicNotAllowed : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = DynamicNotAllowed::class
    }

    interface EnumEntryAsType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = EnumEntryAsType::class
    }

    interface ExpectedCondition : KtFirDiagnostic<KtWhenCondition> {
        override val diagnosticClass get() = ExpectedCondition::class
    }

    interface NoElseInWhen : KtFirDiagnostic<KtWhenExpression> {
        override val diagnosticClass get() = NoElseInWhen::class
        val missingWhenCases: List<WhenMissingCase>
        val description: String
    }

    interface NonExhaustiveWhenStatement : KtFirDiagnostic<KtWhenExpression> {
        override val diagnosticClass get() = NonExhaustiveWhenStatement::class
        val type: String
        val missingWhenCases: List<WhenMissingCase>
    }

    interface InvalidIfAsExpression : KtFirDiagnostic<KtIfExpression> {
        override val diagnosticClass get() = InvalidIfAsExpression::class
    }

    interface ElseMisplacedInWhen : KtFirDiagnostic<KtWhenEntry> {
        override val diagnosticClass get() = ElseMisplacedInWhen::class
    }

    interface IllegalDeclarationInWhenSubject : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IllegalDeclarationInWhenSubject::class
        val illegalReason: String
    }

    interface CommaInWhenConditionWithoutArgument : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommaInWhenConditionWithoutArgument::class
    }

    interface DuplicateBranchConditionInWhen : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DuplicateBranchConditionInWhen::class
    }

    interface ConfusingBranchConditionError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConfusingBranchConditionError::class
    }

    interface ConfusingBranchConditionWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConfusingBranchConditionWarning::class
    }

    interface TypeParameterIsNotAnExpression : KtFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass get() = TypeParameterIsNotAnExpression::class
        val typeParameter: KtTypeParameterSymbol
    }

    interface TypeParameterOnLhsOfDot : KtFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass get() = TypeParameterOnLhsOfDot::class
        val typeParameter: KtTypeParameterSymbol
    }

    interface NoCompanionObject : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NoCompanionObject::class
        val klass: KtClassLikeSymbol
    }

    interface ExpressionExpectedPackageFound : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExpressionExpectedPackageFound::class
    }

    interface ErrorInContractDescription : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ErrorInContractDescription::class
        val reason: String
    }

    interface ContractNotAllowed : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ContractNotAllowed::class
        val reason: String
    }

    interface NoGetMethod : KtFirDiagnostic<KtArrayAccessExpression> {
        override val diagnosticClass get() = NoGetMethod::class
    }

    interface NoSetMethod : KtFirDiagnostic<KtArrayAccessExpression> {
        override val diagnosticClass get() = NoSetMethod::class
    }

    interface IteratorMissing : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = IteratorMissing::class
    }

    interface HasNextMissing : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = HasNextMissing::class
    }

    interface NextMissing : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NextMissing::class
    }

    interface HasNextFunctionNoneApplicable : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = HasNextFunctionNoneApplicable::class
        val candidates: List<KtSymbol>
    }

    interface NextNoneApplicable : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NextNoneApplicable::class
        val candidates: List<KtSymbol>
    }

    interface DelegateSpecialFunctionMissing : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegateSpecialFunctionMissing::class
        val expectedFunctionSignature: String
        val delegateType: KtType
        val description: String
    }

    interface DelegateSpecialFunctionAmbiguity : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegateSpecialFunctionAmbiguity::class
        val expectedFunctionSignature: String
        val candidates: List<KtSymbol>
    }

    interface DelegateSpecialFunctionNoneApplicable : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegateSpecialFunctionNoneApplicable::class
        val expectedFunctionSignature: String
        val candidates: List<KtSymbol>
    }

    interface DelegateSpecialFunctionReturnTypeMismatch : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegateSpecialFunctionReturnTypeMismatch::class
        val delegateFunction: String
        val expectedType: KtType
        val actualType: KtType
    }

    interface UnderscoreIsReserved : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnderscoreIsReserved::class
    }

    interface UnderscoreUsageWithoutBackticks : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnderscoreUsageWithoutBackticks::class
    }

    interface ResolvedToUnderscoreNamedCatchParameter : KtFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass get() = ResolvedToUnderscoreNamedCatchParameter::class
    }

    interface InvalidCharacters : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidCharacters::class
        val message: String
    }

    interface DangerousCharacters : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = DangerousCharacters::class
        val characters: String
    }

    interface EqualityNotApplicable : KtFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = EqualityNotApplicable::class
        val operator: String
        val leftType: KtType
        val rightType: KtType
    }

    interface EqualityNotApplicableWarning : KtFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = EqualityNotApplicableWarning::class
        val operator: String
        val leftType: KtType
        val rightType: KtType
    }

    interface IncompatibleEnumComparisonError : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncompatibleEnumComparisonError::class
        val leftType: KtType
        val rightType: KtType
    }

    interface IncompatibleEnumComparison : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncompatibleEnumComparison::class
        val leftType: KtType
        val rightType: KtType
    }

    interface ForbiddenIdentityEquals : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ForbiddenIdentityEquals::class
        val leftType: KtType
        val rightType: KtType
    }

    interface ForbiddenIdentityEqualsWarning : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ForbiddenIdentityEqualsWarning::class
        val leftType: KtType
        val rightType: KtType
    }

    interface DeprecatedIdentityEquals : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DeprecatedIdentityEquals::class
        val leftType: KtType
        val rightType: KtType
    }

    interface ImplicitBoxingInIdentityEquals : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ImplicitBoxingInIdentityEquals::class
        val leftType: KtType
        val rightType: KtType
    }

    interface IncDecShouldNotReturnUnit : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = IncDecShouldNotReturnUnit::class
    }

    interface AssignmentOperatorShouldReturnUnit : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssignmentOperatorShouldReturnUnit::class
        val functionSymbol: KtFunctionLikeSymbol
        val operator: String
    }

    interface PropertyAsOperator : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PropertyAsOperator::class
        val property: KtVariableSymbol
    }

    interface DslScopeViolation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DslScopeViolation::class
        val calleeSymbol: KtSymbol
    }

    interface ToplevelTypealiasesOnly : KtFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ToplevelTypealiasesOnly::class
    }

    interface RecursiveTypealiasExpansion : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = RecursiveTypealiasExpansion::class
    }

    interface TypealiasShouldExpandToClass : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = TypealiasShouldExpandToClass::class
        val expandedType: KtType
    }

    interface ConstructorOrSupertypeOnTypealiasWithTypeProjectionError : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConstructorOrSupertypeOnTypealiasWithTypeProjectionError::class
    }

    interface ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning::class
    }

    interface RedundantVisibilityModifier : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = RedundantVisibilityModifier::class
    }

    interface RedundantModalityModifier : KtFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = RedundantModalityModifier::class
    }

    interface RedundantReturnUnitType : KtFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = RedundantReturnUnitType::class
    }

    interface RedundantExplicitType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantExplicitType::class
    }

    interface RedundantSingleExpressionStringTemplate : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantSingleExpressionStringTemplate::class
    }

    interface CanBeVal : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = CanBeVal::class
    }

    interface CanBeReplacedWithOperatorAssignment : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CanBeReplacedWithOperatorAssignment::class
    }

    interface RedundantCallOfConversionMethod : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantCallOfConversionMethod::class
    }

    interface ArrayEqualityOperatorCanBeReplacedWithEquals : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ArrayEqualityOperatorCanBeReplacedWithEquals::class
    }

    interface EmptyRange : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EmptyRange::class
    }

    interface RedundantSetterParameterType : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantSetterParameterType::class
    }

    interface UnusedVariable : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = UnusedVariable::class
    }

    interface AssignedValueIsNeverRead : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AssignedValueIsNeverRead::class
    }

    interface VariableInitializerIsRedundant : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VariableInitializerIsRedundant::class
    }

    interface VariableNeverRead : KtFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VariableNeverRead::class
    }

    interface UselessCallOnNotNull : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UselessCallOnNotNull::class
    }

    interface ReturnNotAllowed : KtFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = ReturnNotAllowed::class
    }

    interface NotAFunctionLabel : KtFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = NotAFunctionLabel::class
    }

    interface ReturnInFunctionWithExpressionBody : KtFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = ReturnInFunctionWithExpressionBody::class
    }

    interface NoReturnInFunctionWithBlockBody : KtFirDiagnostic<KtDeclarationWithBody> {
        override val diagnosticClass get() = NoReturnInFunctionWithBlockBody::class
    }

    interface AnonymousInitializerInInterface : KtFirDiagnostic<KtAnonymousInitializer> {
        override val diagnosticClass get() = AnonymousInitializerInInterface::class
    }

    interface UsageIsNotInlinable : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UsageIsNotInlinable::class
        val parameter: KtSymbol
    }

    interface NonLocalReturnNotAllowed : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonLocalReturnNotAllowed::class
        val parameter: KtSymbol
    }

    interface NotYetSupportedInInline : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NotYetSupportedInInline::class
        val message: String
    }

    interface NothingToInline : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NothingToInline::class
    }

    interface NullableInlineParameter : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NullableInlineParameter::class
        val parameter: KtSymbol
        val function: KtSymbol
    }

    interface RecursionInInline : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = RecursionInInline::class
        val symbol: KtSymbol
    }

    interface NonPublicCallFromPublicInline : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonPublicCallFromPublicInline::class
        val inlineDeclaration: KtSymbol
        val referencedDeclaration: KtSymbol
    }

    interface NonPublicCallFromPublicInlineDeprecation : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonPublicCallFromPublicInlineDeprecation::class
        val inlineDeclaration: KtSymbol
        val referencedDeclaration: KtSymbol
    }

    interface ProtectedConstructorCallFromPublicInline : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ProtectedConstructorCallFromPublicInline::class
        val inlineDeclaration: KtSymbol
        val referencedDeclaration: KtSymbol
    }

    interface ProtectedCallFromPublicInlineError : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ProtectedCallFromPublicInlineError::class
        val inlineDeclaration: KtSymbol
        val referencedDeclaration: KtSymbol
    }

    interface ProtectedCallFromPublicInline : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ProtectedCallFromPublicInline::class
        val inlineDeclaration: KtSymbol
        val referencedDeclaration: KtSymbol
    }

    interface PrivateClassMemberFromInline : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = PrivateClassMemberFromInline::class
        val inlineDeclaration: KtSymbol
        val referencedDeclaration: KtSymbol
    }

    interface SuperCallFromPublicInline : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SuperCallFromPublicInline::class
        val symbol: KtSymbol
    }

    interface DeclarationCantBeInlined : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = DeclarationCantBeInlined::class
    }

    interface DeclarationCantBeInlinedDeprecationError : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = DeclarationCantBeInlinedDeprecationError::class
    }

    interface DeclarationCantBeInlinedDeprecationWarning : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = DeclarationCantBeInlinedDeprecationWarning::class
    }

    interface OverrideByInline : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = OverrideByInline::class
    }

    interface NonInternalPublishedApi : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonInternalPublishedApi::class
    }

    interface InvalidDefaultFunctionalParameterForInline : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InvalidDefaultFunctionalParameterForInline::class
        val parameter: KtSymbol
    }

    interface NotSupportedInlineParameterInInlineParameterDefaultValue : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NotSupportedInlineParameterInInlineParameterDefaultValue::class
        val parameter: KtSymbol
    }

    interface ReifiedTypeParameterInOverride : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ReifiedTypeParameterInOverride::class
    }

    interface InlinePropertyWithBackingField : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InlinePropertyWithBackingField::class
    }

    interface InlinePropertyWithBackingFieldDeprecationError : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InlinePropertyWithBackingFieldDeprecationError::class
    }

    interface InlinePropertyWithBackingFieldDeprecationWarning : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InlinePropertyWithBackingFieldDeprecationWarning::class
    }

    interface IllegalInlineParameterModifier : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IllegalInlineParameterModifier::class
    }

    interface InlineSuspendFunctionTypeUnsupported : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = InlineSuspendFunctionTypeUnsupported::class
    }

    interface InefficientEqualsOverridingInValueClass : KtFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = InefficientEqualsOverridingInValueClass::class
        val type: KtType
    }

    interface CannotAllUnderImportFromSingleton : KtFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = CannotAllUnderImportFromSingleton::class
        val objectName: Name
    }

    interface PackageCannotBeImported : KtFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = PackageCannotBeImported::class
    }

    interface CannotBeImported : KtFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = CannotBeImported::class
        val name: Name
    }

    interface ConflictingImport : KtFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = ConflictingImport::class
        val name: Name
    }

    interface OperatorRenamedOnImport : KtFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = OperatorRenamedOnImport::class
    }

    interface TypealiasAsCallableQualifierInImportError : KtFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = TypealiasAsCallableQualifierInImportError::class
        val typealiasName: Name
        val originalClassName: Name
    }

    interface TypealiasAsCallableQualifierInImportWarning : KtFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = TypealiasAsCallableQualifierInImportWarning::class
        val typealiasName: Name
        val originalClassName: Name
    }

    interface IllegalSuspendFunctionCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalSuspendFunctionCall::class
        val suspendCallable: KtSymbol
    }

    interface IllegalSuspendPropertyAccess : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalSuspendPropertyAccess::class
        val suspendCallable: KtSymbol
    }

    interface NonLocalSuspensionPoint : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonLocalSuspensionPoint::class
    }

    interface IllegalRestrictedSuspendingFunctionCall : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalRestrictedSuspendingFunctionCall::class
    }

    interface NonModifierFormForBuiltInSuspend : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonModifierFormForBuiltInSuspend::class
    }

    interface ModifierFormForNonBuiltInSuspend : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspend::class
    }

    interface ModifierFormForNonBuiltInSuspendFunError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspendFunError::class
    }

    interface ModifierFormForNonBuiltInSuspendFunWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspendFunWarning::class
    }

    interface ReturnForBuiltInSuspend : KtFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = ReturnForBuiltInSuspend::class
    }

    interface MixingSuspendAndNonSuspendSupertypes : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MixingSuspendAndNonSuspendSupertypes::class
    }

    interface MixingFunctionalKindsInSupertypes : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MixingFunctionalKindsInSupertypes::class
        val kinds: List<FunctionTypeKind>
    }

    interface RedundantLabelWarning : KtFirDiagnostic<KtLabelReferenceExpression> {
        override val diagnosticClass get() = RedundantLabelWarning::class
    }

    interface MultipleLabelsAreForbidden : KtFirDiagnostic<KtLabelReferenceExpression> {
        override val diagnosticClass get() = MultipleLabelsAreForbidden::class
    }

    interface DeprecatedAccessToEnumEntryCompanionProperty : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedAccessToEnumEntryCompanionProperty::class
    }

    interface DeprecatedAccessToEntryPropertyFromEnum : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedAccessToEntryPropertyFromEnum::class
    }

    interface DeprecatedAccessToEnumEntryPropertyAsReference : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedAccessToEnumEntryPropertyAsReference::class
    }

    interface DeprecatedDeclarationOfEnumEntry : KtFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass get() = DeprecatedDeclarationOfEnumEntry::class
    }

    interface IncompatibleClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IncompatibleClass::class
        val presentableString: String
        val incompatibility: IncompatibleVersionErrorData<*>
    }

    interface PreReleaseClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PreReleaseClass::class
        val presentableString: String
    }

    interface IrWithUnstableAbiCompiledClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IrWithUnstableAbiCompiledClass::class
        val presentableString: String
    }

    interface BuilderInferenceStubReceiver : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = BuilderInferenceStubReceiver::class
        val typeParameterName: Name
        val containingDeclarationName: Name
    }

    interface BuilderInferenceMultiLambdaRestriction : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = BuilderInferenceMultiLambdaRestriction::class
        val typeParameterName: Name
        val containingDeclarationName: Name
    }

    interface OverrideCannotBeStatic : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OverrideCannotBeStatic::class
    }

    interface JvmStaticNotInObjectOrClassCompanion : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticNotInObjectOrClassCompanion::class
    }

    interface JvmStaticNotInObjectOrCompanion : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticNotInObjectOrCompanion::class
    }

    interface JvmStaticOnNonPublicMember : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticOnNonPublicMember::class
    }

    interface JvmStaticOnConstOrJvmField : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticOnConstOrJvmField::class
    }

    interface JvmStaticOnExternalInInterface : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticOnExternalInInterface::class
    }

    interface InapplicableJvmName : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableJvmName::class
    }

    interface IllegalJvmName : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalJvmName::class
    }

    interface FunctionDelegateMemberNameClash : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FunctionDelegateMemberNameClash::class
    }

    interface ValueClassWithoutJvmInlineAnnotation : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ValueClassWithoutJvmInlineAnnotation::class
    }

    interface JvmInlineWithoutValueClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmInlineWithoutValueClass::class
    }

    interface WrongNullabilityForJavaOverride : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongNullabilityForJavaOverride::class
        val override: KtCallableSymbol
        val base: KtCallableSymbol
    }

    interface AccidentalOverrideClashByJvmSignature : KtFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = AccidentalOverrideClashByJvmSignature::class
        val hidden: KtFunctionLikeSymbol
        val overrideDescription: String
        val regular: KtFunctionLikeSymbol
    }

    interface JavaTypeMismatch : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = JavaTypeMismatch::class
        val expectedType: KtType
        val actualType: KtType
    }

    interface ReceiverNullabilityMismatchBasedOnJavaAnnotations : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ReceiverNullabilityMismatchBasedOnJavaAnnotations::class
        val actualType: KtType
        val expectedType: KtType
        val messageSuffix: String
    }

    interface NullabilityMismatchBasedOnJavaAnnotations : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NullabilityMismatchBasedOnJavaAnnotations::class
        val actualType: KtType
        val expectedType: KtType
        val messageSuffix: String
    }

    interface UpperBoundCannotBeArray : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundCannotBeArray::class
    }

    interface UpperBoundViolatedBasedOnJavaAnnotations : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolatedBasedOnJavaAnnotations::class
        val expectedUpperBound: KtType
        val actualUpperBound: KtType
    }

    interface UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations::class
        val expectedUpperBound: KtType
        val actualUpperBound: KtType
    }

    interface StrictfpOnClass : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = StrictfpOnClass::class
    }

    interface SynchronizedOnAbstract : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnAbstract::class
    }

    interface SynchronizedInInterface : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedInInterface::class
    }

    interface SynchronizedOnInline : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnInline::class
    }

    interface SynchronizedOnSuspendError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnSuspendError::class
    }

    interface SynchronizedOnSuspendWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnSuspendWarning::class
    }

    interface OverloadsWithoutDefaultArguments : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsWithoutDefaultArguments::class
    }

    interface OverloadsAbstract : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsAbstract::class
    }

    interface OverloadsInterface : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsInterface::class
    }

    interface OverloadsLocal : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsLocal::class
    }

    interface OverloadsAnnotationClassConstructorError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsAnnotationClassConstructorError::class
    }

    interface OverloadsAnnotationClassConstructorWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsAnnotationClassConstructorWarning::class
    }

    interface OverloadsPrivate : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsPrivate::class
    }

    interface DeprecatedJavaAnnotation : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = DeprecatedJavaAnnotation::class
        val kotlinName: FqName
    }

    interface JvmPackageNameCannotBeEmpty : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmPackageNameCannotBeEmpty::class
    }

    interface JvmPackageNameMustBeValidName : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmPackageNameMustBeValidName::class
    }

    interface JvmPackageNameNotSupportedInFilesWithClasses : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmPackageNameNotSupportedInFilesWithClasses::class
    }

    interface PositionedValueArgumentForJavaAnnotation : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = PositionedValueArgumentForJavaAnnotation::class
    }

    interface RedundantRepeatableAnnotation : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RedundantRepeatableAnnotation::class
        val kotlinRepeatable: FqName
        val javaRepeatable: FqName
    }

    interface LocalJvmRecord : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LocalJvmRecord::class
    }

    interface NonFinalJvmRecord : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonFinalJvmRecord::class
    }

    interface EnumJvmRecord : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EnumJvmRecord::class
    }

    interface JvmRecordWithoutPrimaryConstructorParameters : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmRecordWithoutPrimaryConstructorParameters::class
    }

    interface NonDataClassJvmRecord : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonDataClassJvmRecord::class
    }

    interface JvmRecordNotValParameter : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmRecordNotValParameter::class
    }

    interface JvmRecordNotLastVarargParameter : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmRecordNotLastVarargParameter::class
    }

    interface InnerJvmRecord : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InnerJvmRecord::class
    }

    interface FieldInJvmRecord : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FieldInJvmRecord::class
    }

    interface DelegationByInJvmRecord : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegationByInJvmRecord::class
    }

    interface JvmRecordExtendsClass : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmRecordExtendsClass::class
        val superType: KtType
    }

    interface IllegalJavaLangRecordSupertype : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalJavaLangRecordSupertype::class
    }

    interface JvmDefaultInDeclaration : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JvmDefaultInDeclaration::class
        val annotation: String
    }

    interface JvmDefaultWithCompatibilityInDeclaration : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JvmDefaultWithCompatibilityInDeclaration::class
    }

    interface JvmDefaultWithCompatibilityNotOnInterface : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JvmDefaultWithCompatibilityNotOnInterface::class
    }

    interface ExternalDeclarationCannotBeAbstract : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExternalDeclarationCannotBeAbstract::class
    }

    interface ExternalDeclarationCannotHaveBody : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExternalDeclarationCannotHaveBody::class
    }

    interface ExternalDeclarationInInterface : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExternalDeclarationInInterface::class
    }

    interface ExternalDeclarationCannotBeInlined : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExternalDeclarationCannotBeInlined::class
    }

    interface NonSourceRepeatedAnnotation : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = NonSourceRepeatedAnnotation::class
    }

    interface RepeatedAnnotationWithContainer : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatedAnnotationWithContainer::class
        val name: ClassId
        val explicitContainerName: ClassId
    }

    interface RepeatableContainerMustHaveValueArrayError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerMustHaveValueArrayError::class
        val container: ClassId
        val annotation: ClassId
    }

    interface RepeatableContainerMustHaveValueArrayWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerMustHaveValueArrayWarning::class
        val container: ClassId
        val annotation: ClassId
    }

    interface RepeatableContainerHasNonDefaultParameterError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerHasNonDefaultParameterError::class
        val container: ClassId
        val nonDefault: Name
    }

    interface RepeatableContainerHasNonDefaultParameterWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerHasNonDefaultParameterWarning::class
        val container: ClassId
        val nonDefault: Name
    }

    interface RepeatableContainerHasShorterRetentionError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerHasShorterRetentionError::class
        val container: ClassId
        val retention: String
        val annotation: ClassId
        val annotationRetention: String
    }

    interface RepeatableContainerHasShorterRetentionWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerHasShorterRetentionWarning::class
        val container: ClassId
        val retention: String
        val annotation: ClassId
        val annotationRetention: String
    }

    interface RepeatableContainerTargetSetNotASubsetError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerTargetSetNotASubsetError::class
        val container: ClassId
        val annotation: ClassId
    }

    interface RepeatableContainerTargetSetNotASubsetWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerTargetSetNotASubsetWarning::class
        val container: ClassId
        val annotation: ClassId
    }

    interface RepeatableAnnotationHasNestedClassNamedContainerError : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableAnnotationHasNestedClassNamedContainerError::class
    }

    interface RepeatableAnnotationHasNestedClassNamedContainerWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableAnnotationHasNestedClassNamedContainerWarning::class
    }

    interface SuspensionPointInsideCriticalSection : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuspensionPointInsideCriticalSection::class
        val function: KtCallableSymbol
    }

    interface InapplicableJvmField : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableJvmField::class
        val message: String
    }

    interface InapplicableJvmFieldWarning : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableJvmFieldWarning::class
        val message: String
    }

    interface JvmSyntheticOnDelegate : KtFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmSyntheticOnDelegate::class
    }

    interface SubclassCantCallCompanionProtectedNonStatic : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SubclassCantCallCompanionProtectedNonStatic::class
    }

    interface ConcurrentHashMapContainsOperatorError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConcurrentHashMapContainsOperatorError::class
    }

    interface ConcurrentHashMapContainsOperatorWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConcurrentHashMapContainsOperatorWarning::class
    }

    interface SpreadOnSignaturePolymorphicCallError : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpreadOnSignaturePolymorphicCallError::class
    }

    interface SpreadOnSignaturePolymorphicCallWarning : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpreadOnSignaturePolymorphicCallWarning::class
    }

    interface JavaSamInterfaceConstructorReference : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaSamInterfaceConstructorReference::class
    }

    interface NoReflectionInClassPath : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoReflectionInClassPath::class
    }

    interface SyntheticPropertyWithoutJavaOrigin : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SyntheticPropertyWithoutJavaOrigin::class
        val originalSymbol: KtFunctionLikeSymbol
        val functionName: Name
    }

    interface ImplementingFunctionInterface : KtFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ImplementingFunctionInterface::class
    }

    interface OverridingExternalFunWithOptionalParams : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = OverridingExternalFunWithOptionalParams::class
    }

    interface OverridingExternalFunWithOptionalParamsWithFake : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = OverridingExternalFunWithOptionalParamsWithFake::class
        val function: KtFunctionLikeSymbol
    }

    interface CallToDefinedExternallyFromNonExternalDeclaration : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CallToDefinedExternallyFromNonExternalDeclaration::class
    }

    interface ExternalEnumEntryWithBody : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalEnumEntryWithBody::class
    }

    interface ExternalTypeExtendsNonExternalType : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalTypeExtendsNonExternalType::class
    }

    interface EnumClassInExternalDeclarationWarning : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = EnumClassInExternalDeclarationWarning::class
    }

    interface InlineClassInExternalDeclarationWarning : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InlineClassInExternalDeclarationWarning::class
    }

    interface InlineClassInExternalDeclaration : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InlineClassInExternalDeclaration::class
    }

    interface ExtensionFunctionInExternalDeclaration : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExtensionFunctionInExternalDeclaration::class
    }

    interface NonExternalDeclarationInInappropriateFile : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonExternalDeclarationInInappropriateFile::class
        val type: KtType
    }

    interface JsExternalInheritorsOnly : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = JsExternalInheritorsOnly::class
        val parent: KtClassLikeSymbol
        val kid: KtClassLikeSymbol
    }

    interface JsExternalArgument : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = JsExternalArgument::class
        val argType: KtType
    }

    interface WrongExportedDeclaration : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongExportedDeclaration::class
        val kind: String
    }

    interface NonExportableType : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonExportableType::class
        val kind: String
        val type: KtType
    }

    interface NonConsumableExportedIdentifier : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonConsumableExportedIdentifier::class
        val name: String
    }

    interface NestedJsExport : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NestedJsExport::class
    }

    interface DelegationByDynamic : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DelegationByDynamic::class
    }

    interface PropertyDelegationByDynamic : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = PropertyDelegationByDynamic::class
    }

    interface SpreadOperatorInDynamicCall : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SpreadOperatorInDynamicCall::class
    }

    interface WrongOperationWithDynamic : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongOperationWithDynamic::class
        val operation: String
    }

    interface Syntax : KtFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = Syntax::class
        val message: String
    }

    interface NestedExternalDeclaration : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NestedExternalDeclaration::class
    }

    interface WrongExternalDeclaration : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = WrongExternalDeclaration::class
        val classKind: String
    }

    interface NestedClassInExternalInterface : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NestedClassInExternalInterface::class
    }

    interface InlineExternalDeclaration : KtFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InlineExternalDeclaration::class
    }

    interface NonAbstractMemberOfExternalInterface : KtFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NonAbstractMemberOfExternalInterface::class
    }

    interface ExternalClassConstructorPropertyParameter : KtFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ExternalClassConstructorPropertyParameter::class
    }

    interface ExternalAnonymousInitializer : KtFirDiagnostic<KtAnonymousInitializer> {
        override val diagnosticClass get() = ExternalAnonymousInitializer::class
    }

    interface ExternalDelegation : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalDelegation::class
    }

    interface ExternalDelegatedConstructorCall : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalDelegatedConstructorCall::class
    }

    interface WrongBodyOfExternalDeclaration : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongBodyOfExternalDeclaration::class
    }

    interface WrongInitializerOfExternalDeclaration : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongInitializerOfExternalDeclaration::class
    }

    interface WrongDefaultValueForExternalFunParameter : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongDefaultValueForExternalFunParameter::class
    }

    interface CannotCheckForExternalInterface : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CannotCheckForExternalInterface::class
        val targetType: KtType
    }

    interface UncheckedCastToExternalInterface : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UncheckedCastToExternalInterface::class
        val sourceType: KtType
        val targetType: KtType
    }

    interface ExternalInterfaceAsClassLiteral : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalInterfaceAsClassLiteral::class
    }

    interface ExternalInterfaceAsReifiedTypeArgument : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalInterfaceAsReifiedTypeArgument::class
        val typeArgument: KtType
    }

    interface JscodeArgumentNonConstExpression : KtFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JscodeArgumentNonConstExpression::class
    }

}
