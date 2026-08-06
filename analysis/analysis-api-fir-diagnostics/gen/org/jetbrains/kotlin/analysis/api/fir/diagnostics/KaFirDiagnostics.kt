/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlin.reflect.KClass
import org.jetbrains.kotlin.analysis.api.components.KaWhenMissingCase
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
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
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

public sealed interface KaFirDiagnostic<PSI : PsiElement> : KaDiagnosticWithPsi<PSI> {
    public interface Unsupported : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<Unsupported>
            get() = Unsupported::class

        public val unsupported: String
    }

    public interface UnsupportedFeature : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedFeature>
            get() = UnsupportedFeature::class

        public val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>
    }

    public interface UnsupportedSuspendTest : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedSuspendTest>
            get() = UnsupportedSuspendTest::class
    }

    public interface NewInferenceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NewInferenceError>
            get() = NewInferenceError::class

        public val error: String
    }

    public interface EscapingCapturedVariable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<EscapingCapturedVariable>
            get() = EscapingCapturedVariable::class

        public val variable: KaVariableSymbol
    }

    public interface OtherError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OtherError>
            get() = OtherError::class
    }

    public interface OtherErrorWithReason : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OtherErrorWithReason>
            get() = OtherErrorWithReason::class

        public val reason: String
    }

    public interface IllegalConstExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalConstExpression>
            get() = IllegalConstExpression::class
    }

    public interface IllegalUnderscore : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalUnderscore>
            get() = IllegalUnderscore::class
    }

    public interface ExpressionExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpressionExpected>
            get() = ExpressionExpected::class
    }

    public interface AssignmentInExpressionContext : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<AssignmentInExpressionContext>
            get() = AssignmentInExpressionContext::class
    }

    public interface BreakOrContinueOutsideALoop : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<BreakOrContinueOutsideALoop>
            get() = BreakOrContinueOutsideALoop::class
    }

    public interface NotALoopLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotALoopLabel>
            get() = NotALoopLabel::class
    }

    public interface BreakOrContinueJumpsAcrossFunctionBoundary : KaFirDiagnostic<KtExpressionWithLabel> {
        override val diagnosticClass: KClass<BreakOrContinueJumpsAcrossFunctionBoundary>
            get() = BreakOrContinueJumpsAcrossFunctionBoundary::class
    }

    public interface VariableExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VariableExpected>
            get() = VariableExpected::class
    }

    public interface DelegationInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegationInInterface>
            get() = DelegationInInterface::class
    }

    public interface DelegationNotToInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegationNotToInterface>
            get() = DelegationNotToInterface::class
    }

    public interface NestedClassNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NestedClassNotAllowed>
            get() = NestedClassNotAllowed::class

        public val declaration: String
    }

    public interface NestedClassNotAllowedInLocalError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NestedClassNotAllowedInLocalError>
            get() = NestedClassNotAllowedInLocalError::class

        public val declaration: String
    }

    public interface NestedClassNotAllowedInLocalWarning : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NestedClassNotAllowedInLocalWarning>
            get() = NestedClassNotAllowedInLocalWarning::class

        public val declaration: String
    }

    public interface IncorrectCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IncorrectCharacterLiteral>
            get() = IncorrectCharacterLiteral::class
    }

    public interface EmptyCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<EmptyCharacterLiteral>
            get() = EmptyCharacterLiteral::class
    }

    public interface TooManyCharactersInCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TooManyCharactersInCharacterLiteral>
            get() = TooManyCharactersInCharacterLiteral::class
    }

    public interface IllegalEscape : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalEscape>
            get() = IllegalEscape::class
    }

    public interface IntLiteralOutOfRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IntLiteralOutOfRange>
            get() = IntLiteralOutOfRange::class
    }

    public interface IntLiteralWithLeadingZeros : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IntLiteralWithLeadingZeros>
            get() = IntLiteralWithLeadingZeros::class
    }

    public interface FloatLiteralOutOfRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FloatLiteralOutOfRange>
            get() = FloatLiteralOutOfRange::class
    }

    public interface WrongLongSuffix : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongLongSuffix>
            get() = WrongLongSuffix::class
    }

    public interface UnsignedLiteralWithoutDeclarationsOnClasspath : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnsignedLiteralWithoutDeclarationsOnClasspath>
            get() = UnsignedLiteralWithoutDeclarationsOnClasspath::class
    }

    public interface DivisionByZero : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DivisionByZero>
            get() = DivisionByZero::class
    }

    public interface TrimMarginBlankPrefix : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<TrimMarginBlankPrefix>
            get() = TrimMarginBlankPrefix::class
    }

    public interface ValOrVarOnLoopParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValOrVarOnLoopParameter>
            get() = ValOrVarOnLoopParameter::class

        public val valOrVar: KtKeywordToken
    }

    public interface ValOrVarOnFunParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValOrVarOnFunParameter>
            get() = ValOrVarOnFunParameter::class

        public val valOrVar: KtKeywordToken
    }

    public interface ValOrVarOnCatchParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValOrVarOnCatchParameter>
            get() = ValOrVarOnCatchParameter::class

        public val valOrVar: KtKeywordToken
    }

    public interface ValOrVarOnSecondaryConstructorParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValOrVarOnSecondaryConstructorParameter>
            get() = ValOrVarOnSecondaryConstructorParameter::class

        public val valOrVar: KtKeywordToken
    }

    public interface InnerOnTopLevelScriptClassError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InnerOnTopLevelScriptClassError>
            get() = InnerOnTopLevelScriptClassError::class
    }

    public interface InnerOnTopLevelScriptClassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InnerOnTopLevelScriptClassWarning>
            get() = InnerOnTopLevelScriptClassWarning::class
    }

    public interface ErrorSuppression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ErrorSuppression>
            get() = ErrorSuppression::class

        public val diagnosticName: String
    }

    public interface MissingConstructorKeyword : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingConstructorKeyword>
            get() = MissingConstructorKeyword::class
    }

    public interface RedundantInterpolationPrefix : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantInterpolationPrefix>
            get() = RedundantInterpolationPrefix::class
    }

    public interface WrappedLhsInAssignmentError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrappedLhsInAssignmentError>
            get() = WrappedLhsInAssignmentError::class
    }

    public interface WrappedLhsInAssignmentWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrappedLhsInAssignmentWarning>
            get() = WrappedLhsInAssignmentWarning::class
    }

    public interface ParenthesizedPackageQualifierError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ParenthesizedPackageQualifierError>
            get() = ParenthesizedPackageQualifierError::class
    }

    public interface ParenthesizedPackageQualifierWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ParenthesizedPackageQualifierWarning>
            get() = ParenthesizedPackageQualifierWarning::class
    }

    public interface KotlinPackageUsage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<KotlinPackageUsage>
            get() = KotlinPackageUsage::class
    }

    public interface UnsupportedArrayLiteralOutsideOfAnnotationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedArrayLiteralOutsideOfAnnotationError>
            get() = UnsupportedArrayLiteralOutsideOfAnnotationError::class
    }

    public interface UnsupportedArrayLiteralOutsideOfAnnotationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedArrayLiteralOutsideOfAnnotationWarning>
            get() = UnsupportedArrayLiteralOutsideOfAnnotationWarning::class
    }

    public interface UnresolvedReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnresolvedReference>
            get() = UnresolvedReference::class

        public val reference: String
        public val operator: String?
        public val receiverType: KaType?
    }

    public interface UnresolvedReferenceWrongReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnresolvedReferenceWrongReceiver>
            get() = UnresolvedReferenceWrongReceiver::class

        public val candidate: KaSymbol
        public val operator: String?
    }

    public interface InaccessibleOuterClassReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InaccessibleOuterClassReceiver>
            get() = InaccessibleOuterClassReceiver::class

        public val symbol: KaSymbol
    }

    public interface UnresolvedImport : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnresolvedImport>
            get() = UnresolvedImport::class

        public val reference: String
    }

    public interface InvisibleReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvisibleReference>
            get() = InvisibleReference::class

        public val reference: KaSymbol
        public val visible: Visibility
        public val containingDeclaration: ClassId?
    }

    public interface InvisibleReferenceWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvisibleReferenceWarning>
            get() = InvisibleReferenceWarning::class

        public val reference: KaSymbol
        public val visible: Visibility
        public val containingDeclaration: ClassId?
    }

    public interface InvisibleSetter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvisibleSetter>
            get() = InvisibleSetter::class

        public val property: KaVariableSymbol
        public val visibility: Visibility
        public val callableId: CallableId
    }

    public interface UnresolvedLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnresolvedLabel>
            get() = UnresolvedLabel::class
    }

    public interface AmbiguousLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AmbiguousLabel>
            get() = AmbiguousLabel::class
    }

    public interface LabelNameClash : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LabelNameClash>
            get() = LabelNameClash::class
    }

    public interface DeserializationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeserializationError>
            get() = DeserializationError::class
    }

    public interface ErrorFromJavaResolution : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ErrorFromJavaResolution>
            get() = ErrorFromJavaResolution::class
    }

    public interface MissingStdlibClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingStdlibClass>
            get() = MissingStdlibClass::class
    }

    public interface NoThis : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoThis>
            get() = NoThis::class
    }

    public interface ApiNotAvailable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ApiNotAvailable>
            get() = ApiNotAvailable::class

        public val sinceKotlinVersion: ApiVersion
        public val currentVersion: ApiVersion
    }

    public interface PlaceholderProjectionInQualifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PlaceholderProjectionInQualifier>
            get() = PlaceholderProjectionInQualifier::class
    }

    public interface PlaceholderProjectionInTyperef : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PlaceholderProjectionInTyperef>
            get() = PlaceholderProjectionInTyperef::class
    }

    public interface DuplicateParameterNameInFunctionType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DuplicateParameterNameInFunctionType>
            get() = DuplicateParameterNameInFunctionType::class
    }

    public interface MissingDependencyClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClass>
            get() = MissingDependencyClass::class

        public val type: KaType
    }

    public interface MissingDependencyClassInExpressionType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClassInExpressionType>
            get() = MissingDependencyClassInExpressionType::class

        public val type: KaType
    }

    public interface MissingDependencySuperclass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencySuperclass>
            get() = MissingDependencySuperclass::class

        public val missingTypeConstructorName: FqName
        public val declarationTypeConstructorName: FqName
    }

    public interface MissingDependencySuperclassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencySuperclassWarning>
            get() = MissingDependencySuperclassWarning::class

        public val missingTypeConstructorName: FqName
        public val declarationTypeConstructorName: FqName
    }

    public interface MissingDependencyClassInLambdaParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClassInLambdaParameter>
            get() = MissingDependencyClassInLambdaParameter::class

        public val type: KaType
        public val parameterName: Name
    }

    public interface MissingDependencyClassInLambdaReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClassInLambdaReceiver>
            get() = MissingDependencyClassInLambdaReceiver::class

        public val type: KaType
    }

    public interface MissingDependencyClassInTypealias : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClassInTypealias>
            get() = MissingDependencyClassInTypealias::class

        public val missingType: KaType
        public val declarationType: KaType
    }

    public interface MissingDependencyInInferredTypeAnnotationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyInInferredTypeAnnotationError>
            get() = MissingDependencyInInferredTypeAnnotationError::class

        public val type: KaType
    }

    public interface MissingDependencyInInferredTypeAnnotationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyInInferredTypeAnnotationWarning>
            get() = MissingDependencyInInferredTypeAnnotationWarning::class

        public val type: KaType
    }

    public interface RootIdePackageDeprecated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RootIdePackageDeprecated>
            get() = RootIdePackageDeprecated::class
    }

    public interface CreatingAnInstanceOfAbstractClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CreatingAnInstanceOfAbstractClass>
            get() = CreatingAnInstanceOfAbstractClass::class
    }

    public interface NoConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoConstructor>
            get() = NoConstructor::class
    }

    public interface NoImplicitDefaultConstructorOnExpectClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoImplicitDefaultConstructorOnExpectClass>
            get() = NoImplicitDefaultConstructorOnExpectClass::class
    }

    public interface FunctionCallExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FunctionCallExpected>
            get() = FunctionCallExpected::class

        public val functionName: String
        public val hasValueParameters: Boolean
    }

    public interface IllegalSelector : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalSelector>
            get() = IllegalSelector::class
    }

    public interface NoReceiverAllowed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoReceiverAllowed>
            get() = NoReceiverAllowed::class
    }

    public interface FunctionExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FunctionExpected>
            get() = FunctionExpected::class

        public val expression: String
        public val type: KaType
    }

    public interface InterfaceAsFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InterfaceAsFunction>
            get() = InterfaceAsFunction::class

        public val classSymbol: KaClassLikeSymbol
    }

    public interface ExpectClassAsFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpectClassAsFunction>
            get() = ExpectClassAsFunction::class

        public val classSymbol: KaClassLikeSymbol
    }

    public interface InnerClassConstructorNoReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InnerClassConstructorNoReceiver>
            get() = InnerClassConstructorNoReceiver::class

        public val classSymbol: KaClassLikeSymbol
    }

    public interface PluginAmbiguousInterceptedSymbol : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PluginAmbiguousInterceptedSymbol>
            get() = PluginAmbiguousInterceptedSymbol::class

        public val names: List<String>
    }

    public interface ResolutionToClassifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ResolutionToClassifier>
            get() = ResolutionToClassifier::class

        public val classSymbol: KaClassLikeSymbol
    }

    public interface AmbiguousAlteredAssign : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AmbiguousAlteredAssign>
            get() = AmbiguousAlteredAssign::class

        public val altererNames: List<String?>
    }

    public interface SelfCallInNestedObjectConstructorError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SelfCallInNestedObjectConstructorError>
            get() = SelfCallInNestedObjectConstructorError::class
    }

    public interface AmbiguousCollectionLiteral : KaFirDiagnostic<KtCollectionLiteralExpression> {
        override val diagnosticClass: KClass<AmbiguousCollectionLiteral>
            get() = AmbiguousCollectionLiteral::class

        public val candidatesWithOf: List<KaClassLikeSymbol>
    }

    public interface UnresolvedCollectionLiteral : KaFirDiagnostic<KtCollectionLiteralExpression> {
        override val diagnosticClass: KClass<UnresolvedCollectionLiteral>
            get() = UnresolvedCollectionLiteral::class

        public val incompatibleBound: KaType
    }

    public interface ImplicitPropertyTypeMakesBehaviorOrderDependant : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ImplicitPropertyTypeMakesBehaviorOrderDependant>
            get() = ImplicitPropertyTypeMakesBehaviorOrderDependant::class

        public val property: KaVariableSymbol
    }

    public interface ImplicitPropertyTypeMakesBehaviorOrderDependantError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ImplicitPropertyTypeMakesBehaviorOrderDependantError>
            get() = ImplicitPropertyTypeMakesBehaviorOrderDependantError::class

        public val property: KaVariableSymbol
    }

    public interface SuperIsNotAnExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuperIsNotAnExpression>
            get() = SuperIsNotAnExpression::class
    }

    public interface SuperNotAvailable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuperNotAvailable>
            get() = SuperNotAvailable::class
    }

    public interface AbstractSuperCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AbstractSuperCall>
            get() = AbstractSuperCall::class
    }

    public interface InstanceAccessBeforeSuperCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InstanceAccessBeforeSuperCall>
            get() = InstanceAccessBeforeSuperCall::class

        public val target: String
    }

    public interface SuperCallWithDefaultParameters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuperCallWithDefaultParameters>
            get() = SuperCallWithDefaultParameters::class

        public val name: String
    }

    public interface InterfaceCantCallDefaultMethodViaSuper : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InterfaceCantCallDefaultMethodViaSuper>
            get() = InterfaceCantCallDefaultMethodViaSuper::class
    }

    public interface JavaClassInheritsKtPrivateClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JavaClassInheritsKtPrivateClass>
            get() = JavaClassInheritsKtPrivateClass::class

        public val javaClassId: ClassId
        public val privateKotlinType: KaType
    }

    public interface NotASupertype : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotASupertype>
            get() = NotASupertype::class
    }

    public interface TypeArgumentsRedundantInSuperQualifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypeArgumentsRedundantInSuperQualifier>
            get() = TypeArgumentsRedundantInSuperQualifier::class
    }

    public interface SuperclassNotAccessibleFromInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuperclassNotAccessibleFromInterface>
            get() = SuperclassNotAccessibleFromInterface::class
    }

    public interface SupertypeInitializedInInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeInitializedInInterface>
            get() = SupertypeInitializedInInterface::class
    }

    public interface InterfaceWithSuperclass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InterfaceWithSuperclass>
            get() = InterfaceWithSuperclass::class
    }

    public interface FinalSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<FinalSupertype>
            get() = FinalSupertype::class
    }

    public interface ClassCannotBeExtendedDirectly : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ClassCannotBeExtendedDirectly>
            get() = ClassCannotBeExtendedDirectly::class

        public val classSymbol: KaClassLikeSymbol
    }

    public interface SupertypeIsExtensionOrContextFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeIsExtensionOrContextFunctionType>
            get() = SupertypeIsExtensionOrContextFunctionType::class
    }

    public interface SingletonInSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SingletonInSupertype>
            get() = SingletonInSupertype::class
    }

    public interface NullableSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableSupertype>
            get() = NullableSupertype::class
    }

    public interface NullableSupertypeThroughTypealiasError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableSupertypeThroughTypealiasError>
            get() = NullableSupertypeThroughTypealiasError::class
    }

    public interface NullableSupertypeThroughTypealiasWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableSupertypeThroughTypealiasWarning>
            get() = NullableSupertypeThroughTypealiasWarning::class
    }

    public interface ManyClassesInSupertypeList : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ManyClassesInSupertypeList>
            get() = ManyClassesInSupertypeList::class
    }

    public interface SupertypeAppearsTwice : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeAppearsTwice>
            get() = SupertypeAppearsTwice::class
    }

    public interface ClassInSupertypeForEnum : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ClassInSupertypeForEnum>
            get() = ClassInSupertypeForEnum::class
    }

    public interface SealedSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SealedSupertype>
            get() = SealedSupertype::class
    }

    public interface SealedSupertypeInLocalClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SealedSupertypeInLocalClass>
            get() = SealedSupertypeInLocalClass::class

        public val declarationType: String
        public val sealedClassKind: ClassKind
    }

    public interface SealedInheritorInDifferentPackage : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SealedInheritorInDifferentPackage>
            get() = SealedInheritorInDifferentPackage::class
    }

    public interface SealedInheritorInDifferentModule : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SealedInheritorInDifferentModule>
            get() = SealedInheritorInDifferentModule::class
    }

    public interface ClassInheritsJavaSealedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ClassInheritsJavaSealedClass>
            get() = ClassInheritsJavaSealedClass::class
    }

    public interface UnsupportedSealedFunInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedSealedFunInterface>
            get() = UnsupportedSealedFunInterface::class
    }

    public interface SupertypeNotAClassOrInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeNotAClassOrInterface>
            get() = SupertypeNotAClassOrInterface::class

        public val reason: String
    }

    public interface UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction>
            get() = UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction::class

        public val symbol: KaSymbol
    }

    public interface CyclicInheritanceHierarchy : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CyclicInheritanceHierarchy>
            get() = CyclicInheritanceHierarchy::class
    }

    public interface ProjectionInImmediateArgumentToSupertype : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ProjectionInImmediateArgumentToSupertype>
            get() = ProjectionInImmediateArgumentToSupertype::class
    }

    public interface InconsistentTypeParameterValues : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<InconsistentTypeParameterValues>
            get() = InconsistentTypeParameterValues::class

        public val typeParameter: KaTypeParameterSymbol
        public val type: KaClassLikeSymbol
        public val bounds: List<KaType>
    }

    public interface InconsistentTypeParameterBounds : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InconsistentTypeParameterBounds>
            get() = InconsistentTypeParameterBounds::class

        public val typeParameter: KaTypeParameterSymbol
        public val type: KaClassLikeSymbol
        public val bounds: List<KaType>
    }

    public interface AmbiguousSuper : KaFirDiagnostic<KtSuperExpression> {
        override val diagnosticClass: KClass<AmbiguousSuper>
            get() = AmbiguousSuper::class

        public val candidates: List<KaType>
    }

    public interface WrongMultipleInheritance : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongMultipleInheritance>
            get() = WrongMultipleInheritance::class

        public val symbol: KaCallableSymbol
    }

    public interface ConstructorInObject : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ConstructorInObject>
            get() = ConstructorInObject::class
    }

    public interface ConstructorInInterface : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ConstructorInInterface>
            get() = ConstructorInInterface::class
    }

    public interface NonPrivateConstructorInEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonPrivateConstructorInEnum>
            get() = NonPrivateConstructorInEnum::class
    }

    public interface NonPrivateOrProtectedConstructorInSealed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonPrivateOrProtectedConstructorInSealed>
            get() = NonPrivateOrProtectedConstructorInSealed::class
    }

    public interface CyclicConstructorDelegationCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CyclicConstructorDelegationCall>
            get() = CyclicConstructorDelegationCall::class
    }

    public interface PrimaryConstructorDelegationCallExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PrimaryConstructorDelegationCallExpected>
            get() = PrimaryConstructorDelegationCallExpected::class
    }

    public interface ProtectedConstructorNotInSuperCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ProtectedConstructorNotInSuperCall>
            get() = ProtectedConstructorNotInSuperCall::class

        public val symbol: KaSymbol
    }

    public interface SupertypeNotInitialized : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeNotInitialized>
            get() = SupertypeNotInitialized::class
    }

    public interface SupertypeInitializedWithoutPrimaryConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SupertypeInitializedWithoutPrimaryConstructor>
            get() = SupertypeInitializedWithoutPrimaryConstructor::class
    }

    public interface DelegationSuperCallInEnumConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegationSuperCallInEnumConstructor>
            get() = DelegationSuperCallInEnumConstructor::class
    }

    public interface ExplicitDelegationCallRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExplicitDelegationCallRequired>
            get() = ExplicitDelegationCallRequired::class
    }

    public interface SealedClassConstructorCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SealedClassConstructorCall>
            get() = SealedClassConstructorCall::class
    }

    public interface DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations>
            get() = DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations::class
    }

    public interface DataClassConsistentCopyWrongAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DataClassConsistentCopyWrongAnnotationTarget>
            get() = DataClassConsistentCopyWrongAnnotationTarget::class
    }

    public interface DataClassCopyVisibilityWillBeChangedError : KaFirDiagnostic<KtPrimaryConstructor> {
        override val diagnosticClass: KClass<DataClassCopyVisibilityWillBeChangedError>
            get() = DataClassCopyVisibilityWillBeChangedError::class
    }

    public interface DataClassCopyVisibilityWillBeChangedWarning : KaFirDiagnostic<KtPrimaryConstructor> {
        override val diagnosticClass: KClass<DataClassCopyVisibilityWillBeChangedWarning>
            get() = DataClassCopyVisibilityWillBeChangedWarning::class
    }

    public interface DataClassInvisibleCopyUsageError : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass: KClass<DataClassInvisibleCopyUsageError>
            get() = DataClassInvisibleCopyUsageError::class
    }

    public interface DataClassInvisibleCopyUsageWarning : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass: KClass<DataClassInvisibleCopyUsageWarning>
            get() = DataClassInvisibleCopyUsageWarning::class
    }

    public interface DataClassWithoutParameters : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<DataClassWithoutParameters>
            get() = DataClassWithoutParameters::class
    }

    public interface DataClassVarargParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<DataClassVarargParameter>
            get() = DataClassVarargParameter::class
    }

    public interface DataClassNotPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<DataClassNotPropertyParameter>
            get() = DataClassNotPropertyParameter::class
    }

    public interface DataClassCopyJsExportabilityWillBeChangedError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DataClassCopyJsExportabilityWillBeChangedError>
            get() = DataClassCopyJsExportabilityWillBeChangedError::class
    }

    public interface DataClassCopyJsExportabilityWillBeChangedWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DataClassCopyJsExportabilityWillBeChangedWarning>
            get() = DataClassCopyJsExportabilityWillBeChangedWarning::class
    }

    public interface AnnotationArgumentKclassLiteralOfTypeParameterError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationArgumentKclassLiteralOfTypeParameterError>
            get() = AnnotationArgumentKclassLiteralOfTypeParameterError::class
    }

    public interface AnnotationArgumentMustBeConst : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationArgumentMustBeConst>
            get() = AnnotationArgumentMustBeConst::class
    }

    public interface AnnotationArgumentMustBeEnumConst : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationArgumentMustBeEnumConst>
            get() = AnnotationArgumentMustBeEnumConst::class
    }

    public interface AnnotationArgumentMustBeKclassLiteral : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationArgumentMustBeKclassLiteral>
            get() = AnnotationArgumentMustBeKclassLiteral::class
    }

    public interface AnnotationClassMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AnnotationClassMember>
            get() = AnnotationClassMember::class
    }

    public interface AnnotationParameterDefaultValueMustBeConstant : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationParameterDefaultValueMustBeConstant>
            get() = AnnotationParameterDefaultValueMustBeConstant::class
    }

    public interface InvalidTypeOfAnnotationMember : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InvalidTypeOfAnnotationMember>
            get() = InvalidTypeOfAnnotationMember::class
    }

    public interface ProjectionInTypeOfAnnotationMemberError : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass: KClass<ProjectionInTypeOfAnnotationMemberError>
            get() = ProjectionInTypeOfAnnotationMemberError::class
    }

    public interface ProjectionInTypeOfAnnotationMemberWarning : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass: KClass<ProjectionInTypeOfAnnotationMemberWarning>
            get() = ProjectionInTypeOfAnnotationMemberWarning::class
    }

    public interface LocalAnnotationClassError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<LocalAnnotationClassError>
            get() = LocalAnnotationClassError::class
    }

    public interface MissingValOnAnnotationParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<MissingValOnAnnotationParameter>
            get() = MissingValOnAnnotationParameter::class
    }

    public interface NonConstValUsedInConstantExpression : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NonConstValUsedInConstantExpression>
            get() = NonConstValUsedInConstantExpression::class
    }

    public interface CycleInAnnotationParameterError : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<CycleInAnnotationParameterError>
            get() = CycleInAnnotationParameterError::class
    }

    public interface AnnotationClassConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<AnnotationClassConstructorCall>
            get() = AnnotationClassConstructorCall::class
    }

    public interface EnumClassConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<EnumClassConstructorCall>
            get() = EnumClassConstructorCall::class
    }

    public interface NotAnAnnotationClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotAnAnnotationClass>
            get() = NotAnAnnotationClass::class

        public val annotationName: String
    }

    public interface NullableTypeOfAnnotationMember : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableTypeOfAnnotationMember>
            get() = NullableTypeOfAnnotationMember::class
    }

    public interface VarAnnotationParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<VarAnnotationParameter>
            get() = VarAnnotationParameter::class
    }

    public interface SupertypesForAnnotationClass : KaFirDiagnostic<KtClass> {
        override val diagnosticClass: KClass<SupertypesForAnnotationClass>
            get() = SupertypesForAnnotationClass::class
    }

    public interface AnnotationUsedAsAnnotationArgument : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationUsedAsAnnotationArgument>
            get() = AnnotationUsedAsAnnotationArgument::class
    }

    public interface AnnotationOnAnnotationArgument : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationOnAnnotationArgument>
            get() = AnnotationOnAnnotationArgument::class
    }

    public interface IllegalKotlinVersionStringValue : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<IllegalKotlinVersionStringValue>
            get() = IllegalKotlinVersionStringValue::class
    }

    public interface NewerVersionInSinceKotlin : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NewerVersionInSinceKotlin>
            get() = NewerVersionInSinceKotlin::class

        public val specifiedVersion: String
    }

    public interface DeprecatedSinceKotlinWithUnorderedVersions : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinWithUnorderedVersions>
            get() = DeprecatedSinceKotlinWithUnorderedVersions::class
    }

    public interface DeprecatedSinceKotlinWithoutArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinWithoutArguments>
            get() = DeprecatedSinceKotlinWithoutArguments::class
    }

    public interface DeprecatedSinceKotlinWithoutDeprecated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinWithoutDeprecated>
            get() = DeprecatedSinceKotlinWithoutDeprecated::class
    }

    public interface DeprecatedSinceKotlinWithDeprecatedLevel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinWithDeprecatedLevel>
            get() = DeprecatedSinceKotlinWithDeprecatedLevel::class
    }

    public interface DeprecatedSinceKotlinOutsideKotlinSubpackage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinOutsideKotlinSubpackage>
            get() = DeprecatedSinceKotlinOutsideKotlinSubpackage::class
    }

    public interface KotlinActualAnnotationHasNoEffectInKotlin : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<KotlinActualAnnotationHasNoEffectInKotlin>
            get() = KotlinActualAnnotationHasNoEffectInKotlin::class
    }

    public interface DeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecationError>
            get() = DeprecationError::class

        public val reference: KaSymbol
        public val message: String
    }

    public interface Deprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<Deprecation>
            get() = Deprecation::class

        public val reference: KaSymbol
        public val message: String
    }

    public interface DeprecationErrorMigrationPeriodWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecationErrorMigrationPeriodWarning>
            get() = DeprecationErrorMigrationPeriodWarning::class

        public val reference: KaSymbol
        public val message: String
        public val migrationLanguageFeature: LanguageFeature
    }

    public interface OverrideDeprecation : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<OverrideDeprecation>
            get() = OverrideDeprecation::class

        public val overridenSymbol: KaSymbol
        public val deprecationInfo: FirDeprecationInfo
    }

    public interface ExtendingAnAnnotationClassError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExtendingAnAnnotationClassError>
            get() = ExtendingAnAnnotationClassError::class

        public val annotationSymbol: KaClassLikeSymbol
    }

    public interface ExtendingAnAnnotationClassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExtendingAnAnnotationClassWarning>
            get() = ExtendingAnAnnotationClassWarning::class

        public val annotationSymbol: KaClassLikeSymbol
    }

    public interface TypealiasExpansionDeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypealiasExpansionDeprecationError>
            get() = TypealiasExpansionDeprecationError::class

        public val alias: KaSymbol
        public val reference: KaSymbol
        public val message: String
    }

    public interface TypealiasExpansionDeprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypealiasExpansionDeprecation>
            get() = TypealiasExpansionDeprecation::class

        public val alias: KaSymbol
        public val reference: KaSymbol
        public val message: String
    }

    public interface VersionRequirementDeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VersionRequirementDeprecationError>
            get() = VersionRequirementDeprecationError::class

        public val reference: KaSymbol
        public val version: Version
        public val currentVersion: String
        public val message: String
    }

    public interface VersionRequirementDeprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VersionRequirementDeprecation>
            get() = VersionRequirementDeprecation::class

        public val reference: KaSymbol
        public val version: Version
        public val currentVersion: String
        public val message: String
    }

    public interface RedundantAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RedundantAnnotation>
            get() = RedundantAnnotation::class

        public val annotation: ClassId
    }

    public interface AnnotationOnSuperclassError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationOnSuperclassError>
            get() = AnnotationOnSuperclassError::class
    }

    public interface RestrictedRetentionForExpressionAnnotationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RestrictedRetentionForExpressionAnnotationError>
            get() = RestrictedRetentionForExpressionAnnotationError::class
    }

    public interface WrongAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<WrongAnnotationTarget>
            get() = WrongAnnotationTarget::class

        public val actualTarget: String
        public val allowedTargets: List<KotlinTarget>
    }

    public interface WrongAnnotationTargetWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<WrongAnnotationTargetWarning>
            get() = WrongAnnotationTargetWarning::class

        public val actualTarget: String
        public val allowedTargets: List<KotlinTarget>
    }

    public interface WrongAnnotationTargetWithUseSiteTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<WrongAnnotationTargetWithUseSiteTarget>
            get() = WrongAnnotationTargetWithUseSiteTarget::class

        public val actualTarget: String
        public val useSiteTarget: String
        public val allowedTargets: List<KotlinTarget>
    }

    public interface AnnotationWithUseSiteTargetOnExpressionError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationWithUseSiteTargetOnExpressionError>
            get() = AnnotationWithUseSiteTargetOnExpressionError::class
    }

    public interface AnnotationWithUseSiteTargetOnExpressionWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationWithUseSiteTargetOnExpressionWarning>
            get() = AnnotationWithUseSiteTargetOnExpressionWarning::class
    }

    public interface InapplicableTargetOnProperty : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetOnProperty>
            get() = InapplicableTargetOnProperty::class

        public val useSiteDescription: String
    }

    public interface InapplicableTargetOnPropertyWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetOnPropertyWarning>
            get() = InapplicableTargetOnPropertyWarning::class

        public val useSiteDescription: String
    }

    public interface InapplicableTargetPropertyImmutable : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetPropertyImmutable>
            get() = InapplicableTargetPropertyImmutable::class

        public val useSiteDescription: String
    }

    public interface InapplicableTargetPropertyHasNoDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetPropertyHasNoDelegate>
            get() = InapplicableTargetPropertyHasNoDelegate::class
    }

    public interface InapplicableTargetPropertyHasNoBackingField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetPropertyHasNoBackingField>
            get() = InapplicableTargetPropertyHasNoBackingField::class
    }

    public interface InapplicableParamTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableParamTarget>
            get() = InapplicableParamTarget::class
    }

    public interface InapplicableFileTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableFileTarget>
            get() = InapplicableFileTarget::class
    }

    public interface InapplicableAllTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableAllTarget>
            get() = InapplicableAllTarget::class

        public val inapplicableTargetDescription: String
    }

    public interface InapplicableAllTargetInMultiAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableAllTargetInMultiAnnotation>
            get() = InapplicableAllTargetInMultiAnnotation::class
    }

    public interface RepeatedAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RepeatedAnnotation>
            get() = RepeatedAnnotation::class
    }

    public interface RepeatedAnnotationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RepeatedAnnotationWarning>
            get() = RepeatedAnnotationWarning::class
    }

    public interface RedundantAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RedundantAnnotationTarget>
            get() = RedundantAnnotationTarget::class

        public val useSiteDescription: String
    }

    public interface NotAClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotAClass>
            get() = NotAClass::class
    }

    public interface WrongExtensionFunctionType : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<WrongExtensionFunctionType>
            get() = WrongExtensionFunctionType::class
    }

    public interface AnnotationInWhereClauseError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationInWhereClauseError>
            get() = AnnotationInWhereClauseError::class
    }

    public interface AnnotationInContractError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<AnnotationInContractError>
            get() = AnnotationInContractError::class
    }

    public interface AmbiguousAnnotationArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AmbiguousAnnotationArgument>
            get() = AmbiguousAnnotationArgument::class

        public val symbols: List<KaSymbol>
    }

    public interface VolatileOnValue : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<VolatileOnValue>
            get() = VolatileOnValue::class
    }

    public interface VolatileOnDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<VolatileOnDelegate>
            get() = VolatileOnDelegate::class
    }

    public interface NonInternalPublishedApi : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonInternalPublishedApi>
            get() = NonInternalPublishedApi::class
    }

    public interface NonSourceAnnotationOnInlinedLambdaExpression : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<NonSourceAnnotationOnInlinedLambdaExpression>
            get() = NonSourceAnnotationOnInlinedLambdaExpression::class
    }

    public interface PotentiallyNonReportedAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<PotentiallyNonReportedAnnotation>
            get() = PotentiallyNonReportedAnnotation::class
    }

    public interface AnnotationWillBeAppliedAlsoToPropertyOrField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationWillBeAppliedAlsoToPropertyOrField>
            get() = AnnotationWillBeAppliedAlsoToPropertyOrField::class

        public val useSiteDescription: String
    }

    public interface AnnotationsOnBlockLevelExpressionOnTheSameLine : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AnnotationsOnBlockLevelExpressionOnTheSameLine>
            get() = AnnotationsOnBlockLevelExpressionOnTheSameLine::class
    }

    public interface IgnorabilityAnnotationsWithCheckerDisabled : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<IgnorabilityAnnotationsWithCheckerDisabled>
            get() = IgnorabilityAnnotationsWithCheckerDisabled::class
    }

    public interface DslMarkerPropagatesToMany : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DslMarkerPropagatesToMany>
            get() = DslMarkerPropagatesToMany::class
    }

    public interface DslMarkerAppliedToWrongTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DslMarkerAppliedToWrongTarget>
            get() = DslMarkerAppliedToWrongTarget::class

        public val dslMarkerSymbol: KaClassLikeSymbol
        public val actualTarget: String
    }

    public interface JsModuleProhibitedOnNonNative : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsModuleProhibitedOnNonNative>
            get() = JsModuleProhibitedOnNonNative::class
    }

    public interface CallFromUmdMustBeJsModuleAndJsNonModule : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallFromUmdMustBeJsModuleAndJsNonModule>
            get() = CallFromUmdMustBeJsModuleAndJsNonModule::class
    }

    public interface CallToJsModuleWithoutModuleSystem : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallToJsModuleWithoutModuleSystem>
            get() = CallToJsModuleWithoutModuleSystem::class

        public val callee: KaSymbol
    }

    public interface CallToJsNonModuleWithModuleSystem : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallToJsNonModuleWithModuleSystem>
            get() = CallToJsNonModuleWithModuleSystem::class

        public val callee: KaSymbol
    }

    public interface RuntimeAnnotationOnExternalDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RuntimeAnnotationOnExternalDeclaration>
            get() = RuntimeAnnotationOnExternalDeclaration::class
    }

    public interface NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun>
            get() = NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun::class

        public val type: KaType
    }

    public interface NativeIndexerKeyShouldBeStringOrNumber : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NativeIndexerKeyShouldBeStringOrNumber>
            get() = NativeIndexerKeyShouldBeStringOrNumber::class

        public val kind: String
    }

    public interface NativeIndexerWrongParameterCount : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NativeIndexerWrongParameterCount>
            get() = NativeIndexerWrongParameterCount::class

        public val parametersCount: Int
        public val kind: String
    }

    public interface NativeIndexerCanNotHaveDefaultArguments : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NativeIndexerCanNotHaveDefaultArguments>
            get() = NativeIndexerCanNotHaveDefaultArguments::class

        public val kind: String
    }

    public interface NativeGetterReturnTypeShouldBeNullable : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NativeGetterReturnTypeShouldBeNullable>
            get() = NativeGetterReturnTypeShouldBeNullable::class
    }

    public interface NativeSetterWrongReturnType : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NativeSetterWrongReturnType>
            get() = NativeSetterWrongReturnType::class
    }

    public interface JsNameIsNotOnAllAccessors : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameIsNotOnAllAccessors>
            get() = JsNameIsNotOnAllAccessors::class
    }

    public interface JsNameProhibitedForNamedNative : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameProhibitedForNamedNative>
            get() = JsNameProhibitedForNamedNative::class
    }

    public interface JsNameProhibitedForOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameProhibitedForOverride>
            get() = JsNameProhibitedForOverride::class
    }

    public interface JsNameOnPrimaryConstructorProhibited : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameOnPrimaryConstructorProhibited>
            get() = JsNameOnPrimaryConstructorProhibited::class
    }

    public interface JsNameOnAccessorAndProperty : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameOnAccessorAndProperty>
            get() = JsNameOnAccessorAndProperty::class
    }

    public interface JsNameProhibitedForExtensionProperty : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameProhibitedForExtensionProperty>
            get() = JsNameProhibitedForExtensionProperty::class
    }

    public interface JsBuiltinNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsBuiltinNameClash>
            get() = JsBuiltinNameClash::class

        public val name: String
    }

    public interface NameContainsIllegalChars : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NameContainsIllegalChars>
            get() = NameContainsIllegalChars::class
    }

    public interface JsNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameClash>
            get() = JsNameClash::class

        public val name: String
        public val existing: List<KaSymbol>
    }

    public interface JsFakeNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsFakeNameClash>
            get() = JsFakeNameClash::class

        public val name: String
        public val override: KaSymbol
        public val existing: List<KaSymbol>
    }

    public interface JsSymbolOnTopLevelDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsSymbolOnTopLevelDeclaration>
            get() = JsSymbolOnTopLevelDeclaration::class
    }

    public interface JsSymbolProhibitedForOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsSymbolProhibitedForOverride>
            get() = JsSymbolProhibitedForOverride::class
    }

    public interface WrongJsQualifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongJsQualifier>
            get() = WrongJsQualifier::class
    }

    public interface JsModuleProhibitedOnVar : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsModuleProhibitedOnVar>
            get() = JsModuleProhibitedOnVar::class
    }

    public interface NestedJsModuleProhibited : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NestedJsModuleProhibited>
            get() = NestedJsModuleProhibited::class
    }

    public interface UnresolvedEqualityBoundArgument : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UnresolvedEqualityBoundArgument>
            get() = UnresolvedEqualityBoundArgument::class
    }

    public interface AmbiguouslyResolvedEqualityBoundArgument : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AmbiguouslyResolvedEqualityBoundArgument>
            get() = AmbiguouslyResolvedEqualityBoundArgument::class

        public val candidates: List<KaType>
    }

    public interface EqualityBoundArgumentExpandsToNonStarProjected : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<EqualityBoundArgumentExpandsToNonStarProjected>
            get() = EqualityBoundArgumentExpandsToNonStarProjected::class

        public val expandedType: KaType
    }

    public interface EqualityBoundMismatchOnInheritance : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<EqualityBoundMismatchOnInheritance>
            get() = EqualityBoundMismatchOnInheritance::class

        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    public interface EqualityBoundMismatchByDelegation : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<EqualityBoundMismatchByDelegation>
            get() = EqualityBoundMismatchByDelegation::class

        public val delegateDeclaration: KaCallableSymbol
        public val baseDeclaration: KaCallableSymbol
    }

    public interface InheritedIntersectionEqualityBound : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InheritedIntersectionEqualityBound>
            get() = InheritedIntersectionEqualityBound::class

        public val declaration: KaCallableSymbol
        public val candidates: KaType
    }

    public interface EqualityBoundNotSupertypeOfContainingClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<EqualityBoundNotSupertypeOfContainingClass>
            get() = EqualityBoundNotSupertypeOfContainingClass::class

        public val equalityBoundType: KaType
        public val receiverType: KaType
    }

    public interface EqualityNotApplicableByEqualityBounds : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<EqualityNotApplicableByEqualityBounds>
            get() = EqualityNotApplicableByEqualityBounds::class

        public val leftType: KaType
        public val rightType: KaType
        public val leftIsEqualityBound: String
        public val rightIsEqualityBound: String
    }

    public interface EqualitySuspiciousByEqualityBounds : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<EqualitySuspiciousByEqualityBounds>
            get() = EqualitySuspiciousByEqualityBounds::class

        public val leftType: KaType
        public val rightType: KaType
        public val leftEqualityBound: KaType
        public val rightEqualityBound: KaType
    }

    public interface OptInUsage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInUsage>
            get() = OptInUsage::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    public interface OptInUsageError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInUsageError>
            get() = OptInUsageError::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    public interface OptInToInheritance : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInToInheritance>
            get() = OptInToInheritance::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    public interface OptInToInheritanceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInToInheritanceError>
            get() = OptInToInheritanceError::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    public interface OptInOverride : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInOverride>
            get() = OptInOverride::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    public interface OptInOverrideError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInOverrideError>
            get() = OptInOverrideError::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    public interface OptInCanOnlyBeUsedAsAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInCanOnlyBeUsedAsAnnotation>
            get() = OptInCanOnlyBeUsedAsAnnotation::class
    }

    public interface OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn>
            get() = OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn::class
    }

    public interface OptInWithoutArguments : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInWithoutArguments>
            get() = OptInWithoutArguments::class
    }

    public interface OptInArgumentIsNotMarker : KaFirDiagnostic<KtClassLiteralExpression> {
        override val diagnosticClass: KClass<OptInArgumentIsNotMarker>
            get() = OptInArgumentIsNotMarker::class

        public val notMarkerClassId: ClassId
    }

    public interface OptInMarkerWithWrongTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerWithWrongTarget>
            get() = OptInMarkerWithWrongTarget::class

        public val target: String
    }

    public interface OptInMarkerWithWrongRetention : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerWithWrongRetention>
            get() = OptInMarkerWithWrongRetention::class
    }

    public interface OptInMarkerOnWrongTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerOnWrongTarget>
            get() = OptInMarkerOnWrongTarget::class

        public val target: String
    }

    public interface OptInMarkerOnOverride : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerOnOverride>
            get() = OptInMarkerOnOverride::class
    }

    public interface OptInMarkerOnOverrideWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerOnOverrideWarning>
            get() = OptInMarkerOnOverrideWarning::class
    }

    public interface SubclassOptInInapplicable : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SubclassOptInInapplicable>
            get() = SubclassOptInInapplicable::class

        public val target: String
    }

    public interface SubclassOptInArgumentIsNotMarker : KaFirDiagnostic<KtClassLiteralExpression> {
        override val diagnosticClass: KClass<SubclassOptInArgumentIsNotMarker>
            get() = SubclassOptInArgumentIsNotMarker::class

        public val notMarkerClassId: ClassId
    }

    public interface ExposedTypealiasExpandedType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExposedTypealiasExpandedType>
            get() = ExposedTypealiasExpandedType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedFunctionReturnType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExposedFunctionReturnType>
            get() = ExposedFunctionReturnType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedReceiverType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedReceiverType>
            get() = ExposedReceiverType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedPropertyType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExposedPropertyType>
            get() = ExposedPropertyType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedPropertyTypeInConstructorError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExposedPropertyTypeInConstructorError>
            get() = ExposedPropertyTypeInConstructorError::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedParameterType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ExposedParameterType>
            get() = ExposedParameterType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedSuperInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedSuperInterface>
            get() = ExposedSuperInterface::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedSuperClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedSuperClass>
            get() = ExposedSuperClass::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedTypeParameterBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedTypeParameterBound>
            get() = ExposedTypeParameterBound::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface ExposedTypeParameterBoundDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedTypeParameterBoundDeprecationWarning>
            get() = ExposedTypeParameterBoundDeprecationWarning::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    public interface RepeatedModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RepeatedModifier>
            get() = RepeatedModifier::class

        public val modifier: KtModifierKeywordToken
    }

    public interface WrongModifierTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongModifierTarget>
            get() = WrongModifierTarget::class

        public val modifier: KtModifierKeywordToken
        public val target: String
    }

    public interface WrongModifierContainingDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongModifierContainingDeclaration>
            get() = WrongModifierContainingDeclaration::class

        public val modifier: KtModifierKeywordToken
        public val target: String
    }

    public interface DeprecatedModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedModifier>
            get() = DeprecatedModifier::class

        public val deprecatedModifier: KtModifierKeywordToken
        public val actualModifier: KtModifierKeywordToken
    }

    public interface DeprecatedModifierForTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedModifierForTarget>
            get() = DeprecatedModifierForTarget::class

        public val deprecatedModifier: KtModifierKeywordToken
        public val target: String
    }

    public interface DeprecatedModifierContainingDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedModifierContainingDeclaration>
            get() = DeprecatedModifierContainingDeclaration::class

        public val modifier: KtModifierKeywordToken
        public val target: String
    }

    public interface IncompatibleModifiers : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IncompatibleModifiers>
            get() = IncompatibleModifiers::class

        public val modifier1: KtModifierKeywordToken
        public val modifier2: KtModifierKeywordToken
    }

    public interface DeprecatedModifierPair : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedModifierPair>
            get() = DeprecatedModifierPair::class

        public val deprecatedModifier: KtModifierKeywordToken
        public val conflictingModifier: KtModifierKeywordToken
    }

    public interface RedundantModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantModifier>
            get() = RedundantModifier::class

        public val redundantModifier: KtModifierKeywordToken
        public val conflictingModifier: KtModifierKeywordToken
    }

    public interface RedundantModifierForTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantModifierForTarget>
            get() = RedundantModifierForTarget::class

        public val redundantModifier: KtModifierKeywordToken
        public val target: String
    }

    public interface InfixModifierRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InfixModifierRequired>
            get() = InfixModifierRequired::class

        public val functionSymbol: KaFunctionSymbol
    }

    public interface OperatorModifierRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OperatorModifierRequired>
            get() = OperatorModifierRequired::class

        public val functionSymbol: KaFunctionSymbol
    }

    public interface InapplicableInfixModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableInfixModifier>
            get() = InapplicableInfixModifier::class
    }

    public interface InapplicableOperatorModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableOperatorModifier>
            get() = InapplicableOperatorModifier::class

        public val message: String
    }

    public interface InapplicableOperatorModifierWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableOperatorModifierWarning>
            get() = InapplicableOperatorModifierWarning::class

        public val message: String
        public val deprecatingFeature: LanguageFeature
    }

    public interface InapplicableLateinitModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<InapplicableLateinitModifier>
            get() = InapplicableLateinitModifier::class

        public val reason: String
    }

    public interface PotentiallyNullableReturnTypeOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<PotentiallyNullableReturnTypeOfOperatorOf>
            get() = PotentiallyNullableReturnTypeOfOperatorOf::class
    }

    public interface NullableReturnTypeOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<NullableReturnTypeOfOperatorOf>
            get() = NullableReturnTypeOfOperatorOf::class
    }

    public interface ReturnTypeMismatchOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<ReturnTypeMismatchOfOperatorOf>
            get() = ReturnTypeMismatchOfOperatorOf::class

        public val outerClass: KaClassLikeSymbol
    }

    public interface NoVarargOverloadOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<NoVarargOverloadOfOperatorOf>
            get() = NoVarargOverloadOfOperatorOf::class
    }

    public interface MultipleVarargOverloadsOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<MultipleVarargOverloadsOfOperatorOf>
            get() = MultipleVarargOverloadsOfOperatorOf::class
    }

    public interface InconsistentReturnTypesInOfOverloads : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InconsistentReturnTypesInOfOverloads>
            get() = InconsistentReturnTypesInOfOverloads::class

        public val mainOverloadType: KaType
    }

    public interface InconsistentParameterTypesInOfOverloads : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InconsistentParameterTypesInOfOverloads>
            get() = InconsistentParameterTypesInOfOverloads::class

        public val mainParameterType: KaType
    }

    public interface InconsistentVisibilityInOfOverloads : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InconsistentVisibilityInOfOverloads>
            get() = InconsistentVisibilityInOfOverloads::class

        public val mainVisibility: Visibility
    }

    public interface InconsistentSuspendInOfOverloads : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InconsistentSuspendInOfOverloads>
            get() = InconsistentSuspendInOfOverloads::class

        public val overloadSuspendability: String
        public val mainOverloadSuspendability: String
    }

    public interface OfOverloadsInBlockAndObject : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<OfOverloadsInBlockAndObject>
            get() = OfOverloadsInBlockAndObject::class

        public val overloadOrigin: String
        public val mainOrigin: String
    }

    public interface InconsistentTypeParametersInOfOverloads : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InconsistentTypeParametersInOfOverloads>
            get() = InconsistentTypeParametersInOfOverloads::class

        public val mainOverload: KaFunctionSymbol
    }

    public interface RedundantOpenInInterface : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<RedundantOpenInInterface>
            get() = RedundantOpenInInterface::class
    }

    public interface OperatorCallOnConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OperatorCallOnConstructor>
            get() = OperatorCallOnConstructor::class

        public val name: String
    }

    public interface NoExplicitVisibilityInApiMode : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NoExplicitVisibilityInApiMode>
            get() = NoExplicitVisibilityInApiMode::class
    }

    public interface NoExplicitVisibilityInApiModeWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NoExplicitVisibilityInApiModeWarning>
            get() = NoExplicitVisibilityInApiModeWarning::class
    }

    public interface NoExplicitReturnTypeInApiMode : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NoExplicitReturnTypeInApiMode>
            get() = NoExplicitReturnTypeInApiMode::class
    }

    public interface NoExplicitReturnTypeInApiModeWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NoExplicitReturnTypeInApiModeWarning>
            get() = NoExplicitReturnTypeInApiModeWarning::class
    }

    public interface AnonymousSuspendFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<AnonymousSuspendFunction>
            get() = AnonymousSuspendFunction::class
    }

    public interface ValueClassNotTopLevel : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ValueClassNotTopLevel>
            get() = ValueClassNotTopLevel::class
    }

    public interface ValueClassNotFinal : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ValueClassNotFinal>
            get() = ValueClassNotFinal::class

        public val prefix: String
    }

    public interface ValueClassOpen : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ValueClassOpen>
            get() = ValueClassOpen::class
    }

    public interface AbsenceOfPrimaryConstructorForValueClass : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<AbsenceOfPrimaryConstructorForValueClass>
            get() = AbsenceOfPrimaryConstructorForValueClass::class

        public val modifier: String
    }

    public interface ExpectValueClassWithNoPrimaryConstructorHasSecondary : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExpectValueClassWithNoPrimaryConstructorHasSecondary>
            get() = ExpectValueClassWithNoPrimaryConstructorHasSecondary::class

        public val modifier: String
    }

    public interface InlineClassConstructorWrongParametersSize : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InlineClassConstructorWrongParametersSize>
            get() = InlineClassConstructorWrongParametersSize::class

        public val prefix: String
    }

    public interface ValueClassEmptyConstructor : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassEmptyConstructor>
            get() = ValueClassEmptyConstructor::class

        public val prefix: String
    }

    public interface ValueClassConstructorNotFinalReadOnlyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValueClassConstructorNotFinalReadOnlyParameter>
            get() = ValueClassConstructorNotFinalReadOnlyParameter::class

        public val prefix: String
    }

    public interface AbstractValueClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<AbstractValueClassConstructorPropertyParameter>
            get() = AbstractValueClassConstructorPropertyParameter::class
    }

    public interface SealedValueClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<SealedValueClassConstructorPropertyParameter>
            get() = SealedValueClassConstructorPropertyParameter::class
    }

    public interface PropertyWithBackingFieldInsideValueClass : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<PropertyWithBackingFieldInsideValueClass>
            get() = PropertyWithBackingFieldInsideValueClass::class
    }

    public interface DelegatedPropertyInsideValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegatedPropertyInsideValueClass>
            get() = DelegatedPropertyInsideValueClass::class
    }

    public interface ValueClassHasInapplicableParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassHasInapplicableParameterType>
            get() = ValueClassHasInapplicableParameterType::class

        public val type: KaType
        public val prefix: String
    }

    public interface ValueClassCannotImplementInterfaceByDelegation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ValueClassCannotImplementInterfaceByDelegation>
            get() = ValueClassCannotImplementInterfaceByDelegation::class
    }

    public interface ValueClassCannotExtendClasses : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotExtendClasses>
            get() = ValueClassCannotExtendClasses::class

        public val prefix: String
    }

    public interface ValueClassCannotExtendIdentityClasses : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotExtendIdentityClasses>
            get() = ValueClassCannotExtendIdentityClasses::class
    }

    public interface ValueClassCannotBeRecursive : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotBeRecursive>
            get() = ValueClassCannotBeRecursive::class
    }

    public interface ValueClassCannotBeRecursiveViaTypeParametersError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotBeRecursiveViaTypeParametersError>
            get() = ValueClassCannotBeRecursiveViaTypeParametersError::class
    }

    public interface ValueClassCannotBeRecursiveViaTypeParametersWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotBeRecursiveViaTypeParametersWarning>
            get() = ValueClassCannotBeRecursiveViaTypeParametersWarning::class
    }

    public interface SecondaryConstructorWithBodyInsideValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SecondaryConstructorWithBodyInsideValueClass>
            get() = SecondaryConstructorWithBodyInsideValueClass::class
    }

    public interface ReservedMemberInsideValueClass : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<ReservedMemberInsideValueClass>
            get() = ReservedMemberInsideValueClass::class

        public val name: String
    }

    public interface ReservedMemberFromInterfaceInsideValueClass : KaFirDiagnostic<KtClass> {
        override val diagnosticClass: KClass<ReservedMemberFromInterfaceInsideValueClass>
            get() = ReservedMemberFromInterfaceInsideValueClass::class

        public val interfaceName: String
        public val methodName: String
    }

    public interface TypeArgumentOnTypedValueClassEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypeArgumentOnTypedValueClassEquals>
            get() = TypeArgumentOnTypedValueClassEquals::class
    }

    public interface InnerClassInsideValueClass : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InnerClassInsideValueClass>
            get() = InnerClassInsideValueClass::class

        public val prefix: String
    }

    public interface ValueClassCannotBeCloneable : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ValueClassCannotBeCloneable>
            get() = ValueClassCannotBeCloneable::class
    }

    public interface NoneApplicable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoneApplicable>
            get() = NoneApplicable::class

        public val candidates: List<Pair<KaSymbol, List<String>>>
    }

    public interface InapplicableCandidate : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableCandidate>
            get() = InapplicableCandidate::class

        public val candidate: KaSymbol
    }

    public interface HasNextFunctionNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<HasNextFunctionNoneApplicable>
            get() = HasNextFunctionNoneApplicable::class

        public val candidates: List<KaSymbol>
    }

    public interface NextNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NextNoneApplicable>
            get() = NextNoneApplicable::class

        public val candidates: List<KaSymbol>
    }

    public interface DelegateSpecialFunctionNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegateSpecialFunctionNoneApplicable>
            get() = DelegateSpecialFunctionNoneApplicable::class

        public val expectedFunctionSignature: String
        public val candidates: List<KaSymbol>
    }

    public interface TypeInferenceOnlyInputTypesError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeInferenceOnlyInputTypesError>
            get() = TypeInferenceOnlyInputTypesError::class

        public val typeParameter: KaTypeParameterSymbol
    }

    public interface MemberProjectedOut : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MemberProjectedOut>
            get() = MemberProjectedOut::class

        public val receiver: KaType
        public val projection: String
        public val symbol: KaCallableSymbol
    }

    public interface NoValueForParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NoValueForParameter>
            get() = NoValueForParameter::class

        public val violatedParameter: Name
    }

    public interface TooManyArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TooManyArguments>
            get() = TooManyArguments::class

        public val function: KaCallableSymbol
    }

    public interface NamedParameterNotFound : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass: KClass<NamedParameterNotFound>
            get() = NamedParameterNotFound::class

        public val name: String
    }

    public interface NameForAmbiguousParameter : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass: KClass<NameForAmbiguousParameter>
            get() = NameForAmbiguousParameter::class
    }

    public interface ArgumentPassedTwice : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass: KClass<ArgumentPassedTwice>
            get() = ArgumentPassedTwice::class
    }

    public interface NamedArgumentsNotAllowed : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass: KClass<NamedArgumentsNotAllowed>
            get() = NamedArgumentsNotAllowed::class

        public val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget
    }

    public interface MixingNamedAndPositionalArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MixingNamedAndPositionalArguments>
            get() = MixingNamedAndPositionalArguments::class
    }

    public interface VarargOutsideParentheses : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<VarargOutsideParentheses>
            get() = VarargOutsideParentheses::class
    }

    public interface NonVarargSpread : KaFirDiagnostic<LeafPsiElement> {
        override val diagnosticClass: KClass<NonVarargSpread>
            get() = NonVarargSpread::class
    }

    public interface SpreadOfNullable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SpreadOfNullable>
            get() = SpreadOfNullable::class
    }

    public interface UnexpectedTrailingLambdaOnANewLine : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnexpectedTrailingLambdaOnANewLine>
            get() = UnexpectedTrailingLambdaOnANewLine::class
    }

    public interface ManyLambdaExpressionArguments : KaFirDiagnostic<KtLambdaExpression> {
        override val diagnosticClass: KClass<ManyLambdaExpressionArguments>
            get() = ManyLambdaExpressionArguments::class
    }

    public interface AssigningSingleElementToVarargInNamedFormFunctionError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssigningSingleElementToVarargInNamedFormFunctionError>
            get() = AssigningSingleElementToVarargInNamedFormFunctionError::class

        public val expectedArrayType: KaType
    }

    public interface AssigningSingleElementToVarargInNamedFormFunctionWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssigningSingleElementToVarargInNamedFormFunctionWarning>
            get() = AssigningSingleElementToVarargInNamedFormFunctionWarning::class

        public val expectedArrayType: KaType
    }

    public interface AssigningSingleElementToVarargInNamedFormAnnotationError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssigningSingleElementToVarargInNamedFormAnnotationError>
            get() = AssigningSingleElementToVarargInNamedFormAnnotationError::class
    }

    public interface AssigningSingleElementToVarargInNamedFormAnnotationWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssigningSingleElementToVarargInNamedFormAnnotationWarning>
            get() = AssigningSingleElementToVarargInNamedFormAnnotationWarning::class
    }

    public interface RedundantSpreadOperatorInNamedFormInFunction : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RedundantSpreadOperatorInNamedFormInFunction>
            get() = RedundantSpreadOperatorInNamedFormInFunction::class
    }

    public interface RedundantSpreadOperatorInNamedFormInAnnotation : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RedundantSpreadOperatorInNamedFormInAnnotation>
            get() = RedundantSpreadOperatorInNamedFormInAnnotation::class
    }

    public interface IllegalTypeArgumentForVarargParameterWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IllegalTypeArgumentForVarargParameterWarning>
            get() = IllegalTypeArgumentForVarargParameterWarning::class

        public val type: KaType
    }

    public interface NestedClassAccessedViaInstanceReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NestedClassAccessedViaInstanceReference>
            get() = NestedClassAccessedViaInstanceReference::class

        public val symbol: KaClassLikeSymbol
    }

    public interface TypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeMismatch>
            get() = TypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    public interface ArgumentTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ArgumentTypeMismatch>
            get() = ArgumentTypeMismatch::class

        public val actualType: KaType
        public val expectedType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    public interface ReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ReturnTypeMismatch>
            get() = ReturnTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val targetFunction: KaSymbol
        public val isMismatchDueToNullability: Boolean
    }

    public interface ExpectedParameterTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpectedParameterTypeMismatch>
            get() = ExpectedParameterTypeMismatch::class

        public val actualType: KaType
        public val expectedType: KaType
    }

    public interface InitializerTypeMismatch : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<InitializerTypeMismatch>
            get() = InitializerTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    public interface FieldInitializerTypeMismatch : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<FieldInitializerTypeMismatch>
            get() = FieldInitializerTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    public interface AssignmentTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssignmentTypeMismatch>
            get() = AssignmentTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    public interface ConditionTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConditionTypeMismatch>
            get() = ConditionTypeMismatch::class

        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    public interface ThrowableTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ThrowableTypeMismatch>
            get() = ThrowableTypeMismatch::class

        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    public interface ResultTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ResultTypeMismatch>
            get() = ResultTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
    }

    public interface CompareToTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CompareToTypeMismatch>
            get() = CompareToTypeMismatch::class

        public val actualType: KaType
    }

    public interface HasNextFunctionTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<HasNextFunctionTypeMismatch>
            get() = HasNextFunctionTypeMismatch::class

        public val actualType: KaType
    }

    public interface ComponentFunctionReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ComponentFunctionReturnTypeMismatch>
            get() = ComponentFunctionReturnTypeMismatch::class

        public val componentFunctionName: Name
        public val destructingType: KaType
        public val expectedType: KaType
    }

    public interface DelegateSpecialFunctionReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegateSpecialFunctionReturnTypeMismatch>
            get() = DelegateSpecialFunctionReturnTypeMismatch::class

        public val delegateFunction: String
        public val expectedType: KaType
        public val actualType: KaType
    }

    public interface OverloadResolutionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OverloadResolutionAmbiguity>
            get() = OverloadResolutionAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    public interface AssignOperatorAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AssignOperatorAmbiguity>
            get() = AssignOperatorAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    public interface IteratorAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IteratorAmbiguity>
            get() = IteratorAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    public interface HasNextFunctionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<HasNextFunctionAmbiguity>
            get() = HasNextFunctionAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    public interface NextAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NextAmbiguity>
            get() = NextAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    public interface ComponentFunctionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ComponentFunctionAmbiguity>
            get() = ComponentFunctionAmbiguity::class

        public val functionWithAmbiguityName: Name
        public val candidates: List<KaSymbol>
        public val destructingType: KaType
    }

    public interface DelegateSpecialFunctionAmbiguity : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegateSpecialFunctionAmbiguity>
            get() = DelegateSpecialFunctionAmbiguity::class

        public val expectedFunctionSignature: String
        public val candidates: List<KaSymbol>
    }

    public interface CompilerRequiredAnnotationAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompilerRequiredAnnotationAmbiguity>
            get() = CompilerRequiredAnnotationAmbiguity::class

        public val typeFromCompilerPhase: KaType
        public val typeFromTypesPhase: KaType
    }

    public interface AmbiguousFunctionTypeKind : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AmbiguousFunctionTypeKind>
            get() = AmbiguousFunctionTypeKind::class

        public val kinds: List<FunctionTypeKind>
    }

    public interface ContextSensitiveResolutionAmbiguity : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContextSensitiveResolutionAmbiguity>
            get() = ContextSensitiveResolutionAmbiguity::class

        public val resolvedCandidate: KaSymbol
        public val contextSensitiveCandidates: List<KaSymbol>
    }

    public interface NoContextArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NoContextArgument>
            get() = NoContextArgument::class

        public val symbol: KaSymbol
    }

    public interface AmbiguousContextArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<AmbiguousContextArgument>
            get() = AmbiguousContextArgument::class

        public val symbol: KaSymbol
    }

    public interface ContextualOverloadShadowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContextualOverloadShadowed>
            get() = ContextualOverloadShadowed::class

        public val symbols: List<KaSymbol>
    }

    public interface MultipleContextLists : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleContextLists>
            get() = MultipleContextLists::class
    }

    public interface ContextParameterWithoutName : KaFirDiagnostic<KtContextReceiver> {
        override val diagnosticClass: KClass<ContextParameterWithoutName>
            get() = ContextParameterWithoutName::class
    }

    public interface ContextParametersWithBackingField : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContextParametersWithBackingField>
            get() = ContextParametersWithBackingField::class
    }

    public interface CallableReferenceToContextualDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallableReferenceToContextualDeclaration>
            get() = CallableReferenceToContextualDeclaration::class

        public val symbol: KaCallableSymbol
    }

    public interface NamedContextParameterInFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NamedContextParameterInFunctionType>
            get() = NamedContextParameterInFunctionType::class
    }

    public interface ContextParameterWithDefault : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContextParameterWithDefault>
            get() = ContextParameterWithDefault::class
    }

    public interface UnsupportedContextualDeclarationCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnsupportedContextualDeclarationCall>
            get() = UnsupportedContextualDeclarationCall::class
    }

    public interface AmbiguousCallWithImplicitContextReceiver : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<AmbiguousCallWithImplicitContextReceiver>
            get() = AmbiguousCallWithImplicitContextReceiver::class
    }

    public interface CoroutineContextAsContextParameterIsReserved : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CoroutineContextAsContextParameterIsReserved>
            get() = CoroutineContextAsContextParameterIsReserved::class
    }

    public interface RecursionInImplicitTypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RecursionInImplicitTypes>
            get() = RecursionInImplicitTypes::class
    }

    public interface InferenceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InferenceError>
            get() = InferenceError::class
    }

    public interface ProjectionOnNonClassTypeArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ProjectionOnNonClassTypeArgument>
            get() = ProjectionOnNonClassTypeArgument::class
    }

    public interface UpperBoundViolated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolated>
            get() = UpperBoundViolated::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
        public val extraMessage: String
    }

    public interface UpperBoundViolatedDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedDeprecationWarning>
            get() = UpperBoundViolatedDeprecationWarning::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
        public val extraMessage: String
    }

    public interface UpperBoundViolatedInTypeOperatorOrParameterBoundsError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypeOperatorOrParameterBoundsError>
            get() = UpperBoundViolatedInTypeOperatorOrParameterBoundsError::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
        public val extraMessage: String
    }

    public interface UpperBoundViolatedInTypeOperatorOrParameterBoundsWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypeOperatorOrParameterBoundsWarning>
            get() = UpperBoundViolatedInTypeOperatorOrParameterBoundsWarning::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
        public val extraMessage: String
    }

    public interface UpperBoundViolatedInTypealiasExpansion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypealiasExpansion>
            get() = UpperBoundViolatedInTypealiasExpansion::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    public interface UpperBoundViolatedInTypealiasExpansionDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypealiasExpansionDeprecationWarning>
            get() = UpperBoundViolatedInTypealiasExpansionDeprecationWarning::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    public interface UpperBoundViolatedInLhsOfClassLiteralWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInLhsOfClassLiteralWarning>
            get() = UpperBoundViolatedInLhsOfClassLiteralWarning::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    public interface TypeArgumentsNotAllowed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeArgumentsNotAllowed>
            get() = TypeArgumentsNotAllowed::class

        public val place: String
    }

    public interface TypeArgumentsNotAllowedWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeArgumentsNotAllowedWarning>
            get() = TypeArgumentsNotAllowedWarning::class

        public val place: String
    }

    public interface TypeArgumentsNotAllowedInPackageQualifierWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeArgumentsNotAllowedInPackageQualifierWarning>
            get() = TypeArgumentsNotAllowedInPackageQualifierWarning::class
    }

    public interface TypeArgumentsForOuterClassWhenNestedReferenced : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeArgumentsForOuterClassWhenNestedReferenced>
            get() = TypeArgumentsForOuterClassWhenNestedReferenced::class
    }

    public interface WrongNumberOfTypeArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongNumberOfTypeArguments>
            get() = WrongNumberOfTypeArguments::class

        public val expectedCount: Int
        public val owner: KaSymbol
    }

    public interface WrongNumberOfTypeArgumentsWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongNumberOfTypeArgumentsWarning>
            get() = WrongNumberOfTypeArgumentsWarning::class

        public val expectedCount: Int
        public val owner: KaSymbol
        public val type: KaType
    }

    public interface WrongNumberOfTypeArgumentsInLocalClassInLhsWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongNumberOfTypeArgumentsInLocalClassInLhsWarning>
            get() = WrongNumberOfTypeArgumentsInLocalClassInLhsWarning::class

        public val expectedCount: Int
        public val owner: KaSymbol
    }

    public interface WrongNumberOfTypeArgumentsInGetClassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongNumberOfTypeArgumentsInGetClassWarning>
            get() = WrongNumberOfTypeArgumentsInGetClassWarning::class

        public val expectedCount: Int
        public val owner: KaSymbol
    }

    public interface InvalidQualifierInLhsOfCallableReferenceToStaticError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidQualifierInLhsOfCallableReferenceToStaticError>
            get() = InvalidQualifierInLhsOfCallableReferenceToStaticError::class

        public val kind: String
    }

    public interface InvalidQualifierInLhsOfCallableReferenceToStaticWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidQualifierInLhsOfCallableReferenceToStaticWarning>
            get() = InvalidQualifierInLhsOfCallableReferenceToStaticWarning::class

        public val kind: String
    }

    public interface NoTypeArgumentsOnRhs : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoTypeArgumentsOnRhs>
            get() = NoTypeArgumentsOnRhs::class

        public val expectedCount: Int
        public val classifier: KaClassLikeSymbol
    }

    public interface OuterClassArgumentsRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OuterClassArgumentsRequired>
            get() = OuterClassArgumentsRequired::class

        public val outer: KaClassLikeSymbol
    }

    public interface TypeParametersInObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParametersInObject>
            get() = TypeParametersInObject::class
    }

    public interface TypeParametersInAnonymousObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParametersInAnonymousObject>
            get() = TypeParametersInAnonymousObject::class
    }

    public interface IllegalProjectionUsage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalProjectionUsage>
            get() = IllegalProjectionUsage::class
    }

    public interface TypeParametersInEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParametersInEnum>
            get() = TypeParametersInEnum::class
    }

    public interface ConflictingProjection : KaFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass: KClass<ConflictingProjection>
            get() = ConflictingProjection::class

        public val type: KaType
    }

    public interface ConflictingProjectionInTypealiasExpansion : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConflictingProjectionInTypealiasExpansion>
            get() = ConflictingProjectionInTypealiasExpansion::class

        public val type: KaType
    }

    public interface ConflictingProjectionInCallableReferenceWarning : KaFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass: KClass<ConflictingProjectionInCallableReferenceWarning>
            get() = ConflictingProjectionInCallableReferenceWarning::class

        public val type: KaType
    }

    public interface RedundantProjection : KaFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass: KClass<RedundantProjection>
            get() = RedundantProjection::class

        public val type: KaType
    }

    public interface VarianceOnTypeParameterNotAllowed : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<VarianceOnTypeParameterNotAllowed>
            get() = VarianceOnTypeParameterNotAllowed::class
    }

    public interface CatchParameterWithDefaultValue : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CatchParameterWithDefaultValue>
            get() = CatchParameterWithDefaultValue::class
    }

    public interface TypeParameterInCatchClause : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParameterInCatchClause>
            get() = TypeParameterInCatchClause::class
    }

    public interface GenericThrowableSubclass : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<GenericThrowableSubclass>
            get() = GenericThrowableSubclass::class
    }

    public interface InnerClassOfGenericThrowableSubclass : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<InnerClassOfGenericThrowableSubclass>
            get() = InnerClassOfGenericThrowableSubclass::class
    }

    public interface KclassWithNullableTypeParameterInSignature : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<KclassWithNullableTypeParameterInSignature>
            get() = KclassWithNullableTypeParameterInSignature::class

        public val typeParameter: KaTypeParameterSymbol
    }

    public interface TypeParameterAsReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParameterAsReified>
            get() = TypeParameterAsReified::class

        public val typeParameter: KaTypeParameterSymbol
    }

    public interface TypeParameterAsReifiedDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParameterAsReifiedDeprecationWarning>
            get() = TypeParameterAsReifiedDeprecationWarning::class

        public val typeParameter: KaTypeParameterSymbol
    }

    public interface TypeParameterAsReifiedArrayError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParameterAsReifiedArrayError>
            get() = TypeParameterAsReifiedArrayError::class

        public val typeParameter: KaTypeParameterSymbol
    }

    public interface ReifiedTypeForbiddenSubstitution : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ReifiedTypeForbiddenSubstitution>
            get() = ReifiedTypeForbiddenSubstitution::class

        public val type: KaType
    }

    public interface DefinitelyNonNullableAsReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DefinitelyNonNullableAsReified>
            get() = DefinitelyNonNullableAsReified::class
    }

    public interface TypeIntersectionAsReifiedError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeIntersectionAsReifiedError>
            get() = TypeIntersectionAsReifiedError::class

        public val typeParameter: KaTypeParameterSymbol
        public val types: List<KaType>
    }

    public interface TypeIntersectionAsReifiedWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeIntersectionAsReifiedWarning>
            get() = TypeIntersectionAsReifiedWarning::class

        public val typeParameter: KaTypeParameterSymbol
        public val types: List<KaType>
    }

    public interface TypeIntersectionAsReifiedDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeIntersectionAsReifiedDeprecationWarning>
            get() = TypeIntersectionAsReifiedDeprecationWarning::class

        public val typeParameter: KaTypeParameterSymbol
        public val types: List<KaType>
    }

    public interface FinalUpperBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<FinalUpperBound>
            get() = FinalUpperBound::class

        public val type: KaType
    }

    public interface UpperBoundIsExtensionOrContextFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UpperBoundIsExtensionOrContextFunctionType>
            get() = UpperBoundIsExtensionOrContextFunctionType::class
    }

    public interface BoundsNotAllowedIfBoundedByTypeParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<BoundsNotAllowedIfBoundedByTypeParameter>
            get() = BoundsNotAllowedIfBoundedByTypeParameter::class
    }

    public interface OnlyOneClassBoundAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<OnlyOneClassBoundAllowed>
            get() = OnlyOneClassBoundAllowed::class
    }

    public interface RepeatedBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RepeatedBound>
            get() = RepeatedBound::class
    }

    public interface ConflictingUpperBounds : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ConflictingUpperBounds>
            get() = ConflictingUpperBounds::class

        public val typeParameter: KaTypeParameterSymbol
    }

    public interface NameInConstraintIsNotATypeParameter : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass: KClass<NameInConstraintIsNotATypeParameter>
            get() = NameInConstraintIsNotATypeParameter::class

        public val typeParameterName: Name
        public val typeParametersOwner: KaSymbol
    }

    public interface BoundOnTypeAliasParameterNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<BoundOnTypeAliasParameterNotAllowed>
            get() = BoundOnTypeAliasParameterNotAllowed::class
    }

    public interface ReifiedTypeParameterNoInline : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<ReifiedTypeParameterNoInline>
            get() = ReifiedTypeParameterNoInline::class
    }

    public interface ReifiedTypeParameterOnAliasError : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<ReifiedTypeParameterOnAliasError>
            get() = ReifiedTypeParameterOnAliasError::class
    }

    public interface ReifiedTypeParameterOnAliasWarning : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<ReifiedTypeParameterOnAliasWarning>
            get() = ReifiedTypeParameterOnAliasWarning::class
    }

    public interface TypeParametersNotAllowed : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<TypeParametersNotAllowed>
            get() = TypeParametersNotAllowed::class
    }

    public interface IncorrectTypeParameterOfProperty : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<IncorrectTypeParameterOfProperty>
            get() = IncorrectTypeParameterOfProperty::class
    }

    public interface ImplicitNothingReturnType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ImplicitNothingReturnType>
            get() = ImplicitNothingReturnType::class
    }

    public interface ImplicitNothingPropertyType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ImplicitNothingPropertyType>
            get() = ImplicitNothingPropertyType::class
    }

    public interface AbbreviatedNothingReturnType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AbbreviatedNothingReturnType>
            get() = AbbreviatedNothingReturnType::class
    }

    public interface AbbreviatedNothingPropertyType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AbbreviatedNothingPropertyType>
            get() = AbbreviatedNothingPropertyType::class
    }

    public interface CyclicGenericUpperBound : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CyclicGenericUpperBound>
            get() = CyclicGenericUpperBound::class

        public val typeParameters: List<KaTypeParameterSymbol>
    }

    public interface FiniteBoundsViolation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FiniteBoundsViolation>
            get() = FiniteBoundsViolation::class
    }

    public interface FiniteBoundsViolationInJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FiniteBoundsViolationInJava>
            get() = FiniteBoundsViolationInJava::class

        public val containingTypes: List<KaSymbol>
    }

    public interface ExpansiveInheritance : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpansiveInheritance>
            get() = ExpansiveInheritance::class
    }

    public interface ExpansiveInheritanceInJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpansiveInheritanceInJava>
            get() = ExpansiveInheritanceInJava::class

        public val containingTypes: List<KaSymbol>
    }

    public interface DeprecatedTypeParameterSyntax : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<DeprecatedTypeParameterSyntax>
            get() = DeprecatedTypeParameterSyntax::class
    }

    public interface MisplacedTypeParameterConstraints : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<MisplacedTypeParameterConstraints>
            get() = MisplacedTypeParameterConstraints::class
    }

    public interface DynamicSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DynamicSupertype>
            get() = DynamicSupertype::class
    }

    public interface DynamicUpperBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DynamicUpperBound>
            get() = DynamicUpperBound::class
    }

    public interface DynamicReceiverNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DynamicReceiverNotAllowed>
            get() = DynamicReceiverNotAllowed::class
    }

    public interface DynamicReceiverExpectedButWasNonDynamic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DynamicReceiverExpectedButWasNonDynamic>
            get() = DynamicReceiverExpectedButWasNonDynamic::class

        public val actualType: KaType
    }

    public interface IncompatibleTypes : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncompatibleTypes>
            get() = IncompatibleTypes::class

        public val typeA: KaType
        public val typeB: KaType
    }

    public interface IncompatibleTypesWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncompatibleTypesWarning>
            get() = IncompatibleTypesWarning::class

        public val typeA: KaType
        public val typeB: KaType
    }

    public interface TypeVarianceConflictError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeVarianceConflictError>
            get() = TypeVarianceConflictError::class

        public val typeParameter: KaTypeParameterSymbol
        public val typeParameterVariance: Variance
        public val variance: Variance
        public val containingType: KaType
    }

    public interface TypeVarianceConflictInExpandedType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeVarianceConflictInExpandedType>
            get() = TypeVarianceConflictInExpandedType::class

        public val typeParameter: KaTypeParameterSymbol
        public val typeParameterVariance: Variance
        public val variance: Variance
        public val containingType: KaType
    }

    public interface SmartcastImpossible : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<SmartcastImpossible>
            get() = SmartcastImpossible::class

        public val desiredType: KaType
        public val subject: KtExpression
        public val description: String
        public val isCastToNotNull: Boolean
    }

    public interface SmartcastImpossibleOnImplicitInvokeReceiver : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<SmartcastImpossibleOnImplicitInvokeReceiver>
            get() = SmartcastImpossibleOnImplicitInvokeReceiver::class

        public val desiredType: KaType
        public val subject: KtExpression
        public val description: String
        public val isCastToNotNull: Boolean
    }

    public interface DeprecatedSmartcastOnDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DeprecatedSmartcastOnDelegatedProperty>
            get() = DeprecatedSmartcastOnDelegatedProperty::class

        public val desiredType: KaType
        public val property: KaCallableSymbol
    }

    public interface PlatformClassMappedToKotlin : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PlatformClassMappedToKotlin>
            get() = PlatformClassMappedToKotlin::class

        public val kotlinClass: ClassId
    }

    public interface InferredTypeVariableIntoEmptyIntersectionError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InferredTypeVariableIntoEmptyIntersectionError>
            get() = InferredTypeVariableIntoEmptyIntersectionError::class

        public val typeVariableDescription: String
        public val incompatibleTypes: List<KaType>
        public val description: String
        public val causingTypes: String
    }

    public interface InferredTypeVariableIntoEmptyIntersectionWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InferredTypeVariableIntoEmptyIntersectionWarning>
            get() = InferredTypeVariableIntoEmptyIntersectionWarning::class

        public val typeVariableDescription: String
        public val incompatibleTypes: List<KaType>
        public val description: String
        public val causingTypes: String
    }

    public interface InferredTypeVariableIntoPossibleEmptyIntersection : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InferredTypeVariableIntoPossibleEmptyIntersection>
            get() = InferredTypeVariableIntoPossibleEmptyIntersection::class

        public val typeVariableDescription: String
        public val incompatibleTypes: List<KaType>
        public val description: String
        public val causingTypes: String
    }

    public interface IncorrectLeftComponentOfIntersection : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncorrectLeftComponentOfIntersection>
            get() = IncorrectLeftComponentOfIntersection::class
    }

    public interface IncorrectRightComponentOfIntersection : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncorrectRightComponentOfIntersection>
            get() = IncorrectRightComponentOfIntersection::class
    }

    public interface NullableOnDefinitelyNotNullable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableOnDefinitelyNotNullable>
            get() = NullableOnDefinitelyNotNullable::class
    }

    public interface RedundantNullable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RedundantNullable>
            get() = RedundantNullable::class
    }

    public interface InferredInvisibleReifiedTypeArgumentWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InferredInvisibleReifiedTypeArgumentWarning>
            get() = InferredInvisibleReifiedTypeArgumentWarning::class

        public val typeParameter: KaTypeParameterSymbol
        public val typeArgumentType: KaType
    }

    public interface InferredInvisibleVarargTypeArgumentWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InferredInvisibleVarargTypeArgumentWarning>
            get() = InferredInvisibleVarargTypeArgumentWarning::class

        public val typeParameter: KaTypeParameterSymbol
        public val typeArgumentType: KaType
        public val valueParameter: KaSymbol
    }

    public interface InferredInvisibleReturnTypeWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InferredInvisibleReturnTypeWarning>
            get() = InferredInvisibleReturnTypeWarning::class

        public val calleeSymbol: KaSymbol
        public val returnType: KaType
    }

    public interface GenericQualifierOnConstructorCallError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<GenericQualifierOnConstructorCallError>
            get() = GenericQualifierOnConstructorCallError::class
    }

    public interface GenericQualifierOnConstructorCallWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<GenericQualifierOnConstructorCallWarning>
            get() = GenericQualifierOnConstructorCallWarning::class
    }

    public interface AtomicRefWithoutConsistentIdentity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AtomicRefWithoutConsistentIdentity>
            get() = AtomicRefWithoutConsistentIdentity::class

        public val atomicRef: ClassId
        public val argumentType: KaType
        public val suggestedType: ClassId?
    }

    public interface AtomicRefCallArgumentWithoutConsistentIdentity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AtomicRefCallArgumentWithoutConsistentIdentity>
            get() = AtomicRefCallArgumentWithoutConsistentIdentity::class

        public val argumentType: KaType
    }

    public interface ExtensionInClassReferenceNotAllowed : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExtensionInClassReferenceNotAllowed>
            get() = ExtensionInClassReferenceNotAllowed::class

        public val referencedDeclaration: KaCallableSymbol
    }

    public interface CallableReferenceLhsNotAClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CallableReferenceLhsNotAClass>
            get() = CallableReferenceLhsNotAClass::class
    }

    public interface CallableReferenceToAnnotationConstructor : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CallableReferenceToAnnotationConstructor>
            get() = CallableReferenceToAnnotationConstructor::class
    }

    public interface AdaptedCallableReferenceAgainstReflectionType : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AdaptedCallableReferenceAgainstReflectionType>
            get() = AdaptedCallableReferenceAgainstReflectionType::class
    }

    public interface ClassLiteralLhsNotAClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ClassLiteralLhsNotAClass>
            get() = ClassLiteralLhsNotAClass::class
    }

    public interface ClassLiteralLhsNotAClassWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ClassLiteralLhsNotAClassWarning>
            get() = ClassLiteralLhsNotAClassWarning::class
    }

    public interface NullableTypeInClassLiteralLhs : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NullableTypeInClassLiteralLhs>
            get() = NullableTypeInClassLiteralLhs::class
    }

    public interface ExpressionOfNullableTypeInClassLiteralLhs : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpressionOfNullableTypeInClassLiteralLhs>
            get() = ExpressionOfNullableTypeInClassLiteralLhs::class

        public val lhsType: KaType
    }

    public interface ExpressionOfNullableTypeInClassLiteralLhsWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpressionOfNullableTypeInClassLiteralLhsWarning>
            get() = ExpressionOfNullableTypeInClassLiteralLhsWarning::class

        public val lhsType: KaType
    }

    public interface UnsupportedClassLiteralsWithEmptyLhs : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnsupportedClassLiteralsWithEmptyLhs>
            get() = UnsupportedClassLiteralsWithEmptyLhs::class
    }

    public interface UnsupportedArrayOfNothingInClassLiteralLhs : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedArrayOfNothingInClassLiteralLhs>
            get() = UnsupportedArrayOfNothingInClassLiteralLhs::class

        public val unsupported: String
    }

    public interface MutablePropertyWithCapturedType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MutablePropertyWithCapturedType>
            get() = MutablePropertyWithCapturedType::class
    }

    public interface UnsupportedReflectionApi : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnsupportedReflectionApi>
            get() = UnsupportedReflectionApi::class

        public val unsupportedReflectionAPI: String
    }

    public interface NothingToOverride : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<NothingToOverride>
            get() = NothingToOverride::class

        public val declaration: KaCallableSymbol
        public val candidates: List<KaCallableSymbol>
    }

    public interface CannotOverrideInvisibleMember : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<CannotOverrideInvisibleMember>
            get() = CannotOverrideInvisibleMember::class

        public val overridingMember: KaCallableSymbol
        public val baseMember: KaCallableSymbol
    }

    public interface DataClassOverrideConflict : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<DataClassOverrideConflict>
            get() = DataClassOverrideConflict::class

        public val overridingMember: KaCallableSymbol
        public val baseMember: KaCallableSymbol
    }

    public interface DataClassOverrideDefaultValues : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DataClassOverrideDefaultValues>
            get() = DataClassOverrideDefaultValues::class

        public val overridingMember: KaCallableSymbol
        public val baseType: KaClassLikeSymbol
    }

    public interface CannotWeakenAccessPrivilege : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<CannotWeakenAccessPrivilege>
            get() = CannotWeakenAccessPrivilege::class

        public val overridingVisibility: Visibility
        public val overridden: KaCallableSymbol
        public val containingClassName: Name
    }

    public interface CannotWeakenAccessPrivilegeWarning : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<CannotWeakenAccessPrivilegeWarning>
            get() = CannotWeakenAccessPrivilegeWarning::class

        public val overridingVisibility: Visibility
        public val overridden: KaCallableSymbol
        public val containingClassName: Name
    }

    public interface CannotChangeAccessPrivilege : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<CannotChangeAccessPrivilege>
            get() = CannotChangeAccessPrivilege::class

        public val overridingVisibility: Visibility
        public val overridden: KaCallableSymbol
        public val containingClassName: Name
    }

    public interface CannotChangeAccessPrivilegeWarning : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<CannotChangeAccessPrivilegeWarning>
            get() = CannotChangeAccessPrivilegeWarning::class

        public val overridingVisibility: Visibility
        public val overridden: KaCallableSymbol
        public val containingClassName: Name
    }

    public interface CannotInferVisibility : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CannotInferVisibility>
            get() = CannotInferVisibility::class

        public val callable: KaCallableSymbol
    }

    public interface CannotInferVisibilityWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CannotInferVisibilityWarning>
            get() = CannotInferVisibilityWarning::class

        public val callable: KaCallableSymbol
    }

    public interface MultipleDefaultsInheritedFromSupertypes : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypes>
            get() = MultipleDefaultsInheritedFromSupertypes::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    public interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride>
            get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    public interface MultipleDefaultsInheritedFromSupertypesDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesDeprecationError>
            get() = MultipleDefaultsInheritedFromSupertypesDeprecationError::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    public interface MultipleDefaultsInheritedFromSupertypesDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesDeprecationWarning>
            get() = MultipleDefaultsInheritedFromSupertypesDeprecationWarning::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    public interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError>
            get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    public interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning>
            get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    public interface TypealiasExpandsToArrayOfNothings : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasExpandsToArrayOfNothings>
            get() = TypealiasExpandsToArrayOfNothings::class

        public val type: KaType
    }

    public interface OverridingFinalMember : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<OverridingFinalMember>
            get() = OverridingFinalMember::class

        public val overriddenDeclaration: KaCallableSymbol
        public val containingClassName: Name
    }

    public interface ReturnTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ReturnTypeMismatchOnOverride>
            get() = ReturnTypeMismatchOnOverride::class

        public val function: KaCallableSymbol
        public val superFunction: KaCallableSymbol
    }

    public interface PropertyTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<PropertyTypeMismatchOnOverride>
            get() = PropertyTypeMismatchOnOverride::class

        public val property: KaCallableSymbol
        public val superProperty: KaCallableSymbol
    }

    public interface VarTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VarTypeMismatchOnOverride>
            get() = VarTypeMismatchOnOverride::class

        public val variable: KaCallableSymbol
        public val superVariable: KaCallableSymbol
    }

    public interface ReturnTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ReturnTypeMismatchOnInheritance>
            get() = ReturnTypeMismatchOnInheritance::class

        public val conflictingDeclaration1: KaCallableSymbol
        public val conflictingDeclaration2: KaCallableSymbol
    }

    public interface PropertyTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<PropertyTypeMismatchOnInheritance>
            get() = PropertyTypeMismatchOnInheritance::class

        public val conflictingDeclaration1: KaCallableSymbol
        public val conflictingDeclaration2: KaCallableSymbol
    }

    public interface VarTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<VarTypeMismatchOnInheritance>
            get() = VarTypeMismatchOnInheritance::class

        public val conflictingDeclaration1: KaCallableSymbol
        public val conflictingDeclaration2: KaCallableSymbol
    }

    public interface ReturnTypeMismatchByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ReturnTypeMismatchByDelegation>
            get() = ReturnTypeMismatchByDelegation::class

        public val delegateDeclaration: KaCallableSymbol
        public val baseDeclaration: KaCallableSymbol
    }

    public interface PropertyTypeMismatchByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<PropertyTypeMismatchByDelegation>
            get() = PropertyTypeMismatchByDelegation::class

        public val delegateDeclaration: KaCallableSymbol
        public val baseDeclaration: KaCallableSymbol
    }

    public interface VarOverriddenByValByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<VarOverriddenByValByDelegation>
            get() = VarOverriddenByValByDelegation::class

        public val delegateDeclaration: KaCallableSymbol
        public val baseDeclaration: KaCallableSymbol
    }

    public interface ConflictingInheritedMembers : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ConflictingInheritedMembers>
            get() = ConflictingInheritedMembers::class

        public val owner: KaClassLikeSymbol
        public val conflictingDeclarations: List<KaCallableSymbol>
    }

    public interface AbstractMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<AbstractMemberNotImplemented>
            get() = AbstractMemberNotImplemented::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    public interface AbstractMemberIncorrectlyDelegatedError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<AbstractMemberIncorrectlyDelegatedError>
            get() = AbstractMemberIncorrectlyDelegatedError::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    public interface AbstractMemberIncorrectlyDelegatedWarning : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<AbstractMemberIncorrectlyDelegatedWarning>
            get() = AbstractMemberIncorrectlyDelegatedWarning::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    public interface AbstractMemberNotImplementedByEnumEntry : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass: KClass<AbstractMemberNotImplementedByEnumEntry>
            get() = AbstractMemberNotImplementedByEnumEntry::class

        public val enumEntry: KaSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    public interface AbstractClassMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<AbstractClassMemberNotImplemented>
            get() = AbstractClassMemberNotImplemented::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    public interface InvisibleAbstractMemberFromSuperError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<InvisibleAbstractMemberFromSuperError>
            get() = InvisibleAbstractMemberFromSuperError::class

        public val classOrObject: KaClassLikeSymbol
        public val invisibleDeclarations: List<KaCallableSymbol>
    }

    public interface AmbiguousAnonymousTypeInferred : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<AmbiguousAnonymousTypeInferred>
            get() = AmbiguousAnonymousTypeInferred::class

        public val superTypes: List<KaType>
    }

    public interface ManyImplMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ManyImplMemberNotImplemented>
            get() = ManyImplMemberNotImplemented::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclaration: KaCallableSymbol
    }

    public interface ManyInterfacesMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ManyInterfacesMemberNotImplemented>
            get() = ManyInterfacesMemberNotImplemented::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclaration: KaCallableSymbol
    }

    public interface OverridingFinalMemberByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<OverridingFinalMemberByDelegation>
            get() = OverridingFinalMemberByDelegation::class

        public val delegatedDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    public interface DelegatedMemberHidesSupertypeOverride : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<DelegatedMemberHidesSupertypeOverride>
            get() = DelegatedMemberHidesSupertypeOverride::class

        public val delegatedDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    public interface VarOverriddenByVal : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VarOverriddenByVal>
            get() = VarOverriddenByVal::class

        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    public interface VarImplementedByInheritedValError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VarImplementedByInheritedValError>
            get() = VarImplementedByInheritedValError::class

        public val classOrObject: KaClassLikeSymbol
        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    public interface VarImplementedByInheritedValWarning : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VarImplementedByInheritedValWarning>
            get() = VarImplementedByInheritedValWarning::class

        public val classOrObject: KaClassLikeSymbol
        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    public interface NonFinalMemberInFinalClass : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NonFinalMemberInFinalClass>
            get() = NonFinalMemberInFinalClass::class
    }

    public interface NonFinalMemberInObject : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NonFinalMemberInObject>
            get() = NonFinalMemberInObject::class
    }

    public interface VirtualMemberHidden : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VirtualMemberHidden>
            get() = VirtualMemberHidden::class

        public val declared: KaCallableSymbol
        public val overriddenContainer: KaClassLikeSymbol
    }

    public interface ParameterNameChangedOnOverride : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ParameterNameChangedOnOverride>
            get() = ParameterNameChangedOnOverride::class

        public val superType: KaClassLikeSymbol
        public val conflictingParameter: KaSymbol
    }

    public interface DifferentNamesForTheSameParameterInSupertypes : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<DifferentNamesForTheSameParameterInSupertypes>
            get() = DifferentNamesForTheSameParameterInSupertypes::class

        public val currentParameter: KaSymbol
        public val conflictingParameter: KaSymbol
        public val parameterNumber: Int
        public val conflictingFunctions: List<KaFunctionSymbol>
    }

    public interface SuspendOverriddenByNonSuspend : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass: KClass<SuspendOverriddenByNonSuspend>
            get() = SuspendOverriddenByNonSuspend::class

        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    public interface NonSuspendOverriddenBySuspend : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass: KClass<NonSuspendOverriddenBySuspend>
            get() = NonSuspendOverriddenBySuspend::class

        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    public interface OverridingIgnorableWithMustUse : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<OverridingIgnorableWithMustUse>
            get() = OverridingIgnorableWithMustUse::class

        public val method: KaCallableSymbol
        public val parentClass: KaClassLikeSymbol
    }

    public interface ManyCompanionObjects : KaFirDiagnostic<KtObjectDeclaration> {
        override val diagnosticClass: KClass<ManyCompanionObjects>
            get() = ManyCompanionObjects::class
    }

    public interface ConflictingOverloads : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConflictingOverloads>
            get() = ConflictingOverloads::class

        public val conflictingOverloads: List<KaSymbol>
    }

    public interface Redeclaration : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<Redeclaration>
            get() = Redeclaration::class

        public val conflictingDeclarations: List<KaSymbol>
    }

    public interface ClassifierRedeclaration : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ClassifierRedeclaration>
            get() = ClassifierRedeclaration::class

        public val conflictingDeclarations: List<KaSymbol>
    }

    public interface PackageConflictsWithClassifier : KaFirDiagnostic<KtPackageDirective> {
        override val diagnosticClass: KClass<PackageConflictsWithClassifier>
            get() = PackageConflictsWithClassifier::class

        public val conflictingClassId: ClassId
    }

    public interface ExpectAndActualInTheSameModule : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectAndActualInTheSameModule>
            get() = ExpectAndActualInTheSameModule::class

        public val declaration: KaSymbol
    }

    public interface MethodOfAnyImplementedInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MethodOfAnyImplementedInInterface>
            get() = MethodOfAnyImplementedInInterface::class
    }

    public interface ExtensionShadowedByMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExtensionShadowedByMember>
            get() = ExtensionShadowedByMember::class

        public val member: KaCallableSymbol
    }

    public interface ExtensionFunctionShadowedByMemberPropertyWithInvoke : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExtensionFunctionShadowedByMemberPropertyWithInvoke>
            get() = ExtensionFunctionShadowedByMemberPropertyWithInvoke::class

        public val member: KaCallableSymbol
        public val invokeOperator: KaCallableSymbol
    }

    public interface LocalObjectNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<LocalObjectNotAllowed>
            get() = LocalObjectNotAllowed::class

        public val objectName: Name
    }

    public interface LocalInterfaceNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<LocalInterfaceNotAllowed>
            get() = LocalInterfaceNotAllowed::class

        public val interfaceName: Name
    }

    public interface AbstractFunctionInNonAbstractClass : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<AbstractFunctionInNonAbstractClass>
            get() = AbstractFunctionInNonAbstractClass::class

        public val function: KaCallableSymbol
        public val containingClass: KaClassLikeSymbol
    }

    public interface AbstractFunctionWithBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<AbstractFunctionWithBody>
            get() = AbstractFunctionWithBody::class

        public val function: KaCallableSymbol
    }

    public interface NonAbstractFunctionWithNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<NonAbstractFunctionWithNoBody>
            get() = NonAbstractFunctionWithNoBody::class

        public val function: KaCallableSymbol
    }

    public interface PrivateFunctionWithNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<PrivateFunctionWithNoBody>
            get() = PrivateFunctionWithNoBody::class

        public val function: KaCallableSymbol
    }

    public interface NonMemberFunctionNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<NonMemberFunctionNoBody>
            get() = NonMemberFunctionNoBody::class

        public val function: KaCallableSymbol
    }

    public interface FunctionDeclarationWithNoName : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<FunctionDeclarationWithNoName>
            get() = FunctionDeclarationWithNoName::class
    }

    public interface AnonymousFunctionWithName : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<AnonymousFunctionWithName>
            get() = AnonymousFunctionWithName::class
    }

    public interface SingleAnonymousFunctionWithNameError : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<SingleAnonymousFunctionWithNameError>
            get() = SingleAnonymousFunctionWithNameError::class
    }

    public interface SingleAnonymousFunctionWithNameWarning : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<SingleAnonymousFunctionWithNameWarning>
            get() = SingleAnonymousFunctionWithNameWarning::class
    }

    public interface AnonymousFunctionParameterWithDefaultValue : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<AnonymousFunctionParameterWithDefaultValue>
            get() = AnonymousFunctionParameterWithDefaultValue::class
    }

    public interface UselessVarargOnParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<UselessVarargOnParameter>
            get() = UselessVarargOnParameter::class
    }

    public interface MultipleVarargParameters : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<MultipleVarargParameters>
            get() = MultipleVarargParameters::class
    }

    public interface ForbiddenVarargParameterType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ForbiddenVarargParameterType>
            get() = ForbiddenVarargParameterType::class

        public val varargParameterType: KaType
    }

    public interface ValueParameterWithoutExplicitType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValueParameterWithoutExplicitType>
            get() = ValueParameterWithoutExplicitType::class
    }

    public interface CannotInferParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotInferParameterType>
            get() = CannotInferParameterType::class

        public val parameter: KaTypeParameterSymbol
    }

    public interface CannotInferValueParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotInferValueParameterType>
            get() = CannotInferValueParameterType::class

        public val parameter: KaSymbol
    }

    public interface CannotInferItParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotInferItParameterType>
            get() = CannotInferItParameterType::class
    }

    public interface CannotInferReceiverParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotInferReceiverParameterType>
            get() = CannotInferReceiverParameterType::class
    }

    public interface NoTailCallsFound : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<NoTailCallsFound>
            get() = NoTailCallsFound::class
    }

    public interface TailrecOnVirtualMemberError : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<TailrecOnVirtualMemberError>
            get() = TailrecOnVirtualMemberError::class
    }

    public interface NonTailRecursiveCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonTailRecursiveCall>
            get() = NonTailRecursiveCall::class
    }

    public interface TailRecursionInTryIsNotSupported : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TailRecursionInTryIsNotSupported>
            get() = TailRecursionInTryIsNotSupported::class
    }

    public interface DataObjectCustomEqualsOrHashCode : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<DataObjectCustomEqualsOrHashCode>
            get() = DataObjectCustomEqualsOrHashCode::class
    }

    public interface DefaultValueNotAllowedInOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DefaultValueNotAllowedInOverride>
            get() = DefaultValueNotAllowedInOverride::class
    }

    public interface FunInterfaceWrongCountOfAbstractMembers : KaFirDiagnostic<KtClass> {
        override val diagnosticClass: KClass<FunInterfaceWrongCountOfAbstractMembers>
            get() = FunInterfaceWrongCountOfAbstractMembers::class
    }

    public interface FunInterfaceCannotHaveAbstractProperties : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<FunInterfaceCannotHaveAbstractProperties>
            get() = FunInterfaceCannotHaveAbstractProperties::class
    }

    public interface FunInterfaceAbstractMethodWithTypeParameters : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<FunInterfaceAbstractMethodWithTypeParameters>
            get() = FunInterfaceAbstractMethodWithTypeParameters::class
    }

    public interface FunInterfaceAbstractMethodWithDefaultValue : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<FunInterfaceAbstractMethodWithDefaultValue>
            get() = FunInterfaceAbstractMethodWithDefaultValue::class
    }

    public interface FunInterfaceWithSuspendFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<FunInterfaceWithSuspendFunction>
            get() = FunInterfaceWithSuspendFunction::class
    }

    public interface AbstractPropertyInNonAbstractClass : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<AbstractPropertyInNonAbstractClass>
            get() = AbstractPropertyInNonAbstractClass::class

        public val property: KaCallableSymbol
        public val containingClass: KaClassLikeSymbol
    }

    public interface PrivatePropertyInInterface : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<PrivatePropertyInInterface>
            get() = PrivatePropertyInInterface::class
    }

    public interface AbstractPropertyWithInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AbstractPropertyWithInitializer>
            get() = AbstractPropertyWithInitializer::class
    }

    public interface PropertyInitializerInInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<PropertyInitializerInInterface>
            get() = PropertyInitializerInInterface::class
    }

    public interface PropertyWithNoTypeNoInitializer : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<PropertyWithNoTypeNoInitializer>
            get() = PropertyWithNoTypeNoInitializer::class
    }

    public interface AbstractPropertyWithoutType : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<AbstractPropertyWithoutType>
            get() = AbstractPropertyWithoutType::class
    }

    public interface LateinitPropertyWithoutType : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<LateinitPropertyWithoutType>
            get() = LateinitPropertyWithoutType::class
    }

    public interface MustBeInitialized : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitialized>
            get() = MustBeInitialized::class
    }

    public interface MustBeInitializedWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedWarning>
            get() = MustBeInitializedWarning::class
    }

    public interface MustBeInitializedOrBeFinal : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrBeFinal>
            get() = MustBeInitializedOrBeFinal::class
    }

    public interface MustBeInitializedOrBeFinalWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrBeFinalWarning>
            get() = MustBeInitializedOrBeFinalWarning::class
    }

    public interface MustBeInitializedOrBeAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrBeAbstract>
            get() = MustBeInitializedOrBeAbstract::class
    }

    public interface MustBeInitializedOrBeAbstractWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrBeAbstractWarning>
            get() = MustBeInitializedOrBeAbstractWarning::class
    }

    public interface MustBeInitializedOrFinalOrAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrFinalOrAbstract>
            get() = MustBeInitializedOrFinalOrAbstract::class
    }

    public interface MustBeInitializedOrFinalOrAbstractWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrFinalOrAbstractWarning>
            get() = MustBeInitializedOrFinalOrAbstractWarning::class
    }

    public interface ExplicitFieldMustBeInitialized : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<ExplicitFieldMustBeInitialized>
            get() = ExplicitFieldMustBeInitialized::class
    }

    public interface ExtensionPropertyMustHaveAccessorsOrBeAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<ExtensionPropertyMustHaveAccessorsOrBeAbstract>
            get() = ExtensionPropertyMustHaveAccessorsOrBeAbstract::class
    }

    public interface UnnecessaryLateinit : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<UnnecessaryLateinit>
            get() = UnnecessaryLateinit::class
    }

    public interface BackingFieldInInterface : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<BackingFieldInInterface>
            get() = BackingFieldInInterface::class
    }

    public interface ExtensionPropertyWithBackingField : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExtensionPropertyWithBackingField>
            get() = ExtensionPropertyWithBackingField::class
    }

    public interface PropertyInitializerNoBackingField : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<PropertyInitializerNoBackingField>
            get() = PropertyInitializerNoBackingField::class
    }

    public interface AbstractDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AbstractDelegatedProperty>
            get() = AbstractDelegatedProperty::class
    }

    public interface DelegatedPropertyInInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegatedPropertyInInterface>
            get() = DelegatedPropertyInInterface::class
    }

    public interface AbstractPropertyWithGetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass: KClass<AbstractPropertyWithGetter>
            get() = AbstractPropertyWithGetter::class
    }

    public interface AbstractPropertyWithSetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass: KClass<AbstractPropertyWithSetter>
            get() = AbstractPropertyWithSetter::class
    }

    public interface PrivateSetterForAbstractProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<PrivateSetterForAbstractProperty>
            get() = PrivateSetterForAbstractProperty::class
    }

    public interface PrivateSetterForOpenProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<PrivateSetterForOpenProperty>
            get() = PrivateSetterForOpenProperty::class
    }

    public interface ValWithSetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass: KClass<ValWithSetter>
            get() = ValWithSetter::class
    }

    public interface ConstValNotTopLevelOrObject : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConstValNotTopLevelOrObject>
            get() = ConstValNotTopLevelOrObject::class
    }

    public interface ConstValWithGetter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConstValWithGetter>
            get() = ConstValWithGetter::class
    }

    public interface ConstValWithDelegate : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ConstValWithDelegate>
            get() = ConstValWithDelegate::class
    }

    public interface TypeCantBeUsedForConstVal : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<TypeCantBeUsedForConstVal>
            get() = TypeCantBeUsedForConstVal::class

        public val constValType: KaType
    }

    public interface ConstValWithoutInitializer : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<ConstValWithoutInitializer>
            get() = ConstValWithoutInitializer::class
    }

    public interface ConstValWithEbf : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<ConstValWithEbf>
            get() = ConstValWithEbf::class
    }

    public interface ConstValWithNonConstInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ConstValWithNonConstInitializer>
            get() = ConstValWithNonConstInitializer::class
    }

    public interface DelegateUsesExtensionPropertyTypeParameterError : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<DelegateUsesExtensionPropertyTypeParameterError>
            get() = DelegateUsesExtensionPropertyTypeParameterError::class

        public val usedTypeParameter: KaTypeParameterSymbol
    }

    public interface GetterVisibilityDiffersFromPropertyVisibility : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<GetterVisibilityDiffersFromPropertyVisibility>
            get() = GetterVisibilityDiffersFromPropertyVisibility::class
    }

    public interface SetterVisibilityInconsistentWithPropertyVisibility : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<SetterVisibilityInconsistentWithPropertyVisibility>
            get() = SetterVisibilityInconsistentWithPropertyVisibility::class
    }

    public interface WrongGetterReturnType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongGetterReturnType>
            get() = WrongGetterReturnType::class

        public val expectedType: KaType
        public val actualType: KaType
    }

    public interface WrongSetterReturnType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongSetterReturnType>
            get() = WrongSetterReturnType::class
    }

    public interface WrongSetterParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongSetterParameterType>
            get() = WrongSetterParameterType::class

        public val expectedType: KaType
        public val actualType: KaType
    }

    public interface AccessorForDelegatedProperty : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass: KClass<AccessorForDelegatedProperty>
            get() = AccessorForDelegatedProperty::class
    }

    public interface PropertyInitializerWithExplicitFieldDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<PropertyInitializerWithExplicitFieldDeclaration>
            get() = PropertyInitializerWithExplicitFieldDeclaration::class
    }

    public interface PropertyFieldDeclarationMissingInitializer : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<PropertyFieldDeclarationMissingInitializer>
            get() = PropertyFieldDeclarationMissingInitializer::class
    }

    public interface LateinitNullableBackingField : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<LateinitNullableBackingField>
            get() = LateinitNullableBackingField::class
    }

    public interface BackingFieldForDelegatedProperty : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<BackingFieldForDelegatedProperty>
            get() = BackingFieldForDelegatedProperty::class
    }

    public interface VarPropertyWithExplicitBackingField : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VarPropertyWithExplicitBackingField>
            get() = VarPropertyWithExplicitBackingField::class
    }

    public interface NonFinalPropertyWithExplicitBackingField : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<NonFinalPropertyWithExplicitBackingField>
            get() = NonFinalPropertyWithExplicitBackingField::class
    }

    public interface ExpectPropertyWithExplicitBackingField : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExpectPropertyWithExplicitBackingField>
            get() = ExpectPropertyWithExplicitBackingField::class
    }

    public interface InconsistentBackingFieldType : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<InconsistentBackingFieldType>
            get() = InconsistentBackingFieldType::class
    }

    public interface ExplicitFieldVisibilityMustBeLessPermissive : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<ExplicitFieldVisibilityMustBeLessPermissive>
            get() = ExplicitFieldVisibilityMustBeLessPermissive::class
    }

    public interface PropertyWithExplicitFieldAndAccessors : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PropertyWithExplicitFieldAndAccessors>
            get() = PropertyWithExplicitFieldAndAccessors::class
    }

    public interface ExplicitBackingFieldInInterface : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<ExplicitBackingFieldInInterface>
            get() = ExplicitBackingFieldInInterface::class
    }

    public interface ExplicitBackingFieldInAbstractProperty : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<ExplicitBackingFieldInAbstractProperty>
            get() = ExplicitBackingFieldInAbstractProperty::class
    }

    public interface ExplicitBackingFieldInExtension : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<ExplicitBackingFieldInExtension>
            get() = ExplicitBackingFieldInExtension::class
    }

    public interface RedundantExplicitBackingField : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<RedundantExplicitBackingField>
            get() = RedundantExplicitBackingField::class
    }

    public interface AbstractPropertyInPrimaryConstructorParameters : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<AbstractPropertyInPrimaryConstructorParameters>
            get() = AbstractPropertyInPrimaryConstructorParameters::class
    }

    public interface LocalVariableWithTypeParametersWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<LocalVariableWithTypeParametersWarning>
            get() = LocalVariableWithTypeParametersWarning::class
    }

    public interface LocalVariableWithTypeParameters : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<LocalVariableWithTypeParameters>
            get() = LocalVariableWithTypeParameters::class
    }

    public interface ExplicitTypeArgumentsInPropertyAccess : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExplicitTypeArgumentsInPropertyAccess>
            get() = ExplicitTypeArgumentsInPropertyAccess::class

        public val kind: String
    }

    public interface ExplicitTypeArgumentsInPropertyAccessWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExplicitTypeArgumentsInPropertyAccessWarning>
            get() = ExplicitTypeArgumentsInPropertyAccessWarning::class

        public val kind: String
    }

    public interface SafeCallableReferenceCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<SafeCallableReferenceCall>
            get() = SafeCallableReferenceCall::class
    }

    public interface LateinitIntrinsicCallOnNonLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LateinitIntrinsicCallOnNonLiteral>
            get() = LateinitIntrinsicCallOnNonLiteral::class
    }

    public interface LateinitIntrinsicCallOnNonLateinit : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LateinitIntrinsicCallOnNonLateinit>
            get() = LateinitIntrinsicCallOnNonLateinit::class
    }

    public interface LateinitIntrinsicCallInInlineFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LateinitIntrinsicCallInInlineFunction>
            get() = LateinitIntrinsicCallInInlineFunction::class
    }

    public interface LateinitIntrinsicCallOnNonAccessibleProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LateinitIntrinsicCallOnNonAccessibleProperty>
            get() = LateinitIntrinsicCallOnNonAccessibleProperty::class

        public val declaration: KaSymbol
    }

    public interface LocalExtensionProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LocalExtensionProperty>
            get() = LocalExtensionProperty::class
    }

    public interface UnnamedVarProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnnamedVarProperty>
            get() = UnnamedVarProperty::class
    }

    public interface UnnamedDelegatedProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnnamedDelegatedProperty>
            get() = UnnamedDelegatedProperty::class
    }

    public interface UnnamedPropertyWithImplicitIgnorableType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnnamedPropertyWithImplicitIgnorableType>
            get() = UnnamedPropertyWithImplicitIgnorableType::class

        public val ignorableType: KaType
    }

    public interface DestructuringShortFormNameMismatch : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DestructuringShortFormNameMismatch>
            get() = DestructuringShortFormNameMismatch::class

        public val destructuredName: Name
        public val propertyName: Name
    }

    public interface DestructuringShortFormOfNonDataClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DestructuringShortFormOfNonDataClass>
            get() = DestructuringShortFormOfNonDataClass::class

        public val rhsType: KaType
        public val destructuredName: Name
        public val target: String
    }

    public interface DestructuringShortFormUnderscore : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DestructuringShortFormUnderscore>
            get() = DestructuringShortFormUnderscore::class
    }

    public interface NameBasedDestructuringUnderscoreWithoutRenaming : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NameBasedDestructuringUnderscoreWithoutRenaming>
            get() = NameBasedDestructuringUnderscoreWithoutRenaming::class
    }

    public interface ExpectedDeclarationWithBody : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExpectedDeclarationWithBody>
            get() = ExpectedDeclarationWithBody::class
    }

    public interface ExpectedClassConstructorDelegationCall : KaFirDiagnostic<KtConstructorDelegationCall> {
        override val diagnosticClass: KClass<ExpectedClassConstructorDelegationCall>
            get() = ExpectedClassConstructorDelegationCall::class
    }

    public interface ExpectedClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ExpectedClassConstructorPropertyParameter>
            get() = ExpectedClassConstructorPropertyParameter::class
    }

    public interface ExpectedEnumConstructor : KaFirDiagnostic<KtConstructor<*>> {
        override val diagnosticClass: KClass<ExpectedEnumConstructor>
            get() = ExpectedEnumConstructor::class
    }

    public interface ExpectedEnumEntryWithBody : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass: KClass<ExpectedEnumEntryWithBody>
            get() = ExpectedEnumEntryWithBody::class
    }

    public interface ExpectedPropertyInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExpectedPropertyInitializer>
            get() = ExpectedPropertyInitializer::class
    }

    public interface ExpectedDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExpectedDelegatedProperty>
            get() = ExpectedDelegatedProperty::class
    }

    public interface ExpectedLateinitProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ExpectedLateinitProperty>
            get() = ExpectedLateinitProperty::class
    }

    public interface SupertypeInitializedInExpectedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeInitializedInExpectedClass>
            get() = SupertypeInitializedInExpectedClass::class
    }

    public interface ExpectedPrivateDeclaration : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ExpectedPrivateDeclaration>
            get() = ExpectedPrivateDeclaration::class
    }

    public interface ExpectedExternalDeclaration : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ExpectedExternalDeclaration>
            get() = ExpectedExternalDeclaration::class
    }

    public interface ExpectedTailrecFunction : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ExpectedTailrecFunction>
            get() = ExpectedTailrecFunction::class
    }

    public interface ImplementationByDelegationInExpectClass : KaFirDiagnostic<KtDelegatedSuperTypeEntry> {
        override val diagnosticClass: KClass<ImplementationByDelegationInExpectClass>
            get() = ImplementationByDelegationInExpectClass::class
    }

    public interface ActualTypeAliasNotToClass : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasNotToClass>
            get() = ActualTypeAliasNotToClass::class
    }

    public interface ActualTypeAliasToClassWithDeclarationSiteVariance : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasToClassWithDeclarationSiteVariance>
            get() = ActualTypeAliasToClassWithDeclarationSiteVariance::class
    }

    public interface ActualTypeAliasWithUseSiteVariance : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasWithUseSiteVariance>
            get() = ActualTypeAliasWithUseSiteVariance::class
    }

    public interface ActualTypeAliasWithComplexSubstitution : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasWithComplexSubstitution>
            get() = ActualTypeAliasWithComplexSubstitution::class
    }

    public interface ActualTypeAliasToNullableType : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasToNullableType>
            get() = ActualTypeAliasToNullableType::class
    }

    public interface ActualTypeAliasToNothing : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasToNothing>
            get() = ActualTypeAliasToNothing::class
    }

    public interface ActualFunctionWithDefaultArguments : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<ActualFunctionWithDefaultArguments>
            get() = ActualFunctionWithDefaultArguments::class
    }

    public interface DefaultArgumentsInExpectWithActualTypealias : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<DefaultArgumentsInExpectWithActualTypealias>
            get() = DefaultArgumentsInExpectWithActualTypealias::class

        public val expectClassSymbol: KaClassLikeSymbol
        public val members: List<KaCallableSymbol>
    }

    public interface DefaultArgumentsInExpectActualizedByFakeOverride : KaFirDiagnostic<KtClass> {
        override val diagnosticClass: KClass<DefaultArgumentsInExpectActualizedByFakeOverride>
            get() = DefaultArgumentsInExpectActualizedByFakeOverride::class

        public val expectClassSymbol: KaClassLikeSymbol
        public val members: List<KaFunctionSymbol>
    }

    public interface ExpectedFunctionSourceWithDefaultArgumentsNotFound : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpectedFunctionSourceWithDefaultArgumentsNotFound>
            get() = ExpectedFunctionSourceWithDefaultArgumentsNotFound::class
    }

    public interface ActualWithoutExpect : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ActualWithoutExpect>
            get() = ActualWithoutExpect::class

        public val declaration: KaSymbol
        public val compatibility: Map<ExpectActualMatchingCompatibility, List<KaSymbol>>
    }

    public interface ExpectActualIncompatibleClassTypeParameterCount : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassTypeParameterCount>
            get() = ExpectActualIncompatibleClassTypeParameterCount::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleReturnType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleReturnType>
            get() = ExpectActualIncompatibleReturnType::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleEqualityBounds : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleEqualityBounds>
            get() = ExpectActualIncompatibleEqualityBounds::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleParameterNames>
            get() = ExpectActualIncompatibleParameterNames::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleContextParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleContextParameterNames>
            get() = ExpectActualIncompatibleContextParameterNames::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleTypeParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleTypeParameterNames>
            get() = ExpectActualIncompatibleTypeParameterNames::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleValueParameterVararg : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleValueParameterVararg>
            get() = ExpectActualIncompatibleValueParameterVararg::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleValueParameterNoinline : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleValueParameterNoinline>
            get() = ExpectActualIncompatibleValueParameterNoinline::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleValueParameterCrossinline : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleValueParameterCrossinline>
            get() = ExpectActualIncompatibleValueParameterCrossinline::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleFunctionModifiersDifferent : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleFunctionModifiersDifferent>
            get() = ExpectActualIncompatibleFunctionModifiersDifferent::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleFunctionModifiersNotSubset : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleFunctionModifiersNotSubset>
            get() = ExpectActualIncompatibleFunctionModifiersNotSubset::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride>
            get() = ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatiblePropertyKind : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatiblePropertyKind>
            get() = ExpectActualIncompatiblePropertyKind::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatiblePropertyLateinitModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatiblePropertyLateinitModifier>
            get() = ExpectActualIncompatiblePropertyLateinitModifier::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatiblePropertyConstModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatiblePropertyConstModifier>
            get() = ExpectActualIncompatiblePropertyConstModifier::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatiblePropertySetterVisibility : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatiblePropertySetterVisibility>
            get() = ExpectActualIncompatiblePropertySetterVisibility::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleClassKind : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassKind>
            get() = ExpectActualIncompatibleClassKind::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleClassModifiers : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassModifiers>
            get() = ExpectActualIncompatibleClassModifiers::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleFunInterfaceModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleFunInterfaceModifier>
            get() = ExpectActualIncompatibleFunInterfaceModifier::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleSupertypes : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleSupertypes>
            get() = ExpectActualIncompatibleSupertypes::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleNestedTypeAlias : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleNestedTypeAlias>
            get() = ExpectActualIncompatibleNestedTypeAlias::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleEnumEntries : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleEnumEntries>
            get() = ExpectActualIncompatibleEnumEntries::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleIllegalRequiresOptIn : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleIllegalRequiresOptIn>
            get() = ExpectActualIncompatibleIllegalRequiresOptIn::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleModality : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleModality>
            get() = ExpectActualIncompatibleModality::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleVisibility : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleVisibility>
            get() = ExpectActualIncompatibleVisibility::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleClassTypeParameterUpperBounds : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassTypeParameterUpperBounds>
            get() = ExpectActualIncompatibleClassTypeParameterUpperBounds::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleTypeParameterVariance : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleTypeParameterVariance>
            get() = ExpectActualIncompatibleTypeParameterVariance::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleTypeParameterReified : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleTypeParameterReified>
            get() = ExpectActualIncompatibleTypeParameterReified::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectActualIncompatibleClassScope : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassScope>
            get() = ExpectActualIncompatibleClassScope::class

        public val actualClass: KaSymbol
        public val expectMemberDeclaration: KaSymbol
        public val actualMemberDeclaration: KaSymbol
        public val reason: String
    }

    public interface ExpectRefinementAnnotationWrongTarget : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectRefinementAnnotationWrongTarget>
            get() = ExpectRefinementAnnotationWrongTarget::class
    }

    public interface AmbiguousExpects : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<AmbiguousExpects>
            get() = AmbiguousExpects::class

        public val declaration: KaSymbol
        public val modules: List<FirModuleData>
    }

    public interface NoActualClassMemberForExpectedClass : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NoActualClassMemberForExpectedClass>
            get() = NoActualClassMemberForExpectedClass::class

        public val declaration: KaSymbol
        public val members: List<Pair<KaSymbol, Map<Mismatch, List<KaSymbol>>>>
    }

    public interface ActualMissing : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ActualMissing>
            get() = ActualMissing::class
    }

    public interface ExpectRefinementAnnotationMissing : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectRefinementAnnotationMissing>
            get() = ExpectRefinementAnnotationMissing::class
    }

    public interface ExpectActualClassifiersAreInBetaWarning : KaFirDiagnostic<KtClassLikeDeclaration> {
        override val diagnosticClass: KClass<ExpectActualClassifiersAreInBetaWarning>
            get() = ExpectActualClassifiersAreInBetaWarning::class
    }

    public interface NotAMultiplatformCompilation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotAMultiplatformCompilation>
            get() = NotAMultiplatformCompilation::class
    }

    public interface ExpectActualOptInAnnotation : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualOptInAnnotation>
            get() = ExpectActualOptInAnnotation::class
    }

    public interface ActualTypealiasToSpecialAnnotation : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypealiasToSpecialAnnotation>
            get() = ActualTypealiasToSpecialAnnotation::class

        public val typealiasedClassId: ClassId
    }

    public interface ActualAnnotationsNotMatchExpect : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ActualAnnotationsNotMatchExpect>
            get() = ActualAnnotationsNotMatchExpect::class

        public val expectSymbol: KaSymbol
        public val actualSymbol: KaSymbol
        public val actualAnnotationTargetSourceElement: PsiElement?
        public val incompatibilityType: ExpectActualAnnotationsIncompatibilityType<FirAnnotation>
    }

    public interface ActualIgnorabilityNotMatchExpect : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ActualIgnorabilityNotMatchExpect>
            get() = ActualIgnorabilityNotMatchExpect::class

        public val expectDeclaration: KaSymbol
        public val expectIgnorability: ReturnValueStatus
        public val actualDeclaration: KaSymbol
        public val actualIgnorability: ReturnValueStatus
    }

    public interface OptionalDeclarationOutsideOfAnnotationEntry : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptionalDeclarationOutsideOfAnnotationEntry>
            get() = OptionalDeclarationOutsideOfAnnotationEntry::class
    }

    public interface OptionalDeclarationUsageInNonCommonSource : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptionalDeclarationUsageInNonCommonSource>
            get() = OptionalDeclarationUsageInNonCommonSource::class
    }

    public interface OptionalExpectationNotOnExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptionalExpectationNotOnExpected>
            get() = OptionalExpectationNotOnExpected::class
    }

    public interface UninitializedVariable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UninitializedVariable>
            get() = UninitializedVariable::class

        public val variable: KaVariableSymbol
    }

    public interface UninitializedParameter : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass: KClass<UninitializedParameter>
            get() = UninitializedParameter::class

        public val parameter: KaSymbol
    }

    public interface UninitializedEnumEntry : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UninitializedEnumEntry>
            get() = UninitializedEnumEntry::class

        public val enumEntry: KaSymbol
    }

    public interface UninitializedEnumCompanion : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UninitializedEnumCompanion>
            get() = UninitializedEnumCompanion::class

        public val enumClass: KaClassLikeSymbol
    }

    public interface ValReassignment : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ValReassignment>
            get() = ValReassignment::class

        public val variable: KaVariableSymbol
    }

    public interface ValReassignmentViaBackingFieldError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ValReassignmentViaBackingFieldError>
            get() = ValReassignmentViaBackingFieldError::class

        public val property: KaVariableSymbol
    }

    public interface CapturedValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CapturedValInitialization>
            get() = CapturedValInitialization::class

        public val property: KaVariableSymbol
    }

    public interface CapturedMemberValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CapturedMemberValInitialization>
            get() = CapturedMemberValInitialization::class

        public val property: KaVariableSymbol
    }

    public interface NonInlineMemberValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NonInlineMemberValInitialization>
            get() = NonInlineMemberValInitialization::class

        public val property: KaVariableSymbol
    }

    public interface SetterProjectedOut : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<SetterProjectedOut>
            get() = SetterProjectedOut::class

        public val receiverType: KaType
        public val projection: String
        public val property: KaVariableSymbol
    }

    public interface WrongInvocationKind : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongInvocationKind>
            get() = WrongInvocationKind::class

        public val declaration: KaSymbol
        public val requiredRange: EventOccurrencesRange
        public val actualRange: EventOccurrencesRange
    }

    public interface LeakedInPlaceLambda : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LeakedInPlaceLambda>
            get() = LeakedInPlaceLambda::class

        public val lambda: KaSymbol
    }

    public interface VariableWithNoTypeNoInitializer : KaFirDiagnostic<KtVariableDeclaration> {
        override val diagnosticClass: KClass<VariableWithNoTypeNoInitializer>
            get() = VariableWithNoTypeNoInitializer::class
    }

    public interface InitializationBeforeDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<InitializationBeforeDeclaration>
            get() = InitializationBeforeDeclaration::class

        public val property: KaSymbol
    }

    public interface InitializationBeforeDeclarationWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<InitializationBeforeDeclarationWarning>
            get() = InitializationBeforeDeclarationWarning::class

        public val property: KaSymbol
    }

    public interface UnreachableCode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnreachableCode>
            get() = UnreachableCode::class

        public val reachable: List<PsiElement>
        public val unreachable: List<PsiElement>
    }

    public interface SenselessComparison : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<SenselessComparison>
            get() = SenselessComparison::class

        public val compareResult: Boolean
    }

    public interface SenselessNullInWhen : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SenselessNullInWhen>
            get() = SenselessNullInWhen::class
    }

    public interface TypecheckerHasRunIntoRecursiveProblem : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<TypecheckerHasRunIntoRecursiveProblem>
            get() = TypecheckerHasRunIntoRecursiveProblem::class
    }

    public interface ReturnValueNotUsed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ReturnValueNotUsed>
            get() = ReturnValueNotUsed::class

        public val functionName: Name?
    }

    public interface ReturnValueNotUsedCoercion : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ReturnValueNotUsedCoercion>
            get() = ReturnValueNotUsedCoercion::class

        public val functionName: Name?
    }

    public interface NullForNonnullType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NullForNonnullType>
            get() = NullForNonnullType::class

        public val expectedType: KaType
    }

    public interface UnsafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsafeCall>
            get() = UnsafeCall::class

        public val receiverType: KaType
        public val receiverExpression: KtExpression?
    }

    public interface UnsafeImplicitInvokeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsafeImplicitInvokeCall>
            get() = UnsafeImplicitInvokeCall::class

        public val receiverType: KaType
    }

    public interface UnsafeInfixCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UnsafeInfixCall>
            get() = UnsafeInfixCall::class

        public val receiverType: KaType
        public val receiverExpression: KtExpression
        public val operator: String
        public val argumentExpression: KtExpression?
    }

    public interface UnsafeOperatorCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UnsafeOperatorCall>
            get() = UnsafeOperatorCall::class

        public val receiverType: KaType
        public val receiverExpression: KtExpression
        public val operator: String
        public val argumentExpression: KtExpression?
    }

    public interface UnsafeCallableReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsafeCallableReference>
            get() = UnsafeCallableReference::class

        public val receiverType: KaType
    }

    public interface IteratorOnNullable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<IteratorOnNullable>
            get() = IteratorOnNullable::class
    }

    public interface ComponentFunctionOnNullable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ComponentFunctionOnNullable>
            get() = ComponentFunctionOnNullable::class

        public val componentFunctionName: Name
        public val destructingType: KaType
    }

    public interface UnexpectedSafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnexpectedSafeCall>
            get() = UnexpectedSafeCall::class
    }

    public interface UnnecessarySafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnnecessarySafeCall>
            get() = UnnecessarySafeCall::class

        public val receiverType: KaType
    }

    public interface UnnecessaryNotNullAssertion : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UnnecessaryNotNullAssertion>
            get() = UnnecessaryNotNullAssertion::class

        public val receiverType: KaType
    }

    public interface NotNullAssertionOnLambdaExpression : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NotNullAssertionOnLambdaExpression>
            get() = NotNullAssertionOnLambdaExpression::class
    }

    public interface NotNullAssertionOnCallableReference : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NotNullAssertionOnCallableReference>
            get() = NotNullAssertionOnCallableReference::class
    }

    public interface UselessElvis : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<UselessElvis>
            get() = UselessElvis::class

        public val receiverType: KaType
    }

    public interface UselessElvisRightIsNull : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<UselessElvisRightIsNull>
            get() = UselessElvisRightIsNull::class
    }

    public interface UselessElvisLeftIsNull : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<UselessElvisLeftIsNull>
            get() = UselessElvisLeftIsNull::class
    }

    public interface CannotCheckForErased : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CannotCheckForErased>
            get() = CannotCheckForErased::class

        public val type: KaType
    }

    public interface UnsafeCastRelyingOnNull : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<UnsafeCastRelyingOnNull>
            get() = UnsafeCastRelyingOnNull::class
    }

    public interface SafeCastRelyingOnNull : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<SafeCastRelyingOnNull>
            get() = SafeCastRelyingOnNull::class
    }

    public interface CastNeverSucceeds : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<CastNeverSucceeds>
            get() = CastNeverSucceeds::class
    }

    public interface UselessCast : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<UselessCast>
            get() = UselessCast::class
    }

    public interface UncheckedCast : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<UncheckedCast>
            get() = UncheckedCast::class

        public val originalType: KaType
        public val targetType: KaType
    }

    public interface NumericCastNeverSucceedsButCanBeReplacedWithToCall : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<NumericCastNeverSucceedsButCanBeReplacedWithToCall>
            get() = NumericCastNeverSucceedsButCanBeReplacedWithToCall::class

        public val targetType: KaType
    }

    public interface IntegerLiteralCastInsteadOfToCall : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<IntegerLiteralCastInsteadOfToCall>
            get() = IntegerLiteralCastInsteadOfToCall::class

        public val targetType: KaType
    }

    public interface ImpossibleIsCheckError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckError>
            get() = ImpossibleIsCheckError::class

        public val compileTimeCheckResult: Boolean
    }

    public interface ImpossibleIsCheckWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckWarning>
            get() = ImpossibleIsCheckWarning::class

        public val compileTimeCheckResult: Boolean
    }

    public interface ImpossibleIsCheckDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckDeprecationError>
            get() = ImpossibleIsCheckDeprecationError::class

        public val compileTimeCheckResult: Boolean
    }

    public interface ImpossibleIsCheckDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckDeprecationWarning>
            get() = ImpossibleIsCheckDeprecationWarning::class

        public val compileTimeCheckResult: Boolean
    }

    public interface ImpossibleIsCheckRelyingOnNullError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckRelyingOnNullError>
            get() = ImpossibleIsCheckRelyingOnNullError::class

        public val compileTimeCheckResult: Boolean
    }

    public interface ImpossibleIsCheckRelyingOnNullWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckRelyingOnNullWarning>
            get() = ImpossibleIsCheckRelyingOnNullWarning::class

        public val compileTimeCheckResult: Boolean
    }

    public interface ImpossibleIsCheckRelyingOnNullDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckRelyingOnNullDeprecationError>
            get() = ImpossibleIsCheckRelyingOnNullDeprecationError::class

        public val compileTimeCheckResult: Boolean
    }

    public interface ImpossibleIsCheckRelyingOnNullDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckRelyingOnNullDeprecationWarning>
            get() = ImpossibleIsCheckRelyingOnNullDeprecationWarning::class

        public val compileTimeCheckResult: Boolean
    }

    public interface UselessIsCheck : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UselessIsCheck>
            get() = UselessIsCheck::class

        public val compileTimeCheckResult: Boolean
    }

    public interface IsEnumEntry : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IsEnumEntry>
            get() = IsEnumEntry::class
    }

    public interface DynamicNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DynamicNotAllowed>
            get() = DynamicNotAllowed::class
    }

    public interface EnumEntryAsType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<EnumEntryAsType>
            get() = EnumEntryAsType::class
    }

    public interface ExpectedCondition : KaFirDiagnostic<KtWhenCondition> {
        override val diagnosticClass: KClass<ExpectedCondition>
            get() = ExpectedCondition::class
    }

    public interface NoElseInWhen : KaFirDiagnostic<KtWhenExpression> {
        override val diagnosticClass: KClass<NoElseInWhen>
            get() = NoElseInWhen::class

        public val missingWhenCases: List<KaWhenMissingCase>
        public val description: String
    }

    public interface MissingBranchForNonAbstractSealedClass : KaFirDiagnostic<KtWhenExpression> {
        override val diagnosticClass: KClass<MissingBranchForNonAbstractSealedClass>
            get() = MissingBranchForNonAbstractSealedClass::class

        public val missingWhenCases: List<KaWhenMissingCase>
    }

    public interface InvalidIfAsExpression : KaFirDiagnostic<KtIfExpression> {
        override val diagnosticClass: KClass<InvalidIfAsExpression>
            get() = InvalidIfAsExpression::class
    }

    public interface ElseMisplacedInWhen : KaFirDiagnostic<KtWhenEntry> {
        override val diagnosticClass: KClass<ElseMisplacedInWhen>
            get() = ElseMisplacedInWhen::class
    }

    public interface RedundantElseInWhen : KaFirDiagnostic<KtWhenEntry> {
        override val diagnosticClass: KClass<RedundantElseInWhen>
            get() = RedundantElseInWhen::class
    }

    public interface IllegalDeclarationInWhenSubject : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IllegalDeclarationInWhenSubject>
            get() = IllegalDeclarationInWhenSubject::class

        public val illegalReason: String
    }

    public interface CommaInWhenConditionWithoutArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CommaInWhenConditionWithoutArgument>
            get() = CommaInWhenConditionWithoutArgument::class
    }

    public interface DuplicateBranchConditionInWhen : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DuplicateBranchConditionInWhen>
            get() = DuplicateBranchConditionInWhen::class
    }

    public interface ConfusingBranchConditionError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConfusingBranchConditionError>
            get() = ConfusingBranchConditionError::class
    }

    public interface WrongConditionSuggestGuard : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongConditionSuggestGuard>
            get() = WrongConditionSuggestGuard::class
    }

    public interface CommaInWhenConditionWithWhenGuard : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CommaInWhenConditionWithWhenGuard>
            get() = CommaInWhenConditionWithWhenGuard::class
    }

    public interface WhenGuardWithoutSubject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WhenGuardWithoutSubject>
            get() = WhenGuardWithoutSubject::class
    }

    public interface InferredInvisibleWhenTypeWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InferredInvisibleWhenTypeWarning>
            get() = InferredInvisibleWhenTypeWarning::class

        public val whenType: KaType
        public val syntaxConstructionName: String
    }

    public interface TypeParameterIsNotAnExpression : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass: KClass<TypeParameterIsNotAnExpression>
            get() = TypeParameterIsNotAnExpression::class

        public val typeParameter: KaTypeParameterSymbol
    }

    public interface TypeParameterOnLhsOfDot : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass: KClass<TypeParameterOnLhsOfDot>
            get() = TypeParameterOnLhsOfDot::class

        public val typeParameter: KaTypeParameterSymbol
    }

    public interface NoCompanionObject : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NoCompanionObject>
            get() = NoCompanionObject::class

        public val klass: KaClassLikeSymbol
    }

    public interface ExpressionExpectedPackageFound : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExpressionExpectedPackageFound>
            get() = ExpressionExpectedPackageFound::class
    }

    public interface ErrorInContractDescription : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ErrorInContractDescription>
            get() = ErrorInContractDescription::class

        public val reason: String
    }

    public interface ContractNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContractNotAllowed>
            get() = ContractNotAllowed::class

        public val reason: String
    }

    public interface NoGetMethod : KaFirDiagnostic<KtArrayAccessExpression> {
        override val diagnosticClass: KClass<NoGetMethod>
            get() = NoGetMethod::class
    }

    public interface NoSetMethod : KaFirDiagnostic<KtArrayAccessExpression> {
        override val diagnosticClass: KClass<NoSetMethod>
            get() = NoSetMethod::class
    }

    public interface IteratorMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<IteratorMissing>
            get() = IteratorMissing::class
    }

    public interface HasNextMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<HasNextMissing>
            get() = HasNextMissing::class
    }

    public interface NextMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NextMissing>
            get() = NextMissing::class
    }

    public interface ComponentFunctionMissing : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ComponentFunctionMissing>
            get() = ComponentFunctionMissing::class

        public val missingFunctionName: Name
        public val destructingType: KaType
    }

    public interface DelegateSpecialFunctionMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegateSpecialFunctionMissing>
            get() = DelegateSpecialFunctionMissing::class

        public val expectedFunctionSignature: String
        public val delegateType: KaType
        public val description: String
    }

    public interface UnderscoreIsReserved : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnderscoreIsReserved>
            get() = UnderscoreIsReserved::class
    }

    public interface UnderscoreUsageWithoutBackticks : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnderscoreUsageWithoutBackticks>
            get() = UnderscoreUsageWithoutBackticks::class
    }

    public interface ResolvedToUnderscoreNamedCatchParameter : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass: KClass<ResolvedToUnderscoreNamedCatchParameter>
            get() = ResolvedToUnderscoreNamedCatchParameter::class
    }

    public interface InvalidCharacters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidCharacters>
            get() = InvalidCharacters::class

        public val message: String
    }

    public interface EqualityNotApplicable : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<EqualityNotApplicable>
            get() = EqualityNotApplicable::class

        public val operator: String
        public val leftType: KaType
        public val rightType: KaType
    }

    public interface EqualityNotApplicableWarning : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<EqualityNotApplicableWarning>
            get() = EqualityNotApplicableWarning::class

        public val operator: String
        public val leftType: KaType
        public val rightType: KaType
    }

    public interface IncompatibleEnumComparisonError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncompatibleEnumComparisonError>
            get() = IncompatibleEnumComparisonError::class

        public val leftType: KaType
        public val rightType: KaType
    }

    public interface IncompatibleEnumComparison : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncompatibleEnumComparison>
            get() = IncompatibleEnumComparison::class

        public val leftType: KaType
        public val rightType: KaType
    }

    public interface ForbiddenIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ForbiddenIdentityEquals>
            get() = ForbiddenIdentityEquals::class

        public val leftType: KaType
        public val rightType: KaType
    }

    public interface ForbiddenIdentityEqualsWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ForbiddenIdentityEqualsWarning>
            get() = ForbiddenIdentityEqualsWarning::class

        public val leftType: KaType
        public val rightType: KaType
    }

    public interface DeprecatedIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DeprecatedIdentityEquals>
            get() = DeprecatedIdentityEquals::class

        public val leftType: KaType
        public val rightType: KaType
    }

    public interface ImplicitBoxingInIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImplicitBoxingInIdentityEquals>
            get() = ImplicitBoxingInIdentityEquals::class

        public val leftType: KaType
        public val rightType: KaType
    }

    public interface IncDecShouldNotReturnUnit : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<IncDecShouldNotReturnUnit>
            get() = IncDecShouldNotReturnUnit::class
    }

    public interface AssignmentOperatorShouldReturnUnit : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssignmentOperatorShouldReturnUnit>
            get() = AssignmentOperatorShouldReturnUnit::class

        public val functionSymbol: KaFunctionSymbol
        public val operator: String
    }

    public interface InitializerRequiredForDestructuringDeclaration : KaFirDiagnostic<KtDestructuringDeclaration> {
        override val diagnosticClass: KClass<InitializerRequiredForDestructuringDeclaration>
            get() = InitializerRequiredForDestructuringDeclaration::class
    }

    public interface NotFunctionAsOperator : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotFunctionAsOperator>
            get() = NotFunctionAsOperator::class

        public val elementName: String
        public val elementSymbol: KaSymbol
    }

    public interface DslScopeViolation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DslScopeViolation>
            get() = DslScopeViolation::class

        public val calleeSymbol: KaSymbol
    }

    public interface ReceiverShadowedByContextParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ReceiverShadowedByContextParameter>
            get() = ReceiverShadowedByContextParameter::class

        public val calleeSymbol: KaSymbol
        public val isDispatchOfMemberExtension: Boolean
        public val contextParameterSymbols: List<KaSymbol>
    }

    public interface RecursiveTypealiasExpansion : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RecursiveTypealiasExpansion>
            get() = RecursiveTypealiasExpansion::class
    }

    public interface TypealiasShouldExpandToClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasShouldExpandToClass>
            get() = TypealiasShouldExpandToClass::class

        public val expandedType: KaType
    }

    public interface ConstructorOrSupertypeOnTypealiasWithTypeProjectionError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConstructorOrSupertypeOnTypealiasWithTypeProjectionError>
            get() = ConstructorOrSupertypeOnTypealiasWithTypeProjectionError::class
    }

    public interface ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning>
            get() = ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning::class
    }

    public interface TypealiasExpansionCapturesOuterTypeParameters : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasExpansionCapturesOuterTypeParameters>
            get() = TypealiasExpansionCapturesOuterTypeParameters::class

        public val outerTypeParameters: List<KaTypeParameterSymbol>
    }

    public interface TypealiasExpandsToCompilerRequiredAnnotationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasExpandsToCompilerRequiredAnnotationError>
            get() = TypealiasExpandsToCompilerRequiredAnnotationError::class

        public val annotation: KaClassLikeSymbol
    }

    public interface TypealiasExpandsToCompilerRequiredAnnotationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasExpandsToCompilerRequiredAnnotationWarning>
            get() = TypealiasExpandsToCompilerRequiredAnnotationWarning::class

        public val annotation: KaClassLikeSymbol
    }

    public interface ExpectedTypealias : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExpectedTypealias>
            get() = ExpectedTypealias::class
    }

    public interface RedundantVisibilityModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<RedundantVisibilityModifier>
            get() = RedundantVisibilityModifier::class
    }

    public interface RedundantModalityModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<RedundantModalityModifier>
            get() = RedundantModalityModifier::class
    }

    public interface RedundantReturnUnitType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RedundantReturnUnitType>
            get() = RedundantReturnUnitType::class
    }

    public interface RedundantSingleExpressionStringTemplate : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantSingleExpressionStringTemplate>
            get() = RedundantSingleExpressionStringTemplate::class
    }

    public interface CanBeVal : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CanBeVal>
            get() = CanBeVal::class
    }

    public interface CanBeValLateinit : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CanBeValLateinit>
            get() = CanBeValLateinit::class
    }

    public interface CanBeValDelayedInitialization : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CanBeValDelayedInitialization>
            get() = CanBeValDelayedInitialization::class
    }

    public interface RedundantCallOfConversionMethod : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantCallOfConversionMethod>
            get() = RedundantCallOfConversionMethod::class
    }

    public interface ArrayEqualityOperatorCanBeReplacedWithContentEquals : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ArrayEqualityOperatorCanBeReplacedWithContentEquals>
            get() = ArrayEqualityOperatorCanBeReplacedWithContentEquals::class

        public val operator: String
        public val replacementPrefix: String
    }

    public interface EmptyRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<EmptyRange>
            get() = EmptyRange::class
    }

    public interface RedundantSetterParameterType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantSetterParameterType>
            get() = RedundantSetterParameterType::class
    }

    public interface UnusedVariable : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<UnusedVariable>
            get() = UnusedVariable::class
    }

    public interface AssignedValueIsNeverRead : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AssignedValueIsNeverRead>
            get() = AssignedValueIsNeverRead::class
    }

    public interface VariableInitializerIsRedundant : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VariableInitializerIsRedundant>
            get() = VariableInitializerIsRedundant::class
    }

    public interface VariableNeverRead : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VariableNeverRead>
            get() = VariableNeverRead::class
    }

    public interface UselessCallOnNotNull : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UselessCallOnNotNull>
            get() = UselessCallOnNotNull::class
    }

    public interface UnusedAnonymousParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnusedAnonymousParameter>
            get() = UnusedAnonymousParameter::class

        public val parameter: KaSymbol
    }

    public interface UnusedExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnusedExpression>
            get() = UnusedExpression::class
    }

    public interface UnusedLambdaExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnusedLambdaExpression>
            get() = UnusedLambdaExpression::class
    }

    public interface ReturnNotAllowed : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnNotAllowed>
            get() = ReturnNotAllowed::class
    }

    public interface NotAFunctionLabel : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<NotAFunctionLabel>
            get() = NotAFunctionLabel::class
    }

    public interface ReturnInFunctionWithExpressionBody : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnInFunctionWithExpressionBody>
            get() = ReturnInFunctionWithExpressionBody::class
    }

    public interface ReturnInFunctionWithExpressionBodyWarning : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnInFunctionWithExpressionBodyWarning>
            get() = ReturnInFunctionWithExpressionBodyWarning::class
    }

    public interface ReturnInFunctionWithExpressionBodyAndImplicitType : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnInFunctionWithExpressionBodyAndImplicitType>
            get() = ReturnInFunctionWithExpressionBodyAndImplicitType::class
    }

    public interface NoReturnInFunctionWithBlockBody : KaFirDiagnostic<KtDeclarationWithBody> {
        override val diagnosticClass: KClass<NoReturnInFunctionWithBlockBody>
            get() = NoReturnInFunctionWithBlockBody::class
    }

    public interface RedundantReturn : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<RedundantReturn>
            get() = RedundantReturn::class
    }

    public interface AnonymousInitializerInInterface : KaFirDiagnostic<KtAnonymousInitializer> {
        override val diagnosticClass: KClass<AnonymousInitializerInInterface>
            get() = AnonymousInitializerInInterface::class
    }

    public interface UsageIsNotInlinable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UsageIsNotInlinable>
            get() = UsageIsNotInlinable::class

        public val parameter: KaSymbol
    }

    public interface NonLocalReturnNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonLocalReturnNotAllowed>
            get() = NonLocalReturnNotAllowed::class

        public val parameter: KaSymbol
    }

    public interface NotYetSupportedInInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NotYetSupportedInInline>
            get() = NotYetSupportedInInline::class

        public val message: String
    }

    public interface NotYetSupportedInInlineWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NotYetSupportedInInlineWarning>
            get() = NotYetSupportedInInlineWarning::class
    }

    public interface NothingToInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NothingToInline>
            get() = NothingToInline::class
    }

    public interface NullableInlineParameter : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NullableInlineParameter>
            get() = NullableInlineParameter::class

        public val parameter: KaSymbol
        public val function: KaSymbol
    }

    public interface RecursionInInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RecursionInInline>
            get() = RecursionInInline::class

        public val symbol: KaSymbol
    }

    public interface NonPublicCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicCallFromPublicInline>
            get() = NonPublicCallFromPublicInline::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    public interface NonPublicInlineCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicInlineCallFromPublicInline>
            get() = NonPublicInlineCallFromPublicInline::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    public interface NonPublicCallFromPublicInlineDeprecation : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicCallFromPublicInlineDeprecation>
            get() = NonPublicCallFromPublicInlineDeprecation::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    public interface NonPublicDataCopyCallFromPublicInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicDataCopyCallFromPublicInlineError>
            get() = NonPublicDataCopyCallFromPublicInlineError::class

        public val inlineDeclaration: KaSymbol
    }

    public interface NonPublicDataCopyCallFromPublicInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicDataCopyCallFromPublicInlineWarning>
            get() = NonPublicDataCopyCallFromPublicInlineWarning::class

        public val inlineDeclaration: KaSymbol
    }

    public interface ProtectedConstructorCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ProtectedConstructorCallFromPublicInline>
            get() = ProtectedConstructorCallFromPublicInline::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    public interface ProtectedCallFromPublicInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ProtectedCallFromPublicInlineError>
            get() = ProtectedCallFromPublicInlineError::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    public interface PrivateClassMemberFromInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<PrivateClassMemberFromInline>
            get() = PrivateClassMemberFromInline::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    public interface SuperCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SuperCallFromPublicInline>
            get() = SuperCallFromPublicInline::class

        public val symbol: KaSymbol
    }

    public interface DeclarationCantBeInlined : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<DeclarationCantBeInlined>
            get() = DeclarationCantBeInlined::class
    }

    public interface DeclarationCantBeInlinedDeprecationError : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<DeclarationCantBeInlinedDeprecationError>
            get() = DeclarationCantBeInlinedDeprecationError::class
    }

    public interface DeclarationCantBeInlinedDeprecationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<DeclarationCantBeInlinedDeprecationWarning>
            get() = DeclarationCantBeInlinedDeprecationWarning::class
    }

    public interface OverrideByInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<OverrideByInline>
            get() = OverrideByInline::class
    }

    public interface InvalidDefaultFunctionalParameterForInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InvalidDefaultFunctionalParameterForInline>
            get() = InvalidDefaultFunctionalParameterForInline::class

        public val parameter: KaSymbol
    }

    public interface NotSupportedInlineParameterInInlineParameterDefaultValue : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NotSupportedInlineParameterInInlineParameterDefaultValue>
            get() = NotSupportedInlineParameterInInlineParameterDefaultValue::class

        public val parameter: KaSymbol
    }

    public interface ReifiedTypeParameterInOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ReifiedTypeParameterInOverride>
            get() = ReifiedTypeParameterInOverride::class
    }

    public interface InlinePropertyWithBackingField : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InlinePropertyWithBackingField>
            get() = InlinePropertyWithBackingField::class
    }

    public interface InlinePropertyWithBackingFieldDeprecationError : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InlinePropertyWithBackingFieldDeprecationError>
            get() = InlinePropertyWithBackingFieldDeprecationError::class
    }

    public interface InlinePropertyWithBackingFieldDeprecationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InlinePropertyWithBackingFieldDeprecationWarning>
            get() = InlinePropertyWithBackingFieldDeprecationWarning::class
    }

    public interface IllegalInlineParameterModifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IllegalInlineParameterModifier>
            get() = IllegalInlineParameterModifier::class
    }

    public interface InlineSuspendFunctionTypeUnsupported : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<InlineSuspendFunctionTypeUnsupported>
            get() = InlineSuspendFunctionTypeUnsupported::class
    }

    public interface InefficientEqualsOverridingInValueClass : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InefficientEqualsOverridingInValueClass>
            get() = InefficientEqualsOverridingInValueClass::class

        public val type: KaType
    }

    public interface InlineClassDeprecated : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InlineClassDeprecated>
            get() = InlineClassDeprecated::class
    }

    public interface LessVisibleTypeAccessInInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<LessVisibleTypeAccessInInlineError>
            get() = LessVisibleTypeAccessInInlineError::class

        public val typeVisibility: EffectiveVisibility
        public val type: KaType
        public val inlineVisibility: EffectiveVisibility
    }

    public interface LessVisibleTypeAccessInInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<LessVisibleTypeAccessInInlineWarning>
            get() = LessVisibleTypeAccessInInlineWarning::class

        public val typeVisibility: EffectiveVisibility
        public val type: KaType
        public val inlineVisibility: EffectiveVisibility
    }

    public interface LessVisibleTypeInInlineAccessedSignatureError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<LessVisibleTypeInInlineAccessedSignatureError>
            get() = LessVisibleTypeInInlineAccessedSignatureError::class

        public val symbol: KaSymbol
        public val typeVisibility: EffectiveVisibility
        public val type: KaType
        public val inlineVisibility: EffectiveVisibility
    }

    public interface LessVisibleTypeInInlineAccessedSignatureWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<LessVisibleTypeInInlineAccessedSignatureWarning>
            get() = LessVisibleTypeInInlineAccessedSignatureWarning::class

        public val symbol: KaSymbol
        public val typeVisibility: EffectiveVisibility
        public val type: KaType
        public val inlineVisibility: EffectiveVisibility
    }

    public interface CallableReferenceToLessVisibleDeclarationInInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallableReferenceToLessVisibleDeclarationInInlineError>
            get() = CallableReferenceToLessVisibleDeclarationInInlineError::class

        public val symbol: KaSymbol
        public val visibility: EffectiveVisibility
        public val inlineVisibility: EffectiveVisibility
    }

    public interface CallableReferenceToLessVisibleDeclarationInInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallableReferenceToLessVisibleDeclarationInInlineWarning>
            get() = CallableReferenceToLessVisibleDeclarationInInlineWarning::class

        public val symbol: KaSymbol
        public val visibility: EffectiveVisibility
        public val inlineVisibility: EffectiveVisibility
    }

    public interface ContextParameterMustBeNoinline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ContextParameterMustBeNoinline>
            get() = ContextParameterMustBeNoinline::class

        public val parameter: KaSymbol
        public val function: KaSymbol
    }

    public interface InlineFromHigherPlatform : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InlineFromHigherPlatform>
            get() = InlineFromHigherPlatform::class

        public val inlinedBytecodeVersion: String
        public val currentModuleBytecodeVersion: String
    }

    public interface CannotAllUnderImportFromSingleton : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<CannotAllUnderImportFromSingleton>
            get() = CannotAllUnderImportFromSingleton::class

        public val objectName: Name
    }

    public interface PackageCannotBeImported : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<PackageCannotBeImported>
            get() = PackageCannotBeImported::class
    }

    public interface CannotBeImported : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<CannotBeImported>
            get() = CannotBeImported::class

        public val name: Name
    }

    public interface ConflictingImport : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<ConflictingImport>
            get() = ConflictingImport::class

        public val name: Name
    }

    public interface FunctionTypeOfTooLargeArity : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<FunctionTypeOfTooLargeArity>
            get() = FunctionTypeOfTooLargeArity::class

        public val classId: ClassId
        public val maxArity: Int
    }

    public interface KSuspendFunctionTypeOfDangerouslyLargeArity : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<KSuspendFunctionTypeOfDangerouslyLargeArity>
            get() = KSuspendFunctionTypeOfDangerouslyLargeArity::class

        public val classId: ClassId
        public val maxArity: Int
    }

    public interface OperatorRenamedOnImport : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<OperatorRenamedOnImport>
            get() = OperatorRenamedOnImport::class
    }

    public interface TypealiasAsCallableQualifierInImportError : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<TypealiasAsCallableQualifierInImportError>
            get() = TypealiasAsCallableQualifierInImportError::class

        public val typealiasName: Name
        public val originalClassName: Name
    }

    public interface TypealiasAsCallableQualifierInImportWarning : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<TypealiasAsCallableQualifierInImportWarning>
            get() = TypealiasAsCallableQualifierInImportWarning::class

        public val typealiasName: Name
        public val originalClassName: Name
    }

    public interface IllegalSuspendFunctionCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalSuspendFunctionCall>
            get() = IllegalSuspendFunctionCall::class

        public val suspendCallable: KaSymbol
    }

    public interface IllegalSuspendPropertyAccess : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalSuspendPropertyAccess>
            get() = IllegalSuspendPropertyAccess::class

        public val suspendCallable: KaSymbol
    }

    public interface NonLocalSuspensionPoint : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonLocalSuspensionPoint>
            get() = NonLocalSuspensionPoint::class
    }

    public interface IllegalRestrictedSuspendingFunctionCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalRestrictedSuspendingFunctionCall>
            get() = IllegalRestrictedSuspendingFunctionCall::class
    }

    public interface NonModifierFormForBuiltInSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonModifierFormForBuiltInSuspend>
            get() = NonModifierFormForBuiltInSuspend::class
    }

    public interface ModifierFormForNonBuiltInSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ModifierFormForNonBuiltInSuspend>
            get() = ModifierFormForNonBuiltInSuspend::class
    }

    public interface ModifierFormForNonBuiltInSuspendFunError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ModifierFormForNonBuiltInSuspendFunError>
            get() = ModifierFormForNonBuiltInSuspendFunError::class
    }

    public interface ReturnForBuiltInSuspend : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnForBuiltInSuspend>
            get() = ReturnForBuiltInSuspend::class
    }

    public interface MixingSuspendAndNonSuspendSupertypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MixingSuspendAndNonSuspendSupertypes>
            get() = MixingSuspendAndNonSuspendSupertypes::class
    }

    public interface MixingFunctionalKindsInSupertypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MixingFunctionalKindsInSupertypes>
            get() = MixingFunctionalKindsInSupertypes::class

        public val kinds: List<FunctionTypeKind>
    }

    public interface RedundantLabelWarning : KaFirDiagnostic<KtLabelReferenceExpression> {
        override val diagnosticClass: KClass<RedundantLabelWarning>
            get() = RedundantLabelWarning::class
    }

    public interface MultipleLabelsAreForbidden : KaFirDiagnostic<KtLabelReferenceExpression> {
        override val diagnosticClass: KClass<MultipleLabelsAreForbidden>
            get() = MultipleLabelsAreForbidden::class
    }

    public interface DeprecatedAccessToEnumEntryCompanionProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEnumEntryCompanionProperty>
            get() = DeprecatedAccessToEnumEntryCompanionProperty::class
    }

    public interface DeprecatedAccessToEntryPropertyFromEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEntryPropertyFromEnum>
            get() = DeprecatedAccessToEntryPropertyFromEnum::class
    }

    public interface DeprecatedAccessToEntriesProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEntriesProperty>
            get() = DeprecatedAccessToEntriesProperty::class
    }

    public interface DeprecatedAccessToEnumEntryPropertyAsReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEnumEntryPropertyAsReference>
            get() = DeprecatedAccessToEnumEntryPropertyAsReference::class
    }

    public interface DeprecatedAccessToEntriesAsQualifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEntriesAsQualifier>
            get() = DeprecatedAccessToEntriesAsQualifier::class
    }

    public interface DeclarationOfEnumEntryEntriesError : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass: KClass<DeclarationOfEnumEntryEntriesError>
            get() = DeclarationOfEnumEntryEntriesError::class
    }

    public interface DeclarationOfEnumEntryEntriesWarning : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass: KClass<DeclarationOfEnumEntryEntriesWarning>
            get() = DeclarationOfEnumEntryEntriesWarning::class
    }

    public interface IncompatibleClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IncompatibleClass>
            get() = IncompatibleClass::class

        public val presentableString: String
        public val incompatibility: IncompatibleVersionErrorData<*>
    }

    public interface PreReleaseClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PreReleaseClass>
            get() = PreReleaseClass::class

        public val presentableString: String
        public val poisoningFeatures: List<String>
    }

    public interface IrWithUnstableAbiCompiledClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IrWithUnstableAbiCompiledClass>
            get() = IrWithUnstableAbiCompiledClass::class

        public val presentableString: String
    }

    public interface BuilderInferenceStubReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<BuilderInferenceStubReceiver>
            get() = BuilderInferenceStubReceiver::class

        public val typeParameterName: Name
        public val containingDeclarationName: Name
    }

    public interface BuilderInferenceMultiLambdaRestriction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<BuilderInferenceMultiLambdaRestriction>
            get() = BuilderInferenceMultiLambdaRestriction::class

        public val typeParameterName: Name
        public val containingDeclarationName: Name
    }

    public interface InvalidVersioningOnNonOptional : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnNonOptional>
            get() = InvalidVersioningOnNonOptional::class
    }

    public interface InvalidVersioningOnNonfinalClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnNonfinalClass>
            get() = InvalidVersioningOnNonfinalClass::class
    }

    public interface InvalidVersioningOnLocalFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnLocalFunction>
            get() = InvalidVersioningOnLocalFunction::class
    }

    public interface InvalidVersioningOnAnnotationClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnAnnotationClass>
            get() = InvalidVersioningOnAnnotationClass::class
    }

    public interface InvalidDefaultValueDependency : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidDefaultValueDependency>
            get() = InvalidDefaultValueDependency::class

        public val lowestVersion: MavenComparableVersion?
        public val highestVersion: MavenComparableVersion?
    }

    public interface InvalidNonOptionalParameterPosition : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidNonOptionalParameterPosition>
            get() = InvalidNonOptionalParameterPosition::class
    }

    public interface InvalidVersioningOnReceiverOrContextParameterPosition : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnReceiverOrContextParameterPosition>
            get() = InvalidVersioningOnReceiverOrContextParameterPosition::class
    }

    public interface InvalidVersioningOnVararg : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnVararg>
            get() = InvalidVersioningOnVararg::class
    }

    public interface InvalidVersioningOnValueClassParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnValueClassParameter>
            get() = InvalidVersioningOnValueClassParameter::class
    }

    public interface NonAscendingVersionAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonAscendingVersionAnnotation>
            get() = NonAscendingVersionAnnotation::class

        public val lowestVersion: MavenComparableVersion?
        public val highestVersion: MavenComparableVersion?
        public val sourceOfHighestVersion: KaCallableSymbol
    }

    public interface CompanionBlockMemberExtension : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionBlockMemberExtension>
            get() = CompanionBlockMemberExtension::class
    }

    public interface PrivateConstInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PrivateConstInInterface>
            get() = PrivateConstInInterface::class
    }

    public interface IllegalCompanionBlock : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalCompanionBlock>
            get() = IllegalCompanionBlock::class

        public val parent: KaSymbol
    }

    public interface CompanionBlockNested : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionBlockNested>
            get() = CompanionBlockNested::class
    }

    public interface IllegalCompanionBlockMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalCompanionBlockMember>
            get() = IllegalCompanionBlockMember::class

        public val symbol: KaSymbol
    }

    public interface CompanionExtensionReceiverWithTypeArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionReceiverWithTypeArguments>
            get() = CompanionExtensionReceiverWithTypeArguments::class

        public val type: KaType
    }

    public interface CompanionExtensionReceiverIsObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionReceiverIsObject>
            get() = CompanionExtensionReceiverIsObject::class

        public val type: KaType
    }

    public interface CompanionExtensionReceiverIsTypeParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionReceiverIsTypeParameter>
            get() = CompanionExtensionReceiverIsTypeParameter::class

        public val type: KaType
    }

    public interface CompanionExtensionReceiverAnnotated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionReceiverAnnotated>
            get() = CompanionExtensionReceiverAnnotated::class
    }

    public interface CompanionExtensionNullableReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionNullableReceiver>
            get() = CompanionExtensionNullableReceiver::class
    }

    public interface OverrideCannotBeStatic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OverrideCannotBeStatic>
            get() = OverrideCannotBeStatic::class
    }

    public interface JvmStaticNotInObjectOrClassCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticNotInObjectOrClassCompanion>
            get() = JvmStaticNotInObjectOrClassCompanion::class
    }

    public interface JvmStaticNotInObjectOrCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticNotInObjectOrCompanion>
            get() = JvmStaticNotInObjectOrCompanion::class
    }

    public interface JvmStaticOnNonPublicMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticOnNonPublicMember>
            get() = JvmStaticOnNonPublicMember::class
    }

    public interface JvmStaticOnConstOrJvmField : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticOnConstOrJvmField>
            get() = JvmStaticOnConstOrJvmField::class
    }

    public interface JvmStaticOnExternalInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticOnExternalInInterface>
            get() = JvmStaticOnExternalInInterface::class
    }

    public interface InapplicableJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableJvmName>
            get() = InapplicableJvmName::class
    }

    public interface IllegalJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalJvmName>
            get() = IllegalJvmName::class
    }

    public interface FunctionDelegateMemberNameClash : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FunctionDelegateMemberNameClash>
            get() = FunctionDelegateMemberNameClash::class
    }

    public interface ValueClassWithoutJvmInlineAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ValueClassWithoutJvmInlineAnnotation>
            get() = ValueClassWithoutJvmInlineAnnotation::class
    }

    public interface JvmInlineWithoutValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmInlineWithoutValueClass>
            get() = JvmInlineWithoutValueClass::class
    }

    public interface InapplicableJvmExposeBoxedWithName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableJvmExposeBoxedWithName>
            get() = InapplicableJvmExposeBoxedWithName::class
    }

    public interface UselessJvmExposeBoxed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UselessJvmExposeBoxed>
            get() = UselessJvmExposeBoxed::class
    }

    public interface JvmExposeBoxedCannotExposeSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeSuspend>
            get() = JvmExposeBoxedCannotExposeSuspend::class
    }

    public interface JvmExposeBoxedRequiresName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedRequiresName>
            get() = JvmExposeBoxedRequiresName::class
    }

    public interface JvmExposeBoxedCannotBeTheSame : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotBeTheSame>
            get() = JvmExposeBoxedCannotBeTheSame::class
    }

    public interface JvmExposeBoxedCannotBeTheSameAsJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotBeTheSameAsJvmName>
            get() = JvmExposeBoxedCannotBeTheSameAsJvmName::class
    }

    public interface JvmExposeBoxedCannotExposeOpenAbstract : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeOpenAbstract>
            get() = JvmExposeBoxedCannotExposeOpenAbstract::class
    }

    public interface JvmExposeBoxedCannotExposeSynthetic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeSynthetic>
            get() = JvmExposeBoxedCannotExposeSynthetic::class
    }

    public interface JvmExposeBoxedCannotExposeLocals : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeLocals>
            get() = JvmExposeBoxedCannotExposeLocals::class
    }

    public interface JvmExposeBoxedCannotExposeReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeReified>
            get() = JvmExposeBoxedCannotExposeReified::class
    }

    public interface JvmExposeBoxedCannotExposePrivate : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposePrivate>
            get() = JvmExposeBoxedCannotExposePrivate::class
    }

    public interface JvmExposeBoxedCanBeReplacedWithJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCanBeReplacedWithJvmName>
            get() = JvmExposeBoxedCanBeReplacedWithJvmName::class
    }

    public interface WrongTypeForJavaOverride : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongTypeForJavaOverride>
            get() = WrongTypeForJavaOverride::class

        public val override: KaCallableSymbol
        public val base: KaCallableSymbol
    }

    public interface AccidentalOverrideClashByJvmSignature : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<AccidentalOverrideClashByJvmSignature>
            get() = AccidentalOverrideClashByJvmSignature::class

        public val hidden: KaFunctionSymbol
        public val overrideDescription: String
        public val regular: KaFunctionSymbol
    }

    public interface ImplementationByDelegationWithDifferentGenericSignatureError : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass: KClass<ImplementationByDelegationWithDifferentGenericSignatureError>
            get() = ImplementationByDelegationWithDifferentGenericSignatureError::class

        public val base: KaFunctionSymbol
        public val override: KaFunctionSymbol
    }

    public interface ImplementationByDelegationWithDifferentGenericSignatureWarning : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass: KClass<ImplementationByDelegationWithDifferentGenericSignatureWarning>
            get() = ImplementationByDelegationWithDifferentGenericSignatureWarning::class

        public val base: KaFunctionSymbol
        public val override: KaFunctionSymbol
    }

    public interface NotYetSupportedLocalInlineFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NotYetSupportedLocalInlineFunction>
            get() = NotYetSupportedLocalInlineFunction::class
    }

    public interface PropertyHidesJavaField : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass: KClass<PropertyHidesJavaField>
            get() = PropertyHidesJavaField::class

        public val hidden: KaVariableSymbol
    }

    public interface ConflictVersionAndJvmOverloadsAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConflictVersionAndJvmOverloadsAnnotation>
            get() = ConflictVersionAndJvmOverloadsAnnotation::class
    }

    public interface JavaTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<JavaTypeMismatch>
            get() = JavaTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
    }

    public interface ReceiverNullabilityMismatchBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ReceiverNullabilityMismatchBasedOnJavaAnnotations>
            get() = ReceiverNullabilityMismatchBasedOnJavaAnnotations::class

        public val actualType: KaType
        public val expectedType: KaType
        public val messageSuffix: String
    }

    public interface ReceiverMutabilityMismatchBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ReceiverMutabilityMismatchBasedOnJavaAnnotations>
            get() = ReceiverMutabilityMismatchBasedOnJavaAnnotations::class

        public val actualType: KaType
        public val expectedType: ClassId
    }

    public interface TypeMismatchBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeMismatchBasedOnJavaAnnotations>
            get() = TypeMismatchBasedOnJavaAnnotations::class

        public val actualType: KaType
        public val expectedType: KaType
        public val messageSuffix: String
    }

    public interface NullabilityMismatchBasedOnExplicitTypeArgumentsForJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NullabilityMismatchBasedOnExplicitTypeArgumentsForJava>
            get() = NullabilityMismatchBasedOnExplicitTypeArgumentsForJava::class

        public val actualType: KaType
        public val expectedType: KaType
        public val messageSuffix: String
    }

    public interface TypeMismatchWhenFlexibilityChanges : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeMismatchWhenFlexibilityChanges>
            get() = TypeMismatchWhenFlexibilityChanges::class

        public val actualType: KaType
        public val expectedType: KaType
    }

    public interface JavaClassOnCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaClassOnCompanion>
            get() = JavaClassOnCompanion::class

        public val actualType: KaType
        public val expectedType: KaType
    }

    public interface JavaClassPropertyReferenceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaClassPropertyReferenceError>
            get() = JavaClassPropertyReferenceError::class
    }

    public interface JavaClassPropertyReferenceWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaClassPropertyReferenceWarning>
            get() = JavaClassPropertyReferenceWarning::class
    }

    public interface UnexhaustiveWhenBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnexhaustiveWhenBasedOnJavaAnnotations>
            get() = UnexhaustiveWhenBasedOnJavaAnnotations::class

        public val subjectType: KaType
    }

    public interface UpperBoundCannotBeArray : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundCannotBeArray>
            get() = UpperBoundCannotBeArray::class
    }

    public interface UpperBoundViolatedBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedBasedOnJavaAnnotations>
            get() = UpperBoundViolatedBasedOnJavaAnnotations::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    public interface UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations>
            get() = UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    public interface StrictfpOnClass : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<StrictfpOnClass>
            get() = StrictfpOnClass::class
    }

    public interface SynchronizedOnAbstract : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnAbstract>
            get() = SynchronizedOnAbstract::class
    }

    public interface SynchronizedInInterface : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedInInterface>
            get() = SynchronizedInInterface::class
    }

    public interface SynchronizedInAnnotationError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedInAnnotationError>
            get() = SynchronizedInAnnotationError::class
    }

    public interface SynchronizedInAnnotationWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedInAnnotationWarning>
            get() = SynchronizedInAnnotationWarning::class
    }

    public interface SynchronizedOnInline : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnInline>
            get() = SynchronizedOnInline::class
    }

    public interface SynchronizedOnValueClassError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnValueClassError>
            get() = SynchronizedOnValueClassError::class
    }

    public interface SynchronizedOnValueClassWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnValueClassWarning>
            get() = SynchronizedOnValueClassWarning::class
    }

    public interface SynchronizedOnSuspendError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnSuspendError>
            get() = SynchronizedOnSuspendError::class
    }

    public interface OverloadsWithoutDefaultArguments : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsWithoutDefaultArguments>
            get() = OverloadsWithoutDefaultArguments::class
    }

    public interface OverloadsAbstract : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsAbstract>
            get() = OverloadsAbstract::class
    }

    public interface OverloadsInterface : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsInterface>
            get() = OverloadsInterface::class
    }

    public interface OverloadsLocal : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsLocal>
            get() = OverloadsLocal::class
    }

    public interface OverloadsAnnotationClassConstructorError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsAnnotationClassConstructorError>
            get() = OverloadsAnnotationClassConstructorError::class
    }

    public interface OverloadsPrivate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsPrivate>
            get() = OverloadsPrivate::class
    }

    public interface DeprecatedJavaAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DeprecatedJavaAnnotation>
            get() = DeprecatedJavaAnnotation::class

        public val kotlinName: FqName
    }

    public interface JvmPackageNameCannotBeEmpty : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmPackageNameCannotBeEmpty>
            get() = JvmPackageNameCannotBeEmpty::class
    }

    public interface JvmPackageNameMustBeValidName : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmPackageNameMustBeValidName>
            get() = JvmPackageNameMustBeValidName::class
    }

    public interface JvmPackageNameNotSupportedInFilesWithClasses : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmPackageNameNotSupportedInFilesWithClasses>
            get() = JvmPackageNameNotSupportedInFilesWithClasses::class
    }

    public interface PositionedValueArgumentForJavaAnnotation : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<PositionedValueArgumentForJavaAnnotation>
            get() = PositionedValueArgumentForJavaAnnotation::class
    }

    public interface RedundantRepeatableAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RedundantRepeatableAnnotation>
            get() = RedundantRepeatableAnnotation::class

        public val kotlinRepeatable: FqName
        public val javaRepeatable: FqName
    }

    public interface ThrowsInAnnotationError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<ThrowsInAnnotationError>
            get() = ThrowsInAnnotationError::class
    }

    public interface ThrowsInAnnotationWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<ThrowsInAnnotationWarning>
            get() = ThrowsInAnnotationWarning::class
    }

    public interface JvmSerializableLambdaOnInlinedFunctionLiteralsError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmSerializableLambdaOnInlinedFunctionLiteralsError>
            get() = JvmSerializableLambdaOnInlinedFunctionLiteralsError::class
    }

    public interface JvmSerializableLambdaOnInlinedFunctionLiteralsWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmSerializableLambdaOnInlinedFunctionLiteralsWarning>
            get() = JvmSerializableLambdaOnInlinedFunctionLiteralsWarning::class
    }

    public interface IncompatibleAnnotationTargets : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<IncompatibleAnnotationTargets>
            get() = IncompatibleAnnotationTargets::class

        public val missingJavaTargets: List<String>
        public val correspondingKotlinTargets: List<String>
    }

    public interface AnnotationTargetsOnlyInJava : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationTargetsOnlyInJava>
            get() = AnnotationTargetsOnlyInJava::class
    }

    public interface LocalJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LocalJvmRecord>
            get() = LocalJvmRecord::class
    }

    public interface NonFinalJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonFinalJvmRecord>
            get() = NonFinalJvmRecord::class
    }

    public interface EnumJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<EnumJvmRecord>
            get() = EnumJvmRecord::class
    }

    public interface JvmRecordWithoutPrimaryConstructorParameters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordWithoutPrimaryConstructorParameters>
            get() = JvmRecordWithoutPrimaryConstructorParameters::class
    }

    public interface NonDataClassJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonDataClassJvmRecord>
            get() = NonDataClassJvmRecord::class
    }

    public interface NonDataValueClassJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonDataValueClassJvmRecord>
            get() = NonDataValueClassJvmRecord::class
    }

    public interface JvmRecordNotValParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordNotValParameter>
            get() = JvmRecordNotValParameter::class
    }

    public interface JvmRecordNotLastVarargParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordNotLastVarargParameter>
            get() = JvmRecordNotLastVarargParameter::class
    }

    public interface InnerJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InnerJvmRecord>
            get() = InnerJvmRecord::class
    }

    public interface FieldInJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FieldInJvmRecord>
            get() = FieldInJvmRecord::class
    }

    public interface DelegationByInJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegationByInJvmRecord>
            get() = DelegationByInJvmRecord::class
    }

    public interface JvmRecordExtendsClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordExtendsClass>
            get() = JvmRecordExtendsClass::class

        public val superType: KaType
    }

    public interface IllegalJavaLangRecordSupertype : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalJavaLangRecordSupertype>
            get() = IllegalJavaLangRecordSupertype::class
    }

    public interface JvmRecordsIllegalBytecodeTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordsIllegalBytecodeTarget>
            get() = JvmRecordsIllegalBytecodeTarget::class
    }

    public interface JavaModuleDoesNotDependOnModule : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaModuleDoesNotDependOnModule>
            get() = JavaModuleDoesNotDependOnModule::class

        public val moduleName: String
    }

    public interface JavaModuleDoesNotReadUnnamedModule : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaModuleDoesNotReadUnnamedModule>
            get() = JavaModuleDoesNotReadUnnamedModule::class
    }

    public interface JavaModuleDoesNotExportPackage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaModuleDoesNotExportPackage>
            get() = JavaModuleDoesNotExportPackage::class

        public val moduleName: String
        public val packageName: String
    }

    public interface JvmDefaultWithoutCompatibilityNotInEnableMode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JvmDefaultWithoutCompatibilityNotInEnableMode>
            get() = JvmDefaultWithoutCompatibilityNotInEnableMode::class
    }

    public interface JvmDefaultWithCompatibilityNotInNoCompatibilityMode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JvmDefaultWithCompatibilityNotInNoCompatibilityMode>
            get() = JvmDefaultWithCompatibilityNotInNoCompatibilityMode::class
    }

    public interface ExternalDeclarationCannotBeAbstract : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExternalDeclarationCannotBeAbstract>
            get() = ExternalDeclarationCannotBeAbstract::class
    }

    public interface ExternalDeclarationCannotHaveBody : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExternalDeclarationCannotHaveBody>
            get() = ExternalDeclarationCannotHaveBody::class
    }

    public interface ExternalDeclarationInInterface : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExternalDeclarationInInterface>
            get() = ExternalDeclarationInInterface::class
    }

    public interface ExternalDeclarationCannotBeInlined : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExternalDeclarationCannotBeInlined>
            get() = ExternalDeclarationCannotBeInlined::class
    }

    public interface NonSourceRepeatedAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<NonSourceRepeatedAnnotation>
            get() = NonSourceRepeatedAnnotation::class
    }

    public interface RepeatedAnnotationWithContainer : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatedAnnotationWithContainer>
            get() = RepeatedAnnotationWithContainer::class

        public val name: ClassId
        public val explicitContainerName: ClassId
    }

    public interface RepeatableContainerMustHaveValueArrayError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableContainerMustHaveValueArrayError>
            get() = RepeatableContainerMustHaveValueArrayError::class

        public val container: ClassId
        public val annotation: ClassId
    }

    public interface RepeatableContainerHasNonDefaultParameterError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableContainerHasNonDefaultParameterError>
            get() = RepeatableContainerHasNonDefaultParameterError::class

        public val container: ClassId
        public val nonDefault: Name
    }

    public interface RepeatableContainerHasShorterRetentionError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableContainerHasShorterRetentionError>
            get() = RepeatableContainerHasShorterRetentionError::class

        public val container: ClassId
        public val retention: String
        public val annotation: ClassId
        public val annotationRetention: String
    }

    public interface RepeatableContainerTargetSetNotASubsetError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableContainerTargetSetNotASubsetError>
            get() = RepeatableContainerTargetSetNotASubsetError::class

        public val container: ClassId
        public val annotation: ClassId
    }

    public interface RepeatableAnnotationHasNestedClassNamedContainerError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableAnnotationHasNestedClassNamedContainerError>
            get() = RepeatableAnnotationHasNestedClassNamedContainerError::class
    }

    public interface SuspensionPointInsideCriticalSection : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuspensionPointInsideCriticalSection>
            get() = SuspensionPointInsideCriticalSection::class

        public val function: KaCallableSymbol
    }

    public interface InapplicableJvmField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableJvmField>
            get() = InapplicableJvmField::class

        public val message: String
    }

    public interface InapplicableJvmFieldWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableJvmFieldWarning>
            get() = InapplicableJvmFieldWarning::class

        public val message: String
    }

    public interface IdentitySensitiveOperationsWithValueType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IdentitySensitiveOperationsWithValueType>
            get() = IdentitySensitiveOperationsWithValueType::class

        public val type: KaType
    }

    public interface SynchronizedBlockOnJavaValueBasedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SynchronizedBlockOnJavaValueBasedClass>
            get() = SynchronizedBlockOnJavaValueBasedClass::class

        public val type: KaType
    }

    public interface SynchronizedBlockOnValueClassOrPrimitiveError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SynchronizedBlockOnValueClassOrPrimitiveError>
            get() = SynchronizedBlockOnValueClassOrPrimitiveError::class

        public val valueClassOrPrimitive: KaType
    }

    public interface SynchronizedBlockOnValueClassOrPrimitiveWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SynchronizedBlockOnValueClassOrPrimitiveWarning>
            get() = SynchronizedBlockOnValueClassOrPrimitiveWarning::class

        public val valueClassOrPrimitive: KaType
    }

    public interface JvmSyntheticOnDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmSyntheticOnDelegate>
            get() = JvmSyntheticOnDelegate::class
    }

    public interface SubclassCantCallCompanionProtectedNonStatic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SubclassCantCallCompanionProtectedNonStatic>
            get() = SubclassCantCallCompanionProtectedNonStatic::class
    }

    public interface SubclassCantCallCompanionProtectedNonStaticWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SubclassCantCallCompanionProtectedNonStaticWarning>
            get() = SubclassCantCallCompanionProtectedNonStaticWarning::class
    }

    public interface ConcurrentHashMapContainsOperatorError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConcurrentHashMapContainsOperatorError>
            get() = ConcurrentHashMapContainsOperatorError::class
    }

    public interface SpreadOnSignaturePolymorphicCallError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SpreadOnSignaturePolymorphicCallError>
            get() = SpreadOnSignaturePolymorphicCallError::class
    }

    public interface JavaSamInterfaceConstructorReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaSamInterfaceConstructorReference>
            get() = JavaSamInterfaceConstructorReference::class
    }

    public interface NoReflectionInClassPath : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoReflectionInClassPath>
            get() = NoReflectionInClassPath::class
    }

    public interface SyntheticPropertyWithoutJavaOrigin : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SyntheticPropertyWithoutJavaOrigin>
            get() = SyntheticPropertyWithoutJavaOrigin::class

        public val originalSymbol: KaFunctionSymbol
        public val functionName: Name
    }

    public interface JavaFieldShadowedByKotlinProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaFieldShadowedByKotlinProperty>
            get() = JavaFieldShadowedByKotlinProperty::class

        public val kotlinProperty: KaVariableSymbol
    }

    public interface MissingBuiltInDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingBuiltInDeclaration>
            get() = MissingBuiltInDeclaration::class

        public val symbol: KaSymbol
    }

    public interface DangerousCharacters : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<DangerousCharacters>
            get() = DangerousCharacters::class

        public val characters: String
    }

    public interface ImplementingFunctionInterface : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ImplementingFunctionInterface>
            get() = ImplementingFunctionInterface::class
    }

    public interface OverridingExternalFunWithOptionalParams : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<OverridingExternalFunWithOptionalParams>
            get() = OverridingExternalFunWithOptionalParams::class
    }

    public interface OverridingExternalFunWithOptionalParamsWithFake : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<OverridingExternalFunWithOptionalParamsWithFake>
            get() = OverridingExternalFunWithOptionalParamsWithFake::class

        public val function: KaFunctionSymbol
    }

    public interface ExternalEnumEntryWithBody : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalEnumEntryWithBody>
            get() = ExternalEnumEntryWithBody::class
    }

    public interface EnumClassInExternalDeclarationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<EnumClassInExternalDeclarationWarning>
            get() = EnumClassInExternalDeclarationWarning::class
    }

    public interface InlineClassInExternalDeclarationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InlineClassInExternalDeclarationWarning>
            get() = InlineClassInExternalDeclarationWarning::class
    }

    public interface InlineClassInExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InlineClassInExternalDeclaration>
            get() = InlineClassInExternalDeclaration::class
    }

    public interface ExtensionFunctionInExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExtensionFunctionInExternalDeclaration>
            get() = ExtensionFunctionInExternalDeclaration::class
    }

    public interface JsExternalInheritorsOnly : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<JsExternalInheritorsOnly>
            get() = JsExternalInheritorsOnly::class

        public val parent: KaClassLikeSymbol
        public val kid: KaClassLikeSymbol
    }

    public interface JsExternalArgument : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<JsExternalArgument>
            get() = JsExternalArgument::class

        public val argType: KaType
    }

    public interface WrongExportedDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongExportedDeclaration>
            get() = WrongExportedDeclaration::class

        public val kind: String
    }

    public interface NonExportableType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonExportableType>
            get() = NonExportableType::class

        public val kind: String
        public val type: KaType
    }

    public interface NonExportableTypeInSyntheticCopyFunctionWithExposedCopyVisibility : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonExportableTypeInSyntheticCopyFunctionWithExposedCopyVisibility>
            get() = NonExportableTypeInSyntheticCopyFunctionWithExposedCopyVisibility::class

        public val type: KaType
    }

    public interface NonExportableTypeInSyntheticCopyWithoutConsistentVisibility : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonExportableTypeInSyntheticCopyWithoutConsistentVisibility>
            get() = NonExportableTypeInSyntheticCopyWithoutConsistentVisibility::class

        public val type: KaType
    }

    public interface NonConsumableExportedIdentifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonConsumableExportedIdentifier>
            get() = NonConsumableExportedIdentifier::class

        public val name: String
    }

    public interface NamedCompanionInExportedInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NamedCompanionInExportedInterface>
            get() = NamedCompanionInExportedInterface::class
    }

    public interface NotExportedOrExternalActualDeclarationWhileExpectIsExported : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NotExportedOrExternalActualDeclarationWhileExpectIsExported>
            get() = NotExportedOrExternalActualDeclarationWhileExpectIsExported::class
    }

    public interface ExposedNotExportedSuperInterfaceError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedNotExportedSuperInterfaceError>
            get() = ExposedNotExportedSuperInterfaceError::class

        public val restrictingDeclaration: KaClassLikeSymbol
    }

    public interface ExposedNotExportedSuperInterfaceWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedNotExportedSuperInterfaceWarning>
            get() = ExposedNotExportedSuperInterfaceWarning::class

        public val restrictingDeclaration: KaClassLikeSymbol
    }

    public interface NestedJsExport : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NestedJsExport>
            get() = NestedJsExport::class
    }

    public interface MultipleJsExportDefaultInOneFile : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleJsExportDefaultInOneFile>
            get() = MultipleJsExportDefaultInOneFile::class
    }

    public interface WrongJsExportTargetVisibility : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongJsExportTargetVisibility>
            get() = WrongJsExportTargetVisibility::class
    }

    public interface DelegationByDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DelegationByDynamic>
            get() = DelegationByDynamic::class
    }

    public interface PropertyDelegationByDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<PropertyDelegationByDynamic>
            get() = PropertyDelegationByDynamic::class
    }

    public interface SpreadOperatorInDynamicCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SpreadOperatorInDynamicCall>
            get() = SpreadOperatorInDynamicCall::class
    }

    public interface WrongOperationWithDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongOperationWithDynamic>
            get() = WrongOperationWithDynamic::class

        public val operation: String
    }

    public interface JsStaticNotInObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JsStaticNotInObject>
            get() = JsStaticNotInObject::class
    }

    public interface JsStaticOnNonPublicMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JsStaticOnNonPublicMember>
            get() = JsStaticOnNonPublicMember::class
    }

    public interface JsStaticOnConst : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JsStaticOnConst>
            get() = JsStaticOnConst::class
    }

    public interface JsNoRuntimeWrongTarget : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeWrongTarget>
            get() = JsNoRuntimeWrongTarget::class
    }

    public interface JsNoRuntimeForbiddenIsCheck : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeForbiddenIsCheck>
            get() = JsNoRuntimeForbiddenIsCheck::class
    }

    public interface JsNoRuntimeForbiddenAsCast : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeForbiddenAsCast>
            get() = JsNoRuntimeForbiddenAsCast::class
    }

    public interface JsNoRuntimeForbiddenClassReference : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeForbiddenClassReference>
            get() = JsNoRuntimeForbiddenClassReference::class
    }

    public interface JsNoRuntimeUselessOnExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeUselessOnExternalInterface>
            get() = JsNoRuntimeUselessOnExternalInterface::class
    }

    public interface JsNoRuntimeInterfaceAsReifiedTypeArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeInterfaceAsReifiedTypeArgument>
            get() = JsNoRuntimeInterfaceAsReifiedTypeArgument::class

        public val typeArgument: KaType
    }

    public interface JsActualExternalInterfaceWhileExpectWithoutJsNoRuntime : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<JsActualExternalInterfaceWhileExpectWithoutJsNoRuntime>
            get() = JsActualExternalInterfaceWhileExpectWithoutJsNoRuntime::class
    }

    public interface JsNoRuntimeActualAnnotationsNotMatchExpect : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<JsNoRuntimeActualAnnotationsNotMatchExpect>
            get() = JsNoRuntimeActualAnnotationsNotMatchExpect::class
    }

    public interface Syntax : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<Syntax>
            get() = Syntax::class

        public val message: String
    }

    public interface NestedExternalDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NestedExternalDeclaration>
            get() = NestedExternalDeclaration::class
    }

    public interface WrongExternalDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<WrongExternalDeclaration>
            get() = WrongExternalDeclaration::class

        public val classKind: String
    }

    public interface NestedClassInExternalInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NestedClassInExternalInterface>
            get() = NestedClassInExternalInterface::class
    }

    public interface InlineExternalDeclaration : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InlineExternalDeclaration>
            get() = InlineExternalDeclaration::class
    }

    public interface NonAbstractMemberOfExternalInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NonAbstractMemberOfExternalInterface>
            get() = NonAbstractMemberOfExternalInterface::class
    }

    public interface ExternalClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ExternalClassConstructorPropertyParameter>
            get() = ExternalClassConstructorPropertyParameter::class
    }

    public interface ExternalAnonymousInitializer : KaFirDiagnostic<KtAnonymousInitializer> {
        override val diagnosticClass: KClass<ExternalAnonymousInitializer>
            get() = ExternalAnonymousInitializer::class
    }

    public interface ExternalDelegation : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalDelegation>
            get() = ExternalDelegation::class
    }

    public interface ExternalDelegatedConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalDelegatedConstructorCall>
            get() = ExternalDelegatedConstructorCall::class
    }

    public interface WrongBodyOfExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongBodyOfExternalDeclaration>
            get() = WrongBodyOfExternalDeclaration::class
    }

    public interface WrongInitializerOfExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongInitializerOfExternalDeclaration>
            get() = WrongInitializerOfExternalDeclaration::class
    }

    public interface WrongDefaultValueForExternalFunParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongDefaultValueForExternalFunParameter>
            get() = WrongDefaultValueForExternalFunParameter::class
    }

    public interface CannotCheckForExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotCheckForExternalInterface>
            get() = CannotCheckForExternalInterface::class

        public val targetType: KaType
    }

    public interface UncheckedCastToExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UncheckedCastToExternalInterface>
            get() = UncheckedCastToExternalInterface::class

        public val sourceType: KaType
        public val targetType: KaType
    }

    public interface ExternalInterfaceAsClassLiteral : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalInterfaceAsClassLiteral>
            get() = ExternalInterfaceAsClassLiteral::class
    }

    public interface ExternalInterfaceAsReifiedTypeArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalInterfaceAsReifiedTypeArgument>
            get() = ExternalInterfaceAsReifiedTypeArgument::class

        public val typeArgument: KaType
    }

    public interface NamedCompanionInExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NamedCompanionInExternalInterface>
            get() = NamedCompanionInExternalInterface::class
    }

    public interface CallToDefinedExternallyFromNonExternalDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CallToDefinedExternallyFromNonExternalDeclaration>
            get() = CallToDefinedExternallyFromNonExternalDeclaration::class
    }

    public interface ExternalTypeExtendsNonExternalType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalTypeExtendsNonExternalType>
            get() = ExternalTypeExtendsNonExternalType::class

        public val superType: KaType
    }

    public interface NonExternalDeclarationInInappropriateFile : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonExternalDeclarationInInappropriateFile>
            get() = NonExternalDeclarationInInappropriateFile::class

        public val type: KaType
    }

    public interface JscodeArgumentNonConstExpression : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JscodeArgumentNonConstExpression>
            get() = JscodeArgumentNonConstExpression::class
    }

}
