/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlin.reflect.KClass
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
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

@KaUnstableDiagnosticApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaFirDiagnostic<PSI : PsiElement> : KaDiagnosticWithPsi<PSI> {
    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Unsupported : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<Unsupported>
            get() = Unsupported::class

        public val unsupported: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedFeature : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedFeature>
            get() = UnsupportedFeature::class

        public val unsupportedFeature: Pair<LanguageFeature, LanguageVersionSettings>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedSuspendTest : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedSuspendTest>
            get() = UnsupportedSuspendTest::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NewInferenceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NewInferenceError>
            get() = NewInferenceError::class

        public val error: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EscapingCapturedVariable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<EscapingCapturedVariable>
            get() = EscapingCapturedVariable::class

        public val variable: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OtherError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OtherError>
            get() = OtherError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OtherErrorWithReason : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OtherErrorWithReason>
            get() = OtherErrorWithReason::class

        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalConstExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalConstExpression>
            get() = IllegalConstExpression::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalUnderscore : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalUnderscore>
            get() = IllegalUnderscore::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpressionExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpressionExpected>
            get() = ExpressionExpected::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssignmentInExpressionContext : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<AssignmentInExpressionContext>
            get() = AssignmentInExpressionContext::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface BreakOrContinueOutsideALoop : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<BreakOrContinueOutsideALoop>
            get() = BreakOrContinueOutsideALoop::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotALoopLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotALoopLabel>
            get() = NotALoopLabel::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface BreakOrContinueJumpsAcrossFunctionBoundary : KaFirDiagnostic<KtExpressionWithLabel> {
        override val diagnosticClass: KClass<BreakOrContinueJumpsAcrossFunctionBoundary>
            get() = BreakOrContinueJumpsAcrossFunctionBoundary::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VariableExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VariableExpected>
            get() = VariableExpected::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegationInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegationInInterface>
            get() = DelegationInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegationNotToInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegationNotToInterface>
            get() = DelegationNotToInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NestedClassNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NestedClassNotAllowed>
            get() = NestedClassNotAllowed::class

        public val declaration: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NestedClassNotAllowedInLocalError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NestedClassNotAllowedInLocalError>
            get() = NestedClassNotAllowedInLocalError::class

        public val declaration: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NestedClassNotAllowedInLocalWarning : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NestedClassNotAllowedInLocalWarning>
            get() = NestedClassNotAllowedInLocalWarning::class

        public val declaration: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncorrectCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IncorrectCharacterLiteral>
            get() = IncorrectCharacterLiteral::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EmptyCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<EmptyCharacterLiteral>
            get() = EmptyCharacterLiteral::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TooManyCharactersInCharacterLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TooManyCharactersInCharacterLiteral>
            get() = TooManyCharactersInCharacterLiteral::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalEscape : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalEscape>
            get() = IllegalEscape::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IntLiteralOutOfRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IntLiteralOutOfRange>
            get() = IntLiteralOutOfRange::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IntLiteralWithLeadingZeros : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IntLiteralWithLeadingZeros>
            get() = IntLiteralWithLeadingZeros::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FloatLiteralOutOfRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FloatLiteralOutOfRange>
            get() = FloatLiteralOutOfRange::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongLongSuffix : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongLongSuffix>
            get() = WrongLongSuffix::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsignedLiteralWithoutDeclarationsOnClasspath : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnsignedLiteralWithoutDeclarationsOnClasspath>
            get() = UnsignedLiteralWithoutDeclarationsOnClasspath::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DivisionByZero : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DivisionByZero>
            get() = DivisionByZero::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TrimMarginBlankPrefix : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<TrimMarginBlankPrefix>
            get() = TrimMarginBlankPrefix::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValOrVarOnLoopParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValOrVarOnLoopParameter>
            get() = ValOrVarOnLoopParameter::class

        public val valOrVar: KtKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValOrVarOnFunParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValOrVarOnFunParameter>
            get() = ValOrVarOnFunParameter::class

        public val valOrVar: KtKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValOrVarOnCatchParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValOrVarOnCatchParameter>
            get() = ValOrVarOnCatchParameter::class

        public val valOrVar: KtKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValOrVarOnSecondaryConstructorParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValOrVarOnSecondaryConstructorParameter>
            get() = ValOrVarOnSecondaryConstructorParameter::class

        public val valOrVar: KtKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InnerOnTopLevelScriptClassError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InnerOnTopLevelScriptClassError>
            get() = InnerOnTopLevelScriptClassError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InnerOnTopLevelScriptClassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InnerOnTopLevelScriptClassWarning>
            get() = InnerOnTopLevelScriptClassWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ErrorSuppression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ErrorSuppression>
            get() = ErrorSuppression::class

        public val diagnosticName: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingConstructorKeyword : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingConstructorKeyword>
            get() = MissingConstructorKeyword::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantInterpolationPrefix : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantInterpolationPrefix>
            get() = RedundantInterpolationPrefix::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrappedLhsInAssignmentError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrappedLhsInAssignmentError>
            get() = WrappedLhsInAssignmentError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrappedLhsInAssignmentWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrappedLhsInAssignmentWarning>
            get() = WrappedLhsInAssignmentWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ParenthesizedPackageQualifierError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ParenthesizedPackageQualifierError>
            get() = ParenthesizedPackageQualifierError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ParenthesizedPackageQualifierWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ParenthesizedPackageQualifierWarning>
            get() = ParenthesizedPackageQualifierWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface KotlinPackageUsage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<KotlinPackageUsage>
            get() = KotlinPackageUsage::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedArrayLiteralOutsideOfAnnotationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedArrayLiteralOutsideOfAnnotationError>
            get() = UnsupportedArrayLiteralOutsideOfAnnotationError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedArrayLiteralOutsideOfAnnotationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedArrayLiteralOutsideOfAnnotationWarning>
            get() = UnsupportedArrayLiteralOutsideOfAnnotationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnresolvedReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnresolvedReference>
            get() = UnresolvedReference::class

        public val reference: String
        public val operator: String?
        public val receiverType: KaType?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnresolvedReferenceWrongReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnresolvedReferenceWrongReceiver>
            get() = UnresolvedReferenceWrongReceiver::class

        public val candidate: KaSymbol
        public val operator: String?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InaccessibleOuterClassReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InaccessibleOuterClassReceiver>
            get() = InaccessibleOuterClassReceiver::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnresolvedImport : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnresolvedImport>
            get() = UnresolvedImport::class

        public val reference: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvisibleReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvisibleReference>
            get() = InvisibleReference::class

        public val reference: KaSymbol
        public val visible: Visibility
        public val containingDeclaration: ClassId?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvisibleReferenceWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvisibleReferenceWarning>
            get() = InvisibleReferenceWarning::class

        public val reference: KaSymbol
        public val visible: Visibility
        public val containingDeclaration: ClassId?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvisibleSetter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvisibleSetter>
            get() = InvisibleSetter::class

        public val property: KaVariableSymbol
        public val visibility: Visibility
        public val callableId: CallableId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnresolvedLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnresolvedLabel>
            get() = UnresolvedLabel::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousLabel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AmbiguousLabel>
            get() = AmbiguousLabel::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LabelNameClash : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LabelNameClash>
            get() = LabelNameClash::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeserializationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeserializationError>
            get() = DeserializationError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ErrorFromJavaResolution : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ErrorFromJavaResolution>
            get() = ErrorFromJavaResolution::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingStdlibClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingStdlibClass>
            get() = MissingStdlibClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoThis : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoThis>
            get() = NoThis::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ApiNotAvailable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ApiNotAvailable>
            get() = ApiNotAvailable::class

        public val sinceKotlinVersion: ApiVersion
        public val currentVersion: ApiVersion
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PlaceholderProjectionInQualifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PlaceholderProjectionInQualifier>
            get() = PlaceholderProjectionInQualifier::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PlaceholderProjectionInTyperef : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PlaceholderProjectionInTyperef>
            get() = PlaceholderProjectionInTyperef::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DuplicateParameterNameInFunctionType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DuplicateParameterNameInFunctionType>
            get() = DuplicateParameterNameInFunctionType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencyClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClass>
            get() = MissingDependencyClass::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencyClassInExpressionType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClassInExpressionType>
            get() = MissingDependencyClassInExpressionType::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencySuperclass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencySuperclass>
            get() = MissingDependencySuperclass::class

        public val missingTypeConstructorName: FqName
        public val declarationTypeConstructorName: FqName
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencySuperclassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencySuperclassWarning>
            get() = MissingDependencySuperclassWarning::class

        public val missingTypeConstructorName: FqName
        public val declarationTypeConstructorName: FqName
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencyClassInLambdaParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClassInLambdaParameter>
            get() = MissingDependencyClassInLambdaParameter::class

        public val type: KaType
        public val parameterName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencyClassInLambdaReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClassInLambdaReceiver>
            get() = MissingDependencyClassInLambdaReceiver::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencyClassInTypealias : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyClassInTypealias>
            get() = MissingDependencyClassInTypealias::class

        public val missingType: KaType
        public val declarationType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencyInInferredTypeAnnotationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyInInferredTypeAnnotationError>
            get() = MissingDependencyInInferredTypeAnnotationError::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingDependencyInInferredTypeAnnotationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingDependencyInInferredTypeAnnotationWarning>
            get() = MissingDependencyInInferredTypeAnnotationWarning::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RootIdePackageDeprecated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RootIdePackageDeprecated>
            get() = RootIdePackageDeprecated::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SmartcastToTypeVariable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SmartcastToTypeVariable>
            get() = SmartcastToTypeVariable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CreatingAnInstanceOfAbstractClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CreatingAnInstanceOfAbstractClass>
            get() = CreatingAnInstanceOfAbstractClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoConstructor>
            get() = NoConstructor::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoImplicitDefaultConstructorOnExpectClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoImplicitDefaultConstructorOnExpectClass>
            get() = NoImplicitDefaultConstructorOnExpectClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunctionCallExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FunctionCallExpected>
            get() = FunctionCallExpected::class

        public val functionName: String
        public val hasValueParameters: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalSelector : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalSelector>
            get() = IllegalSelector::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoReceiverAllowed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoReceiverAllowed>
            get() = NoReceiverAllowed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunctionExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FunctionExpected>
            get() = FunctionExpected::class

        public val expression: String
        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InterfaceAsFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InterfaceAsFunction>
            get() = InterfaceAsFunction::class

        public val classSymbol: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectClassAsFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpectClassAsFunction>
            get() = ExpectClassAsFunction::class

        public val classSymbol: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InnerClassConstructorNoReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InnerClassConstructorNoReceiver>
            get() = InnerClassConstructorNoReceiver::class

        public val classSymbol: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PluginAmbiguousInterceptedSymbol : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PluginAmbiguousInterceptedSymbol>
            get() = PluginAmbiguousInterceptedSymbol::class

        public val names: List<String>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ResolutionToClassifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ResolutionToClassifier>
            get() = ResolutionToClassifier::class

        public val classSymbol: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousAlteredAssign : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AmbiguousAlteredAssign>
            get() = AmbiguousAlteredAssign::class

        public val altererNames: List<String?>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SelfCallInNestedObjectConstructorError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SelfCallInNestedObjectConstructorError>
            get() = SelfCallInNestedObjectConstructorError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousCollectionLiteral : KaFirDiagnostic<KtCollectionLiteralExpression> {
        override val diagnosticClass: KClass<AmbiguousCollectionLiteral>
            get() = AmbiguousCollectionLiteral::class

        public val candidatesWithOf: List<KaClassLikeSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnresolvedCollectionLiteral : KaFirDiagnostic<KtCollectionLiteralExpression> {
        override val diagnosticClass: KClass<UnresolvedCollectionLiteral>
            get() = UnresolvedCollectionLiteral::class

        public val incompatibleBound: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplicitPropertyTypeMakesBehaviorOrderDependant : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ImplicitPropertyTypeMakesBehaviorOrderDependant>
            get() = ImplicitPropertyTypeMakesBehaviorOrderDependant::class

        public val property: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplicitPropertyTypeMakesBehaviorOrderDependantError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ImplicitPropertyTypeMakesBehaviorOrderDependantError>
            get() = ImplicitPropertyTypeMakesBehaviorOrderDependantError::class

        public val property: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SuperIsNotAnExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuperIsNotAnExpression>
            get() = SuperIsNotAnExpression::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SuperNotAvailable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuperNotAvailable>
            get() = SuperNotAvailable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractSuperCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AbstractSuperCall>
            get() = AbstractSuperCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InstanceAccessBeforeSuperCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InstanceAccessBeforeSuperCall>
            get() = InstanceAccessBeforeSuperCall::class

        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SuperCallWithDefaultParameters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuperCallWithDefaultParameters>
            get() = SuperCallWithDefaultParameters::class

        public val name: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InterfaceCantCallDefaultMethodViaSuper : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InterfaceCantCallDefaultMethodViaSuper>
            get() = InterfaceCantCallDefaultMethodViaSuper::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaClassInheritsKtPrivateClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JavaClassInheritsKtPrivateClass>
            get() = JavaClassInheritsKtPrivateClass::class

        public val javaClassId: ClassId
        public val privateKotlinType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotASupertype : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotASupertype>
            get() = NotASupertype::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeArgumentsRedundantInSuperQualifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypeArgumentsRedundantInSuperQualifier>
            get() = TypeArgumentsRedundantInSuperQualifier::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SuperclassNotAccessibleFromInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuperclassNotAccessibleFromInterface>
            get() = SuperclassNotAccessibleFromInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SupertypeInitializedInInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeInitializedInInterface>
            get() = SupertypeInitializedInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InterfaceWithSuperclass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InterfaceWithSuperclass>
            get() = InterfaceWithSuperclass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FinalSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<FinalSupertype>
            get() = FinalSupertype::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ClassCannotBeExtendedDirectly : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ClassCannotBeExtendedDirectly>
            get() = ClassCannotBeExtendedDirectly::class

        public val classSymbol: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SupertypeIsExtensionOrContextFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeIsExtensionOrContextFunctionType>
            get() = SupertypeIsExtensionOrContextFunctionType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SingletonInSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SingletonInSupertype>
            get() = SingletonInSupertype::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullableSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableSupertype>
            get() = NullableSupertype::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullableSupertypeThroughTypealiasError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableSupertypeThroughTypealiasError>
            get() = NullableSupertypeThroughTypealiasError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullableSupertypeThroughTypealiasWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableSupertypeThroughTypealiasWarning>
            get() = NullableSupertypeThroughTypealiasWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ManyClassesInSupertypeList : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ManyClassesInSupertypeList>
            get() = ManyClassesInSupertypeList::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SupertypeAppearsTwice : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeAppearsTwice>
            get() = SupertypeAppearsTwice::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ClassInSupertypeForEnum : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ClassInSupertypeForEnum>
            get() = ClassInSupertypeForEnum::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SealedSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SealedSupertype>
            get() = SealedSupertype::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SealedSupertypeInLocalClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SealedSupertypeInLocalClass>
            get() = SealedSupertypeInLocalClass::class

        public val declarationType: String
        public val sealedClassKind: ClassKind
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SealedInheritorInDifferentPackage : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SealedInheritorInDifferentPackage>
            get() = SealedInheritorInDifferentPackage::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SealedInheritorInDifferentModule : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SealedInheritorInDifferentModule>
            get() = SealedInheritorInDifferentModule::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ClassInheritsJavaSealedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ClassInheritsJavaSealedClass>
            get() = ClassInheritsJavaSealedClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedSealedFunInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedSealedFunInterface>
            get() = UnsupportedSealedFunInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SupertypeNotAClassOrInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeNotAClassOrInterface>
            get() = SupertypeNotAClassOrInterface::class

        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction>
            get() = UnsupportedInheritanceFromJavaMemberReferencingKotlinFunction::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CyclicInheritanceHierarchy : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CyclicInheritanceHierarchy>
            get() = CyclicInheritanceHierarchy::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ProjectionInImmediateArgumentToSupertype : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ProjectionInImmediateArgumentToSupertype>
            get() = ProjectionInImmediateArgumentToSupertype::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InconsistentTypeParameterValues : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<InconsistentTypeParameterValues>
            get() = InconsistentTypeParameterValues::class

        public val typeParameter: KaTypeParameterSymbol
        public val type: KaClassLikeSymbol
        public val bounds: List<KaType>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InconsistentTypeParameterBounds : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InconsistentTypeParameterBounds>
            get() = InconsistentTypeParameterBounds::class

        public val typeParameter: KaTypeParameterSymbol
        public val type: KaClassLikeSymbol
        public val bounds: List<KaType>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousSuper : KaFirDiagnostic<KtSuperExpression> {
        override val diagnosticClass: KClass<AmbiguousSuper>
            get() = AmbiguousSuper::class

        public val candidates: List<KaType>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongMultipleInheritance : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongMultipleInheritance>
            get() = WrongMultipleInheritance::class

        public val symbol: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstructorInObject : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ConstructorInObject>
            get() = ConstructorInObject::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstructorInInterface : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ConstructorInInterface>
            get() = ConstructorInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonPrivateConstructorInEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonPrivateConstructorInEnum>
            get() = NonPrivateConstructorInEnum::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonPrivateOrProtectedConstructorInSealed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonPrivateOrProtectedConstructorInSealed>
            get() = NonPrivateOrProtectedConstructorInSealed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CyclicConstructorDelegationCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CyclicConstructorDelegationCall>
            get() = CyclicConstructorDelegationCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PrimaryConstructorDelegationCallExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PrimaryConstructorDelegationCallExpected>
            get() = PrimaryConstructorDelegationCallExpected::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ProtectedConstructorNotInSuperCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ProtectedConstructorNotInSuperCall>
            get() = ProtectedConstructorNotInSuperCall::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SupertypeNotInitialized : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeNotInitialized>
            get() = SupertypeNotInitialized::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SupertypeInitializedWithoutPrimaryConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SupertypeInitializedWithoutPrimaryConstructor>
            get() = SupertypeInitializedWithoutPrimaryConstructor::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegationSuperCallInEnumConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegationSuperCallInEnumConstructor>
            get() = DelegationSuperCallInEnumConstructor::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExplicitDelegationCallRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExplicitDelegationCallRequired>
            get() = ExplicitDelegationCallRequired::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SealedClassConstructorCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SealedClassConstructorCall>
            get() = SealedClassConstructorCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations>
            get() = DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotations::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassConsistentCopyWrongAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DataClassConsistentCopyWrongAnnotationTarget>
            get() = DataClassConsistentCopyWrongAnnotationTarget::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassCopyVisibilityWillBeChangedError : KaFirDiagnostic<KtPrimaryConstructor> {
        override val diagnosticClass: KClass<DataClassCopyVisibilityWillBeChangedError>
            get() = DataClassCopyVisibilityWillBeChangedError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassCopyVisibilityWillBeChangedWarning : KaFirDiagnostic<KtPrimaryConstructor> {
        override val diagnosticClass: KClass<DataClassCopyVisibilityWillBeChangedWarning>
            get() = DataClassCopyVisibilityWillBeChangedWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassInvisibleCopyUsageError : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass: KClass<DataClassInvisibleCopyUsageError>
            get() = DataClassInvisibleCopyUsageError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassInvisibleCopyUsageWarning : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass: KClass<DataClassInvisibleCopyUsageWarning>
            get() = DataClassInvisibleCopyUsageWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassWithoutParameters : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<DataClassWithoutParameters>
            get() = DataClassWithoutParameters::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassVarargParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<DataClassVarargParameter>
            get() = DataClassVarargParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassNotPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<DataClassNotPropertyParameter>
            get() = DataClassNotPropertyParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassCopyJsExportabilityWillBeChangedError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DataClassCopyJsExportabilityWillBeChangedError>
            get() = DataClassCopyJsExportabilityWillBeChangedError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassCopyJsExportabilityWillBeChangedWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DataClassCopyJsExportabilityWillBeChangedWarning>
            get() = DataClassCopyJsExportabilityWillBeChangedWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationArgumentKclassLiteralOfTypeParameterError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationArgumentKclassLiteralOfTypeParameterError>
            get() = AnnotationArgumentKclassLiteralOfTypeParameterError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationArgumentMustBeConst : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationArgumentMustBeConst>
            get() = AnnotationArgumentMustBeConst::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationArgumentMustBeEnumConst : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationArgumentMustBeEnumConst>
            get() = AnnotationArgumentMustBeEnumConst::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationArgumentMustBeKclassLiteral : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationArgumentMustBeKclassLiteral>
            get() = AnnotationArgumentMustBeKclassLiteral::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationClassMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AnnotationClassMember>
            get() = AnnotationClassMember::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationParameterDefaultValueMustBeConstant : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AnnotationParameterDefaultValueMustBeConstant>
            get() = AnnotationParameterDefaultValueMustBeConstant::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidTypeOfAnnotationMember : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InvalidTypeOfAnnotationMember>
            get() = InvalidTypeOfAnnotationMember::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ProjectionInTypeOfAnnotationMemberError : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass: KClass<ProjectionInTypeOfAnnotationMemberError>
            get() = ProjectionInTypeOfAnnotationMemberError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ProjectionInTypeOfAnnotationMemberWarning : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass: KClass<ProjectionInTypeOfAnnotationMemberWarning>
            get() = ProjectionInTypeOfAnnotationMemberWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LocalAnnotationClassError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<LocalAnnotationClassError>
            get() = LocalAnnotationClassError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingValOnAnnotationParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<MissingValOnAnnotationParameter>
            get() = MissingValOnAnnotationParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonConstValUsedInConstantExpression : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NonConstValUsedInConstantExpression>
            get() = NonConstValUsedInConstantExpression::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CycleInAnnotationParameterError : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<CycleInAnnotationParameterError>
            get() = CycleInAnnotationParameterError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationClassConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<AnnotationClassConstructorCall>
            get() = AnnotationClassConstructorCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EnumClassConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<EnumClassConstructorCall>
            get() = EnumClassConstructorCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotAnAnnotationClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotAnAnnotationClass>
            get() = NotAnAnnotationClass::class

        public val annotationName: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullableTypeOfAnnotationMember : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableTypeOfAnnotationMember>
            get() = NullableTypeOfAnnotationMember::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarAnnotationParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<VarAnnotationParameter>
            get() = VarAnnotationParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SupertypesForAnnotationClass : KaFirDiagnostic<KtClass> {
        override val diagnosticClass: KClass<SupertypesForAnnotationClass>
            get() = SupertypesForAnnotationClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationUsedAsAnnotationArgument : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationUsedAsAnnotationArgument>
            get() = AnnotationUsedAsAnnotationArgument::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationOnAnnotationArgument : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationOnAnnotationArgument>
            get() = AnnotationOnAnnotationArgument::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalKotlinVersionStringValue : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<IllegalKotlinVersionStringValue>
            get() = IllegalKotlinVersionStringValue::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NewerVersionInSinceKotlin : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NewerVersionInSinceKotlin>
            get() = NewerVersionInSinceKotlin::class

        public val specifiedVersion: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedSinceKotlinWithUnorderedVersions : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinWithUnorderedVersions>
            get() = DeprecatedSinceKotlinWithUnorderedVersions::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedSinceKotlinWithoutArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinWithoutArguments>
            get() = DeprecatedSinceKotlinWithoutArguments::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedSinceKotlinWithoutDeprecated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinWithoutDeprecated>
            get() = DeprecatedSinceKotlinWithoutDeprecated::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedSinceKotlinWithDeprecatedLevel : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinWithDeprecatedLevel>
            get() = DeprecatedSinceKotlinWithDeprecatedLevel::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedSinceKotlinOutsideKotlinSubpackage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedSinceKotlinOutsideKotlinSubpackage>
            get() = DeprecatedSinceKotlinOutsideKotlinSubpackage::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface KotlinActualAnnotationHasNoEffectInKotlin : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<KotlinActualAnnotationHasNoEffectInKotlin>
            get() = KotlinActualAnnotationHasNoEffectInKotlin::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecationError>
            get() = DeprecationError::class

        public val reference: KaSymbol
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Deprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<Deprecation>
            get() = Deprecation::class

        public val reference: KaSymbol
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecationErrorMigrationPeriodWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecationErrorMigrationPeriodWarning>
            get() = DeprecationErrorMigrationPeriodWarning::class

        public val reference: KaSymbol
        public val message: String
        public val migrationLanguageFeature: LanguageFeature
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverrideDeprecation : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<OverrideDeprecation>
            get() = OverrideDeprecation::class

        public val overridenSymbol: KaSymbol
        public val deprecationInfo: FirDeprecationInfo
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExtendingAnAnnotationClassError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExtendingAnAnnotationClassError>
            get() = ExtendingAnAnnotationClassError::class

        public val annotationSymbol: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExtendingAnAnnotationClassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExtendingAnAnnotationClassWarning>
            get() = ExtendingAnAnnotationClassWarning::class

        public val annotationSymbol: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasExpansionDeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypealiasExpansionDeprecationError>
            get() = TypealiasExpansionDeprecationError::class

        public val alias: KaSymbol
        public val reference: KaSymbol
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasExpansionDeprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypealiasExpansionDeprecation>
            get() = TypealiasExpansionDeprecation::class

        public val alias: KaSymbol
        public val reference: KaSymbol
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VersionRequirementDeprecationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VersionRequirementDeprecationError>
            get() = VersionRequirementDeprecationError::class

        public val reference: KaSymbol
        public val version: Version
        public val currentVersion: String
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VersionRequirementDeprecation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VersionRequirementDeprecation>
            get() = VersionRequirementDeprecation::class

        public val reference: KaSymbol
        public val version: Version
        public val currentVersion: String
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RedundantAnnotation>
            get() = RedundantAnnotation::class

        public val annotation: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationOnSuperclassError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationOnSuperclassError>
            get() = AnnotationOnSuperclassError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RestrictedRetentionForExpressionAnnotationError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RestrictedRetentionForExpressionAnnotationError>
            get() = RestrictedRetentionForExpressionAnnotationError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<WrongAnnotationTarget>
            get() = WrongAnnotationTarget::class

        public val actualTarget: String
        public val allowedTargets: List<KotlinTarget>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongAnnotationTargetWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<WrongAnnotationTargetWarning>
            get() = WrongAnnotationTargetWarning::class

        public val actualTarget: String
        public val allowedTargets: List<KotlinTarget>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongAnnotationTargetWithUseSiteTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<WrongAnnotationTargetWithUseSiteTarget>
            get() = WrongAnnotationTargetWithUseSiteTarget::class

        public val actualTarget: String
        public val useSiteTarget: String
        public val allowedTargets: List<KotlinTarget>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationWithUseSiteTargetOnExpressionError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationWithUseSiteTargetOnExpressionError>
            get() = AnnotationWithUseSiteTargetOnExpressionError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationWithUseSiteTargetOnExpressionWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationWithUseSiteTargetOnExpressionWarning>
            get() = AnnotationWithUseSiteTargetOnExpressionWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableTargetOnProperty : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetOnProperty>
            get() = InapplicableTargetOnProperty::class

        public val useSiteDescription: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableTargetOnPropertyWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetOnPropertyWarning>
            get() = InapplicableTargetOnPropertyWarning::class

        public val useSiteDescription: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableTargetPropertyImmutable : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetPropertyImmutable>
            get() = InapplicableTargetPropertyImmutable::class

        public val useSiteDescription: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableTargetPropertyHasNoDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetPropertyHasNoDelegate>
            get() = InapplicableTargetPropertyHasNoDelegate::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableTargetPropertyHasNoBackingField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableTargetPropertyHasNoBackingField>
            get() = InapplicableTargetPropertyHasNoBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableParamTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableParamTarget>
            get() = InapplicableParamTarget::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableFileTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableFileTarget>
            get() = InapplicableFileTarget::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableAllTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableAllTarget>
            get() = InapplicableAllTarget::class

        public val inapplicableTargetDescription: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableAllTargetInMultiAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableAllTargetInMultiAnnotation>
            get() = InapplicableAllTargetInMultiAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatedAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RepeatedAnnotation>
            get() = RepeatedAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatedAnnotationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RepeatedAnnotationWarning>
            get() = RepeatedAnnotationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantAnnotationTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RedundantAnnotationTarget>
            get() = RedundantAnnotationTarget::class

        public val useSiteDescription: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotAClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotAClass>
            get() = NotAClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongExtensionFunctionType : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<WrongExtensionFunctionType>
            get() = WrongExtensionFunctionType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationInWhereClauseError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationInWhereClauseError>
            get() = AnnotationInWhereClauseError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationInContractError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<AnnotationInContractError>
            get() = AnnotationInContractError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousAnnotationArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AmbiguousAnnotationArgument>
            get() = AmbiguousAnnotationArgument::class

        public val symbols: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VolatileOnValue : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<VolatileOnValue>
            get() = VolatileOnValue::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VolatileOnDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<VolatileOnDelegate>
            get() = VolatileOnDelegate::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonInternalPublishedApi : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonInternalPublishedApi>
            get() = NonInternalPublishedApi::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonSourceAnnotationOnInlinedLambdaExpression : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<NonSourceAnnotationOnInlinedLambdaExpression>
            get() = NonSourceAnnotationOnInlinedLambdaExpression::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PotentiallyNonReportedAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<PotentiallyNonReportedAnnotation>
            get() = PotentiallyNonReportedAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationWillBeAppliedAlsoToPropertyOrField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationWillBeAppliedAlsoToPropertyOrField>
            get() = AnnotationWillBeAppliedAlsoToPropertyOrField::class

        public val useSiteDescription: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationsOnBlockLevelExpressionOnTheSameLine : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AnnotationsOnBlockLevelExpressionOnTheSameLine>
            get() = AnnotationsOnBlockLevelExpressionOnTheSameLine::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IgnorabilityAnnotationsWithCheckerDisabled : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<IgnorabilityAnnotationsWithCheckerDisabled>
            get() = IgnorabilityAnnotationsWithCheckerDisabled::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DslMarkerPropagatesToMany : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DslMarkerPropagatesToMany>
            get() = DslMarkerPropagatesToMany::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DslMarkerAppliedToWrongTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DslMarkerAppliedToWrongTarget>
            get() = DslMarkerAppliedToWrongTarget::class

        public val dslMarkerSymbol: KaClassLikeSymbol
        public val actualTarget: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsModuleProhibitedOnNonNative : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsModuleProhibitedOnNonNative>
            get() = JsModuleProhibitedOnNonNative::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallFromUmdMustBeJsModuleAndJsNonModule : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallFromUmdMustBeJsModuleAndJsNonModule>
            get() = CallFromUmdMustBeJsModuleAndJsNonModule::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallToJsModuleWithoutModuleSystem : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallToJsModuleWithoutModuleSystem>
            get() = CallToJsModuleWithoutModuleSystem::class

        public val callee: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallToJsNonModuleWithModuleSystem : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallToJsNonModuleWithModuleSystem>
            get() = CallToJsNonModuleWithModuleSystem::class

        public val callee: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RuntimeAnnotationOnExternalDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RuntimeAnnotationOnExternalDeclaration>
            get() = RuntimeAnnotationOnExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun>
            get() = NativeAnnotationsAllowedOnlyOnMemberOrExtensionFun::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NativeIndexerKeyShouldBeStringOrNumber : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NativeIndexerKeyShouldBeStringOrNumber>
            get() = NativeIndexerKeyShouldBeStringOrNumber::class

        public val kind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NativeIndexerWrongParameterCount : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NativeIndexerWrongParameterCount>
            get() = NativeIndexerWrongParameterCount::class

        public val parametersCount: Int
        public val kind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NativeIndexerCanNotHaveDefaultArguments : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NativeIndexerCanNotHaveDefaultArguments>
            get() = NativeIndexerCanNotHaveDefaultArguments::class

        public val kind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NativeGetterReturnTypeShouldBeNullable : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NativeGetterReturnTypeShouldBeNullable>
            get() = NativeGetterReturnTypeShouldBeNullable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NativeSetterWrongReturnType : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NativeSetterWrongReturnType>
            get() = NativeSetterWrongReturnType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNameIsNotOnAllAccessors : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameIsNotOnAllAccessors>
            get() = JsNameIsNotOnAllAccessors::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNameProhibitedForNamedNative : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameProhibitedForNamedNative>
            get() = JsNameProhibitedForNamedNative::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNameProhibitedForOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameProhibitedForOverride>
            get() = JsNameProhibitedForOverride::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNameOnPrimaryConstructorProhibited : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameOnPrimaryConstructorProhibited>
            get() = JsNameOnPrimaryConstructorProhibited::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNameOnAccessorAndProperty : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameOnAccessorAndProperty>
            get() = JsNameOnAccessorAndProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNameProhibitedForExtensionProperty : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameProhibitedForExtensionProperty>
            get() = JsNameProhibitedForExtensionProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsBuiltinNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsBuiltinNameClash>
            get() = JsBuiltinNameClash::class

        public val name: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NameContainsIllegalChars : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NameContainsIllegalChars>
            get() = NameContainsIllegalChars::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNameClash>
            get() = JsNameClash::class

        public val name: String
        public val existing: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsFakeNameClash : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsFakeNameClash>
            get() = JsFakeNameClash::class

        public val name: String
        public val override: KaSymbol
        public val existing: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsSymbolOnTopLevelDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsSymbolOnTopLevelDeclaration>
            get() = JsSymbolOnTopLevelDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsSymbolProhibitedForOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsSymbolProhibitedForOverride>
            get() = JsSymbolProhibitedForOverride::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongJsQualifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongJsQualifier>
            get() = WrongJsQualifier::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsModuleProhibitedOnVar : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsModuleProhibitedOnVar>
            get() = JsModuleProhibitedOnVar::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NestedJsModuleProhibited : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NestedJsModuleProhibited>
            get() = NestedJsModuleProhibited::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnresolvedEqualityBoundArgument : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UnresolvedEqualityBoundArgument>
            get() = UnresolvedEqualityBoundArgument::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguouslyResolvedEqualityBoundArgument : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AmbiguouslyResolvedEqualityBoundArgument>
            get() = AmbiguouslyResolvedEqualityBoundArgument::class

        public val candidates: List<KaType>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EqualityBoundArgumentExpandsToNonStarProjected : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<EqualityBoundArgumentExpandsToNonStarProjected>
            get() = EqualityBoundArgumentExpandsToNonStarProjected::class

        public val expandedType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EqualityBoundMismatchOnInheritance : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<EqualityBoundMismatchOnInheritance>
            get() = EqualityBoundMismatchOnInheritance::class

        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EqualityBoundMismatchByDelegation : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<EqualityBoundMismatchByDelegation>
            get() = EqualityBoundMismatchByDelegation::class

        public val delegateDeclaration: KaCallableSymbol
        public val baseDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InheritedIntersectionEqualityBound : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InheritedIntersectionEqualityBound>
            get() = InheritedIntersectionEqualityBound::class

        public val declaration: KaCallableSymbol
        public val candidates: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EqualityBoundNotSupertypeOfContainingClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<EqualityBoundNotSupertypeOfContainingClass>
            get() = EqualityBoundNotSupertypeOfContainingClass::class

        public val equalityBoundType: KaType
        public val receiverType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EqualityNotApplicableByEqualityBounds : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<EqualityNotApplicableByEqualityBounds>
            get() = EqualityNotApplicableByEqualityBounds::class

        public val leftType: KaType
        public val rightType: KaType
        public val leftIsEqualityBound: String
        public val rightIsEqualityBound: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EqualitySuspiciousByEqualityBounds : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<EqualitySuspiciousByEqualityBounds>
            get() = EqualitySuspiciousByEqualityBounds::class

        public val leftType: KaType
        public val rightType: KaType
        public val leftEqualityBound: KaType
        public val rightEqualityBound: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInUsage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInUsage>
            get() = OptInUsage::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInUsageError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInUsageError>
            get() = OptInUsageError::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInToInheritance : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInToInheritance>
            get() = OptInToInheritance::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInToInheritanceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInToInheritanceError>
            get() = OptInToInheritanceError::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInOverride : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInOverride>
            get() = OptInOverride::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInOverrideError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInOverrideError>
            get() = OptInOverrideError::class

        public val optInMarkerClassId: ClassId
        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInCanOnlyBeUsedAsAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInCanOnlyBeUsedAsAnnotation>
            get() = OptInCanOnlyBeUsedAsAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn>
            get() = OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptIn::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInWithoutArguments : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInWithoutArguments>
            get() = OptInWithoutArguments::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInArgumentIsNotMarker : KaFirDiagnostic<KtClassLiteralExpression> {
        override val diagnosticClass: KClass<OptInArgumentIsNotMarker>
            get() = OptInArgumentIsNotMarker::class

        public val notMarkerClassId: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInMarkerWithWrongTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerWithWrongTarget>
            get() = OptInMarkerWithWrongTarget::class

        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInMarkerWithWrongRetention : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerWithWrongRetention>
            get() = OptInMarkerWithWrongRetention::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInMarkerOnWrongTarget : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerOnWrongTarget>
            get() = OptInMarkerOnWrongTarget::class

        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInMarkerOnOverride : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerOnOverride>
            get() = OptInMarkerOnOverride::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptInMarkerOnOverrideWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OptInMarkerOnOverrideWarning>
            get() = OptInMarkerOnOverrideWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SubclassOptInInapplicable : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SubclassOptInInapplicable>
            get() = SubclassOptInInapplicable::class

        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SubclassOptInArgumentIsNotMarker : KaFirDiagnostic<KtClassLiteralExpression> {
        override val diagnosticClass: KClass<SubclassOptInArgumentIsNotMarker>
            get() = SubclassOptInArgumentIsNotMarker::class

        public val notMarkerClassId: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedTypealiasExpandedType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExposedTypealiasExpandedType>
            get() = ExposedTypealiasExpandedType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedFunctionReturnType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExposedFunctionReturnType>
            get() = ExposedFunctionReturnType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedReceiverType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedReceiverType>
            get() = ExposedReceiverType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedPropertyType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExposedPropertyType>
            get() = ExposedPropertyType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedPropertyTypeInConstructorError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExposedPropertyTypeInConstructorError>
            get() = ExposedPropertyTypeInConstructorError::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedParameterType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ExposedParameterType>
            get() = ExposedParameterType::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedSuperInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedSuperInterface>
            get() = ExposedSuperInterface::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedSuperClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedSuperClass>
            get() = ExposedSuperClass::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedTypeParameterBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedTypeParameterBound>
            get() = ExposedTypeParameterBound::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedTypeParameterBoundDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedTypeParameterBoundDeprecationWarning>
            get() = ExposedTypeParameterBoundDeprecationWarning::class

        public val elementVisibility: EffectiveVisibility
        public val restrictingDeclaration: KaClassLikeSymbol
        public val relationToType: RelationToType
        public val restrictingVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatedModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RepeatedModifier>
            get() = RepeatedModifier::class

        public val modifier: KtModifierKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongModifierTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongModifierTarget>
            get() = WrongModifierTarget::class

        public val modifier: KtModifierKeywordToken
        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongModifierContainingDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongModifierContainingDeclaration>
            get() = WrongModifierContainingDeclaration::class

        public val modifier: KtModifierKeywordToken
        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedModifier>
            get() = DeprecatedModifier::class

        public val deprecatedModifier: KtModifierKeywordToken
        public val actualModifier: KtModifierKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedModifierForTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedModifierForTarget>
            get() = DeprecatedModifierForTarget::class

        public val deprecatedModifier: KtModifierKeywordToken
        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedModifierContainingDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedModifierContainingDeclaration>
            get() = DeprecatedModifierContainingDeclaration::class

        public val modifier: KtModifierKeywordToken
        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncompatibleModifiers : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IncompatibleModifiers>
            get() = IncompatibleModifiers::class

        public val modifier1: KtModifierKeywordToken
        public val modifier2: KtModifierKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedModifierPair : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedModifierPair>
            get() = DeprecatedModifierPair::class

        public val deprecatedModifier: KtModifierKeywordToken
        public val conflictingModifier: KtModifierKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantModifier>
            get() = RedundantModifier::class

        public val redundantModifier: KtModifierKeywordToken
        public val conflictingModifier: KtModifierKeywordToken
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantModifierForTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantModifierForTarget>
            get() = RedundantModifierForTarget::class

        public val redundantModifier: KtModifierKeywordToken
        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InfixModifierRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InfixModifierRequired>
            get() = InfixModifierRequired::class

        public val functionSymbol: KaFunctionSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OperatorModifierRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OperatorModifierRequired>
            get() = OperatorModifierRequired::class

        public val functionSymbol: KaFunctionSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableInfixModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableInfixModifier>
            get() = InapplicableInfixModifier::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableOperatorModifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableOperatorModifier>
            get() = InapplicableOperatorModifier::class

        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableOperatorModifierWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableOperatorModifierWarning>
            get() = InapplicableOperatorModifierWarning::class

        public val message: String
        public val deprecatingFeature: LanguageFeature
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableLateinitModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<InapplicableLateinitModifier>
            get() = InapplicableLateinitModifier::class

        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PotentiallyNullableReturnTypeOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<PotentiallyNullableReturnTypeOfOperatorOf>
            get() = PotentiallyNullableReturnTypeOfOperatorOf::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullableReturnTypeOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<NullableReturnTypeOfOperatorOf>
            get() = NullableReturnTypeOfOperatorOf::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnTypeMismatchOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<ReturnTypeMismatchOfOperatorOf>
            get() = ReturnTypeMismatchOfOperatorOf::class

        public val outerClass: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoVarargOverloadOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<NoVarargOverloadOfOperatorOf>
            get() = NoVarargOverloadOfOperatorOf::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleVarargOverloadsOfOperatorOf : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<MultipleVarargOverloadsOfOperatorOf>
            get() = MultipleVarargOverloadsOfOperatorOf::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InconsistentReturnTypesInOfOverloads : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InconsistentReturnTypesInOfOverloads>
            get() = InconsistentReturnTypesInOfOverloads::class

        public val mainOverloadType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InconsistentParameterTypesInOfOverloads : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InconsistentParameterTypesInOfOverloads>
            get() = InconsistentParameterTypesInOfOverloads::class

        public val mainParameterType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InconsistentVisibilityInOfOverloads : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InconsistentVisibilityInOfOverloads>
            get() = InconsistentVisibilityInOfOverloads::class

        public val mainVisibility: Visibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InconsistentSuspendInOfOverloads : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InconsistentSuspendInOfOverloads>
            get() = InconsistentSuspendInOfOverloads::class

        public val overloadSuspendability: String
        public val mainOverloadSuspendability: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OfOverloadsInBlockAndObject : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<OfOverloadsInBlockAndObject>
            get() = OfOverloadsInBlockAndObject::class

        public val overloadOrigin: String
        public val mainOrigin: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InconsistentTypeParametersInOfOverloads : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InconsistentTypeParametersInOfOverloads>
            get() = InconsistentTypeParametersInOfOverloads::class

        public val mainOverload: KaFunctionSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantOpenInInterface : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<RedundantOpenInInterface>
            get() = RedundantOpenInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OperatorCallOnConstructor : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OperatorCallOnConstructor>
            get() = OperatorCallOnConstructor::class

        public val name: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoExplicitVisibilityInApiMode : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NoExplicitVisibilityInApiMode>
            get() = NoExplicitVisibilityInApiMode::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoExplicitVisibilityInApiModeWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NoExplicitVisibilityInApiModeWarning>
            get() = NoExplicitVisibilityInApiModeWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoExplicitReturnTypeInApiMode : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NoExplicitReturnTypeInApiMode>
            get() = NoExplicitReturnTypeInApiMode::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoExplicitReturnTypeInApiModeWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NoExplicitReturnTypeInApiModeWarning>
            get() = NoExplicitReturnTypeInApiModeWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnonymousSuspendFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<AnonymousSuspendFunction>
            get() = AnonymousSuspendFunction::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassNotTopLevel : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ValueClassNotTopLevel>
            get() = ValueClassNotTopLevel::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassNotFinal : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ValueClassNotFinal>
            get() = ValueClassNotFinal::class

        public val prefix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassOpen : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ValueClassOpen>
            get() = ValueClassOpen::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbsenceOfPrimaryConstructorForValueClass : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<AbsenceOfPrimaryConstructorForValueClass>
            get() = AbsenceOfPrimaryConstructorForValueClass::class

        public val modifier: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectValueClassWithNoPrimaryConstructorHasSecondary : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExpectValueClassWithNoPrimaryConstructorHasSecondary>
            get() = ExpectValueClassWithNoPrimaryConstructorHasSecondary::class

        public val modifier: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlineClassConstructorWrongParametersSize : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InlineClassConstructorWrongParametersSize>
            get() = InlineClassConstructorWrongParametersSize::class

        public val prefix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassEmptyConstructor : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassEmptyConstructor>
            get() = ValueClassEmptyConstructor::class

        public val prefix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassConstructorNotFinalReadOnlyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValueClassConstructorNotFinalReadOnlyParameter>
            get() = ValueClassConstructorNotFinalReadOnlyParameter::class

        public val prefix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractValueClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<AbstractValueClassConstructorPropertyParameter>
            get() = AbstractValueClassConstructorPropertyParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SealedValueClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<SealedValueClassConstructorPropertyParameter>
            get() = SealedValueClassConstructorPropertyParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyWithBackingFieldInsideValueClass : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<PropertyWithBackingFieldInsideValueClass>
            get() = PropertyWithBackingFieldInsideValueClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegatedPropertyInsideValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegatedPropertyInsideValueClass>
            get() = DelegatedPropertyInsideValueClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassHasInapplicableParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassHasInapplicableParameterType>
            get() = ValueClassHasInapplicableParameterType::class

        public val type: KaType
        public val prefix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassCannotImplementInterfaceByDelegation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ValueClassCannotImplementInterfaceByDelegation>
            get() = ValueClassCannotImplementInterfaceByDelegation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassCannotExtendClasses : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotExtendClasses>
            get() = ValueClassCannotExtendClasses::class

        public val prefix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassCannotExtendIdentityClasses : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotExtendIdentityClasses>
            get() = ValueClassCannotExtendIdentityClasses::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassCannotBeRecursive : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotBeRecursive>
            get() = ValueClassCannotBeRecursive::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassCannotBeRecursiveViaTypeParametersError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotBeRecursiveViaTypeParametersError>
            get() = ValueClassCannotBeRecursiveViaTypeParametersError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassCannotBeRecursiveViaTypeParametersWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ValueClassCannotBeRecursiveViaTypeParametersWarning>
            get() = ValueClassCannotBeRecursiveViaTypeParametersWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SecondaryConstructorWithBodyInsideValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SecondaryConstructorWithBodyInsideValueClass>
            get() = SecondaryConstructorWithBodyInsideValueClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReservedMemberInsideValueClass : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<ReservedMemberInsideValueClass>
            get() = ReservedMemberInsideValueClass::class

        public val name: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReservedMemberFromInterfaceInsideValueClass : KaFirDiagnostic<KtClass> {
        override val diagnosticClass: KClass<ReservedMemberFromInterfaceInsideValueClass>
            get() = ReservedMemberFromInterfaceInsideValueClass::class

        public val interfaceName: String
        public val methodName: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeArgumentOnTypedValueClassEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypeArgumentOnTypedValueClassEquals>
            get() = TypeArgumentOnTypedValueClassEquals::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InnerClassInsideValueClass : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InnerClassInsideValueClass>
            get() = InnerClassInsideValueClass::class

        public val prefix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassCannotBeCloneable : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ValueClassCannotBeCloneable>
            get() = ValueClassCannotBeCloneable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoneApplicable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoneApplicable>
            get() = NoneApplicable::class

        public val candidates: List<Pair<KaSymbol, List<String>>>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableCandidate : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableCandidate>
            get() = InapplicableCandidate::class

        public val candidate: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface HasNextFunctionNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<HasNextFunctionNoneApplicable>
            get() = HasNextFunctionNoneApplicable::class

        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NextNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NextNoneApplicable>
            get() = NextNoneApplicable::class

        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegateSpecialFunctionNoneApplicable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegateSpecialFunctionNoneApplicable>
            get() = DelegateSpecialFunctionNoneApplicable::class

        public val expectedFunctionSignature: String
        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeInferenceOnlyInputTypesError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeInferenceOnlyInputTypesError>
            get() = TypeInferenceOnlyInputTypesError::class

        public val typeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MemberProjectedOut : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MemberProjectedOut>
            get() = MemberProjectedOut::class

        public val receiver: KaType
        public val projection: String
        public val symbol: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoValueForParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NoValueForParameter>
            get() = NoValueForParameter::class

        public val violatedParameter: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TooManyArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TooManyArguments>
            get() = TooManyArguments::class

        public val function: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NamedParameterNotFound : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass: KClass<NamedParameterNotFound>
            get() = NamedParameterNotFound::class

        public val name: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NameForAmbiguousParameter : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass: KClass<NameForAmbiguousParameter>
            get() = NameForAmbiguousParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ArgumentPassedTwice : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass: KClass<ArgumentPassedTwice>
            get() = ArgumentPassedTwice::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NamedArgumentsNotAllowed : KaFirDiagnostic<KtValueArgument> {
        override val diagnosticClass: KClass<NamedArgumentsNotAllowed>
            get() = NamedArgumentsNotAllowed::class

        public val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MixingNamedAndPositionalArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MixingNamedAndPositionalArguments>
            get() = MixingNamedAndPositionalArguments::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarargOutsideParentheses : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<VarargOutsideParentheses>
            get() = VarargOutsideParentheses::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonVarargSpread : KaFirDiagnostic<LeafPsiElement> {
        override val diagnosticClass: KClass<NonVarargSpread>
            get() = NonVarargSpread::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SpreadOfNullable : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SpreadOfNullable>
            get() = SpreadOfNullable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnexpectedTrailingLambdaOnANewLine : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnexpectedTrailingLambdaOnANewLine>
            get() = UnexpectedTrailingLambdaOnANewLine::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ManyLambdaExpressionArguments : KaFirDiagnostic<KtLambdaExpression> {
        override val diagnosticClass: KClass<ManyLambdaExpressionArguments>
            get() = ManyLambdaExpressionArguments::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssigningSingleElementToVarargInNamedFormFunctionError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssigningSingleElementToVarargInNamedFormFunctionError>
            get() = AssigningSingleElementToVarargInNamedFormFunctionError::class

        public val expectedArrayType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssigningSingleElementToVarargInNamedFormFunctionWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssigningSingleElementToVarargInNamedFormFunctionWarning>
            get() = AssigningSingleElementToVarargInNamedFormFunctionWarning::class

        public val expectedArrayType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssigningSingleElementToVarargInNamedFormAnnotationError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssigningSingleElementToVarargInNamedFormAnnotationError>
            get() = AssigningSingleElementToVarargInNamedFormAnnotationError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssigningSingleElementToVarargInNamedFormAnnotationWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssigningSingleElementToVarargInNamedFormAnnotationWarning>
            get() = AssigningSingleElementToVarargInNamedFormAnnotationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantSpreadOperatorInNamedFormInFunction : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RedundantSpreadOperatorInNamedFormInFunction>
            get() = RedundantSpreadOperatorInNamedFormInFunction::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantSpreadOperatorInNamedFormInAnnotation : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RedundantSpreadOperatorInNamedFormInAnnotation>
            get() = RedundantSpreadOperatorInNamedFormInAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalTypeArgumentForVarargParameterWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IllegalTypeArgumentForVarargParameterWarning>
            get() = IllegalTypeArgumentForVarargParameterWarning::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NestedClassAccessedViaInstanceReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NestedClassAccessedViaInstanceReference>
            get() = NestedClassAccessedViaInstanceReference::class

        public val symbol: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeMismatch>
            get() = TypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ArgumentTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ArgumentTypeMismatch>
            get() = ArgumentTypeMismatch::class

        public val actualType: KaType
        public val expectedType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ReturnTypeMismatch>
            get() = ReturnTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val targetFunction: KaSymbol
        public val isMismatchDueToNullability: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedParameterTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpectedParameterTypeMismatch>
            get() = ExpectedParameterTypeMismatch::class

        public val actualType: KaType
        public val expectedType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InitializerTypeMismatch : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<InitializerTypeMismatch>
            get() = InitializerTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FieldInitializerTypeMismatch : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<FieldInitializerTypeMismatch>
            get() = FieldInitializerTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssignmentTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssignmentTypeMismatch>
            get() = AssignmentTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConditionTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConditionTypeMismatch>
            get() = ConditionTypeMismatch::class

        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ThrowableTypeMismatch : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ThrowableTypeMismatch>
            get() = ThrowableTypeMismatch::class

        public val actualType: KaType
        public val isMismatchDueToNullability: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ResultTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ResultTypeMismatch>
            get() = ResultTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompareToTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CompareToTypeMismatch>
            get() = CompareToTypeMismatch::class

        public val actualType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface HasNextFunctionTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<HasNextFunctionTypeMismatch>
            get() = HasNextFunctionTypeMismatch::class

        public val actualType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ComponentFunctionReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ComponentFunctionReturnTypeMismatch>
            get() = ComponentFunctionReturnTypeMismatch::class

        public val componentFunctionName: Name
        public val destructingType: KaType
        public val expectedType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegateSpecialFunctionReturnTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegateSpecialFunctionReturnTypeMismatch>
            get() = DelegateSpecialFunctionReturnTypeMismatch::class

        public val delegateFunction: String
        public val expectedType: KaType
        public val actualType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverloadResolutionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OverloadResolutionAmbiguity>
            get() = OverloadResolutionAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssignOperatorAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AssignOperatorAmbiguity>
            get() = AssignOperatorAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IteratorAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IteratorAmbiguity>
            get() = IteratorAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface HasNextFunctionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<HasNextFunctionAmbiguity>
            get() = HasNextFunctionAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NextAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NextAmbiguity>
            get() = NextAmbiguity::class

        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ComponentFunctionAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ComponentFunctionAmbiguity>
            get() = ComponentFunctionAmbiguity::class

        public val functionWithAmbiguityName: Name
        public val candidates: List<KaSymbol>
        public val destructingType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegateSpecialFunctionAmbiguity : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegateSpecialFunctionAmbiguity>
            get() = DelegateSpecialFunctionAmbiguity::class

        public val expectedFunctionSignature: String
        public val candidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompilerRequiredAnnotationAmbiguity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompilerRequiredAnnotationAmbiguity>
            get() = CompilerRequiredAnnotationAmbiguity::class

        public val typeFromCompilerPhase: KaType
        public val typeFromTypesPhase: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousFunctionTypeKind : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AmbiguousFunctionTypeKind>
            get() = AmbiguousFunctionTypeKind::class

        public val kinds: List<FunctionTypeKind>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ContextSensitiveResolutionAmbiguity : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContextSensitiveResolutionAmbiguity>
            get() = ContextSensitiveResolutionAmbiguity::class

        public val resolvedCandidate: KaSymbol
        public val contextSensitiveCandidates: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoContextArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NoContextArgument>
            get() = NoContextArgument::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousContextArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<AmbiguousContextArgument>
            get() = AmbiguousContextArgument::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ContextualOverloadShadowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContextualOverloadShadowed>
            get() = ContextualOverloadShadowed::class

        public val symbols: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleContextLists : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleContextLists>
            get() = MultipleContextLists::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ContextParameterWithoutName : KaFirDiagnostic<KtContextReceiver> {
        override val diagnosticClass: KClass<ContextParameterWithoutName>
            get() = ContextParameterWithoutName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ContextParametersWithBackingField : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContextParametersWithBackingField>
            get() = ContextParametersWithBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallableReferenceToContextualDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallableReferenceToContextualDeclaration>
            get() = CallableReferenceToContextualDeclaration::class

        public val symbol: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NamedContextParameterInFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NamedContextParameterInFunctionType>
            get() = NamedContextParameterInFunctionType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ContextParameterWithDefault : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContextParameterWithDefault>
            get() = ContextParameterWithDefault::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedContextualDeclarationCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnsupportedContextualDeclarationCall>
            get() = UnsupportedContextualDeclarationCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousCallWithImplicitContextReceiver : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<AmbiguousCallWithImplicitContextReceiver>
            get() = AmbiguousCallWithImplicitContextReceiver::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CoroutineContextAsContextParameterIsReserved : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CoroutineContextAsContextParameterIsReserved>
            get() = CoroutineContextAsContextParameterIsReserved::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RecursionInImplicitTypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RecursionInImplicitTypes>
            get() = RecursionInImplicitTypes::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InferenceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InferenceError>
            get() = InferenceError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ProjectionOnNonClassTypeArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ProjectionOnNonClassTypeArgument>
            get() = ProjectionOnNonClassTypeArgument::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolated>
            get() = UpperBoundViolated::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
        public val extraMessage: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolatedDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedDeprecationWarning>
            get() = UpperBoundViolatedDeprecationWarning::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
        public val extraMessage: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolatedInTypeOperatorOrParameterBoundsError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypeOperatorOrParameterBoundsError>
            get() = UpperBoundViolatedInTypeOperatorOrParameterBoundsError::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
        public val extraMessage: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolatedInTypeOperatorOrParameterBoundsWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypeOperatorOrParameterBoundsWarning>
            get() = UpperBoundViolatedInTypeOperatorOrParameterBoundsWarning::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
        public val extraMessage: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolatedInTypealiasExpansion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypealiasExpansion>
            get() = UpperBoundViolatedInTypealiasExpansion::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolatedInTypealiasExpansionDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypealiasExpansionDeprecationWarning>
            get() = UpperBoundViolatedInTypealiasExpansionDeprecationWarning::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolatedInLhsOfClassLiteralWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInLhsOfClassLiteralWarning>
            get() = UpperBoundViolatedInLhsOfClassLiteralWarning::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeArgumentsNotAllowed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeArgumentsNotAllowed>
            get() = TypeArgumentsNotAllowed::class

        public val place: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeArgumentsNotAllowedWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeArgumentsNotAllowedWarning>
            get() = TypeArgumentsNotAllowedWarning::class

        public val place: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeArgumentsNotAllowedInPackageQualifierWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeArgumentsNotAllowedInPackageQualifierWarning>
            get() = TypeArgumentsNotAllowedInPackageQualifierWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeArgumentsForOuterClassWhenNestedReferenced : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeArgumentsForOuterClassWhenNestedReferenced>
            get() = TypeArgumentsForOuterClassWhenNestedReferenced::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongNumberOfTypeArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongNumberOfTypeArguments>
            get() = WrongNumberOfTypeArguments::class

        public val expectedCount: Int
        public val owner: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongNumberOfTypeArgumentsWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongNumberOfTypeArgumentsWarning>
            get() = WrongNumberOfTypeArgumentsWarning::class

        public val expectedCount: Int
        public val owner: KaSymbol
        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongNumberOfTypeArgumentsInLocalClassInLhsWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongNumberOfTypeArgumentsInLocalClassInLhsWarning>
            get() = WrongNumberOfTypeArgumentsInLocalClassInLhsWarning::class

        public val expectedCount: Int
        public val owner: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongNumberOfTypeArgumentsInGetClassWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongNumberOfTypeArgumentsInGetClassWarning>
            get() = WrongNumberOfTypeArgumentsInGetClassWarning::class

        public val expectedCount: Int
        public val owner: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidQualifierInLhsOfCallableReferenceToStaticError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidQualifierInLhsOfCallableReferenceToStaticError>
            get() = InvalidQualifierInLhsOfCallableReferenceToStaticError::class

        public val kind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidQualifierInLhsOfCallableReferenceToStaticWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidQualifierInLhsOfCallableReferenceToStaticWarning>
            get() = InvalidQualifierInLhsOfCallableReferenceToStaticWarning::class

        public val kind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoTypeArgumentsOnRhs : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoTypeArgumentsOnRhs>
            get() = NoTypeArgumentsOnRhs::class

        public val expectedCount: Int
        public val classifier: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OuterClassArgumentsRequired : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OuterClassArgumentsRequired>
            get() = OuterClassArgumentsRequired::class

        public val outer: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParametersInObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParametersInObject>
            get() = TypeParametersInObject::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParametersInAnonymousObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParametersInAnonymousObject>
            get() = TypeParametersInAnonymousObject::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalProjectionUsage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalProjectionUsage>
            get() = IllegalProjectionUsage::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParametersInEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParametersInEnum>
            get() = TypeParametersInEnum::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConflictingProjection : KaFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass: KClass<ConflictingProjection>
            get() = ConflictingProjection::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConflictingProjectionInTypealiasExpansion : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConflictingProjectionInTypealiasExpansion>
            get() = ConflictingProjectionInTypealiasExpansion::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConflictingProjectionInCallableReferenceWarning : KaFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass: KClass<ConflictingProjectionInCallableReferenceWarning>
            get() = ConflictingProjectionInCallableReferenceWarning::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantProjection : KaFirDiagnostic<KtTypeProjection> {
        override val diagnosticClass: KClass<RedundantProjection>
            get() = RedundantProjection::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarianceOnTypeParameterNotAllowed : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<VarianceOnTypeParameterNotAllowed>
            get() = VarianceOnTypeParameterNotAllowed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CatchParameterWithDefaultValue : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CatchParameterWithDefaultValue>
            get() = CatchParameterWithDefaultValue::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParameterInCatchClause : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParameterInCatchClause>
            get() = TypeParameterInCatchClause::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface GenericThrowableSubclass : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<GenericThrowableSubclass>
            get() = GenericThrowableSubclass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InnerClassOfGenericThrowableSubclass : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<InnerClassOfGenericThrowableSubclass>
            get() = InnerClassOfGenericThrowableSubclass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface KclassWithNullableTypeParameterInSignature : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<KclassWithNullableTypeParameterInSignature>
            get() = KclassWithNullableTypeParameterInSignature::class

        public val typeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParameterAsReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParameterAsReified>
            get() = TypeParameterAsReified::class

        public val typeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParameterAsReifiedDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParameterAsReifiedDeprecationWarning>
            get() = TypeParameterAsReifiedDeprecationWarning::class

        public val typeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParameterAsReifiedArrayError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeParameterAsReifiedArrayError>
            get() = TypeParameterAsReifiedArrayError::class

        public val typeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReifiedTypeForbiddenSubstitution : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ReifiedTypeForbiddenSubstitution>
            get() = ReifiedTypeForbiddenSubstitution::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DefinitelyNonNullableAsReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DefinitelyNonNullableAsReified>
            get() = DefinitelyNonNullableAsReified::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeIntersectionAsReifiedError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeIntersectionAsReifiedError>
            get() = TypeIntersectionAsReifiedError::class

        public val typeParameter: KaTypeParameterSymbol
        public val types: List<KaType>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeIntersectionAsReifiedWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeIntersectionAsReifiedWarning>
            get() = TypeIntersectionAsReifiedWarning::class

        public val typeParameter: KaTypeParameterSymbol
        public val types: List<KaType>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeIntersectionAsReifiedDeprecationWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeIntersectionAsReifiedDeprecationWarning>
            get() = TypeIntersectionAsReifiedDeprecationWarning::class

        public val typeParameter: KaTypeParameterSymbol
        public val types: List<KaType>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FinalUpperBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<FinalUpperBound>
            get() = FinalUpperBound::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundIsExtensionOrContextFunctionType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UpperBoundIsExtensionOrContextFunctionType>
            get() = UpperBoundIsExtensionOrContextFunctionType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface BoundsNotAllowedIfBoundedByTypeParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<BoundsNotAllowedIfBoundedByTypeParameter>
            get() = BoundsNotAllowedIfBoundedByTypeParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OnlyOneClassBoundAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<OnlyOneClassBoundAllowed>
            get() = OnlyOneClassBoundAllowed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatedBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RepeatedBound>
            get() = RepeatedBound::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConflictingUpperBounds : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ConflictingUpperBounds>
            get() = ConflictingUpperBounds::class

        public val typeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NameInConstraintIsNotATypeParameter : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass: KClass<NameInConstraintIsNotATypeParameter>
            get() = NameInConstraintIsNotATypeParameter::class

        public val typeParameterName: Name
        public val typeParametersOwner: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface BoundOnTypeAliasParameterNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<BoundOnTypeAliasParameterNotAllowed>
            get() = BoundOnTypeAliasParameterNotAllowed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReifiedTypeParameterNoInline : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<ReifiedTypeParameterNoInline>
            get() = ReifiedTypeParameterNoInline::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReifiedTypeParameterOnAliasError : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<ReifiedTypeParameterOnAliasError>
            get() = ReifiedTypeParameterOnAliasError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReifiedTypeParameterOnAliasWarning : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<ReifiedTypeParameterOnAliasWarning>
            get() = ReifiedTypeParameterOnAliasWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParametersNotAllowed : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<TypeParametersNotAllowed>
            get() = TypeParametersNotAllowed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncorrectTypeParameterOfProperty : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<IncorrectTypeParameterOfProperty>
            get() = IncorrectTypeParameterOfProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplicitNothingReturnType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ImplicitNothingReturnType>
            get() = ImplicitNothingReturnType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplicitNothingPropertyType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ImplicitNothingPropertyType>
            get() = ImplicitNothingPropertyType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbbreviatedNothingReturnType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AbbreviatedNothingReturnType>
            get() = AbbreviatedNothingReturnType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbbreviatedNothingPropertyType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AbbreviatedNothingPropertyType>
            get() = AbbreviatedNothingPropertyType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CyclicGenericUpperBound : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CyclicGenericUpperBound>
            get() = CyclicGenericUpperBound::class

        public val typeParameters: List<KaTypeParameterSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FiniteBoundsViolation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FiniteBoundsViolation>
            get() = FiniteBoundsViolation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FiniteBoundsViolationInJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FiniteBoundsViolationInJava>
            get() = FiniteBoundsViolationInJava::class

        public val containingTypes: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpansiveInheritance : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpansiveInheritance>
            get() = ExpansiveInheritance::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpansiveInheritanceInJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpansiveInheritanceInJava>
            get() = ExpansiveInheritanceInJava::class

        public val containingTypes: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedTypeParameterSyntax : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<DeprecatedTypeParameterSyntax>
            get() = DeprecatedTypeParameterSyntax::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MisplacedTypeParameterConstraints : KaFirDiagnostic<KtTypeParameter> {
        override val diagnosticClass: KClass<MisplacedTypeParameterConstraints>
            get() = MisplacedTypeParameterConstraints::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DynamicSupertype : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DynamicSupertype>
            get() = DynamicSupertype::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DynamicUpperBound : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DynamicUpperBound>
            get() = DynamicUpperBound::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DynamicReceiverNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DynamicReceiverNotAllowed>
            get() = DynamicReceiverNotAllowed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DynamicReceiverExpectedButWasNonDynamic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DynamicReceiverExpectedButWasNonDynamic>
            get() = DynamicReceiverExpectedButWasNonDynamic::class

        public val actualType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncompatibleTypes : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncompatibleTypes>
            get() = IncompatibleTypes::class

        public val typeA: KaType
        public val typeB: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncompatibleTypesWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncompatibleTypesWarning>
            get() = IncompatibleTypesWarning::class

        public val typeA: KaType
        public val typeB: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeVarianceConflictError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeVarianceConflictError>
            get() = TypeVarianceConflictError::class

        public val typeParameter: KaTypeParameterSymbol
        public val typeParameterVariance: Variance
        public val variance: Variance
        public val containingType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeVarianceConflictInExpandedType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeVarianceConflictInExpandedType>
            get() = TypeVarianceConflictInExpandedType::class

        public val typeParameter: KaTypeParameterSymbol
        public val typeParameterVariance: Variance
        public val variance: Variance
        public val containingType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SmartcastImpossible : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<SmartcastImpossible>
            get() = SmartcastImpossible::class

        public val desiredType: KaType
        public val subject: KtExpression
        public val description: String
        public val isCastToNotNull: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SmartcastImpossibleOnImplicitInvokeReceiver : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<SmartcastImpossibleOnImplicitInvokeReceiver>
            get() = SmartcastImpossibleOnImplicitInvokeReceiver::class

        public val desiredType: KaType
        public val subject: KtExpression
        public val description: String
        public val isCastToNotNull: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedSmartcastOnDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DeprecatedSmartcastOnDelegatedProperty>
            get() = DeprecatedSmartcastOnDelegatedProperty::class

        public val desiredType: KaType
        public val property: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PlatformClassMappedToKotlin : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PlatformClassMappedToKotlin>
            get() = PlatformClassMappedToKotlin::class

        public val kotlinClass: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InferredTypeVariableIntoEmptyIntersectionError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InferredTypeVariableIntoEmptyIntersectionError>
            get() = InferredTypeVariableIntoEmptyIntersectionError::class

        public val typeVariableDescription: String
        public val incompatibleTypes: List<KaType>
        public val description: String
        public val causingTypes: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InferredTypeVariableIntoEmptyIntersectionWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InferredTypeVariableIntoEmptyIntersectionWarning>
            get() = InferredTypeVariableIntoEmptyIntersectionWarning::class

        public val typeVariableDescription: String
        public val incompatibleTypes: List<KaType>
        public val description: String
        public val causingTypes: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InferredTypeVariableIntoPossibleEmptyIntersection : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InferredTypeVariableIntoPossibleEmptyIntersection>
            get() = InferredTypeVariableIntoPossibleEmptyIntersection::class

        public val typeVariableDescription: String
        public val incompatibleTypes: List<KaType>
        public val description: String
        public val causingTypes: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncorrectLeftComponentOfIntersection : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncorrectLeftComponentOfIntersection>
            get() = IncorrectLeftComponentOfIntersection::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncorrectRightComponentOfIntersection : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncorrectRightComponentOfIntersection>
            get() = IncorrectRightComponentOfIntersection::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullableOnDefinitelyNotNullable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NullableOnDefinitelyNotNullable>
            get() = NullableOnDefinitelyNotNullable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantNullable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RedundantNullable>
            get() = RedundantNullable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InferredInvisibleReifiedTypeArgumentWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InferredInvisibleReifiedTypeArgumentWarning>
            get() = InferredInvisibleReifiedTypeArgumentWarning::class

        public val typeParameter: KaTypeParameterSymbol
        public val typeArgumentType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InferredInvisibleVarargTypeArgumentWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InferredInvisibleVarargTypeArgumentWarning>
            get() = InferredInvisibleVarargTypeArgumentWarning::class

        public val typeParameter: KaTypeParameterSymbol
        public val typeArgumentType: KaType
        public val valueParameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InferredInvisibleReturnTypeWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InferredInvisibleReturnTypeWarning>
            get() = InferredInvisibleReturnTypeWarning::class

        public val calleeSymbol: KaSymbol
        public val returnType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface GenericQualifierOnConstructorCallError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<GenericQualifierOnConstructorCallError>
            get() = GenericQualifierOnConstructorCallError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface GenericQualifierOnConstructorCallWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<GenericQualifierOnConstructorCallWarning>
            get() = GenericQualifierOnConstructorCallWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AtomicRefWithoutConsistentIdentity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AtomicRefWithoutConsistentIdentity>
            get() = AtomicRefWithoutConsistentIdentity::class

        public val atomicRef: ClassId
        public val argumentType: KaType
        public val suggestedType: ClassId?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AtomicRefCallArgumentWithoutConsistentIdentity : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AtomicRefCallArgumentWithoutConsistentIdentity>
            get() = AtomicRefCallArgumentWithoutConsistentIdentity::class

        public val argumentType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExtensionInClassReferenceNotAllowed : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExtensionInClassReferenceNotAllowed>
            get() = ExtensionInClassReferenceNotAllowed::class

        public val referencedDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallableReferenceLhsNotAClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CallableReferenceLhsNotAClass>
            get() = CallableReferenceLhsNotAClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallableReferenceToAnnotationConstructor : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CallableReferenceToAnnotationConstructor>
            get() = CallableReferenceToAnnotationConstructor::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AdaptedCallableReferenceAgainstReflectionType : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AdaptedCallableReferenceAgainstReflectionType>
            get() = AdaptedCallableReferenceAgainstReflectionType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ClassLiteralLhsNotAClass : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ClassLiteralLhsNotAClass>
            get() = ClassLiteralLhsNotAClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ClassLiteralLhsNotAClassWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ClassLiteralLhsNotAClassWarning>
            get() = ClassLiteralLhsNotAClassWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullableTypeInClassLiteralLhs : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NullableTypeInClassLiteralLhs>
            get() = NullableTypeInClassLiteralLhs::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpressionOfNullableTypeInClassLiteralLhs : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpressionOfNullableTypeInClassLiteralLhs>
            get() = ExpressionOfNullableTypeInClassLiteralLhs::class

        public val lhsType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpressionOfNullableTypeInClassLiteralLhsWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpressionOfNullableTypeInClassLiteralLhsWarning>
            get() = ExpressionOfNullableTypeInClassLiteralLhsWarning::class

        public val lhsType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedClassLiteralsWithEmptyLhs : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnsupportedClassLiteralsWithEmptyLhs>
            get() = UnsupportedClassLiteralsWithEmptyLhs::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedArrayOfNothingInClassLiteralLhs : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsupportedArrayOfNothingInClassLiteralLhs>
            get() = UnsupportedArrayOfNothingInClassLiteralLhs::class

        public val unsupported: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MutablePropertyWithCapturedType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MutablePropertyWithCapturedType>
            get() = MutablePropertyWithCapturedType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsupportedReflectionApi : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnsupportedReflectionApi>
            get() = UnsupportedReflectionApi::class

        public val unsupportedReflectionAPI: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NothingToOverride : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<NothingToOverride>
            get() = NothingToOverride::class

        public val declaration: KaCallableSymbol
        public val candidates: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotOverrideInvisibleMember : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<CannotOverrideInvisibleMember>
            get() = CannotOverrideInvisibleMember::class

        public val overridingMember: KaCallableSymbol
        public val baseMember: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassOverrideConflict : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<DataClassOverrideConflict>
            get() = DataClassOverrideConflict::class

        public val overridingMember: KaCallableSymbol
        public val baseMember: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataClassOverrideDefaultValues : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DataClassOverrideDefaultValues>
            get() = DataClassOverrideDefaultValues::class

        public val overridingMember: KaCallableSymbol
        public val baseType: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotWeakenAccessPrivilege : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<CannotWeakenAccessPrivilege>
            get() = CannotWeakenAccessPrivilege::class

        public val overridingVisibility: Visibility
        public val overridden: KaCallableSymbol
        public val containingClassName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotWeakenAccessPrivilegeWarning : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<CannotWeakenAccessPrivilegeWarning>
            get() = CannotWeakenAccessPrivilegeWarning::class

        public val overridingVisibility: Visibility
        public val overridden: KaCallableSymbol
        public val containingClassName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotChangeAccessPrivilege : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<CannotChangeAccessPrivilege>
            get() = CannotChangeAccessPrivilege::class

        public val overridingVisibility: Visibility
        public val overridden: KaCallableSymbol
        public val containingClassName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotChangeAccessPrivilegeWarning : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<CannotChangeAccessPrivilegeWarning>
            get() = CannotChangeAccessPrivilegeWarning::class

        public val overridingVisibility: Visibility
        public val overridden: KaCallableSymbol
        public val containingClassName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotInferVisibility : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CannotInferVisibility>
            get() = CannotInferVisibility::class

        public val callable: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotInferVisibilityWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CannotInferVisibilityWarning>
            get() = CannotInferVisibilityWarning::class

        public val callable: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleDefaultsInheritedFromSupertypes : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypes>
            get() = MultipleDefaultsInheritedFromSupertypes::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride>
            get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverride::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleDefaultsInheritedFromSupertypesDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesDeprecationError>
            get() = MultipleDefaultsInheritedFromSupertypesDeprecationError::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleDefaultsInheritedFromSupertypesDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesDeprecationWarning>
            get() = MultipleDefaultsInheritedFromSupertypesDeprecationWarning::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError>
            get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationError::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning>
            get() = MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarning::class

        public val name: Name
        public val valueParameter: KaSymbol
        public val baseFunctions: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasExpandsToArrayOfNothings : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasExpandsToArrayOfNothings>
            get() = TypealiasExpandsToArrayOfNothings::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverridingFinalMember : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<OverridingFinalMember>
            get() = OverridingFinalMember::class

        public val overriddenDeclaration: KaCallableSymbol
        public val containingClassName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ReturnTypeMismatchOnOverride>
            get() = ReturnTypeMismatchOnOverride::class

        public val function: KaCallableSymbol
        public val superFunction: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<PropertyTypeMismatchOnOverride>
            get() = PropertyTypeMismatchOnOverride::class

        public val property: KaCallableSymbol
        public val superProperty: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarTypeMismatchOnOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VarTypeMismatchOnOverride>
            get() = VarTypeMismatchOnOverride::class

        public val variable: KaCallableSymbol
        public val superVariable: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ReturnTypeMismatchOnInheritance>
            get() = ReturnTypeMismatchOnInheritance::class

        public val conflictingDeclaration1: KaCallableSymbol
        public val conflictingDeclaration2: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<PropertyTypeMismatchOnInheritance>
            get() = PropertyTypeMismatchOnInheritance::class

        public val conflictingDeclaration1: KaCallableSymbol
        public val conflictingDeclaration2: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarTypeMismatchOnInheritance : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<VarTypeMismatchOnInheritance>
            get() = VarTypeMismatchOnInheritance::class

        public val conflictingDeclaration1: KaCallableSymbol
        public val conflictingDeclaration2: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnTypeMismatchByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ReturnTypeMismatchByDelegation>
            get() = ReturnTypeMismatchByDelegation::class

        public val delegateDeclaration: KaCallableSymbol
        public val baseDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyTypeMismatchByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<PropertyTypeMismatchByDelegation>
            get() = PropertyTypeMismatchByDelegation::class

        public val delegateDeclaration: KaCallableSymbol
        public val baseDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarOverriddenByValByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<VarOverriddenByValByDelegation>
            get() = VarOverriddenByValByDelegation::class

        public val delegateDeclaration: KaCallableSymbol
        public val baseDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConflictingInheritedMembers : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ConflictingInheritedMembers>
            get() = ConflictingInheritedMembers::class

        public val owner: KaClassLikeSymbol
        public val conflictingDeclarations: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<AbstractMemberNotImplemented>
            get() = AbstractMemberNotImplemented::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractMemberIncorrectlyDelegatedError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<AbstractMemberIncorrectlyDelegatedError>
            get() = AbstractMemberIncorrectlyDelegatedError::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractMemberIncorrectlyDelegatedWarning : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<AbstractMemberIncorrectlyDelegatedWarning>
            get() = AbstractMemberIncorrectlyDelegatedWarning::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractMemberNotImplementedByEnumEntry : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass: KClass<AbstractMemberNotImplementedByEnumEntry>
            get() = AbstractMemberNotImplementedByEnumEntry::class

        public val enumEntry: KaSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractClassMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<AbstractClassMemberNotImplemented>
            get() = AbstractClassMemberNotImplemented::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclarations: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvisibleAbstractMemberFromSuperError : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<InvisibleAbstractMemberFromSuperError>
            get() = InvisibleAbstractMemberFromSuperError::class

        public val classOrObject: KaClassLikeSymbol
        public val invisibleDeclarations: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousAnonymousTypeInferred : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<AmbiguousAnonymousTypeInferred>
            get() = AmbiguousAnonymousTypeInferred::class

        public val superTypes: List<KaType>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ManyImplMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ManyImplMemberNotImplemented>
            get() = ManyImplMemberNotImplemented::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ManyInterfacesMemberNotImplemented : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ManyInterfacesMemberNotImplemented>
            get() = ManyInterfacesMemberNotImplemented::class

        public val classOrObject: KaClassLikeSymbol
        public val missingDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverridingFinalMemberByDelegation : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<OverridingFinalMemberByDelegation>
            get() = OverridingFinalMemberByDelegation::class

        public val delegatedDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegatedMemberHidesSupertypeOverride : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<DelegatedMemberHidesSupertypeOverride>
            get() = DelegatedMemberHidesSupertypeOverride::class

        public val delegatedDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarOverriddenByVal : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VarOverriddenByVal>
            get() = VarOverriddenByVal::class

        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarImplementedByInheritedValError : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VarImplementedByInheritedValError>
            get() = VarImplementedByInheritedValError::class

        public val classOrObject: KaClassLikeSymbol
        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarImplementedByInheritedValWarning : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VarImplementedByInheritedValWarning>
            get() = VarImplementedByInheritedValWarning::class

        public val classOrObject: KaClassLikeSymbol
        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonFinalMemberInFinalClass : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NonFinalMemberInFinalClass>
            get() = NonFinalMemberInFinalClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonFinalMemberInObject : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NonFinalMemberInObject>
            get() = NonFinalMemberInObject::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VirtualMemberHidden : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VirtualMemberHidden>
            get() = VirtualMemberHidden::class

        public val declared: KaCallableSymbol
        public val overriddenContainer: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ParameterNameChangedOnOverride : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ParameterNameChangedOnOverride>
            get() = ParameterNameChangedOnOverride::class

        public val superType: KaClassLikeSymbol
        public val conflictingParameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DifferentNamesForTheSameParameterInSupertypes : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<DifferentNamesForTheSameParameterInSupertypes>
            get() = DifferentNamesForTheSameParameterInSupertypes::class

        public val currentParameter: KaSymbol
        public val conflictingParameter: KaSymbol
        public val parameterNumber: Int
        public val conflictingFunctions: List<KaFunctionSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SuspendOverriddenByNonSuspend : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass: KClass<SuspendOverriddenByNonSuspend>
            get() = SuspendOverriddenByNonSuspend::class

        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonSuspendOverriddenBySuspend : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass: KClass<NonSuspendOverriddenBySuspend>
            get() = NonSuspendOverriddenBySuspend::class

        public val overridingDeclaration: KaCallableSymbol
        public val overriddenDeclaration: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverridingIgnorableWithMustUse : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<OverridingIgnorableWithMustUse>
            get() = OverridingIgnorableWithMustUse::class

        public val method: KaCallableSymbol
        public val parentClass: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ManyCompanionObjects : KaFirDiagnostic<KtObjectDeclaration> {
        override val diagnosticClass: KClass<ManyCompanionObjects>
            get() = ManyCompanionObjects::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConflictingOverloads : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConflictingOverloads>
            get() = ConflictingOverloads::class

        public val conflictingOverloads: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Redeclaration : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<Redeclaration>
            get() = Redeclaration::class

        public val conflictingDeclarations: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ClassifierRedeclaration : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ClassifierRedeclaration>
            get() = ClassifierRedeclaration::class

        public val conflictingDeclarations: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PackageConflictsWithClassifier : KaFirDiagnostic<KtPackageDirective> {
        override val diagnosticClass: KClass<PackageConflictsWithClassifier>
            get() = PackageConflictsWithClassifier::class

        public val conflictingClassId: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectAndActualInTheSameModule : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectAndActualInTheSameModule>
            get() = ExpectAndActualInTheSameModule::class

        public val declaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MethodOfAnyImplementedInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MethodOfAnyImplementedInInterface>
            get() = MethodOfAnyImplementedInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExtensionShadowedByMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExtensionShadowedByMember>
            get() = ExtensionShadowedByMember::class

        public val member: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExtensionFunctionShadowedByMemberPropertyWithInvoke : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExtensionFunctionShadowedByMemberPropertyWithInvoke>
            get() = ExtensionFunctionShadowedByMemberPropertyWithInvoke::class

        public val member: KaCallableSymbol
        public val invokeOperator: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LocalObjectNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<LocalObjectNotAllowed>
            get() = LocalObjectNotAllowed::class

        public val objectName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LocalInterfaceNotAllowed : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<LocalInterfaceNotAllowed>
            get() = LocalInterfaceNotAllowed::class

        public val interfaceName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractFunctionInNonAbstractClass : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<AbstractFunctionInNonAbstractClass>
            get() = AbstractFunctionInNonAbstractClass::class

        public val function: KaCallableSymbol
        public val containingClass: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractFunctionWithBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<AbstractFunctionWithBody>
            get() = AbstractFunctionWithBody::class

        public val function: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonAbstractFunctionWithNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<NonAbstractFunctionWithNoBody>
            get() = NonAbstractFunctionWithNoBody::class

        public val function: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PrivateFunctionWithNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<PrivateFunctionWithNoBody>
            get() = PrivateFunctionWithNoBody::class

        public val function: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonMemberFunctionNoBody : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<NonMemberFunctionNoBody>
            get() = NonMemberFunctionNoBody::class

        public val function: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunctionDeclarationWithNoName : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<FunctionDeclarationWithNoName>
            get() = FunctionDeclarationWithNoName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnonymousFunctionWithName : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<AnonymousFunctionWithName>
            get() = AnonymousFunctionWithName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SingleAnonymousFunctionWithNameError : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<SingleAnonymousFunctionWithNameError>
            get() = SingleAnonymousFunctionWithNameError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SingleAnonymousFunctionWithNameWarning : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<SingleAnonymousFunctionWithNameWarning>
            get() = SingleAnonymousFunctionWithNameWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnonymousFunctionParameterWithDefaultValue : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<AnonymousFunctionParameterWithDefaultValue>
            get() = AnonymousFunctionParameterWithDefaultValue::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UselessVarargOnParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<UselessVarargOnParameter>
            get() = UselessVarargOnParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleVarargParameters : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<MultipleVarargParameters>
            get() = MultipleVarargParameters::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ForbiddenVarargParameterType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ForbiddenVarargParameterType>
            get() = ForbiddenVarargParameterType::class

        public val varargParameterType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueParameterWithoutExplicitType : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ValueParameterWithoutExplicitType>
            get() = ValueParameterWithoutExplicitType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotInferParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotInferParameterType>
            get() = CannotInferParameterType::class

        public val parameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotInferValueParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotInferValueParameterType>
            get() = CannotInferValueParameterType::class

        public val parameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotInferItParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotInferItParameterType>
            get() = CannotInferItParameterType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotInferReceiverParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotInferReceiverParameterType>
            get() = CannotInferReceiverParameterType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoTailCallsFound : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<NoTailCallsFound>
            get() = NoTailCallsFound::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TailrecOnVirtualMemberError : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<TailrecOnVirtualMemberError>
            get() = TailrecOnVirtualMemberError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonTailRecursiveCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonTailRecursiveCall>
            get() = NonTailRecursiveCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TailRecursionInTryIsNotSupported : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TailRecursionInTryIsNotSupported>
            get() = TailRecursionInTryIsNotSupported::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DataObjectCustomEqualsOrHashCode : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<DataObjectCustomEqualsOrHashCode>
            get() = DataObjectCustomEqualsOrHashCode::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DefaultValueNotAllowedInOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DefaultValueNotAllowedInOverride>
            get() = DefaultValueNotAllowedInOverride::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunInterfaceWrongCountOfAbstractMembers : KaFirDiagnostic<KtClass> {
        override val diagnosticClass: KClass<FunInterfaceWrongCountOfAbstractMembers>
            get() = FunInterfaceWrongCountOfAbstractMembers::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunInterfaceCannotHaveAbstractProperties : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<FunInterfaceCannotHaveAbstractProperties>
            get() = FunInterfaceCannotHaveAbstractProperties::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunInterfaceAbstractMethodWithTypeParameters : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<FunInterfaceAbstractMethodWithTypeParameters>
            get() = FunInterfaceAbstractMethodWithTypeParameters::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunInterfaceAbstractMethodWithDefaultValue : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<FunInterfaceAbstractMethodWithDefaultValue>
            get() = FunInterfaceAbstractMethodWithDefaultValue::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunInterfaceWithSuspendFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<FunInterfaceWithSuspendFunction>
            get() = FunInterfaceWithSuspendFunction::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractPropertyInNonAbstractClass : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<AbstractPropertyInNonAbstractClass>
            get() = AbstractPropertyInNonAbstractClass::class

        public val property: KaCallableSymbol
        public val containingClass: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PrivatePropertyInInterface : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<PrivatePropertyInInterface>
            get() = PrivatePropertyInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractPropertyWithInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AbstractPropertyWithInitializer>
            get() = AbstractPropertyWithInitializer::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyInitializerInInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<PropertyInitializerInInterface>
            get() = PropertyInitializerInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyWithNoTypeNoInitializer : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<PropertyWithNoTypeNoInitializer>
            get() = PropertyWithNoTypeNoInitializer::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractPropertyWithoutType : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<AbstractPropertyWithoutType>
            get() = AbstractPropertyWithoutType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LateinitPropertyWithoutType : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<LateinitPropertyWithoutType>
            get() = LateinitPropertyWithoutType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MustBeInitialized : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitialized>
            get() = MustBeInitialized::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MustBeInitializedWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedWarning>
            get() = MustBeInitializedWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MustBeInitializedOrBeFinal : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrBeFinal>
            get() = MustBeInitializedOrBeFinal::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MustBeInitializedOrBeFinalWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrBeFinalWarning>
            get() = MustBeInitializedOrBeFinalWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MustBeInitializedOrBeAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrBeAbstract>
            get() = MustBeInitializedOrBeAbstract::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MustBeInitializedOrBeAbstractWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrBeAbstractWarning>
            get() = MustBeInitializedOrBeAbstractWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MustBeInitializedOrFinalOrAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrFinalOrAbstract>
            get() = MustBeInitializedOrFinalOrAbstract::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MustBeInitializedOrFinalOrAbstractWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<MustBeInitializedOrFinalOrAbstractWarning>
            get() = MustBeInitializedOrFinalOrAbstractWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExplicitFieldMustBeInitialized : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<ExplicitFieldMustBeInitialized>
            get() = ExplicitFieldMustBeInitialized::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExtensionPropertyMustHaveAccessorsOrBeAbstract : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<ExtensionPropertyMustHaveAccessorsOrBeAbstract>
            get() = ExtensionPropertyMustHaveAccessorsOrBeAbstract::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnnecessaryLateinit : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<UnnecessaryLateinit>
            get() = UnnecessaryLateinit::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface BackingFieldInInterface : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<BackingFieldInInterface>
            get() = BackingFieldInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExtensionPropertyWithBackingField : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExtensionPropertyWithBackingField>
            get() = ExtensionPropertyWithBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyInitializerNoBackingField : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<PropertyInitializerNoBackingField>
            get() = PropertyInitializerNoBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AbstractDelegatedProperty>
            get() = AbstractDelegatedProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegatedPropertyInInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegatedPropertyInInterface>
            get() = DelegatedPropertyInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractPropertyWithGetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass: KClass<AbstractPropertyWithGetter>
            get() = AbstractPropertyWithGetter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractPropertyWithSetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass: KClass<AbstractPropertyWithSetter>
            get() = AbstractPropertyWithSetter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PrivateSetterForAbstractProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<PrivateSetterForAbstractProperty>
            get() = PrivateSetterForAbstractProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PrivateSetterForOpenProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<PrivateSetterForOpenProperty>
            get() = PrivateSetterForOpenProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValWithSetter : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass: KClass<ValWithSetter>
            get() = ValWithSetter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstValNotTopLevelOrObject : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConstValNotTopLevelOrObject>
            get() = ConstValNotTopLevelOrObject::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstValWithGetter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConstValWithGetter>
            get() = ConstValWithGetter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstValWithDelegate : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ConstValWithDelegate>
            get() = ConstValWithDelegate::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeCantBeUsedForConstVal : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<TypeCantBeUsedForConstVal>
            get() = TypeCantBeUsedForConstVal::class

        public val constValType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstValWithoutInitializer : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<ConstValWithoutInitializer>
            get() = ConstValWithoutInitializer::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstValWithEbf : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<ConstValWithEbf>
            get() = ConstValWithEbf::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstValWithNonConstInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ConstValWithNonConstInitializer>
            get() = ConstValWithNonConstInitializer::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegateUsesExtensionPropertyTypeParameterError : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<DelegateUsesExtensionPropertyTypeParameterError>
            get() = DelegateUsesExtensionPropertyTypeParameterError::class

        public val usedTypeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface GetterVisibilityDiffersFromPropertyVisibility : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<GetterVisibilityDiffersFromPropertyVisibility>
            get() = GetterVisibilityDiffersFromPropertyVisibility::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SetterVisibilityInconsistentWithPropertyVisibility : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<SetterVisibilityInconsistentWithPropertyVisibility>
            get() = SetterVisibilityInconsistentWithPropertyVisibility::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongGetterReturnType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongGetterReturnType>
            get() = WrongGetterReturnType::class

        public val expectedType: KaType
        public val actualType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongSetterReturnType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongSetterReturnType>
            get() = WrongSetterReturnType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongSetterParameterType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongSetterParameterType>
            get() = WrongSetterParameterType::class

        public val expectedType: KaType
        public val actualType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AccessorForDelegatedProperty : KaFirDiagnostic<KtPropertyAccessor> {
        override val diagnosticClass: KClass<AccessorForDelegatedProperty>
            get() = AccessorForDelegatedProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyInitializerWithExplicitFieldDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<PropertyInitializerWithExplicitFieldDeclaration>
            get() = PropertyInitializerWithExplicitFieldDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyFieldDeclarationMissingInitializer : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<PropertyFieldDeclarationMissingInitializer>
            get() = PropertyFieldDeclarationMissingInitializer::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LateinitNullableBackingField : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<LateinitNullableBackingField>
            get() = LateinitNullableBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface BackingFieldForDelegatedProperty : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<BackingFieldForDelegatedProperty>
            get() = BackingFieldForDelegatedProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VarPropertyWithExplicitBackingField : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VarPropertyWithExplicitBackingField>
            get() = VarPropertyWithExplicitBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonFinalPropertyWithExplicitBackingField : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<NonFinalPropertyWithExplicitBackingField>
            get() = NonFinalPropertyWithExplicitBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectPropertyWithExplicitBackingField : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExpectPropertyWithExplicitBackingField>
            get() = ExpectPropertyWithExplicitBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InconsistentBackingFieldType : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<InconsistentBackingFieldType>
            get() = InconsistentBackingFieldType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExplicitFieldVisibilityMustBeLessPermissive : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<ExplicitFieldVisibilityMustBeLessPermissive>
            get() = ExplicitFieldVisibilityMustBeLessPermissive::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyWithExplicitFieldAndAccessors : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PropertyWithExplicitFieldAndAccessors>
            get() = PropertyWithExplicitFieldAndAccessors::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExplicitBackingFieldInInterface : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<ExplicitBackingFieldInInterface>
            get() = ExplicitBackingFieldInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExplicitBackingFieldInAbstractProperty : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<ExplicitBackingFieldInAbstractProperty>
            get() = ExplicitBackingFieldInAbstractProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExplicitBackingFieldInExtension : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<ExplicitBackingFieldInExtension>
            get() = ExplicitBackingFieldInExtension::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantExplicitBackingField : KaFirDiagnostic<KtBackingField> {
        override val diagnosticClass: KClass<RedundantExplicitBackingField>
            get() = RedundantExplicitBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AbstractPropertyInPrimaryConstructorParameters : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<AbstractPropertyInPrimaryConstructorParameters>
            get() = AbstractPropertyInPrimaryConstructorParameters::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LocalVariableWithTypeParametersWarning : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<LocalVariableWithTypeParametersWarning>
            get() = LocalVariableWithTypeParametersWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LocalVariableWithTypeParameters : KaFirDiagnostic<KtProperty> {
        override val diagnosticClass: KClass<LocalVariableWithTypeParameters>
            get() = LocalVariableWithTypeParameters::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExplicitTypeArgumentsInPropertyAccess : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExplicitTypeArgumentsInPropertyAccess>
            get() = ExplicitTypeArgumentsInPropertyAccess::class

        public val kind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExplicitTypeArgumentsInPropertyAccessWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExplicitTypeArgumentsInPropertyAccessWarning>
            get() = ExplicitTypeArgumentsInPropertyAccessWarning::class

        public val kind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SafeCallableReferenceCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<SafeCallableReferenceCall>
            get() = SafeCallableReferenceCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LateinitIntrinsicCallOnNonLiteral : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LateinitIntrinsicCallOnNonLiteral>
            get() = LateinitIntrinsicCallOnNonLiteral::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LateinitIntrinsicCallOnNonLateinit : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LateinitIntrinsicCallOnNonLateinit>
            get() = LateinitIntrinsicCallOnNonLateinit::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LateinitIntrinsicCallInInlineFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LateinitIntrinsicCallInInlineFunction>
            get() = LateinitIntrinsicCallInInlineFunction::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LateinitIntrinsicCallOnNonAccessibleProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LateinitIntrinsicCallOnNonAccessibleProperty>
            get() = LateinitIntrinsicCallOnNonAccessibleProperty::class

        public val declaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LocalExtensionProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LocalExtensionProperty>
            get() = LocalExtensionProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnnamedVarProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnnamedVarProperty>
            get() = UnnamedVarProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnnamedDelegatedProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnnamedDelegatedProperty>
            get() = UnnamedDelegatedProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnnamedPropertyWithImplicitIgnorableType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnnamedPropertyWithImplicitIgnorableType>
            get() = UnnamedPropertyWithImplicitIgnorableType::class

        public val ignorableType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DestructuringShortFormNameMismatch : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DestructuringShortFormNameMismatch>
            get() = DestructuringShortFormNameMismatch::class

        public val destructuredName: Name
        public val propertyName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DestructuringShortFormOfNonDataClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DestructuringShortFormOfNonDataClass>
            get() = DestructuringShortFormOfNonDataClass::class

        public val rhsType: KaType
        public val destructuredName: Name
        public val target: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DestructuringShortFormUnderscore : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DestructuringShortFormUnderscore>
            get() = DestructuringShortFormUnderscore::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NameBasedDestructuringUnderscoreWithoutRenaming : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NameBasedDestructuringUnderscoreWithoutRenaming>
            get() = NameBasedDestructuringUnderscoreWithoutRenaming::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedDeclarationWithBody : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExpectedDeclarationWithBody>
            get() = ExpectedDeclarationWithBody::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedClassConstructorDelegationCall : KaFirDiagnostic<KtConstructorDelegationCall> {
        override val diagnosticClass: KClass<ExpectedClassConstructorDelegationCall>
            get() = ExpectedClassConstructorDelegationCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ExpectedClassConstructorPropertyParameter>
            get() = ExpectedClassConstructorPropertyParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedEnumConstructor : KaFirDiagnostic<KtConstructor<*>> {
        override val diagnosticClass: KClass<ExpectedEnumConstructor>
            get() = ExpectedEnumConstructor::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedEnumEntryWithBody : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass: KClass<ExpectedEnumEntryWithBody>
            get() = ExpectedEnumEntryWithBody::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedPropertyInitializer : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExpectedPropertyInitializer>
            get() = ExpectedPropertyInitializer::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedDelegatedProperty : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExpectedDelegatedProperty>
            get() = ExpectedDelegatedProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedLateinitProperty : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ExpectedLateinitProperty>
            get() = ExpectedLateinitProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SupertypeInitializedInExpectedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SupertypeInitializedInExpectedClass>
            get() = SupertypeInitializedInExpectedClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedPrivateDeclaration : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ExpectedPrivateDeclaration>
            get() = ExpectedPrivateDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedExternalDeclaration : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ExpectedExternalDeclaration>
            get() = ExpectedExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedTailrecFunction : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<ExpectedTailrecFunction>
            get() = ExpectedTailrecFunction::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplementationByDelegationInExpectClass : KaFirDiagnostic<KtDelegatedSuperTypeEntry> {
        override val diagnosticClass: KClass<ImplementationByDelegationInExpectClass>
            get() = ImplementationByDelegationInExpectClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualTypeAliasNotToClass : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasNotToClass>
            get() = ActualTypeAliasNotToClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualTypeAliasToClassWithDeclarationSiteVariance : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasToClassWithDeclarationSiteVariance>
            get() = ActualTypeAliasToClassWithDeclarationSiteVariance::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualTypeAliasWithUseSiteVariance : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasWithUseSiteVariance>
            get() = ActualTypeAliasWithUseSiteVariance::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualTypeAliasWithComplexSubstitution : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasWithComplexSubstitution>
            get() = ActualTypeAliasWithComplexSubstitution::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualTypeAliasToNullableType : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasToNullableType>
            get() = ActualTypeAliasToNullableType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualTypeAliasToNothing : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypeAliasToNothing>
            get() = ActualTypeAliasToNothing::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualFunctionWithDefaultArguments : KaFirDiagnostic<KtFunction> {
        override val diagnosticClass: KClass<ActualFunctionWithDefaultArguments>
            get() = ActualFunctionWithDefaultArguments::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DefaultArgumentsInExpectWithActualTypealias : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<DefaultArgumentsInExpectWithActualTypealias>
            get() = DefaultArgumentsInExpectWithActualTypealias::class

        public val expectClassSymbol: KaClassLikeSymbol
        public val members: List<KaCallableSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DefaultArgumentsInExpectActualizedByFakeOverride : KaFirDiagnostic<KtClass> {
        override val diagnosticClass: KClass<DefaultArgumentsInExpectActualizedByFakeOverride>
            get() = DefaultArgumentsInExpectActualizedByFakeOverride::class

        public val expectClassSymbol: KaClassLikeSymbol
        public val members: List<KaFunctionSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedFunctionSourceWithDefaultArgumentsNotFound : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ExpectedFunctionSourceWithDefaultArgumentsNotFound>
            get() = ExpectedFunctionSourceWithDefaultArgumentsNotFound::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualWithoutExpect : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ActualWithoutExpect>
            get() = ActualWithoutExpect::class

        public val declaration: KaSymbol
        public val compatibility: Map<ExpectActualMatchingCompatibility, List<KaSymbol>>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleClassTypeParameterCount : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassTypeParameterCount>
            get() = ExpectActualIncompatibleClassTypeParameterCount::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleReturnType : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleReturnType>
            get() = ExpectActualIncompatibleReturnType::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleEqualityBounds : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleEqualityBounds>
            get() = ExpectActualIncompatibleEqualityBounds::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleParameterNames>
            get() = ExpectActualIncompatibleParameterNames::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleContextParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleContextParameterNames>
            get() = ExpectActualIncompatibleContextParameterNames::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleTypeParameterNames : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleTypeParameterNames>
            get() = ExpectActualIncompatibleTypeParameterNames::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleValueParameterVararg : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleValueParameterVararg>
            get() = ExpectActualIncompatibleValueParameterVararg::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleValueParameterNoinline : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleValueParameterNoinline>
            get() = ExpectActualIncompatibleValueParameterNoinline::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleValueParameterCrossinline : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleValueParameterCrossinline>
            get() = ExpectActualIncompatibleValueParameterCrossinline::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleFunctionModifiersDifferent : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleFunctionModifiersDifferent>
            get() = ExpectActualIncompatibleFunctionModifiersDifferent::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleFunctionModifiersNotSubset : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleFunctionModifiersNotSubset>
            get() = ExpectActualIncompatibleFunctionModifiersNotSubset::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride>
            get() = ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverride::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatiblePropertyKind : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatiblePropertyKind>
            get() = ExpectActualIncompatiblePropertyKind::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatiblePropertyLateinitModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatiblePropertyLateinitModifier>
            get() = ExpectActualIncompatiblePropertyLateinitModifier::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatiblePropertyConstModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatiblePropertyConstModifier>
            get() = ExpectActualIncompatiblePropertyConstModifier::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatiblePropertySetterVisibility : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatiblePropertySetterVisibility>
            get() = ExpectActualIncompatiblePropertySetterVisibility::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleClassKind : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassKind>
            get() = ExpectActualIncompatibleClassKind::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleClassModifiers : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassModifiers>
            get() = ExpectActualIncompatibleClassModifiers::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleFunInterfaceModifier : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleFunInterfaceModifier>
            get() = ExpectActualIncompatibleFunInterfaceModifier::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleSupertypes : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleSupertypes>
            get() = ExpectActualIncompatibleSupertypes::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleNestedTypeAlias : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleNestedTypeAlias>
            get() = ExpectActualIncompatibleNestedTypeAlias::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleEnumEntries : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleEnumEntries>
            get() = ExpectActualIncompatibleEnumEntries::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleIllegalRequiresOptIn : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleIllegalRequiresOptIn>
            get() = ExpectActualIncompatibleIllegalRequiresOptIn::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleModality : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleModality>
            get() = ExpectActualIncompatibleModality::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleVisibility : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleVisibility>
            get() = ExpectActualIncompatibleVisibility::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleClassTypeParameterUpperBounds : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassTypeParameterUpperBounds>
            get() = ExpectActualIncompatibleClassTypeParameterUpperBounds::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleTypeParameterVariance : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleTypeParameterVariance>
            get() = ExpectActualIncompatibleTypeParameterVariance::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleTypeParameterReified : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleTypeParameterReified>
            get() = ExpectActualIncompatibleTypeParameterReified::class

        public val expectDeclaration: KaSymbol
        public val actualDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualIncompatibleClassScope : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualIncompatibleClassScope>
            get() = ExpectActualIncompatibleClassScope::class

        public val actualClass: KaSymbol
        public val expectMemberDeclaration: KaSymbol
        public val actualMemberDeclaration: KaSymbol
        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectRefinementAnnotationWrongTarget : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectRefinementAnnotationWrongTarget>
            get() = ExpectRefinementAnnotationWrongTarget::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AmbiguousExpects : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<AmbiguousExpects>
            get() = AmbiguousExpects::class

        public val declaration: KaSymbol
        public val modules: List<FirModuleData>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoActualClassMemberForExpectedClass : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<NoActualClassMemberForExpectedClass>
            get() = NoActualClassMemberForExpectedClass::class

        public val declaration: KaSymbol
        public val members: List<Pair<KaSymbol, Map<Mismatch, List<KaSymbol>>>>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualMissing : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ActualMissing>
            get() = ActualMissing::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectRefinementAnnotationMissing : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectRefinementAnnotationMissing>
            get() = ExpectRefinementAnnotationMissing::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualClassifiersAreInBetaWarning : KaFirDiagnostic<KtClassLikeDeclaration> {
        override val diagnosticClass: KClass<ExpectActualClassifiersAreInBetaWarning>
            get() = ExpectActualClassifiersAreInBetaWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotAMultiplatformCompilation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotAMultiplatformCompilation>
            get() = NotAMultiplatformCompilation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectActualOptInAnnotation : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ExpectActualOptInAnnotation>
            get() = ExpectActualOptInAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualTypealiasToSpecialAnnotation : KaFirDiagnostic<KtTypeAlias> {
        override val diagnosticClass: KClass<ActualTypealiasToSpecialAnnotation>
            get() = ActualTypealiasToSpecialAnnotation::class

        public val typealiasedClassId: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualAnnotationsNotMatchExpect : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ActualAnnotationsNotMatchExpect>
            get() = ActualAnnotationsNotMatchExpect::class

        public val expectSymbol: KaSymbol
        public val actualSymbol: KaSymbol
        public val actualAnnotationTargetSourceElement: PsiElement?
        public val incompatibilityType: ExpectActualAnnotationsIncompatibilityType<FirAnnotation>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ActualIgnorabilityNotMatchExpect : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<ActualIgnorabilityNotMatchExpect>
            get() = ActualIgnorabilityNotMatchExpect::class

        public val expectDeclaration: KaSymbol
        public val expectIgnorability: ReturnValueStatus
        public val actualDeclaration: KaSymbol
        public val actualIgnorability: ReturnValueStatus
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptionalDeclarationOutsideOfAnnotationEntry : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptionalDeclarationOutsideOfAnnotationEntry>
            get() = OptionalDeclarationOutsideOfAnnotationEntry::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptionalDeclarationUsageInNonCommonSource : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptionalDeclarationUsageInNonCommonSource>
            get() = OptionalDeclarationUsageInNonCommonSource::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OptionalExpectationNotOnExpected : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OptionalExpectationNotOnExpected>
            get() = OptionalExpectationNotOnExpected::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UninitializedVariable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UninitializedVariable>
            get() = UninitializedVariable::class

        public val variable: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UninitializedParameter : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass: KClass<UninitializedParameter>
            get() = UninitializedParameter::class

        public val parameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UninitializedEnumEntry : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UninitializedEnumEntry>
            get() = UninitializedEnumEntry::class

        public val enumEntry: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UninitializedEnumCompanion : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UninitializedEnumCompanion>
            get() = UninitializedEnumCompanion::class

        public val enumClass: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValReassignment : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ValReassignment>
            get() = ValReassignment::class

        public val variable: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValReassignmentViaBackingFieldError : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ValReassignmentViaBackingFieldError>
            get() = ValReassignmentViaBackingFieldError::class

        public val property: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CapturedValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CapturedValInitialization>
            get() = CapturedValInitialization::class

        public val property: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CapturedMemberValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<CapturedMemberValInitialization>
            get() = CapturedMemberValInitialization::class

        public val property: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonInlineMemberValInitialization : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NonInlineMemberValInitialization>
            get() = NonInlineMemberValInitialization::class

        public val property: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SetterProjectedOut : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<SetterProjectedOut>
            get() = SetterProjectedOut::class

        public val receiverType: KaType
        public val projection: String
        public val property: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongInvocationKind : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongInvocationKind>
            get() = WrongInvocationKind::class

        public val declaration: KaSymbol
        public val requiredRange: EventOccurrencesRange
        public val actualRange: EventOccurrencesRange
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LeakedInPlaceLambda : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LeakedInPlaceLambda>
            get() = LeakedInPlaceLambda::class

        public val lambda: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VariableWithNoTypeNoInitializer : KaFirDiagnostic<KtVariableDeclaration> {
        override val diagnosticClass: KClass<VariableWithNoTypeNoInitializer>
            get() = VariableWithNoTypeNoInitializer::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InitializationBeforeDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<InitializationBeforeDeclaration>
            get() = InitializationBeforeDeclaration::class

        public val property: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InitializationBeforeDeclarationWarning : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<InitializationBeforeDeclarationWarning>
            get() = InitializationBeforeDeclarationWarning::class

        public val property: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnreachableCode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnreachableCode>
            get() = UnreachableCode::class

        public val reachable: List<PsiElement>
        public val unreachable: List<PsiElement>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SenselessComparison : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<SenselessComparison>
            get() = SenselessComparison::class

        public val compareResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SenselessNullInWhen : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SenselessNullInWhen>
            get() = SenselessNullInWhen::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypecheckerHasRunIntoRecursiveProblem : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<TypecheckerHasRunIntoRecursiveProblem>
            get() = TypecheckerHasRunIntoRecursiveProblem::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnValueNotUsed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ReturnValueNotUsed>
            get() = ReturnValueNotUsed::class

        public val functionName: Name?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnValueNotUsedCoercion : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ReturnValueNotUsedCoercion>
            get() = ReturnValueNotUsedCoercion::class

        public val functionName: Name?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullForNonnullType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NullForNonnullType>
            get() = NullForNonnullType::class

        public val expectedType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsafeCall>
            get() = UnsafeCall::class

        public val receiverType: KaType
        public val receiverExpression: KtExpression?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsafeImplicitInvokeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsafeImplicitInvokeCall>
            get() = UnsafeImplicitInvokeCall::class

        public val receiverType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsafeInfixCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UnsafeInfixCall>
            get() = UnsafeInfixCall::class

        public val receiverType: KaType
        public val receiverExpression: KtExpression
        public val operator: String
        public val argumentExpression: KtExpression?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsafeOperatorCall : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UnsafeOperatorCall>
            get() = UnsafeOperatorCall::class

        public val receiverType: KaType
        public val receiverExpression: KtExpression
        public val operator: String
        public val argumentExpression: KtExpression?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsafeCallableReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnsafeCallableReference>
            get() = UnsafeCallableReference::class

        public val receiverType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IteratorOnNullable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<IteratorOnNullable>
            get() = IteratorOnNullable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ComponentFunctionOnNullable : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ComponentFunctionOnNullable>
            get() = ComponentFunctionOnNullable::class

        public val componentFunctionName: Name
        public val destructingType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnexpectedSafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnexpectedSafeCall>
            get() = UnexpectedSafeCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnnecessarySafeCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnnecessarySafeCall>
            get() = UnnecessarySafeCall::class

        public val receiverType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnnecessaryNotNullAssertion : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<UnnecessaryNotNullAssertion>
            get() = UnnecessaryNotNullAssertion::class

        public val receiverType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotNullAssertionOnLambdaExpression : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NotNullAssertionOnLambdaExpression>
            get() = NotNullAssertionOnLambdaExpression::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotNullAssertionOnCallableReference : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NotNullAssertionOnCallableReference>
            get() = NotNullAssertionOnCallableReference::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UselessElvis : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<UselessElvis>
            get() = UselessElvis::class

        public val receiverType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UselessElvisRightIsNull : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<UselessElvisRightIsNull>
            get() = UselessElvisRightIsNull::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UselessElvisLeftIsNull : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<UselessElvisLeftIsNull>
            get() = UselessElvisLeftIsNull::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotCheckForErased : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CannotCheckForErased>
            get() = CannotCheckForErased::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnsafeCastRelyingOnNull : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<UnsafeCastRelyingOnNull>
            get() = UnsafeCastRelyingOnNull::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SafeCastRelyingOnNull : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<SafeCastRelyingOnNull>
            get() = SafeCastRelyingOnNull::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CastNeverSucceeds : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<CastNeverSucceeds>
            get() = CastNeverSucceeds::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UselessCast : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<UselessCast>
            get() = UselessCast::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UncheckedCast : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<UncheckedCast>
            get() = UncheckedCast::class

        public val originalType: KaType
        public val targetType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NumericCastNeverSucceedsButCanBeReplacedWithToCall : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<NumericCastNeverSucceedsButCanBeReplacedWithToCall>
            get() = NumericCastNeverSucceedsButCanBeReplacedWithToCall::class

        public val targetType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IntegerLiteralCastInsteadOfToCall : KaFirDiagnostic<KtBinaryExpressionWithTypeRHS> {
        override val diagnosticClass: KClass<IntegerLiteralCastInsteadOfToCall>
            get() = IntegerLiteralCastInsteadOfToCall::class

        public val targetType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImpossibleIsCheckError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckError>
            get() = ImpossibleIsCheckError::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImpossibleIsCheckWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckWarning>
            get() = ImpossibleIsCheckWarning::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImpossibleIsCheckDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckDeprecationError>
            get() = ImpossibleIsCheckDeprecationError::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImpossibleIsCheckDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckDeprecationWarning>
            get() = ImpossibleIsCheckDeprecationWarning::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImpossibleIsCheckRelyingOnNullError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckRelyingOnNullError>
            get() = ImpossibleIsCheckRelyingOnNullError::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImpossibleIsCheckRelyingOnNullWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckRelyingOnNullWarning>
            get() = ImpossibleIsCheckRelyingOnNullWarning::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImpossibleIsCheckRelyingOnNullDeprecationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckRelyingOnNullDeprecationError>
            get() = ImpossibleIsCheckRelyingOnNullDeprecationError::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImpossibleIsCheckRelyingOnNullDeprecationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImpossibleIsCheckRelyingOnNullDeprecationWarning>
            get() = ImpossibleIsCheckRelyingOnNullDeprecationWarning::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UselessIsCheck : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UselessIsCheck>
            get() = UselessIsCheck::class

        public val compileTimeCheckResult: Boolean
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IsEnumEntry : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IsEnumEntry>
            get() = IsEnumEntry::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DynamicNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DynamicNotAllowed>
            get() = DynamicNotAllowed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EnumEntryAsType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<EnumEntryAsType>
            get() = EnumEntryAsType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedCondition : KaFirDiagnostic<KtWhenCondition> {
        override val diagnosticClass: KClass<ExpectedCondition>
            get() = ExpectedCondition::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoElseInWhen : KaFirDiagnostic<KtWhenExpression> {
        override val diagnosticClass: KClass<NoElseInWhen>
            get() = NoElseInWhen::class

        public val missingWhenCases: List<KaWhenMissingCase>
        public val description: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingBranchForNonAbstractSealedClass : KaFirDiagnostic<KtWhenExpression> {
        override val diagnosticClass: KClass<MissingBranchForNonAbstractSealedClass>
            get() = MissingBranchForNonAbstractSealedClass::class

        public val missingWhenCases: List<KaWhenMissingCase>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidIfAsExpression : KaFirDiagnostic<KtIfExpression> {
        override val diagnosticClass: KClass<InvalidIfAsExpression>
            get() = InvalidIfAsExpression::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ElseMisplacedInWhen : KaFirDiagnostic<KtWhenEntry> {
        override val diagnosticClass: KClass<ElseMisplacedInWhen>
            get() = ElseMisplacedInWhen::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantElseInWhen : KaFirDiagnostic<KtWhenEntry> {
        override val diagnosticClass: KClass<RedundantElseInWhen>
            get() = RedundantElseInWhen::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalDeclarationInWhenSubject : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IllegalDeclarationInWhenSubject>
            get() = IllegalDeclarationInWhenSubject::class

        public val illegalReason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CommaInWhenConditionWithoutArgument : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CommaInWhenConditionWithoutArgument>
            get() = CommaInWhenConditionWithoutArgument::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DuplicateBranchConditionInWhen : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DuplicateBranchConditionInWhen>
            get() = DuplicateBranchConditionInWhen::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConfusingBranchConditionError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConfusingBranchConditionError>
            get() = ConfusingBranchConditionError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongConditionSuggestGuard : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongConditionSuggestGuard>
            get() = WrongConditionSuggestGuard::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CommaInWhenConditionWithWhenGuard : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CommaInWhenConditionWithWhenGuard>
            get() = CommaInWhenConditionWithWhenGuard::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WhenGuardWithoutSubject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WhenGuardWithoutSubject>
            get() = WhenGuardWithoutSubject::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InferredInvisibleWhenTypeWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InferredInvisibleWhenTypeWarning>
            get() = InferredInvisibleWhenTypeWarning::class

        public val whenType: KaType
        public val syntaxConstructionName: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParameterIsNotAnExpression : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass: KClass<TypeParameterIsNotAnExpression>
            get() = TypeParameterIsNotAnExpression::class

        public val typeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeParameterOnLhsOfDot : KaFirDiagnostic<KtSimpleNameExpression> {
        override val diagnosticClass: KClass<TypeParameterOnLhsOfDot>
            get() = TypeParameterOnLhsOfDot::class

        public val typeParameter: KaTypeParameterSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoCompanionObject : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NoCompanionObject>
            get() = NoCompanionObject::class

        public val klass: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpressionExpectedPackageFound : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ExpressionExpectedPackageFound>
            get() = ExpressionExpectedPackageFound::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ErrorInContractDescription : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ErrorInContractDescription>
            get() = ErrorInContractDescription::class

        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ContractNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ContractNotAllowed>
            get() = ContractNotAllowed::class

        public val reason: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoGetMethod : KaFirDiagnostic<KtArrayAccessExpression> {
        override val diagnosticClass: KClass<NoGetMethod>
            get() = NoGetMethod::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoSetMethod : KaFirDiagnostic<KtArrayAccessExpression> {
        override val diagnosticClass: KClass<NoSetMethod>
            get() = NoSetMethod::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IteratorMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<IteratorMissing>
            get() = IteratorMissing::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface HasNextMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<HasNextMissing>
            get() = HasNextMissing::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NextMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NextMissing>
            get() = NextMissing::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ComponentFunctionMissing : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ComponentFunctionMissing>
            get() = ComponentFunctionMissing::class

        public val missingFunctionName: Name
        public val destructingType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegateSpecialFunctionMissing : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<DelegateSpecialFunctionMissing>
            get() = DelegateSpecialFunctionMissing::class

        public val expectedFunctionSignature: String
        public val delegateType: KaType
        public val description: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnderscoreIsReserved : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnderscoreIsReserved>
            get() = UnderscoreIsReserved::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnderscoreUsageWithoutBackticks : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnderscoreUsageWithoutBackticks>
            get() = UnderscoreUsageWithoutBackticks::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ResolvedToUnderscoreNamedCatchParameter : KaFirDiagnostic<KtNameReferenceExpression> {
        override val diagnosticClass: KClass<ResolvedToUnderscoreNamedCatchParameter>
            get() = ResolvedToUnderscoreNamedCatchParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidCharacters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidCharacters>
            get() = InvalidCharacters::class

        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EqualityNotApplicable : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<EqualityNotApplicable>
            get() = EqualityNotApplicable::class

        public val operator: String
        public val leftType: KaType
        public val rightType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EqualityNotApplicableWarning : KaFirDiagnostic<KtBinaryExpression> {
        override val diagnosticClass: KClass<EqualityNotApplicableWarning>
            get() = EqualityNotApplicableWarning::class

        public val operator: String
        public val leftType: KaType
        public val rightType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncompatibleEnumComparisonError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncompatibleEnumComparisonError>
            get() = IncompatibleEnumComparisonError::class

        public val leftType: KaType
        public val rightType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncompatibleEnumComparison : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IncompatibleEnumComparison>
            get() = IncompatibleEnumComparison::class

        public val leftType: KaType
        public val rightType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ForbiddenIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ForbiddenIdentityEquals>
            get() = ForbiddenIdentityEquals::class

        public val leftType: KaType
        public val rightType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ForbiddenIdentityEqualsWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ForbiddenIdentityEqualsWarning>
            get() = ForbiddenIdentityEqualsWarning::class

        public val leftType: KaType
        public val rightType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DeprecatedIdentityEquals>
            get() = DeprecatedIdentityEquals::class

        public val leftType: KaType
        public val rightType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplicitBoxingInIdentityEquals : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ImplicitBoxingInIdentityEquals>
            get() = ImplicitBoxingInIdentityEquals::class

        public val leftType: KaType
        public val rightType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncDecShouldNotReturnUnit : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<IncDecShouldNotReturnUnit>
            get() = IncDecShouldNotReturnUnit::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssignmentOperatorShouldReturnUnit : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<AssignmentOperatorShouldReturnUnit>
            get() = AssignmentOperatorShouldReturnUnit::class

        public val functionSymbol: KaFunctionSymbol
        public val operator: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InitializerRequiredForDestructuringDeclaration : KaFirDiagnostic<KtDestructuringDeclaration> {
        override val diagnosticClass: KClass<InitializerRequiredForDestructuringDeclaration>
            get() = InitializerRequiredForDestructuringDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotFunctionAsOperator : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NotFunctionAsOperator>
            get() = NotFunctionAsOperator::class

        public val elementName: String
        public val elementSymbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DslScopeViolation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DslScopeViolation>
            get() = DslScopeViolation::class

        public val calleeSymbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReceiverShadowedByContextParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ReceiverShadowedByContextParameter>
            get() = ReceiverShadowedByContextParameter::class

        public val calleeSymbol: KaSymbol
        public val isDispatchOfMemberExtension: Boolean
        public val contextParameterSymbols: List<KaSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RecursiveTypealiasExpansion : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RecursiveTypealiasExpansion>
            get() = RecursiveTypealiasExpansion::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasShouldExpandToClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasShouldExpandToClass>
            get() = TypealiasShouldExpandToClass::class

        public val expandedType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstructorOrSupertypeOnTypealiasWithTypeProjectionError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConstructorOrSupertypeOnTypealiasWithTypeProjectionError>
            get() = ConstructorOrSupertypeOnTypealiasWithTypeProjectionError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning>
            get() = ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasExpansionCapturesOuterTypeParameters : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasExpansionCapturesOuterTypeParameters>
            get() = TypealiasExpansionCapturesOuterTypeParameters::class

        public val outerTypeParameters: List<KaTypeParameterSymbol>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasExpandsToCompilerRequiredAnnotationError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasExpandsToCompilerRequiredAnnotationError>
            get() = TypealiasExpandsToCompilerRequiredAnnotationError::class

        public val annotation: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasExpandsToCompilerRequiredAnnotationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<TypealiasExpandsToCompilerRequiredAnnotationWarning>
            get() = TypealiasExpandsToCompilerRequiredAnnotationWarning::class

        public val annotation: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExpectedTypealias : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExpectedTypealias>
            get() = ExpectedTypealias::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantVisibilityModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<RedundantVisibilityModifier>
            get() = RedundantVisibilityModifier::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantModalityModifier : KaFirDiagnostic<KtModifierListOwner> {
        override val diagnosticClass: KClass<RedundantModalityModifier>
            get() = RedundantModalityModifier::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantReturnUnitType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RedundantReturnUnitType>
            get() = RedundantReturnUnitType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantSingleExpressionStringTemplate : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantSingleExpressionStringTemplate>
            get() = RedundantSingleExpressionStringTemplate::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CanBeVal : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CanBeVal>
            get() = CanBeVal::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CanBeValLateinit : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CanBeValLateinit>
            get() = CanBeValLateinit::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CanBeValDelayedInitialization : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<CanBeValDelayedInitialization>
            get() = CanBeValDelayedInitialization::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantCallOfConversionMethod : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantCallOfConversionMethod>
            get() = RedundantCallOfConversionMethod::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ArrayEqualityOperatorCanBeReplacedWithContentEquals : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<ArrayEqualityOperatorCanBeReplacedWithContentEquals>
            get() = ArrayEqualityOperatorCanBeReplacedWithContentEquals::class

        public val operator: String
        public val replacementPrefix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EmptyRange : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<EmptyRange>
            get() = EmptyRange::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantSetterParameterType : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<RedundantSetterParameterType>
            get() = RedundantSetterParameterType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnusedVariable : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<UnusedVariable>
            get() = UnusedVariable::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AssignedValueIsNeverRead : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<AssignedValueIsNeverRead>
            get() = AssignedValueIsNeverRead::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VariableInitializerIsRedundant : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<VariableInitializerIsRedundant>
            get() = VariableInitializerIsRedundant::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface VariableNeverRead : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<VariableNeverRead>
            get() = VariableNeverRead::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UselessCallOnNotNull : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UselessCallOnNotNull>
            get() = UselessCallOnNotNull::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnusedAnonymousParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UnusedAnonymousParameter>
            get() = UnusedAnonymousParameter::class

        public val parameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnusedExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnusedExpression>
            get() = UnusedExpression::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnusedLambdaExpression : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnusedLambdaExpression>
            get() = UnusedLambdaExpression::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnNotAllowed : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnNotAllowed>
            get() = ReturnNotAllowed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotAFunctionLabel : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<NotAFunctionLabel>
            get() = NotAFunctionLabel::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnInFunctionWithExpressionBody : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnInFunctionWithExpressionBody>
            get() = ReturnInFunctionWithExpressionBody::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnInFunctionWithExpressionBodyWarning : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnInFunctionWithExpressionBodyWarning>
            get() = ReturnInFunctionWithExpressionBodyWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnInFunctionWithExpressionBodyAndImplicitType : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnInFunctionWithExpressionBodyAndImplicitType>
            get() = ReturnInFunctionWithExpressionBodyAndImplicitType::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoReturnInFunctionWithBlockBody : KaFirDiagnostic<KtDeclarationWithBody> {
        override val diagnosticClass: KClass<NoReturnInFunctionWithBlockBody>
            get() = NoReturnInFunctionWithBlockBody::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantReturn : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<RedundantReturn>
            get() = RedundantReturn::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnonymousInitializerInInterface : KaFirDiagnostic<KtAnonymousInitializer> {
        override val diagnosticClass: KClass<AnonymousInitializerInInterface>
            get() = AnonymousInitializerInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UsageIsNotInlinable : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UsageIsNotInlinable>
            get() = UsageIsNotInlinable::class

        public val parameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonLocalReturnNotAllowed : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonLocalReturnNotAllowed>
            get() = NonLocalReturnNotAllowed::class

        public val parameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotYetSupportedInInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NotYetSupportedInInline>
            get() = NotYetSupportedInInline::class

        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotYetSupportedInInlineWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NotYetSupportedInInlineWarning>
            get() = NotYetSupportedInInlineWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NothingToInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NothingToInline>
            get() = NothingToInline::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullableInlineParameter : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NullableInlineParameter>
            get() = NullableInlineParameter::class

        public val parameter: KaSymbol
        public val function: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RecursionInInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<RecursionInInline>
            get() = RecursionInInline::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonPublicCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicCallFromPublicInline>
            get() = NonPublicCallFromPublicInline::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonPublicInlineCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicInlineCallFromPublicInline>
            get() = NonPublicInlineCallFromPublicInline::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonPublicCallFromPublicInlineDeprecation : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicCallFromPublicInlineDeprecation>
            get() = NonPublicCallFromPublicInlineDeprecation::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonPublicDataCopyCallFromPublicInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicDataCopyCallFromPublicInlineError>
            get() = NonPublicDataCopyCallFromPublicInlineError::class

        public val inlineDeclaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonPublicDataCopyCallFromPublicInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonPublicDataCopyCallFromPublicInlineWarning>
            get() = NonPublicDataCopyCallFromPublicInlineWarning::class

        public val inlineDeclaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ProtectedConstructorCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ProtectedConstructorCallFromPublicInline>
            get() = ProtectedConstructorCallFromPublicInline::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ProtectedCallFromPublicInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ProtectedCallFromPublicInlineError>
            get() = ProtectedCallFromPublicInlineError::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PrivateClassMemberFromInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<PrivateClassMemberFromInline>
            get() = PrivateClassMemberFromInline::class

        public val inlineDeclaration: KaSymbol
        public val referencedDeclaration: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SuperCallFromPublicInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SuperCallFromPublicInline>
            get() = SuperCallFromPublicInline::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeclarationCantBeInlined : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<DeclarationCantBeInlined>
            get() = DeclarationCantBeInlined::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeclarationCantBeInlinedDeprecationError : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<DeclarationCantBeInlinedDeprecationError>
            get() = DeclarationCantBeInlinedDeprecationError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeclarationCantBeInlinedDeprecationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<DeclarationCantBeInlinedDeprecationWarning>
            get() = DeclarationCantBeInlinedDeprecationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverrideByInline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<OverrideByInline>
            get() = OverrideByInline::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidDefaultFunctionalParameterForInline : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InvalidDefaultFunctionalParameterForInline>
            get() = InvalidDefaultFunctionalParameterForInline::class

        public val parameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotSupportedInlineParameterInInlineParameterDefaultValue : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NotSupportedInlineParameterInInlineParameterDefaultValue>
            get() = NotSupportedInlineParameterInInlineParameterDefaultValue::class

        public val parameter: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReifiedTypeParameterInOverride : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ReifiedTypeParameterInOverride>
            get() = ReifiedTypeParameterInOverride::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlinePropertyWithBackingField : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InlinePropertyWithBackingField>
            get() = InlinePropertyWithBackingField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlinePropertyWithBackingFieldDeprecationError : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InlinePropertyWithBackingFieldDeprecationError>
            get() = InlinePropertyWithBackingFieldDeprecationError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlinePropertyWithBackingFieldDeprecationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InlinePropertyWithBackingFieldDeprecationWarning>
            get() = InlinePropertyWithBackingFieldDeprecationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalInlineParameterModifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IllegalInlineParameterModifier>
            get() = IllegalInlineParameterModifier::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlineSuspendFunctionTypeUnsupported : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<InlineSuspendFunctionTypeUnsupported>
            get() = InlineSuspendFunctionTypeUnsupported::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InefficientEqualsOverridingInValueClass : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<InefficientEqualsOverridingInValueClass>
            get() = InefficientEqualsOverridingInValueClass::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlineClassDeprecated : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InlineClassDeprecated>
            get() = InlineClassDeprecated::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LessVisibleTypeAccessInInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<LessVisibleTypeAccessInInlineError>
            get() = LessVisibleTypeAccessInInlineError::class

        public val typeVisibility: EffectiveVisibility
        public val type: KaType
        public val inlineVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LessVisibleTypeAccessInInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<LessVisibleTypeAccessInInlineWarning>
            get() = LessVisibleTypeAccessInInlineWarning::class

        public val typeVisibility: EffectiveVisibility
        public val type: KaType
        public val inlineVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LessVisibleTypeInInlineAccessedSignatureError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<LessVisibleTypeInInlineAccessedSignatureError>
            get() = LessVisibleTypeInInlineAccessedSignatureError::class

        public val symbol: KaSymbol
        public val typeVisibility: EffectiveVisibility
        public val type: KaType
        public val inlineVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LessVisibleTypeInInlineAccessedSignatureWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<LessVisibleTypeInInlineAccessedSignatureWarning>
            get() = LessVisibleTypeInInlineAccessedSignatureWarning::class

        public val symbol: KaSymbol
        public val typeVisibility: EffectiveVisibility
        public val type: KaType
        public val inlineVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallableReferenceToLessVisibleDeclarationInInlineError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallableReferenceToLessVisibleDeclarationInInlineError>
            get() = CallableReferenceToLessVisibleDeclarationInInlineError::class

        public val symbol: KaSymbol
        public val visibility: EffectiveVisibility
        public val inlineVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallableReferenceToLessVisibleDeclarationInInlineWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CallableReferenceToLessVisibleDeclarationInInlineWarning>
            get() = CallableReferenceToLessVisibleDeclarationInInlineWarning::class

        public val symbol: KaSymbol
        public val visibility: EffectiveVisibility
        public val inlineVisibility: EffectiveVisibility
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ContextParameterMustBeNoinline : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ContextParameterMustBeNoinline>
            get() = ContextParameterMustBeNoinline::class

        public val parameter: KaSymbol
        public val function: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlineFromHigherPlatform : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InlineFromHigherPlatform>
            get() = InlineFromHigherPlatform::class

        public val inlinedBytecodeVersion: String
        public val currentModuleBytecodeVersion: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotAllUnderImportFromSingleton : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<CannotAllUnderImportFromSingleton>
            get() = CannotAllUnderImportFromSingleton::class

        public val objectName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PackageCannotBeImported : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<PackageCannotBeImported>
            get() = PackageCannotBeImported::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotBeImported : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<CannotBeImported>
            get() = CannotBeImported::class

        public val name: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConflictingImport : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<ConflictingImport>
            get() = ConflictingImport::class

        public val name: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunctionTypeOfTooLargeArity : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<FunctionTypeOfTooLargeArity>
            get() = FunctionTypeOfTooLargeArity::class

        public val classId: ClassId
        public val maxArity: Int
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface KSuspendFunctionTypeOfDangerouslyLargeArity : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<KSuspendFunctionTypeOfDangerouslyLargeArity>
            get() = KSuspendFunctionTypeOfDangerouslyLargeArity::class

        public val classId: ClassId
        public val maxArity: Int
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OperatorRenamedOnImport : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<OperatorRenamedOnImport>
            get() = OperatorRenamedOnImport::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasAsCallableQualifierInImportError : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<TypealiasAsCallableQualifierInImportError>
            get() = TypealiasAsCallableQualifierInImportError::class

        public val typealiasName: Name
        public val originalClassName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypealiasAsCallableQualifierInImportWarning : KaFirDiagnostic<KtImportDirective> {
        override val diagnosticClass: KClass<TypealiasAsCallableQualifierInImportWarning>
            get() = TypealiasAsCallableQualifierInImportWarning::class

        public val typealiasName: Name
        public val originalClassName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalSuspendFunctionCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalSuspendFunctionCall>
            get() = IllegalSuspendFunctionCall::class

        public val suspendCallable: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalSuspendPropertyAccess : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalSuspendPropertyAccess>
            get() = IllegalSuspendPropertyAccess::class

        public val suspendCallable: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonLocalSuspensionPoint : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonLocalSuspensionPoint>
            get() = NonLocalSuspensionPoint::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalRestrictedSuspendingFunctionCall : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalRestrictedSuspendingFunctionCall>
            get() = IllegalRestrictedSuspendingFunctionCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonModifierFormForBuiltInSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonModifierFormForBuiltInSuspend>
            get() = NonModifierFormForBuiltInSuspend::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ModifierFormForNonBuiltInSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ModifierFormForNonBuiltInSuspend>
            get() = ModifierFormForNonBuiltInSuspend::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ModifierFormForNonBuiltInSuspendFunError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ModifierFormForNonBuiltInSuspendFunError>
            get() = ModifierFormForNonBuiltInSuspendFunError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReturnForBuiltInSuspend : KaFirDiagnostic<KtReturnExpression> {
        override val diagnosticClass: KClass<ReturnForBuiltInSuspend>
            get() = ReturnForBuiltInSuspend::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MixingSuspendAndNonSuspendSupertypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MixingSuspendAndNonSuspendSupertypes>
            get() = MixingSuspendAndNonSuspendSupertypes::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MixingFunctionalKindsInSupertypes : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MixingFunctionalKindsInSupertypes>
            get() = MixingFunctionalKindsInSupertypes::class

        public val kinds: List<FunctionTypeKind>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantLabelWarning : KaFirDiagnostic<KtLabelReferenceExpression> {
        override val diagnosticClass: KClass<RedundantLabelWarning>
            get() = RedundantLabelWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleLabelsAreForbidden : KaFirDiagnostic<KtLabelReferenceExpression> {
        override val diagnosticClass: KClass<MultipleLabelsAreForbidden>
            get() = MultipleLabelsAreForbidden::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedAccessToEnumEntryCompanionProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEnumEntryCompanionProperty>
            get() = DeprecatedAccessToEnumEntryCompanionProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedAccessToEntryPropertyFromEnum : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEntryPropertyFromEnum>
            get() = DeprecatedAccessToEntryPropertyFromEnum::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedAccessToEntriesProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEntriesProperty>
            get() = DeprecatedAccessToEntriesProperty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedAccessToEnumEntryPropertyAsReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEnumEntryPropertyAsReference>
            get() = DeprecatedAccessToEnumEntryPropertyAsReference::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedAccessToEntriesAsQualifier : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DeprecatedAccessToEntriesAsQualifier>
            get() = DeprecatedAccessToEntriesAsQualifier::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeclarationOfEnumEntryEntriesError : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass: KClass<DeclarationOfEnumEntryEntriesError>
            get() = DeclarationOfEnumEntryEntriesError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeclarationOfEnumEntryEntriesWarning : KaFirDiagnostic<KtEnumEntry> {
        override val diagnosticClass: KClass<DeclarationOfEnumEntryEntriesWarning>
            get() = DeclarationOfEnumEntryEntriesWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncompatibleClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IncompatibleClass>
            get() = IncompatibleClass::class

        public val presentableString: String
        public val incompatibility: IncompatibleVersionErrorData<*>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PreReleaseClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PreReleaseClass>
            get() = PreReleaseClass::class

        public val presentableString: String
        public val poisoningFeatures: List<String>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IrWithUnstableAbiCompiledClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IrWithUnstableAbiCompiledClass>
            get() = IrWithUnstableAbiCompiledClass::class

        public val presentableString: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface BuilderInferenceStubReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<BuilderInferenceStubReceiver>
            get() = BuilderInferenceStubReceiver::class

        public val typeParameterName: Name
        public val containingDeclarationName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface BuilderInferenceMultiLambdaRestriction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<BuilderInferenceMultiLambdaRestriction>
            get() = BuilderInferenceMultiLambdaRestriction::class

        public val typeParameterName: Name
        public val containingDeclarationName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidVersioningOnNonOptional : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnNonOptional>
            get() = InvalidVersioningOnNonOptional::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidVersioningOnNonfinalClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnNonfinalClass>
            get() = InvalidVersioningOnNonfinalClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidVersioningOnLocalFunction : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnLocalFunction>
            get() = InvalidVersioningOnLocalFunction::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidVersioningOnAnnotationClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnAnnotationClass>
            get() = InvalidVersioningOnAnnotationClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidDefaultValueDependency : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidDefaultValueDependency>
            get() = InvalidDefaultValueDependency::class

        public val lowestVersion: MavenComparableVersion?
        public val highestVersion: MavenComparableVersion?
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidNonOptionalParameterPosition : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidNonOptionalParameterPosition>
            get() = InvalidNonOptionalParameterPosition::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidVersioningOnReceiverOrContextParameterPosition : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnReceiverOrContextParameterPosition>
            get() = InvalidVersioningOnReceiverOrContextParameterPosition::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidVersioningOnVararg : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnVararg>
            get() = InvalidVersioningOnVararg::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InvalidVersioningOnValueClassParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InvalidVersioningOnValueClassParameter>
            get() = InvalidVersioningOnValueClassParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonAscendingVersionAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonAscendingVersionAnnotation>
            get() = NonAscendingVersionAnnotation::class

        public val lowestVersion: MavenComparableVersion?
        public val highestVersion: MavenComparableVersion?
        public val sourceOfHighestVersion: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompanionBlockMemberExtension : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionBlockMemberExtension>
            get() = CompanionBlockMemberExtension::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PrivateConstInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<PrivateConstInInterface>
            get() = PrivateConstInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalCompanionBlock : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalCompanionBlock>
            get() = IllegalCompanionBlock::class

        public val parent: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompanionBlockNested : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionBlockNested>
            get() = CompanionBlockNested::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalCompanionBlockMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalCompanionBlockMember>
            get() = IllegalCompanionBlockMember::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompanionExtensionReceiverWithTypeArguments : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionReceiverWithTypeArguments>
            get() = CompanionExtensionReceiverWithTypeArguments::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompanionExtensionReceiverIsObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionReceiverIsObject>
            get() = CompanionExtensionReceiverIsObject::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompanionExtensionReceiverIsTypeParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionReceiverIsTypeParameter>
            get() = CompanionExtensionReceiverIsTypeParameter::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompanionExtensionReceiverAnnotated : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionReceiverAnnotated>
            get() = CompanionExtensionReceiverAnnotated::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CompanionExtensionNullableReceiver : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CompanionExtensionNullableReceiver>
            get() = CompanionExtensionNullableReceiver::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverrideCannotBeStatic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<OverrideCannotBeStatic>
            get() = OverrideCannotBeStatic::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmStaticNotInObjectOrClassCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticNotInObjectOrClassCompanion>
            get() = JvmStaticNotInObjectOrClassCompanion::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmStaticNotInObjectOrCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticNotInObjectOrCompanion>
            get() = JvmStaticNotInObjectOrCompanion::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmStaticOnNonPublicMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticOnNonPublicMember>
            get() = JvmStaticOnNonPublicMember::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmStaticOnConstOrJvmField : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticOnConstOrJvmField>
            get() = JvmStaticOnConstOrJvmField::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmStaticOnExternalInInterface : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmStaticOnExternalInInterface>
            get() = JvmStaticOnExternalInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableJvmName>
            get() = InapplicableJvmName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalJvmName>
            get() = IllegalJvmName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FunctionDelegateMemberNameClash : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FunctionDelegateMemberNameClash>
            get() = FunctionDelegateMemberNameClash::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ValueClassWithoutJvmInlineAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ValueClassWithoutJvmInlineAnnotation>
            get() = ValueClassWithoutJvmInlineAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmInlineWithoutValueClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmInlineWithoutValueClass>
            get() = JvmInlineWithoutValueClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableJvmExposeBoxedWithName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InapplicableJvmExposeBoxedWithName>
            get() = InapplicableJvmExposeBoxedWithName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UselessJvmExposeBoxed : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UselessJvmExposeBoxed>
            get() = UselessJvmExposeBoxed::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCannotExposeSuspend : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeSuspend>
            get() = JvmExposeBoxedCannotExposeSuspend::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedRequiresName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedRequiresName>
            get() = JvmExposeBoxedRequiresName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCannotBeTheSame : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotBeTheSame>
            get() = JvmExposeBoxedCannotBeTheSame::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCannotBeTheSameAsJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotBeTheSameAsJvmName>
            get() = JvmExposeBoxedCannotBeTheSameAsJvmName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCannotExposeOpenAbstract : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeOpenAbstract>
            get() = JvmExposeBoxedCannotExposeOpenAbstract::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCannotExposeSynthetic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeSynthetic>
            get() = JvmExposeBoxedCannotExposeSynthetic::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCannotExposeLocals : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeLocals>
            get() = JvmExposeBoxedCannotExposeLocals::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCannotExposeReified : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposeReified>
            get() = JvmExposeBoxedCannotExposeReified::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCannotExposePrivate : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCannotExposePrivate>
            get() = JvmExposeBoxedCannotExposePrivate::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmExposeBoxedCanBeReplacedWithJvmName : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmExposeBoxedCanBeReplacedWithJvmName>
            get() = JvmExposeBoxedCanBeReplacedWithJvmName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongTypeForJavaOverride : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<WrongTypeForJavaOverride>
            get() = WrongTypeForJavaOverride::class

        public val override: KaCallableSymbol
        public val base: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AccidentalOverrideClashByJvmSignature : KaFirDiagnostic<KtNamedFunction> {
        override val diagnosticClass: KClass<AccidentalOverrideClashByJvmSignature>
            get() = AccidentalOverrideClashByJvmSignature::class

        public val hidden: KaFunctionSymbol
        public val overrideDescription: String
        public val regular: KaFunctionSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplementationByDelegationWithDifferentGenericSignatureError : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass: KClass<ImplementationByDelegationWithDifferentGenericSignatureError>
            get() = ImplementationByDelegationWithDifferentGenericSignatureError::class

        public val base: KaFunctionSymbol
        public val override: KaFunctionSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplementationByDelegationWithDifferentGenericSignatureWarning : KaFirDiagnostic<KtTypeReference> {
        override val diagnosticClass: KClass<ImplementationByDelegationWithDifferentGenericSignatureWarning>
            get() = ImplementationByDelegationWithDifferentGenericSignatureWarning::class

        public val base: KaFunctionSymbol
        public val override: KaFunctionSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotYetSupportedLocalInlineFunction : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<NotYetSupportedLocalInlineFunction>
            get() = NotYetSupportedLocalInlineFunction::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyHidesJavaField : KaFirDiagnostic<KtCallableDeclaration> {
        override val diagnosticClass: KClass<PropertyHidesJavaField>
            get() = PropertyHidesJavaField::class

        public val hidden: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConflictVersionAndJvmOverloadsAnnotation : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConflictVersionAndJvmOverloadsAnnotation>
            get() = ConflictVersionAndJvmOverloadsAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaTypeMismatch : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<JavaTypeMismatch>
            get() = JavaTypeMismatch::class

        public val expectedType: KaType
        public val actualType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReceiverNullabilityMismatchBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ReceiverNullabilityMismatchBasedOnJavaAnnotations>
            get() = ReceiverNullabilityMismatchBasedOnJavaAnnotations::class

        public val actualType: KaType
        public val expectedType: KaType
        public val messageSuffix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ReceiverMutabilityMismatchBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ReceiverMutabilityMismatchBasedOnJavaAnnotations>
            get() = ReceiverMutabilityMismatchBasedOnJavaAnnotations::class

        public val actualType: KaType
        public val expectedType: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeMismatchBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeMismatchBasedOnJavaAnnotations>
            get() = TypeMismatchBasedOnJavaAnnotations::class

        public val actualType: KaType
        public val expectedType: KaType
        public val messageSuffix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NullabilityMismatchBasedOnExplicitTypeArgumentsForJava : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NullabilityMismatchBasedOnExplicitTypeArgumentsForJava>
            get() = NullabilityMismatchBasedOnExplicitTypeArgumentsForJava::class

        public val actualType: KaType
        public val expectedType: KaType
        public val messageSuffix: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface TypeMismatchWhenFlexibilityChanges : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<TypeMismatchWhenFlexibilityChanges>
            get() = TypeMismatchWhenFlexibilityChanges::class

        public val actualType: KaType
        public val expectedType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaClassOnCompanion : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaClassOnCompanion>
            get() = JavaClassOnCompanion::class

        public val actualType: KaType
        public val expectedType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaClassPropertyReferenceError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaClassPropertyReferenceError>
            get() = JavaClassPropertyReferenceError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaClassPropertyReferenceWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaClassPropertyReferenceWarning>
            get() = JavaClassPropertyReferenceWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UnexhaustiveWhenBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UnexhaustiveWhenBasedOnJavaAnnotations>
            get() = UnexhaustiveWhenBasedOnJavaAnnotations::class

        public val subjectType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundCannotBeArray : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundCannotBeArray>
            get() = UpperBoundCannotBeArray::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolatedBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedBasedOnJavaAnnotations>
            get() = UpperBoundViolatedBasedOnJavaAnnotations::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations>
            get() = UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotations::class

        public val expectedUpperBound: KaType
        public val actualType: KaType
        public val onTypeParameter: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface StrictfpOnClass : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<StrictfpOnClass>
            get() = StrictfpOnClass::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedOnAbstract : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnAbstract>
            get() = SynchronizedOnAbstract::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedInInterface : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedInInterface>
            get() = SynchronizedInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedInAnnotationError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedInAnnotationError>
            get() = SynchronizedInAnnotationError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedInAnnotationWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedInAnnotationWarning>
            get() = SynchronizedInAnnotationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedOnInline : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnInline>
            get() = SynchronizedOnInline::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedOnValueClassError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnValueClassError>
            get() = SynchronizedOnValueClassError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedOnValueClassWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnValueClassWarning>
            get() = SynchronizedOnValueClassWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedOnSuspendError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<SynchronizedOnSuspendError>
            get() = SynchronizedOnSuspendError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverloadsWithoutDefaultArguments : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsWithoutDefaultArguments>
            get() = OverloadsWithoutDefaultArguments::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverloadsAbstract : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsAbstract>
            get() = OverloadsAbstract::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverloadsInterface : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsInterface>
            get() = OverloadsInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverloadsLocal : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsLocal>
            get() = OverloadsLocal::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverloadsAnnotationClassConstructorError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsAnnotationClassConstructorError>
            get() = OverloadsAnnotationClassConstructorError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverloadsPrivate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<OverloadsPrivate>
            get() = OverloadsPrivate::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DeprecatedJavaAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<DeprecatedJavaAnnotation>
            get() = DeprecatedJavaAnnotation::class

        public val kotlinName: FqName
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmPackageNameCannotBeEmpty : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmPackageNameCannotBeEmpty>
            get() = JvmPackageNameCannotBeEmpty::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmPackageNameMustBeValidName : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmPackageNameMustBeValidName>
            get() = JvmPackageNameMustBeValidName::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmPackageNameNotSupportedInFilesWithClasses : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmPackageNameNotSupportedInFilesWithClasses>
            get() = JvmPackageNameNotSupportedInFilesWithClasses::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PositionedValueArgumentForJavaAnnotation : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<PositionedValueArgumentForJavaAnnotation>
            get() = PositionedValueArgumentForJavaAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RedundantRepeatableAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RedundantRepeatableAnnotation>
            get() = RedundantRepeatableAnnotation::class

        public val kotlinRepeatable: FqName
        public val javaRepeatable: FqName
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ThrowsInAnnotationError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<ThrowsInAnnotationError>
            get() = ThrowsInAnnotationError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ThrowsInAnnotationWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<ThrowsInAnnotationWarning>
            get() = ThrowsInAnnotationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmSerializableLambdaOnInlinedFunctionLiteralsError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmSerializableLambdaOnInlinedFunctionLiteralsError>
            get() = JvmSerializableLambdaOnInlinedFunctionLiteralsError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmSerializableLambdaOnInlinedFunctionLiteralsWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmSerializableLambdaOnInlinedFunctionLiteralsWarning>
            get() = JvmSerializableLambdaOnInlinedFunctionLiteralsWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IncompatibleAnnotationTargets : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<IncompatibleAnnotationTargets>
            get() = IncompatibleAnnotationTargets::class

        public val missingJavaTargets: List<String>
        public val correspondingKotlinTargets: List<String>
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface AnnotationTargetsOnlyInJava : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<AnnotationTargetsOnlyInJava>
            get() = AnnotationTargetsOnlyInJava::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface LocalJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<LocalJvmRecord>
            get() = LocalJvmRecord::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonFinalJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonFinalJvmRecord>
            get() = NonFinalJvmRecord::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EnumJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<EnumJvmRecord>
            get() = EnumJvmRecord::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmRecordWithoutPrimaryConstructorParameters : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordWithoutPrimaryConstructorParameters>
            get() = JvmRecordWithoutPrimaryConstructorParameters::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonDataClassJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonDataClassJvmRecord>
            get() = NonDataClassJvmRecord::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonDataValueClassJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NonDataValueClassJvmRecord>
            get() = NonDataValueClassJvmRecord::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmRecordNotValParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordNotValParameter>
            get() = JvmRecordNotValParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmRecordNotLastVarargParameter : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordNotLastVarargParameter>
            get() = JvmRecordNotLastVarargParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InnerJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<InnerJvmRecord>
            get() = InnerJvmRecord::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface FieldInJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<FieldInJvmRecord>
            get() = FieldInJvmRecord::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegationByInJvmRecord : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<DelegationByInJvmRecord>
            get() = DelegationByInJvmRecord::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmRecordExtendsClass : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordExtendsClass>
            get() = JvmRecordExtendsClass::class

        public val superType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IllegalJavaLangRecordSupertype : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<IllegalJavaLangRecordSupertype>
            get() = IllegalJavaLangRecordSupertype::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmRecordsIllegalBytecodeTarget : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JvmRecordsIllegalBytecodeTarget>
            get() = JvmRecordsIllegalBytecodeTarget::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaModuleDoesNotDependOnModule : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaModuleDoesNotDependOnModule>
            get() = JavaModuleDoesNotDependOnModule::class

        public val moduleName: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaModuleDoesNotReadUnnamedModule : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaModuleDoesNotReadUnnamedModule>
            get() = JavaModuleDoesNotReadUnnamedModule::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaModuleDoesNotExportPackage : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaModuleDoesNotExportPackage>
            get() = JavaModuleDoesNotExportPackage::class

        public val moduleName: String
        public val packageName: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmDefaultWithoutCompatibilityNotInEnableMode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JvmDefaultWithoutCompatibilityNotInEnableMode>
            get() = JvmDefaultWithoutCompatibilityNotInEnableMode::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmDefaultWithCompatibilityNotInNoCompatibilityMode : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JvmDefaultWithCompatibilityNotInNoCompatibilityMode>
            get() = JvmDefaultWithCompatibilityNotInNoCompatibilityMode::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalDeclarationCannotBeAbstract : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExternalDeclarationCannotBeAbstract>
            get() = ExternalDeclarationCannotBeAbstract::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalDeclarationCannotHaveBody : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExternalDeclarationCannotHaveBody>
            get() = ExternalDeclarationCannotHaveBody::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalDeclarationInInterface : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExternalDeclarationInInterface>
            get() = ExternalDeclarationInInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalDeclarationCannotBeInlined : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<ExternalDeclarationCannotBeInlined>
            get() = ExternalDeclarationCannotBeInlined::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonSourceRepeatedAnnotation : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<NonSourceRepeatedAnnotation>
            get() = NonSourceRepeatedAnnotation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatedAnnotationWithContainer : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatedAnnotationWithContainer>
            get() = RepeatedAnnotationWithContainer::class

        public val name: ClassId
        public val explicitContainerName: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatableContainerMustHaveValueArrayError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableContainerMustHaveValueArrayError>
            get() = RepeatableContainerMustHaveValueArrayError::class

        public val container: ClassId
        public val annotation: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatableContainerHasNonDefaultParameterError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableContainerHasNonDefaultParameterError>
            get() = RepeatableContainerHasNonDefaultParameterError::class

        public val container: ClassId
        public val nonDefault: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatableContainerHasShorterRetentionError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableContainerHasShorterRetentionError>
            get() = RepeatableContainerHasShorterRetentionError::class

        public val container: ClassId
        public val retention: String
        public val annotation: ClassId
        public val annotationRetention: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatableContainerTargetSetNotASubsetError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableContainerTargetSetNotASubsetError>
            get() = RepeatableContainerTargetSetNotASubsetError::class

        public val container: ClassId
        public val annotation: ClassId
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface RepeatableAnnotationHasNestedClassNamedContainerError : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<RepeatableAnnotationHasNestedClassNamedContainerError>
            get() = RepeatableAnnotationHasNestedClassNamedContainerError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SuspensionPointInsideCriticalSection : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SuspensionPointInsideCriticalSection>
            get() = SuspensionPointInsideCriticalSection::class

        public val function: KaCallableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableJvmField : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableJvmField>
            get() = InapplicableJvmField::class

        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InapplicableJvmFieldWarning : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<InapplicableJvmFieldWarning>
            get() = InapplicableJvmFieldWarning::class

        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface IdentitySensitiveOperationsWithValueType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<IdentitySensitiveOperationsWithValueType>
            get() = IdentitySensitiveOperationsWithValueType::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedBlockOnJavaValueBasedClass : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SynchronizedBlockOnJavaValueBasedClass>
            get() = SynchronizedBlockOnJavaValueBasedClass::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedBlockOnValueClassOrPrimitiveError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SynchronizedBlockOnValueClassOrPrimitiveError>
            get() = SynchronizedBlockOnValueClassOrPrimitiveError::class

        public val valueClassOrPrimitive: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SynchronizedBlockOnValueClassOrPrimitiveWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SynchronizedBlockOnValueClassOrPrimitiveWarning>
            get() = SynchronizedBlockOnValueClassOrPrimitiveWarning::class

        public val valueClassOrPrimitive: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JvmSyntheticOnDelegate : KaFirDiagnostic<KtAnnotationEntry> {
        override val diagnosticClass: KClass<JvmSyntheticOnDelegate>
            get() = JvmSyntheticOnDelegate::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SubclassCantCallCompanionProtectedNonStatic : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SubclassCantCallCompanionProtectedNonStatic>
            get() = SubclassCantCallCompanionProtectedNonStatic::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SubclassCantCallCompanionProtectedNonStaticWarning : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SubclassCantCallCompanionProtectedNonStaticWarning>
            get() = SubclassCantCallCompanionProtectedNonStaticWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ConcurrentHashMapContainsOperatorError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<ConcurrentHashMapContainsOperatorError>
            get() = ConcurrentHashMapContainsOperatorError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SpreadOnSignaturePolymorphicCallError : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SpreadOnSignaturePolymorphicCallError>
            get() = SpreadOnSignaturePolymorphicCallError::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaSamInterfaceConstructorReference : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaSamInterfaceConstructorReference>
            get() = JavaSamInterfaceConstructorReference::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NoReflectionInClassPath : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<NoReflectionInClassPath>
            get() = NoReflectionInClassPath::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SyntheticPropertyWithoutJavaOrigin : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<SyntheticPropertyWithoutJavaOrigin>
            get() = SyntheticPropertyWithoutJavaOrigin::class

        public val originalSymbol: KaFunctionSymbol
        public val functionName: Name
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JavaFieldShadowedByKotlinProperty : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JavaFieldShadowedByKotlinProperty>
            get() = JavaFieldShadowedByKotlinProperty::class

        public val kotlinProperty: KaVariableSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MissingBuiltInDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<MissingBuiltInDeclaration>
            get() = MissingBuiltInDeclaration::class

        public val symbol: KaSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DangerousCharacters : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<DangerousCharacters>
            get() = DangerousCharacters::class

        public val characters: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImplementingFunctionInterface : KaFirDiagnostic<KtClassOrObject> {
        override val diagnosticClass: KClass<ImplementingFunctionInterface>
            get() = ImplementingFunctionInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverridingExternalFunWithOptionalParams : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<OverridingExternalFunWithOptionalParams>
            get() = OverridingExternalFunWithOptionalParams::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface OverridingExternalFunWithOptionalParamsWithFake : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<OverridingExternalFunWithOptionalParamsWithFake>
            get() = OverridingExternalFunWithOptionalParamsWithFake::class

        public val function: KaFunctionSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalEnumEntryWithBody : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalEnumEntryWithBody>
            get() = ExternalEnumEntryWithBody::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface EnumClassInExternalDeclarationWarning : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<EnumClassInExternalDeclarationWarning>
            get() = EnumClassInExternalDeclarationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlineClassInExternalDeclarationWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InlineClassInExternalDeclarationWarning>
            get() = InlineClassInExternalDeclarationWarning::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlineClassInExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<InlineClassInExternalDeclaration>
            get() = InlineClassInExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExtensionFunctionInExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExtensionFunctionInExternalDeclaration>
            get() = ExtensionFunctionInExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsExternalInheritorsOnly : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<JsExternalInheritorsOnly>
            get() = JsExternalInheritorsOnly::class

        public val parent: KaClassLikeSymbol
        public val kid: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsExternalArgument : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<JsExternalArgument>
            get() = JsExternalArgument::class

        public val argType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongExportedDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongExportedDeclaration>
            get() = WrongExportedDeclaration::class

        public val kind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonExportableType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonExportableType>
            get() = NonExportableType::class

        public val kind: String
        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonExportableTypeInSyntheticCopyFunctionWithExposedCopyVisibility : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonExportableTypeInSyntheticCopyFunctionWithExposedCopyVisibility>
            get() = NonExportableTypeInSyntheticCopyFunctionWithExposedCopyVisibility::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonExportableTypeInSyntheticCopyWithoutConsistentVisibility : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonExportableTypeInSyntheticCopyWithoutConsistentVisibility>
            get() = NonExportableTypeInSyntheticCopyWithoutConsistentVisibility::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonConsumableExportedIdentifier : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonConsumableExportedIdentifier>
            get() = NonConsumableExportedIdentifier::class

        public val name: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NamedCompanionInExportedInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NamedCompanionInExportedInterface>
            get() = NamedCompanionInExportedInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotExportedOrExternalActualDeclarationWhileExpectIsExported : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NotExportedOrExternalActualDeclarationWhileExpectIsExported>
            get() = NotExportedOrExternalActualDeclarationWhileExpectIsExported::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedNotExportedSuperInterfaceError : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedNotExportedSuperInterfaceError>
            get() = ExposedNotExportedSuperInterfaceError::class

        public val restrictingDeclaration: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExposedNotExportedSuperInterfaceWarning : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExposedNotExportedSuperInterfaceWarning>
            get() = ExposedNotExportedSuperInterfaceWarning::class

        public val restrictingDeclaration: KaClassLikeSymbol
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NestedJsExport : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NestedJsExport>
            get() = NestedJsExport::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface MultipleJsExportDefaultInOneFile : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<MultipleJsExportDefaultInOneFile>
            get() = MultipleJsExportDefaultInOneFile::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongJsExportTargetVisibility : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongJsExportTargetVisibility>
            get() = WrongJsExportTargetVisibility::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface DelegationByDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<DelegationByDynamic>
            get() = DelegationByDynamic::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface PropertyDelegationByDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<PropertyDelegationByDynamic>
            get() = PropertyDelegationByDynamic::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface SpreadOperatorInDynamicCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<SpreadOperatorInDynamicCall>
            get() = SpreadOperatorInDynamicCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongOperationWithDynamic : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongOperationWithDynamic>
            get() = WrongOperationWithDynamic::class

        public val operation: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsStaticNotInObject : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JsStaticNotInObject>
            get() = JsStaticNotInObject::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsStaticOnNonPublicMember : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JsStaticOnNonPublicMember>
            get() = JsStaticOnNonPublicMember::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsStaticOnConst : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<JsStaticOnConst>
            get() = JsStaticOnConst::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNoRuntimeWrongTarget : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeWrongTarget>
            get() = JsNoRuntimeWrongTarget::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNoRuntimeForbiddenIsCheck : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeForbiddenIsCheck>
            get() = JsNoRuntimeForbiddenIsCheck::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNoRuntimeForbiddenAsCast : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeForbiddenAsCast>
            get() = JsNoRuntimeForbiddenAsCast::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNoRuntimeForbiddenClassReference : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeForbiddenClassReference>
            get() = JsNoRuntimeForbiddenClassReference::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNoRuntimeUselessOnExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeUselessOnExternalInterface>
            get() = JsNoRuntimeUselessOnExternalInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNoRuntimeInterfaceAsReifiedTypeArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JsNoRuntimeInterfaceAsReifiedTypeArgument>
            get() = JsNoRuntimeInterfaceAsReifiedTypeArgument::class

        public val typeArgument: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsActualExternalInterfaceWhileExpectWithoutJsNoRuntime : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<JsActualExternalInterfaceWhileExpectWithoutJsNoRuntime>
            get() = JsActualExternalInterfaceWhileExpectWithoutJsNoRuntime::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JsNoRuntimeActualAnnotationsNotMatchExpect : KaFirDiagnostic<KtNamedDeclaration> {
        override val diagnosticClass: KClass<JsNoRuntimeActualAnnotationsNotMatchExpect>
            get() = JsNoRuntimeActualAnnotationsNotMatchExpect::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Syntax : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<Syntax>
            get() = Syntax::class

        public val message: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NestedExternalDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NestedExternalDeclaration>
            get() = NestedExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongExternalDeclaration : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<WrongExternalDeclaration>
            get() = WrongExternalDeclaration::class

        public val classKind: String
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NestedClassInExternalInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NestedClassInExternalInterface>
            get() = NestedClassInExternalInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface InlineExternalDeclaration : KaFirDiagnostic<KtDeclaration> {
        override val diagnosticClass: KClass<InlineExternalDeclaration>
            get() = InlineExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonAbstractMemberOfExternalInterface : KaFirDiagnostic<KtExpression> {
        override val diagnosticClass: KClass<NonAbstractMemberOfExternalInterface>
            get() = NonAbstractMemberOfExternalInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalClassConstructorPropertyParameter : KaFirDiagnostic<KtParameter> {
        override val diagnosticClass: KClass<ExternalClassConstructorPropertyParameter>
            get() = ExternalClassConstructorPropertyParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalAnonymousInitializer : KaFirDiagnostic<KtAnonymousInitializer> {
        override val diagnosticClass: KClass<ExternalAnonymousInitializer>
            get() = ExternalAnonymousInitializer::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalDelegation : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalDelegation>
            get() = ExternalDelegation::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalDelegatedConstructorCall : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalDelegatedConstructorCall>
            get() = ExternalDelegatedConstructorCall::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongBodyOfExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongBodyOfExternalDeclaration>
            get() = WrongBodyOfExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongInitializerOfExternalDeclaration : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongInitializerOfExternalDeclaration>
            get() = WrongInitializerOfExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface WrongDefaultValueForExternalFunParameter : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<WrongDefaultValueForExternalFunParameter>
            get() = WrongDefaultValueForExternalFunParameter::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CannotCheckForExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<CannotCheckForExternalInterface>
            get() = CannotCheckForExternalInterface::class

        public val targetType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface UncheckedCastToExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<UncheckedCastToExternalInterface>
            get() = UncheckedCastToExternalInterface::class

        public val sourceType: KaType
        public val targetType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalInterfaceAsClassLiteral : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalInterfaceAsClassLiteral>
            get() = ExternalInterfaceAsClassLiteral::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalInterfaceAsReifiedTypeArgument : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalInterfaceAsReifiedTypeArgument>
            get() = ExternalInterfaceAsReifiedTypeArgument::class

        public val typeArgument: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NamedCompanionInExternalInterface : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NamedCompanionInExternalInterface>
            get() = NamedCompanionInExternalInterface::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface CallToDefinedExternallyFromNonExternalDeclaration : KaFirDiagnostic<PsiElement> {
        override val diagnosticClass: KClass<CallToDefinedExternallyFromNonExternalDeclaration>
            get() = CallToDefinedExternallyFromNonExternalDeclaration::class
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ExternalTypeExtendsNonExternalType : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<ExternalTypeExtendsNonExternalType>
            get() = ExternalTypeExtendsNonExternalType::class

        public val superType: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NonExternalDeclarationInInappropriateFile : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<NonExternalDeclarationInInappropriateFile>
            get() = NonExternalDeclarationInInappropriateFile::class

        public val type: KaType
    }

    @KaUnstableDiagnosticApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface JscodeArgumentNonConstExpression : KaFirDiagnostic<KtElement> {
        override val diagnosticClass: KClass<JscodeArgumentNonConstExpression>
            get() = JscodeArgumentNonConstExpression::class
    }

}
