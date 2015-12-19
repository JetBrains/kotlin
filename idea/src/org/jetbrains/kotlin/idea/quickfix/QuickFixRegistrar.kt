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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.inspections.AddModifierFixFactory
import org.jetbrains.kotlin.idea.inspections.AddReflectionQuickFix
import org.jetbrains.kotlin.idea.inspections.InfixCallFix
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromCallWithConstructorCalleeActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromConstructorCallActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromReferenceExpressionActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromTypeReferenceActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateLocalVariableActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterByNamedArgumentActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterByRefActionFactory
import org.jetbrains.kotlin.idea.quickfix.migration.MigrateTypeParameterListFix
import org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
import org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageInWholeProjectFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.NO_REFLECTION_IN_CLASS_PATH
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION

public class QuickFixRegistrar : QuickFixContributor {
    public override fun registerQuickFixes(quickFixes: QuickFixes) {
        fun DiagnosticFactory<*>.registerFactory(vararg factory: KotlinIntentionActionsFactory) {
            quickFixes.register(this, *factory)
        }

        fun DiagnosticFactory<*>.registerActions(vararg action: IntentionAction) {
            quickFixes.register(this, *action)
        }

        val removeAbstractModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD)
        val addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD)

        ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.registerFactory(removeAbstractModifierFactory)

        val removePartsFromPropertyFactory = RemovePartsFromPropertyFix.createFactory()
        ABSTRACT_PROPERTY_WITH_INITIALIZER.registerFactory(removeAbstractModifierFactory, removePartsFromPropertyFactory)
        ABSTRACT_PROPERTY_WITH_GETTER.registerFactory(removeAbstractModifierFactory, removePartsFromPropertyFactory)
        ABSTRACT_PROPERTY_WITH_SETTER.registerFactory(removeAbstractModifierFactory, removePartsFromPropertyFactory)

        PROPERTY_INITIALIZER_IN_INTERFACE.registerFactory(removePartsFromPropertyFactory)

        MUST_BE_INITIALIZED_OR_BE_ABSTRACT.registerFactory(addAbstractModifierFactory)
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerFactory(addAbstractModifierFactory)
        ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.registerFactory(addAbstractModifierFactory)

        MUST_BE_INITIALIZED_OR_BE_ABSTRACT.registerFactory(InitializePropertyQuickFixFactory)
        MUST_BE_INITIALIZED.registerFactory(InitializePropertyQuickFixFactory)

        val addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, KtClass::class.java)
        ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.registerFactory(removeAbstractModifierFactory, addAbstractToClassFactory)

        ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.registerFactory(removeAbstractModifierFactory, addAbstractToClassFactory)

        val removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory()
        ABSTRACT_FUNCTION_WITH_BODY.registerFactory(removeAbstractModifierFactory, removeFunctionBodyFactory)

        val addFunctionBodyFactory = AddFunctionBodyFix.createFactory()
        NON_ABSTRACT_FUNCTION_WITH_NO_BODY.registerFactory(addAbstractModifierFactory, addFunctionBodyFactory)

        NON_VARARG_SPREAD.registerFactory(RemovePsiElementSimpleFix.createRemoveSpreadFactory())

        MIXING_NAMED_AND_POSITIONED_ARGUMENTS.registerFactory(AddNameToArgumentFix)

        NON_MEMBER_FUNCTION_NO_BODY.registerFactory(addFunctionBodyFactory)

        NOTHING_TO_OVERRIDE.registerFactory( RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD),
                                        ChangeMemberFunctionSignatureFix,
                                        AddFunctionToSupertypeFix)
        VIRTUAL_MEMBER_HIDDEN.registerFactory(AddModifierFix.createFactory(OVERRIDE_KEYWORD))

        USELESS_CAST.registerFactory(RemoveRightPartOfBinaryExpressionFix.createRemoveTypeFromBinaryExpressionFactory("Remove cast"))

        val changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory()
        WRONG_SETTER_PARAMETER_TYPE.registerFactory(changeAccessorTypeFactory)
        WRONG_GETTER_RETURN_TYPE.registerFactory(changeAccessorTypeFactory)

        USELESS_ELVIS.registerFactory(RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory())

        val removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true)
        REDUNDANT_MODIFIER.registerFactory(removeRedundantModifierFactory)
        ABSTRACT_MODIFIER_IN_INTERFACE.registerFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD, true))

        REDUNDANT_PROJECTION.registerFactory(RemoveModifierFix.createRemoveProjectionFactory(true))
        INCOMPATIBLE_MODIFIERS.registerFactory(RemoveModifierFix.createRemoveModifierFactory(false))
        VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY.registerFactory(RemoveModifierFix.createRemoveVarianceFactory())

        val removeOpenModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD)
        NON_FINAL_MEMBER_IN_FINAL_CLASS.registerFactory(AddModifierFix.createFactory(OPEN_KEYWORD, KtClass::class.java),
                                                        removeOpenModifierFactory)

        val removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory()
        GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY.registerFactory(removeModifierFactory)
        SETTER_VISIBILITY_DIFFERS_FROM_LATEINIT_VISIBILITY.registerFactory(removeModifierFactory)
        SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY.registerFactory(removeModifierFactory)
        PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY.registerFactory(removeModifierFactory)
        PRIVATE_SETTER_FOR_OPEN_PROPERTY.registerFactory(removeModifierFactory)
        REDUNDANT_MODIFIER_IN_GETTER.registerFactory(removeRedundantModifierFactory)
        WRONG_MODIFIER_TARGET.registerFactory(removeModifierFactory)
        REDUNDANT_MODIFIER_FOR_TARGET.registerFactory(removeModifierFactory)
        WRONG_MODIFIER_CONTAINING_DECLARATION.registerFactory(removeModifierFactory)
        REPEATED_MODIFIER.registerFactory(removeModifierFactory)

        UNRESOLVED_REFERENCE.registerFactory(AutoImportFix)
        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(AutoImportFix)

        FUNCTION_EXPECTED.registerFactory(MissingInvokeAutoImportFix)

        DELEGATE_SPECIAL_FUNCTION_MISSING.registerFactory(MissingDelegateAccessorsAutoImportFix)
        COMPONENT_FUNCTION_MISSING.registerFactory(MissingComponentsAutoImportFix)

        NO_GET_METHOD.registerFactory(MissingArrayAccessorAutoImportFix)
        NO_SET_METHOD.registerFactory(MissingArrayAccessorAutoImportFix)

        val removeImportFixFactory = RemovePsiElementSimpleFix.createRemoveImportFactory()
        CONFLICTING_IMPORT.registerFactory(removeImportFixFactory)

        SUPERTYPE_NOT_INITIALIZED.registerFactory(SuperClassNotInitialized)
        FUNCTION_CALL_EXPECTED.registerFactory(ChangeToFunctionInvocationFix.createFactory())

        CANNOT_CHANGE_ACCESS_PRIVILEGE.registerFactory(ChangeVisibilityModifierFix.createFactory())
        CANNOT_WEAKEN_ACCESS_PRIVILEGE.registerFactory(ChangeVisibilityModifierFix.createFactory())

        INVISIBLE_REFERENCE.registerFactory(ChangePrivateTopLevelToInternalFix)
        INVISIBLE_MEMBER.registerFactory(ChangePrivateTopLevelToInternalFix)
        INVISIBLE_SETTER.registerFactory(ChangePrivateTopLevelToInternalFix)

        REDUNDANT_NULLABLE.registerFactory(RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.REDUNDANT))
        NULLABLE_SUPERTYPE.registerFactory(RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.SUPERTYPE))
        USELESS_NULLABLE_CHECK.registerFactory(RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.USELESS))


        val implementMethodsHandler = ImplementMembersHandler()
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerActions(implementMethodsHandler)
        ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.registerActions(implementMethodsHandler)
        MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.registerActions(implementMethodsHandler)

        VAL_WITH_SETTER.registerFactory(ChangeVariableMutabilityFix.VAL_WITH_SETTER_FACTORY)
        VAL_REASSIGNMENT.registerFactory(ChangeVariableMutabilityFix.VAL_REASSIGNMENT_FACTORY)
        VAR_OVERRIDDEN_BY_VAL.registerFactory(ChangeVariableMutabilityFix.VAR_OVERRIDDEN_BY_VAL_FACTORY)

        val removeValVarFromParameterFixFactory = RemoveValVarFromParameterFix.createFactory()
        VAL_OR_VAR_ON_FUN_PARAMETER.registerFactory(removeValVarFromParameterFixFactory)
        VAL_OR_VAR_ON_LOOP_PARAMETER.registerFactory(removeValVarFromParameterFixFactory)
        VAL_OR_VAR_ON_LOOP_MULTI_PARAMETER.registerFactory(removeValVarFromParameterFixFactory)
        VAL_OR_VAR_ON_CATCH_PARAMETER.registerFactory(removeValVarFromParameterFixFactory)
        VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER.registerFactory(removeValVarFromParameterFixFactory)

        VIRTUAL_MEMBER_HIDDEN.registerFactory(AddOverrideToEqualsHashCodeToStringFix.createFactory())

        UNUSED_VARIABLE.registerFactory(RemovePsiElementSimpleFix.createRemoveVariableFactory())

        UNNECESSARY_SAFE_CALL.registerFactory(ReplaceWithDotCallFix)
        UNSAFE_CALL.registerFactory(ReplaceWithSafeCallFix)

        UNSAFE_CALL.registerFactory(AddExclExclCallFix)
        UNNECESSARY_NOT_NULL_ASSERTION.registerFactory(RemoveExclExclCallFix)
        UNSAFE_INFIX_CALL.registerFactory(ReplaceInfixCallFix)

        AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.registerActions(SpecifyTypeExplicitlyFix())

        ELSE_MISPLACED_IN_WHEN.registerFactory(MoveWhenElseBranchFix.createFactory())
        NO_ELSE_IN_WHEN.registerFactory(AddWhenElseBranchFix.createFactory())
        BREAK_OR_CONTINUE_IN_WHEN.registerFactory(AddLoopLabelFix)

        NO_TYPE_ARGUMENTS_ON_RHS.registerFactory(AddStarProjectionsFix.createFactoryForIsExpression())
        WRONG_NUMBER_OF_TYPE_ARGUMENTS.registerFactory(AddStarProjectionsFix.createFactoryForJavaClass())

        TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER.registerFactory(RemovePsiElementSimpleFix.createRemoveTypeArgumentsFactory())

        val changeToStarProjectionFactory = ChangeToStarProjectionFix.createFactory()
        UNCHECKED_CAST.registerFactory(changeToStarProjectionFactory)
        CANNOT_CHECK_FOR_ERASED.registerFactory(changeToStarProjectionFactory)

        INACCESSIBLE_OUTER_CLASS_EXPRESSION.registerFactory(AddModifierFix.createFactory(INNER_KEYWORD, KtClass::class.java))

        FINAL_SUPERTYPE.registerFactory(AddModifierFix.MakeClassOpenFactory)
        FINAL_UPPER_BOUND.registerFactory(AddModifierFix.MakeClassOpenFactory)

        OVERRIDING_FINAL_MEMBER.registerFactory(MakeOverriddenMemberOpenFix)

        PARAMETER_NAME_CHANGED_ON_OVERRIDE.registerFactory(RenameParameterToMatchOverriddenMethodFix.createFactory())

        NESTED_CLASS_NOT_ALLOWED.registerFactory(AddModifierFix.createFactory(INNER_KEYWORD))

        CONFLICTING_PROJECTION.registerFactory(RemoveModifierFix.createRemoveProjectionFactory(false))
        PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.registerFactory(RemoveModifierFix.createRemoveProjectionFactory(false))
        PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.registerFactory(RemoveModifierFix.createRemoveProjectionFactory(false))

        NOT_AN_ANNOTATION_CLASS.registerFactory(MakeClassAnAnnotationClassFix.createFactory())

        val changeVariableTypeFix = ChangeVariableTypeFix.createFactoryForPropertyOrReturnTypeMismatchOnOverride()
        RETURN_TYPE_MISMATCH_ON_OVERRIDE.registerFactory(changeVariableTypeFix)
        PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.registerFactory(changeVariableTypeFix)
        VAR_TYPE_MISMATCH_ON_OVERRIDE.registerFactory(changeVariableTypeFix)
        COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.registerFactory(ChangeVariableTypeFix.createFactoryForComponentFunctionReturnTypeMismatch())

        val changeFunctionReturnTypeFix = ChangeFunctionReturnTypeFix.createFactoryForChangingReturnTypeToUnit()
        RETURN_TYPE_MISMATCH.registerFactory(changeFunctionReturnTypeFix)
        NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.registerFactory(changeFunctionReturnTypeFix)
        RETURN_TYPE_MISMATCH_ON_OVERRIDE.registerFactory(ChangeFunctionReturnTypeFix.createFactoryForReturnTypeMismatchOnOverride())
        COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.registerFactory(ChangeFunctionReturnTypeFix.createFactoryForComponentFunctionReturnTypeMismatch())
        HAS_NEXT_FUNCTION_TYPE_MISMATCH.registerFactory(ChangeFunctionReturnTypeFix.createFactoryForHasNextFunctionTypeMismatch())
        COMPARE_TO_TYPE_MISMATCH.registerFactory(ChangeFunctionReturnTypeFix.createFactoryForCompareToTypeMismatch())
        IMPLICIT_NOTHING_RETURN_TYPE.registerFactory(ChangeFunctionReturnTypeFix.createFactoryForChangingReturnTypeToNothing())

        TOO_MANY_ARGUMENTS.registerFactory(ChangeFunctionSignatureFix)
        NO_VALUE_FOR_PARAMETER.registerFactory(ChangeFunctionSignatureFix)
        UNUSED_PARAMETER.registerFactory(RemoveUnusedFunctionParameterFix)
        EXPECTED_PARAMETERS_NUMBER_MISMATCH.registerFactory(ChangeFunctionLiteralSignatureFix)

        EXPECTED_PARAMETER_TYPE_MISMATCH.registerFactory(ChangeTypeFix.createFactoryForExpectedParameterTypeMismatch())

        EXTENSION_PROPERTY_WITH_BACKING_FIELD.registerFactory(ConvertExtensionPropertyInitializerToGetterFix)

        val changeFunctionLiteralReturnTypeFix = ChangeFunctionLiteralReturnTypeFix.createFactoryForExpectedOrAssignmentTypeMismatch()
        EXPECTED_TYPE_MISMATCH.registerFactory(changeFunctionLiteralReturnTypeFix)
        ASSIGNMENT_TYPE_MISMATCH.registerFactory(changeFunctionLiteralReturnTypeFix)

        UNRESOLVED_REFERENCE.registerFactory(CreateUnaryOperationActionFactory)
        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(CreateUnaryOperationActionFactory)
        NO_VALUE_FOR_PARAMETER.registerFactory(CreateUnaryOperationActionFactory)

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(CreateBinaryOperationActionFactory)
        UNRESOLVED_REFERENCE.registerFactory(CreateBinaryOperationActionFactory)
        NONE_APPLICABLE.registerFactory(CreateBinaryOperationActionFactory)
        NO_VALUE_FOR_PARAMETER.registerFactory(CreateBinaryOperationActionFactory)
        TOO_MANY_ARGUMENTS.registerFactory(CreateBinaryOperationActionFactory)

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        UNRESOLVED_REFERENCE.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        UNRESOLVED_REFERENCE.registerFactory(CreateFunctionFromCallableReferenceActionFactory)
        NO_VALUE_FOR_PARAMETER.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        TOO_MANY_ARGUMENTS.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)

        NO_VALUE_FOR_PARAMETER.registerFactory(CreateConstructorFromDelegationCallActionFactory)
        TOO_MANY_ARGUMENTS.registerFactory(CreateConstructorFromDelegationCallActionFactory)

        NO_VALUE_FOR_PARAMETER.registerFactory(CreateConstructorFromSuperTypeCallActionFactory)
        TOO_MANY_ARGUMENTS.registerFactory(CreateConstructorFromSuperTypeCallActionFactory)

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(CreateClassFromConstructorCallActionFactory)
        UNRESOLVED_REFERENCE.registerFactory(CreateClassFromConstructorCallActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.registerFactory(CreateClassFromConstructorCallActionFactory)

        UNRESOLVED_REFERENCE.registerFactory(CreateLocalVariableActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.registerFactory(CreateLocalVariableActionFactory)

        UNRESOLVED_REFERENCE.registerFactory(CreateParameterByRefActionFactory)
        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(CreateParameterByRefActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.registerFactory(CreateParameterByRefActionFactory)

        NAMED_PARAMETER_NOT_FOUND.registerFactory(CreateParameterByNamedArgumentActionFactory)

        FUNCTION_EXPECTED.registerFactory(CreateInvokeFunctionActionFactory)

        val factoryForTypeMismatchError = QuickFixFactoryForTypeMismatchError()
        TYPE_MISMATCH.registerFactory(factoryForTypeMismatchError)
        NULL_FOR_NONNULL_TYPE.registerFactory(factoryForTypeMismatchError)
        CONSTANT_EXPECTED_TYPE_MISMATCH.registerFactory(factoryForTypeMismatchError)

        SMARTCAST_IMPOSSIBLE.registerFactory(CastExpressionFix.createFactoryForSmartCastImpossible())

        PLATFORM_CLASS_MAPPED_TO_KOTLIN.registerFactory(MapPlatformClassToKotlinFix)

        MANY_CLASSES_IN_SUPERTYPE_LIST.registerFactory(RemoveSupertypeFix.createFactory())

        NO_GET_METHOD.registerFactory(CreateGetFunctionActionFactory)
        NO_SET_METHOD.registerFactory(CreateSetFunctionActionFactory)
        HAS_NEXT_MISSING.registerFactory(CreateHasNextFunctionActionFactory)
        HAS_NEXT_FUNCTION_NONE_APPLICABLE.registerFactory(CreateHasNextFunctionActionFactory)
        NEXT_MISSING.registerFactory(CreateNextFunctionActionFactory)
        NEXT_NONE_APPLICABLE.registerFactory(CreateNextFunctionActionFactory)
        ITERATOR_MISSING.registerFactory(CreateIteratorFunctionActionFactory)
        ITERATOR_MISSING.registerFactory(MissingIteratorExclExclFixFactory)
        COMPONENT_FUNCTION_MISSING.registerFactory(CreateComponentFunctionActionFactory)

        DELEGATE_SPECIAL_FUNCTION_MISSING.registerFactory(CreatePropertyDelegateAccessorsActionFactory)
        DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE.registerFactory(CreatePropertyDelegateAccessorsActionFactory)

        UNRESOLVED_REFERENCE.registerFactory(CreateClassFromTypeReferenceActionFactory,
                                        CreateClassFromReferenceExpressionActionFactory,
                                        CreateClassFromCallWithConstructorCalleeActionFactory)

        PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.registerFactory(InsertDelegationCallQuickfix.InsertThisDelegationCallFactory)

        EXPLICIT_DELEGATION_CALL_REQUIRED.registerFactory(InsertDelegationCallQuickfix.InsertThisDelegationCallFactory)
        EXPLICIT_DELEGATION_CALL_REQUIRED.registerFactory(InsertDelegationCallQuickfix.InsertSuperDelegationCallFactory)

        MISSING_CONSTRUCTOR_KEYWORD.registerFactory(MissingConstructorKeywordFix,
                                               MissingConstructorKeywordFix.createWholeProjectFixFactory())

        ANONYMOUS_FUNCTION_WITH_NAME.registerFactory(RemoveNameFromFunctionExpressionFix)

        UNRESOLVED_REFERENCE.registerFactory(ReplaceObsoleteLabelSyntaxFix,
                                        ReplaceObsoleteLabelSyntaxFix.createWholeProjectFixFactory())

        DEPRECATION.registerFactory(DeprecatedSymbolUsageFix, DeprecatedSymbolUsageInWholeProjectFix)
        DEPRECATION_ERROR.registerFactory(DeprecatedSymbolUsageFix, DeprecatedSymbolUsageInWholeProjectFix)

        POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION.registerFactory(ReplaceJavaAnnotationPositionedArgumentsFix)

        NO_REFLECTION_IN_CLASS_PATH.registerFactory(AddReflectionQuickFix)

        ErrorsJvm.JAVA_TYPE_MISMATCH.registerFactory(CastExpressionFix.createFactoryForGenericVarianceConversion())

        UPPER_BOUND_VIOLATED.registerFactory(AddGenericUpperBoundFix.Factory)
        TYPE_INFERENCE_UPPER_BOUND_VIOLATED.registerFactory(AddGenericUpperBoundFix.Factory)

        NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION.registerFactory(ConstFixFactory)

        OPERATOR_MODIFIER_REQUIRED.registerFactory(AddModifierFixFactory(KtTokens.OPERATOR_KEYWORD))
        OPERATOR_MODIFIER_REQUIRED.registerFactory(AutoImportForMissingOperatorFactory)

        INFIX_MODIFIER_REQUIRED.registerFactory(AddModifierFixFactory(KtTokens.INFIX_KEYWORD))
        INFIX_MODIFIER_REQUIRED.registerFactory(InfixCallFix)

        UNDERSCORE_IS_RESERVED.registerFactory(RenameUnderscoreFix)

        CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS.registerFactory(AddTypeToLHSOfCallableReferenceFix)

        DEPRECATED_TYPE_PARAMETER_SYNTAX.registerFactory(MigrateTypeParameterListFix)

        UNRESOLVED_REFERENCE.registerFactory(KotlinAddOrderEntryActionFactory)

        UNRESOLVED_REFERENCE.registerFactory(RenameUnresolvedReferenceActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.registerFactory(RenameUnresolvedReferenceActionFactory)

        MISPLACED_TYPE_PARAMETER_CONSTRAINTS.registerFactory(MoveTypeParameterConstraintFix)

        DELEGATE_RESOLVED_TO_DEPRECATED_CONVENTION.registerFactory(DeprecatedFunctionConventionFix)
        DEPRECATED_UNARY_PLUS_MINUS.registerFactory(DeprecatedFunctionConventionFix)

        COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT.registerFactory(CommaInWhenConditionWithoutArgumentFix)

        UNSUPPORTED.registerFactory(UnsupportedAsyncFix)
    }
}
