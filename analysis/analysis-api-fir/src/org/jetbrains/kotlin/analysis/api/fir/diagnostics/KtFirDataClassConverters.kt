/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal val KT_DIAGNOSTIC_CONVERTER = KtDiagnosticConverterBuilder.buildConverter {
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
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_REFERENCE) { firDiagnostic ->
        InvisibleReferenceImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_REFERENCE) { firDiagnostic ->
        UnresolvedReferenceImpl(
            firDiagnostic.a,
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
    add(FirErrors.CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS) { firDiagnostic ->
        CreatingAnInstanceOfAbstractClassImpl(
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
    add(FirErrors.FORBIDDEN_BINARY_MOD) { firDiagnostic ->
        ForbiddenBinaryModImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_BINARY_MOD) { firDiagnostic ->
        DeprecatedBinaryModImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
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
    add(FirJvmErrors.SUPER_CALL_WITH_DEFAULT_PARAMETERS) { firDiagnostic ->
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
    add(FirErrors.QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE) { firDiagnostic ->
        QualifiedSupertypeExtendedByOtherSupertypeImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
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
    add(FirErrors.SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE) { firDiagnostic ->
        SupertypeIsExtensionFunctionTypeImpl(
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
    add(FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE) { firDiagnostic ->
        SupertypeNotAClassOrInterfaceImpl(
            firDiagnostic.a,
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
    add(FirErrors.PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS) { firDiagnostic ->
        PrimaryConstructorRequiredForDataClassImpl(
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
    add(FirErrors.OVERRIDE_DEPRECATION) { firDiagnostic ->
        OverrideDeprecationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ON_SUPERCLASS.errorFactory) { firDiagnostic ->
        AnnotationOnSuperclassErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ON_SUPERCLASS.warningFactory) { firDiagnostic ->
        AnnotationOnSuperclassWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION.errorFactory) { firDiagnostic ->
        RestrictedRetentionForExpressionAnnotationErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION.warningFactory) { firDiagnostic ->
        RestrictedRetentionForExpressionAnnotationWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_ANNOTATION_TARGET) { firDiagnostic ->
        WrongAnnotationTargetImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET) { firDiagnostic ->
        WrongAnnotationTargetWithUseSiteTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
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
    add(FirErrors.PLUGIN_ANNOTATION_AMBIGUITY) { firDiagnostic ->
        PluginAnnotationAmbiguityImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.WRONG_JS_QUALIFIER) { firDiagnostic ->
        WrongJsQualifierImpl(
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
    add(FirErrors.OPT_IN_IS_NOT_ENABLED) { firDiagnostic ->
        OptInIsNotEnabledImpl(
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
    add(FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE) { firDiagnostic ->
        ExposedTypealiasExpandedTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_FUNCTION_RETURN_TYPE) { firDiagnostic ->
        ExposedFunctionReturnTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_RECEIVER_TYPE) { firDiagnostic ->
        ExposedReceiverTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE) { firDiagnostic ->
        ExposedPropertyTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR.errorFactory) { firDiagnostic ->
        ExposedPropertyTypeInConstructorErrorImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR.warningFactory) { firDiagnostic ->
        ExposedPropertyTypeInConstructorWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PARAMETER_TYPE) { firDiagnostic ->
        ExposedParameterTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_INTERFACE) { firDiagnostic ->
        ExposedSuperInterfaceImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_CLASS) { firDiagnostic ->
        ExposedSuperClassImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPE_PARAMETER_BOUND) { firDiagnostic ->
        ExposedTypeParameterBoundImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic.c,
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
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFIX_MODIFIER_REQUIRED) { firDiagnostic ->
        InfixModifierRequiredImpl(
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firDiagnostic.a),
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
    add(FirErrors.NULL_FOR_NONNULL_TYPE) { firDiagnostic ->
        NullForNonnullTypeImpl(
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
    add(FirErrors.NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER) { firDiagnostic ->
        NewInferenceNoInformationForParameterImpl(
            firDiagnostic.a,
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
    add(FirErrors.INFERENCE_UNSUCCESSFUL_FORK) { firDiagnostic ->
        InferenceUnsuccessfulForkImpl(
            firDiagnostic.a,
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
    add(FirErrors.NO_CONTEXT_RECEIVER) { firDiagnostic ->
        NoContextReceiverImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER) { firDiagnostic ->
        MultipleArgumentsApplicableForContextReceiverImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
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
    add(FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED) { firDiagnostic ->
        TypeArgumentsNotAllowedImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS) { firDiagnostic ->
        WrongNumberOfTypeArgumentsImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b),
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
    add(FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE) { firDiagnostic ->
        ReifiedTypeInCatchClauseImpl(
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
    add(FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY.errorFactory) { firDiagnostic ->
        TypeParameterAsReifiedArrayErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY.warningFactory) { firDiagnostic ->
        TypeParameterAsReifiedArrayWarningImpl(
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
    add(FirErrors.FINAL_UPPER_BOUND) { firDiagnostic ->
        FinalUpperBoundImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE) { firDiagnostic ->
        UpperBoundIsExtensionFunctionTypeImpl(
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
    add(FirErrors.CYCLIC_GENERIC_UPPER_BOUND) { firDiagnostic ->
        CyclicGenericUpperBoundImpl(
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
    add(FirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION) { firDiagnostic ->
        InferredTypeVariableIntoEmptyIntersectionImpl(
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
    add(FirErrors.NOTHING_TO_OVERRIDE) { firDiagnostic ->
        NothingToOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a),
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
    add(FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE) { firDiagnostic ->
        CannotWeakenAccessPrivilegeImpl(
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
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        AbstractClassMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER.errorFactory) { firDiagnostic ->
        InvisibleAbstractMemberFromSuperErrorImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER.warningFactory) { firDiagnostic ->
        InvisibleAbstractMemberFromSuperWarningImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b),
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
    add(FirErrors.PACKAGE_OR_CLASSIFIER_REDECLARATION) { firDiagnostic ->
        PackageOrClassifierRedeclarationImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
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
    add(FirErrors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION) { firDiagnostic ->
        ValueParameterWithNoTypeAnnotationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_PARAMETER_TYPE) { firDiagnostic ->
        CannotInferParameterTypeImpl(
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
    add(FirErrors.FUN_INTERFACE_CONSTRUCTOR_REFERENCE) { firDiagnostic ->
        FunInterfaceConstructorReferenceImpl(
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
    add(FirErrors.MUST_BE_INITIALIZED) { firDiagnostic ->
        MustBeInitializedImpl(
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
    add(FirErrors.ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS) { firDiagnostic ->
        ActualFunctionWithDefaultArgumentsImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE) { firDiagnostic ->
        ActualAnnotationConflictingDefaultArgumentValueImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(firDiagnostic.a),
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
    add(FirErrors.NO_ACTUAL_FOR_EXPECT) { firDiagnostic ->
        NoActualForExpectImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic.c.mapKeys { (incompatible, _) ->
                incompatible
            }.mapValues { (_, collection) -> 
                collection.map { firBasedSymbol ->
                                    firSymbolBuilder.buildSymbol(firBasedSymbol)
                                }
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_WITHOUT_EXPECT) { firDiagnostic ->
        ActualWithoutExpectImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b.mapKeys { (incompatible, _) ->
                incompatible
            }.mapValues { (_, collection) -> 
                collection.map { firBasedSymbol ->
                                    firSymbolBuilder.buildSymbol(firBasedSymbol)
                                }
            },
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_ACTUALS) { firDiagnostic ->
        AmbiguousActualsImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol)
            },
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
                firSymbolBuilder.buildSymbol(pair.first) to pair.second.mapKeys { (incompatible, _) ->
                                    incompatible
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
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_ON_NULLABLE) { firDiagnostic ->
        ComponentFunctionOnNullableImpl(
            firDiagnostic.a,
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
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a),
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
            firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD.errorFactory) { firDiagnostic ->
        ValReassignmentViaBackingFieldErrorImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir.propertySymbol),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD.warningFactory) { firDiagnostic ->
        ValReassignmentViaBackingFieldWarningImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir.propertySymbol),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAPTURED_VAL_INITIALIZATION) { firDiagnostic ->
        CapturedValInitializationImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION) { firDiagnostic ->
        CapturedMemberValInitializationImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a),
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SETTER_PROJECTED_OUT) { firDiagnostic ->
        SetterProjectedOutImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a),
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
    add(FirErrors.WRONG_IMPLIES_CONDITION) { firDiagnostic ->
        WrongImpliesConditionImpl(
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
            firDiagnostic.a.source!!.psi as KtExpression,
            firDiagnostic.b,
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
            firDiagnostic.a.source!!.psi as KtExpression,
            firDiagnostic.b,
            firDiagnostic.c.source!!.psi as KtExpression,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_OPERATOR_CALL) { firDiagnostic ->
        UnsafeOperatorCallImpl(
            firDiagnostic.a.source!!.psi as KtExpression,
            firDiagnostic.b,
            firDiagnostic.c.source!!.psi as KtExpression,
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
    add(FirErrors.SAFE_CALL_WILL_CHANGE_NULLABILITY) { firDiagnostic ->
        SafeCallWillChangeNullabilityImpl(
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
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_EXHAUSTIVE_WHEN_STATEMENT) { firDiagnostic ->
        NonExhaustiveWhenStatementImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { whenMissingCase ->
                whenMissingCase
            },
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
    add(FirErrors.DUPLICATE_LABEL_IN_WHEN) { firDiagnostic ->
        DuplicateLabelInWhenImpl(
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
    add(FirErrors.DANGEROUS_CHARACTERS) { firDiagnostic ->
        DangerousCharactersImpl(
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
    add(FirErrors.INC_DEC_SHOULD_NOT_RETURN_UNIT) { firDiagnostic ->
        IncDecShouldNotReturnUnitImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT) { firDiagnostic ->
        AssignmentOperatorShouldReturnUnitImpl(
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firDiagnostic.a),
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_AS_OPERATOR) { firDiagnostic ->
        PropertyAsOperatorImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a),
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
    add(FirErrors.REDUNDANT_EXPLICIT_TYPE) { firDiagnostic ->
        RedundantExplicitTypeImpl(
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
    add(FirErrors.CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT) { firDiagnostic ->
        CanBeReplacedWithOperatorAssignmentImpl(
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
    add(FirErrors.ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS) { firDiagnostic ->
        ArrayEqualityOperatorCanBeReplacedWithEqualsImpl(
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
    add(FirErrors.NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY) { firDiagnostic ->
        NoReturnInFunctionWithBlockBodyImpl(
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
    add(FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        ProtectedCallFromPublicInlineImpl(
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
            firDiagnostic.a.source!!.psi as KtExpression,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
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
    add(FirErrors.REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE) { firDiagnostic ->
        RedundantInlineSuspendFunctionTypeImpl(
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
    add(FirErrors.REDUNDANT_LABEL_WARNING) { firDiagnostic ->
        RedundantLabelWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.CONFLICTING_JVM_DECLARATIONS) { firDiagnostic ->
        ConflictingJvmDeclarationsImpl(
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
    add(FirJvmErrors.JVM_EXPOSE_BOXED_WITHOUT_INLINE) { firDiagnostic ->
        JvmExposeBoxedWithoutInlineImpl(
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
    add(FirJvmErrors.UPPER_BOUND_CANNOT_BE_ARRAY) { firDiagnostic ->
        UpperBoundCannotBeArrayImpl(
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
    add(FirJvmErrors.VOLATILE_ON_VALUE) { firDiagnostic ->
        VolatileOnValueImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.VOLATILE_ON_DELEGATE) { firDiagnostic ->
        VolatileOnDelegateImpl(
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
    add(FirJvmErrors.SYNCHRONIZED_ON_INLINE) { firDiagnostic ->
        SynchronizedOnInlineImpl(
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
    add(FirJvmErrors.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR.errorFactory) { firDiagnostic ->
        OverloadsAnnotationClassConstructorErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR.warningFactory) { firDiagnostic ->
        OverloadsAnnotationClassConstructorWarningImpl(
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
    add(FirJvmErrors.JVM_DEFAULT_NOT_IN_INTERFACE) { firDiagnostic ->
        JvmDefaultNotInInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_DEFAULT_IN_JVM6_TARGET) { firDiagnostic ->
        JvmDefaultInJvm6TargetImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_DEFAULT_REQUIRED_FOR_OVERRIDE) { firDiagnostic ->
        JvmDefaultRequiredForOverrideImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_DEFAULT_IN_DECLARATION) { firDiagnostic ->
        JvmDefaultInDeclarationImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION) { firDiagnostic ->
        JvmDefaultWithCompatibilityInDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE) { firDiagnostic ->
        JvmDefaultWithCompatibilityNotOnInterfaceImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT) { firDiagnostic ->
        NonJvmDefaultOverridesJavaDefaultImpl(
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
    add(FirJvmErrors.REPEATED_ANNOTATION_TARGET6) { firDiagnostic ->
        RepeatedAnnotationTarget6Impl(
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
    add(FirJvmErrors.REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY.errorFactory) { firDiagnostic ->
        RepeatableContainerMustHaveValueArrayErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY.warningFactory) { firDiagnostic ->
        RepeatableContainerMustHaveValueArrayWarningImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER.errorFactory) { firDiagnostic ->
        RepeatableContainerHasNonDefaultParameterErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER.warningFactory) { firDiagnostic ->
        RepeatableContainerHasNonDefaultParameterWarningImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION.errorFactory) { firDiagnostic ->
        RepeatableContainerHasShorterRetentionErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION.warningFactory) { firDiagnostic ->
        RepeatableContainerHasShorterRetentionWarningImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic.d,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET.errorFactory) { firDiagnostic ->
        RepeatableContainerTargetSetNotASubsetErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET.warningFactory) { firDiagnostic ->
        RepeatableContainerTargetSetNotASubsetWarningImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER.errorFactory) { firDiagnostic ->
        RepeatableAnnotationHasNestedClassNamedContainerErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER.warningFactory) { firDiagnostic ->
        RepeatableAnnotationHasNestedClassNamedContainerWarningImpl(
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
    add(FirJvmErrors.JVM_SYNTHETIC_ON_DELEGATE) { firDiagnostic ->
        JvmSyntheticOnDelegateImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET.errorFactory) { firDiagnostic ->
        DefaultMethodCallFromJava6TargetErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET.warningFactory) { firDiagnostic ->
        DefaultMethodCallFromJava6TargetWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET.errorFactory) { firDiagnostic ->
        InterfaceStaticMethodCallFromJava6TargetErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET.warningFactory) { firDiagnostic ->
        InterfaceStaticMethodCallFromJava6TargetWarningImpl(
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
    add(FirJvmErrors.CONCURRENT_HASH_MAP_CONTAINS_OPERATOR.errorFactory) { firDiagnostic ->
        ConcurrentHashMapContainsOperatorErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.CONCURRENT_HASH_MAP_CONTAINS_OPERATOR.warningFactory) { firDiagnostic ->
        ConcurrentHashMapContainsOperatorWarningImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL.errorFactory) { firDiagnostic ->
        SpreadOnSignaturePolymorphicCallErrorImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL.warningFactory) { firDiagnostic ->
        SpreadOnSignaturePolymorphicCallWarningImpl(
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
    add(FirJvmErrors.JAVA_SHADOWED_PROTECTED_FIELD_REFERENCE) { firDiagnostic ->
        JavaShadowedProtectedFieldReferenceImpl(
            firDiagnostic.a,
            firDiagnostic.b,
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
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firDiagnostic.a),
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
    add(FirJsErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER) { firDiagnostic ->
        ExternalClassConstructorPropertyParameterImpl(
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
    add(FirJsErrors.EXTERNAL_ANONYMOUS_INITIALIZER) { firDiagnostic ->
        ExternalAnonymousInitializerImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.EXTERNAL_DELEGATION) { firDiagnostic ->
        ExternalDelegationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL) { firDiagnostic ->
        ExternalDelegatedConstructorCallImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION) { firDiagnostic ->
        WrongBodyOfExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION) { firDiagnostic ->
        WrongInitializerOfExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER) { firDiagnostic ->
        WrongDefaultValueForExternalFunParameterImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NESTED_EXTERNAL_DECLARATION) { firDiagnostic ->
        NestedExternalDeclarationImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.WRONG_EXTERNAL_DECLARATION) { firDiagnostic ->
        WrongExternalDeclarationImpl(
            firDiagnostic.a,
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirJsErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE) { firDiagnostic ->
        NestedClassInExternalInterfaceImpl(
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
    add(FirJsErrors.INLINE_EXTERNAL_DECLARATION) { firDiagnostic ->
        InlineExternalDeclarationImpl(
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
    add(FirJsErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE) { firDiagnostic ->
        NonAbstractMemberOfExternalInterfaceImpl(
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
    add(FirJsErrors.DELEGATION_BY_DYNAMIC) { firDiagnostic ->
        DelegationByDynamicImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
    add(FirSyntaxErrors.SYNTAX) { firDiagnostic ->
        SyntaxImpl(
            firDiagnostic as KtPsiDiagnostic,
            token,
        )
    }
}
