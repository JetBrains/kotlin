/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix;

import org.jetbrains.kotlin.idea.codeInsight.ImplementMethodsHandler;
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromCallWithConstructorCalleeActionFactory;
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromConstructorCallActionFactory;
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromReferenceExpressionActionFactory;
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromTypeReferenceActionFactory;
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.*;
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateLocalVariableActionFactory;
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterActionFactory;
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterByNamedArgumentActionFactory;
import org.jetbrains.kotlin.psi.JetClass;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.JetTokens.*;

public class QuickFixRegistrar {
    public static void registerQuickFixes() {
        JetSingleIntentionActionFactory
                removeAbstractModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD);
        JetSingleIntentionActionFactory addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD);

        QuickFixes.factories.put(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS, removeAbstractModifierFactory);

        JetSingleIntentionActionFactory removePartsFromPropertyFactory = RemovePartsFromPropertyFix.createFactory();
        QuickFixes.factories.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, removeAbstractModifierFactory);
        QuickFixes.factories.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, removePartsFromPropertyFactory);

        QuickFixes.factories.put(ABSTRACT_PROPERTY_WITH_GETTER, removeAbstractModifierFactory);
        QuickFixes.factories.put(ABSTRACT_PROPERTY_WITH_GETTER, removePartsFromPropertyFactory);

        QuickFixes.factories.put(ABSTRACT_PROPERTY_WITH_SETTER, removeAbstractModifierFactory);
        QuickFixes.factories.put(ABSTRACT_PROPERTY_WITH_SETTER, removePartsFromPropertyFactory);

        QuickFixes.factories.put(PROPERTY_INITIALIZER_IN_TRAIT, removePartsFromPropertyFactory);

        QuickFixes.factories.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, addAbstractModifierFactory);
        QuickFixes.factories.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, addAbstractModifierFactory);
        QuickFixes.factories.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, addAbstractModifierFactory);

        JetSingleIntentionActionFactory removeFinalModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(FINAL_KEYWORD);

        JetSingleIntentionActionFactory addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, JetClass.class);
        QuickFixes.factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        QuickFixes.factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        JetSingleIntentionActionFactory removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory();
        QuickFixes.factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        QuickFixes.factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        QuickFixes.factories.put(ABSTRACT_FUNCTION_WITH_BODY, removeAbstractModifierFactory);
        QuickFixes.factories.put(ABSTRACT_FUNCTION_WITH_BODY, removeFunctionBodyFactory);

        QuickFixes.factories.put(FINAL_PROPERTY_IN_TRAIT, removeFinalModifierFactory);
        QuickFixes.factories.put(FINAL_FUNCTION_WITH_NO_BODY, removeFinalModifierFactory);

        JetSingleIntentionActionFactory addFunctionBodyFactory = AddFunctionBodyFix.createFactory();
        QuickFixes.factories.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addAbstractModifierFactory);
        QuickFixes.factories.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addFunctionBodyFactory);

        QuickFixes.factories.put(NON_VARARG_SPREAD, RemovePsiElementSimpleFix.createRemoveSpreadFactory());

        QuickFixes.factories.put(MIXING_NAMED_AND_POSITIONED_ARGUMENTS, AddNameToArgumentFix.createFactory());

        QuickFixes.factories.put(NON_MEMBER_FUNCTION_NO_BODY, addFunctionBodyFactory);

        QuickFixes.factories.put(NOTHING_TO_OVERRIDE, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD));
        QuickFixes.factories.put(NOTHING_TO_OVERRIDE, ChangeMemberFunctionSignatureFix.createFactory());
        QuickFixes.factories.put(NOTHING_TO_OVERRIDE, AddFunctionToSupertypeFix.createFactory());
        QuickFixes.factories.put(VIRTUAL_MEMBER_HIDDEN, AddModifierFix.createFactory(OVERRIDE_KEYWORD));

        QuickFixes.factories.put(USELESS_CAST_STATIC_ASSERT_IS_FINE, ReplaceOperationInBinaryExpressionFix.createChangeCastToStaticAssertFactory());
        QuickFixes.factories.put(USELESS_CAST, RemoveRightPartOfBinaryExpressionFix.createRemoveCastFactory());

        JetSingleIntentionActionFactory changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory();
        QuickFixes.factories.put(WRONG_SETTER_PARAMETER_TYPE, changeAccessorTypeFactory);
        QuickFixes.factories.put(WRONG_GETTER_RETURN_TYPE, changeAccessorTypeFactory);

        QuickFixes.factories.put(USELESS_ELVIS, RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory());

        JetSingleIntentionActionFactory removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true);
        QuickFixes.factories.put(REDUNDANT_MODIFIER, removeRedundantModifierFactory);
        QuickFixes.factories.put(ABSTRACT_MODIFIER_IN_TRAIT, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD,
                                                                                                                        true));
        QuickFixes.factories.put(OPEN_MODIFIER_IN_TRAIT, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD, true));
        QuickFixes.factories.put(TRAIT_CAN_NOT_BE_FINAL, removeFinalModifierFactory);
        QuickFixes.factories.put(REDUNDANT_PROJECTION, RemoveModifierFix.createRemoveProjectionFactory(true));
        QuickFixes.factories.put(INCOMPATIBLE_MODIFIERS, RemoveModifierFix.createRemoveModifierFactory(false));
        QuickFixes.factories.put(VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY, RemoveModifierFix.createRemoveVarianceFactory());

        JetSingleIntentionActionFactory removeOpenModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD);
        QuickFixes.factories.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, AddModifierFix.createFactory(OPEN_KEYWORD, JetClass.class));
        QuickFixes.factories.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, removeOpenModifierFactory);

        JetSingleIntentionActionFactory removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory();
        QuickFixes.factories.put(GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, removeModifierFactory);
        QuickFixes.factories.put(REDUNDANT_MODIFIER_IN_GETTER, removeRedundantModifierFactory);
        QuickFixes.factories.put(ILLEGAL_MODIFIER, removeModifierFactory);
        QuickFixes.factories.put(REPEATED_MODIFIER, removeModifierFactory);

        JetSingleIntentionActionFactory removeInnerModifierFactory =
                RemoveModifierFix.createRemoveModifierFromListOwnerFactory(INNER_KEYWORD);
        QuickFixes.factories.put(INNER_CLASS_IN_TRAIT, removeInnerModifierFactory);
        QuickFixes.factories.put(INNER_CLASS_IN_OBJECT, removeInnerModifierFactory);

        JetSingleIntentionActionFactory changeToBackingFieldFactory = ChangeToBackingFieldFix.createFactory();
        QuickFixes.factories.put(INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER, changeToBackingFieldFactory);
        QuickFixes.factories.put(INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER, changeToBackingFieldFactory);

        JetSingleIntentionActionFactory changeToPropertyNameFactory = ChangeToPropertyNameFix.createFactory();
        QuickFixes.factories.put(NO_BACKING_FIELD_ABSTRACT_PROPERTY, changeToPropertyNameFactory);
        QuickFixes.factories.put(NO_BACKING_FIELD_CUSTOM_ACCESSORS, changeToPropertyNameFactory);
        QuickFixes.factories.put(INACCESSIBLE_BACKING_FIELD, changeToPropertyNameFactory);

        JetSingleIntentionActionFactory unresolvedReferenceFactory = AutoImportFix.Default.createFactory();
        QuickFixes.factories.put(UNRESOLVED_REFERENCE, unresolvedReferenceFactory);
        QuickFixes.factories.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, unresolvedReferenceFactory);

        JetSingleIntentionActionFactory removeImportFixFactory = RemovePsiElementSimpleFix.createRemoveImportFactory();
        QuickFixes.factories.put(CONFLICTING_IMPORT, removeImportFixFactory);

        QuickFixes.factories.put(SUPERTYPE_NOT_INITIALIZED, ChangeToConstructorInvocationFix.createFactory());
        QuickFixes.factories.put(FUNCTION_CALL_EXPECTED, ChangeToFunctionInvocationFix.createFactory());

        QuickFixes.factories.put(CANNOT_CHANGE_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());
        QuickFixes.factories.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());

        QuickFixes.factories.put(REDUNDANT_NULLABLE, RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.REDUNDANT));
        QuickFixes.factories.put(NULLABLE_SUPERTYPE, RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.SUPERTYPE));
        QuickFixes.factories.put(USELESS_NULLABLE_CHECK, RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.USELESS));


        ImplementMethodsHandler implementMethodsHandler = new ImplementMethodsHandler();
        QuickFixes.actions.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);
        QuickFixes.actions.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);

        ChangeVariableMutabilityFix changeVariableMutabilityFix = new ChangeVariableMutabilityFix();
        QuickFixes.actions.put(VAL_WITH_SETTER, changeVariableMutabilityFix);
        QuickFixes.actions.put(VAL_REASSIGNMENT, changeVariableMutabilityFix);
        QuickFixes.actions.put(VAR_OVERRIDDEN_BY_VAL, changeVariableMutabilityFix);

        JetSingleIntentionActionFactory removeValVarFromParameterFixFactory = RemoveValVarFromParameterFix.createFactory();
        QuickFixes.factories.put(VAL_OR_VAR_ON_FUN_PARAMETER, removeValVarFromParameterFixFactory);
        QuickFixes.factories.put(VAL_OR_VAR_ON_LOOP_PARAMETER, removeValVarFromParameterFixFactory);
        QuickFixes.factories.put(VAL_OR_VAR_ON_CATCH_PARAMETER, removeValVarFromParameterFixFactory);

        QuickFixes.factories.put(VIRTUAL_MEMBER_HIDDEN, AddOverrideToEqualsHashCodeToStringFix.createFactory());

        QuickFixes.factories.put(UNUSED_VARIABLE, RemovePsiElementSimpleFix.createRemoveVariableFactory());

        QuickFixes.actions.put(UNNECESSARY_SAFE_CALL, ReplaceCallFix.toDotCallFromSafeCall());
        QuickFixes.actions.put(UNSAFE_CALL, ReplaceCallFix.toSafeCall());

        QuickFixes.actions.put(UNSAFE_CALL, ExclExclCallFix.introduceExclExclCall());
        QuickFixes.actions.put(UNNECESSARY_NOT_NULL_ASSERTION, ExclExclCallFix.removeExclExclCall());
        QuickFixes.factories.put(UNSAFE_INFIX_CALL, ReplaceInfixCallFix.createFactory());

        JetSingleIntentionActionFactory removeProtectedModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(PROTECTED_KEYWORD);
        QuickFixes.factories.put(PACKAGE_MEMBER_CANNOT_BE_PROTECTED, removeProtectedModifierFactory);

        QuickFixes.actions.put(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, new SpecifyTypeExplicitlyFix());
        QuickFixes.actions.put(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED, new SpecifyTypeExplicitlyFix());

        QuickFixes.factories.put(ELSE_MISPLACED_IN_WHEN, MoveWhenElseBranchFix.createFactory());
        QuickFixes.factories.put(NO_ELSE_IN_WHEN, AddWhenElseBranchFix.createFactory());

        QuickFixes.factories.put(NO_TYPE_ARGUMENTS_ON_RHS, AddStarProjectionsFix.createFactoryForIsExpression());
        QuickFixes.factories.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS, AddStarProjectionsFix.createFactoryForJavaClass());

        QuickFixes.factories.put(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER, RemovePsiElementSimpleFix.createRemoveTypeArgumentsFactory());

        JetSingleIntentionActionFactory changeToStarProjectionFactory = ChangeToStarProjectionFix.createFactory();
        QuickFixes.factories.put(UNCHECKED_CAST, changeToStarProjectionFactory);
        QuickFixes.factories.put(CANNOT_CHECK_FOR_ERASED, changeToStarProjectionFactory);

        QuickFixes.factories.put(INACCESSIBLE_OUTER_CLASS_EXPRESSION, AddModifierFix.createFactory(INNER_KEYWORD, JetClass.class));

        JetSingleIntentionActionFactory addOpenModifierToClassDeclarationFix = AddOpenModifierToClassDeclarationFix.createFactory();
        QuickFixes.factories.put(FINAL_SUPERTYPE, addOpenModifierToClassDeclarationFix);
        QuickFixes.factories.put(FINAL_UPPER_BOUND, addOpenModifierToClassDeclarationFix);

        QuickFixes.factories.put(OVERRIDING_FINAL_MEMBER, MakeOverriddenMemberOpenFix.createFactory());

        QuickFixes.factories.put(PARAMETER_NAME_CHANGED_ON_OVERRIDE, RenameParameterToMatchOverriddenMethodFix.createFactory());

        QuickFixes.factories.put(OPEN_MODIFIER_IN_ENUM, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD));
        QuickFixes.factories.put(ABSTRACT_MODIFIER_IN_ENUM, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD));
        QuickFixes.factories.put(ILLEGAL_ENUM_ANNOTATION, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ENUM_KEYWORD));

        QuickFixes.factories.put(NESTED_CLASS_NOT_ALLOWED, AddModifierFix.createFactory(INNER_KEYWORD));

        QuickFixes.factories.put(CONFLICTING_PROJECTION, RemoveModifierFix.createRemoveProjectionFactory(false));
        QuickFixes.factories.put(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT, RemoveModifierFix.createRemoveProjectionFactory(false));
        QuickFixes.factories.put(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE, RemoveModifierFix.createRemoveProjectionFactory(false));

        QuickFixes.factories.put(NOT_AN_ANNOTATION_CLASS, MakeClassAnAnnotationClassFix.createFactory());

        QuickFixes.factories.put(DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED, AddSemicolonAfterFunctionCallFix.createFactory());

        JetIntentionActionsFactory changeVariableTypeFix = ChangeVariableTypeFix.createFactoryForPropertyOrReturnTypeMismatchOnOverride();
        QuickFixes.factories.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, changeVariableTypeFix);
        QuickFixes.factories.put(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, changeVariableTypeFix);
        QuickFixes.factories.put(COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, ChangeVariableTypeFix.createFactoryForComponentFunctionReturnTypeMismatch());

        JetSingleIntentionActionFactory changeFunctionReturnTypeFix = ChangeFunctionReturnTypeFix.createFactoryForChangingReturnTypeToUnit();
        QuickFixes.factories.put(RETURN_TYPE_MISMATCH, changeFunctionReturnTypeFix);
        QuickFixes.factories.put(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY, changeFunctionReturnTypeFix);
        QuickFixes.factories.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, ChangeFunctionReturnTypeFix.createFactoryForReturnTypeMismatchOnOverride());
        QuickFixes.factories.put(COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, ChangeFunctionReturnTypeFix.createFactoryForComponentFunctionReturnTypeMismatch());
        QuickFixes.factories.put(HAS_NEXT_FUNCTION_TYPE_MISMATCH, ChangeFunctionReturnTypeFix.createFactoryForHasNextFunctionTypeMismatch());
        QuickFixes.factories.put(COMPARE_TO_TYPE_MISMATCH, ChangeFunctionReturnTypeFix.createFactoryForCompareToTypeMismatch());

        QuickFixes.factories.put(TOO_MANY_ARGUMENTS, ChangeFunctionSignatureFix.createFactory());
        QuickFixes.factories.put(NO_VALUE_FOR_PARAMETER, ChangeFunctionSignatureFix.createFactory());
        QuickFixes.factories.put(UNUSED_PARAMETER, ChangeFunctionSignatureFix.createFactoryForUnusedParameter());
        QuickFixes.factories.put(EXPECTED_PARAMETERS_NUMBER_MISMATCH, ChangeFunctionSignatureFix.createFactoryForParametersNumberMismatch());

        QuickFixes.factories.put(EXPECTED_PARAMETER_TYPE_MISMATCH, ChangeTypeFix.createFactoryForExpectedParameterTypeMismatch());
        QuickFixes.factories.put(EXPECTED_RETURN_TYPE_MISMATCH, ChangeTypeFix.createFactoryForExpectedReturnTypeMismatch());

        JetSingleIntentionActionFactory changeFunctionLiteralReturnTypeFix = ChangeFunctionLiteralReturnTypeFix.createFactoryForExpectedOrAssignmentTypeMismatch();
        QuickFixes.factories.put(EXPECTED_TYPE_MISMATCH, changeFunctionLiteralReturnTypeFix);
        QuickFixes.factories.put(ASSIGNMENT_TYPE_MISMATCH, changeFunctionLiteralReturnTypeFix);

        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateUnaryOperationActionFactory.INSTANCE$);
        QuickFixes.factories.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, CreateUnaryOperationActionFactory.INSTANCE$);
        QuickFixes.factories.put(NO_VALUE_FOR_PARAMETER, CreateUnaryOperationActionFactory.INSTANCE$);

        QuickFixes.factories.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, CreateBinaryOperationActionFactory.INSTANCE$);
        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateBinaryOperationActionFactory.INSTANCE$);
        QuickFixes.factories.put(NONE_APPLICABLE, CreateBinaryOperationActionFactory.INSTANCE$);
        QuickFixes.factories.put(NO_VALUE_FOR_PARAMETER, CreateBinaryOperationActionFactory.INSTANCE$);
        QuickFixes.factories.put(TOO_MANY_ARGUMENTS, CreateBinaryOperationActionFactory.INSTANCE$);

        QuickFixes.factories.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, CreateFunctionOrPropertyFromCallActionFactory.INSTANCE$);
        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateFunctionOrPropertyFromCallActionFactory.INSTANCE$);
        QuickFixes.factories.put(NO_VALUE_FOR_PARAMETER, CreateFunctionOrPropertyFromCallActionFactory.INSTANCE$);
        QuickFixes.factories.put(TOO_MANY_ARGUMENTS, CreateFunctionOrPropertyFromCallActionFactory.INSTANCE$);
        QuickFixes.factories.put(EXPRESSION_EXPECTED_PACKAGE_FOUND, CreateFunctionOrPropertyFromCallActionFactory.INSTANCE$);

        QuickFixes.factories.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, CreateClassFromConstructorCallActionFactory.INSTANCE$);
        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateClassFromConstructorCallActionFactory.INSTANCE$);
        QuickFixes.factories.put(EXPRESSION_EXPECTED_PACKAGE_FOUND, CreateClassFromConstructorCallActionFactory.INSTANCE$);

        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateLocalVariableActionFactory.INSTANCE$);
        QuickFixes.factories.put(EXPRESSION_EXPECTED_PACKAGE_FOUND, CreateLocalVariableActionFactory.INSTANCE$);

        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateParameterActionFactory.INSTANCE$);
        QuickFixes.factories.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, CreateParameterActionFactory.INSTANCE$);
        QuickFixes.factories.put(EXPRESSION_EXPECTED_PACKAGE_FOUND, CreateParameterActionFactory.INSTANCE$);

        QuickFixes.factories.put(NAMED_PARAMETER_NOT_FOUND, CreateParameterByNamedArgumentActionFactory.INSTANCE$);

        QuickFixes.factories.put(FUNCTION_EXPECTED, CreateInvokeFunctionActionFactory.INSTANCE$);

        QuickFixFactoryForTypeMismatchError factoryForTypeMismatchError = new QuickFixFactoryForTypeMismatchError();
        QuickFixes.factories.put(TYPE_MISMATCH, factoryForTypeMismatchError);
        QuickFixes.factories.put(NULL_FOR_NONNULL_TYPE, factoryForTypeMismatchError);
        QuickFixes.factories.put(CONSTANT_EXPECTED_TYPE_MISMATCH, factoryForTypeMismatchError);

        QuickFixes.factories.put(SMARTCAST_IMPOSSIBLE, CastExpressionFix.createFactoryForSmartCastImpossible());

        QuickFixes.factories.put(PLATFORM_CLASS_MAPPED_TO_KOTLIN, MapPlatformClassToKotlinFix.createFactory());

        QuickFixes.factories.put(MANY_CLASSES_IN_SUPERTYPE_LIST, RemoveSupertypeFix.createFactory());

        QuickFixes.factories.put(NO_GET_METHOD, CreateGetFunctionActionFactory.INSTANCE$);
        QuickFixes.factories.put(NO_SET_METHOD, CreateSetFunctionActionFactory.INSTANCE$);
        QuickFixes.factories.put(HAS_NEXT_MISSING, CreateHasNextFunctionActionFactory.INSTANCE$);
        QuickFixes.factories.put(HAS_NEXT_FUNCTION_NONE_APPLICABLE, CreateHasNextFunctionActionFactory.INSTANCE$);
        QuickFixes.factories.put(NEXT_MISSING, CreateNextFunctionActionFactory.INSTANCE$);
        QuickFixes.factories.put(NEXT_NONE_APPLICABLE, CreateNextFunctionActionFactory.INSTANCE$);
        QuickFixes.factories.put(ITERATOR_MISSING, CreateIteratorFunctionActionFactory.INSTANCE$);
        QuickFixes.factories.put(COMPONENT_FUNCTION_MISSING, CreateComponentFunctionActionFactory.INSTANCE$);

        QuickFixes.factories.put(DELEGATE_SPECIAL_FUNCTION_MISSING, CreatePropertyDelegateAccessorsActionFactory.INSTANCE$);
        QuickFixes.factories.put(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, CreatePropertyDelegateAccessorsActionFactory.INSTANCE$);

        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateClassFromTypeReferenceActionFactory.INSTANCE$);
        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateClassFromReferenceExpressionActionFactory.INSTANCE$);
        QuickFixes.factories.put(UNRESOLVED_REFERENCE, CreateClassFromCallWithConstructorCalleeActionFactory.INSTANCE$);

        QuickFixes.factories.put(DEPRECATED_CLASS_OBJECT_SYNTAX, ClassObjectToDefaultObjectFix.Factory);
        QuickFixes.factories.put(DEPRECATED_CLASS_OBJECT_SYNTAX, ClassObjectToDefaultObjectInWholeProjectFix.Factory);
        QuickFixes.factories.put(INIT_KEYWORD_BEFORE_CLASS_INITIALIZER_EXPECTED, AddInitKeywordFix.Default);
    }
}
