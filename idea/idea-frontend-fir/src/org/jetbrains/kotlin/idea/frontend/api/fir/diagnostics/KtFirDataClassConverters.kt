/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtValueArgument
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
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNSUPPORTED_FEATURE) { firDiagnostic ->
        UnsupportedFeatureImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SYNTAX) { firDiagnostic ->
        SyntaxImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.OTHER_ERROR) { firDiagnostic ->
        OtherErrorImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ILLEGAL_CONST_EXPRESSION) { firDiagnostic ->
        IllegalConstExpressionImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ILLEGAL_UNDERSCORE) { firDiagnostic ->
        IllegalUnderscoreImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPRESSION_EXPECTED) { firDiagnostic ->
        ExpressionExpectedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ASSIGNMENT_IN_EXPRESSION_CONTEXT) { firDiagnostic ->
        AssignmentInExpressionContextImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP) { firDiagnostic ->
        BreakOrContinueOutsideALoopImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NOT_A_LOOP_LABEL) { firDiagnostic ->
        NotALoopLabelImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VARIABLE_EXPECTED) { firDiagnostic ->
        VariableExpectedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATION_IN_INTERFACE) { firDiagnostic ->
        DelegationInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NESTED_CLASS_NOT_ALLOWED) { firDiagnostic ->
        NestedClassNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INCORRECT_CHARACTER_LITERAL) { firDiagnostic ->
        IncorrectCharacterLiteralImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EMPTY_CHARACTER_LITERAL) { firDiagnostic ->
        EmptyCharacterLiteralImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL) { firDiagnostic ->
        TooManyCharactersInCharacterLiteralImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ILLEGAL_ESCAPE) { firDiagnostic ->
        IllegalEscapeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INT_LITERAL_OUT_OF_RANGE) { firDiagnostic ->
        IntLiteralOutOfRangeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FLOAT_LITERAL_OUT_OF_RANGE) { firDiagnostic ->
        FloatLiteralOutOfRangeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.WRONG_LONG_SUFFIX) { firDiagnostic ->
        WrongLongSuffixImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DIVISION_BY_ZERO) { firDiagnostic ->
        DivisionByZeroImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INVISIBLE_REFERENCE) { firDiagnostic ->
        InvisibleReferenceImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_REFERENCE) { firDiagnostic ->
        UnresolvedReferenceImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNRESOLVED_LABEL) { firDiagnostic ->
        UnresolvedLabelImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DESERIALIZATION_ERROR) { firDiagnostic ->
        DeserializationErrorImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ERROR_FROM_JAVA_RESOLUTION) { firDiagnostic ->
        ErrorFromJavaResolutionImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNKNOWN_CALLABLE_KIND) { firDiagnostic ->
        UnknownCallableKindImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MISSING_STDLIB_CLASS) { firDiagnostic ->
        MissingStdlibClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NO_THIS) { firDiagnostic ->
        NoThisImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS) { firDiagnostic ->
        CreatingAnInstanceOfAbstractClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPER_IS_NOT_AN_EXPRESSION) { firDiagnostic ->
        SuperIsNotAnExpressionImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPER_NOT_AVAILABLE) { firDiagnostic ->
        SuperNotAvailableImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_SUPER_CALL) { firDiagnostic ->
        AbstractSuperCallImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL) { firDiagnostic ->
        InstanceAccessBeforeSuperCallImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ENUM_AS_SUPERTYPE) { firDiagnostic ->
        EnumAsSupertypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.RECURSION_IN_SUPERTYPES) { firDiagnostic ->
        RecursionInSupertypesImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NOT_A_SUPERTYPE) { firDiagnostic ->
        NotASupertypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE) { firDiagnostic ->
        SuperclassNotAccessibleFromInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE) { firDiagnostic ->
        QualifiedSupertypeExtendedByOtherSupertypeImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_INITIALIZED_IN_INTERFACE) { firDiagnostic ->
        SupertypeInitializedInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INTERFACE_WITH_SUPERCLASS) { firDiagnostic ->
        InterfaceWithSuperclassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CLASS_IN_SUPERTYPE_FOR_ENUM) { firDiagnostic ->
        ClassInSupertypeForEnumImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SEALED_SUPERTYPE) { firDiagnostic ->
        SealedSupertypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SEALED_SUPERTYPE_IN_LOCAL_CLASS) { firDiagnostic ->
        SealedSupertypeInLocalClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE) { firDiagnostic ->
        SupertypeNotAClassOrInterfaceImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONSTRUCTOR_IN_OBJECT) { firDiagnostic ->
        ConstructorInObjectImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONSTRUCTOR_IN_INTERFACE) { firDiagnostic ->
        ConstructorInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_PRIVATE_CONSTRUCTOR_IN_ENUM) { firDiagnostic ->
        NonPrivateConstructorInEnumImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED) { firDiagnostic ->
        NonPrivateOrProtectedConstructorInSealedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CYCLIC_CONSTRUCTOR_DELEGATION_CALL) { firDiagnostic ->
        CyclicConstructorDelegationCallImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED) { firDiagnostic ->
        PrimaryConstructorDelegationCallExpectedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_NOT_INITIALIZED) { firDiagnostic ->
        SupertypeNotInitializedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR) { firDiagnostic ->
        SupertypeInitializedWithoutPrimaryConstructorImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR) { firDiagnostic ->
        DelegationSuperCallInEnumConstructorImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS) { firDiagnostic ->
        PrimaryConstructorRequiredForDataClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPLICIT_DELEGATION_CALL_REQUIRED) { firDiagnostic ->
        ExplicitDelegationCallRequiredImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SEALED_CLASS_CONSTRUCTOR_CALL) { firDiagnostic ->
        SealedClassConstructorCallImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_WITHOUT_PARAMETERS) { firDiagnostic ->
        DataClassWithoutParametersImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_VARARG_PARAMETER) { firDiagnostic ->
        DataClassVarargParameterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DATA_CLASS_NOT_PROPERTY_PARAMETER) { firDiagnostic ->
        DataClassNotPropertyParameterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR) { firDiagnostic ->
        AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST) { firDiagnostic ->
        AnnotationArgumentMustBeConstImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST) { firDiagnostic ->
        AnnotationArgumentMustBeEnumConstImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL) { firDiagnostic ->
        AnnotationArgumentMustBeKclassLiteralImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANNOTATION_CLASS_MEMBER) { firDiagnostic ->
        AnnotationClassMemberImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT) { firDiagnostic ->
        AnnotationParameterDefaultValueMustBeConstantImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER) { firDiagnostic ->
        InvalidTypeOfAnnotationMemberImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.LOCAL_ANNOTATION_CLASS_ERROR) { firDiagnostic ->
        LocalAnnotationClassErrorImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER) { firDiagnostic ->
        MissingValOnAnnotationParameterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION) { firDiagnostic ->
        NonConstValUsedInConstantExpressionImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NOT_AN_ANNOTATION_CLASS) { firDiagnostic ->
        NotAnAnnotationClassImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER) { firDiagnostic ->
        NullableTypeOfAnnotationMemberImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VAR_ANNOTATION_PARAMETER) { firDiagnostic ->
        VarAnnotationParameterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPERTYPES_FOR_ANNOTATION_CLASS) { firDiagnostic ->
        SupertypesForAnnotationClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT) { firDiagnostic ->
        AnnotationUsedAsAnnotationArgumentImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE) { firDiagnostic ->
        ExposedTypealiasExpandedTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_FUNCTION_RETURN_TYPE) { firDiagnostic ->
        ExposedFunctionReturnTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_RECEIVER_TYPE) { firDiagnostic ->
        ExposedReceiverTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE) { firDiagnostic ->
        ExposedPropertyTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR) { firDiagnostic ->
        ExposedPropertyTypeInConstructorImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_PARAMETER_TYPE) { firDiagnostic ->
        ExposedParameterTypeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_INTERFACE) { firDiagnostic ->
        ExposedSuperInterfaceImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_CLASS) { firDiagnostic ->
        ExposedSuperClassImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPE_PARAMETER_BOUND) { firDiagnostic ->
        ExposedTypeParameterBoundImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_INFIX_MODIFIER) { firDiagnostic ->
        InapplicableInfixModifierImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REPEATED_MODIFIER) { firDiagnostic ->
        RepeatedModifierImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_MODIFIER) { firDiagnostic ->
        RedundantModifierImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DEPRECATED_MODIFIER_PAIR) { firDiagnostic ->
        DeprecatedModifierPairImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INCOMPATIBLE_MODIFIERS) { firDiagnostic ->
        IncompatibleModifiersImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_OPEN_IN_INTERFACE) { firDiagnostic ->
        RedundantOpenInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.WRONG_MODIFIER_TARGET) { firDiagnostic ->
        WrongModifierTargetImpl(
            firDiagnostic.a,
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.OPERATOR_MODIFIER_REQUIRED) { firDiagnostic ->
        OperatorModifierRequiredImpl(
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firDiagnostic.a.fir),
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_NOT_TOP_LEVEL) { firDiagnostic ->
        InlineClassNotTopLevelImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_NOT_FINAL) { firDiagnostic ->
        InlineClassNotFinalImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS) { firDiagnostic ->
        AbsenceOfPrimaryConstructorForInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE) { firDiagnostic ->
        InlineClassConstructorWrongParametersSizeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER) { firDiagnostic ->
        InlineClassConstructorNotFinalReadOnlyParameterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS) { firDiagnostic ->
        PropertyWithBackingFieldInsideInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATED_PROPERTY_INSIDE_INLINE_CLASS) { firDiagnostic ->
        DelegatedPropertyInsideInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE) { firDiagnostic ->
        InlineClassHasInapplicableParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION) { firDiagnostic ->
        InlineClassCannotImplementInterfaceByDelegationImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CANNOT_EXTEND_CLASSES) { firDiagnostic ->
        InlineClassCannotExtendClassesImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INLINE_CLASS_CANNOT_BE_RECURSIVE) { firDiagnostic ->
        InlineClassCannotBeRecursiveImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.RESERVED_MEMBER_INSIDE_INLINE_CLASS) { firDiagnostic ->
        ReservedMemberInsideInlineClassImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS) { firDiagnostic ->
        SecondaryConstructorWithBodyInsideInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INNER_CLASS_INSIDE_INLINE_CLASS) { firDiagnostic ->
        InnerClassInsideInlineClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VALUE_CLASS_CANNOT_BE_CLONEABLE) { firDiagnostic ->
        ValueClassCannotBeCloneableImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NONE_APPLICABLE) { firDiagnostic ->
        NoneApplicableImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_CANDIDATE) { firDiagnostic ->
        InapplicableCandidateImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ARGUMENT_TYPE_MISMATCH) { firDiagnostic ->
        ArgumentTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NULL_FOR_NONNULL_TYPE) { firDiagnostic ->
        NullForNonnullTypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INAPPLICABLE_LATEINIT_MODIFIER) { firDiagnostic ->
        InapplicableLateinitModifierImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VARARG_OUTSIDE_PARENTHESES) { firDiagnostic ->
        VarargOutsideParenthesesImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NAMED_ARGUMENTS_NOT_ALLOWED) { firDiagnostic ->
        NamedArgumentsNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_VARARG_SPREAD) { firDiagnostic ->
        NonVarargSpreadImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ARGUMENT_PASSED_TWICE) { firDiagnostic ->
        ArgumentPassedTwiceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TOO_MANY_ARGUMENTS) { firDiagnostic ->
        TooManyArgumentsImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NO_VALUE_FOR_PARAMETER) { firDiagnostic ->
        NoValueForParameterImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NAMED_PARAMETER_NOT_FOUND) { firDiagnostic ->
        NamedParameterNotFoundImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MANY_LAMBDA_EXPRESSION_ARGUMENTS) { firDiagnostic ->
        ManyLambdaExpressionArgumentsImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.OVERLOAD_RESOLUTION_AMBIGUITY) { firDiagnostic ->
        OverloadResolutionAmbiguityImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ASSIGN_OPERATOR_AMBIGUITY) { firDiagnostic ->
        AssignOperatorAmbiguityImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ITERATOR_AMBIGUITY) { firDiagnostic ->
        IteratorAmbiguityImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_FUNCTION_AMBIGUITY) { firDiagnostic ->
        HasNextFunctionAmbiguityImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NEXT_AMBIGUITY) { firDiagnostic ->
        NextAmbiguityImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_MISMATCH) { firDiagnostic ->
        TypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.RECURSION_IN_IMPLICIT_TYPES) { firDiagnostic ->
        RecursionInImplicitTypesImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INFERENCE_ERROR) { firDiagnostic ->
        InferenceErrorImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT) { firDiagnostic ->
        ProjectionOnNonClassTypeArgumentImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_VIOLATED) { firDiagnostic ->
        UpperBoundViolatedImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED) { firDiagnostic ->
        TypeArgumentsNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS) { firDiagnostic ->
        WrongNumberOfTypeArgumentsImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir as FirClass<*>),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NO_TYPE_ARGUMENTS_ON_RHS) { firDiagnostic ->
        NoTypeArgumentsOnRhsImpl(
            firDiagnostic.a,
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.b.fir as FirClass<*>),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_IN_OBJECT) { firDiagnostic ->
        TypeParametersInObjectImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ILLEGAL_PROJECTION_USAGE) { firDiagnostic ->
        IllegalProjectionUsageImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_IN_ENUM) { firDiagnostic ->
        TypeParametersInEnumImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONFLICTING_PROJECTION) { firDiagnostic ->
        ConflictingProjectionImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED) { firDiagnostic ->
        VarianceOnTypeParameterNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE) { firDiagnostic ->
        CatchParameterWithDefaultValueImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE) { firDiagnostic ->
        ReifiedTypeInCatchClauseImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE) { firDiagnostic ->
        TypeParameterInCatchClauseImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.GENERIC_THROWABLE_SUBCLASS) { firDiagnostic ->
        GenericThrowableSubclassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS) { firDiagnostic ->
        InnerClassOfGenericThrowableSubclassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE) { firDiagnostic ->
        KclassWithNullableTypeParameterInSignatureImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_AS_REIFIED) { firDiagnostic ->
        TypeParameterAsReifiedImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FINAL_UPPER_BOUND) { firDiagnostic ->
        FinalUpperBoundImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE) { firDiagnostic ->
        UpperBoundIsExtensionFunctionTypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER) { firDiagnostic ->
        BoundsNotAllowedIfBoundedByTypeParameterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED) { firDiagnostic ->
        OnlyOneClassBoundAllowedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REPEATED_BOUND) { firDiagnostic ->
        RepeatedBoundImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONFLICTING_UPPER_BOUNDS) { firDiagnostic ->
        ConflictingUpperBoundsImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER) { firDiagnostic ->
        NameInConstraintIsNotATypeParameterImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildSymbol(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED) { firDiagnostic ->
        BoundOnTypeAliasParameterNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REIFIED_TYPE_PARAMETER_NO_INLINE) { firDiagnostic ->
        ReifiedTypeParameterNoInlineImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETERS_NOT_ALLOWED) { firDiagnostic ->
        TypeParametersNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER) { firDiagnostic ->
        TypeParameterOfPropertyNotUsedInReceiverImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH) { firDiagnostic ->
        ReturnTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CYCLIC_GENERIC_UPPER_BOUND) { firDiagnostic ->
        CyclicGenericUpperBoundImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DEPRECATED_TYPE_PARAMETER_SYNTAX) { firDiagnostic ->
        DeprecatedTypeParameterSyntaxImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS) { firDiagnostic ->
        MisplacedTypeParameterConstraintsImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DYNAMIC_UPPER_BOUND) { firDiagnostic ->
        DynamicUpperBoundImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED) { firDiagnostic ->
        ExtensionInClassReferenceNotAllowedImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS) { firDiagnostic ->
        CallableReferenceLhsNotAClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR) { firDiagnostic ->
        CallableReferenceToAnnotationConstructorImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CLASS_LITERAL_LHS_NOT_A_CLASS) { firDiagnostic ->
        ClassLiteralLhsNotAClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NULLABLE_TYPE_IN_CLASS_LITERAL_LHS) { firDiagnostic ->
        NullableTypeInClassLiteralLhsImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS) { firDiagnostic ->
        ExpressionOfNullableTypeInClassLiteralLhsImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NOTHING_TO_OVERRIDE) { firDiagnostic ->
        NothingToOverrideImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE) { firDiagnostic ->
        CannotWeakenAccessPrivilegeImpl(
            firDiagnostic.a,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE) { firDiagnostic ->
        CannotChangeAccessPrivilegeImpl(
            firDiagnostic.a,
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.OVERRIDING_FINAL_MEMBER) { firDiagnostic ->
        OverridingFinalMemberImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a as FirCallableDeclaration),
            firDiagnostic.b,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        AbstractMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        AbstractClassMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER) { firDiagnostic ->
        InvisibleAbstractMemberFromSuperImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING) { firDiagnostic ->
        InvisibleAbstractMemberFromSuperWarningImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        ManyImplMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED) { firDiagnostic ->
        ManyInterfacesMemberNotImplementedImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.OVERRIDING_FINAL_MEMBER_BY_DELEGATION) { firDiagnostic ->
        OverridingFinalMemberByDelegationImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a as FirCallableDeclaration),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE) { firDiagnostic ->
        DelegatedMemberHidesSupertypeOverrideImpl(
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.a as FirCallableDeclaration),
            firSymbolBuilder.callableBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        ReturnTypeMismatchOnOverrideImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        PropertyTypeMismatchOnOverrideImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE) { firDiagnostic ->
        VarTypeMismatchOnOverrideImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VAR_OVERRIDDEN_BY_VAL) { firDiagnostic ->
        VarOverriddenByValImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_FINAL_MEMBER_IN_FINAL_CLASS) { firDiagnostic ->
        NonFinalMemberInFinalClassImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_FINAL_MEMBER_IN_OBJECT) { firDiagnostic ->
        NonFinalMemberInObjectImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MANY_COMPANION_OBJECTS) { firDiagnostic ->
        ManyCompanionObjectsImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONFLICTING_OVERLOADS) { firDiagnostic ->
        ConflictingOverloadsImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDECLARATION) { firDiagnostic ->
        RedeclarationImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE) { firDiagnostic ->
        MethodOfAnyImplementedInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.LOCAL_OBJECT_NOT_ALLOWED) { firDiagnostic ->
        LocalObjectNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.LOCAL_INTERFACE_NOT_ALLOWED) { firDiagnostic ->
        LocalInterfaceNotAllowedImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS) { firDiagnostic ->
        AbstractFunctionInNonAbstractClassImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_FUNCTION_WITH_BODY) { firDiagnostic ->
        AbstractFunctionWithBodyImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY) { firDiagnostic ->
        NonAbstractFunctionWithNoBodyImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PRIVATE_FUNCTION_WITH_NO_BODY) { firDiagnostic ->
        PrivateFunctionWithNoBodyImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_MEMBER_FUNCTION_NO_BODY) { firDiagnostic ->
        NonMemberFunctionNoBodyImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FUNCTION_DECLARATION_WITH_NO_NAME) { firDiagnostic ->
        FunctionDeclarationWithNoNameImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_FUNCTION_WITH_NAME) { firDiagnostic ->
        AnonymousFunctionWithNameImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE) { firDiagnostic ->
        AnonymousFunctionParameterWithDefaultValueImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.USELESS_VARARG_ON_PARAMETER) { firDiagnostic ->
        UselessVarargOnParameterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MULTIPLE_VARARG_PARAMETERS) { firDiagnostic ->
        MultipleVarargParametersImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FORBIDDEN_VARARG_PARAMETER_TYPE) { firDiagnostic ->
        ForbiddenVarargParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION) { firDiagnostic ->
        ValueParameterWithNoTypeAnnotationImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CANNOT_INFER_PARAMETER_TYPE) { firDiagnostic ->
        CannotInferParameterTypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_CONSTRUCTOR_REFERENCE) { firDiagnostic ->
        FunInterfaceConstructorReferenceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS) { firDiagnostic ->
        FunInterfaceWrongCountOfAbstractMembersImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES) { firDiagnostic ->
        FunInterfaceCannotHaveAbstractPropertiesImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS) { firDiagnostic ->
        FunInterfaceAbstractMethodWithTypeParametersImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE) { firDiagnostic ->
        FunInterfaceAbstractMethodWithDefaultValueImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.FUN_INTERFACE_WITH_SUSPEND_FUNCTION) { firDiagnostic ->
        FunInterfaceWithSuspendFunctionImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS) { firDiagnostic ->
        AbstractPropertyInNonAbstractClassImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PRIVATE_PROPERTY_IN_INTERFACE) { firDiagnostic ->
        PrivatePropertyInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER) { firDiagnostic ->
        AbstractPropertyWithInitializerImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROPERTY_INITIALIZER_IN_INTERFACE) { firDiagnostic ->
        PropertyInitializerInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER) { firDiagnostic ->
        PropertyWithNoTypeNoInitializerImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED) { firDiagnostic ->
        MustBeInitializedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT) { firDiagnostic ->
        MustBeInitializedOrBeAbstractImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT) { firDiagnostic ->
        ExtensionPropertyMustHaveAccessorsOrBeAbstractImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_LATEINIT) { firDiagnostic ->
        UnnecessaryLateinitImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.BACKING_FIELD_IN_INTERFACE) { firDiagnostic ->
        BackingFieldInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXTENSION_PROPERTY_WITH_BACKING_FIELD) { firDiagnostic ->
        ExtensionPropertyWithBackingFieldImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROPERTY_INITIALIZER_NO_BACKING_FIELD) { firDiagnostic ->
        PropertyInitializerNoBackingFieldImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_DELEGATED_PROPERTY) { firDiagnostic ->
        AbstractDelegatedPropertyImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATED_PROPERTY_IN_INTERFACE) { firDiagnostic ->
        DelegatedPropertyInInterfaceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_GETTER) { firDiagnostic ->
        AbstractPropertyWithGetterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ABSTRACT_PROPERTY_WITH_SETTER) { firDiagnostic ->
        AbstractPropertyWithSetterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY) { firDiagnostic ->
        PrivateSetterForAbstractPropertyImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PRIVATE_SETTER_FOR_OPEN_PROPERTY) { firDiagnostic ->
        PrivateSetterForOpenPropertyImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPECTED_PRIVATE_DECLARATION) { firDiagnostic ->
        ExpectedPrivateDeclarationImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VAL_WITH_SETTER) { firDiagnostic ->
        ValWithSetterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT) { firDiagnostic ->
        ConstValNotTopLevelOrObjectImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_GETTER) { firDiagnostic ->
        ConstValWithGetterImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_DELEGATE) { firDiagnostic ->
        ConstValWithDelegateImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_CANT_BE_USED_FOR_CONST_VAL) { firDiagnostic ->
        TypeCantBeUsedForConstValImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITHOUT_INITIALIZER) { firDiagnostic ->
        ConstValWithoutInitializerImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONST_VAL_WITH_NON_CONST_INITIALIZER) { firDiagnostic ->
        ConstValWithNonConstInitializerImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.WRONG_SETTER_PARAMETER_TYPE) { firDiagnostic ->
        WrongSetterParameterTypeImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INITIALIZER_TYPE_MISMATCH) { firDiagnostic ->
        InitializerTypeMismatchImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY) { firDiagnostic ->
        GetterVisibilityDiffersFromPropertyVisibilityImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.WRONG_SETTER_RETURN_TYPE) { firDiagnostic ->
        WrongSetterReturnTypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPECTED_DECLARATION_WITH_BODY) { firDiagnostic ->
        ExpectedDeclarationWithBodyImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPECTED_PROPERTY_INITIALIZER) { firDiagnostic ->
        ExpectedPropertyInitializerImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPECTED_DELEGATED_PROPERTY) { firDiagnostic ->
        ExpectedDelegatedPropertyImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPECTED_LATEINIT_PROPERTY) { firDiagnostic ->
        ExpectedLateinitPropertyImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION) { firDiagnostic ->
        InitializerRequiredForDestructuringDeclarationImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_MISSING) { firDiagnostic ->
        ComponentFunctionMissingImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_AMBIGUITY) { firDiagnostic ->
        ComponentFunctionAmbiguityImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_ON_NULLABLE) { firDiagnostic ->
        ComponentFunctionOnNullableImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH) { firDiagnostic ->
        ComponentFunctionReturnTypeMismatchImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_VARIABLE) { firDiagnostic ->
        UninitializedVariableImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_ENUM_ENTRY) { firDiagnostic ->
        UninitializedEnumEntryImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNINITIALIZED_ENUM_COMPANION) { firDiagnostic ->
        UninitializedEnumCompanionImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT) { firDiagnostic ->
        ValReassignmentImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD) { firDiagnostic ->
        ValReassignmentViaBackingFieldImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR) { firDiagnostic ->
        ValReassignmentViaBackingFieldErrorImpl(
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.WRONG_INVOCATION_KIND) { firDiagnostic ->
        WrongInvocationKindImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firDiagnostic.b,
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.LEAKED_IN_PLACE_LAMBDA) { firDiagnostic ->
        LeakedInPlaceLambdaImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.WRONG_IMPLIES_CONDITION) { firDiagnostic ->
        WrongImpliesConditionImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNSAFE_CALL) { firDiagnostic ->
        UnsafeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL) { firDiagnostic ->
        UnsafeImplicitInvokeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNSAFE_INFIX_CALL) { firDiagnostic ->
        UnsafeInfixCallImpl(
            firDiagnostic.a.source!!.psi as KtExpression,
            firDiagnostic.b,
            firDiagnostic.c.source!!.psi as KtExpression,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNSAFE_OPERATOR_CALL) { firDiagnostic ->
        UnsafeOperatorCallImpl(
            firDiagnostic.a.source!!.psi as KtExpression,
            firDiagnostic.b,
            firDiagnostic.c.source!!.psi as KtExpression,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ITERATOR_ON_NULLABLE) { firDiagnostic ->
        IteratorOnNullableImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_SAFE_CALL) { firDiagnostic ->
        UnnecessarySafeCallImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNEXPECTED_SAFE_CALL) { firDiagnostic ->
        UnexpectedSafeCallImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNNECESSARY_NOT_NULL_ASSERTION) { firDiagnostic ->
        UnnecessaryNotNullAssertionImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION) { firDiagnostic ->
        NotNullAssertionOnLambdaExpressionImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE) { firDiagnostic ->
        NotNullAssertionOnCallableReferenceImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.USELESS_ELVIS) { firDiagnostic ->
        UselessElvisImpl(
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.USELESS_ELVIS_RIGHT_IS_NULL) { firDiagnostic ->
        UselessElvisRightIsNullImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NO_ELSE_IN_WHEN) { firDiagnostic ->
        NoElseInWhenImpl(
            firDiagnostic.a.map { whenMissingCase ->
                whenMissingCase
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.INVALID_IF_AS_EXPRESSION) { firDiagnostic ->
        InvalidIfAsExpressionImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ELSE_MISPLACED_IN_WHEN) { firDiagnostic ->
        ElseMisplacedInWhenImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION) { firDiagnostic ->
        TypeParameterIsNotAnExpressionImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TYPE_PARAMETER_ON_LHS_OF_DOT) { firDiagnostic ->
        TypeParameterOnLhsOfDotImpl(
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NO_COMPANION_OBJECT) { firDiagnostic ->
        NoCompanionObjectImpl(
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(firDiagnostic.a.fir),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPRESSION_EXPECTED_PACKAGE_FOUND) { firDiagnostic ->
        ExpressionExpectedPackageFoundImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ERROR_IN_CONTRACT_DESCRIPTION) { firDiagnostic ->
        ErrorInContractDescriptionImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NO_GET_METHOD) { firDiagnostic ->
        NoGetMethodImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NO_SET_METHOD) { firDiagnostic ->
        NoSetMethodImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ITERATOR_MISSING) { firDiagnostic ->
        IteratorMissingImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_MISSING) { firDiagnostic ->
        HasNextMissingImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NEXT_MISSING) { firDiagnostic ->
        NextMissingImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.HAS_NEXT_FUNCTION_NONE_APPLICABLE) { firDiagnostic ->
        HasNextFunctionNoneApplicableImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NEXT_NONE_APPLICABLE) { firDiagnostic ->
        NextNoneApplicableImpl(
            firDiagnostic.a.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_MISSING) { firDiagnostic ->
        DelegateSpecialFunctionMissingImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_AMBIGUITY) { firDiagnostic ->
        DelegateSpecialFunctionAmbiguityImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE) { firDiagnostic ->
        DelegateSpecialFunctionNoneApplicableImpl(
            firDiagnostic.a,
            firDiagnostic.b.map { abstractFirBasedSymbol ->
                firSymbolBuilder.buildSymbol(abstractFirBasedSymbol.fir as FirDeclaration)
            },
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH) { firDiagnostic ->
        DelegateSpecialFunctionReturnTypeMismatchImpl(
            firDiagnostic.a,
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.b),
            firSymbolBuilder.typeBuilder.buildKtType(firDiagnostic.c),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNDERSCORE_IS_RESERVED) { firDiagnostic ->
        UnderscoreIsReservedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS) { firDiagnostic ->
        UnderscoreUsageWithoutBackticksImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.TOPLEVEL_TYPEALIASES_ONLY) { firDiagnostic ->
        ToplevelTypealiasesOnlyImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_VISIBILITY_MODIFIER) { firDiagnostic ->
        RedundantVisibilityModifierImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_MODALITY_MODIFIER) { firDiagnostic ->
        RedundantModalityModifierImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_RETURN_UNIT_TYPE) { firDiagnostic ->
        RedundantReturnUnitTypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_EXPLICIT_TYPE) { firDiagnostic ->
        RedundantExplicitTypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE) { firDiagnostic ->
        RedundantSingleExpressionStringTemplateImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CAN_BE_VAL) { firDiagnostic ->
        CanBeValImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT) { firDiagnostic ->
        CanBeReplacedWithOperatorAssignmentImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_CALL_OF_CONVERSION_METHOD) { firDiagnostic ->
        RedundantCallOfConversionMethodImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS) { firDiagnostic ->
        ArrayEqualityOperatorCanBeReplacedWithEqualsImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EMPTY_RANGE) { firDiagnostic ->
        EmptyRangeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDUNDANT_SETTER_PARAMETER_TYPE) { firDiagnostic ->
        RedundantSetterParameterTypeImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNUSED_VARIABLE) { firDiagnostic ->
        UnusedVariableImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ASSIGNED_VALUE_IS_NEVER_READ) { firDiagnostic ->
        AssignedValueIsNeverReadImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT) { firDiagnostic ->
        VariableInitializerIsRedundantImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.VARIABLE_NEVER_READ) { firDiagnostic ->
        VariableNeverReadImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.USELESS_CALL_ON_NOT_NULL) { firDiagnostic ->
        UselessCallOnNotNullImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.RETURN_NOT_ALLOWED) { firDiagnostic ->
        ReturnNotAllowedImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY) { firDiagnostic ->
        ReturnInFunctionWithExpressionBodyImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.USAGE_IS_NOT_INLINABLE) { firDiagnostic ->
        UsageIsNotInlinableImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED) { firDiagnostic ->
        NonLocalReturnNotAllowedImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.RECURSION_IN_INLINE) { firDiagnostic ->
        RecursionInInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        NonPublicCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        ProtectedConstructorCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR) { firDiagnostic ->
        ProtectedCallFromPublicInlineErrorImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        ProtectedCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.PRIVATE_CLASS_MEMBER_FROM_INLINE) { firDiagnostic ->
        PrivateClassMemberFromInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firSymbolBuilder.buildSymbol(firDiagnostic.b.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.SUPER_CALL_FROM_PUBLIC_INLINE) { firDiagnostic ->
        SuperCallFromPublicInlineImpl(
            firSymbolBuilder.buildSymbol(firDiagnostic.a.fir as FirDeclaration),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
}
