/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.WhenMissingCase
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtElement
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

internal val KT_DIAGNOSTIC_CONVERTER = KtDiagnosticConverterBuilder.buildConverter {
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
    add(FirErrors.EXPRESSION_REQUIRED) { firDiagnostic ->
        ExpressionRequiredImpl(
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
    add(FirErrors.RETURN_NOT_ALLOWED) { firDiagnostic ->
        ReturnNotAllowedImpl(
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
    add(FirErrors.HIDDEN) { firDiagnostic ->
        HiddenImpl(
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
    add(FirErrors.TYPE_PARAMETER_AS_SUPERTYPE) { firDiagnostic ->
        TypeParameterAsSupertypeImpl(
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
            firSymbolBuilder.buildClassLikeSymbol(firDiagnostic.a),
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
    add(FirErrors.NON_PRIVATE_CONSTRUCTOR_IN_SEALED) { firDiagnostic ->
        NonPrivateConstructorInSealedImpl(
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
    add(FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE) { firDiagnostic ->
        ExposedTypealiasExpandedTypeImpl(
            firDiagnostic.a.toVisibility(),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c.toVisibility(),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_FUNCTION_RETURN_TYPE) { firDiagnostic ->
        ExposedFunctionReturnTypeImpl(
            firDiagnostic.a.toVisibility(),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c.toVisibility(),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_RECEIVER_TYPE) { firDiagnostic ->
        ExposedReceiverTypeImpl(
            firDiagnostic.a.toVisibility(),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c.toVisibility(),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_PROPERTY_TYPE) { firDiagnostic ->
        ExposedPropertyTypeImpl(
            firDiagnostic.a.toVisibility(),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c.toVisibility(),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_PARAMETER_TYPE) { firDiagnostic ->
        ExposedParameterTypeImpl(
            firDiagnostic.a.toVisibility(),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c.toVisibility(),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_INTERFACE) { firDiagnostic ->
        ExposedSuperInterfaceImpl(
            firDiagnostic.a.toVisibility(),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c.toVisibility(),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_SUPER_CLASS) { firDiagnostic ->
        ExposedSuperClassImpl(
            firDiagnostic.a.toVisibility(),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c.toVisibility(),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.EXPOSED_TYPE_PARAMETER_BOUND) { firDiagnostic ->
        ExposedTypeParameterBoundImpl(
            firDiagnostic.a.toVisibility(),
            firSymbolBuilder.buildSymbol(firDiagnostic.b as FirDeclaration),
            firDiagnostic.c.toVisibility(),
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
    add(FirErrors.INAPPLICABLE_LATEINIT_MODIFIER) { firDiagnostic ->
        InapplicableLateinitModifierImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.AMBIGUITY) { firDiagnostic ->
        AmbiguityImpl(
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
    add(FirErrors.TYPE_MISMATCH) { firDiagnostic ->
        TypeMismatchImpl(
            firSymbolBuilder.buildKtType(firDiagnostic.a),
            firSymbolBuilder.buildKtType(firDiagnostic.b),
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
            firSymbolBuilder.buildTypeParameterSymbol(firDiagnostic.a.fir as FirTypeParameter),
            firSymbolBuilder.buildKtType(firDiagnostic.b),
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
            firSymbolBuilder.buildClassLikeSymbol(firDiagnostic.b.fir as FirClass<*>),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.NO_TYPE_FOR_TYPE_PARAMETER) { firDiagnostic ->
        NoTypeForTypeParameterImpl(
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
            firSymbolBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE) { firDiagnostic ->
        CannotChangeAccessPrivilegeImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildCallableSymbol(firDiagnostic.b as FirCallableDeclaration),
            firDiagnostic.c,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.OVERRIDING_FINAL_MEMBER) { firDiagnostic ->
        OverridingFinalMemberImpl(
            firSymbolBuilder.buildCallableSymbol(firDiagnostic.a as FirCallableDeclaration),
            firDiagnostic.b,
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
    add(FirErrors.MANY_COMPANION_OBJECTS) { firDiagnostic ->
        ManyCompanionObjectsImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.CONFLICTING_OVERLOADS) { firDiagnostic ->
        ConflictingOverloadsImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.REDECLARATION) { firDiagnostic ->
        RedeclarationImpl(
            firDiagnostic.a,
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.ANY_METHOD_IMPLEMENTED_IN_INTERFACE) { firDiagnostic ->
        AnyMethodImplementedInInterfaceImpl(
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
    add(FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION) { firDiagnostic ->
        InitializerRequiredForDestructuringDeclarationImpl(
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.COMPONENT_FUNCTION_MISSING) { firDiagnostic ->
        ComponentFunctionMissingImpl(
            firDiagnostic.a,
            firSymbolBuilder.buildKtType(firDiagnostic.b),
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
    add(FirErrors.UNINITIALIZED_VARIABLE) { firDiagnostic ->
        UninitializedVariableImpl(
            firSymbolBuilder.buildVariableSymbol(firDiagnostic.a.fir as FirProperty),
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
            firSymbolBuilder.buildKtType(firDiagnostic.a),
            firDiagnostic as FirPsiDiagnostic<*>,
            token,
        )
    }
    add(FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL) { firDiagnostic ->
        UnsafeImplicitInvokeCallImpl(
            firSymbolBuilder.buildKtType(firDiagnostic.a),
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
    add(FirErrors.NO_ELSE_IN_WHEN) { firDiagnostic ->
        NoElseInWhenImpl(
            firDiagnostic.a.map { whenMissingCase ->
                TODO("WhenMissingCase conversion is not supported yet")
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
    add(FirErrors.ERROR_IN_CONTRACT_DESCRIPTION) { firDiagnostic ->
        ErrorInContractDescriptionImpl(
            firDiagnostic.a,
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
}
