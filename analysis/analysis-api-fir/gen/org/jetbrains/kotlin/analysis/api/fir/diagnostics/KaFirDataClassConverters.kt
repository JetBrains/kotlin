/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.psi
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

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal val KT_DIAGNOSTIC_CONVERTER = KaDiagnosticConverterBuilder.buildConverter {
    add(FirErrors.UNSUPPORTED) { firDiagnostic ->
        UnsupportedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSUPPORTED_FEATURE) { firDiagnostic ->
        UnsupportedFeatureImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSUPPORTED_SUSPEND_TEST) { firDiagnostic ->
        UnsupportedSuspendTestImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEW_INFERENCE_ERROR) { firDiagnostic ->
        NewInferenceErrorImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OTHER_ERROR) { firDiagnostic ->
        OtherErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OTHER_ERROR_WITH_REASON) { firDiagnostic ->
        OtherErrorWithReasonImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_CONST_EXPRESSION) { firDiagnostic ->
        IllegalConstExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_UNDERSCORE) { firDiagnostic ->
        IllegalUnderscoreImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPRESSION_EXPECTED) { firDiagnostic ->
        ExpressionExpectedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNMENT_IN_EXPRESSION_CONTEXT) { firDiagnostic ->
        AssignmentInExpressionContextImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP) { firDiagnostic ->
        BreakOrContinueOutsideALoopImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_A_LOOP_LABEL) { firDiagnostic ->
        NotALoopLabelImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY) { firDiagnostic ->
        BreakOrContinueJumpsAcrossFunctionBoundaryImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIABLE_EXPECTED) { firDiagnostic ->
        VariableExpectedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATION_IN_INTERFACE) { firDiagnostic ->
        DelegationInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATION_NOT_TO_INTERFACE) { firDiagnostic ->
        DelegationNotToInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NESTED_CLASS_NOT_ALLOWED) { firDiagnostic ->
        NestedClassNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NESTED_CLASS_NOT_ALLOWED_IN_LOCAL.errorFactory) { firDiagnostic ->
        NestedClassNotAllowedInLocalErrorImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NESTED_CLASS_NOT_ALLOWED_IN_LOCAL.warningFactory) { firDiagnostic ->
        NestedClassNotAllowedInLocalWarningImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCORRECT_CHARACTER_LITERAL) { firDiagnostic ->
        IncorrectCharacterLiteralImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EMPTY_CHARACTER_LITERAL) { firDiagnostic ->
        EmptyCharacterLiteralImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL) { firDiagnostic ->
        TooManyCharactersInCharacterLiteralImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_ESCAPE) { firDiagnostic ->
        IllegalEscapeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INT_LITERAL_OUT_OF_RANGE) { firDiagnostic ->
        IntLiteralOutOfRangeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FLOAT_LITERAL_OUT_OF_RANGE) { firDiagnostic ->
        FloatLiteralOutOfRangeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_LONG_SUFFIX) { firDiagnostic ->
        WrongLongSuffixImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH) { firDiagnostic ->
        UnsignedLiteralWithoutDeclarationsOnClasspathImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DIVISION_BY_ZERO) { firDiagnostic ->
        DivisionByZeroImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_OR_VAR_ON_LOOP_PARAMETER) { firDiagnostic ->
        ValOrVarOnLoopParameterImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_OR_VAR_ON_FUN_PARAMETER) { firDiagnostic ->
        ValOrVarOnFunParameterImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_OR_VAR_ON_CATCH_PARAMETER) { firDiagnostic ->
        ValOrVarOnCatchParameterImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER) { firDiagnostic ->
        ValOrVarOnSecondaryConstructorParameterImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_SETTER) { firDiagnostic ->
        InvisibleSetterImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INNER_ON_TOP_LEVEL_SCRIPT_CLASS.errorFactory) { firDiagnostic ->
        InnerOnTopLevelScriptClassErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INNER_ON_TOP_LEVEL_SCRIPT_CLASS.warningFactory) { firDiagnostic ->
        InnerOnTopLevelScriptClassWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ERROR_SUPPRESSION) { firDiagnostic ->
        ErrorSuppressionImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_CONSTRUCTOR_KEYWORD) { firDiagnostic ->
        MissingConstructorKeywordImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_INTERPOLATION_PREFIX) { firDiagnostic ->
        RedundantInterpolationPrefixImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRAPPED_LHS_IN_ASSIGNMENT.errorFactory) { firDiagnostic ->
        WrappedLhsInAssignmentErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRAPPED_LHS_IN_ASSIGNMENT.warningFactory) { firDiagnostic ->
        WrappedLhsInAssignmentWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_REFERENCE) { firDiagnostic ->
        InvisibleReferenceImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_REFERENCE) { firDiagnostic ->
        UnresolvedReferenceImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_LABEL) { firDiagnostic ->
        UnresolvedLabelImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_LABEL) { firDiagnostic ->
        AmbiguousLabelImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LABEL_NAME_CLASH) { firDiagnostic ->
        LabelNameClashImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DESERIALIZATION_ERROR) { firDiagnostic ->
        DeserializationErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ERROR_FROM_JAVA_RESOLUTION) { firDiagnostic ->
        ErrorFromJavaResolutionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_STDLIB_CLASS) { firDiagnostic ->
        MissingStdlibClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_THIS) { firDiagnostic ->
        NoThisImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATION_ERROR) { firDiagnostic ->
        DeprecationErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATION) { firDiagnostic ->
        DeprecationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VERSION_REQUIREMENT_DEPRECATION_ERROR) { firDiagnostic ->
        VersionRequirementDeprecationErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VERSION_REQUIREMENT_DEPRECATION) { firDiagnostic ->
        VersionRequirementDeprecationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPEALIAS_EXPANSION_DEPRECATION_ERROR) { firDiagnostic ->
        TypealiasExpansionDeprecationErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPEALIAS_EXPANSION_DEPRECATION) { firDiagnostic ->
        TypealiasExpansionDeprecationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.API_NOT_AVAILABLE) { firDiagnostic ->
        ApiNotAvailableImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_REFERENCE_WRONG_RECEIVER) { firDiagnostic ->
        UnresolvedReferenceWrongReceiverImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_IMPORT) { firDiagnostic ->
        UnresolvedImportImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PLACEHOLDER_PROJECTION_IN_QUALIFIER) { firDiagnostic ->
        PlaceholderProjectionInQualifierImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE) { firDiagnostic ->
        DuplicateParameterNameInFunctionTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_DEPENDENCY_CLASS) { firDiagnostic ->
        MissingDependencyClassImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE) { firDiagnostic ->
        MissingDependencyClassInExpressionTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_DEPENDENCY_SUPERCLASS) { firDiagnostic ->
        MissingDependencySuperclassImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_DEPENDENCY_SUPERCLASS_WARNING) { firDiagnostic ->
        MissingDependencySuperclassWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT) { firDiagnostic ->
        MissingDependencySuperclassInTypeArgumentImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER) { firDiagnostic ->
        MissingDependencyClassInLambdaParameterImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_DEPENDENCY_CLASS_IN_LAMBDA_RECEIVER) { firDiagnostic ->
        MissingDependencyClassInLambdaReceiverImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS) { firDiagnostic ->
        CreatingAnInstanceOfAbstractClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_CONSTRUCTOR) { firDiagnostic ->
        NoConstructorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUNCTION_CALL_EXPECTED) { firDiagnostic ->
        FunctionCallExpectedImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_SELECTOR) { firDiagnostic ->
        IllegalSelectorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_RECEIVER_ALLOWED) { firDiagnostic ->
        NoReceiverAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUNCTION_EXPECTED) { firDiagnostic ->
        FunctionExpectedImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INTERFACE_AS_FUNCTION) { firDiagnostic ->
        InterfaceAsFunctionImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_CLASS_AS_FUNCTION) { firDiagnostic ->
        ExpectClassAsFunctionImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INNER_CLASS_CONSTRUCTOR_NO_RECEIVER) { firDiagnostic ->
        InnerClassConstructorNoReceiverImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PLUGIN_AMBIGUOUS_INTERCEPTED_SYMBOL) { firDiagnostic ->
        PluginAmbiguousInterceptedSymbolImpl(
            firDiagnostic.a.map { string ->
                string
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESOLUTION_TO_CLASSIFIER) { firDiagnostic ->
        ResolutionToClassifierImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_ALTERED_ASSIGN) { firDiagnostic ->
        AmbiguousAlteredAssignImpl(
            firDiagnostic.a.map { string ->
                string
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR) { firDiagnostic ->
        SelfCallInNestedObjectConstructorErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPER_IS_NOT_AN_EXPRESSION) { firDiagnostic ->
        SuperIsNotAnExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPER_NOT_AVAILABLE) { firDiagnostic ->
        SuperNotAvailableImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_SUPER_CALL) { firDiagnostic ->
        AbstractSuperCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_SUPER_CALL_WARNING) { firDiagnostic ->
        AbstractSuperCallWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL) { firDiagnostic ->
        InstanceAccessBeforeSuperCallImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPER_CALL_WITH_DEFAULT_PARAMETERS) { firDiagnostic ->
        SuperCallWithDefaultParametersImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER) { firDiagnostic ->
        InterfaceCantCallDefaultMethodViaSuperImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS) { firDiagnostic ->
        JavaClassInheritsKtPrivateClassImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_A_SUPERTYPE) { firDiagnostic ->
        NotASupertypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER) { firDiagnostic ->
        TypeArgumentsRedundantInSuperQualifierImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE) { firDiagnostic ->
        SuperclassNotAccessibleFromInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_INITIALIZED_IN_INTERFACE) { firDiagnostic ->
        SupertypeInitializedInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INTERFACE_WITH_SUPERCLASS) { firDiagnostic ->
        InterfaceWithSuperclassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FINAL_SUPERTYPE) { firDiagnostic ->
        FinalSupertypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASS_CANNOT_BE_EXTENDED_DIRECTLY) { firDiagnostic ->
        ClassCannotBeExtendedDirectlyImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE) { firDiagnostic ->
        SupertypeIsExtensionOrContextFunctionTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SINGLETON_IN_SUPERTYPE) { firDiagnostic ->
        SingletonInSupertypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_SUPERTYPE) { firDiagnostic ->
        NullableSupertypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_SUPERTYPE_THROUGH_TYPEALIAS.errorFactory) { firDiagnostic ->
        NullableSupertypeThroughTypealiasErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_SUPERTYPE_THROUGH_TYPEALIAS.warningFactory) { firDiagnostic ->
        NullableSupertypeThroughTypealiasWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_CLASSES_IN_SUPERTYPE_LIST) { firDiagnostic ->
        ManyClassesInSupertypeListImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_APPEARS_TWICE) { firDiagnostic ->
        SupertypeAppearsTwiceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASS_IN_SUPERTYPE_FOR_ENUM) { firDiagnostic ->
        ClassInSupertypeForEnumImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_SUPERTYPE) { firDiagnostic ->
        SealedSupertypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_SUPERTYPE_IN_LOCAL_CLASS) { firDiagnostic ->
        SealedSupertypeInLocalClassImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_INHERITOR_IN_DIFFERENT_PACKAGE) { firDiagnostic ->
        SealedInheritorInDifferentPackageImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_INHERITOR_IN_DIFFERENT_MODULE) { firDiagnostic ->
        SealedInheritorInDifferentModuleImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASS_INHERITS_JAVA_SEALED_CLASS) { firDiagnostic ->
        ClassInheritsJavaSealedClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSUPPORTED_SEALED_FUN_INTERFACE) { firDiagnostic ->
        UnsupportedSealedFunInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE) { firDiagnostic ->
        SupertypeNotAClassOrInterfaceImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION) { firDiagnostic ->
        UnsupportedInheritanceFromJavaMemberReferencingKotlinFunctionImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CYCLIC_INHERITANCE_HIERARCHY) { firDiagnostic ->
        CyclicInheritanceHierarchyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPANDED_TYPE_CANNOT_BE_INHERITED) { firDiagnostic ->
        ExpandedTypeCannotBeInheritedImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE) { firDiagnostic ->
        ProjectionInImmediateArgumentToSupertypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCONSISTENT_TYPE_PARAMETER_VALUES) { firDiagnostic ->
        InconsistentTypeParameterValuesImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCONSISTENT_TYPE_PARAMETER_BOUNDS) { firDiagnostic ->
        InconsistentTypeParameterBoundsImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_SUPER) { firDiagnostic ->
        AmbiguousSuperImpl(
            firDiagnostic.a.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.WRONG_MULTIPLE_INHERITANCE) { firDiagnostic ->
        WrongMultipleInheritanceImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONSTRUCTOR_IN_OBJECT) { firDiagnostic ->
        ConstructorInObjectImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONSTRUCTOR_IN_INTERFACE) { firDiagnostic ->
        ConstructorInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PRIVATE_CONSTRUCTOR_IN_ENUM) { firDiagnostic ->
        NonPrivateConstructorInEnumImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED) { firDiagnostic ->
        NonPrivateOrProtectedConstructorInSealedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CYCLIC_CONSTRUCTOR_DELEGATION_CALL) { firDiagnostic ->
        CyclicConstructorDelegationCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED) { firDiagnostic ->
        PrimaryConstructorDelegationCallExpectedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL) { firDiagnostic ->
        ProtectedConstructorNotInSuperCallImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_NOT_INITIALIZED) { firDiagnostic ->
        SupertypeNotInitializedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR) { firDiagnostic ->
        SupertypeInitializedWithoutPrimaryConstructorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR) { firDiagnostic ->
        DelegationSuperCallInEnumConstructorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPLICIT_DELEGATION_CALL_REQUIRED) { firDiagnostic ->
        ExplicitDelegationCallRequiredImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_CLASS_CONSTRUCTOR_CALL) { firDiagnostic ->
        SealedClassConstructorCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_CONSISTENT_COPY_AND_EXPOSED_COPY_ARE_INCOMPATIBLE_ANNOTATIONS) { firDiagnostic ->
        DataClassConsistentCopyAndExposedCopyAreIncompatibleAnnotationsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET) { firDiagnostic ->
        DataClassConsistentCopyWrongAnnotationTargetImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED.errorFactory) { firDiagnostic ->
        DataClassCopyVisibilityWillBeChangedErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED.warningFactory) { firDiagnostic ->
        DataClassCopyVisibilityWillBeChangedWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_INVISIBLE_COPY_USAGE.errorFactory) { firDiagnostic ->
        DataClassInvisibleCopyUsageErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_INVISIBLE_COPY_USAGE.warningFactory) { firDiagnostic ->
        DataClassInvisibleCopyUsageWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_WITHOUT_PARAMETERS) { firDiagnostic ->
        DataClassWithoutParametersImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_VARARG_PARAMETER) { firDiagnostic ->
        DataClassVarargParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_NOT_PROPERTY_PARAMETER) { firDiagnostic ->
        DataClassNotPropertyParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR) { firDiagnostic ->
        AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST) { firDiagnostic ->
        AnnotationArgumentMustBeConstImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST) { firDiagnostic ->
        AnnotationArgumentMustBeEnumConstImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL) { firDiagnostic ->
        AnnotationArgumentMustBeKclassLiteralImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_CLASS_MEMBER) { firDiagnostic ->
        AnnotationClassMemberImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT) { firDiagnostic ->
        AnnotationParameterDefaultValueMustBeConstantImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER) { firDiagnostic ->
        InvalidTypeOfAnnotationMemberImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER.errorFactory) { firDiagnostic ->
        ProjectionInTypeOfAnnotationMemberErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER.warningFactory) { firDiagnostic ->
        ProjectionInTypeOfAnnotationMemberWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_ANNOTATION_CLASS_ERROR) { firDiagnostic ->
        LocalAnnotationClassErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER) { firDiagnostic ->
        MissingValOnAnnotationParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION) { firDiagnostic ->
        NonConstValUsedInConstantExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CYCLE_IN_ANNOTATION_PARAMETER.errorFactory) { firDiagnostic ->
        CycleInAnnotationParameterErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CYCLE_IN_ANNOTATION_PARAMETER.warningFactory) { firDiagnostic ->
        CycleInAnnotationParameterWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_CLASS_CONSTRUCTOR_CALL) { firDiagnostic ->
        AnnotationClassConstructorCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ENUM_CLASS_CONSTRUCTOR_CALL) { firDiagnostic ->
        EnumClassConstructorCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_AN_ANNOTATION_CLASS) { firDiagnostic ->
        NotAnAnnotationClassImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER) { firDiagnostic ->
        NullableTypeOfAnnotationMemberImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_ANNOTATION_PARAMETER) { firDiagnostic ->
        VarAnnotationParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPES_FOR_ANNOTATION_CLASS) { firDiagnostic ->
        SupertypesForAnnotationClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT) { firDiagnostic ->
        AnnotationUsedAsAnnotationArgumentImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ON_ANNOTATION_ARGUMENT) { firDiagnostic ->
        AnnotationOnAnnotationArgumentImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE) { firDiagnostic ->
        IllegalKotlinVersionStringValueImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEWER_VERSION_IN_SINCE_KOTLIN) { firDiagnostic ->
        NewerVersionInSinceKotlinImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS) { firDiagnostic ->
        DeprecatedSinceKotlinWithUnorderedVersionsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS) { firDiagnostic ->
        DeprecatedSinceKotlinWithoutArgumentsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED) { firDiagnostic ->
        DeprecatedSinceKotlinWithoutDeprecatedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL) { firDiagnostic ->
        DeprecatedSinceKotlinWithDeprecatedLevelImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE) { firDiagnostic ->
        DeprecatedSinceKotlinOutsideKotlinSubpackageImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN) { firDiagnostic ->
        KotlinActualAnnotationHasNoEffectInKotlinImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERRIDE_DEPRECATION) { firDiagnostic ->
        OverrideDeprecationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_ANNOTATION) { firDiagnostic ->
        RedundantAnnotationImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ON_SUPERCLASS_ERROR) { firDiagnostic ->
        AnnotationOnSuperclassErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_ERROR) { firDiagnostic ->
        RestrictedRetentionForExpressionAnnotationErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_ANNOTATION_TARGET) { firDiagnostic ->
        WrongAnnotationTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { kotlinTarget ->
                kotlinTarget
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_ANNOTATION_TARGET_WARNING) { firDiagnostic ->
        WrongAnnotationTargetWarningImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { kotlinTarget ->
                kotlinTarget
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET) { firDiagnostic ->
        WrongAnnotationTargetWithUseSiteTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic.c.map { kotlinTarget ->
                kotlinTarget
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION.errorFactory) { firDiagnostic ->
        AnnotationWithUseSiteTargetOnExpressionErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION.warningFactory) { firDiagnostic ->
        AnnotationWithUseSiteTargetOnExpressionWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY) { firDiagnostic ->
        InapplicableTargetOnPropertyImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY_WARNING) { firDiagnostic ->
        InapplicableTargetOnPropertyWarningImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE) { firDiagnostic ->
        InapplicableTargetPropertyImmutableImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE) { firDiagnostic ->
        InapplicableTargetPropertyHasNoDelegateImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD) { firDiagnostic ->
        InapplicableTargetPropertyHasNoBackingFieldImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_PARAM_TARGET) { firDiagnostic ->
        InapplicableParamTargetImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_ANNOTATION_TARGET) { firDiagnostic ->
        RedundantAnnotationTargetImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_FILE_TARGET) { firDiagnostic ->
        InapplicableFileTargetImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_ALL_TARGET) { firDiagnostic ->
        InapplicableAllTargetImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION) { firDiagnostic ->
        InapplicableAllTargetInMultiAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REPEATED_ANNOTATION) { firDiagnostic ->
        RepeatedAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REPEATED_ANNOTATION_WARNING) { firDiagnostic ->
        RepeatedAnnotationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_A_CLASS) { firDiagnostic ->
        NotAClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_EXTENSION_FUNCTION_TYPE) { firDiagnostic ->
        WrongExtensionFunctionTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_EXTENSION_FUNCTION_TYPE_WARNING) { firDiagnostic ->
        WrongExtensionFunctionTypeWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_IN_WHERE_CLAUSE_ERROR) { firDiagnostic ->
        AnnotationInWhereClauseErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_IN_CONTRACT_ERROR) { firDiagnostic ->
        AnnotationInContractErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPILER_REQUIRED_ANNOTATION_AMBIGUITY) { firDiagnostic ->
        CompilerRequiredAnnotationAmbiguityImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_ANNOTATION_ARGUMENT) { firDiagnostic ->
        AmbiguousAnnotationArgumentImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VOLATILE_ON_VALUE) { firDiagnostic ->
        VolatileOnValueImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VOLATILE_ON_DELEGATE) { firDiagnostic ->
        VolatileOnDelegateImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION) { firDiagnostic ->
        NonSourceAnnotationOnInlinedLambdaExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.POTENTIALLY_NON_REPORTED_ANNOTATION) { firDiagnostic ->
        PotentiallyNonReportedAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD) { firDiagnostic ->
        AnnotationWillBeAppliedAlsoToPropertyOrFieldImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE) { firDiagnostic ->
        AnnotationsOnBlockLevelExpressionOnTheSameLineImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED) { firDiagnostic ->
        IgnorabilityAnnotationsWithCheckerDisabledImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DSL_MARKER_PROPAGATES_TO_MANY) { firDiagnostic ->
        DslMarkerPropagatesToManyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR) { firDiagnostic ->
        JsModuleProhibitedOnVarImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_MODULE_PROHIBITED_ON_NON_NATIVE) { firDiagnostic ->
        JsModuleProhibitedOnNonNativeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NESTED_JS_MODULE_PROHIBITED) { firDiagnostic ->
        NestedJsModuleProhibitedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE) { firDiagnostic ->
        CallFromUmdMustBeJsModuleAndJsNonModuleImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM) { firDiagnostic ->
        CallToJsModuleWithoutModuleSystemImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM) { firDiagnostic ->
        CallToJsNonModuleWithModuleSystemImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.RUNTIME_ANNOTATION_NOT_SUPPORTED) { firDiagnostic ->
        RuntimeAnnotationNotSupportedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION) { firDiagnostic ->
        RuntimeAnnotationOnExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN) { firDiagnostic ->
        NativeAnnotationsAllowedOnlyOnMemberOrExtensionFunImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER) { firDiagnostic ->
        NativeIndexerKeyShouldBeStringOrNumberImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NATIVE_INDEXER_WRONG_PARAMETER_COUNT) { firDiagnostic ->
        NativeIndexerWrongParameterCountImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS) { firDiagnostic ->
        NativeIndexerCanNotHaveDefaultArgumentsImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE) { firDiagnostic ->
        NativeGetterReturnTypeShouldBeNullableImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NATIVE_SETTER_WRONG_RETURN_TYPE) { firDiagnostic ->
        NativeSetterWrongReturnTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_NAME_IS_NOT_ON_ALL_ACCESSORS) { firDiagnostic ->
        JsNameIsNotOnAllAccessorsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_NAME_PROHIBITED_FOR_NAMED_NATIVE) { firDiagnostic ->
        JsNameProhibitedForNamedNativeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_NAME_PROHIBITED_FOR_OVERRIDE) { firDiagnostic ->
        JsNameProhibitedForOverrideImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED) { firDiagnostic ->
        JsNameOnPrimaryConstructorProhibitedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_NAME_ON_ACCESSOR_AND_PROPERTY) { firDiagnostic ->
        JsNameOnAccessorAndPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY) { firDiagnostic ->
        JsNameProhibitedForExtensionPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_BUILTIN_NAME_CLASH) { firDiagnostic ->
        JsBuiltinNameClashImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NAME_CONTAINS_ILLEGAL_CHARS) { firDiagnostic ->
        NameContainsIllegalCharsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_NAME_CLASH) { firDiagnostic ->
        JsNameClashImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_FAKE_NAME_CLASH) { firDiagnostic ->
        JsFakeNameClashImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.WRONG_JS_QUALIFIER) { firDiagnostic ->
        WrongJsQualifierImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_USAGE) { firDiagnostic ->
        OptInUsageImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_USAGE_ERROR) { firDiagnostic ->
        OptInUsageErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_TO_INHERITANCE) { firDiagnostic ->
        OptInToInheritanceImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_TO_INHERITANCE_ERROR) { firDiagnostic ->
        OptInToInheritanceErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_OVERRIDE) { firDiagnostic ->
        OptInOverrideImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_OVERRIDE_ERROR) { firDiagnostic ->
        OptInOverrideErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION) { firDiagnostic ->
        OptInCanOnlyBeUsedAsAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN) { firDiagnostic ->
        OptInMarkerCanOnlyBeUsedAsAnnotationOrArgumentInOptInImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_WITHOUT_ARGUMENTS) { firDiagnostic ->
        OptInWithoutArgumentsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_ARGUMENT_IS_NOT_MARKER) { firDiagnostic ->
        OptInArgumentIsNotMarkerImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_MARKER_WITH_WRONG_TARGET) { firDiagnostic ->
        OptInMarkerWithWrongTargetImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_MARKER_WITH_WRONG_RETENTION) { firDiagnostic ->
        OptInMarkerWithWrongRetentionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET) { firDiagnostic ->
        OptInMarkerOnWrongTargetImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_MARKER_ON_OVERRIDE) { firDiagnostic ->
        OptInMarkerOnOverrideImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPT_IN_MARKER_ON_OVERRIDE_WARNING) { firDiagnostic ->
        OptInMarkerOnOverrideWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUBCLASS_OPT_IN_INAPPLICABLE) { firDiagnostic ->
        SubclassOptInInapplicableImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUBCLASS_OPT_IN_ARGUMENT_IS_NOT_MARKER) { firDiagnostic ->
        SubclassOptInArgumentIsNotMarkerImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE) { firDiagnostic ->
        ExposedTypealiasExpandedTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_FUNCTION_RETURN_TYPE) { firDiagnostic ->
        ExposedFunctionReturnTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_RECEIVER_TYPE) { firDiagnostic ->
        ExposedReceiverTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE) { firDiagnostic ->
        ExposedPropertyTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR) { firDiagnostic ->
        ExposedPropertyTypeInConstructorErrorImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PARAMETER_TYPE) { firDiagnostic ->
        ExposedParameterTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_INTERFACE) { firDiagnostic ->
        ExposedSuperInterfaceImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_CLASS) { firDiagnostic ->
        ExposedSuperClassImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPE_PARAMETER_BOUND) { firDiagnostic ->
        ExposedTypeParameterBoundImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING) { firDiagnostic ->
        ExposedTypeParameterBoundDeprecationWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PACKAGE_PRIVATE_TYPE_FROM_INTERNAL_WARNING) { firDiagnostic ->
        ExposedPackagePrivateTypeFromInternalWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_INFIX_MODIFIER) { firDiagnostic ->
        InapplicableInfixModifierImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REPEATED_MODIFIER) { firDiagnostic ->
        RepeatedModifierImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_MODIFIER) { firDiagnostic ->
        RedundantModifierImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER) { firDiagnostic ->
        DeprecatedModifierImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER_PAIR) { firDiagnostic ->
        DeprecatedModifierPairImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER_FOR_TARGET) { firDiagnostic ->
        DeprecatedModifierForTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_MODIFIER_FOR_TARGET) { firDiagnostic ->
        RedundantModifierForTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_MODIFIERS) { firDiagnostic ->
        IncompatibleModifiersImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_OPEN_IN_INTERFACE) { firDiagnostic ->
        RedundantOpenInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_MODIFIER_TARGET) { firDiagnostic ->
        WrongModifierTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPERATOR_MODIFIER_REQUIRED) { firDiagnostic ->
        OperatorModifierRequiredImpl(
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPERATOR_CALL_ON_CONSTRUCTOR) { firDiagnostic ->
        OperatorCallOnConstructorImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFIX_MODIFIER_REQUIRED) { firDiagnostic ->
        InfixModifierRequiredImpl(
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION) { firDiagnostic ->
        WrongModifierContainingDeclarationImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION) { firDiagnostic ->
        DeprecatedModifierContainingDeclarationImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_OPERATOR_MODIFIER) { firDiagnostic ->
        InapplicableOperatorModifierImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_OPERATOR_MODIFIER_WARNING) { firDiagnostic ->
        InapplicableOperatorModifierWarningImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_EXPLICIT_VISIBILITY_IN_API_MODE) { firDiagnostic ->
        NoExplicitVisibilityInApiModeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING) { firDiagnostic ->
        NoExplicitVisibilityInApiModeWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE) { firDiagnostic ->
        NoExplicitReturnTypeInApiModeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING) { firDiagnostic ->
        NoExplicitReturnTypeInApiModeWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_SUSPEND_FUNCTION) { firDiagnostic ->
        AnonymousSuspendFunctionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_NOT_TOP_LEVEL) { firDiagnostic ->
        ValueClassNotTopLevelImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_NOT_FINAL) { firDiagnostic ->
        ValueClassNotFinalImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS) { firDiagnostic ->
        AbsenceOfPrimaryConstructorForValueClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE) { firDiagnostic ->
        InlineClassConstructorWrongParametersSizeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_EMPTY_CONSTRUCTOR) { firDiagnostic ->
        ValueClassEmptyConstructorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER) { firDiagnostic ->
        ValueClassConstructorNotFinalReadOnlyParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS) { firDiagnostic ->
        PropertyWithBackingFieldInsideValueClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATED_PROPERTY_INSIDE_VALUE_CLASS) { firDiagnostic ->
        DelegatedPropertyInsideValueClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE) { firDiagnostic ->
        ValueClassHasInapplicableParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION) { firDiagnostic ->
        ValueClassCannotImplementInterfaceByDelegationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_CANNOT_EXTEND_CLASSES) { firDiagnostic ->
        ValueClassCannotExtendClassesImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_CANNOT_BE_RECURSIVE) { firDiagnostic ->
        ValueClassCannotBeRecursiveImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTI_FIELD_VALUE_CLASS_PRIMARY_CONSTRUCTOR_DEFAULT_PARAMETER) { firDiagnostic ->
        MultiFieldValueClassPrimaryConstructorDefaultParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS) { firDiagnostic ->
        SecondaryConstructorWithBodyInsideValueClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESERVED_MEMBER_INSIDE_VALUE_CLASS) { firDiagnostic ->
        ReservedMemberInsideValueClassImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS) { firDiagnostic ->
        ReservedMemberFromInterfaceInsideValueClassImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS) { firDiagnostic ->
        TypeArgumentOnTypedValueClassEqualsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INNER_CLASS_INSIDE_VALUE_CLASS) { firDiagnostic ->
        InnerClassInsideValueClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_CANNOT_BE_CLONEABLE) { firDiagnostic ->
        ValueClassCannotBeCloneableImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS) { firDiagnostic ->
        ValueClassCannotHaveContextReceiversImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET) { firDiagnostic ->
        AnnotationOnIllegalMultiFieldValueClassTypedTargetImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NONE_APPLICABLE) { firDiagnostic ->
        NoneApplicableImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_CANDIDATE) { firDiagnostic ->
        InapplicableCandidateImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_MISMATCH) { firDiagnostic ->
        TypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR) { firDiagnostic ->
        TypeInferenceOnlyInputTypesErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.THROWABLE_TYPE_MISMATCH) { firDiagnostic ->
        ThrowableTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONDITION_TYPE_MISMATCH) { firDiagnostic ->
        ConditionTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ARGUMENT_TYPE_MISMATCH) { firDiagnostic ->
        ArgumentTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MEMBER_PROJECTED_OUT) { firDiagnostic ->
        MemberProjectedOutImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULL_FOR_NONNULL_TYPE) { firDiagnostic ->
        NullForNonnullTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_LATEINIT_MODIFIER) { firDiagnostic ->
        InapplicableLateinitModifierImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARARG_OUTSIDE_PARENTHESES) { firDiagnostic ->
        VarargOutsideParenthesesImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NAMED_ARGUMENTS_NOT_ALLOWED) { firDiagnostic ->
        NamedArgumentsNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_VARARG_SPREAD) { firDiagnostic ->
        NonVarargSpreadImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ARGUMENT_PASSED_TWICE) { firDiagnostic ->
        ArgumentPassedTwiceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TOO_MANY_ARGUMENTS) { firDiagnostic ->
        TooManyArgumentsImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE) { firDiagnostic ->
        UnexpectedTrailingLambdaOnANewLineImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_VALUE_FOR_PARAMETER) { firDiagnostic ->
        NoValueForParameterImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NAMED_PARAMETER_NOT_FOUND) { firDiagnostic ->
        NamedParameterNotFoundImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NAME_FOR_AMBIGUOUS_PARAMETER) { firDiagnostic ->
        NameForAmbiguousParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MIXING_NAMED_AND_POSITIONAL_ARGUMENTS) { firDiagnostic ->
        MixingNamedAndPositionalArgumentsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNMENT_TYPE_MISMATCH) { firDiagnostic ->
        AssignmentTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESULT_TYPE_MISMATCH) { firDiagnostic ->
        ResultTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_LAMBDA_EXPRESSION_ARGUMENTS) { firDiagnostic ->
        ManyLambdaExpressionArgumentsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SPREAD_OF_NULLABLE) { firDiagnostic ->
        SpreadOfNullableImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION.errorFactory) { firDiagnostic ->
        AssigningSingleElementToVarargInNamedFormFunctionErrorImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION.warningFactory) { firDiagnostic ->
        AssigningSingleElementToVarargInNamedFormFunctionWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION.errorFactory) { firDiagnostic ->
        AssigningSingleElementToVarargInNamedFormAnnotationErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION.warningFactory) { firDiagnostic ->
        AssigningSingleElementToVarargInNamedFormAnnotationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION) { firDiagnostic ->
        RedundantSpreadOperatorInNamedFormInAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION) { firDiagnostic ->
        RedundantSpreadOperatorInNamedFormInFunctionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE) { firDiagnostic ->
        NestedClassAccessedViaInstanceReferenceImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPARE_TO_TYPE_MISMATCH) { firDiagnostic ->
        CompareToTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_FUNCTION_TYPE_MISMATCH) { firDiagnostic ->
        HasNextFunctionTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING) { firDiagnostic ->
        IllegalTypeArgumentForVarargParameterWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERLOAD_RESOLUTION_AMBIGUITY) { firDiagnostic ->
        OverloadResolutionAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGN_OPERATOR_AMBIGUITY) { firDiagnostic ->
        AssignOperatorAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ITERATOR_AMBIGUITY) { firDiagnostic ->
        IteratorAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_FUNCTION_AMBIGUITY) { firDiagnostic ->
        HasNextFunctionAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEXT_AMBIGUITY) { firDiagnostic ->
        NextAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_FUNCTION_TYPE_KIND) { firDiagnostic ->
        AmbiguousFunctionTypeKindImpl(
            firDiagnostic.a.map { functionTypeKind ->
                functionTypeKind
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_CONTEXT_ARGUMENT) { firDiagnostic ->
        NoContextArgumentImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_CONTEXT_ARGUMENT) { firDiagnostic ->
        AmbiguousContextArgumentImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER) { firDiagnostic ->
        AmbiguousCallWithImplicitContextReceiverImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL) { firDiagnostic ->
        UnsupportedContextualDeclarationCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS) { firDiagnostic ->
        SubtypingBetweenContextReceiversImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONTEXT_PARAMETERS_WITH_BACKING_FIELD) { firDiagnostic ->
        ContextParametersWithBackingFieldImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONTEXT_RECEIVERS_DEPRECATED) { firDiagnostic ->
        ContextReceiversDeprecatedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONTEXT_CLASS_OR_CONSTRUCTOR) { firDiagnostic ->
        ContextClassOrConstructorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONTEXT_PARAMETER_WITHOUT_NAME) { firDiagnostic ->
        ContextParameterWithoutNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONTEXT_PARAMETER_WITH_DEFAULT) { firDiagnostic ->
        ContextParameterWithDefaultImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION) { firDiagnostic ->
        CallableReferenceToContextualDeclarationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_CONTEXT_LISTS) { firDiagnostic ->
        MultipleContextListsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE) { firDiagnostic ->
        NamedContextParameterInFunctionTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONTEXTUAL_OVERLOAD_SHADOWED) { firDiagnostic ->
        ContextualOverloadShadowedImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RECURSION_IN_IMPLICIT_TYPES) { firDiagnostic ->
        RecursionInImplicitTypesImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERENCE_ERROR) { firDiagnostic ->
        InferenceErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT) { firDiagnostic ->
        ProjectionOnNonClassTypeArgumentImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_VIOLATED) { firDiagnostic ->
        UpperBoundViolatedImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_VIOLATED_DEPRECATION_WARNING) { firDiagnostic ->
        UpperBoundViolatedDeprecationWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION) { firDiagnostic ->
        UpperBoundViolatedInTypealiasExpansionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING) { firDiagnostic ->
        UpperBoundViolatedInTypealiasExpansionDeprecationWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED) { firDiagnostic ->
        TypeArgumentsNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED) { firDiagnostic ->
        TypeArgumentsForOuterClassWhenNestedReferencedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS) { firDiagnostic ->
        WrongNumberOfTypeArgumentsImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_TYPE_ARGUMENTS_ON_RHS) { firDiagnostic ->
        NoTypeArgumentsOnRhsImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OUTER_CLASS_ARGUMENTS_REQUIRED) { firDiagnostic ->
        OuterClassArgumentsRequiredImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_IN_OBJECT) { firDiagnostic ->
        TypeParametersInObjectImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT) { firDiagnostic ->
        TypeParametersInAnonymousObjectImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_PROJECTION_USAGE) { firDiagnostic ->
        IllegalProjectionUsageImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_IN_ENUM) { firDiagnostic ->
        TypeParametersInEnumImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_PROJECTION) { firDiagnostic ->
        ConflictingProjectionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION) { firDiagnostic ->
        ConflictingProjectionInTypealiasExpansionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_PROJECTION) { firDiagnostic ->
        RedundantProjectionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED) { firDiagnostic ->
        VarianceOnTypeParameterNotAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE) { firDiagnostic ->
        CatchParameterWithDefaultValueImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE) { firDiagnostic ->
        TypeParameterInCatchClauseImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.GENERIC_THROWABLE_SUBCLASS) { firDiagnostic ->
        GenericThrowableSubclassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS) { firDiagnostic ->
        InnerClassOfGenericThrowableSubclassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE) { firDiagnostic ->
        KclassWithNullableTypeParameterInSignatureImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_AS_REIFIED) { firDiagnostic ->
        TypeParameterAsReifiedImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR) { firDiagnostic ->
        TypeParameterAsReifiedArrayErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION) { firDiagnostic ->
        ReifiedTypeForbiddenSubstitutionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEFINITELY_NON_NULLABLE_AS_REIFIED) { firDiagnostic ->
        DefinitelyNonNullableAsReifiedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_INTERSECTION_AS_REIFIED.errorFactory) { firDiagnostic ->
        TypeIntersectionAsReifiedErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic.b.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_INTERSECTION_AS_REIFIED.warningFactory) { firDiagnostic ->
        TypeIntersectionAsReifiedWarningImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic.b.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FINAL_UPPER_BOUND) { firDiagnostic ->
        FinalUpperBoundImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE) { firDiagnostic ->
        UpperBoundIsExtensionOrContextFunctionTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER) { firDiagnostic ->
        BoundsNotAllowedIfBoundedByTypeParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED) { firDiagnostic ->
        OnlyOneClassBoundAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REPEATED_BOUND) { firDiagnostic ->
        RepeatedBoundImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_UPPER_BOUNDS) { firDiagnostic ->
        ConflictingUpperBoundsImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER) { firDiagnostic ->
        NameInConstraintIsNotATypeParameterImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED) { firDiagnostic ->
        BoundOnTypeAliasParameterNotAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_PARAMETER_NO_INLINE) { firDiagnostic ->
        ReifiedTypeParameterNoInlineImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_PARAMETER_ON_ALIAS.errorFactory) { firDiagnostic ->
        ReifiedTypeParameterOnAliasErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_PARAMETER_ON_ALIAS.warningFactory) { firDiagnostic ->
        ReifiedTypeParameterOnAliasWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_NOT_ALLOWED) { firDiagnostic ->
        TypeParametersNotAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER) { firDiagnostic ->
        TypeParameterOfPropertyNotUsedInReceiverImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH) { firDiagnostic ->
        ReturnTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.buildSymbol(firDiagnostic.c),
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IMPLICIT_NOTHING_RETURN_TYPE) { firDiagnostic ->
        ImplicitNothingReturnTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IMPLICIT_NOTHING_PROPERTY_TYPE) { firDiagnostic ->
        ImplicitNothingPropertyTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABBREVIATED_NOTHING_RETURN_TYPE) { firDiagnostic ->
        AbbreviatedNothingReturnTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABBREVIATED_NOTHING_PROPERTY_TYPE) { firDiagnostic ->
        AbbreviatedNothingPropertyTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CYCLIC_GENERIC_UPPER_BOUND) { firDiagnostic ->
        CyclicGenericUpperBoundImpl(
            firDiagnostic.a.map { firTypeParameterSymbol ->
                firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firTypeParameterSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FINITE_BOUNDS_VIOLATION) { firDiagnostic ->
        FiniteBoundsViolationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FINITE_BOUNDS_VIOLATION_IN_JAVA) { firDiagnostic ->
        FiniteBoundsViolationInJavaImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPANSIVE_INHERITANCE) { firDiagnostic ->
        ExpansiveInheritanceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPANSIVE_INHERITANCE_IN_JAVA) { firDiagnostic ->
        ExpansiveInheritanceInJavaImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_TYPE_PARAMETER_SYNTAX) { firDiagnostic ->
        DeprecatedTypeParameterSyntaxImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS) { firDiagnostic ->
        MisplacedTypeParameterConstraintsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DYNAMIC_SUPERTYPE) { firDiagnostic ->
        DynamicSupertypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DYNAMIC_UPPER_BOUND) { firDiagnostic ->
        DynamicUpperBoundImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DYNAMIC_RECEIVER_NOT_ALLOWED) { firDiagnostic ->
        DynamicReceiverNotAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC) { firDiagnostic ->
        DynamicReceiverExpectedButWasNonDynamicImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_TYPES) { firDiagnostic ->
        IncompatibleTypesImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_TYPES_WARNING) { firDiagnostic ->
        IncompatibleTypesWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_VARIANCE_CONFLICT_ERROR) { firDiagnostic ->
        TypeVarianceConflictErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.d),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE) { firDiagnostic ->
        TypeVarianceConflictInExpandedTypeImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.d),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SMARTCAST_IMPOSSIBLE) { firDiagnostic ->
        SmartcastImpossibleImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b.source!!.psi as KtExpression,
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SMARTCAST_IMPOSSIBLE_ON_IMPLICIT_INVOKE_RECEIVER) { firDiagnostic ->
        SmartcastImpossibleOnImplicitInvokeReceiverImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b.source!!.psi as KtExpression,
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SMARTCAST_ON_DELEGATED_PROPERTY) { firDiagnostic ->
        DeprecatedSmartcastOnDelegatedPropertyImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_NULLABLE) { firDiagnostic ->
        RedundantNullableImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PLATFORM_CLASS_MAPPED_TO_KOTLIN) { firDiagnostic ->
        PlatformClassMappedToKotlinImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION.errorFactory) { firDiagnostic ->
        InferredTypeVariableIntoEmptyIntersectionErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION.warningFactory) { firDiagnostic ->
        InferredTypeVariableIntoEmptyIntersectionWarningImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION) { firDiagnostic ->
        InferredTypeVariableIntoPossibleEmptyIntersectionImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCORRECT_LEFT_COMPONENT_OF_INTERSECTION) { firDiagnostic ->
        IncorrectLeftComponentOfIntersectionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION) { firDiagnostic ->
        IncorrectRightComponentOfIntersectionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_ON_DEFINITELY_NOT_NULLABLE) { firDiagnostic ->
        NullableOnDefinitelyNotNullableImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT.errorFactory) { firDiagnostic ->
        InferredInvisibleReifiedTypeArgumentErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT.warningFactory) { firDiagnostic ->
        InferredInvisibleReifiedTypeArgumentWarningImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT.errorFactory) { firDiagnostic ->
        InferredInvisibleVarargTypeArgumentErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.buildSymbol(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT.warningFactory) { firDiagnostic ->
        InferredInvisibleVarargTypeArgumentWarningImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.buildSymbol(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_INVISIBLE_RETURN_TYPE.errorFactory) { firDiagnostic ->
        InferredInvisibleReturnTypeErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_INVISIBLE_RETURN_TYPE.warningFactory) { firDiagnostic ->
        InferredInvisibleReturnTypeWarningImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.GENERIC_QUALIFIER_ON_CONSTRUCTOR_CALL.errorFactory) { firDiagnostic ->
        GenericQualifierOnConstructorCallErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.GENERIC_QUALIFIER_ON_CONSTRUCTOR_CALL.warningFactory) { firDiagnostic ->
        GenericQualifierOnConstructorCallWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY) { firDiagnostic ->
        AtomicRefWithoutConsistentIdentityImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED) { firDiagnostic ->
        ExtensionInClassReferenceNotAllowedImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS) { firDiagnostic ->
        CallableReferenceLhsNotAClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR) { firDiagnostic ->
        CallableReferenceToAnnotationConstructorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE) { firDiagnostic ->
        AdaptedCallableReferenceAgainstReflectionTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASS_LITERAL_LHS_NOT_A_CLASS) { firDiagnostic ->
        ClassLiteralLhsNotAClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_TYPE_IN_CLASS_LITERAL_LHS) { firDiagnostic ->
        NullableTypeInClassLiteralLhsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS) { firDiagnostic ->
        ExpressionOfNullableTypeInClassLiteralLhsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS) { firDiagnostic ->
        UnsupportedClassLiteralsWithEmptyLhsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUTABLE_PROPERTY_WITH_CAPTURED_TYPE) { firDiagnostic ->
        MutablePropertyWithCapturedTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.UNSUPPORTED_REFLECTION_API) { firDiagnostic ->
        UnsupportedReflectionApiImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOTHING_TO_OVERRIDE) { firDiagnostic ->
        NothingToOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER) { firDiagnostic ->
        CannotOverrideInvisibleMemberImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_OVERRIDE_CONFLICT) { firDiagnostic ->
        DataClassOverrideConflictImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_OVERRIDE_DEFAULT_VALUES) { firDiagnostic ->
        DataClassOverrideDefaultValuesImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE) { firDiagnostic ->
        CannotWeakenAccessPrivilegeImpl(
            firDiagnostic.a,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING) { firDiagnostic ->
        CannotWeakenAccessPrivilegeWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE) { firDiagnostic ->
        CannotChangeAccessPrivilegeImpl(
            firDiagnostic.a,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE_WARNING) { firDiagnostic ->
        CannotChangeAccessPrivilegeWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_VISIBILITY) { firDiagnostic ->
        CannotInferVisibilityImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_VISIBILITY_WARNING) { firDiagnostic ->
        CannotInferVisibilityWarningImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES) { firDiagnostic ->
        MultipleDefaultsInheritedFromSupertypesImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE) { firDiagnostic ->
        MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION.errorFactory) { firDiagnostic ->
        MultipleDefaultsInheritedFromSupertypesDeprecationErrorImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION.warningFactory) { firDiagnostic ->
        MultipleDefaultsInheritedFromSupertypesDeprecationWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE_DEPRECATION.errorFactory) { firDiagnostic ->
        MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationErrorImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE_DEPRECATION.warningFactory) { firDiagnostic ->
        MultipleDefaultsInheritedFromSupertypesWhenNoExplicitOverrideDeprecationWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS) { firDiagnostic ->
        TypealiasExpandsToArrayOfNothingsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERRIDING_FINAL_MEMBER) { firDiagnostic ->
        OverridingFinalMemberImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH_ON_INHERITANCE) { firDiagnostic ->
        ReturnTypeMismatchOnInheritanceImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_TYPE_MISMATCH_ON_INHERITANCE) { firDiagnostic ->
        PropertyTypeMismatchOnInheritanceImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_TYPE_MISMATCH_ON_INHERITANCE) { firDiagnostic ->
        VarTypeMismatchOnInheritanceImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH_BY_DELEGATION) { firDiagnostic ->
        ReturnTypeMismatchByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_TYPE_MISMATCH_BY_DELEGATION) { firDiagnostic ->
        PropertyTypeMismatchByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION) { firDiagnostic ->
        VarOverriddenByValByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_INHERITED_MEMBERS) { firDiagnostic ->
        ConflictingInheritedMembersImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        AbstractMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_MEMBER_INCORRECTLY_DELEGATED.errorFactory) { firDiagnostic ->
        AbstractMemberIncorrectlyDelegatedErrorImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_MEMBER_INCORRECTLY_DELEGATED.warningFactory) { firDiagnostic ->
        AbstractMemberIncorrectlyDelegatedWarningImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY) { firDiagnostic ->
        AbstractMemberNotImplementedByEnumEntryImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        AbstractClassMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR) { firDiagnostic ->
        InvisibleAbstractMemberFromSuperErrorImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_ANONYMOUS_TYPE_INFERRED) { firDiagnostic ->
        AmbiguousAnonymousTypeInferredImpl(
            firDiagnostic.a.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        ManyImplMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        ManyInterfacesMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERRIDING_FINAL_MEMBER_BY_DELEGATION) { firDiagnostic ->
        OverridingFinalMemberByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE) { firDiagnostic ->
        DelegatedMemberHidesSupertypeOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        ReturnTypeMismatchOnOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        PropertyTypeMismatchOnOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        VarTypeMismatchOnOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_OVERRIDDEN_BY_VAL) { firDiagnostic ->
        VarOverriddenByValImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_IMPLEMENTED_BY_INHERITED_VAL.errorFactory) { firDiagnostic ->
        VarImplementedByInheritedValErrorImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_IMPLEMENTED_BY_INHERITED_VAL.warningFactory) { firDiagnostic ->
        VarImplementedByInheritedValWarningImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_FINAL_MEMBER_IN_FINAL_CLASS) { firDiagnostic ->
        NonFinalMemberInFinalClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_FINAL_MEMBER_IN_OBJECT) { firDiagnostic ->
        NonFinalMemberInObjectImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VIRTUAL_MEMBER_HIDDEN) { firDiagnostic ->
        VirtualMemberHiddenImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PARAMETER_NAME_CHANGED_ON_OVERRIDE) { firDiagnostic ->
        ParameterNameChangedOnOverrideImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES) { firDiagnostic ->
        DifferentNamesForTheSameParameterInSupertypesImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic.d.map { firNamedFunctionSymbol ->
                firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firNamedFunctionSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUSPEND_OVERRIDDEN_BY_NON_SUSPEND) { firDiagnostic ->
        SuspendOverriddenByNonSuspendImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_SUSPEND_OVERRIDDEN_BY_SUSPEND) { firDiagnostic ->
        NonSuspendOverriddenBySuspendImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_COMPANION_OBJECTS) { firDiagnostic ->
        ManyCompanionObjectsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_OVERLOADS) { firDiagnostic ->
        ConflictingOverloadsImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDECLARATION) { firDiagnostic ->
        RedeclarationImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASSIFIER_REDECLARATION) { firDiagnostic ->
        ClassifierRedeclarationImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PACKAGE_CONFLICTS_WITH_CLASSIFIER) { firDiagnostic ->
        PackageConflictsWithClassifierImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE) { firDiagnostic ->
        ExpectAndActualInTheSameModuleImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE) { firDiagnostic ->
        MethodOfAnyImplementedInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXTENSION_SHADOWED_BY_MEMBER) { firDiagnostic ->
        ExtensionShadowedByMemberImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE) { firDiagnostic ->
        ExtensionFunctionShadowedByMemberPropertyWithInvokeImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_OBJECT_NOT_ALLOWED) { firDiagnostic ->
        LocalObjectNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_INTERFACE_NOT_ALLOWED) { firDiagnostic ->
        LocalInterfaceNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS) { firDiagnostic ->
        AbstractFunctionInNonAbstractClassImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_FUNCTION_WITH_BODY) { firDiagnostic ->
        AbstractFunctionWithBodyImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY) { firDiagnostic ->
        NonAbstractFunctionWithNoBodyImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_FUNCTION_WITH_NO_BODY) { firDiagnostic ->
        PrivateFunctionWithNoBodyImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_MEMBER_FUNCTION_NO_BODY) { firDiagnostic ->
        NonMemberFunctionNoBodyImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUNCTION_DECLARATION_WITH_NO_NAME) { firDiagnostic ->
        FunctionDeclarationWithNoNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_FUNCTION_WITH_NAME) { firDiagnostic ->
        AnonymousFunctionWithNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SINGLE_ANONYMOUS_FUNCTION_WITH_NAME.errorFactory) { firDiagnostic ->
        SingleAnonymousFunctionWithNameErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SINGLE_ANONYMOUS_FUNCTION_WITH_NAME.warningFactory) { firDiagnostic ->
        SingleAnonymousFunctionWithNameWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE) { firDiagnostic ->
        AnonymousFunctionParameterWithDefaultValueImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_VARARG_ON_PARAMETER) { firDiagnostic ->
        UselessVarargOnParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_VARARG_PARAMETERS) { firDiagnostic ->
        MultipleVarargParametersImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FORBIDDEN_VARARG_PARAMETER_TYPE) { firDiagnostic ->
        ForbiddenVarargParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE) { firDiagnostic ->
        ValueParameterWithoutExplicitTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_PARAMETER_TYPE) { firDiagnostic ->
        CannotInferParameterTypeImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_VALUE_PARAMETER_TYPE) { firDiagnostic ->
        CannotInferValueParameterTypeImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_IT_PARAMETER_TYPE) { firDiagnostic ->
        CannotInferItParameterTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_RECEIVER_PARAMETER_TYPE) { firDiagnostic ->
        CannotInferReceiverParameterTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_TAIL_CALLS_FOUND) { firDiagnostic ->
        NoTailCallsFoundImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TAILREC_ON_VIRTUAL_MEMBER_ERROR) { firDiagnostic ->
        TailrecOnVirtualMemberErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_TAIL_RECURSIVE_CALL) { firDiagnostic ->
        NonTailRecursiveCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED) { firDiagnostic ->
        TailRecursionInTryIsNotSupportedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE) { firDiagnostic ->
        DataObjectCustomEqualsOrHashCodeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE) { firDiagnostic ->
        DefaultValueNotAllowedInOverrideImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS) { firDiagnostic ->
        FunInterfaceWrongCountOfAbstractMembersImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES) { firDiagnostic ->
        FunInterfaceCannotHaveAbstractPropertiesImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS) { firDiagnostic ->
        FunInterfaceAbstractMethodWithTypeParametersImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE) { firDiagnostic ->
        FunInterfaceAbstractMethodWithDefaultValueImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_WITH_SUSPEND_FUNCTION) { firDiagnostic ->
        FunInterfaceWithSuspendFunctionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS) { firDiagnostic ->
        AbstractPropertyInNonAbstractClassImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_PROPERTY_IN_INTERFACE) { firDiagnostic ->
        PrivatePropertyInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER) { firDiagnostic ->
        AbstractPropertyWithInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_INITIALIZER_IN_INTERFACE) { firDiagnostic ->
        PropertyInitializerInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER) { firDiagnostic ->
        PropertyWithNoTypeNoInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITHOUT_TYPE) { firDiagnostic ->
        AbstractPropertyWithoutTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LATEINIT_PROPERTY_WITHOUT_TYPE) { firDiagnostic ->
        LateinitPropertyWithoutTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED) { firDiagnostic ->
        MustBeInitializedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_WARNING) { firDiagnostic ->
        MustBeInitializedWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_OR_BE_FINAL) { firDiagnostic ->
        MustBeInitializedOrBeFinalImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_OR_BE_FINAL_WARNING) { firDiagnostic ->
        MustBeInitializedOrBeFinalWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT) { firDiagnostic ->
        MustBeInitializedOrBeAbstractImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT_WARNING) { firDiagnostic ->
        MustBeInitializedOrBeAbstractWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT) { firDiagnostic ->
        MustBeInitializedOrFinalOrAbstractImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING) { firDiagnostic ->
        MustBeInitializedOrFinalOrAbstractWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT) { firDiagnostic ->
        ExtensionPropertyMustHaveAccessorsOrBeAbstractImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_LATEINIT) { firDiagnostic ->
        UnnecessaryLateinitImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BACKING_FIELD_IN_INTERFACE) { firDiagnostic ->
        BackingFieldInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXTENSION_PROPERTY_WITH_BACKING_FIELD) { firDiagnostic ->
        ExtensionPropertyWithBackingFieldImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_INITIALIZER_NO_BACKING_FIELD) { firDiagnostic ->
        PropertyInitializerNoBackingFieldImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_DELEGATED_PROPERTY) { firDiagnostic ->
        AbstractDelegatedPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATED_PROPERTY_IN_INTERFACE) { firDiagnostic ->
        DelegatedPropertyInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_GETTER) { firDiagnostic ->
        AbstractPropertyWithGetterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_SETTER) { firDiagnostic ->
        AbstractPropertyWithSetterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY) { firDiagnostic ->
        PrivateSetterForAbstractPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_SETTER_FOR_OPEN_PROPERTY) { firDiagnostic ->
        PrivateSetterForOpenPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_WITH_SETTER) { firDiagnostic ->
        ValWithSetterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT) { firDiagnostic ->
        ConstValNotTopLevelOrObjectImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_GETTER) { firDiagnostic ->
        ConstValWithGetterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_DELEGATE) { firDiagnostic ->
        ConstValWithDelegateImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_CANT_BE_USED_FOR_CONST_VAL) { firDiagnostic ->
        TypeCantBeUsedForConstValImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITHOUT_INITIALIZER) { firDiagnostic ->
        ConstValWithoutInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_NON_CONST_INITIALIZER) { firDiagnostic ->
        ConstValWithNonConstInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_SETTER_PARAMETER_TYPE) { firDiagnostic ->
        WrongSetterParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER.errorFactory) { firDiagnostic ->
        DelegateUsesExtensionPropertyTypeParameterErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER.warningFactory) { firDiagnostic ->
        DelegateUsesExtensionPropertyTypeParameterWarningImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INITIALIZER_TYPE_MISMATCH) { firDiagnostic ->
        InitializerTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY) { firDiagnostic ->
        GetterVisibilityDiffersFromPropertyVisibilityImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY) { firDiagnostic ->
        SetterVisibilityInconsistentWithPropertyVisibilityImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_SETTER_RETURN_TYPE) { firDiagnostic ->
        WrongSetterReturnTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_GETTER_RETURN_TYPE) { firDiagnostic ->
        WrongGetterReturnTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACCESSOR_FOR_DELEGATED_PROPERTY) { firDiagnostic ->
        AccessorForDelegatedPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION) { firDiagnostic ->
        PropertyInitializerWithExplicitFieldDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_FIELD_DECLARATION_MISSING_INITIALIZER) { firDiagnostic ->
        PropertyFieldDeclarationMissingInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER) { firDiagnostic ->
        LateinitPropertyFieldDeclarationWithInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LATEINIT_FIELD_IN_VAL_PROPERTY) { firDiagnostic ->
        LateinitFieldInValPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LATEINIT_NULLABLE_BACKING_FIELD) { firDiagnostic ->
        LateinitNullableBackingFieldImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BACKING_FIELD_FOR_DELEGATED_PROPERTY) { firDiagnostic ->
        BackingFieldForDelegatedPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_MUST_HAVE_GETTER) { firDiagnostic ->
        PropertyMustHaveGetterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_MUST_HAVE_SETTER) { firDiagnostic ->
        PropertyMustHaveSetterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPLICIT_BACKING_FIELD_IN_INTERFACE) { firDiagnostic ->
        ExplicitBackingFieldInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPLICIT_BACKING_FIELD_IN_ABSTRACT_PROPERTY) { firDiagnostic ->
        ExplicitBackingFieldInAbstractPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPLICIT_BACKING_FIELD_IN_EXTENSION) { firDiagnostic ->
        ExplicitBackingFieldInExtensionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_EXPLICIT_BACKING_FIELD) { firDiagnostic ->
        RedundantExplicitBackingFieldImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS) { firDiagnostic ->
        AbstractPropertyInPrimaryConstructorParametersImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING) { firDiagnostic ->
        LocalVariableWithTypeParametersWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_VARIABLE_WITH_TYPE_PARAMETERS) { firDiagnostic ->
        LocalVariableWithTypeParametersImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS) { firDiagnostic ->
        ExplicitTypeArgumentsInPropertyAccessImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SAFE_CALLABLE_REFERENCE_CALL) { firDiagnostic ->
        SafeCallableReferenceCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL) { firDiagnostic ->
        LateinitIntrinsicCallOnNonLiteralImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT) { firDiagnostic ->
        LateinitIntrinsicCallOnNonLateinitImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION) { firDiagnostic ->
        LateinitIntrinsicCallInInlineFunctionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY) { firDiagnostic ->
        LateinitIntrinsicCallOnNonAccessiblePropertyImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_EXTENSION_PROPERTY) { firDiagnostic ->
        LocalExtensionPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNNAMED_VAR_PROPERTY) { firDiagnostic ->
        UnnamedVarPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNNAMED_DELEGATED_PROPERTY) { firDiagnostic ->
        UnnamedDelegatedPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_DECLARATION_WITH_BODY) { firDiagnostic ->
        ExpectedDeclarationWithBodyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL) { firDiagnostic ->
        ExpectedClassConstructorDelegationCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER) { firDiagnostic ->
        ExpectedClassConstructorPropertyParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_ENUM_CONSTRUCTOR) { firDiagnostic ->
        ExpectedEnumConstructorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_ENUM_ENTRY_WITH_BODY) { firDiagnostic ->
        ExpectedEnumEntryWithBodyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_PROPERTY_INITIALIZER) { firDiagnostic ->
        ExpectedPropertyInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_DELEGATED_PROPERTY) { firDiagnostic ->
        ExpectedDelegatedPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_LATEINIT_PROPERTY) { firDiagnostic ->
        ExpectedLateinitPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS) { firDiagnostic ->
        SupertypeInitializedInExpectedClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_PRIVATE_DECLARATION) { firDiagnostic ->
        ExpectedPrivateDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_EXTERNAL_DECLARATION) { firDiagnostic ->
        ExpectedExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_TAILREC_FUNCTION) { firDiagnostic ->
        ExpectedTailrecFunctionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS) { firDiagnostic ->
        ImplementationByDelegationInExpectClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_NOT_TO_CLASS) { firDiagnostic ->
        ActualTypeAliasNotToClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE) { firDiagnostic ->
        ActualTypeAliasToClassWithDeclarationSiteVarianceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE) { firDiagnostic ->
        ActualTypeAliasWithUseSiteVarianceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION) { firDiagnostic ->
        ActualTypeAliasWithComplexSubstitutionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_TO_NULLABLE_TYPE) { firDiagnostic ->
        ActualTypeAliasToNullableTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_TO_NOTHING) { firDiagnostic ->
        ActualTypeAliasToNothingImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS) { firDiagnostic ->
        ActualFunctionWithDefaultArgumentsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS) { firDiagnostic ->
        DefaultArgumentsInExpectWithActualTypealiasImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE) { firDiagnostic ->
        DefaultArgumentsInExpectActualizedByFakeOverrideImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firNamedFunctionSymbol ->
                firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firNamedFunctionSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND) { firDiagnostic ->
        ExpectedFunctionSourceWithDefaultArgumentsNotFoundImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_WITHOUT_EXPECT) { firDiagnostic ->
        ActualWithoutExpectImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b.mapKeys { (expectActualMatchingCompatibility, _) ->
                expectActualMatchingCompatibility
            }.mapValues { (_, collection) -> 
                collection.map { firBasedSymbol ->
                                    firSymbolBuilder.buildSymbol(firBasedSymbol)
                                }
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_COUNT) { firDiagnostic ->
        ExpectActualIncompatibleClassTypeParameterCountImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE) { firDiagnostic ->
        ExpectActualIncompatibleReturnTypeImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PARAMETER_NAMES) { firDiagnostic ->
        ExpectActualIncompatibleParameterNamesImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CONTEXT_PARAMETER_NAMES) { firDiagnostic ->
        ExpectActualIncompatibleContextParameterNamesImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_NAMES) { firDiagnostic ->
        ExpectActualIncompatibleTypeParameterNamesImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_VARARG) { firDiagnostic ->
        ExpectActualIncompatibleValueParameterVarargImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_NOINLINE) { firDiagnostic ->
        ExpectActualIncompatibleValueParameterNoinlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_CROSSINLINE) { firDiagnostic ->
        ExpectActualIncompatibleValueParameterCrossinlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_DIFFERENT) { firDiagnostic ->
        ExpectActualIncompatibleFunctionModifiersDifferentImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_NOT_SUBSET) { firDiagnostic ->
        ExpectActualIncompatibleFunctionModifiersNotSubsetImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PARAMETERS_WITH_DEFAULT_VALUES_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE) { firDiagnostic ->
        ExpectActualIncompatibleParametersWithDefaultValuesInExpectActualizedByFakeOverrideImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_KIND) { firDiagnostic ->
        ExpectActualIncompatiblePropertyKindImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_LATEINIT_MODIFIER) { firDiagnostic ->
        ExpectActualIncompatiblePropertyLateinitModifierImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_CONST_MODIFIER) { firDiagnostic ->
        ExpectActualIncompatiblePropertyConstModifierImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY) { firDiagnostic ->
        ExpectActualIncompatiblePropertySetterVisibilityImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND) { firDiagnostic ->
        ExpectActualIncompatibleClassKindImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS) { firDiagnostic ->
        ExpectActualIncompatibleClassModifiersImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER) { firDiagnostic ->
        ExpectActualIncompatibleFunInterfaceModifierImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES) { firDiagnostic ->
        ExpectActualIncompatibleSupertypesImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_NESTED_TYPE_ALIAS) { firDiagnostic ->
        ExpectActualIncompatibleNestedTypeAliasImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_ENUM_ENTRIES) { firDiagnostic ->
        ExpectActualIncompatibleEnumEntriesImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_ILLEGAL_REQUIRES_OPT_IN) { firDiagnostic ->
        ExpectActualIncompatibleIllegalRequiresOptInImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_MODALITY) { firDiagnostic ->
        ExpectActualIncompatibleModalityImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY) { firDiagnostic ->
        ExpectActualIncompatibleVisibilityImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_UPPER_BOUNDS) { firDiagnostic ->
        ExpectActualIncompatibleClassTypeParameterUpperBoundsImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_VARIANCE) { firDiagnostic ->
        ExpectActualIncompatibleTypeParameterVarianceImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_REIFIED) { firDiagnostic ->
        ExpectActualIncompatibleTypeParameterReifiedImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE) { firDiagnostic ->
        ExpectActualIncompatibleClassScopeImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firSymbolBuilder.buildSymbol(firDiagnostic.c),
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET) { firDiagnostic ->
        ExpectRefinementAnnotationWrongTargetImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_EXPECTS) { firDiagnostic ->
        AmbiguousExpectsImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firModuleData ->
                firModuleData
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS) { firDiagnostic ->
        NoActualClassMemberForExpectedClassImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b.map { pair ->
                firSymbolBuilder.buildSymbol(pair.first) to pair.second.mapKeys { (mismatch, _) ->
                                    mismatch
                                }.mapValues { (_, collection) -> 
                                    collection.map { firBasedSymbol ->
                                                            firSymbolBuilder.buildSymbol(firBasedSymbol)
                                                        }
                                }
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_MISSING) { firDiagnostic ->
        ActualMissingImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_REFINEMENT_ANNOTATION_MISSING) { firDiagnostic ->
        ExpectRefinementAnnotationMissingImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING) { firDiagnostic ->
        ExpectActualClassifiersAreInBetaWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_A_MULTIPLATFORM_COMPILATION) { firDiagnostic ->
        NotAMultiplatformCompilationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECT_ACTUAL_OPT_IN_ANNOTATION) { firDiagnostic ->
        ExpectActualOptInAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION) { firDiagnostic ->
        ActualTypealiasToSpecialAnnotationImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT) { firDiagnostic ->
        ActualAnnotationsNotMatchExpectImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            (firDiagnostic.c as? KtPsiSourceElement)?.psi,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY) { firDiagnostic ->
        OptionalDeclarationOutsideOfAnnotationEntryImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE) { firDiagnostic ->
        OptionalDeclarationUsageInNonCommonSourceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPTIONAL_EXPECTATION_NOT_ON_EXPECTED) { firDiagnostic ->
        OptionalExpectationNotOnExpectedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION) { firDiagnostic ->
        InitializerRequiredForDestructuringDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_MISSING) { firDiagnostic ->
        ComponentFunctionMissingImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_AMBIGUITY) { firDiagnostic ->
        ComponentFunctionAmbiguityImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_ON_NULLABLE) { firDiagnostic ->
        ComponentFunctionOnNullableImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH) { firDiagnostic ->
        ComponentFunctionReturnTypeMismatchImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_VARIABLE) { firDiagnostic ->
        UninitializedVariableImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_PARAMETER) { firDiagnostic ->
        UninitializedParameterImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_ENUM_ENTRY) { firDiagnostic ->
        UninitializedEnumEntryImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_ENUM_COMPANION) { firDiagnostic ->
        UninitializedEnumCompanionImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT) { firDiagnostic ->
        ValReassignmentImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR) { firDiagnostic ->
        ValReassignmentViaBackingFieldErrorImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a.fir.propertySymbol),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAPTURED_VAL_INITIALIZATION) { firDiagnostic ->
        CapturedValInitializationImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION) { firDiagnostic ->
        CapturedMemberValInitializationImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_INLINE_MEMBER_VAL_INITIALIZATION) { firDiagnostic ->
        NonInlineMemberValInitializationImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SETTER_PROJECTED_OUT) { firDiagnostic ->
        SetterProjectedOutImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b,
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_INVOCATION_KIND) { firDiagnostic ->
        WrongInvocationKindImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LEAKED_IN_PLACE_LAMBDA) { firDiagnostic ->
        LeakedInPlaceLambdaImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIABLE_WITH_NO_TYPE_NO_INITIALIZER) { firDiagnostic ->
        VariableWithNoTypeNoInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INITIALIZATION_BEFORE_DECLARATION) { firDiagnostic ->
        InitializationBeforeDeclarationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INITIALIZATION_BEFORE_DECLARATION_WARNING) { firDiagnostic ->
        InitializationBeforeDeclarationWarningImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNREACHABLE_CODE) { firDiagnostic ->
        UnreachableCodeImpl(
            firDiagnostic.a.map { ktSourceElement ->
                (ktSourceElement as KtPsiSourceElement).psi
            },
            firDiagnostic.b.map { ktSourceElement ->
                (ktSourceElement as KtPsiSourceElement).psi
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SENSELESS_COMPARISON) { firDiagnostic ->
        SenselessComparisonImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SENSELESS_NULL_IN_WHEN) { firDiagnostic ->
        SenselessNullInWhenImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM) { firDiagnostic ->
        TypecheckerHasRunIntoRecursiveProblemImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_VALUE_NOT_USED) { firDiagnostic ->
        ReturnValueNotUsedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_CALL) { firDiagnostic ->
        UnsafeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b?.source?.psi as? KtExpression,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL) { firDiagnostic ->
        UnsafeImplicitInvokeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_INFIX_CALL) { firDiagnostic ->
        UnsafeInfixCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b.source!!.psi as KtExpression,
            firDiagnostic.c,
            firDiagnostic.d?.source?.psi as? KtExpression,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_OPERATOR_CALL) { firDiagnostic ->
        UnsafeOperatorCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b.source!!.psi as KtExpression,
            firDiagnostic.c,
            firDiagnostic.d?.source?.psi as? KtExpression,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ITERATOR_ON_NULLABLE) { firDiagnostic ->
        IteratorOnNullableImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_SAFE_CALL) { firDiagnostic ->
        UnnecessarySafeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNEXPECTED_SAFE_CALL) { firDiagnostic ->
        UnexpectedSafeCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_NOT_NULL_ASSERTION) { firDiagnostic ->
        UnnecessaryNotNullAssertionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION) { firDiagnostic ->
        NotNullAssertionOnLambdaExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE) { firDiagnostic ->
        NotNullAssertionOnCallableReferenceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_ELVIS) { firDiagnostic ->
        UselessElvisImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_ELVIS_RIGHT_IS_NULL) { firDiagnostic ->
        UselessElvisRightIsNullImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_CHECK_FOR_ERASED) { firDiagnostic ->
        CannotCheckForErasedImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAST_NEVER_SUCCEEDS) { firDiagnostic ->
        CastNeverSucceedsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_CAST) { firDiagnostic ->
        UselessCastImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNCHECKED_CAST) { firDiagnostic ->
        UncheckedCastImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_IS_CHECK) { firDiagnostic ->
        UselessIsCheckImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IS_ENUM_ENTRY) { firDiagnostic ->
        IsEnumEntryImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DYNAMIC_NOT_ALLOWED) { firDiagnostic ->
        DynamicNotAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ENUM_ENTRY_AS_TYPE) { firDiagnostic ->
        EnumEntryAsTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_CONDITION) { firDiagnostic ->
        ExpectedConditionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_ELSE_IN_WHEN) { firDiagnostic ->
        NoElseInWhenImpl(
            firDiagnostic.a.map { whenMissingCase ->
                whenMissingCase
            },
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVALID_IF_AS_EXPRESSION) { firDiagnostic ->
        InvalidIfAsExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ELSE_MISPLACED_IN_WHEN) { firDiagnostic ->
        ElseMisplacedInWhenImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_ELSE_IN_WHEN) { firDiagnostic ->
        RedundantElseInWhenImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_DECLARATION_IN_WHEN_SUBJECT) { firDiagnostic ->
        IllegalDeclarationInWhenSubjectImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT) { firDiagnostic ->
        CommaInWhenConditionWithoutArgumentImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DUPLICATE_BRANCH_CONDITION_IN_WHEN) { firDiagnostic ->
        DuplicateBranchConditionInWhenImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFUSING_BRANCH_CONDITION.errorFactory) { firDiagnostic ->
        ConfusingBranchConditionErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFUSING_BRANCH_CONDITION.warningFactory) { firDiagnostic ->
        ConfusingBranchConditionWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_CONDITION_SUGGEST_GUARD) { firDiagnostic ->
        WrongConditionSuggestGuardImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD) { firDiagnostic ->
        CommaInWhenConditionWithWhenGuardImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WHEN_GUARD_WITHOUT_SUBJECT) { firDiagnostic ->
        WhenGuardWithoutSubjectImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_INVISIBLE_WHEN_TYPE.errorFactory) { firDiagnostic ->
        InferredInvisibleWhenTypeErrorImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERRED_INVISIBLE_WHEN_TYPE.warningFactory) { firDiagnostic ->
        InferredInvisibleWhenTypeWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION) { firDiagnostic ->
        TypeParameterIsNotAnExpressionImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_ON_LHS_OF_DOT) { firDiagnostic ->
        TypeParameterOnLhsOfDotImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_COMPANION_OBJECT) { firDiagnostic ->
        NoCompanionObjectImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPRESSION_EXPECTED_PACKAGE_FOUND) { firDiagnostic ->
        ExpressionExpectedPackageFoundImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ERROR_IN_CONTRACT_DESCRIPTION) { firDiagnostic ->
        ErrorInContractDescriptionImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONTRACT_NOT_ALLOWED) { firDiagnostic ->
        ContractNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_GET_METHOD) { firDiagnostic ->
        NoGetMethodImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_SET_METHOD) { firDiagnostic ->
        NoSetMethodImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ITERATOR_MISSING) { firDiagnostic ->
        IteratorMissingImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_MISSING) { firDiagnostic ->
        HasNextMissingImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEXT_MISSING) { firDiagnostic ->
        NextMissingImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_FUNCTION_NONE_APPLICABLE) { firDiagnostic ->
        HasNextFunctionNoneApplicableImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEXT_NONE_APPLICABLE) { firDiagnostic ->
        NextNoneApplicableImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_MISSING) { firDiagnostic ->
        DelegateSpecialFunctionMissingImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_AMBIGUITY) { firDiagnostic ->
        DelegateSpecialFunctionAmbiguityImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE) { firDiagnostic ->
        DelegateSpecialFunctionNoneApplicableImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH) { firDiagnostic ->
        DelegateSpecialFunctionReturnTypeMismatchImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNDERSCORE_IS_RESERVED) { firDiagnostic ->
        UnderscoreIsReservedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS) { firDiagnostic ->
        UnderscoreUsageWithoutBackticksImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER) { firDiagnostic ->
        ResolvedToUnderscoreNamedCatchParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVALID_CHARACTERS) { firDiagnostic ->
        InvalidCharactersImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EQUALITY_NOT_APPLICABLE) { firDiagnostic ->
        EqualityNotApplicableImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EQUALITY_NOT_APPLICABLE_WARNING) { firDiagnostic ->
        EqualityNotApplicableWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_ENUM_COMPARISON_ERROR) { firDiagnostic ->
        IncompatibleEnumComparisonErrorImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_ENUM_COMPARISON) { firDiagnostic ->
        IncompatibleEnumComparisonImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FORBIDDEN_IDENTITY_EQUALS) { firDiagnostic ->
        ForbiddenIdentityEqualsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FORBIDDEN_IDENTITY_EQUALS_WARNING) { firDiagnostic ->
        ForbiddenIdentityEqualsWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_IDENTITY_EQUALS) { firDiagnostic ->
        DeprecatedIdentityEqualsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IMPLICIT_BOXING_IN_IDENTITY_EQUALS) { firDiagnostic ->
        ImplicitBoxingInIdentityEqualsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INC_DEC_SHOULD_NOT_RETURN_UNIT) { firDiagnostic ->
        IncDecShouldNotReturnUnitImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT) { firDiagnostic ->
        AssignmentOperatorShouldReturnUnitImpl(
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_FUNCTION_AS_OPERATOR) { firDiagnostic ->
        NotFunctionAsOperatorImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DSL_SCOPE_VIOLATION) { firDiagnostic ->
        DslScopeViolationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TOPLEVEL_TYPEALIASES_ONLY) { firDiagnostic ->
        ToplevelTypealiasesOnlyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RECURSIVE_TYPEALIAS_EXPANSION) { firDiagnostic ->
        RecursiveTypealiasExpansionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPEALIAS_SHOULD_EXPAND_TO_CLASS) { firDiagnostic ->
        TypealiasShouldExpandToClassImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION.errorFactory) { firDiagnostic ->
        ConstructorOrSupertypeOnTypealiasWithTypeProjectionErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION.warningFactory) { firDiagnostic ->
        ConstructorOrSupertypeOnTypealiasWithTypeProjectionWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS) { firDiagnostic ->
        TypealiasExpansionCapturesOuterTypeParametersImpl(
            firDiagnostic.a.map { firTypeParameterSymbol ->
                firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firTypeParameterSymbol)
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_VISIBILITY_MODIFIER) { firDiagnostic ->
        RedundantVisibilityModifierImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_MODALITY_MODIFIER) { firDiagnostic ->
        RedundantModalityModifierImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_RETURN_UNIT_TYPE) { firDiagnostic ->
        RedundantReturnUnitTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE) { firDiagnostic ->
        RedundantSingleExpressionStringTemplateImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAN_BE_VAL) { firDiagnostic ->
        CanBeValImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAN_BE_VAL_LATEINIT) { firDiagnostic ->
        CanBeValLateinitImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAN_BE_VAL_DELAYED_INITIALIZATION) { firDiagnostic ->
        CanBeValDelayedInitializationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_CALL_OF_CONVERSION_METHOD) { firDiagnostic ->
        RedundantCallOfConversionMethodImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS) { firDiagnostic ->
        ArrayEqualityOperatorCanBeReplacedWithContentEqualsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EMPTY_RANGE) { firDiagnostic ->
        EmptyRangeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_SETTER_PARAMETER_TYPE) { firDiagnostic ->
        RedundantSetterParameterTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNUSED_VARIABLE) { firDiagnostic ->
        UnusedVariableImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNED_VALUE_IS_NEVER_READ) { firDiagnostic ->
        AssignedValueIsNeverReadImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT) { firDiagnostic ->
        VariableInitializerIsRedundantImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIABLE_NEVER_READ) { firDiagnostic ->
        VariableNeverReadImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_CALL_ON_NOT_NULL) { firDiagnostic ->
        UselessCallOnNotNullImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNUSED_ANONYMOUS_PARAMETER) { firDiagnostic ->
        UnusedAnonymousParameterImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNUSED_EXPRESSION) { firDiagnostic ->
        UnusedExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNUSED_LAMBDA_EXPRESSION) { firDiagnostic ->
        UnusedLambdaExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_NOT_ALLOWED) { firDiagnostic ->
        ReturnNotAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_A_FUNCTION_LABEL) { firDiagnostic ->
        NotAFunctionLabelImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY) { firDiagnostic ->
        ReturnInFunctionWithExpressionBodyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING) { firDiagnostic ->
        ReturnInFunctionWithExpressionBodyWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE) { firDiagnostic ->
        ReturnInFunctionWithExpressionBodyAndImplicitTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY) { firDiagnostic ->
        NoReturnInFunctionWithBlockBodyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_RETURN) { firDiagnostic ->
        RedundantReturnImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_INITIALIZER_IN_INTERFACE) { firDiagnostic ->
        AnonymousInitializerInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USAGE_IS_NOT_INLINABLE) { firDiagnostic ->
        UsageIsNotInlinableImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED) { firDiagnostic ->
        NonLocalReturnNotAllowedImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_YET_SUPPORTED_IN_INLINE) { firDiagnostic ->
        NotYetSupportedInInlineImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOTHING_TO_INLINE) { firDiagnostic ->
        NothingToInlineImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_INLINE_PARAMETER) { firDiagnostic ->
        NullableInlineParameterImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RECURSION_IN_INLINE) { firDiagnostic ->
        RecursionInInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        NonPublicCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        NonPublicInlineCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE_DEPRECATION) { firDiagnostic ->
        NonPublicCallFromPublicInlineDeprecationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE.errorFactory) { firDiagnostic ->
        NonPublicDataCopyCallFromPublicInlineErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE.warningFactory) { firDiagnostic ->
        NonPublicDataCopyCallFromPublicInlineWarningImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        ProtectedConstructorCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR) { firDiagnostic ->
        ProtectedCallFromPublicInlineErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_CLASS_MEMBER_FROM_INLINE) { firDiagnostic ->
        PrivateClassMemberFromInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPER_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        SuperCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DECLARATION_CANT_BE_INLINED) { firDiagnostic ->
        DeclarationCantBeInlinedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DECLARATION_CANT_BE_INLINED_DEPRECATION.errorFactory) { firDiagnostic ->
        DeclarationCantBeInlinedDeprecationErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DECLARATION_CANT_BE_INLINED_DEPRECATION.warningFactory) { firDiagnostic ->
        DeclarationCantBeInlinedDeprecationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERRIDE_BY_INLINE) { firDiagnostic ->
        OverrideByInlineImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_INTERNAL_PUBLISHED_API) { firDiagnostic ->
        NonInternalPublishedApiImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE) { firDiagnostic ->
        InvalidDefaultFunctionalParameterForInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE) { firDiagnostic ->
        NotSupportedInlineParameterInInlineParameterDefaultValueImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_PARAMETER_IN_OVERRIDE) { firDiagnostic ->
        ReifiedTypeParameterInOverrideImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_PROPERTY_WITH_BACKING_FIELD) { firDiagnostic ->
        InlinePropertyWithBackingFieldImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_PROPERTY_WITH_BACKING_FIELD_DEPRECATION.errorFactory) { firDiagnostic ->
        InlinePropertyWithBackingFieldDeprecationErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_PROPERTY_WITH_BACKING_FIELD_DEPRECATION.warningFactory) { firDiagnostic ->
        InlinePropertyWithBackingFieldDeprecationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_INLINE_PARAMETER_MODIFIER) { firDiagnostic ->
        IllegalInlineParameterModifierImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED) { firDiagnostic ->
        InlineSuspendFunctionTypeUnsupportedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS) { firDiagnostic ->
        InefficientEqualsOverridingInValueClassImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_DEPRECATED) { firDiagnostic ->
        InlineClassDeprecatedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LESS_VISIBLE_TYPE_ACCESS_IN_INLINE.errorFactory) { firDiagnostic ->
        LessVisibleTypeAccessInInlineErrorImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LESS_VISIBLE_TYPE_ACCESS_IN_INLINE.warningFactory) { firDiagnostic ->
        LessVisibleTypeAccessInInlineWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE.errorFactory) { firDiagnostic ->
        LessVisibleTypeInInlineAccessedSignatureErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE.warningFactory) { firDiagnostic ->
        LessVisibleTypeInInlineAccessedSignatureWarningImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE.errorFactory) { firDiagnostic ->
        LessVisibleContainingClassInInlineErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.c),
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE.warningFactory) { firDiagnostic ->
        LessVisibleContainingClassInInlineWarningImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.c),
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE.errorFactory) { firDiagnostic ->
        CallableReferenceToLessVisibleDeclarationInInlineErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE.warningFactory) { firDiagnostic ->
        CallableReferenceToLessVisibleDeclarationInInlineWarningImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INLINE_FROM_HIGHER_PLATFORM) { firDiagnostic ->
        InlineFromHigherPlatformImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON) { firDiagnostic ->
        CannotAllUnderImportFromSingletonImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PACKAGE_CANNOT_BE_IMPORTED) { firDiagnostic ->
        PackageCannotBeImportedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_BE_IMPORTED) { firDiagnostic ->
        CannotBeImportedImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_IMPORT) { firDiagnostic ->
        ConflictingImportImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPERATOR_RENAMED_ON_IMPORT) { firDiagnostic ->
        OperatorRenamedOnImportImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT.errorFactory) { firDiagnostic ->
        TypealiasAsCallableQualifierInImportErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT.warningFactory) { firDiagnostic ->
        TypealiasAsCallableQualifierInImportWarningImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_SUSPEND_FUNCTION_CALL) { firDiagnostic ->
        IllegalSuspendFunctionCallImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_SUSPEND_PROPERTY_ACCESS) { firDiagnostic ->
        IllegalSuspendPropertyAccessImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_LOCAL_SUSPENSION_POINT) { firDiagnostic ->
        NonLocalSuspensionPointImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL) { firDiagnostic ->
        IllegalRestrictedSuspendingFunctionCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND) { firDiagnostic ->
        NonModifierFormForBuiltInSuspendImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND) { firDiagnostic ->
        ModifierFormForNonBuiltInSuspendImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN.errorFactory) { firDiagnostic ->
        ModifierFormForNonBuiltInSuspendFunErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN.warningFactory) { firDiagnostic ->
        ModifierFormForNonBuiltInSuspendFunWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_FOR_BUILT_IN_SUSPEND) { firDiagnostic ->
        ReturnForBuiltInSuspendImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES) { firDiagnostic ->
        MixingSuspendAndNonSuspendSupertypesImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MIXING_FUNCTIONAL_KINDS_IN_SUPERTYPES) { firDiagnostic ->
        MixingFunctionalKindsInSupertypesImpl(
            firDiagnostic.a.map { functionTypeKind ->
                functionTypeKind
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_LABEL_WARNING) { firDiagnostic ->
        RedundantLabelWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_LABELS_ARE_FORBIDDEN) { firDiagnostic ->
        MultipleLabelsAreForbiddenImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY) { firDiagnostic ->
        DeprecatedAccessToEnumEntryCompanionPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM) { firDiagnostic ->
        DeprecatedAccessToEntryPropertyFromEnumImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_ACCESS_TO_ENTRIES_PROPERTY) { firDiagnostic ->
        DeprecatedAccessToEntriesPropertyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE) { firDiagnostic ->
        DeprecatedAccessToEnumEntryPropertyAsReferenceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_ACCESS_TO_ENTRIES_AS_QUALIFIER) { firDiagnostic ->
        DeprecatedAccessToEntriesAsQualifierImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DECLARATION_OF_ENUM_ENTRY_ENTRIES.errorFactory) { firDiagnostic ->
        DeclarationOfEnumEntryEntriesErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DECLARATION_OF_ENUM_ENTRY_ENTRIES.warningFactory) { firDiagnostic ->
        DeclarationOfEnumEntryEntriesWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_CLASS) { firDiagnostic ->
        IncompatibleClassImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRE_RELEASE_CLASS) { firDiagnostic ->
        PreReleaseClassImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { string ->
                string
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IR_WITH_UNSTABLE_ABI_COMPILED_CLASS) { firDiagnostic ->
        IrWithUnstableAbiCompiledClassImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BUILDER_INFERENCE_STUB_RECEIVER) { firDiagnostic ->
        BuilderInferenceStubReceiverImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION) { firDiagnostic ->
        BuilderInferenceMultiLambdaRestrictionImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERRIDE_CANNOT_BE_STATIC) { firDiagnostic ->
        OverrideCannotBeStaticImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION) { firDiagnostic ->
        JvmStaticNotInObjectOrClassCompanionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION) { firDiagnostic ->
        JvmStaticNotInObjectOrCompanionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_STATIC_ON_NON_PUBLIC_MEMBER) { firDiagnostic ->
        JvmStaticOnNonPublicMemberImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_STATIC_ON_CONST_OR_JVM_FIELD) { firDiagnostic ->
        JvmStaticOnConstOrJvmFieldImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_STATIC_ON_EXTERNAL_IN_INTERFACE) { firDiagnostic ->
        JvmStaticOnExternalInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INAPPLICABLE_JVM_NAME) { firDiagnostic ->
        InapplicableJvmNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.ILLEGAL_JVM_NAME) { firDiagnostic ->
        IllegalJvmNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.FUNCTION_DELEGATE_MEMBER_NAME_CLASH) { firDiagnostic ->
        FunctionDelegateMemberNameClashImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION) { firDiagnostic ->
        ValueClassWithoutJvmInlineAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_INLINE_WITHOUT_VALUE_CLASS) { firDiagnostic ->
        JvmInlineWithoutValueClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME) { firDiagnostic ->
        InapplicableJvmExposeBoxedWithNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.USELESS_JVM_EXPOSE_BOXED) { firDiagnostic ->
        UselessJvmExposeBoxedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND) { firDiagnostic ->
        JvmExposeBoxedCannotExposeSuspendImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_EXPOSE_BOXED_REQUIRES_NAME) { firDiagnostic ->
        JvmExposeBoxedRequiresNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME) { firDiagnostic ->
        JvmExposeBoxedCannotBeTheSameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME) { firDiagnostic ->
        JvmExposeBoxedCannotBeTheSameAsJvmNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT) { firDiagnostic ->
        JvmExposeBoxedCannotExposeOpenAbstractImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC) { firDiagnostic ->
        JvmExposeBoxedCannotExposeSyntheticImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_LOCALS) { firDiagnostic ->
        JvmExposeBoxedCannotExposeLocalsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_REIFIED) { firDiagnostic ->
        JvmExposeBoxedCannotExposeReifiedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE) { firDiagnostic ->
        WrongNullabilityForJavaOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE) { firDiagnostic ->
        AccidentalOverrideClashByJvmSignatureImpl(
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.c),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE.errorFactory) { firDiagnostic ->
        ImplementationByDelegationWithDifferentGenericSignatureErrorImpl(
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.a),
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE.warningFactory) { firDiagnostic ->
        ImplementationByDelegationWithDifferentGenericSignatureWarningImpl(
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.a),
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION) { firDiagnostic ->
        NotYetSupportedLocalInlineFunctionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.PROPERTY_HIDES_JAVA_FIELD) { firDiagnostic ->
        PropertyHidesJavaFieldImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_TYPE_MISMATCH) { firDiagnostic ->
        JavaTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS) { firDiagnostic ->
        ReceiverNullabilityMismatchBasedOnJavaAnnotationsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS) { firDiagnostic ->
        NullabilityMismatchBasedOnJavaAnnotationsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA) { firDiagnostic ->
        NullabilityMismatchBasedOnExplicitTypeArgumentsForJavaImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES) { firDiagnostic ->
        TypeMismatchWhenFlexibilityChangesImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_CLASS_ON_COMPANION) { firDiagnostic ->
        JavaClassOnCompanionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.UPPER_BOUND_CANNOT_BE_ARRAY) { firDiagnostic ->
        UpperBoundCannotBeArrayImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS) { firDiagnostic ->
        UpperBoundViolatedBasedOnJavaAnnotationsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS) { firDiagnostic ->
        UpperBoundViolatedInTypealiasExpansionBasedOnJavaAnnotationsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.STRICTFP_ON_CLASS) { firDiagnostic ->
        StrictfpOnClassImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_ON_ABSTRACT) { firDiagnostic ->
        SynchronizedOnAbstractImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_IN_INTERFACE) { firDiagnostic ->
        SynchronizedInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_IN_ANNOTATION.errorFactory) { firDiagnostic ->
        SynchronizedInAnnotationErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_IN_ANNOTATION.warningFactory) { firDiagnostic ->
        SynchronizedInAnnotationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_ON_INLINE) { firDiagnostic ->
        SynchronizedOnInlineImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_ON_VALUE_CLASS.errorFactory) { firDiagnostic ->
        SynchronizedOnValueClassErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_ON_VALUE_CLASS.warningFactory) { firDiagnostic ->
        SynchronizedOnValueClassWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_ON_SUSPEND.errorFactory) { firDiagnostic ->
        SynchronizedOnSuspendErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_ON_SUSPEND.warningFactory) { firDiagnostic ->
        SynchronizedOnSuspendWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS) { firDiagnostic ->
        OverloadsWithoutDefaultArgumentsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_ABSTRACT) { firDiagnostic ->
        OverloadsAbstractImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_INTERFACE) { firDiagnostic ->
        OverloadsInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_LOCAL) { firDiagnostic ->
        OverloadsLocalImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR_ERROR) { firDiagnostic ->
        OverloadsAnnotationClassConstructorErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_PRIVATE) { firDiagnostic ->
        OverloadsPrivateImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.DEPRECATED_JAVA_ANNOTATION) { firDiagnostic ->
        DeprecatedJavaAnnotationImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_PACKAGE_NAME_CANNOT_BE_EMPTY) { firDiagnostic ->
        JvmPackageNameCannotBeEmptyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_PACKAGE_NAME_MUST_BE_VALID_NAME) { firDiagnostic ->
        JvmPackageNameMustBeValidNameImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES) { firDiagnostic ->
        JvmPackageNameNotSupportedInFilesWithClassesImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION) { firDiagnostic ->
        PositionedValueArgumentForJavaAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REDUNDANT_REPEATABLE_ANNOTATION) { firDiagnostic ->
        RedundantRepeatableAnnotationImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.THROWS_IN_ANNOTATION.errorFactory) { firDiagnostic ->
        ThrowsInAnnotationErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.THROWS_IN_ANNOTATION.warningFactory) { firDiagnostic ->
        ThrowsInAnnotationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_SERIALIZABLE_LAMBDA_ON_INLINED_FUNCTION_LITERALS.errorFactory) { firDiagnostic ->
        JvmSerializableLambdaOnInlinedFunctionLiteralsErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_SERIALIZABLE_LAMBDA_ON_INLINED_FUNCTION_LITERALS.warningFactory) { firDiagnostic ->
        JvmSerializableLambdaOnInlinedFunctionLiteralsWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INCOMPATIBLE_ANNOTATION_TARGETS) { firDiagnostic ->
        IncompatibleAnnotationTargetsImpl(
            firDiagnostic.a.map { string ->
                string
            },
            firDiagnostic.b.map { string ->
                string
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.ANNOTATION_TARGETS_ONLY_IN_JAVA) { firDiagnostic ->
        AnnotationTargetsOnlyInJavaImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.LOCAL_JVM_RECORD) { firDiagnostic ->
        LocalJvmRecordImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NON_FINAL_JVM_RECORD) { firDiagnostic ->
        NonFinalJvmRecordImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.ENUM_JVM_RECORD) { firDiagnostic ->
        EnumJvmRecordImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS) { firDiagnostic ->
        JvmRecordWithoutPrimaryConstructorParametersImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NON_DATA_CLASS_JVM_RECORD) { firDiagnostic ->
        NonDataClassJvmRecordImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_RECORD_NOT_VAL_PARAMETER) { firDiagnostic ->
        JvmRecordNotValParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_RECORD_NOT_LAST_VARARG_PARAMETER) { firDiagnostic ->
        JvmRecordNotLastVarargParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INNER_JVM_RECORD) { firDiagnostic ->
        InnerJvmRecordImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.FIELD_IN_JVM_RECORD) { firDiagnostic ->
        FieldInJvmRecordImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.DELEGATION_BY_IN_JVM_RECORD) { firDiagnostic ->
        DelegationByInJvmRecordImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_RECORD_EXTENDS_CLASS) { firDiagnostic ->
        JvmRecordExtendsClassImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE) { firDiagnostic ->
        IllegalJavaLangRecordSupertypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE) { firDiagnostic ->
        JavaModuleDoesNotDependOnModuleImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE) { firDiagnostic ->
        JavaModuleDoesNotReadUnnamedModuleImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE) { firDiagnostic ->
        JavaModuleDoesNotExportPackageImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_DEFAULT_WITHOUT_COMPATIBILITY_NOT_IN_ENABLE_MODE) { firDiagnostic ->
        JvmDefaultWithoutCompatibilityNotInEnableModeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_NOT_IN_NO_COMPATIBILITY_MODE) { firDiagnostic ->
        JvmDefaultWithCompatibilityNotInNoCompatibilityModeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT) { firDiagnostic ->
        ExternalDeclarationCannotBeAbstractImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_HAVE_BODY) { firDiagnostic ->
        ExternalDeclarationCannotHaveBodyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.EXTERNAL_DECLARATION_IN_INTERFACE) { firDiagnostic ->
        ExternalDeclarationInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_BE_INLINED) { firDiagnostic ->
        ExternalDeclarationCannotBeInlinedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NON_SOURCE_REPEATED_ANNOTATION) { firDiagnostic ->
        NonSourceRepeatedAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATED_ANNOTATION_WITH_CONTAINER) { firDiagnostic ->
        RepeatedAnnotationWithContainerImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR) { firDiagnostic ->
        RepeatableContainerMustHaveValueArrayErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER_ERROR) { firDiagnostic ->
        RepeatableContainerHasNonDefaultParameterErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR) { firDiagnostic ->
        RepeatableContainerHasShorterRetentionErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR) { firDiagnostic ->
        RepeatableContainerTargetSetNotASubsetErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER_ERROR) { firDiagnostic ->
        RepeatableAnnotationHasNestedClassNamedContainerErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SUSPENSION_POINT_INSIDE_CRITICAL_SECTION) { firDiagnostic ->
        SuspensionPointInsideCriticalSectionImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INAPPLICABLE_JVM_FIELD) { firDiagnostic ->
        InapplicableJvmFieldImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INAPPLICABLE_JVM_FIELD_WARNING) { firDiagnostic ->
        InapplicableJvmFieldWarningImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE) { firDiagnostic ->
        IdentitySensitiveOperationsWithValueTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS) { firDiagnostic ->
        SynchronizedBlockOnJavaValueBasedClassImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE.errorFactory) { firDiagnostic ->
        SynchronizedBlockOnValueClassOrPrimitiveErrorImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE.warningFactory) { firDiagnostic ->
        SynchronizedBlockOnValueClassOrPrimitiveWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_SYNTHETIC_ON_DELEGATE) { firDiagnostic ->
        JvmSyntheticOnDelegateImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC) { firDiagnostic ->
        SubclassCantCallCompanionProtectedNonStaticImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR) { firDiagnostic ->
        ConcurrentHashMapContainsOperatorErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR) { firDiagnostic ->
        SpreadOnSignaturePolymorphicCallErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE) { firDiagnostic ->
        JavaSamInterfaceConstructorReferenceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NO_REFLECTION_IN_CLASS_PATH) { firDiagnostic ->
        NoReflectionInClassPathImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN) { firDiagnostic ->
        SyntheticPropertyWithoutJavaOriginImpl(
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY) { firDiagnostic ->
        JavaFieldShadowedByKotlinPropertyImpl(
            firSymbolBuilder.variableBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.MISSING_BUILT_IN_DECLARATION) { firDiagnostic ->
        MissingBuiltInDeclarationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.DANGEROUS_CHARACTERS) { firDiagnostic ->
        DangerousCharactersImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.IMPLEMENTING_FUNCTION_INTERFACE) { firDiagnostic ->
        ImplementingFunctionInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS) { firDiagnostic ->
        OverridingExternalFunWithOptionalParamsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE) { firDiagnostic ->
        OverridingExternalFunWithOptionalParamsWithFakeImpl(
            firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION) { firDiagnostic ->
        CallToDefinedExternallyFromNonExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.EXTERNAL_ENUM_ENTRY_WITH_BODY) { firDiagnostic ->
        ExternalEnumEntryWithBodyImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE) { firDiagnostic ->
        ExternalTypeExtendsNonExternalTypeImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING) { firDiagnostic ->
        EnumClassInExternalDeclarationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING) { firDiagnostic ->
        InlineClassInExternalDeclarationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION) { firDiagnostic ->
        InlineClassInExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION) { firDiagnostic ->
        ExtensionFunctionInExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE) { firDiagnostic ->
        NonExternalDeclarationInInappropriateFileImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_EXTERNAL_INHERITORS_ONLY) { firDiagnostic ->
        JsExternalInheritorsOnlyImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_EXTERNAL_ARGUMENT) { firDiagnostic ->
        JsExternalArgumentImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.WRONG_EXPORTED_DECLARATION) { firDiagnostic ->
        WrongExportedDeclarationImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NON_EXPORTABLE_TYPE) { firDiagnostic ->
        NonExportableTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NON_CONSUMABLE_EXPORTED_IDENTIFIER) { firDiagnostic ->
        NonConsumableExportedIdentifierImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NAMED_COMPANION_IN_EXPORTED_INTERFACE) { firDiagnostic ->
        NamedCompanionInExportedInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NOT_EXPORTED_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED) { firDiagnostic ->
        NotExportedActualDeclarationWhileExpectIsExportedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.NESTED_JS_EXPORT) { firDiagnostic ->
        NestedJsExportImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.DELEGATION_BY_DYNAMIC) { firDiagnostic ->
        DelegationByDynamicImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.PROPERTY_DELEGATION_BY_DYNAMIC) { firDiagnostic ->
        PropertyDelegationByDynamicImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.SPREAD_OPERATOR_IN_DYNAMIC_CALL) { firDiagnostic ->
        SpreadOperatorInDynamicCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.WRONG_OPERATION_WITH_DYNAMIC) { firDiagnostic ->
        WrongOperationWithDynamicImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_STATIC_NOT_IN_CLASS_COMPANION) { firDiagnostic ->
        JsStaticNotInClassCompanionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_STATIC_ON_NON_PUBLIC_MEMBER) { firDiagnostic ->
        JsStaticOnNonPublicMemberImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.JS_STATIC_ON_CONST) { firDiagnostic ->
        JsStaticOnConstImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirSyntaxErrors.SYNTAX) { firDiagnostic ->
        SyntaxImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.NESTED_EXTERNAL_DECLARATION) { firDiagnostic ->
        NestedExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION) { firDiagnostic ->
        WrongExternalDeclarationImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE) { firDiagnostic ->
        NestedClassInExternalInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.INLINE_EXTERNAL_DECLARATION) { firDiagnostic ->
        InlineExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE) { firDiagnostic ->
        NonAbstractMemberOfExternalInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER) { firDiagnostic ->
        ExternalClassConstructorPropertyParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.EXTERNAL_ANONYMOUS_INITIALIZER) { firDiagnostic ->
        ExternalAnonymousInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.EXTERNAL_DELEGATION) { firDiagnostic ->
        ExternalDelegationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL) { firDiagnostic ->
        ExternalDelegatedConstructorCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION) { firDiagnostic ->
        WrongBodyOfExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION) { firDiagnostic ->
        WrongInitializerOfExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER) { firDiagnostic ->
        WrongDefaultValueForExternalFunParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.CANNOT_CHECK_FOR_EXTERNAL_INTERFACE) { firDiagnostic ->
        CannotCheckForExternalInterfaceImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.UNCHECKED_CAST_TO_EXTERNAL_INTERFACE) { firDiagnostic ->
        UncheckedCastToExternalInterfaceImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.EXTERNAL_INTERFACE_AS_CLASS_LITERAL) { firDiagnostic ->
        ExternalInterfaceAsClassLiteralImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT) { firDiagnostic ->
        ExternalInterfaceAsReifiedTypeArgumentImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.NAMED_COMPANION_IN_EXTERNAL_INTERFACE) { firDiagnostic ->
        NamedCompanionInExternalInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirWebCommonErrors.JSCODE_ARGUMENT_NON_CONST_EXPRESSION) { firDiagnostic ->
        JscodeArgumentNonConstExpressionImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
}
