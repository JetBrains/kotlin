/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
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

sealed interface KaFirDiagnostic<PSI : PsiElement> : KaDiagnosticWithPsi<PSI> {
    interface Unsupported : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = Unsupported::class
        val unsupported: String
    }

    interface UnsupportedFeature : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedFeature::class
        val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>
    }

    interface UnsupportedSuspendTest : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedSuspendTest::class
    }

    interface NewInferenceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NewInferenceError::class
        val error: String
    }

    interface OtherError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OtherError::class
    }

    interface OtherErrorWithReason : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OtherErrorWithReason::class
        val reason: String
    }

    interface IllegalConstExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalConstExpression::class
    }

    interface IllegalUnderscore : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalUnderscore::class
    }

    interface ExpressionExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpressionExpected::class
    }

    interface AssignmentInExpressionContext : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = AssignmentInExpressionContext::class
    }

    interface BreakOrContinueOutsideALoop : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = BreakOrContinueOutsideALoop::class
    }

    interface NotALoopLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotALoopLabel::class
    }

    interface BreakOrContinueJumpsAcrossFunctionBoundary : KaFirDiagnostic<KtExpressionWithLabel> {
        override val diagnosticClass get() = BreakOrContinueJumpsAcrossFunctionBoundary::class
    }

    interface VariableExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VariableExpected::class
    }

    interface DelegationInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegationInInterface::class
    }

    interface DelegationNotToInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegationNotToInterface::class
    }

    interface NestedClassNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NestedClassNotAllowed::class
        val declaration: String
    }

    interface NestedClassNotAllowedInLocalError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NestedClassNotAllowedInLocalError::class
        val declaration: String
    }

    interface NestedClassNotAllowedInLocalWarning : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NestedClassNotAllowedInLocalWarning::class
        val declaration: String
    }

    interface IncorrectCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IncorrectCharacterLiteral::class
    }

    interface EmptyCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EmptyCharacterLiteral::class
    }

    interface TooManyCharactersInCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TooManyCharactersInCharacterLiteral::class
    }

    interface IllegalEscape : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalEscape::class
    }

    interface IntLiteralOutOfRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IntLiteralOutOfRange::class
    }

    interface FloatLiteralOutOfRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FloatLiteralOutOfRange::class
    }

    interface WrongLongSuffix : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongLongSuffix::class
    }

    interface UnsignedLiteralWithoutDeclarationsOnClasspath : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnsignedLiteralWithoutDeclarationsOnClasspath::class
    }

    interface DivisionByZero : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DivisionByZero::class
    }

    interface ValOrVarOnLoopParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValOrVarOnLoopParameter::class
        val valOrVar: KtKeywordToken
    }

    interface ValOrVarOnFunParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValOrVarOnFunParameter::class
        val valOrVar: KtKeywordToken
    }

    interface ValOrVarOnCatchParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValOrVarOnCatchParameter::class
        val valOrVar: KtKeywordToken
    }

    interface ValOrVarOnSecondaryConstructorParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValOrVarOnSecondaryConstructorParameter::class
        val valOrVar: KtKeywordToken
    }

    interface InvisibleSetter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvisibleSetter::class
        val property: KaVariableSymbol
        val visibility: Visibility
        val callableId: CallableId
    }

    interface InnerOnTopLevelScriptClassError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InnerOnTopLevelScriptClassError::class
    }

    interface InnerOnTopLevelScriptClassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InnerOnTopLevelScriptClassWarning::class
    }

    interface ErrorSuppression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ErrorSuppression::class
        val diagnosticName: String
    }

    interface MissingConstructorKeyword : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingConstructorKeyword::class
    }

    interface RedundantInterpolationPrefix : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantInterpolationPrefix::class
    }

    interface WrappedLhsInAssignmentError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrappedLhsInAssignmentError::class
    }

    interface WrappedLhsInAssignmentWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrappedLhsInAssignmentWarning::class
    }

    interface InvisibleReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvisibleReference::class
        val reference: KaSymbol
        val visible: Visibility
        val containingDeclaration: ClassId?
    }

    interface UnresolvedReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedReference::class
        val reference: String
        val operator: String?
    }

    interface UnresolvedLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedLabel::class
    }

    interface AmbiguousLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousLabel::class
    }

    interface LabelNameClash : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LabelNameClash::class
    }

    interface DeserializationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeserializationError::class
    }

    interface ErrorFromJavaResolution : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ErrorFromJavaResolution::class
    }

    interface MissingStdlibClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingStdlibClass::class
    }

    interface NoThis : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoThis::class
    }

    interface DeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecationError::class
        val reference: KaSymbol
        val message: String
    }

    interface Deprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = Deprecation::class
        val reference: KaSymbol
        val message: String
    }

    interface VersionRequirementDeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VersionRequirementDeprecationError::class
        val reference: KaSymbol
        val version: Version
        val currentVersion: String
        val message: String
    }

    interface VersionRequirementDeprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VersionRequirementDeprecation::class
        val reference: KaSymbol
        val version: Version
        val currentVersion: String
        val message: String
    }

    interface TypealiasExpansionDeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypealiasExpansionDeprecationError::class
        val alias: KaSymbol
        val reference: KaSymbol
        val message: String
    }

    interface TypealiasExpansionDeprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypealiasExpansionDeprecation::class
        val alias: KaSymbol
        val reference: KaSymbol
        val message: String
    }

    interface ApiNotAvailable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ApiNotAvailable::class
        val sinceKotlinVersion: ApiVersion
        val currentVersion: ApiVersion
    }

    interface UnresolvedReferenceWrongReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedReferenceWrongReceiver::class
        val candidates: List<KaSymbol>
    }

    interface UnresolvedImport : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedImport::class
        val reference: String
    }

    interface PlaceholderProjectionInQualifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PlaceholderProjectionInQualifier::class
    }

    interface DuplicateParameterNameInFunctionType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DuplicateParameterNameInFunctionType::class
    }

    interface MissingDependencyClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencyClass::class
        val type: KaType
    }

    interface MissingDependencyClassInExpressionType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencyClassInExpressionType::class
        val type: KaType
    }

    interface MissingDependencySuperclass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencySuperclass::class
        val missingType: KaType
        val declarationType: KaType
    }

    interface MissingDependencySuperclassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencySuperclassWarning::class
        val missingType: KaType
        val declarationType: KaType
    }

    interface MissingDependencySuperclassInTypeArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencySuperclassInTypeArgument::class
        val missingType: KaType
        val declarationType: KaType
    }

    interface MissingDependencyClassInLambdaParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencyClassInLambdaParameter::class
        val type: KaType
        val parameterName: Name
    }

    interface MissingDependencyClassInLambdaReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingDependencyClassInLambdaReceiver::class
        val type: KaType
    }

    interface CreatingAnInstanceOfAbstractClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CreatingAnInstanceOfAbstractClass::class
    }

    interface NoConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoConstructor::class
    }

    interface FunctionCallExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FunctionCallExpected::class
        val functionName: String
        val hasValueParameters: Boolean
    }

    interface IllegalSelector : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalSelector::class
    }

    interface NoReceiverAllowed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoReceiverAllowed::class
    }

    interface FunctionExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FunctionExpected::class
        val expression: String
        val type: KaType
    }

    interface InterfaceAsFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InterfaceAsFunction::class
        val classSymbol: KaClassLikeSymbol
    }

    interface ExpectClassAsFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpectClassAsFunction::class
        val classSymbol: KaClassLikeSymbol
    }

    interface InnerClassConstructorNoReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InnerClassConstructorNoReceiver::class
        val classSymbol: KaClassLikeSymbol
    }

    interface PluginAmbiguousInterceptedSymbol : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PluginAmbiguousInterceptedSymbol::class
        val names: List<String>
    }

    interface ResolutionToClassifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ResolutionToClassifier::class
        val classSymbol: KaClassLikeSymbol
    }

    interface AmbiguousAlteredAssign : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousAlteredAssign::class
        val altererNames: List<String?>
    }

    interface SelfCallInNestedObjectConstructorError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SelfCallInNestedObjectConstructorError::class
    }

    interface SuperIsNotAnExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuperIsNotAnExpression::class
    }

    interface SuperNotAvailable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuperNotAvailable::class
    }

    interface AbstractSuperCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AbstractSuperCall::class
    }

    interface AbstractSuperCallWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AbstractSuperCallWarning::class
    }

    interface InstanceAccessBeforeSuperCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InstanceAccessBeforeSuperCall::class
        val target: String
    }

    interface SuperCallWithDefaultParameters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuperCallWithDefaultParameters::class
        val name: String
    }

    interface InterfaceCantCallDefaultMethodViaSuper : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InterfaceCantCallDefaultMethodViaSuper::class
    }

    interface JavaClassInheritsKtPrivateClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JavaClassInheritsKtPrivateClass::class
        val javaClassId: ClassId
        val privateKotlinType: KaType
    }

    interface NotASupertype : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotASupertype::class
    }

    interface TypeArgumentsRedundantInSuperQualifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = TypeArgumentsRedundantInSuperQualifier::class
    }

    interface SuperclassNotAccessibleFromInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuperclassNotAccessibleFromInterface::class
    }

    interface SupertypeInitializedInInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SupertypeInitializedInInterface::class
    }

    interface InterfaceWithSuperclass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InterfaceWithSuperclass::class
    }

    interface FinalSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = FinalSupertype::class
    }

    interface ClassCannotBeExtendedDirectly : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ClassCannotBeExtendedDirectly::class
        val classSymbol: KaClassLikeSymbol
    }

    interface SupertypeIsExtensionOrContextFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SupertypeIsExtensionOrContextFunctionType::class
    }

    interface SingletonInSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SingletonInSupertype::class
    }

    interface NullableSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NullableSupertype::class
    }

    interface NullableSupertypeThroughTypealiasError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NullableSupertypeThroughTypealiasError::class
    }

    interface NullableSupertypeThroughTypealiasWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NullableSupertypeThroughTypealiasWarning::class
    }

    interface ManyClassesInSupertypeList : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ManyClassesInSupertypeList::class
    }

    interface SupertypeAppearsTwice : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SupertypeAppearsTwice::class
    }

    interface ClassInSupertypeForEnum : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ClassInSupertypeForEnum::class
    }

    interface SealedSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SealedSupertype::class
    }

    interface SealedSupertypeInLocalClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SealedSupertypeInLocalClass::class
        val declarationType: String
        val sealedClassKind: ClassKind
    }

    interface SealedInheritorInDifferentPackage : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SealedInheritorInDifferentPackage::class
    }

    interface SealedInheritorInDifferentModule : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SealedInheritorInDifferentModule::class
    }

    interface ClassInheritsJavaSealedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ClassInheritsJavaSealedClass::class
    }

    interface UnsupportedSealedFunInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedSealedFunInterface::class
    }

    interface SupertypeNotAClassOrInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SupertypeNotAClassOrInterface::class
        val reason: String
    }

    interface UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction::class
        val symbol: KaSymbol
    }

    interface CyclicInheritanceHierarchy : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CyclicInheritanceHierarchy::class
    }

    interface ExpandedTypeCannotBeInherited : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ExpandedTypeCannotBeInherited::class
        val type: KaType
    }

    interface ProjectionInImmediateArgumentToSupertype : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ProjectionInImmediateArgumentToSupertype::class
    }

    interface InconsistentTypeParameterValues : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = InconsistentTypeParameterValues::class
        val typeParameter: KaTypeParameterSymbol
        val type: KaClassLikeSymbol
        val bounds: List<KaType>
    }

    interface InconsistentTypeParameterBounds : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InconsistentTypeParameterBounds::class
        val typeParameter: KaTypeParameterSymbol
        val type: KaClassLikeSymbol
        val bounds: List<KaType>
    }

    interface AmbiguousSuper : KaFirDiagnostic<KtSuperExpression> {
        override val diagnosticClass get() = AmbiguousSuper::class
        val candidates: List<KaType>
    }

    interface WrongMultipleInheritance : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongMultipleInheritance::class
        val symbol: KaCallableSymbol
    }

    interface ConstructorInObject : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ConstructorInObject::class
    }

    interface ConstructorInInterface : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ConstructorInInterface::class
    }

    interface NonPrivateConstructorInEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonPrivateConstructorInEnum::class
    }

    interface NonPrivateOrProtectedConstructorInSealed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonPrivateOrProtectedConstructorInSealed::class
    }

    interface CyclicConstructorDelegationCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CyclicConstructorDelegationCall::class
    }

    interface PrimaryConstructorDelegationCallExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PrimaryConstructorDelegationCallExpected::class
    }

    interface ProtectedConstructorNotInSuperCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ProtectedConstructorNotInSuperCall::class
        val symbol: KaSymbol
    }

    interface SupertypeNotInitialized : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SupertypeNotInitialized::class
    }

    interface SupertypeInitializedWithoutPrimaryConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SupertypeInitializedWithoutPrimaryConstructor::class
    }

    interface DelegationSuperCallInEnumConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegationSuperCallInEnumConstructor::class
    }

    interface ExplicitDelegationCallRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExplicitDelegationCallRequired::class
    }

    interface SealedClassConstructorCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SealedClassConstructorCall::class
    }

    interface DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations::class
    }

    interface DataClassConsistentCopyWrongAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = DataClassConsistentCopyWrongAnnotationTarget::class
    }

    interface DataClassCopyVisibilityWillBeChangedError : KaFirDiagnostic<KtPrimaryConstructor> {
        override val diagnosticClass get() = DataClassCopyVisibilityWillBeChangedError::class
    }

    interface DataClassCopyVisibilityWillBeChangedWarning : KaFirDiagnostic<KtPrimaryConstructor> {
        override val diagnosticClass get() = DataClassCopyVisibilityWillBeChangedWarning::class
    }

    interface DataClassInvisibleCopyUsageError : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass get() = DataClassInvisibleCopyUsageError::class
    }

    interface DataClassInvisibleCopyUsageWarning : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass get() = DataClassInvisibleCopyUsageWarning::class
    }

    interface DataClassWithoutParameters : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = DataClassWithoutParameters::class
    }

    interface DataClassVarargParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = DataClassVarargParameter::class
    }

    interface DataClassNotPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = DataClassNotPropertyParameter::class
    }

    interface AnnotationArgumentKclassLiteralOfTypeParameterError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationArgumentKclassLiteralOfTypeParameterError::class
    }

    interface AnnotationArgumentMustBeConst : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationArgumentMustBeConst::class
    }

    interface AnnotationArgumentMustBeEnumConst : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationArgumentMustBeEnumConst::class
    }

    interface AnnotationArgumentMustBeKclassLiteral : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationArgumentMustBeKclassLiteral::class
    }

    interface AnnotationClassMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationClassMember::class
    }

    interface AnnotationParameterDefaultValueMustBeConstant : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AnnotationParameterDefaultValueMustBeConstant::class
    }

    interface InvalidTypeOfAnnotationMember : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InvalidTypeOfAnnotationMember::class
    }

    interface ProjectionInTypeOfAnnotationMemberError : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ProjectionInTypeOfAnnotationMemberError::class
    }

    interface ProjectionInTypeOfAnnotationMemberWarning : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ProjectionInTypeOfAnnotationMemberWarning::class
    }

    interface LocalAnnotationClassError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = LocalAnnotationClassError::class
    }

    interface MissingValOnAnnotationParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = MissingValOnAnnotationParameter::class
    }

    interface NonConstValUsedInConstantExpression : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NonConstValUsedInConstantExpression::class
    }

    interface CycleInAnnotationParameterError : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = CycleInAnnotationParameterError::class
    }

    interface CycleInAnnotationParameterWarning : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = CycleInAnnotationParameterWarning::class
    }

    interface AnnotationClassConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = AnnotationClassConstructorCall::class
    }

    interface EnumClassConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = EnumClassConstructorCall::class
    }

    interface NotAnAnnotationClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotAnAnnotationClass::class
        val annotationName: String
    }

    interface NullableTypeOfAnnotationMember : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NullableTypeOfAnnotationMember::class
    }

    interface VarAnnotationParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = VarAnnotationParameter::class
    }

    interface SupertypesForAnnotationClass : KaFirDiagnostic<KtClass> {
        override val diagnosticClass get() = SupertypesForAnnotationClass::class
    }

    interface AnnotationUsedAsAnnotationArgument : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationUsedAsAnnotationArgument::class
    }

    interface AnnotationOnAnnotationArgument : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationOnAnnotationArgument::class
    }

    interface IllegalKotlinVersionStringValue : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = IllegalKotlinVersionStringValue::class
    }

    interface NewerVersionInSinceKotlin : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NewerVersionInSinceKotlin::class
        val specifiedVersion: String
    }

    interface DeprecatedSinceKotlinWithUnorderedVersions : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithUnorderedVersions::class
    }

    interface DeprecatedSinceKotlinWithoutArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithoutArguments::class
    }

    interface DeprecatedSinceKotlinWithoutDeprecated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithoutDeprecated::class
    }

    interface DeprecatedSinceKotlinWithDeprecatedLevel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinWithDeprecatedLevel::class
    }

    interface DeprecatedSinceKotlinOutsideKotlinSubpackage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedSinceKotlinOutsideKotlinSubpackage::class
    }

    interface KotlinActualAnnotationHasNoEffectInKotlin : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = KotlinActualAnnotationHasNoEffectInKotlin::class
    }

    interface OverrideDeprecation : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = OverrideDeprecation::class
        val overridenSymbol: KaSymbol
        val deprecationInfo: FirDeprecationInfo
    }

    interface RedundantAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RedundantAnnotation::class
        val annotation: ClassId
    }

    interface AnnotationOnSuperclassError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationOnSuperclassError::class
    }

    interface RestrictedRetentionForExpressionAnnotationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RestrictedRetentionForExpressionAnnotationError::class
    }

    interface WrongAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongAnnotationTarget::class
        val actualTarget: String
        val allowedTargets: List<KotlinTarget>
    }

    interface WrongAnnotationTargetWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongAnnotationTargetWarning::class
        val actualTarget: String
        val allowedTargets: List<KotlinTarget>
    }

    interface WrongAnnotationTargetWithUseSiteTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongAnnotationTargetWithUseSiteTarget::class
        val actualTarget: String
        val useSiteTarget: String
        val allowedTargets: List<KotlinTarget>
    }

    interface AnnotationWithUseSiteTargetOnExpressionError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationWithUseSiteTargetOnExpressionError::class
    }

    interface AnnotationWithUseSiteTargetOnExpressionWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationWithUseSiteTargetOnExpressionWarning::class
    }

    interface InapplicableTargetOnProperty : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetOnProperty::class
        val useSiteDescription: String
    }

    interface InapplicableTargetOnPropertyWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetOnPropertyWarning::class
        val useSiteDescription: String
    }

    interface InapplicableTargetPropertyImmutable : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetPropertyImmutable::class
        val useSiteDescription: String
    }

    interface InapplicableTargetPropertyHasNoDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetPropertyHasNoDelegate::class
    }

    interface InapplicableTargetPropertyHasNoBackingField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableTargetPropertyHasNoBackingField::class
    }

    interface InapplicableParamTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableParamTarget::class
    }

    interface RedundantAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RedundantAnnotationTarget::class
        val useSiteDescription: String
    }

    interface InapplicableFileTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableFileTarget::class
    }

    interface InapplicableAllTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableAllTarget::class
    }

    interface InapplicableAllTargetInMultiAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableAllTargetInMultiAnnotation::class
    }

    interface RepeatedAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RepeatedAnnotation::class
    }

    interface RepeatedAnnotationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RepeatedAnnotationWarning::class
    }

    interface NotAClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotAClass::class
    }

    interface WrongExtensionFunctionType : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongExtensionFunctionType::class
    }

    interface WrongExtensionFunctionTypeWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = WrongExtensionFunctionTypeWarning::class
    }

    interface AnnotationInWhereClauseError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationInWhereClauseError::class
    }

    interface AnnotationInContractError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = AnnotationInContractError::class
    }

    interface CompilerRequiredAnnotationAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CompilerRequiredAnnotationAmbiguity::class
        val typeFromCompilerPhase: KaType
        val typeFromTypesPhase: KaType
    }

    interface AmbiguousAnnotationArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousAnnotationArgument::class
        val symbols: List<KaSymbol>
    }

    interface VolatileOnValue : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = VolatileOnValue::class
    }

    interface VolatileOnDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = VolatileOnDelegate::class
    }

    interface NonSourceAnnotationOnInlinedLambdaExpression : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = NonSourceAnnotationOnInlinedLambdaExpression::class
    }

    interface PotentiallyNonReportedAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = PotentiallyNonReportedAnnotation::class
    }

    interface AnnotationWillBeAppliedAlsoToPropertyOrField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationWillBeAppliedAlsoToPropertyOrField::class
        val useSiteDescription: String
    }

    interface AnnotationsOnBlockLevelExpressionOnTheSameLine : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationsOnBlockLevelExpressionOnTheSameLine::class
    }

    interface IgnorabilityAnnotationsWithCheckerDisabled : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = IgnorabilityAnnotationsWithCheckerDisabled::class
    }

    interface DslMarkerPropagatesToMany : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = DslMarkerPropagatesToMany::class
    }

    interface JsModuleProhibitedOnVar : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsModuleProhibitedOnVar::class
    }

    interface JsModuleProhibitedOnNonNative : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsModuleProhibitedOnNonNative::class
    }

    interface NestedJsModuleProhibited : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NestedJsModuleProhibited::class
    }

    interface CallFromUmdMustBeJsModuleAndJsNonModule : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallFromUmdMustBeJsModuleAndJsNonModule::class
    }

    interface CallToJsModuleWithoutModuleSystem : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallToJsModuleWithoutModuleSystem::class
        val callee: KaSymbol
    }

    interface CallToJsNonModuleWithModuleSystem : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallToJsNonModuleWithModuleSystem::class
        val callee: KaSymbol
    }

    interface RuntimeAnnotationNotSupported : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RuntimeAnnotationNotSupported::class
    }

    interface RuntimeAnnotationOnExternalDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RuntimeAnnotationOnExternalDeclaration::class
    }

    interface NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun::class
        val type: KaType
    }

    interface NativeIndexerKeyShouldBeStringOrNumber : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NativeIndexerKeyShouldBeStringOrNumber::class
        val kind: String
    }

    interface NativeIndexerWrongParameterCount : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NativeIndexerWrongParameterCount::class
        val parametersCount: Int
        val kind: String
    }

    interface NativeIndexerCanNotHaveDefaultArguments : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NativeIndexerCanNotHaveDefaultArguments::class
        val kind: String
    }

    interface NativeGetterReturnTypeShouldBeNullable : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NativeGetterReturnTypeShouldBeNullable::class
    }

    interface NativeSetterWrongReturnType : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NativeSetterWrongReturnType::class
    }

    interface JsNameIsNotOnAllAccessors : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameIsNotOnAllAccessors::class
    }

    interface JsNameProhibitedForNamedNative : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameProhibitedForNamedNative::class
    }

    interface JsNameProhibitedForOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameProhibitedForOverride::class
    }

    interface JsNameOnPrimaryConstructorProhibited : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameOnPrimaryConstructorProhibited::class
    }

    interface JsNameOnAccessorAndProperty : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameOnAccessorAndProperty::class
    }

    interface JsNameProhibitedForExtensionProperty : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameProhibitedForExtensionProperty::class
    }

    interface JsBuiltinNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsBuiltinNameClash::class
        val name: String
    }

    interface NameContainsIllegalChars : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NameContainsIllegalChars::class
    }

    interface JsNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsNameClash::class
        val name: String
        val existing: List<KaSymbol>
    }

    interface JsFakeNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JsFakeNameClash::class
        val name: String
        val override: KaSymbol
        val existing: List<KaSymbol>
    }

    interface WrongJsQualifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongJsQualifier::class
    }

    interface OptInUsage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInUsage::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInUsageError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInUsageError::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInToInheritance : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInToInheritance::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInToInheritanceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInToInheritanceError::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInOverride : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInOverride::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInOverrideError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInOverrideError::class
        val optInMarkerClassId: ClassId
        val message: String
    }

    interface OptInCanOnlyBeUsedAsAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInCanOnlyBeUsedAsAnnotation::class
    }

    interface OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn::class
    }

    interface OptInWithoutArguments : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInWithoutArguments::class
    }

    interface OptInArgumentIsNotMarker : KaFirDiagnostic<KtClassLiteralExpression> {
        override val diagnosticClass get() = OptInArgumentIsNotMarker::class
        val notMarkerClassId: ClassId
    }

    interface OptInMarkerWithWrongTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerWithWrongTarget::class
        val target: String
    }

    interface OptInMarkerWithWrongRetention : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerWithWrongRetention::class
    }

    interface OptInMarkerOnWrongTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerOnWrongTarget::class
        val target: String
    }

    interface OptInMarkerOnOverride : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerOnOverride::class
    }

    interface OptInMarkerOnOverrideWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OptInMarkerOnOverrideWarning::class
    }

    interface SubclassOptInInapplicable : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SubclassOptInInapplicable::class
        val target: String
    }

    interface SubclassOptInArgumentIsNotMarker : KaFirDiagnostic<KtClassLiteralExpression> {
        override val diagnosticClass get() = SubclassOptInArgumentIsNotMarker::class
        val notMarkerClassId: ClassId
    }

    interface ExposedTypealiasExpandedType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedTypealiasExpandedType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedFunctionReturnType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedFunctionReturnType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedReceiverType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExposedReceiverType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedPropertyType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedPropertyType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedPropertyTypeInConstructorError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExposedPropertyTypeInConstructorError::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedParameterType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ExposedParameterType::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedSuperInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExposedSuperInterface::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedSuperClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExposedSuperClass::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedTypeParameterBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExposedTypeParameterBound::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedTypeParameterBoundDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExposedTypeParameterBoundDeprecationWarning::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface ExposedPackagePrivateTypeFromInternalWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExposedPackagePrivateTypeFromInternalWarning::class
        val elementVisibility: EffectiveVisibility
        val restrictingDeclaration: KaClassLikeSymbol
        val relationToType: RelationToType
        val restrictingVisibility: EffectiveVisibility
    }

    interface InapplicableInfixModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableInfixModifier::class
    }

    interface RepeatedModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RepeatedModifier::class
        val modifier: KtModifierKeywordToken
    }

    interface RedundantModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantModifier::class
        val redundantModifier: KtModifierKeywordToken
        val conflictingModifier: KtModifierKeywordToken
    }

    interface DeprecatedModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifier::class
        val deprecatedModifier: KtModifierKeywordToken
        val actualModifier: KtModifierKeywordToken
    }

    interface DeprecatedModifierPair : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierPair::class
        val deprecatedModifier: KtModifierKeywordToken
        val conflictingModifier: KtModifierKeywordToken
    }

    interface DeprecatedModifierForTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierForTarget::class
        val deprecatedModifier: KtModifierKeywordToken
        val target: String
    }

    interface RedundantModifierForTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantModifierForTarget::class
        val redundantModifier: KtModifierKeywordToken
        val target: String
    }

    interface IncompatibleModifiers : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IncompatibleModifiers::class
        val modifier1: KtModifierKeywordToken
        val modifier2: KtModifierKeywordToken
    }

    interface RedundantOpenInInterface : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = RedundantOpenInInterface::class
    }

    interface WrongModifierTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongModifierTarget::class
        val modifier: KtModifierKeywordToken
        val target: String
    }

    interface OperatorModifierRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OperatorModifierRequired::class
        val functionSymbol: KaFunctionSymbol
    }

    interface OperatorCallOnConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OperatorCallOnConstructor::class
        val name: String
    }

    interface InfixModifierRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InfixModifierRequired::class
        val functionSymbol: KaFunctionSymbol
    }

    interface WrongModifierContainingDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongModifierContainingDeclaration::class
        val modifier: KtModifierKeywordToken
        val target: String
    }

    interface DeprecatedModifierContainingDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierContainingDeclaration::class
        val modifier: KtModifierKeywordToken
        val target: String
    }

    interface InapplicableOperatorModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableOperatorModifier::class
        val message: String
    }

    interface InapplicableOperatorModifierWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableOperatorModifierWarning::class
        val message: String
    }

    interface NoExplicitVisibilityInApiMode : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NoExplicitVisibilityInApiMode::class
    }

    interface NoExplicitVisibilityInApiModeWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NoExplicitVisibilityInApiModeWarning::class
    }

    interface NoExplicitReturnTypeInApiMode : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NoExplicitReturnTypeInApiMode::class
    }

    interface NoExplicitReturnTypeInApiModeWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NoExplicitReturnTypeInApiModeWarning::class
    }

    interface AnonymousSuspendFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = AnonymousSuspendFunction::class
    }

    interface ValueClassNotTopLevel : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ValueClassNotTopLevel::class
    }

    interface ValueClassNotFinal : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ValueClassNotFinal::class
    }

    interface AbsenceOfPrimaryConstructorForValueClass : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = AbsenceOfPrimaryConstructorForValueClass::class
    }

    interface InlineClassConstructorWrongParametersSize : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InlineClassConstructorWrongParametersSize::class
    }

    interface ValueClassEmptyConstructor : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ValueClassEmptyConstructor::class
    }

    interface ValueClassConstructorNotFinalReadOnlyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValueClassConstructorNotFinalReadOnlyParameter::class
    }

    interface PropertyWithBackingFieldInsideValueClass : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PropertyWithBackingFieldInsideValueClass::class
    }

    interface DelegatedPropertyInsideValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegatedPropertyInsideValueClass::class
    }

    interface ValueClassHasInapplicableParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ValueClassHasInapplicableParameterType::class
        val type: KaType
    }

    interface ValueClassCannotImplementInterfaceByDelegation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ValueClassCannotImplementInterfaceByDelegation::class
    }

    interface ValueClassCannotExtendClasses : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ValueClassCannotExtendClasses::class
    }

    interface ValueClassCannotBeRecursive : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ValueClassCannotBeRecursive::class
    }

    interface MultiFieldValueClassPrimaryConstructorDefaultParameter : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = MultiFieldValueClassPrimaryConstructorDefaultParameter::class
    }

    interface SecondaryConstructorWithBodyInsideValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SecondaryConstructorWithBodyInsideValueClass::class
    }

    interface ReservedMemberInsideValueClass : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = ReservedMemberInsideValueClass::class
        val name: String
    }

    interface ReservedMemberFromInterfaceInsideValueClass : KaFirDiagnostic<KtClass> {
        override val diagnosticClass get() = ReservedMemberFromInterfaceInsideValueClass::class
        val interfaceName: String
        val methodName: String
    }

    interface TypeArgumentOnTypedValueClassEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = TypeArgumentOnTypedValueClassEquals::class
    }

    interface InnerClassInsideValueClass : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InnerClassInsideValueClass::class
    }

    interface ValueClassCannotBeCloneable : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ValueClassCannotBeCloneable::class
    }

    interface ValueClassCannotHaveContextReceivers : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ValueClassCannotHaveContextReceivers::class
    }

    interface AnnotationOnIllegalMultiFieldValueClassTypedTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationOnIllegalMultiFieldValueClassTypedTarget::class
        val name: String
    }

    interface NoneApplicable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoneApplicable::class
        val candidates: List<KaSymbol>
    }

    interface InapplicableCandidate : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableCandidate::class
        val candidate: KaSymbol
    }

    interface TypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeMismatch::class
        val expectedType: KaType
        val actualType: KaType
        val isMismatchDueToNullability: Boolean
    }

    interface TypeInferenceOnlyInputTypesError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeInferenceOnlyInputTypesError::class
        val typeParameter: KaTypeParameterSymbol
    }

    interface ThrowableTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ThrowableTypeMismatch::class
        val actualType: KaType
        val isMismatchDueToNullability: Boolean
    }

    interface ConditionTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConditionTypeMismatch::class
        val actualType: KaType
        val isMismatchDueToNullability: Boolean
    }

    interface ArgumentTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ArgumentTypeMismatch::class
        val actualType: KaType
        val expectedType: KaType
        val isMismatchDueToNullability: Boolean
    }

    interface MemberProjectedOut : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MemberProjectedOut::class
        val receiver: KaType
        val projection: String
        val symbol: KaCallableSymbol
    }

    interface NullForNonnullType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NullForNonnullType::class
        val expectedType: KaType
    }

    interface InapplicableLateinitModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = InapplicableLateinitModifier::class
        val reason: String
    }

    interface VarargOutsideParentheses : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = VarargOutsideParentheses::class
    }

    interface NamedArgumentsNotAllowed : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = NamedArgumentsNotAllowed::class
        val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget
    }

    interface NonVarargSpread : KaFirDiagnostic<LeafPsiElement> {
        override val diagnosticClass get() = NonVarargSpread::class
    }

    interface ArgumentPassedTwice : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = ArgumentPassedTwice::class
    }

    interface TooManyArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TooManyArguments::class
        val function: KaCallableSymbol
    }

    interface UnexpectedTrailingLambdaOnANewLine : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnexpectedTrailingLambdaOnANewLine::class
    }

    interface NoValueForParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NoValueForParameter::class
        val violatedParameter: KaSymbol
    }

    interface NamedParameterNotFound : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = NamedParameterNotFound::class
        val name: String
    }

    interface NameForAmbiguousParameter : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass get() = NameForAmbiguousParameter::class
    }

    interface MixingNamedAndPositionalArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MixingNamedAndPositionalArguments::class
    }

    interface AssignmentTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssignmentTypeMismatch::class
        val expectedType: KaType
        val actualType: KaType
        val isMismatchDueToNullability: Boolean
    }

    interface ResultTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ResultTypeMismatch::class
        val expectedType: KaType
        val actualType: KaType
    }

    interface ManyLambdaExpressionArguments : KaFirDiagnostic<KtLambdaExpression> {
        override val diagnosticClass get() = ManyLambdaExpressionArguments::class
    }

    interface SpreadOfNullable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpreadOfNullable::class
    }

    interface AssigningSingleElementToVarargInNamedFormFunctionError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormFunctionError::class
        val expectedArrayType: KaType
    }

    interface AssigningSingleElementToVarargInNamedFormFunctionWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormFunctionWarning::class
        val expectedArrayType: KaType
    }

    interface AssigningSingleElementToVarargInNamedFormAnnotationError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormAnnotationError::class
    }

    interface AssigningSingleElementToVarargInNamedFormAnnotationWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssigningSingleElementToVarargInNamedFormAnnotationWarning::class
    }

    interface RedundantSpreadOperatorInNamedFormInAnnotation : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = RedundantSpreadOperatorInNamedFormInAnnotation::class
    }

    interface RedundantSpreadOperatorInNamedFormInFunction : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = RedundantSpreadOperatorInNamedFormInFunction::class
    }

    interface NestedClassAccessedViaInstanceReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NestedClassAccessedViaInstanceReference::class
        val symbol: KaClassLikeSymbol
    }

    interface CompareToTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CompareToTypeMismatch::class
        val actualType: KaType
    }

    interface HasNextFunctionTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = HasNextFunctionTypeMismatch::class
        val actualType: KaType
    }

    interface IllegalTypeArgumentForVarargParameterWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IllegalTypeArgumentForVarargParameterWarning::class
        val type: KaType
    }

    interface OverloadResolutionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OverloadResolutionAmbiguity::class
        val candidates: List<KaSymbol>
    }

    interface AssignOperatorAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AssignOperatorAmbiguity::class
        val candidates: List<KaSymbol>
    }

    interface IteratorAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IteratorAmbiguity::class
        val candidates: List<KaSymbol>
    }

    interface HasNextFunctionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = HasNextFunctionAmbiguity::class
        val candidates: List<KaSymbol>
    }

    interface NextAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NextAmbiguity::class
        val candidates: List<KaSymbol>
    }

    interface AmbiguousFunctionTypeKind : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousFunctionTypeKind::class
        val kinds: List<FunctionTypeKind>
    }

    interface NoContextArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NoContextArgument::class
        val symbol: KaSymbol
    }

    interface AmbiguousContextArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = AmbiguousContextArgument::class
        val symbol: KaSymbol
    }

    interface AmbiguousCallWithImplicitContextReceiver : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = AmbiguousCallWithImplicitContextReceiver::class
    }

    interface UnsupportedContextualDeclarationCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnsupportedContextualDeclarationCall::class
    }

    interface SubtypingBetweenContextReceivers : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SubtypingBetweenContextReceivers::class
    }

    interface ContextParametersWithBackingField : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ContextParametersWithBackingField::class
    }

    interface ContextReceiversDeprecated : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ContextReceiversDeprecated::class
        val message: String
    }

    interface ContextClassOrConstructor : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ContextClassOrConstructor::class
    }

    interface ContextParameterWithoutName : KaFirDiagnostic<KtContextReceiver> {
        override val diagnosticClass get() = ContextParameterWithoutName::class
    }

    interface ContextParameterWithDefault : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ContextParameterWithDefault::class
    }

    interface CallableReferenceToContextualDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallableReferenceToContextualDeclaration::class
        val symbol: KaCallableSymbol
    }

    interface MultipleContextLists : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleContextLists::class
    }

    interface NamedContextParameterInFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NamedContextParameterInFunctionType::class
    }

    interface ContextualOverloadShadowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ContextualOverloadShadowed::class
        val symbols: List<KaSymbol>
    }

    interface RecursionInImplicitTypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RecursionInImplicitTypes::class
    }

    interface InferenceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferenceError::class
    }

    interface ProjectionOnNonClassTypeArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ProjectionOnNonClassTypeArgument::class
    }

    interface UpperBoundViolated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolated::class
        val expectedUpperBound: KaType
        val actualUpperBound: KaType
        val extraMessage: String
    }

    interface UpperBoundViolatedDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolatedDeprecationWarning::class
        val expectedUpperBound: KaType
        val actualUpperBound: KaType
        val extraMessage: String
    }

    interface UpperBoundViolatedInTypealiasExpansion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolatedInTypealiasExpansion::class
        val expectedUpperBound: KaType
        val actualUpperBound: KaType
    }

    interface UpperBoundViolatedInTypealiasExpansionDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolatedInTypealiasExpansionDeprecationWarning::class
        val expectedUpperBound: KaType
        val actualUpperBound: KaType
    }

    interface TypeArgumentsNotAllowed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeArgumentsNotAllowed::class
        val place: String
    }

    interface TypeArgumentsForOuterClassWhenNestedReferenced : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeArgumentsForOuterClassWhenNestedReferenced::class
    }

    interface WrongNumberOfTypeArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongNumberOfTypeArguments::class
        val expectedCount: Int
        val owner: KaSymbol
    }

    interface NoTypeArgumentsOnRhs : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoTypeArgumentsOnRhs::class
        val expectedCount: Int
        val classifier: KaClassLikeSymbol
    }

    interface OuterClassArgumentsRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OuterClassArgumentsRequired::class
        val outer: KaClassLikeSymbol
    }

    interface TypeParametersInObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParametersInObject::class
    }

    interface TypeParametersInAnonymousObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParametersInAnonymousObject::class
    }

    interface IllegalProjectionUsage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalProjectionUsage::class
    }

    interface TypeParametersInEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParametersInEnum::class
    }

    interface ConflictingProjection : KaFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass get() = ConflictingProjection::class
        val type: KaType
    }

    interface ConflictingProjectionInTypealiasExpansion : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConflictingProjectionInTypealiasExpansion::class
        val type: KaType
    }

    interface RedundantProjection : KaFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass get() = RedundantProjection::class
        val type: KaType
    }

    interface VarianceOnTypeParameterNotAllowed : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = VarianceOnTypeParameterNotAllowed::class
    }

    interface CatchParameterWithDefaultValue : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CatchParameterWithDefaultValue::class
    }

    interface TypeParameterInCatchClause : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParameterInCatchClause::class
    }

    interface GenericThrowableSubclass : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = GenericThrowableSubclass::class
    }

    interface InnerClassOfGenericThrowableSubclass : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = InnerClassOfGenericThrowableSubclass::class
    }

    interface KclassWithNullableTypeParameterInSignature : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = KclassWithNullableTypeParameterInSignature::class
        val typeParameter: KaTypeParameterSymbol
    }

    interface TypeParameterAsReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParameterAsReified::class
        val typeParameter: KaTypeParameterSymbol
    }

    interface TypeParameterAsReifiedArrayError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeParameterAsReifiedArrayError::class
        val typeParameter: KaTypeParameterSymbol
    }

    interface ReifiedTypeForbiddenSubstitution : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ReifiedTypeForbiddenSubstitution::class
        val type: KaType
    }

    interface DefinitelyNonNullableAsReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DefinitelyNonNullableAsReified::class
    }

    interface TypeIntersectionAsReifiedError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeIntersectionAsReifiedError::class
        val typeParameter: KaTypeParameterSymbol
        val types: List<KaType>
    }

    interface TypeIntersectionAsReifiedWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeIntersectionAsReifiedWarning::class
        val typeParameter: KaTypeParameterSymbol
        val types: List<KaType>
    }

    interface FinalUpperBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = FinalUpperBound::class
        val type: KaType
    }

    interface UpperBoundIsExtensionOrContextFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UpperBoundIsExtensionOrContextFunctionType::class
    }

    interface BoundsNotAllowedIfBoundedByTypeParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = BoundsNotAllowedIfBoundedByTypeParameter::class
    }

    interface OnlyOneClassBoundAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = OnlyOneClassBoundAllowed::class
    }

    interface RepeatedBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = RepeatedBound::class
    }

    interface ConflictingUpperBounds : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ConflictingUpperBounds::class
        val typeParameter: KaTypeParameterSymbol
    }

    interface NameInConstraintIsNotATypeParameter : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass get() = NameInConstraintIsNotATypeParameter::class
        val typeParameterName: Name
        val typeParametersOwner: KaSymbol
    }

    interface BoundOnTypeAliasParameterNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = BoundOnTypeAliasParameterNotAllowed::class
    }

    interface ReifiedTypeParameterNoInline : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = ReifiedTypeParameterNoInline::class
    }

    interface ReifiedTypeParameterOnAliasError : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = ReifiedTypeParameterOnAliasError::class
    }

    interface ReifiedTypeParameterOnAliasWarning : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = ReifiedTypeParameterOnAliasWarning::class
    }

    interface TypeParametersNotAllowed : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = TypeParametersNotAllowed::class
    }

    interface TypeParameterOfPropertyNotUsedInReceiver : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = TypeParameterOfPropertyNotUsedInReceiver::class
    }

    interface ReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ReturnTypeMismatch::class
        val expectedType: KaType
        val actualType: KaType
        val targetFunction: KaSymbol
        val isMismatchDueToNullability: Boolean
    }

    interface ImplicitNothingReturnType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ImplicitNothingReturnType::class
    }

    interface ImplicitNothingPropertyType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ImplicitNothingPropertyType::class
    }

    interface AbbreviatedNothingReturnType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AbbreviatedNothingReturnType::class
    }

    interface AbbreviatedNothingPropertyType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AbbreviatedNothingPropertyType::class
    }

    interface CyclicGenericUpperBound : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CyclicGenericUpperBound::class
        val typeParameters: List<KaTypeParameterSymbol>
    }

    interface FiniteBoundsViolation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FiniteBoundsViolation::class
    }

    interface FiniteBoundsViolationInJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FiniteBoundsViolationInJava::class
        val containingTypes: List<KaSymbol>
    }

    interface ExpansiveInheritance : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpansiveInheritance::class
    }

    interface ExpansiveInheritanceInJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpansiveInheritanceInJava::class
        val containingTypes: List<KaSymbol>
    }

    interface DeprecatedTypeParameterSyntax : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = DeprecatedTypeParameterSyntax::class
    }

    interface MisplacedTypeParameterConstraints : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass get() = MisplacedTypeParameterConstraints::class
    }

    interface DynamicSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DynamicSupertype::class
    }

    interface DynamicUpperBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DynamicUpperBound::class
    }

    interface DynamicReceiverNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DynamicReceiverNotAllowed::class
    }

    interface DynamicReceiverExpectedButWasNonDynamic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DynamicReceiverExpectedButWasNonDynamic::class
        val actualType: KaType
    }

    interface IncompatibleTypes : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncompatibleTypes::class
        val typeA: KaType
        val typeB: KaType
    }

    interface IncompatibleTypesWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncompatibleTypesWarning::class
        val typeA: KaType
        val typeB: KaType
    }

    interface TypeVarianceConflictError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeVarianceConflictError::class
        val typeParameter: KaTypeParameterSymbol
        val typeParameterVariance: Variance
        val variance: Variance
        val containingType: KaType
    }

    interface TypeVarianceConflictInExpandedType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeVarianceConflictInExpandedType::class
        val typeParameter: KaTypeParameterSymbol
        val typeParameterVariance: Variance
        val variance: Variance
        val containingType: KaType
    }

    interface SmartcastImpossible : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = SmartcastImpossible::class
        val desiredType: KaType
        val subject: KtExpression
        val description: String
        val isCastToNotNull: Boolean
    }

    interface SmartcastImpossibleOnImplicitInvokeReceiver : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = SmartcastImpossibleOnImplicitInvokeReceiver::class
        val desiredType: KaType
        val subject: KtExpression
        val description: String
        val isCastToNotNull: Boolean
    }

    interface DeprecatedSmartcastOnDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DeprecatedSmartcastOnDelegatedProperty::class
        val desiredType: KaType
        val property: KaCallableSymbol
    }

    interface RedundantNullable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = RedundantNullable::class
    }

    interface PlatformClassMappedToKotlin : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PlatformClassMappedToKotlin::class
        val kotlinClass: ClassId
    }

    interface InferredTypeVariableIntoEmptyIntersectionError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferredTypeVariableIntoEmptyIntersectionError::class
        val typeVariableDescription: String
        val incompatibleTypes: List<KaType>
        val description: String
        val causingTypes: String
    }

    interface InferredTypeVariableIntoEmptyIntersectionWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferredTypeVariableIntoEmptyIntersectionWarning::class
        val typeVariableDescription: String
        val incompatibleTypes: List<KaType>
        val description: String
        val causingTypes: String
    }

    interface InferredTypeVariableIntoPossibleEmptyIntersection : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferredTypeVariableIntoPossibleEmptyIntersection::class
        val typeVariableDescription: String
        val incompatibleTypes: List<KaType>
        val description: String
        val causingTypes: String
    }

    interface IncorrectLeftComponentOfIntersection : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncorrectLeftComponentOfIntersection::class
    }

    interface IncorrectRightComponentOfIntersection : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncorrectRightComponentOfIntersection::class
    }

    interface NullableOnDefinitelyNotNullable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NullableOnDefinitelyNotNullable::class
    }

    interface InferredInvisibleReifiedTypeArgumentError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InferredInvisibleReifiedTypeArgumentError::class
        val typeParameter: KaTypeParameterSymbol
        val typeArgumentType: KaType
    }

    interface InferredInvisibleReifiedTypeArgumentWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InferredInvisibleReifiedTypeArgumentWarning::class
        val typeParameter: KaTypeParameterSymbol
        val typeArgumentType: KaType
    }

    interface InferredInvisibleVarargTypeArgumentError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InferredInvisibleVarargTypeArgumentError::class
        val typeParameter: KaTypeParameterSymbol
        val typeArgumentType: KaType
        val valueParameter: KaSymbol
    }

    interface InferredInvisibleVarargTypeArgumentWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InferredInvisibleVarargTypeArgumentWarning::class
        val typeParameter: KaTypeParameterSymbol
        val typeArgumentType: KaType
        val valueParameter: KaSymbol
    }

    interface InferredInvisibleReturnTypeError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InferredInvisibleReturnTypeError::class
        val calleeSymbol: KaSymbol
        val returnType: KaType
    }

    interface InferredInvisibleReturnTypeWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InferredInvisibleReturnTypeWarning::class
        val calleeSymbol: KaSymbol
        val returnType: KaType
    }

    interface GenericQualifierOnConstructorCallError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericQualifierOnConstructorCallError::class
    }

    interface GenericQualifierOnConstructorCallWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericQualifierOnConstructorCallWarning::class
    }

    interface AtomicRefWithoutConsistentIdentity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AtomicRefWithoutConsistentIdentity::class
        val atomicRef: ClassId
        val argumentType: KaType
        val suggestedType: ClassId?
    }

    interface ExtensionInClassReferenceNotAllowed : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExtensionInClassReferenceNotAllowed::class
        val referencedDeclaration: KaCallableSymbol
    }

    interface CallableReferenceLhsNotAClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CallableReferenceLhsNotAClass::class
    }

    interface CallableReferenceToAnnotationConstructor : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CallableReferenceToAnnotationConstructor::class
    }

    interface AdaptedCallableReferenceAgainstReflectionType : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AdaptedCallableReferenceAgainstReflectionType::class
    }

    interface ClassLiteralLhsNotAClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ClassLiteralLhsNotAClass::class
    }

    interface NullableTypeInClassLiteralLhs : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NullableTypeInClassLiteralLhs::class
    }

    interface ExpressionOfNullableTypeInClassLiteralLhs : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpressionOfNullableTypeInClassLiteralLhs::class
        val lhsType: KaType
    }

    interface UnsupportedClassLiteralsWithEmptyLhs : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnsupportedClassLiteralsWithEmptyLhs::class
    }

    interface MutablePropertyWithCapturedType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MutablePropertyWithCapturedType::class
    }

    interface UnsupportedReflectionApi : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnsupportedReflectionApi::class
        val unsupportedReflectionAPI: String
    }

    interface NothingToOverride : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = NothingToOverride::class
        val declaration: KaCallableSymbol
        val candidates: List<KaCallableSymbol>
    }

    interface CannotOverrideInvisibleMember : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = CannotOverrideInvisibleMember::class
        val overridingMember: KaCallableSymbol
        val baseMember: KaCallableSymbol
    }

    interface DataClassOverrideConflict : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = DataClassOverrideConflict::class
        val overridingMember: KaCallableSymbol
        val baseMember: KaCallableSymbol
    }

    interface DataClassOverrideDefaultValues : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DataClassOverrideDefaultValues::class
        val overridingMember: KaCallableSymbol
        val baseType: KaClassLikeSymbol
    }

    interface CannotWeakenAccessPrivilege : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = CannotWeakenAccessPrivilege::class
        val overridingVisibility: Visibility
        val overridden: KaCallableSymbol
        val containingClassName: Name
    }

    interface CannotWeakenAccessPrivilegeWarning : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = CannotWeakenAccessPrivilegeWarning::class
        val overridingVisibility: Visibility
        val overridden: KaCallableSymbol
        val containingClassName: Name
    }

    interface CannotChangeAccessPrivilege : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = CannotChangeAccessPrivilege::class
        val overridingVisibility: Visibility
        val overridden: KaCallableSymbol
        val containingClassName: Name
    }

    interface CannotChangeAccessPrivilegeWarning : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = CannotChangeAccessPrivilegeWarning::class
        val overridingVisibility: Visibility
        val overridden: KaCallableSymbol
        val containingClassName: Name
    }

    interface CannotInferVisibility : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = CannotInferVisibility::class
        val callable: KaCallableSymbol
    }

    interface CannotInferVisibilityWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = CannotInferVisibilityWarning::class
        val callable: KaCallableSymbol
    }

    interface MultipleDefaultsInheritedFromSupertypes : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypes::class
        val name: Name
        val valueParameter: KaSymbol
        val baseFunctions: List<KaCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride::class
        val name: Name
        val valueParameter: KaSymbol
        val baseFunctions: List<KaCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesDeprecationError::class
        val name: Name
        val valueParameter: KaSymbol
        val baseFunctions: List<KaCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesDeprecationWarning::class
        val name: Name
        val valueParameter: KaSymbol
        val baseFunctions: List<KaCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError::class
        val name: Name
        val valueParameter: KaSymbol
        val baseFunctions: List<KaCallableSymbol>
    }

    interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning::class
        val name: Name
        val valueParameter: KaSymbol
        val baseFunctions: List<KaCallableSymbol>
    }

    interface TypealiasExpandsToArrayOfNothings : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = TypealiasExpandsToArrayOfNothings::class
        val type: KaType
    }

    interface OverridingFinalMember : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = OverridingFinalMember::class
        val overriddenDeclaration: KaCallableSymbol
        val containingClassName: Name
    }

    interface ReturnTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ReturnTypeMismatchOnInheritance::class
        val conflictingDeclaration1: KaCallableSymbol
        val conflictingDeclaration2: KaCallableSymbol
    }

    interface PropertyTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = PropertyTypeMismatchOnInheritance::class
        val conflictingDeclaration1: KaCallableSymbol
        val conflictingDeclaration2: KaCallableSymbol
    }

    interface VarTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = VarTypeMismatchOnInheritance::class
        val conflictingDeclaration1: KaCallableSymbol
        val conflictingDeclaration2: KaCallableSymbol
    }

    interface ReturnTypeMismatchByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ReturnTypeMismatchByDelegation::class
        val delegateDeclaration: KaCallableSymbol
        val baseDeclaration: KaCallableSymbol
    }

    interface PropertyTypeMismatchByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = PropertyTypeMismatchByDelegation::class
        val delegateDeclaration: KaCallableSymbol
        val baseDeclaration: KaCallableSymbol
    }

    interface VarOverriddenByValByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = VarOverriddenByValByDelegation::class
        val delegateDeclaration: KaCallableSymbol
        val baseDeclaration: KaCallableSymbol
    }

    interface ConflictingInheritedMembers : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ConflictingInheritedMembers::class
        val owner: KaClassLikeSymbol
        val conflictingDeclarations: List<KaCallableSymbol>
    }

    interface AbstractMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = AbstractMemberNotImplemented::class
        val classOrObject: KaClassLikeSymbol
        val missingDeclarations: List<KaCallableSymbol>
    }

    interface AbstractMemberIncorrectlyDelegatedError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = AbstractMemberIncorrectlyDelegatedError::class
        val classOrObject: KaClassLikeSymbol
        val missingDeclarations: List<KaCallableSymbol>
    }

    interface AbstractMemberIncorrectlyDelegatedWarning : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = AbstractMemberIncorrectlyDelegatedWarning::class
        val classOrObject: KaClassLikeSymbol
        val missingDeclarations: List<KaCallableSymbol>
    }

    interface AbstractMemberNotImplementedByEnumEntry : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass get() = AbstractMemberNotImplementedByEnumEntry::class
        val enumEntry: KaSymbol
        val missingDeclarations: List<KaCallableSymbol>
    }

    interface AbstractClassMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = AbstractClassMemberNotImplemented::class
        val classOrObject: KaClassLikeSymbol
        val missingDeclarations: List<KaCallableSymbol>
    }

    interface InvisibleAbstractMemberFromSuperError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = InvisibleAbstractMemberFromSuperError::class
        val classOrObject: KaClassLikeSymbol
        val invisibleDeclarations: List<KaCallableSymbol>
    }

    interface AmbiguousAnonymousTypeInferred : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = AmbiguousAnonymousTypeInferred::class
        val superTypes: List<KaType>
    }

    interface ManyImplMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ManyImplMemberNotImplemented::class
        val classOrObject: KaClassLikeSymbol
        val missingDeclaration: KaCallableSymbol
    }

    interface ManyInterfacesMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ManyInterfacesMemberNotImplemented::class
        val classOrObject: KaClassLikeSymbol
        val missingDeclaration: KaCallableSymbol
    }

    interface OverridingFinalMemberByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = OverridingFinalMemberByDelegation::class
        val delegatedDeclaration: KaCallableSymbol
        val overriddenDeclaration: KaCallableSymbol
    }

    interface DelegatedMemberHidesSupertypeOverride : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = DelegatedMemberHidesSupertypeOverride::class
        val delegatedDeclaration: KaCallableSymbol
        val overriddenDeclaration: KaCallableSymbol
    }

    interface ReturnTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ReturnTypeMismatchOnOverride::class
        val function: KaCallableSymbol
        val superFunction: KaCallableSymbol
    }

    interface PropertyTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = PropertyTypeMismatchOnOverride::class
        val property: KaCallableSymbol
        val superProperty: KaCallableSymbol
    }

    interface VarTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VarTypeMismatchOnOverride::class
        val variable: KaCallableSymbol
        val superVariable: KaCallableSymbol
    }

    interface VarOverriddenByVal : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VarOverriddenByVal::class
        val overridingDeclaration: KaCallableSymbol
        val overriddenDeclaration: KaCallableSymbol
    }

    interface VarImplementedByInheritedValError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VarImplementedByInheritedValError::class
        val classOrObject: KaClassLikeSymbol
        val overridingDeclaration: KaCallableSymbol
        val overriddenDeclaration: KaCallableSymbol
    }

    interface VarImplementedByInheritedValWarning : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VarImplementedByInheritedValWarning::class
        val classOrObject: KaClassLikeSymbol
        val overridingDeclaration: KaCallableSymbol
        val overriddenDeclaration: KaCallableSymbol
    }

    interface NonFinalMemberInFinalClass : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NonFinalMemberInFinalClass::class
    }

    interface NonFinalMemberInObject : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NonFinalMemberInObject::class
    }

    interface VirtualMemberHidden : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VirtualMemberHidden::class
        val declared: KaCallableSymbol
        val overriddenContainer: KaClassLikeSymbol
    }

    interface ParameterNameChangedOnOverride : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ParameterNameChangedOnOverride::class
        val superType: KaClassLikeSymbol
        val conflictingParameter: KaSymbol
    }

    interface DifferentNamesForTheSameParameterInSupertypes : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = DifferentNamesForTheSameParameterInSupertypes::class
        val currentParameter: KaSymbol
        val conflictingParameter: KaSymbol
        val parameterNumber: Int
        val conflictingFunctions: List<KaFunctionSymbol>
    }

    interface SuspendOverriddenByNonSuspend : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass get() = SuspendOverriddenByNonSuspend::class
        val overridingDeclaration: KaCallableSymbol
        val overriddenDeclaration: KaCallableSymbol
    }

    interface NonSuspendOverriddenBySuspend : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass get() = NonSuspendOverriddenBySuspend::class
        val overridingDeclaration: KaCallableSymbol
        val overriddenDeclaration: KaCallableSymbol
    }

    interface ManyCompanionObjects : KaFirDiagnostic<KtObjectDeclaration> {
        override val diagnosticClass get() = ManyCompanionObjects::class
    }

    interface ConflictingOverloads : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConflictingOverloads::class
        val conflictingOverloads: List<KaSymbol>
    }

    interface Redeclaration : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = Redeclaration::class
        val conflictingDeclarations: List<KaSymbol>
    }

    interface ClassifierRedeclaration : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ClassifierRedeclaration::class
        val conflictingDeclarations: List<KaSymbol>
    }

    interface PackageConflictsWithClassifier : KaFirDiagnostic<KtPackageDirective> {
        override val diagnosticClass get() = PackageConflictsWithClassifier::class
        val conflictingClassId: ClassId
    }

    interface ExpectAndActualInTheSameModule : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectAndActualInTheSameModule::class
        val declaration: KaSymbol
    }

    interface MethodOfAnyImplementedInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MethodOfAnyImplementedInInterface::class
    }

    interface ExtensionShadowedByMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExtensionShadowedByMember::class
        val member: KaCallableSymbol
    }

    interface ExtensionFunctionShadowedByMemberPropertyWithInvoke : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExtensionFunctionShadowedByMemberPropertyWithInvoke::class
        val member: KaCallableSymbol
        val invokeOperator: KaCallableSymbol
    }

    interface LocalObjectNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = LocalObjectNotAllowed::class
        val objectName: Name
    }

    interface LocalInterfaceNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = LocalInterfaceNotAllowed::class
        val interfaceName: Name
    }

    interface AbstractFunctionInNonAbstractClass : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = AbstractFunctionInNonAbstractClass::class
        val function: KaCallableSymbol
        val containingClass: KaClassLikeSymbol
    }

    interface AbstractFunctionWithBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = AbstractFunctionWithBody::class
        val function: KaCallableSymbol
    }

    interface NonAbstractFunctionWithNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = NonAbstractFunctionWithNoBody::class
        val function: KaCallableSymbol
    }

    interface PrivateFunctionWithNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = PrivateFunctionWithNoBody::class
        val function: KaCallableSymbol
    }

    interface NonMemberFunctionNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = NonMemberFunctionNoBody::class
        val function: KaCallableSymbol
    }

    interface FunctionDeclarationWithNoName : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = FunctionDeclarationWithNoName::class
    }

    interface AnonymousFunctionWithName : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = AnonymousFunctionWithName::class
    }

    interface SingleAnonymousFunctionWithNameError : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = SingleAnonymousFunctionWithNameError::class
    }

    interface SingleAnonymousFunctionWithNameWarning : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = SingleAnonymousFunctionWithNameWarning::class
    }

    interface AnonymousFunctionParameterWithDefaultValue : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = AnonymousFunctionParameterWithDefaultValue::class
    }

    interface UselessVarargOnParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = UselessVarargOnParameter::class
    }

    interface MultipleVarargParameters : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = MultipleVarargParameters::class
    }

    interface ForbiddenVarargParameterType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ForbiddenVarargParameterType::class
        val varargParameterType: KaType
    }

    interface ValueParameterWithoutExplicitType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ValueParameterWithoutExplicitType::class
    }

    interface CannotInferParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CannotInferParameterType::class
        val parameter: KaTypeParameterSymbol
    }

    interface CannotInferValueParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CannotInferValueParameterType::class
        val parameter: KaSymbol
    }

    interface CannotInferItParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CannotInferItParameterType::class
    }

    interface CannotInferReceiverParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CannotInferReceiverParameterType::class
    }

    interface NoTailCallsFound : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = NoTailCallsFound::class
    }

    interface TailrecOnVirtualMemberError : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = TailrecOnVirtualMemberError::class
    }

    interface NonTailRecursiveCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonTailRecursiveCall::class
    }

    interface TailRecursionInTryIsNotSupported : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TailRecursionInTryIsNotSupported::class
    }

    interface DataObjectCustomEqualsOrHashCode : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = DataObjectCustomEqualsOrHashCode::class
    }

    interface DefaultValueNotAllowedInOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DefaultValueNotAllowedInOverride::class
    }

    interface FunInterfaceWrongCountOfAbstractMembers : KaFirDiagnostic<KtClass> {
        override val diagnosticClass get() = FunInterfaceWrongCountOfAbstractMembers::class
    }

    interface FunInterfaceCannotHaveAbstractProperties : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = FunInterfaceCannotHaveAbstractProperties::class
    }

    interface FunInterfaceAbstractMethodWithTypeParameters : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = FunInterfaceAbstractMethodWithTypeParameters::class
    }

    interface FunInterfaceAbstractMethodWithDefaultValue : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = FunInterfaceAbstractMethodWithDefaultValue::class
    }

    interface FunInterfaceWithSuspendFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = FunInterfaceWithSuspendFunction::class
    }

    interface AbstractPropertyInNonAbstractClass : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = AbstractPropertyInNonAbstractClass::class
        val property: KaCallableSymbol
        val containingClass: KaClassLikeSymbol
    }

    interface PrivatePropertyInInterface : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PrivatePropertyInInterface::class
    }

    interface AbstractPropertyWithInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AbstractPropertyWithInitializer::class
    }

    interface PropertyInitializerInInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = PropertyInitializerInInterface::class
    }

    interface PropertyWithNoTypeNoInitializer : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PropertyWithNoTypeNoInitializer::class
    }

    interface AbstractPropertyWithoutType : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = AbstractPropertyWithoutType::class
    }

    interface LateinitPropertyWithoutType : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = LateinitPropertyWithoutType::class
    }

    interface MustBeInitialized : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitialized::class
    }

    interface MustBeInitializedWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedWarning::class
    }

    interface MustBeInitializedOrBeFinal : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrBeFinal::class
    }

    interface MustBeInitializedOrBeFinalWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrBeFinalWarning::class
    }

    interface MustBeInitializedOrBeAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrBeAbstract::class
    }

    interface MustBeInitializedOrBeAbstractWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrBeAbstractWarning::class
    }

    interface MustBeInitializedOrFinalOrAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrFinalOrAbstract::class
    }

    interface MustBeInitializedOrFinalOrAbstractWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = MustBeInitializedOrFinalOrAbstractWarning::class
    }

    interface ExtensionPropertyMustHaveAccessorsOrBeAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = ExtensionPropertyMustHaveAccessorsOrBeAbstract::class
    }

    interface UnnecessaryLateinit : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = UnnecessaryLateinit::class
    }

    interface BackingFieldInInterface : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = BackingFieldInInterface::class
    }

    interface ExtensionPropertyWithBackingField : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExtensionPropertyWithBackingField::class
    }

    interface PropertyInitializerNoBackingField : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = PropertyInitializerNoBackingField::class
    }

    interface AbstractDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AbstractDelegatedProperty::class
    }

    interface DelegatedPropertyInInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegatedPropertyInInterface::class
    }

    interface AbstractPropertyWithGetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass get() = AbstractPropertyWithGetter::class
    }

    interface AbstractPropertyWithSetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass get() = AbstractPropertyWithSetter::class
    }

    interface PrivateSetterForAbstractProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = PrivateSetterForAbstractProperty::class
    }

    interface PrivateSetterForOpenProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = PrivateSetterForOpenProperty::class
    }

    interface ValWithSetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass get() = ValWithSetter::class
    }

    interface ConstValNotTopLevelOrObject : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConstValNotTopLevelOrObject::class
    }

    interface ConstValWithGetter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConstValWithGetter::class
    }

    interface ConstValWithDelegate : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ConstValWithDelegate::class
    }

    interface TypeCantBeUsedForConstVal : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = TypeCantBeUsedForConstVal::class
        val constValType: KaType
    }

    interface ConstValWithoutInitializer : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = ConstValWithoutInitializer::class
    }

    interface ConstValWithNonConstInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ConstValWithNonConstInitializer::class
    }

    interface WrongSetterParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongSetterParameterType::class
        val expectedType: KaType
        val actualType: KaType
    }

    interface DelegateUsesExtensionPropertyTypeParameterError : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = DelegateUsesExtensionPropertyTypeParameterError::class
        val usedTypeParameter: KaTypeParameterSymbol
    }

    interface DelegateUsesExtensionPropertyTypeParameterWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = DelegateUsesExtensionPropertyTypeParameterWarning::class
        val usedTypeParameter: KaTypeParameterSymbol
    }

    interface InitializerTypeMismatch : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = InitializerTypeMismatch::class
        val expectedType: KaType
        val actualType: KaType
        val isMismatchDueToNullability: Boolean
    }

    interface GetterVisibilityDiffersFromPropertyVisibility : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = GetterVisibilityDiffersFromPropertyVisibility::class
    }

    interface SetterVisibilityInconsistentWithPropertyVisibility : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = SetterVisibilityInconsistentWithPropertyVisibility::class
    }

    interface WrongSetterReturnType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongSetterReturnType::class
    }

    interface WrongGetterReturnType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongGetterReturnType::class
        val expectedType: KaType
        val actualType: KaType
    }

    interface AccessorForDelegatedProperty : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass get() = AccessorForDelegatedProperty::class
    }

    interface PropertyInitializerWithExplicitFieldDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = PropertyInitializerWithExplicitFieldDeclaration::class
    }

    interface PropertyFieldDeclarationMissingInitializer : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = PropertyFieldDeclarationMissingInitializer::class
    }

    interface LateinitPropertyFieldDeclarationWithInitializer : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = LateinitPropertyFieldDeclarationWithInitializer::class
    }

    interface LateinitFieldInValProperty : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = LateinitFieldInValProperty::class
    }

    interface LateinitNullableBackingField : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = LateinitNullableBackingField::class
    }

    interface BackingFieldForDelegatedProperty : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = BackingFieldForDelegatedProperty::class
    }

    interface PropertyMustHaveGetter : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PropertyMustHaveGetter::class
    }

    interface PropertyMustHaveSetter : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = PropertyMustHaveSetter::class
    }

    interface ExplicitBackingFieldInInterface : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = ExplicitBackingFieldInInterface::class
    }

    interface ExplicitBackingFieldInAbstractProperty : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = ExplicitBackingFieldInAbstractProperty::class
    }

    interface ExplicitBackingFieldInExtension : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = ExplicitBackingFieldInExtension::class
    }

    interface RedundantExplicitBackingField : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass get() = RedundantExplicitBackingField::class
    }

    interface AbstractPropertyInPrimaryConstructorParameters : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = AbstractPropertyInPrimaryConstructorParameters::class
    }

    interface LocalVariableWithTypeParametersWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = LocalVariableWithTypeParametersWarning::class
    }

    interface LocalVariableWithTypeParameters : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass get() = LocalVariableWithTypeParameters::class
    }

    interface ExplicitTypeArgumentsInPropertyAccess : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExplicitTypeArgumentsInPropertyAccess::class
        val kind: String
    }

    interface SafeCallableReferenceCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = SafeCallableReferenceCall::class
    }

    interface LateinitIntrinsicCallOnNonLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonLiteral::class
    }

    interface LateinitIntrinsicCallOnNonLateinit : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonLateinit::class
    }

    interface LateinitIntrinsicCallInInlineFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LateinitIntrinsicCallInInlineFunction::class
    }

    interface LateinitIntrinsicCallOnNonAccessibleProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LateinitIntrinsicCallOnNonAccessibleProperty::class
        val declaration: KaSymbol
    }

    interface LocalExtensionProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LocalExtensionProperty::class
    }

    interface UnnamedVarProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnnamedVarProperty::class
    }

    interface UnnamedDelegatedProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnnamedDelegatedProperty::class
    }

    interface ExpectedDeclarationWithBody : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExpectedDeclarationWithBody::class
    }

    interface ExpectedClassConstructorDelegationCall : KaFirDiagnostic<KtConstructorDelegationCall> {
        override val diagnosticClass get() = ExpectedClassConstructorDelegationCall::class
    }

    interface ExpectedClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ExpectedClassConstructorPropertyParameter::class
    }

    interface ExpectedEnumConstructor : KaFirDiagnostic<KtConstructor<*>> {
        override val diagnosticClass get() = ExpectedEnumConstructor::class
    }

    interface ExpectedEnumEntryWithBody : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass get() = ExpectedEnumEntryWithBody::class
    }

    interface ExpectedPropertyInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExpectedPropertyInitializer::class
    }

    interface ExpectedDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExpectedDelegatedProperty::class
    }

    interface ExpectedLateinitProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ExpectedLateinitProperty::class
    }

    interface SupertypeInitializedInExpectedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SupertypeInitializedInExpectedClass::class
    }

    interface ExpectedPrivateDeclaration : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ExpectedPrivateDeclaration::class
    }

    interface ExpectedExternalDeclaration : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ExpectedExternalDeclaration::class
    }

    interface ExpectedTailrecFunction : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = ExpectedTailrecFunction::class
    }

    interface ImplementationByDelegationInExpectClass : KaFirDiagnostic<KtDelegatedSuperTypeEntry> {
        override val diagnosticClass get() = ImplementationByDelegationInExpectClass::class
    }

    interface ActualTypeAliasNotToClass : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasNotToClass::class
    }

    interface ActualTypeAliasToClassWithDeclarationSiteVariance : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasToClassWithDeclarationSiteVariance::class
    }

    interface ActualTypeAliasWithUseSiteVariance : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasWithUseSiteVariance::class
    }

    interface ActualTypeAliasWithComplexSubstitution : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasWithComplexSubstitution::class
    }

    interface ActualTypeAliasToNullableType : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasToNullableType::class
    }

    interface ActualTypeAliasToNothing : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypeAliasToNothing::class
    }

    interface ActualFunctionWithDefaultArguments : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass get() = ActualFunctionWithDefaultArguments::class
    }

    interface DefaultArgumentsInExpectWithActualTypealias : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = DefaultArgumentsInExpectWithActualTypealias::class
        val expectClassSymbol: KaClassLikeSymbol
        val members: List<KaCallableSymbol>
    }

    interface DefaultArgumentsInExpectActualizedByFakeOverride : KaFirDiagnostic<KtClass> {
        override val diagnosticClass get() = DefaultArgumentsInExpectActualizedByFakeOverride::class
        val expectClassSymbol: KaClassLikeSymbol
        val members: List<KaFunctionSymbol>
    }

    interface ExpectedFunctionSourceWithDefaultArgumentsNotFound : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpectedFunctionSourceWithDefaultArgumentsNotFound::class
    }

    interface ActualWithoutExpect : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ActualWithoutExpect::class
        val declaration: KaSymbol
        val compatibility: Map<ExpectActualMatchingCompatibility, List<KaSymbol>>
    }

    interface ExpectActualIncompatibleClassTypeParameterCount : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleClassTypeParameterCount::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleReturnType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleReturnType::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleParameterNames::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleContextParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleContextParameterNames::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleTypeParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleTypeParameterNames::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleValueParameterVararg : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleValueParameterVararg::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleValueParameterNoinline : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleValueParameterNoinline::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleValueParameterCrossinline : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleValueParameterCrossinline::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleFunctionModifiersDifferent : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleFunctionModifiersDifferent::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleFunctionModifiersNotSubset : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleFunctionModifiersNotSubset::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatiblePropertyKind : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatiblePropertyKind::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatiblePropertyLateinitModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatiblePropertyLateinitModifier::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatiblePropertyConstModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatiblePropertyConstModifier::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatiblePropertySetterVisibility : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatiblePropertySetterVisibility::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleClassKind : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleClassKind::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleClassModifiers : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleClassModifiers::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleFunInterfaceModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleFunInterfaceModifier::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleSupertypes : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleSupertypes::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleNestedTypeAlias : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleNestedTypeAlias::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleEnumEntries : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleEnumEntries::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleIllegalRequiresOptIn : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleIllegalRequiresOptIn::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleModality : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleModality::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleVisibility : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleVisibility::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleClassTypeParameterUpperBounds : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleClassTypeParameterUpperBounds::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleTypeParameterVariance : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleTypeParameterVariance::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleTypeParameterReified : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleTypeParameterReified::class
        val expectDeclaration: KaSymbol
        val actualDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectActualIncompatibleClassScope : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualIncompatibleClassScope::class
        val actualClass: KaSymbol
        val expectMemberDeclaration: KaSymbol
        val actualMemberDeclaration: KaSymbol
        val reason: String
    }

    interface ExpectRefinementAnnotationWrongTarget : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectRefinementAnnotationWrongTarget::class
    }

    interface AmbiguousExpects : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = AmbiguousExpects::class
        val declaration: KaSymbol
        val modules: List<FirModuleData>
    }

    interface NoActualClassMemberForExpectedClass : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = NoActualClassMemberForExpectedClass::class
        val declaration: KaSymbol
        val members: List<Pair<KaSymbol, Map<Mismatch, List<KaSymbol>>>>
    }

    interface ActualMissing : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ActualMissing::class
    }

    interface ExpectRefinementAnnotationMissing : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectRefinementAnnotationMissing::class
    }

    interface ExpectActualClassifiersAreInBetaWarning : KaFirDiagnostic<KtClassLikeDeclaration> {
        override val diagnosticClass get() = ExpectActualClassifiersAreInBetaWarning::class
    }

    interface NotAMultiplatformCompilation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotAMultiplatformCompilation::class
    }

    interface ExpectActualOptInAnnotation : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = ExpectActualOptInAnnotation::class
    }

    interface ActualTypealiasToSpecialAnnotation : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ActualTypealiasToSpecialAnnotation::class
        val typealiasedClassId: ClassId
    }

    interface ActualAnnotationsNotMatchExpect : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ActualAnnotationsNotMatchExpect::class
        val expectSymbol: KaSymbol
        val actualSymbol: KaSymbol
        val actualAnnotationTargetSourceElement: PsiElement?
        val incompatibilityType: ExpectActualAnnotationsIncompatibilityType<FirAnnotation>
    }

    interface OptionalDeclarationOutsideOfAnnotationEntry : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptionalDeclarationOutsideOfAnnotationEntry::class
    }

    interface OptionalDeclarationUsageInNonCommonSource : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptionalDeclarationUsageInNonCommonSource::class
    }

    interface OptionalExpectationNotOnExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptionalExpectationNotOnExpected::class
    }

    interface InitializerRequiredForDestructuringDeclaration : KaFirDiagnostic<KtDestructuringDeclaration> {
        override val diagnosticClass get() = InitializerRequiredForDestructuringDeclaration::class
    }

    interface ComponentFunctionMissing : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ComponentFunctionMissing::class
        val missingFunctionName: Name
        val destructingType: KaType
    }

    interface ComponentFunctionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ComponentFunctionAmbiguity::class
        val functionWithAmbiguityName: Name
        val candidates: List<KaSymbol>
        val destructingType: KaType
    }

    interface ComponentFunctionOnNullable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ComponentFunctionOnNullable::class
        val componentFunctionName: Name
        val destructingType: KaType
    }

    interface ComponentFunctionReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ComponentFunctionReturnTypeMismatch::class
        val componentFunctionName: Name
        val destructingType: KaType
        val expectedType: KaType
    }

    interface UninitializedVariable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UninitializedVariable::class
        val variable: KaVariableSymbol
    }

    interface UninitializedParameter : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass get() = UninitializedParameter::class
        val parameter: KaSymbol
    }

    interface UninitializedEnumEntry : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UninitializedEnumEntry::class
        val enumEntry: KaSymbol
    }

    interface UninitializedEnumCompanion : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UninitializedEnumCompanion::class
        val enumClass: KaClassLikeSymbol
    }

    interface ValReassignment : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ValReassignment::class
        val variable: KaVariableSymbol
    }

    interface ValReassignmentViaBackingFieldError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ValReassignmentViaBackingFieldError::class
        val property: KaVariableSymbol
    }

    interface CapturedValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CapturedValInitialization::class
        val property: KaVariableSymbol
    }

    interface CapturedMemberValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = CapturedMemberValInitialization::class
        val property: KaVariableSymbol
    }

    interface NonInlineMemberValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NonInlineMemberValInitialization::class
        val property: KaVariableSymbol
    }

    interface SetterProjectedOut : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = SetterProjectedOut::class
        val receiverType: KaType
        val projection: String
        val property: KaVariableSymbol
    }

    interface WrongInvocationKind : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongInvocationKind::class
        val declaration: KaSymbol
        val requiredRange: EventOccurrencesRange
        val actualRange: EventOccurrencesRange
    }

    interface LeakedInPlaceLambda : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LeakedInPlaceLambda::class
        val lambda: KaSymbol
    }

    interface VariableWithNoTypeNoInitializer : KaFirDiagnostic<KtVariableDeclaration> {
        override val diagnosticClass get() = VariableWithNoTypeNoInitializer::class
    }

    interface InitializationBeforeDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = InitializationBeforeDeclaration::class
        val property: KaSymbol
    }

    interface InitializationBeforeDeclarationWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = InitializationBeforeDeclarationWarning::class
        val property: KaSymbol
    }

    interface UnreachableCode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnreachableCode::class
        val reachable: List<PsiElement>
        val unreachable: List<PsiElement>
    }

    interface SenselessComparison : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = SenselessComparison::class
        val compareResult: Boolean
    }

    interface SenselessNullInWhen : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SenselessNullInWhen::class
    }

    interface TypecheckerHasRunIntoRecursiveProblem : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = TypecheckerHasRunIntoRecursiveProblem::class
    }

    interface ReturnValueNotUsed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ReturnValueNotUsed::class
        val functionName: Name?
    }

    interface UnsafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsafeCall::class
        val receiverType: KaType
        val receiverExpression: KtExpression?
    }

    interface UnsafeImplicitInvokeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsafeImplicitInvokeCall::class
        val receiverType: KaType
    }

    interface UnsafeInfixCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UnsafeInfixCall::class
        val receiverType: KaType
        val receiverExpression: KtExpression
        val operator: String
        val argumentExpression: KtExpression?
    }

    interface UnsafeOperatorCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UnsafeOperatorCall::class
        val receiverType: KaType
        val receiverExpression: KtExpression
        val operator: String
        val argumentExpression: KtExpression?
    }

    interface IteratorOnNullable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = IteratorOnNullable::class
    }

    interface UnnecessarySafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnnecessarySafeCall::class
        val receiverType: KaType
    }

    interface UnexpectedSafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnexpectedSafeCall::class
    }

    interface UnnecessaryNotNullAssertion : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = UnnecessaryNotNullAssertion::class
        val receiverType: KaType
    }

    interface NotNullAssertionOnLambdaExpression : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NotNullAssertionOnLambdaExpression::class
    }

    interface NotNullAssertionOnCallableReference : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NotNullAssertionOnCallableReference::class
    }

    interface UselessElvis : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = UselessElvis::class
        val receiverType: KaType
    }

    interface UselessElvisRightIsNull : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = UselessElvisRightIsNull::class
    }

    interface CannotCheckForErased : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotCheckForErased::class
        val type: KaType
    }

    interface CastNeverSucceeds : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass get() = CastNeverSucceeds::class
    }

    interface UselessCast : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass get() = UselessCast::class
    }

    interface UncheckedCast : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass get() = UncheckedCast::class
        val originalType: KaType
        val targetType: KaType
    }

    interface UselessIsCheck : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UselessIsCheck::class
        val compileTimeCheckResult: Boolean
    }

    interface IsEnumEntry : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IsEnumEntry::class
    }

    interface DynamicNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DynamicNotAllowed::class
    }

    interface EnumEntryAsType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = EnumEntryAsType::class
    }

    interface ExpectedCondition : KaFirDiagnostic<KtWhenCondition> {
        override val diagnosticClass get() = ExpectedCondition::class
    }

    interface NoElseInWhen : KaFirDiagnostic<KtWhenExpression> {
        override val diagnosticClass get() = NoElseInWhen::class
        val missingWhenCases: List<WhenMissingCase>
        val description: String
    }

    interface InvalidIfAsExpression : KaFirDiagnostic<KtIfExpression> {
        override val diagnosticClass get() = InvalidIfAsExpression::class
    }

    interface ElseMisplacedInWhen : KaFirDiagnostic<KtWhenEntry> {
        override val diagnosticClass get() = ElseMisplacedInWhen::class
    }

    interface RedundantElseInWhen : KaFirDiagnostic<KtWhenEntry> {
        override val diagnosticClass get() = RedundantElseInWhen::class
    }

    interface IllegalDeclarationInWhenSubject : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IllegalDeclarationInWhenSubject::class
        val illegalReason: String
    }

    interface CommaInWhenConditionWithoutArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommaInWhenConditionWithoutArgument::class
    }

    interface DuplicateBranchConditionInWhen : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DuplicateBranchConditionInWhen::class
    }

    interface ConfusingBranchConditionError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConfusingBranchConditionError::class
    }

    interface ConfusingBranchConditionWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConfusingBranchConditionWarning::class
    }

    interface WrongConditionSuggestGuard : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongConditionSuggestGuard::class
    }

    interface CommaInWhenConditionWithWhenGuard : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommaInWhenConditionWithWhenGuard::class
    }

    interface WhenGuardWithoutSubject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WhenGuardWithoutSubject::class
    }

    interface InferredInvisibleWhenTypeError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InferredInvisibleWhenTypeError::class
        val whenType: KaType
        val syntaxConstructionName: String
    }

    interface InferredInvisibleWhenTypeWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InferredInvisibleWhenTypeWarning::class
        val whenType: KaType
        val syntaxConstructionName: String
    }

    interface TypeParameterIsNotAnExpression : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass get() = TypeParameterIsNotAnExpression::class
        val typeParameter: KaTypeParameterSymbol
    }

    interface TypeParameterOnLhsOfDot : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass get() = TypeParameterOnLhsOfDot::class
        val typeParameter: KaTypeParameterSymbol
    }

    interface NoCompanionObject : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NoCompanionObject::class
        val klass: KaClassLikeSymbol
    }

    interface ExpressionExpectedPackageFound : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ExpressionExpectedPackageFound::class
    }

    interface ErrorInContractDescription : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ErrorInContractDescription::class
        val reason: String
    }

    interface ContractNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ContractNotAllowed::class
        val reason: String
    }

    interface NoGetMethod : KaFirDiagnostic<KtArrayAccessExpression> {
        override val diagnosticClass get() = NoGetMethod::class
    }

    interface NoSetMethod : KaFirDiagnostic<KtArrayAccessExpression> {
        override val diagnosticClass get() = NoSetMethod::class
    }

    interface IteratorMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = IteratorMissing::class
    }

    interface HasNextMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = HasNextMissing::class
    }

    interface NextMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NextMissing::class
    }

    interface HasNextFunctionNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = HasNextFunctionNoneApplicable::class
        val candidates: List<KaSymbol>
    }

    interface NextNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NextNoneApplicable::class
        val candidates: List<KaSymbol>
    }

    interface DelegateSpecialFunctionMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegateSpecialFunctionMissing::class
        val expectedFunctionSignature: String
        val delegateType: KaType
        val description: String
    }

    interface DelegateSpecialFunctionAmbiguity : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegateSpecialFunctionAmbiguity::class
        val expectedFunctionSignature: String
        val candidates: List<KaSymbol>
    }

    interface DelegateSpecialFunctionNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegateSpecialFunctionNoneApplicable::class
        val expectedFunctionSignature: String
        val candidates: List<KaSymbol>
    }

    interface DelegateSpecialFunctionReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = DelegateSpecialFunctionReturnTypeMismatch::class
        val delegateFunction: String
        val expectedType: KaType
        val actualType: KaType
    }

    interface UnderscoreIsReserved : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnderscoreIsReserved::class
    }

    interface UnderscoreUsageWithoutBackticks : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnderscoreUsageWithoutBackticks::class
    }

    interface ResolvedToUnderscoreNamedCatchParameter : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass get() = ResolvedToUnderscoreNamedCatchParameter::class
    }

    interface InvalidCharacters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidCharacters::class
        val message: String
    }

    interface EqualityNotApplicable : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = EqualityNotApplicable::class
        val operator: String
        val leftType: KaType
        val rightType: KaType
    }

    interface EqualityNotApplicableWarning : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass get() = EqualityNotApplicableWarning::class
        val operator: String
        val leftType: KaType
        val rightType: KaType
    }

    interface IncompatibleEnumComparisonError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncompatibleEnumComparisonError::class
        val leftType: KaType
        val rightType: KaType
    }

    interface IncompatibleEnumComparison : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IncompatibleEnumComparison::class
        val leftType: KaType
        val rightType: KaType
    }

    interface ForbiddenIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ForbiddenIdentityEquals::class
        val leftType: KaType
        val rightType: KaType
    }

    interface ForbiddenIdentityEqualsWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ForbiddenIdentityEqualsWarning::class
        val leftType: KaType
        val rightType: KaType
    }

    interface DeprecatedIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DeprecatedIdentityEquals::class
        val leftType: KaType
        val rightType: KaType
    }

    interface ImplicitBoxingInIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ImplicitBoxingInIdentityEquals::class
        val leftType: KaType
        val rightType: KaType
    }

    interface IncDecShouldNotReturnUnit : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = IncDecShouldNotReturnUnit::class
    }

    interface AssignmentOperatorShouldReturnUnit : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = AssignmentOperatorShouldReturnUnit::class
        val functionSymbol: KaFunctionSymbol
        val operator: String
    }

    interface NotFunctionAsOperator : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotFunctionAsOperator::class
        val elementName: String
        val elementSymbol: KaSymbol
    }

    interface DslScopeViolation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DslScopeViolation::class
        val calleeSymbol: KaSymbol
    }

    interface ToplevelTypealiasesOnly : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass get() = ToplevelTypealiasesOnly::class
    }

    interface RecursiveTypealiasExpansion : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = RecursiveTypealiasExpansion::class
    }

    interface TypealiasShouldExpandToClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = TypealiasShouldExpandToClass::class
        val expandedType: KaType
    }

    interface ConstructorOrSupertypeOnTypealiasWithTypeProjectionError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConstructorOrSupertypeOnTypealiasWithTypeProjectionError::class
    }

    interface ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning::class
    }

    interface TypealiasExpansionCapturesOuterTypeParameters : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = TypealiasExpansionCapturesOuterTypeParameters::class
        val outerTypeParameters: List<KaTypeParameterSymbol>
    }

    interface RedundantVisibilityModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = RedundantVisibilityModifier::class
    }

    interface RedundantModalityModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass get() = RedundantModalityModifier::class
    }

    interface RedundantReturnUnitType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = RedundantReturnUnitType::class
    }

    interface RedundantSingleExpressionStringTemplate : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantSingleExpressionStringTemplate::class
    }

    interface CanBeVal : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = CanBeVal::class
    }

    interface CanBeValLateinit : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = CanBeValLateinit::class
    }

    interface CanBeValDelayedInitialization : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = CanBeValDelayedInitialization::class
    }

    interface RedundantCallOfConversionMethod : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantCallOfConversionMethod::class
    }

    interface ArrayEqualityOperatorCanBeReplacedWithContentEquals : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = ArrayEqualityOperatorCanBeReplacedWithContentEquals::class
    }

    interface EmptyRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EmptyRange::class
    }

    interface RedundantSetterParameterType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantSetterParameterType::class
    }

    interface UnusedVariable : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = UnusedVariable::class
    }

    interface AssignedValueIsNeverRead : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AssignedValueIsNeverRead::class
    }

    interface VariableInitializerIsRedundant : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VariableInitializerIsRedundant::class
    }

    interface VariableNeverRead : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = VariableNeverRead::class
    }

    interface UselessCallOnNotNull : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UselessCallOnNotNull::class
    }

    interface UnusedAnonymousParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UnusedAnonymousParameter::class
        val parameter: KaSymbol
    }

    interface UnusedExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnusedExpression::class
    }

    interface UnusedLambdaExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnusedLambdaExpression::class
    }

    interface ReturnNotAllowed : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = ReturnNotAllowed::class
    }

    interface NotAFunctionLabel : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = NotAFunctionLabel::class
    }

    interface ReturnInFunctionWithExpressionBody : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = ReturnInFunctionWithExpressionBody::class
    }

    interface ReturnInFunctionWithExpressionBodyWarning : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = ReturnInFunctionWithExpressionBodyWarning::class
    }

    interface ReturnInFunctionWithExpressionBodyAndImplicitType : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = ReturnInFunctionWithExpressionBodyAndImplicitType::class
    }

    interface NoReturnInFunctionWithBlockBody : KaFirDiagnostic<KtDeclarationWithBody> {
        override val diagnosticClass get() = NoReturnInFunctionWithBlockBody::class
    }

    interface RedundantReturn : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = RedundantReturn::class
    }

    interface AnonymousInitializerInInterface : KaFirDiagnostic<KtAnonymousInitializer> {
        override val diagnosticClass get() = AnonymousInitializerInInterface::class
    }

    interface UsageIsNotInlinable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UsageIsNotInlinable::class
        val parameter: KaSymbol
    }

    interface NonLocalReturnNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonLocalReturnNotAllowed::class
        val parameter: KaSymbol
    }

    interface NotYetSupportedInInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NotYetSupportedInInline::class
        val message: String
    }

    interface NothingToInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NothingToInline::class
    }

    interface NullableInlineParameter : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NullableInlineParameter::class
        val parameter: KaSymbol
        val function: KaSymbol
    }

    interface RecursionInInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = RecursionInInline::class
        val symbol: KaSymbol
    }

    interface NonPublicCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonPublicCallFromPublicInline::class
        val inlineDeclaration: KaSymbol
        val referencedDeclaration: KaSymbol
    }

    interface NonPublicInlineCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonPublicInlineCallFromPublicInline::class
        val inlineDeclaration: KaSymbol
        val referencedDeclaration: KaSymbol
    }

    interface NonPublicCallFromPublicInlineDeprecation : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonPublicCallFromPublicInlineDeprecation::class
        val inlineDeclaration: KaSymbol
        val referencedDeclaration: KaSymbol
    }

    interface NonPublicDataCopyCallFromPublicInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonPublicDataCopyCallFromPublicInlineError::class
        val inlineDeclaration: KaSymbol
    }

    interface NonPublicDataCopyCallFromPublicInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonPublicDataCopyCallFromPublicInlineWarning::class
        val inlineDeclaration: KaSymbol
    }

    interface ProtectedConstructorCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ProtectedConstructorCallFromPublicInline::class
        val inlineDeclaration: KaSymbol
        val referencedDeclaration: KaSymbol
    }

    interface ProtectedCallFromPublicInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ProtectedCallFromPublicInlineError::class
        val inlineDeclaration: KaSymbol
        val referencedDeclaration: KaSymbol
    }

    interface PrivateClassMemberFromInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = PrivateClassMemberFromInline::class
        val inlineDeclaration: KaSymbol
        val referencedDeclaration: KaSymbol
    }

    interface SuperCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SuperCallFromPublicInline::class
        val symbol: KaSymbol
    }

    interface DeclarationCantBeInlined : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = DeclarationCantBeInlined::class
    }

    interface DeclarationCantBeInlinedDeprecationError : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = DeclarationCantBeInlinedDeprecationError::class
    }

    interface DeclarationCantBeInlinedDeprecationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = DeclarationCantBeInlinedDeprecationWarning::class
    }

    interface OverrideByInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = OverrideByInline::class
    }

    interface NonInternalPublishedApi : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonInternalPublishedApi::class
    }

    interface InvalidDefaultFunctionalParameterForInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InvalidDefaultFunctionalParameterForInline::class
        val parameter: KaSymbol
    }

    interface NotSupportedInlineParameterInInlineParameterDefaultValue : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NotSupportedInlineParameterInInlineParameterDefaultValue::class
        val parameter: KaSymbol
    }

    interface ReifiedTypeParameterInOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ReifiedTypeParameterInOverride::class
    }

    interface InlinePropertyWithBackingField : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InlinePropertyWithBackingField::class
    }

    interface InlinePropertyWithBackingFieldDeprecationError : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InlinePropertyWithBackingFieldDeprecationError::class
    }

    interface InlinePropertyWithBackingFieldDeprecationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InlinePropertyWithBackingFieldDeprecationWarning::class
    }

    interface IllegalInlineParameterModifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IllegalInlineParameterModifier::class
    }

    interface InlineSuspendFunctionTypeUnsupported : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = InlineSuspendFunctionTypeUnsupported::class
    }

    interface InefficientEqualsOverridingInValueClass : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = InefficientEqualsOverridingInValueClass::class
        val type: KaType
    }

    interface InlineClassDeprecated : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InlineClassDeprecated::class
    }

    interface LessVisibleTypeAccessInInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = LessVisibleTypeAccessInInlineError::class
        val typeVisibility: EffectiveVisibility
        val type: KaType
        val inlineVisibility: EffectiveVisibility
    }

    interface LessVisibleTypeAccessInInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = LessVisibleTypeAccessInInlineWarning::class
        val typeVisibility: EffectiveVisibility
        val type: KaType
        val inlineVisibility: EffectiveVisibility
    }

    interface LessVisibleTypeInInlineAccessedSignatureError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = LessVisibleTypeInInlineAccessedSignatureError::class
        val symbol: KaSymbol
        val typeVisibility: EffectiveVisibility
        val type: KaType
        val inlineVisibility: EffectiveVisibility
    }

    interface LessVisibleTypeInInlineAccessedSignatureWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = LessVisibleTypeInInlineAccessedSignatureWarning::class
        val symbol: KaSymbol
        val typeVisibility: EffectiveVisibility
        val type: KaType
        val inlineVisibility: EffectiveVisibility
    }

    interface LessVisibleContainingClassInInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = LessVisibleContainingClassInInlineError::class
        val symbol: KaSymbol
        val visibility: EffectiveVisibility
        val containingClass: KaClassLikeSymbol
        val inlineVisibility: EffectiveVisibility
    }

    interface LessVisibleContainingClassInInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = LessVisibleContainingClassInInlineWarning::class
        val symbol: KaSymbol
        val visibility: EffectiveVisibility
        val containingClass: KaClassLikeSymbol
        val inlineVisibility: EffectiveVisibility
    }

    interface CallableReferenceToLessVisibleDeclarationInInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallableReferenceToLessVisibleDeclarationInInlineError::class
        val symbol: KaSymbol
        val visibility: EffectiveVisibility
        val inlineVisibility: EffectiveVisibility
    }

    interface CallableReferenceToLessVisibleDeclarationInInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CallableReferenceToLessVisibleDeclarationInInlineWarning::class
        val symbol: KaSymbol
        val visibility: EffectiveVisibility
        val inlineVisibility: EffectiveVisibility
    }

    interface InlineFromHigherPlatform : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InlineFromHigherPlatform::class
        val inlinedBytecodeVersion: String
        val currentModuleBytecodeVersion: String
    }

    interface CannotAllUnderImportFromSingleton : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = CannotAllUnderImportFromSingleton::class
        val objectName: Name
    }

    interface PackageCannotBeImported : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = PackageCannotBeImported::class
    }

    interface CannotBeImported : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = CannotBeImported::class
        val name: Name
    }

    interface ConflictingImport : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = ConflictingImport::class
        val name: Name
    }

    interface OperatorRenamedOnImport : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = OperatorRenamedOnImport::class
    }

    interface TypealiasAsCallableQualifierInImportError : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = TypealiasAsCallableQualifierInImportError::class
        val typealiasName: Name
        val originalClassName: Name
    }

    interface TypealiasAsCallableQualifierInImportWarning : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass get() = TypealiasAsCallableQualifierInImportWarning::class
        val typealiasName: Name
        val originalClassName: Name
    }

    interface IllegalSuspendFunctionCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalSuspendFunctionCall::class
        val suspendCallable: KaSymbol
    }

    interface IllegalSuspendPropertyAccess : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalSuspendPropertyAccess::class
        val suspendCallable: KaSymbol
    }

    interface NonLocalSuspensionPoint : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonLocalSuspensionPoint::class
    }

    interface IllegalRestrictedSuspendingFunctionCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalRestrictedSuspendingFunctionCall::class
    }

    interface NonModifierFormForBuiltInSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonModifierFormForBuiltInSuspend::class
    }

    interface ModifierFormForNonBuiltInSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspend::class
    }

    interface ModifierFormForNonBuiltInSuspendFunError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspendFunError::class
    }

    interface ModifierFormForNonBuiltInSuspendFunWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ModifierFormForNonBuiltInSuspendFunWarning::class
    }

    interface ReturnForBuiltInSuspend : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass get() = ReturnForBuiltInSuspend::class
    }

    interface MixingSuspendAndNonSuspendSupertypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MixingSuspendAndNonSuspendSupertypes::class
    }

    interface MixingFunctionalKindsInSupertypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MixingFunctionalKindsInSupertypes::class
        val kinds: List<FunctionTypeKind>
    }

    interface RedundantLabelWarning : KaFirDiagnostic<KtLabelReferenceExpression> {
        override val diagnosticClass get() = RedundantLabelWarning::class
    }

    interface MultipleLabelsAreForbidden : KaFirDiagnostic<KtLabelReferenceExpression> {
        override val diagnosticClass get() = MultipleLabelsAreForbidden::class
    }

    interface DeprecatedAccessToEnumEntryCompanionProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedAccessToEnumEntryCompanionProperty::class
    }

    interface DeprecatedAccessToEntryPropertyFromEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedAccessToEntryPropertyFromEnum::class
    }

    interface DeprecatedAccessToEntriesProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedAccessToEntriesProperty::class
    }

    interface DeprecatedAccessToEnumEntryPropertyAsReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedAccessToEnumEntryPropertyAsReference::class
    }

    interface DeprecatedAccessToEntriesAsQualifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedAccessToEntriesAsQualifier::class
    }

    interface DeclarationOfEnumEntryEntriesError : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass get() = DeclarationOfEnumEntryEntriesError::class
    }

    interface DeclarationOfEnumEntryEntriesWarning : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass get() = DeclarationOfEnumEntryEntriesWarning::class
    }

    interface IncompatibleClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IncompatibleClass::class
        val presentableString: String
        val incompatibility: IncompatibleVersionErrorData<*>
    }

    interface PreReleaseClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PreReleaseClass::class
        val presentableString: String
        val poisoningFeatures: List<String>
    }

    interface IrWithUnstableAbiCompiledClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IrWithUnstableAbiCompiledClass::class
        val presentableString: String
    }

    interface BuilderInferenceStubReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = BuilderInferenceStubReceiver::class
        val typeParameterName: Name
        val containingDeclarationName: Name
    }

    interface BuilderInferenceMultiLambdaRestriction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = BuilderInferenceMultiLambdaRestriction::class
        val typeParameterName: Name
        val containingDeclarationName: Name
    }

    interface OverrideCannotBeStatic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OverrideCannotBeStatic::class
    }

    interface JvmStaticNotInObjectOrClassCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticNotInObjectOrClassCompanion::class
    }

    interface JvmStaticNotInObjectOrCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticNotInObjectOrCompanion::class
    }

    interface JvmStaticOnNonPublicMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticOnNonPublicMember::class
    }

    interface JvmStaticOnConstOrJvmField : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticOnConstOrJvmField::class
    }

    interface JvmStaticOnExternalInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmStaticOnExternalInInterface::class
    }

    interface InapplicableJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableJvmName::class
    }

    interface IllegalJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalJvmName::class
    }

    interface FunctionDelegateMemberNameClash : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FunctionDelegateMemberNameClash::class
    }

    interface ValueClassWithoutJvmInlineAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ValueClassWithoutJvmInlineAnnotation::class
    }

    interface JvmInlineWithoutValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmInlineWithoutValueClass::class
    }

    interface InapplicableJvmExposeBoxedWithName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InapplicableJvmExposeBoxedWithName::class
    }

    interface UselessJvmExposeBoxed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UselessJvmExposeBoxed::class
    }

    interface JvmExposeBoxedCannotExposeSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmExposeBoxedCannotExposeSuspend::class
    }

    interface JvmExposeBoxedRequiresName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmExposeBoxedRequiresName::class
    }

    interface JvmExposeBoxedCannotBeTheSame : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmExposeBoxedCannotBeTheSame::class
    }

    interface JvmExposeBoxedCannotBeTheSameAsJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmExposeBoxedCannotBeTheSameAsJvmName::class
    }

    interface JvmExposeBoxedCannotExposeOpenAbstract : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmExposeBoxedCannotExposeOpenAbstract::class
    }

    interface JvmExposeBoxedCannotExposeSynthetic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmExposeBoxedCannotExposeSynthetic::class
    }

    interface JvmExposeBoxedCannotExposeLocals : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmExposeBoxedCannotExposeLocals::class
    }

    interface JvmExposeBoxedCannotExposeReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmExposeBoxedCannotExposeReified::class
    }

    interface WrongNullabilityForJavaOverride : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongNullabilityForJavaOverride::class
        val override: KaCallableSymbol
        val base: KaCallableSymbol
    }

    interface AccidentalOverrideClashByJvmSignature : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass get() = AccidentalOverrideClashByJvmSignature::class
        val hidden: KaFunctionSymbol
        val overrideDescription: String
        val regular: KaFunctionSymbol
    }

    interface ImplementationByDelegationWithDifferentGenericSignatureError : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ImplementationByDelegationWithDifferentGenericSignatureError::class
        val base: KaFunctionSymbol
        val override: KaFunctionSymbol
    }

    interface ImplementationByDelegationWithDifferentGenericSignatureWarning : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass get() = ImplementationByDelegationWithDifferentGenericSignatureWarning::class
        val base: KaFunctionSymbol
        val override: KaFunctionSymbol
    }

    interface NotYetSupportedLocalInlineFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = NotYetSupportedLocalInlineFunction::class
    }

    interface PropertyHidesJavaField : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass get() = PropertyHidesJavaField::class
        val hidden: KaVariableSymbol
    }

    interface JavaTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = JavaTypeMismatch::class
        val expectedType: KaType
        val actualType: KaType
    }

    interface ReceiverNullabilityMismatchBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ReceiverNullabilityMismatchBasedOnJavaAnnotations::class
        val actualType: KaType
        val expectedType: KaType
        val messageSuffix: String
    }

    interface NullabilityMismatchBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NullabilityMismatchBasedOnJavaAnnotations::class
        val actualType: KaType
        val expectedType: KaType
        val messageSuffix: String
    }

    interface NullabilityMismatchBasedOnExplicitTypeArgumentsForJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NullabilityMismatchBasedOnExplicitTypeArgumentsForJava::class
        val actualType: KaType
        val expectedType: KaType
        val messageSuffix: String
    }

    interface TypeMismatchWhenFlexibilityChanges : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeMismatchWhenFlexibilityChanges::class
        val actualType: KaType
        val expectedType: KaType
    }

    interface JavaClassOnCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaClassOnCompanion::class
        val actualType: KaType
        val expectedType: KaType
    }

    interface UpperBoundCannotBeArray : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundCannotBeArray::class
    }

    interface UpperBoundViolatedBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolatedBasedOnJavaAnnotations::class
        val expectedUpperBound: KaType
        val actualUpperBound: KaType
    }

    interface UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations::class
        val expectedUpperBound: KaType
        val actualUpperBound: KaType
    }

    interface StrictfpOnClass : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = StrictfpOnClass::class
    }

    interface SynchronizedOnAbstract : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnAbstract::class
    }

    interface SynchronizedInInterface : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedInInterface::class
    }

    interface SynchronizedInAnnotationError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedInAnnotationError::class
    }

    interface SynchronizedInAnnotationWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedInAnnotationWarning::class
    }

    interface SynchronizedOnInline : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnInline::class
    }

    interface SynchronizedOnValueClassError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnValueClassError::class
    }

    interface SynchronizedOnValueClassWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnValueClassWarning::class
    }

    interface SynchronizedOnSuspendError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnSuspendError::class
    }

    interface SynchronizedOnSuspendWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = SynchronizedOnSuspendWarning::class
    }

    interface OverloadsWithoutDefaultArguments : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsWithoutDefaultArguments::class
    }

    interface OverloadsAbstract : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsAbstract::class
    }

    interface OverloadsInterface : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsInterface::class
    }

    interface OverloadsLocal : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsLocal::class
    }

    interface OverloadsAnnotationClassConstructorError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsAnnotationClassConstructorError::class
    }

    interface OverloadsPrivate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = OverloadsPrivate::class
    }

    interface DeprecatedJavaAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = DeprecatedJavaAnnotation::class
        val kotlinName: FqName
    }

    interface JvmPackageNameCannotBeEmpty : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmPackageNameCannotBeEmpty::class
    }

    interface JvmPackageNameMustBeValidName : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmPackageNameMustBeValidName::class
    }

    interface JvmPackageNameNotSupportedInFilesWithClasses : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmPackageNameNotSupportedInFilesWithClasses::class
    }

    interface PositionedValueArgumentForJavaAnnotation : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = PositionedValueArgumentForJavaAnnotation::class
    }

    interface RedundantRepeatableAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RedundantRepeatableAnnotation::class
        val kotlinRepeatable: FqName
        val javaRepeatable: FqName
    }

    interface ThrowsInAnnotationError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = ThrowsInAnnotationError::class
    }

    interface ThrowsInAnnotationWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = ThrowsInAnnotationWarning::class
    }

    interface JvmSerializableLambdaOnInlinedFunctionLiteralsError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmSerializableLambdaOnInlinedFunctionLiteralsError::class
    }

    interface JvmSerializableLambdaOnInlinedFunctionLiteralsWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmSerializableLambdaOnInlinedFunctionLiteralsWarning::class
    }

    interface IncompatibleAnnotationTargets : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = IncompatibleAnnotationTargets::class
        val missingJavaTargets: List<String>
        val correspondingKotlinTargets: List<String>
    }

    interface AnnotationTargetsOnlyInJava : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = AnnotationTargetsOnlyInJava::class
    }

    interface LocalJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LocalJvmRecord::class
    }

    interface NonFinalJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonFinalJvmRecord::class
    }

    interface EnumJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EnumJvmRecord::class
    }

    interface JvmRecordWithoutPrimaryConstructorParameters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmRecordWithoutPrimaryConstructorParameters::class
    }

    interface NonDataClassJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonDataClassJvmRecord::class
    }

    interface JvmRecordNotValParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmRecordNotValParameter::class
    }

    interface JvmRecordNotLastVarargParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmRecordNotLastVarargParameter::class
    }

    interface InnerJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InnerJvmRecord::class
    }

    interface FieldInJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FieldInJvmRecord::class
    }

    interface DelegationByInJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DelegationByInJvmRecord::class
    }

    interface JvmRecordExtendsClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JvmRecordExtendsClass::class
        val superType: KaType
    }

    interface IllegalJavaLangRecordSupertype : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalJavaLangRecordSupertype::class
    }

    interface JavaModuleDoesNotDependOnModule : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaModuleDoesNotDependOnModule::class
        val moduleName: String
    }

    interface JavaModuleDoesNotReadUnnamedModule : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaModuleDoesNotReadUnnamedModule::class
    }

    interface JavaModuleDoesNotExportPackage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaModuleDoesNotExportPackage::class
        val moduleName: String
        val packageName: String
    }

    interface JvmDefaultWithoutCompatibilityNotInEnableMode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JvmDefaultWithoutCompatibilityNotInEnableMode::class
    }

    interface JvmDefaultWithCompatibilityNotInNoCompatibilityMode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JvmDefaultWithCompatibilityNotInNoCompatibilityMode::class
    }

    interface ExternalDeclarationCannotBeAbstract : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExternalDeclarationCannotBeAbstract::class
    }

    interface ExternalDeclarationCannotHaveBody : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExternalDeclarationCannotHaveBody::class
    }

    interface ExternalDeclarationInInterface : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExternalDeclarationInInterface::class
    }

    interface ExternalDeclarationCannotBeInlined : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = ExternalDeclarationCannotBeInlined::class
    }

    interface NonSourceRepeatedAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = NonSourceRepeatedAnnotation::class
    }

    interface RepeatedAnnotationWithContainer : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatedAnnotationWithContainer::class
        val name: ClassId
        val explicitContainerName: ClassId
    }

    interface RepeatableContainerMustHaveValueArrayError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerMustHaveValueArrayError::class
        val container: ClassId
        val annotation: ClassId
    }

    interface RepeatableContainerHasNonDefaultParameterError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerHasNonDefaultParameterError::class
        val container: ClassId
        val nonDefault: Name
    }

    interface RepeatableContainerHasShorterRetentionError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerHasShorterRetentionError::class
        val container: ClassId
        val retention: String
        val annotation: ClassId
        val annotationRetention: String
    }

    interface RepeatableContainerTargetSetNotASubsetError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableContainerTargetSetNotASubsetError::class
        val container: ClassId
        val annotation: ClassId
    }

    interface RepeatableAnnotationHasNestedClassNamedContainerError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = RepeatableAnnotationHasNestedClassNamedContainerError::class
    }

    interface SuspensionPointInsideCriticalSection : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SuspensionPointInsideCriticalSection::class
        val function: KaCallableSymbol
    }

    interface InapplicableJvmField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableJvmField::class
        val message: String
    }

    interface InapplicableJvmFieldWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = InapplicableJvmFieldWarning::class
        val message: String
    }

    interface IdentitySensitiveOperationsWithValueType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = IdentitySensitiveOperationsWithValueType::class
        val type: KaType
    }

    interface SynchronizedBlockOnJavaValueBasedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SynchronizedBlockOnJavaValueBasedClass::class
        val type: KaType
    }

    interface SynchronizedBlockOnValueClassOrPrimitiveError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SynchronizedBlockOnValueClassOrPrimitiveError::class
        val valueClassOrPrimitive: KaType
    }

    interface SynchronizedBlockOnValueClassOrPrimitiveWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SynchronizedBlockOnValueClassOrPrimitiveWarning::class
        val valueClassOrPrimitive: KaType
    }

    interface JvmSyntheticOnDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass get() = JvmSyntheticOnDelegate::class
    }

    interface SubclassCantCallCompanionProtectedNonStatic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SubclassCantCallCompanionProtectedNonStatic::class
    }

    interface ConcurrentHashMapContainsOperatorError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConcurrentHashMapContainsOperatorError::class
    }

    interface SpreadOnSignaturePolymorphicCallError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpreadOnSignaturePolymorphicCallError::class
    }

    interface JavaSamInterfaceConstructorReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaSamInterfaceConstructorReference::class
    }

    interface NoReflectionInClassPath : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoReflectionInClassPath::class
    }

    interface SyntheticPropertyWithoutJavaOrigin : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SyntheticPropertyWithoutJavaOrigin::class
        val originalSymbol: KaFunctionSymbol
        val functionName: Name
    }

    interface JavaFieldShadowedByKotlinProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaFieldShadowedByKotlinProperty::class
        val kotlinProperty: KaVariableSymbol
    }

    interface MissingBuiltInDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingBuiltInDeclaration::class
        val symbol: KaSymbol
    }

    interface DangerousCharacters : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass get() = DangerousCharacters::class
        val characters: String
    }

    interface ImplementingFunctionInterface : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass get() = ImplementingFunctionInterface::class
    }

    interface OverridingExternalFunWithOptionalParams : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = OverridingExternalFunWithOptionalParams::class
    }

    interface OverridingExternalFunWithOptionalParamsWithFake : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = OverridingExternalFunWithOptionalParamsWithFake::class
        val function: KaFunctionSymbol
    }

    interface CallToDefinedExternallyFromNonExternalDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CallToDefinedExternallyFromNonExternalDeclaration::class
    }

    interface ExternalEnumEntryWithBody : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalEnumEntryWithBody::class
    }

    interface ExternalTypeExtendsNonExternalType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalTypeExtendsNonExternalType::class
    }

    interface EnumClassInExternalDeclarationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = EnumClassInExternalDeclarationWarning::class
    }

    interface InlineClassInExternalDeclarationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InlineClassInExternalDeclarationWarning::class
    }

    interface InlineClassInExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = InlineClassInExternalDeclaration::class
    }

    interface ExtensionFunctionInExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExtensionFunctionInExternalDeclaration::class
    }

    interface NonExternalDeclarationInInappropriateFile : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonExternalDeclarationInInappropriateFile::class
        val type: KaType
    }

    interface JsExternalInheritorsOnly : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = JsExternalInheritorsOnly::class
        val parent: KaClassLikeSymbol
        val kid: KaClassLikeSymbol
    }

    interface JsExternalArgument : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = JsExternalArgument::class
        val argType: KaType
    }

    interface WrongExportedDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongExportedDeclaration::class
        val kind: String
    }

    interface NonExportableType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonExportableType::class
        val kind: String
        val type: KaType
    }

    interface NonConsumableExportedIdentifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NonConsumableExportedIdentifier::class
        val name: String
    }

    interface NamedCompanionInExportedInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NamedCompanionInExportedInterface::class
    }

    interface NotExportedActualDeclarationWhileExpectIsExported : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NotExportedActualDeclarationWhileExpectIsExported::class
    }

    interface NestedJsExport : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NestedJsExport::class
    }

    interface DelegationByDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = DelegationByDynamic::class
    }

    interface PropertyDelegationByDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = PropertyDelegationByDynamic::class
    }

    interface SpreadOperatorInDynamicCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = SpreadOperatorInDynamicCall::class
    }

    interface WrongOperationWithDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongOperationWithDynamic::class
        val operation: String
    }

    interface JsStaticNotInClassCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JsStaticNotInClassCompanion::class
    }

    interface JsStaticOnNonPublicMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JsStaticOnNonPublicMember::class
    }

    interface JsStaticOnConst : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JsStaticOnConst::class
    }

    interface Syntax : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass get() = Syntax::class
        val message: String
    }

    interface NestedExternalDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NestedExternalDeclaration::class
    }

    interface WrongExternalDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = WrongExternalDeclaration::class
        val classKind: String
    }

    interface NestedClassInExternalInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NestedClassInExternalInterface::class
    }

    interface InlineExternalDeclaration : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass get() = InlineExternalDeclaration::class
    }

    interface NonAbstractMemberOfExternalInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass get() = NonAbstractMemberOfExternalInterface::class
    }

    interface ExternalClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass get() = ExternalClassConstructorPropertyParameter::class
    }

    interface ExternalAnonymousInitializer : KaFirDiagnostic<KtAnonymousInitializer> {
        override val diagnosticClass get() = ExternalAnonymousInitializer::class
    }

    interface ExternalDelegation : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalDelegation::class
    }

    interface ExternalDelegatedConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalDelegatedConstructorCall::class
    }

    interface WrongBodyOfExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongBodyOfExternalDeclaration::class
    }

    interface WrongInitializerOfExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongInitializerOfExternalDeclaration::class
    }

    interface WrongDefaultValueForExternalFunParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = WrongDefaultValueForExternalFunParameter::class
    }

    interface CannotCheckForExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = CannotCheckForExternalInterface::class
        val targetType: KaType
    }

    interface UncheckedCastToExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = UncheckedCastToExternalInterface::class
        val sourceType: KaType
        val targetType: KaType
    }

    interface ExternalInterfaceAsClassLiteral : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalInterfaceAsClassLiteral::class
    }

    interface ExternalInterfaceAsReifiedTypeArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = ExternalInterfaceAsReifiedTypeArgument::class
        val typeArgument: KaType
    }

    interface NamedCompanionInExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = NamedCompanionInExternalInterface::class
    }

    interface JscodeArgumentNonConstExpression : KaFirDiagnostic<KtElement> {
        override val diagnosticClass get() = JscodeArgumentNonConstExpression::class
    }

}
