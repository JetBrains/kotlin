/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
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
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
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

internal val KT_DIAGNOSTIC_CONVERTER = KtDiagnosticConverterBuilder.buildConverter {
    add(FirErrors.UNSUPPORTED) { firDiagnostic ->
        UnsupportedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSUPPORTED_FEATURE) { firDiagnostic ->
        UnsupportedFeatureImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEW_INFERENCE_ERROR) { firDiagnostic ->
        NewInferenceErrorImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SYNTAX) { firDiagnostic ->
        SyntaxImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OTHER_ERROR) { firDiagnostic ->
        OtherErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_CONST_EXPRESSION) { firDiagnostic ->
        IllegalConstExpressionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_UNDERSCORE) { firDiagnostic ->
        IllegalUnderscoreImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPRESSION_EXPECTED) { firDiagnostic ->
        ExpressionExpectedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNMENT_IN_EXPRESSION_CONTEXT) { firDiagnostic ->
        AssignmentInExpressionContextImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP) { firDiagnostic ->
        BreakOrContinueOutsideALoopImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_A_LOOP_LABEL) { firDiagnostic ->
        NotALoopLabelImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY) { firDiagnostic ->
        BreakOrContinueJumpsAcrossFunctionBoundaryImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIABLE_EXPECTED) { firDiagnostic ->
        VariableExpectedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATION_IN_INTERFACE) { firDiagnostic ->
        DelegationInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATION_NOT_TO_INTERFACE) { firDiagnostic ->
        DelegationNotToInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NESTED_CLASS_NOT_ALLOWED) { firDiagnostic ->
        NestedClassNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCORRECT_CHARACTER_LITERAL) { firDiagnostic ->
        IncorrectCharacterLiteralImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EMPTY_CHARACTER_LITERAL) { firDiagnostic ->
        EmptyCharacterLiteralImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL) { firDiagnostic ->
        TooManyCharactersInCharacterLiteralImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_ESCAPE) { firDiagnostic ->
        IllegalEscapeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INT_LITERAL_OUT_OF_RANGE) { firDiagnostic ->
        IntLiteralOutOfRangeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FLOAT_LITERAL_OUT_OF_RANGE) { firDiagnostic ->
        FloatLiteralOutOfRangeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_LONG_SUFFIX) { firDiagnostic ->
        WrongLongSuffixImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DIVISION_BY_ZERO) { firDiagnostic ->
        DivisionByZeroImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_OR_VAR_ON_LOOP_PARAMETER) { firDiagnostic ->
        ValOrVarOnLoopParameterImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_OR_VAR_ON_FUN_PARAMETER) { firDiagnostic ->
        ValOrVarOnFunParameterImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_OR_VAR_ON_CATCH_PARAMETER) { firDiagnostic ->
        ValOrVarOnCatchParameterImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER) { firDiagnostic ->
        ValOrVarOnSecondaryConstructorParameterImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_REFERENCE) { firDiagnostic ->
        InvisibleReferenceImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_REFERENCE) { firDiagnostic ->
        UnresolvedReferenceImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_LABEL) { firDiagnostic ->
        UnresolvedLabelImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DESERIALIZATION_ERROR) { firDiagnostic ->
        DeserializationErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ERROR_FROM_JAVA_RESOLUTION) { firDiagnostic ->
        ErrorFromJavaResolutionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_STDLIB_CLASS) { firDiagnostic ->
        MissingStdlibClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_THIS) { firDiagnostic ->
        NoThisImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATION_ERROR) { firDiagnostic ->
        DeprecationErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATION) { firDiagnostic ->
        DeprecationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_REFERENCE_WRONG_RECEIVER) { firDiagnostic ->
        UnresolvedReferenceWrongReceiverImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS) { firDiagnostic ->
        CreatingAnInstanceOfAbstractClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUNCTION_CALL_EXPECTED) { firDiagnostic ->
        FunctionCallExpectedImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_SELECTOR) { firDiagnostic ->
        IllegalSelectorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_RECEIVER_ALLOWED) { firDiagnostic ->
        NoReceiverAllowedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUNCTION_EXPECTED) { firDiagnostic ->
        FunctionExpectedImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESOLUTION_TO_CLASSIFIER) { firDiagnostic ->
        ResolutionToClassifierImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SUPER_CALL_WITH_DEFAULT_PARAMETERS) { firDiagnostic ->
        SuperCallWithDefaultParametersImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_A_SUPERTYPE) { firDiagnostic ->
        NotASupertypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER) { firDiagnostic ->
        TypeArgumentsRedundantInSuperQualifierImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE) { firDiagnostic ->
        SuperclassNotAccessibleFromInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE) { firDiagnostic ->
        QualifiedSupertypeExtendedByOtherSupertypeImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_INITIALIZED_IN_INTERFACE) { firDiagnostic ->
        SupertypeInitializedInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INTERFACE_WITH_SUPERCLASS) { firDiagnostic ->
        InterfaceWithSuperclassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FINAL_SUPERTYPE) { firDiagnostic ->
        FinalSupertypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASS_CANNOT_BE_EXTENDED_DIRECTLY) { firDiagnostic ->
        ClassCannotBeExtendedDirectlyImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE) { firDiagnostic ->
        SupertypeIsExtensionFunctionTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SINGLETON_IN_SUPERTYPE) { firDiagnostic ->
        SingletonInSupertypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_SUPERTYPE) { firDiagnostic ->
        NullableSupertypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_CLASSES_IN_SUPERTYPE_LIST) { firDiagnostic ->
        ManyClassesInSupertypeListImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_APPEARS_TWICE) { firDiagnostic ->
        SupertypeAppearsTwiceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASS_IN_SUPERTYPE_FOR_ENUM) { firDiagnostic ->
        ClassInSupertypeForEnumImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_SUPERTYPE) { firDiagnostic ->
        SealedSupertypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_SUPERTYPE_IN_LOCAL_CLASS) { firDiagnostic ->
        SealedSupertypeInLocalClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_INHERITOR_IN_DIFFERENT_PACKAGE) { firDiagnostic ->
        SealedInheritorInDifferentPackageImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_INHERITOR_IN_DIFFERENT_MODULE) { firDiagnostic ->
        SealedInheritorInDifferentModuleImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASS_INHERITS_JAVA_SEALED_CLASS) { firDiagnostic ->
        ClassInheritsJavaSealedClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE) { firDiagnostic ->
        SupertypeNotAClassOrInterfaceImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CYCLIC_INHERITANCE_HIERARCHY) { firDiagnostic ->
        CyclicInheritanceHierarchyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPANDED_TYPE_CANNOT_BE_INHERITED) { firDiagnostic ->
        ExpandedTypeCannotBeInheritedImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE) { firDiagnostic ->
        ProjectionInImmediateArgumentToSupertypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCONSISTENT_TYPE_PARAMETER_VALUES) { firDiagnostic ->
        InconsistentTypeParameterValuesImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir),
            firDiagnostic.c.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCONSISTENT_TYPE_PARAMETER_BOUNDS) { firDiagnostic ->
        InconsistentTypeParameterBoundsImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir),
            firDiagnostic.c.map { coneKotlinType ->
                firSymbolBuilder.typeBuilder.buildKtType(coneKotlinType)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_SUPER) { firDiagnostic ->
        AmbiguousSuperImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONSTRUCTOR_IN_OBJECT) { firDiagnostic ->
        ConstructorInObjectImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONSTRUCTOR_IN_INTERFACE) { firDiagnostic ->
        ConstructorInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PRIVATE_CONSTRUCTOR_IN_ENUM) { firDiagnostic ->
        NonPrivateConstructorInEnumImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED) { firDiagnostic ->
        NonPrivateOrProtectedConstructorInSealedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CYCLIC_CONSTRUCTOR_DELEGATION_CALL) { firDiagnostic ->
        CyclicConstructorDelegationCallImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED) { firDiagnostic ->
        PrimaryConstructorDelegationCallExpectedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_NOT_INITIALIZED) { firDiagnostic ->
        SupertypeNotInitializedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR) { firDiagnostic ->
        SupertypeInitializedWithoutPrimaryConstructorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR) { firDiagnostic ->
        DelegationSuperCallInEnumConstructorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS) { firDiagnostic ->
        PrimaryConstructorRequiredForDataClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPLICIT_DELEGATION_CALL_REQUIRED) { firDiagnostic ->
        ExplicitDelegationCallRequiredImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SEALED_CLASS_CONSTRUCTOR_CALL) { firDiagnostic ->
        SealedClassConstructorCallImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_WITHOUT_PARAMETERS) { firDiagnostic ->
        DataClassWithoutParametersImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_VARARG_PARAMETER) { firDiagnostic ->
        DataClassVarargParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_NOT_PROPERTY_PARAMETER) { firDiagnostic ->
        DataClassNotPropertyParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR) { firDiagnostic ->
        AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST) { firDiagnostic ->
        AnnotationArgumentMustBeConstImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST) { firDiagnostic ->
        AnnotationArgumentMustBeEnumConstImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL) { firDiagnostic ->
        AnnotationArgumentMustBeKclassLiteralImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_CLASS_MEMBER) { firDiagnostic ->
        AnnotationClassMemberImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT) { firDiagnostic ->
        AnnotationParameterDefaultValueMustBeConstantImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER) { firDiagnostic ->
        InvalidTypeOfAnnotationMemberImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_ANNOTATION_CLASS_ERROR) { firDiagnostic ->
        LocalAnnotationClassErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER) { firDiagnostic ->
        MissingValOnAnnotationParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION) { firDiagnostic ->
        NonConstValUsedInConstantExpressionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_CLASS_CONSTRUCTOR_CALL) { firDiagnostic ->
        AnnotationClassConstructorCallImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_AN_ANNOTATION_CLASS) { firDiagnostic ->
        NotAnAnnotationClassImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER) { firDiagnostic ->
        NullableTypeOfAnnotationMemberImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_ANNOTATION_PARAMETER) { firDiagnostic ->
        VarAnnotationParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPES_FOR_ANNOTATION_CLASS) { firDiagnostic ->
        SupertypesForAnnotationClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT) { firDiagnostic ->
        AnnotationUsedAsAnnotationArgumentImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE) { firDiagnostic ->
        IllegalKotlinVersionStringValueImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEWER_VERSION_IN_SINCE_KOTLIN) { firDiagnostic ->
        NewerVersionInSinceKotlinImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS) { firDiagnostic ->
        DeprecatedSinceKotlinWithUnorderedVersionsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS) { firDiagnostic ->
        DeprecatedSinceKotlinWithoutArgumentsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED) { firDiagnostic ->
        DeprecatedSinceKotlinWithoutDeprecatedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL) { firDiagnostic ->
        DeprecatedSinceKotlinWithDeprecatedLevelImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE) { firDiagnostic ->
        DeprecatedSinceKotlinOutsideKotlinSubpackageImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ON_SUPERCLASS.errorFactory) { firDiagnostic ->
        AnnotationOnSuperclassErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ON_SUPERCLASS.warningFactory) { firDiagnostic ->
        AnnotationOnSuperclassWarningImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION.errorFactory) { firDiagnostic ->
        RestrictedRetentionForExpressionAnnotationErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION.warningFactory) { firDiagnostic ->
        RestrictedRetentionForExpressionAnnotationWarningImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_ANNOTATION_TARGET) { firDiagnostic ->
        WrongAnnotationTargetImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET) { firDiagnostic ->
        WrongAnnotationTargetWithUseSiteTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY) { firDiagnostic ->
        InapplicableTargetOnPropertyImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE) { firDiagnostic ->
        InapplicableTargetPropertyImmutableImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE) { firDiagnostic ->
        InapplicableTargetPropertyHasNoDelegateImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD) { firDiagnostic ->
        InapplicableTargetPropertyHasNoBackingFieldImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_PARAM_TARGET) { firDiagnostic ->
        InapplicableParamTargetImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_ANNOTATION_TARGET) { firDiagnostic ->
        RedundantAnnotationTargetImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_FILE_TARGET) { firDiagnostic ->
        InapplicableFileTargetImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_API_USAGE) { firDiagnostic ->
        ExperimentalApiUsageImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_API_USAGE_ERROR) { firDiagnostic ->
        ExperimentalApiUsageErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_OVERRIDE) { firDiagnostic ->
        ExperimentalOverrideImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_OVERRIDE_ERROR) { firDiagnostic ->
        ExperimentalOverrideErrorImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_IS_NOT_ENABLED) { firDiagnostic ->
        ExperimentalIsNotEnabledImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION) { firDiagnostic ->
        ExperimentalCanOnlyBeUsedAsAnnotationImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL) { firDiagnostic ->
        ExperimentalMarkerCanOnlyBeUsedAsAnnotationOrArgumentInUseExperimentalImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USE_EXPERIMENTAL_WITHOUT_ARGUMENTS) { firDiagnostic ->
        UseExperimentalWithoutArgumentsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USE_EXPERIMENTAL_ARGUMENT_IS_NOT_MARKER) { firDiagnostic ->
        UseExperimentalArgumentIsNotMarkerImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET) { firDiagnostic ->
        ExperimentalAnnotationWithWrongTargetImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPERIMENTAL_ANNOTATION_WITH_WRONG_RETENTION) { firDiagnostic ->
        ExperimentalAnnotationWithWrongRetentionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE) { firDiagnostic ->
        ExposedTypealiasExpandedTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_FUNCTION_RETURN_TYPE) { firDiagnostic ->
        ExposedFunctionReturnTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_RECEIVER_TYPE) { firDiagnostic ->
        ExposedReceiverTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE) { firDiagnostic ->
        ExposedPropertyTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR) { firDiagnostic ->
        ExposedPropertyTypeInConstructorImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_PARAMETER_TYPE) { firDiagnostic ->
        ExposedParameterTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_INTERFACE) { firDiagnostic ->
        ExposedSuperInterfaceImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_CLASS) { firDiagnostic ->
        ExposedSuperClassImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPE_PARAMETER_BOUND) { firDiagnostic ->
        ExposedTypeParameterBoundImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_INFIX_MODIFIER) { firDiagnostic ->
        InapplicableInfixModifierImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REPEATED_MODIFIER) { firDiagnostic ->
        RepeatedModifierImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_MODIFIER) { firDiagnostic ->
        RedundantModifierImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER) { firDiagnostic ->
        DeprecatedModifierImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER_PAIR) { firDiagnostic ->
        DeprecatedModifierPairImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER_FOR_TARGET) { firDiagnostic ->
        DeprecatedModifierForTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_MODIFIER_FOR_TARGET) { firDiagnostic ->
        RedundantModifierForTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_MODIFIERS) { firDiagnostic ->
        IncompatibleModifiersImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_OPEN_IN_INTERFACE) { firDiagnostic ->
        RedundantOpenInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_MODIFIER_TARGET) { firDiagnostic ->
        WrongModifierTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPERATOR_MODIFIER_REQUIRED) { firDiagnostic ->
        OperatorModifierRequiredImpl(
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFIX_MODIFIER_REQUIRED) { firDiagnostic ->
        InfixModifierRequiredImpl(
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION) { firDiagnostic ->
        WrongModifierContainingDeclarationImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION) { firDiagnostic ->
        DeprecatedModifierContainingDeclarationImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_OPERATOR_MODIFIER) { firDiagnostic ->
        InapplicableOperatorModifierImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_NOT_TOP_LEVEL) { firDiagnostic ->
        InlineClassNotTopLevelImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_NOT_FINAL) { firDiagnostic ->
        InlineClassNotFinalImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS) { firDiagnostic ->
        AbsenceOfPrimaryConstructorForInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE) { firDiagnostic ->
        InlineClassConstructorWrongParametersSizeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER) { firDiagnostic ->
        InlineClassConstructorNotFinalReadOnlyParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS) { firDiagnostic ->
        PropertyWithBackingFieldInsideInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATED_PROPERTY_INSIDE_INLINE_CLASS) { firDiagnostic ->
        DelegatedPropertyInsideInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE) { firDiagnostic ->
        InlineClassHasInapplicableParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION) { firDiagnostic ->
        InlineClassCannotImplementInterfaceByDelegationImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CANNOT_EXTEND_CLASSES) { firDiagnostic ->
        InlineClassCannotExtendClassesImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CANNOT_BE_RECURSIVE) { firDiagnostic ->
        InlineClassCannotBeRecursiveImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESERVED_MEMBER_INSIDE_INLINE_CLASS) { firDiagnostic ->
        ReservedMemberInsideInlineClassImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS) { firDiagnostic ->
        SecondaryConstructorWithBodyInsideInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INNER_CLASS_INSIDE_INLINE_CLASS) { firDiagnostic ->
        InnerClassInsideInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_CANNOT_BE_CLONEABLE) { firDiagnostic ->
        ValueClassCannotBeCloneableImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NONE_APPLICABLE) { firDiagnostic ->
        NoneApplicableImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_CANDIDATE) { firDiagnostic ->
        InapplicableCandidateImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_MISMATCH) { firDiagnostic ->
        TypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.THROWABLE_TYPE_MISMATCH) { firDiagnostic ->
        ThrowableTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONDITION_TYPE_MISMATCH) { firDiagnostic ->
        ConditionTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ARGUMENT_TYPE_MISMATCH) { firDiagnostic ->
        ArgumentTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULL_FOR_NONNULL_TYPE) { firDiagnostic ->
        NullForNonnullTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_LATEINIT_MODIFIER) { firDiagnostic ->
        InapplicableLateinitModifierImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARARG_OUTSIDE_PARENTHESES) { firDiagnostic ->
        VarargOutsideParenthesesImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NAMED_ARGUMENTS_NOT_ALLOWED) { firDiagnostic ->
        NamedArgumentsNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_VARARG_SPREAD) { firDiagnostic ->
        NonVarargSpreadImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ARGUMENT_PASSED_TWICE) { firDiagnostic ->
        ArgumentPassedTwiceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TOO_MANY_ARGUMENTS) { firDiagnostic ->
        TooManyArgumentsImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_VALUE_FOR_PARAMETER) { firDiagnostic ->
        NoValueForParameterImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NAMED_PARAMETER_NOT_FOUND) { firDiagnostic ->
        NamedParameterNotFoundImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNMENT_TYPE_MISMATCH) { firDiagnostic ->
        AssignmentTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESULT_TYPE_MISMATCH) { firDiagnostic ->
        ResultTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_LAMBDA_EXPRESSION_ARGUMENTS) { firDiagnostic ->
        ManyLambdaExpressionArgumentsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER) { firDiagnostic ->
        NewInferenceNoInformationForParameterImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SPREAD_OF_NULLABLE) { firDiagnostic ->
        SpreadOfNullableImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION.errorFactory) { firDiagnostic ->
        AssigningSingleElementToVarargInNamedFormFunctionErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION.warningFactory) { firDiagnostic ->
        AssigningSingleElementToVarargInNamedFormFunctionWarningImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION.errorFactory) { firDiagnostic ->
        AssigningSingleElementToVarargInNamedFormAnnotationErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION.warningFactory) { firDiagnostic ->
        AssigningSingleElementToVarargInNamedFormAnnotationWarningImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERLOAD_RESOLUTION_AMBIGUITY) { firDiagnostic ->
        OverloadResolutionAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGN_OPERATOR_AMBIGUITY) { firDiagnostic ->
        AssignOperatorAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ITERATOR_AMBIGUITY) { firDiagnostic ->
        IteratorAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_FUNCTION_AMBIGUITY) { firDiagnostic ->
        HasNextFunctionAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEXT_AMBIGUITY) { firDiagnostic ->
        NextAmbiguityImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RECURSION_IN_IMPLICIT_TYPES) { firDiagnostic ->
        RecursionInImplicitTypesImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INFERENCE_ERROR) { firDiagnostic ->
        InferenceErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT) { firDiagnostic ->
        ProjectionOnNonClassTypeArgumentImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_VIOLATED) { firDiagnostic ->
        UpperBoundViolatedImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION) { firDiagnostic ->
        UpperBoundViolatedInTypealiasExpansionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED) { firDiagnostic ->
        TypeArgumentsNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS) { firDiagnostic ->
        WrongNumberOfTypeArgumentsImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_TYPE_ARGUMENTS_ON_RHS) { firDiagnostic ->
        NoTypeArgumentsOnRhsImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir as FirClass),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OUTER_CLASS_ARGUMENTS_REQUIRED) { firDiagnostic ->
        OuterClassArgumentsRequiredImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_IN_OBJECT) { firDiagnostic ->
        TypeParametersInObjectImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_PROJECTION_USAGE) { firDiagnostic ->
        IllegalProjectionUsageImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_IN_ENUM) { firDiagnostic ->
        TypeParametersInEnumImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_PROJECTION) { firDiagnostic ->
        ConflictingProjectionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION) { firDiagnostic ->
        ConflictingProjectionInTypealiasExpansionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_PROJECTION) { firDiagnostic ->
        RedundantProjectionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED) { firDiagnostic ->
        VarianceOnTypeParameterNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE) { firDiagnostic ->
        CatchParameterWithDefaultValueImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE) { firDiagnostic ->
        ReifiedTypeInCatchClauseImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE) { firDiagnostic ->
        TypeParameterInCatchClauseImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.GENERIC_THROWABLE_SUBCLASS) { firDiagnostic ->
        GenericThrowableSubclassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS) { firDiagnostic ->
        InnerClassOfGenericThrowableSubclassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE) { firDiagnostic ->
        KclassWithNullableTypeParameterInSignatureImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_AS_REIFIED) { firDiagnostic ->
        TypeParameterAsReifiedImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY.errorFactory) { firDiagnostic ->
        TypeParameterAsReifiedArrayErrorImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY.warningFactory) { firDiagnostic ->
        TypeParameterAsReifiedArrayWarningImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION) { firDiagnostic ->
        ReifiedTypeForbiddenSubstitutionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FINAL_UPPER_BOUND) { firDiagnostic ->
        FinalUpperBoundImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE) { firDiagnostic ->
        UpperBoundIsExtensionFunctionTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER) { firDiagnostic ->
        BoundsNotAllowedIfBoundedByTypeParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED) { firDiagnostic ->
        OnlyOneClassBoundAllowedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REPEATED_BOUND) { firDiagnostic ->
        RepeatedBoundImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_UPPER_BOUNDS) { firDiagnostic ->
        ConflictingUpperBoundsImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER) { firDiagnostic ->
        NameInConstraintIsNotATypeParameterImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED) { firDiagnostic ->
        BoundOnTypeAliasParameterNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_PARAMETER_NO_INLINE) { firDiagnostic ->
        ReifiedTypeParameterNoInlineImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_NOT_ALLOWED) { firDiagnostic ->
        TypeParametersNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER) { firDiagnostic ->
        TypeParameterOfPropertyNotUsedInReceiverImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH) { firDiagnostic ->
        ReturnTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.buildSymbol(firDiagnostic.c),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CYCLIC_GENERIC_UPPER_BOUND) { firDiagnostic ->
        CyclicGenericUpperBoundImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DEPRECATED_TYPE_PARAMETER_SYNTAX) { firDiagnostic ->
        DeprecatedTypeParameterSyntaxImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS) { firDiagnostic ->
        MisplacedTypeParameterConstraintsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DYNAMIC_UPPER_BOUND) { firDiagnostic ->
        DynamicUpperBoundImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_TYPES) { firDiagnostic ->
        IncompatibleTypesImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_TYPES_WARNING) { firDiagnostic ->
        IncompatibleTypesWarningImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_VARIANCE_CONFLICT) { firDiagnostic ->
        TypeVarianceConflictImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic.c,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.d),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE) { firDiagnostic ->
        TypeVarianceConflictInExpandedTypeImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic.c,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.d),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SMARTCAST_IMPOSSIBLE) { firDiagnostic ->
        SmartcastImpossibleImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b.source!!.psi as KtExpression,
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED) { firDiagnostic ->
        ExtensionInClassReferenceNotAllowedImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS) { firDiagnostic ->
        CallableReferenceLhsNotAClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR) { firDiagnostic ->
        CallableReferenceToAnnotationConstructorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CLASS_LITERAL_LHS_NOT_A_CLASS) { firDiagnostic ->
        ClassLiteralLhsNotAClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_TYPE_IN_CLASS_LITERAL_LHS) { firDiagnostic ->
        NullableTypeInClassLiteralLhsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS) { firDiagnostic ->
        ExpressionOfNullableTypeInClassLiteralLhsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOTHING_TO_OVERRIDE) { firDiagnostic ->
        NothingToOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER) { firDiagnostic ->
        CannotOverrideInvisibleMemberImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_OVERRIDE_CONFLICT) { firDiagnostic ->
        DataClassOverrideConflictImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE) { firDiagnostic ->
        CannotWeakenAccessPrivilegeImpl(
            firDiagnostic.a,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE) { firDiagnostic ->
        CannotChangeAccessPrivilegeImpl(
            firDiagnostic.a,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERRIDING_FINAL_MEMBER) { firDiagnostic ->
        OverridingFinalMemberImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH_ON_INHERITANCE) { firDiagnostic ->
        ReturnTypeMismatchOnInheritanceImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_TYPE_MISMATCH_ON_INHERITANCE) { firDiagnostic ->
        PropertyTypeMismatchOnInheritanceImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_TYPE_MISMATCH_ON_INHERITANCE) { firDiagnostic ->
        VarTypeMismatchOnInheritanceImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH_BY_DELEGATION) { firDiagnostic ->
        ReturnTypeMismatchByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_TYPE_MISMATCH_BY_DELEGATION) { firDiagnostic ->
        PropertyTypeMismatchByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION) { firDiagnostic ->
        VarOverriddenByValByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_INHERITED_MEMBERS) { firDiagnostic ->
        ConflictingInheritedMembersImpl(
            firDiagnostic.a.map { firCallableSymbol ->
                firSymbolBuilder.callableBuilder.buildCallableSymbol(firCallableSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        AbstractMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        AbstractClassMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER.errorFactory) { firDiagnostic ->
        InvisibleAbstractMemberFromSuperErrorImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER.warningFactory) { firDiagnostic ->
        InvisibleAbstractMemberFromSuperWarningImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        ManyImplMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        ManyInterfacesMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERRIDING_FINAL_MEMBER_BY_DELEGATION) { firDiagnostic ->
        OverridingFinalMemberByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE) { firDiagnostic ->
        DelegatedMemberHidesSupertypeOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        ReturnTypeMismatchOnOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        PropertyTypeMismatchOnOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        VarTypeMismatchOnOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAR_OVERRIDDEN_BY_VAL) { firDiagnostic ->
        VarOverriddenByValImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_FINAL_MEMBER_IN_FINAL_CLASS) { firDiagnostic ->
        NonFinalMemberInFinalClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_FINAL_MEMBER_IN_OBJECT) { firDiagnostic ->
        NonFinalMemberInObjectImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VIRTUAL_MEMBER_HIDDEN) { firDiagnostic ->
        VirtualMemberHiddenImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MANY_COMPANION_OBJECTS) { firDiagnostic ->
        ManyCompanionObjectsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_OVERLOADS) { firDiagnostic ->
        ConflictingOverloadsImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDECLARATION) { firDiagnostic ->
        RedeclarationImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PACKAGE_OR_CLASSIFIER_REDECLARATION) { firDiagnostic ->
        PackageOrClassifierRedeclarationImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE) { firDiagnostic ->
        MethodOfAnyImplementedInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_OBJECT_NOT_ALLOWED) { firDiagnostic ->
        LocalObjectNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LOCAL_INTERFACE_NOT_ALLOWED) { firDiagnostic ->
        LocalInterfaceNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS) { firDiagnostic ->
        AbstractFunctionInNonAbstractClassImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_FUNCTION_WITH_BODY) { firDiagnostic ->
        AbstractFunctionWithBodyImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY) { firDiagnostic ->
        NonAbstractFunctionWithNoBodyImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_FUNCTION_WITH_NO_BODY) { firDiagnostic ->
        PrivateFunctionWithNoBodyImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_MEMBER_FUNCTION_NO_BODY) { firDiagnostic ->
        NonMemberFunctionNoBodyImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUNCTION_DECLARATION_WITH_NO_NAME) { firDiagnostic ->
        FunctionDeclarationWithNoNameImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_FUNCTION_WITH_NAME) { firDiagnostic ->
        AnonymousFunctionWithNameImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE) { firDiagnostic ->
        AnonymousFunctionParameterWithDefaultValueImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_VARARG_ON_PARAMETER) { firDiagnostic ->
        UselessVarargOnParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MULTIPLE_VARARG_PARAMETERS) { firDiagnostic ->
        MultipleVarargParametersImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FORBIDDEN_VARARG_PARAMETER_TYPE) { firDiagnostic ->
        ForbiddenVarargParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION) { firDiagnostic ->
        ValueParameterWithNoTypeAnnotationImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_PARAMETER_TYPE) { firDiagnostic ->
        CannotInferParameterTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_CONSTRUCTOR_REFERENCE) { firDiagnostic ->
        FunInterfaceConstructorReferenceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS) { firDiagnostic ->
        FunInterfaceWrongCountOfAbstractMembersImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES) { firDiagnostic ->
        FunInterfaceCannotHaveAbstractPropertiesImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS) { firDiagnostic ->
        FunInterfaceAbstractMethodWithTypeParametersImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE) { firDiagnostic ->
        FunInterfaceAbstractMethodWithDefaultValueImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_WITH_SUSPEND_FUNCTION) { firDiagnostic ->
        FunInterfaceWithSuspendFunctionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS) { firDiagnostic ->
        AbstractPropertyInNonAbstractClassImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_PROPERTY_IN_INTERFACE) { firDiagnostic ->
        PrivatePropertyInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER) { firDiagnostic ->
        AbstractPropertyWithInitializerImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_INITIALIZER_IN_INTERFACE) { firDiagnostic ->
        PropertyInitializerInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER) { firDiagnostic ->
        PropertyWithNoTypeNoInitializerImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED) { firDiagnostic ->
        MustBeInitializedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT) { firDiagnostic ->
        MustBeInitializedOrBeAbstractImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT) { firDiagnostic ->
        ExtensionPropertyMustHaveAccessorsOrBeAbstractImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_LATEINIT) { firDiagnostic ->
        UnnecessaryLateinitImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.BACKING_FIELD_IN_INTERFACE) { firDiagnostic ->
        BackingFieldInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXTENSION_PROPERTY_WITH_BACKING_FIELD) { firDiagnostic ->
        ExtensionPropertyWithBackingFieldImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROPERTY_INITIALIZER_NO_BACKING_FIELD) { firDiagnostic ->
        PropertyInitializerNoBackingFieldImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_DELEGATED_PROPERTY) { firDiagnostic ->
        AbstractDelegatedPropertyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATED_PROPERTY_IN_INTERFACE) { firDiagnostic ->
        DelegatedPropertyInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_GETTER) { firDiagnostic ->
        AbstractPropertyWithGetterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_SETTER) { firDiagnostic ->
        AbstractPropertyWithSetterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY) { firDiagnostic ->
        PrivateSetterForAbstractPropertyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_SETTER_FOR_OPEN_PROPERTY) { firDiagnostic ->
        PrivateSetterForOpenPropertyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_WITH_SETTER) { firDiagnostic ->
        ValWithSetterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT) { firDiagnostic ->
        ConstValNotTopLevelOrObjectImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_GETTER) { firDiagnostic ->
        ConstValWithGetterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_DELEGATE) { firDiagnostic ->
        ConstValWithDelegateImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_CANT_BE_USED_FOR_CONST_VAL) { firDiagnostic ->
        TypeCantBeUsedForConstValImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITHOUT_INITIALIZER) { firDiagnostic ->
        ConstValWithoutInitializerImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_NON_CONST_INITIALIZER) { firDiagnostic ->
        ConstValWithNonConstInitializerImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_SETTER_PARAMETER_TYPE) { firDiagnostic ->
        WrongSetterParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INITIALIZER_TYPE_MISMATCH) { firDiagnostic ->
        InitializerTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY) { firDiagnostic ->
        GetterVisibilityDiffersFromPropertyVisibilityImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY) { firDiagnostic ->
        SetterVisibilityInconsistentWithPropertyVisibilityImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_SETTER_RETURN_TYPE) { firDiagnostic ->
        WrongSetterReturnTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_GETTER_RETURN_TYPE) { firDiagnostic ->
        WrongGetterReturnTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACCESSOR_FOR_DELEGATED_PROPERTY) { firDiagnostic ->
        AccessorForDelegatedPropertyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS) { firDiagnostic ->
        AbstractPropertyInPrimaryConstructorParametersImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_DECLARATION_WITH_BODY) { firDiagnostic ->
        ExpectedDeclarationWithBodyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL) { firDiagnostic ->
        ExpectedClassConstructorDelegationCallImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER) { firDiagnostic ->
        ExpectedClassConstructorPropertyParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_ENUM_CONSTRUCTOR) { firDiagnostic ->
        ExpectedEnumConstructorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_ENUM_ENTRY_WITH_BODY) { firDiagnostic ->
        ExpectedEnumEntryWithBodyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_PROPERTY_INITIALIZER) { firDiagnostic ->
        ExpectedPropertyInitializerImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_DELEGATED_PROPERTY) { firDiagnostic ->
        ExpectedDelegatedPropertyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_LATEINIT_PROPERTY) { firDiagnostic ->
        ExpectedLateinitPropertyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS) { firDiagnostic ->
        SupertypeInitializedInExpectedClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_PRIVATE_DECLARATION) { firDiagnostic ->
        ExpectedPrivateDeclarationImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS) { firDiagnostic ->
        ImplementationByDelegationInExpectClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_NOT_TO_CLASS) { firDiagnostic ->
        ActualTypeAliasNotToClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE) { firDiagnostic ->
        ActualTypeAliasToClassWithDeclarationSiteVarianceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE) { firDiagnostic ->
        ActualTypeAliasWithUseSiteVarianceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION) { firDiagnostic ->
        ActualTypeAliasWithComplexSubstitutionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS) { firDiagnostic ->
        ActualFunctionWithDefaultArgumentsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE) { firDiagnostic ->
        ActualAnnotationConflictingDefaultArgumentValueImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND) { firDiagnostic ->
        ExpectedFunctionSourceWithDefaultArgumentsNotFoundImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_ACTUAL_FOR_EXPECT) { firDiagnostic ->
        NoActualForExpectImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic.c.mapKeys { (incompatible, _) ->
                incompatible
            }.mapValues { (_, collection) -> 
                collection.map { firBasedSymbol ->
                                    firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
                                }
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_WITHOUT_EXPECT) { firDiagnostic ->
        ActualWithoutExpectImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic.b.mapKeys { (incompatible, _) ->
                incompatible
            }.mapValues { (_, collection) -> 
                collection.map { firBasedSymbol ->
                                    firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
                                }
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_ACTUALS) { firDiagnostic ->
        AmbiguousActualsImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.AMBIGUOUS_EXPECTS) { firDiagnostic ->
        AmbiguousExpectsImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic.b.map { firModuleData ->
                firModuleData
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS) { firDiagnostic ->
        NoActualClassMemberForExpectedClassImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic.b.map { pair ->
                firSymbolBuilder.buildSymbol(pair.first.fir) to pair.second.mapKeys { (incompatible, _) ->
                                    incompatible
                                }.mapValues { (_, collection) -> 
                                    collection.map { firBasedSymbol ->
                                                            firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
                                                        }
                                }
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ACTUAL_MISSING) { firDiagnostic ->
        ActualMissingImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION) { firDiagnostic ->
        InitializerRequiredForDestructuringDeclarationImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_MISSING) { firDiagnostic ->
        ComponentFunctionMissingImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_AMBIGUITY) { firDiagnostic ->
        ComponentFunctionAmbiguityImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_ON_NULLABLE) { firDiagnostic ->
        ComponentFunctionOnNullableImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH) { firDiagnostic ->
        ComponentFunctionReturnTypeMismatchImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_VARIABLE) { firDiagnostic ->
        UninitializedVariableImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_PARAMETER) { firDiagnostic ->
        UninitializedParameterImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_ENUM_ENTRY) { firDiagnostic ->
        UninitializedEnumEntryImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_ENUM_COMPANION) { firDiagnostic ->
        UninitializedEnumCompanionImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT) { firDiagnostic ->
        ValReassignmentImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD.errorFactory) { firDiagnostic ->
        ValReassignmentViaBackingFieldErrorImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD.warningFactory) { firDiagnostic ->
        ValReassignmentViaBackingFieldWarningImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAPTURED_VAL_INITIALIZATION) { firDiagnostic ->
        CapturedValInitializationImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION) { firDiagnostic ->
        CapturedMemberValInitializationImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SETTER_PROJECTED_OUT) { firDiagnostic ->
        SetterProjectedOutImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_INVOCATION_KIND) { firDiagnostic ->
        WrongInvocationKindImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.LEAKED_IN_PLACE_LAMBDA) { firDiagnostic ->
        LeakedInPlaceLambdaImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.WRONG_IMPLIES_CONDITION) { firDiagnostic ->
        WrongImpliesConditionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIABLE_WITH_NO_TYPE_NO_INITIALIZER) { firDiagnostic ->
        VariableWithNoTypeNoInitializerImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INITIALIZATION_BEFORE_DECLARATION) { firDiagnostic ->
        InitializationBeforeDeclarationImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNREACHABLE_CODE) { firDiagnostic ->
        UnreachableCodeImpl(
            firDiagnostic.a.map { firSourceElement ->
                (firSourceElement as FirPsiSourceElement).psi
            },
            firDiagnostic.b.map { firSourceElement ->
                (firSourceElement as FirPsiSourceElement).psi
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SENSELESS_COMPARISON) { firDiagnostic ->
        SenselessComparisonImpl(
            firDiagnostic.a.source!!.psi as KtExpression,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SENSELESS_NULL_IN_WHEN) { firDiagnostic ->
        SenselessNullInWhenImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_CALL) { firDiagnostic ->
        UnsafeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic.b?.source?.psi as? KtExpression,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL) { firDiagnostic ->
        UnsafeImplicitInvokeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_INFIX_CALL) { firDiagnostic ->
        UnsafeInfixCallImpl(
            firDiagnostic.a.source!!.psi as KtExpression,
            firDiagnostic.b,
            firDiagnostic.c.source!!.psi as KtExpression,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNSAFE_OPERATOR_CALL) { firDiagnostic ->
        UnsafeOperatorCallImpl(
            firDiagnostic.a.source!!.psi as KtExpression,
            firDiagnostic.b,
            firDiagnostic.c.source!!.psi as KtExpression,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ITERATOR_ON_NULLABLE) { firDiagnostic ->
        IteratorOnNullableImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_SAFE_CALL) { firDiagnostic ->
        UnnecessarySafeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNEXPECTED_SAFE_CALL) { firDiagnostic ->
        UnexpectedSafeCallImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_NOT_NULL_ASSERTION) { firDiagnostic ->
        UnnecessaryNotNullAssertionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION) { firDiagnostic ->
        NotNullAssertionOnLambdaExpressionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE) { firDiagnostic ->
        NotNullAssertionOnCallableReferenceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_ELVIS) { firDiagnostic ->
        UselessElvisImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_ELVIS_RIGHT_IS_NULL) { firDiagnostic ->
        UselessElvisRightIsNullImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_CAST) { firDiagnostic ->
        UselessCastImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_IS_CHECK) { firDiagnostic ->
        UselessIsCheckImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.IS_ENUM_ENTRY) { firDiagnostic ->
        IsEnumEntryImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ENUM_ENTRY_AS_TYPE) { firDiagnostic ->
        EnumEntryAsTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPECTED_CONDITION) { firDiagnostic ->
        ExpectedConditionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_ELSE_IN_WHEN) { firDiagnostic ->
        NoElseInWhenImpl(
            firDiagnostic.a.map { whenMissingCase ->
                whenMissingCase
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_EXHAUSTIVE_WHEN_STATEMENT) { firDiagnostic ->
        NonExhaustiveWhenStatementImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { whenMissingCase ->
                whenMissingCase
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVALID_IF_AS_EXPRESSION) { firDiagnostic ->
        InvalidIfAsExpressionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ELSE_MISPLACED_IN_WHEN) { firDiagnostic ->
        ElseMisplacedInWhenImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_DECLARATION_IN_WHEN_SUBJECT) { firDiagnostic ->
        IllegalDeclarationInWhenSubjectImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT) { firDiagnostic ->
        CommaInWhenConditionWithoutArgumentImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION) { firDiagnostic ->
        TypeParameterIsNotAnExpressionImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_ON_LHS_OF_DOT) { firDiagnostic ->
        TypeParameterOnLhsOfDotImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_COMPANION_OBJECT) { firDiagnostic ->
        NoCompanionObjectImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EXPRESSION_EXPECTED_PACKAGE_FOUND) { firDiagnostic ->
        ExpressionExpectedPackageFoundImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ERROR_IN_CONTRACT_DESCRIPTION) { firDiagnostic ->
        ErrorInContractDescriptionImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_GET_METHOD) { firDiagnostic ->
        NoGetMethodImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_SET_METHOD) { firDiagnostic ->
        NoSetMethodImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ITERATOR_MISSING) { firDiagnostic ->
        IteratorMissingImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_MISSING) { firDiagnostic ->
        HasNextMissingImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEXT_MISSING) { firDiagnostic ->
        NextMissingImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_FUNCTION_NONE_APPLICABLE) { firDiagnostic ->
        HasNextFunctionNoneApplicableImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NEXT_NONE_APPLICABLE) { firDiagnostic ->
        NextNoneApplicableImpl(
            firDiagnostic.a.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_MISSING) { firDiagnostic ->
        DelegateSpecialFunctionMissingImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_AMBIGUITY) { firDiagnostic ->
        DelegateSpecialFunctionAmbiguityImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE) { firDiagnostic ->
        DelegateSpecialFunctionNoneApplicableImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { firBasedSymbol ->
                firSymbolBuilder.buildSymbol(firBasedSymbol.fir)
            },
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH) { firDiagnostic ->
        DelegateSpecialFunctionReturnTypeMismatchImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNDERSCORE_IS_RESERVED) { firDiagnostic ->
        UnderscoreIsReservedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS) { firDiagnostic ->
        UnderscoreUsageWithoutBackticksImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER) { firDiagnostic ->
        ResolvedToUnderscoreNamedCatchParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVALID_CHARACTERS) { firDiagnostic ->
        InvalidCharactersImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DANGEROUS_CHARACTERS) { firDiagnostic ->
        DangerousCharactersImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EQUALITY_NOT_APPLICABLE) { firDiagnostic ->
        EqualityNotApplicableImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EQUALITY_NOT_APPLICABLE_WARNING) { firDiagnostic ->
        EqualityNotApplicableWarningImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_ENUM_COMPARISON_ERROR) { firDiagnostic ->
        IncompatibleEnumComparisonErrorImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INC_DEC_SHOULD_NOT_RETURN_UNIT) { firDiagnostic ->
        IncDecShouldNotReturnUnitImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT) { firDiagnostic ->
        AssignmentOperatorShouldReturnUnitImpl(
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TOPLEVEL_TYPEALIASES_ONLY) { firDiagnostic ->
        ToplevelTypealiasesOnlyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RECURSIVE_TYPEALIAS_EXPANSION) { firDiagnostic ->
        RecursiveTypealiasExpansionImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.TYPEALIAS_SHOULD_EXPAND_TO_CLASS) { firDiagnostic ->
        TypealiasShouldExpandToClassImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_VISIBILITY_MODIFIER) { firDiagnostic ->
        RedundantVisibilityModifierImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_MODALITY_MODIFIER) { firDiagnostic ->
        RedundantModalityModifierImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_RETURN_UNIT_TYPE) { firDiagnostic ->
        RedundantReturnUnitTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_EXPLICIT_TYPE) { firDiagnostic ->
        RedundantExplicitTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE) { firDiagnostic ->
        RedundantSingleExpressionStringTemplateImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAN_BE_VAL) { firDiagnostic ->
        CanBeValImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT) { firDiagnostic ->
        CanBeReplacedWithOperatorAssignmentImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_CALL_OF_CONVERSION_METHOD) { firDiagnostic ->
        RedundantCallOfConversionMethodImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS) { firDiagnostic ->
        ArrayEqualityOperatorCanBeReplacedWithEqualsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.EMPTY_RANGE) { firDiagnostic ->
        EmptyRangeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_SETTER_PARAMETER_TYPE) { firDiagnostic ->
        RedundantSetterParameterTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.UNUSED_VARIABLE) { firDiagnostic ->
        UnusedVariableImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ASSIGNED_VALUE_IS_NEVER_READ) { firDiagnostic ->
        AssignedValueIsNeverReadImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT) { firDiagnostic ->
        VariableInitializerIsRedundantImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.VARIABLE_NEVER_READ) { firDiagnostic ->
        VariableNeverReadImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USELESS_CALL_ON_NOT_NULL) { firDiagnostic ->
        UselessCallOnNotNullImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_NOT_ALLOWED) { firDiagnostic ->
        ReturnNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY) { firDiagnostic ->
        ReturnInFunctionWithExpressionBodyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY) { firDiagnostic ->
        NoReturnInFunctionWithBlockBodyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_INITIALIZER_IN_INTERFACE) { firDiagnostic ->
        AnonymousInitializerInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.USAGE_IS_NOT_INLINABLE) { firDiagnostic ->
        UsageIsNotInlinableImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED) { firDiagnostic ->
        NonLocalReturnNotAllowedImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOT_YET_SUPPORTED_IN_INLINE) { firDiagnostic ->
        NotYetSupportedInInlineImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NOTHING_TO_INLINE) { firDiagnostic ->
        NothingToInlineImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NULLABLE_INLINE_PARAMETER) { firDiagnostic ->
        NullableInlineParameterImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RECURSION_IN_INLINE) { firDiagnostic ->
        RecursionInInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        NonPublicCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        ProtectedConstructorCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR) { firDiagnostic ->
        ProtectedCallFromPublicInlineErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        ProtectedCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PRIVATE_CLASS_MEMBER_FROM_INLINE) { firDiagnostic ->
        PrivateClassMemberFromInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.SUPER_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        SuperCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.DECLARATION_CANT_BE_INLINED) { firDiagnostic ->
        DeclarationCantBeInlinedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OVERRIDE_BY_INLINE) { firDiagnostic ->
        OverrideByInlineImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_INTERNAL_PUBLISHED_API) { firDiagnostic ->
        NonInternalPublishedApiImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE) { firDiagnostic ->
        InvalidDefaultFunctionalParameterForInlineImpl(
            firDiagnostic.a.source!!.psi as KtExpression,
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_PARAMETER_IN_OVERRIDE) { firDiagnostic ->
        ReifiedTypeParameterInOverrideImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_PROPERTY_WITH_BACKING_FIELD) { firDiagnostic ->
        InlinePropertyWithBackingFieldImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_INLINE_PARAMETER_MODIFIER) { firDiagnostic ->
        IllegalInlineParameterModifierImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED) { firDiagnostic ->
        InlineSuspendFunctionTypeUnsupportedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE) { firDiagnostic ->
        RedundantInlineSuspendFunctionTypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON) { firDiagnostic ->
        CannotAllUnderImportFromSingletonImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.PACKAGE_CANNOT_BE_IMPORTED) { firDiagnostic ->
        PackageCannotBeImportedImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CANNOT_BE_IMPORTED) { firDiagnostic ->
        CannotBeImportedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.CONFLICTING_IMPORT) { firDiagnostic ->
        ConflictingImportImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.OPERATOR_RENAMED_ON_IMPORT) { firDiagnostic ->
        OperatorRenamedOnImportImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_SUSPEND_FUNCTION_CALL) { firDiagnostic ->
        IllegalSuspendFunctionCallImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_SUSPEND_PROPERTY_ACCESS) { firDiagnostic ->
        IllegalSuspendPropertyAccessImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_LOCAL_SUSPENSION_POINT) { firDiagnostic ->
        NonLocalSuspensionPointImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL) { firDiagnostic ->
        IllegalRestrictedSuspendingFunctionCallImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND) { firDiagnostic ->
        NonModifierFormForBuiltInSuspendImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND) { firDiagnostic ->
        ModifierFormForNonBuiltInSuspendImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirErrors.RETURN_FOR_BUILT_IN_SUSPEND) { firDiagnostic ->
        ReturnForBuiltInSuspendImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.CONFLICTING_JVM_DECLARATIONS) { firDiagnostic ->
        ConflictingJvmDeclarationsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JAVA_TYPE_MISMATCH) { firDiagnostic ->
        JavaTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.UPPER_BOUND_CANNOT_BE_ARRAY) { firDiagnostic ->
        UpperBoundCannotBeArrayImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.STRICTFP_ON_CLASS) { firDiagnostic ->
        StrictfpOnClassImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.VOLATILE_ON_VALUE) { firDiagnostic ->
        VolatileOnValueImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.VOLATILE_ON_DELEGATE) { firDiagnostic ->
        VolatileOnDelegateImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_ON_ABSTRACT) { firDiagnostic ->
        SynchronizedOnAbstractImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_IN_INTERFACE) { firDiagnostic ->
        SynchronizedInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.SYNCHRONIZED_ON_INLINE) { firDiagnostic ->
        SynchronizedOnInlineImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS) { firDiagnostic ->
        OverloadsWithoutDefaultArgumentsImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_ABSTRACT) { firDiagnostic ->
        OverloadsAbstractImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_INTERFACE) { firDiagnostic ->
        OverloadsInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_LOCAL) { firDiagnostic ->
        OverloadsLocalImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR.errorFactory) { firDiagnostic ->
        OverloadsAnnotationClassConstructorErrorImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR.warningFactory) { firDiagnostic ->
        OverloadsAnnotationClassConstructorWarningImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.OVERLOADS_PRIVATE) { firDiagnostic ->
        OverloadsPrivateImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.DEPRECATED_JAVA_ANNOTATION) { firDiagnostic ->
        DeprecatedJavaAnnotationImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_PACKAGE_NAME_CANNOT_BE_EMPTY) { firDiagnostic ->
        JvmPackageNameCannotBeEmptyImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_PACKAGE_NAME_MUST_BE_VALID_NAME) { firDiagnostic ->
        JvmPackageNameMustBeValidNameImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES) { firDiagnostic ->
        JvmPackageNameNotSupportedInFilesWithClassesImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.LOCAL_JVM_RECORD) { firDiagnostic ->
        LocalJvmRecordImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NON_FINAL_JVM_RECORD) { firDiagnostic ->
        NonFinalJvmRecordImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.ENUM_JVM_RECORD) { firDiagnostic ->
        EnumJvmRecordImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS) { firDiagnostic ->
        JvmRecordWithoutPrimaryConstructorParametersImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.NON_DATA_CLASS_JVM_RECORD) { firDiagnostic ->
        NonDataClassJvmRecordImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_RECORD_NOT_VAL_PARAMETER) { firDiagnostic ->
        JvmRecordNotValParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_RECORD_NOT_LAST_VARARG_PARAMETER) { firDiagnostic ->
        JvmRecordNotLastVarargParameterImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.INNER_JVM_RECORD) { firDiagnostic ->
        InnerJvmRecordImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.FIELD_IN_JVM_RECORD) { firDiagnostic ->
        FieldInJvmRecordImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.DELEGATION_BY_IN_JVM_RECORD) { firDiagnostic ->
        DelegationByInJvmRecordImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.JVM_RECORD_EXTENDS_CLASS) { firDiagnostic ->
        JvmRecordExtendsClassImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
    add(FirJvmErrors.ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE) { firDiagnostic ->
        IllegalJavaLangRecordSupertypeImpl(
            firDiagnostic as FirPsiDiagnostic,
            token,
        )
    }
}
