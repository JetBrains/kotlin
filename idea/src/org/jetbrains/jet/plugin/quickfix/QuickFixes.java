/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.codeInsight.intention.IntentionAction;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler;

import java.util.Collection;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class QuickFixes {

    private static final Multimap<DiagnosticFactory<?>, JetIntentionActionsFactory> factories = HashMultimap.create();
    private static final Multimap<DiagnosticFactory<?>, IntentionAction> actions = HashMultimap.create();

    public static Collection<JetIntentionActionsFactory> getActionsFactories(DiagnosticFactory<?> diagnosticFactory) {
        return factories.get(diagnosticFactory);
    }

    public static Collection<IntentionAction> getActions(DiagnosticFactory<?> diagnosticFactory) {
        return actions.get(diagnosticFactory);
    }

    private QuickFixes() {}

    static {
        JetSingleIntentionActionFactory removeAbstractModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD);
        JetSingleIntentionActionFactory addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD);

        factories.put(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS, removeAbstractModifierFactory);

        JetSingleIntentionActionFactory removePartsFromPropertyFactory = RemovePartsFromPropertyFix.createFactory();
        factories.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, removeAbstractModifierFactory);
        factories.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, removePartsFromPropertyFactory);

        factories.put(ABSTRACT_PROPERTY_WITH_GETTER, removeAbstractModifierFactory);
        factories.put(ABSTRACT_PROPERTY_WITH_GETTER, removePartsFromPropertyFactory);

        factories.put(ABSTRACT_PROPERTY_WITH_SETTER, removeAbstractModifierFactory);
        factories.put(ABSTRACT_PROPERTY_WITH_SETTER, removePartsFromPropertyFactory);

        factories.put(PROPERTY_INITIALIZER_IN_TRAIT, removePartsFromPropertyFactory);

        factories.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, addAbstractModifierFactory);

        JetSingleIntentionActionFactory removeFinalModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(FINAL_KEYWORD);

        JetSingleIntentionActionFactory addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, JetClass.class);
        factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        JetSingleIntentionActionFactory removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory();
        factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        factories.put(ABSTRACT_FUNCTION_WITH_BODY, removeAbstractModifierFactory);
        factories.put(ABSTRACT_FUNCTION_WITH_BODY, removeFunctionBodyFactory);

        factories.put(FINAL_PROPERTY_IN_TRAIT, removeFinalModifierFactory);
        factories.put(FINAL_FUNCTION_WITH_NO_BODY, removeFinalModifierFactory);

        JetSingleIntentionActionFactory addFunctionBodyFactory = AddFunctionBodyFix.createFactory();
        factories.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addAbstractModifierFactory);
        factories.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addFunctionBodyFactory);

        factories.put(NON_VARARG_SPREAD, RemovePsiElementSimpleFix.createRemoveSpreadFactory());

        factories.put(MIXING_NAMED_AND_POSITIONED_ARGUMENTS, AddNameToArgumentFix.createFactory());

        factories.put(NON_MEMBER_FUNCTION_NO_BODY, addFunctionBodyFactory);

        factories.put(NOTHING_TO_OVERRIDE, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD));
        factories.put(NOTHING_TO_OVERRIDE, ChangeMemberFunctionSignatureFix.createFactory());
        factories.put(NOTHING_TO_OVERRIDE, AddFunctionToSupertypeFix.createFactory());
        factories.put(VIRTUAL_MEMBER_HIDDEN, AddModifierFix.createFactory(OVERRIDE_KEYWORD));

        factories.put(USELESS_CAST_STATIC_ASSERT_IS_FINE, ReplaceOperationInBinaryExpressionFix.createChangeCastToStaticAssertFactory());
        factories.put(USELESS_CAST, RemoveRightPartOfBinaryExpressionFix.createRemoveCastFactory());

        JetSingleIntentionActionFactory changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory();
        factories.put(WRONG_SETTER_PARAMETER_TYPE, changeAccessorTypeFactory);
        factories.put(WRONG_GETTER_RETURN_TYPE, changeAccessorTypeFactory);

        factories.put(USELESS_ELVIS, RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory());

        JetSingleIntentionActionFactory removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true);
        factories.put(REDUNDANT_MODIFIER, removeRedundantModifierFactory);
        factories.put(ABSTRACT_MODIFIER_IN_TRAIT, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD, true));
        factories.put(OPEN_MODIFIER_IN_TRAIT, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD, true));
        factories.put(TRAIT_CAN_NOT_BE_FINAL, removeFinalModifierFactory);
        factories.put(REDUNDANT_PROJECTION, RemoveModifierFix.createRemoveProjectionFactory(true));
        factories.put(INCOMPATIBLE_MODIFIERS, RemoveModifierFix.createRemoveModifierFactory(false));
        factories.put(VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY, RemoveModifierFix.createRemoveVarianceFactory());

        JetSingleIntentionActionFactory removeOpenModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD);
        factories.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, AddModifierFix.createFactory(OPEN_KEYWORD, JetClass.class));
        factories.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, removeOpenModifierFactory);

        JetSingleIntentionActionFactory removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory();
        factories.put(GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, removeModifierFactory);
        factories.put(REDUNDANT_MODIFIER_IN_GETTER, removeRedundantModifierFactory);
        factories.put(ILLEGAL_MODIFIER, removeModifierFactory);

        JetSingleIntentionActionFactory changeToBackingFieldFactory = ChangeToBackingFieldFix.createFactory();
        factories.put(INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER, changeToBackingFieldFactory);
        factories.put(INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER, changeToBackingFieldFactory);

        JetSingleIntentionActionFactory changeToPropertyNameFactory = ChangeToPropertyNameFix.createFactory();
        factories.put(NO_BACKING_FIELD_ABSTRACT_PROPERTY, changeToPropertyNameFactory);
        factories.put(NO_BACKING_FIELD_CUSTOM_ACCESSORS, changeToPropertyNameFactory);
        factories.put(INACCESSIBLE_BACKING_FIELD, changeToPropertyNameFactory);

        JetSingleIntentionActionFactory unresolvedReferenceFactory = AutoImportFix.createFactory();
        factories.put(UNRESOLVED_REFERENCE, unresolvedReferenceFactory);
        factories.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, unresolvedReferenceFactory);

        JetSingleIntentionActionFactory removeImportFixFactory = RemovePsiElementSimpleFix.createRemoveImportFactory();
        factories.put(USELESS_SIMPLE_IMPORT, removeImportFixFactory);
        factories.put(USELESS_HIDDEN_IMPORT, removeImportFixFactory);

        factories.put(SUPERTYPE_NOT_INITIALIZED, ChangeToConstructorInvocationFix.createFactory());
        factories.put(FUNCTION_CALL_EXPECTED, ChangeToFunctionInvocationFix.createFactory());
        
        factories.put(CANNOT_CHANGE_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());
        factories.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());

        factories.put(REDUNDANT_NULLABLE, RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.REDUNDANT));
        factories.put(NULLABLE_SUPERTYPE, RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.SUPERTYPE));
        factories.put(USELESS_NULLABLE_CHECK, RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.USELESS));


        ImplementMethodsHandler implementMethodsHandler = new ImplementMethodsHandler();
        actions.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);
        actions.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);

        ChangeVariableMutabilityFix changeVariableMutabilityFix = new ChangeVariableMutabilityFix();
        actions.put(VAL_WITH_SETTER, changeVariableMutabilityFix);
        actions.put(VAL_REASSIGNMENT, changeVariableMutabilityFix);
        actions.put(VAR_OVERRIDDEN_BY_VAL, changeVariableMutabilityFix);

        RemoveValVarFromParametersFix removeValVarFromParametersFix = new RemoveValVarFromParametersFix();
        actions.put(VAL_OR_VAR_ON_FUN_PARAMETER, removeValVarFromParametersFix);
        actions.put(VAL_OR_VAR_ON_LOOP_PARAMETER, removeValVarFromParametersFix);
        actions.put(VAL_OR_VAR_ON_CATCH_PARAMETER, removeValVarFromParametersFix);

        factories.put(VIRTUAL_MEMBER_HIDDEN, AddOverrideToEqualsHashCodeToStringFix.createFactory());

        factories.put(UNUSED_VARIABLE, RemovePsiElementSimpleFix.createRemoveVariableFactory());

        actions.put(UNNECESSARY_SAFE_CALL, ReplaceCallFix.toDotCallFromSafeCall());
        actions.put(UNSAFE_CALL, ReplaceCallFix.toSafeCall());

        actions.put(UNSAFE_CALL, ExclExclCallFix.introduceExclExclCall());
        actions.put(UNNECESSARY_NOT_NULL_ASSERTION, ExclExclCallFix.removeExclExclCall());
        factories.put(UNSAFE_INFIX_CALL, ReplaceInfixCallFix.createFactory());

        JetSingleIntentionActionFactory removeProtectedModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(PROTECTED_KEYWORD);
        factories.put(PACKAGE_MEMBER_CANNOT_BE_PROTECTED, removeProtectedModifierFactory);

        actions.put(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, new SpecifyTypeExplicitlyFix());
        actions.put(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED, new SpecifyTypeExplicitlyFix());

        factories.put(ELSE_MISPLACED_IN_WHEN, MoveWhenElseBranchFix.createFactory());
        factories.put(NO_ELSE_IN_WHEN, AddWhenElseBranchFix.createFactory());

        factories.put(NO_TYPE_ARGUMENTS_ON_RHS, AddStarProjectionsFix.createFactoryForIsExpression());
        factories.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS, AddStarProjectionsFix.createFactoryForJavaClass());

        factories.put(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER, RemovePsiElementSimpleFix.createRemoveTypeArgumentsFactory());

        JetSingleIntentionActionFactory changeToStarProjectionFactory = ChangeToStarProjectionFix.createFactory();
        factories.put(UNCHECKED_CAST, changeToStarProjectionFactory);
        factories.put(CANNOT_CHECK_FOR_ERASED, changeToStarProjectionFactory);

        factories.put(INACCESSIBLE_OUTER_CLASS_EXPRESSION, AddModifierFix.createFactory(INNER_KEYWORD, JetClass.class));

        JetSingleIntentionActionFactory addOpenModifierToClassDeclarationFix = AddOpenModifierToClassDeclarationFix.createFactory();
        factories.put(FINAL_SUPERTYPE, addOpenModifierToClassDeclarationFix);
        factories.put(FINAL_UPPER_BOUND, addOpenModifierToClassDeclarationFix);

        factories.put(OVERRIDING_FINAL_MEMBER, MakeOverriddenMemberOpenFix.createFactory());

        factories.put(PARAMETER_NAME_CHANGED_ON_OVERRIDE, RenameParameterToMatchOverriddenMethodFix.createFactory());

        factories.put(OPEN_MODIFIER_IN_ENUM, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD));
        factories.put(ILLEGAL_ENUM_ANNOTATION, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ENUM_KEYWORD));

        factories.put(NESTED_CLASS_NOT_ALLOWED, AddModifierFix.createFactory(INNER_KEYWORD));

        factories.put(CONFLICTING_PROJECTION, RemoveModifierFix.createRemoveProjectionFactory(false));
        factories.put(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT, RemoveModifierFix.createRemoveProjectionFactory(false));
        factories.put(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE, RemoveModifierFix.createRemoveProjectionFactory(false));

        factories.put(NOT_AN_ANNOTATION_CLASS, MakeClassAnAnnotationClassFix.createFactory());

        factories.put(DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED, AddSemicolonAfterFunctionCallFix.createFactory());

        JetIntentionActionsFactory changeVariableTypeFix = ChangeVariableTypeFix.createFactoryForPropertyOrReturnTypeMismatchOnOverride();
        factories.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, changeVariableTypeFix);
        factories.put(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, changeVariableTypeFix);
        factories.put(COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, ChangeVariableTypeFix.createFactoryForComponentFunctionReturnTypeMismatch());

        JetSingleIntentionActionFactory changeFunctionReturnTypeFix = ChangeFunctionReturnTypeFix.createFactoryForChangingReturnTypeToUnit();
        factories.put(RETURN_TYPE_MISMATCH, changeFunctionReturnTypeFix);
        factories.put(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY, changeFunctionReturnTypeFix);
        factories.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, ChangeFunctionReturnTypeFix.createFactoryForReturnTypeMismatchOnOverride());
        factories.put(COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, ChangeFunctionReturnTypeFix.createFactoryForComponentFunctionReturnTypeMismatch());
        factories.put(HAS_NEXT_FUNCTION_TYPE_MISMATCH, ChangeFunctionReturnTypeFix.createFactoryForHasNextFunctionTypeMismatch());
        factories.put(COMPARE_TO_TYPE_MISMATCH, ChangeFunctionReturnTypeFix.createFactoryForCompareToTypeMismatch());

        factories.put(TOO_MANY_ARGUMENTS, ChangeFunctionSignatureFix.createFactory());
        factories.put(NO_VALUE_FOR_PARAMETER, ChangeFunctionSignatureFix.createFactory());
        factories.put(UNUSED_PARAMETER, ChangeFunctionSignatureFix.createFactoryForUnusedParameter());
        factories.put(EXPECTED_PARAMETERS_NUMBER_MISMATCH, ChangeFunctionSignatureFix.createFactoryForParametersNumberMismatch());

        factories.put(EXPECTED_PARAMETER_TYPE_MISMATCH, ChangeTypeFix.createFactoryForExpectedParameterTypeMismatch());
        factories.put(EXPECTED_RETURN_TYPE_MISMATCH, ChangeTypeFix.createFactoryForExpectedReturnTypeMismatch());

        JetSingleIntentionActionFactory changeFunctionLiteralReturnTypeFix = ChangeFunctionLiteralReturnTypeFix.createFactoryForExpectedOrAssignmentTypeMismatch();
        factories.put(EXPECTED_TYPE_MISMATCH, changeFunctionLiteralReturnTypeFix);
        factories.put(ASSIGNMENT_TYPE_MISMATCH, changeFunctionLiteralReturnTypeFix);

        factories.put(TYPE_MISMATCH, new QuickFixFactoryForTypeMismatchError());

        factories.put(AUTOCAST_IMPOSSIBLE, CastExpressionFix.createFactoryForAutoCastImpossible());

        factories.put(PLATFORM_CLASS_MAPPED_TO_KOTLIN, MapPlatformClassToKotlinFix.createFactory());

        factories.put(MANY_CLASSES_IN_SUPERTYPE_LIST, RemoveSupertypeFix.createFactory());

        factories.put(NO_GET_METHOD, CreateFunctionFromUsageFix.object$.createCreateGetFunctionFromUsageFactory());
        factories.put(NO_SET_METHOD, CreateFunctionFromUsageFix.object$.createCreateSetFunctionFromUsageFactory());
        JetSingleIntentionActionFactory createHasNextFromUsageFactory = CreateFunctionFromUsageFix.object$.createCreateHasNextFunctionFromUsageFactory();
        factories.put(HAS_NEXT_MISSING, createHasNextFromUsageFactory);
        factories.put(HAS_NEXT_FUNCTION_NONE_APPLICABLE, createHasNextFromUsageFactory);
        JetSingleIntentionActionFactory createNextFromUsageFactory = CreateFunctionFromUsageFix.object$.createCreateNextFunctionFromUsageFactory();
        factories.put(NEXT_MISSING, createNextFromUsageFactory);
        factories.put(NEXT_NONE_APPLICABLE, createNextFromUsageFactory);
        factories.put(ITERATOR_MISSING, CreateFunctionFromUsageFix.object$.createCreateIteratorFunctionFromUsageFactory());
        factories.put(COMPONENT_FUNCTION_MISSING, CreateFunctionFromUsageFix.object$.createCreateComponentFunctionFromUsageFactory());
    }
}
