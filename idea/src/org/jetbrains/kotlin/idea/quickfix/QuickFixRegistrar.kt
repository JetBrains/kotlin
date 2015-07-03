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
import org.jetbrains.kotlin.idea.core.codeInsight.ImplementMethodsHandler
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromCallWithConstructorCalleeActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromConstructorCallActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromReferenceExpressionActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromTypeReferenceActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateLocalVariableActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterByNamedArgumentActionFactory
import org.jetbrains.kotlin.lexer.JetTokens.*
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

public class QuickFixRegistrar : QuickFixContributor {
    public override fun registerQuickFixes(quickFixes: QuickFixes) {
        fun DiagnosticFactory<*>.fixFactory(vararg factory: JetIntentionActionsFactory) {
            quickFixes.register(this, *factory)
        }

        fun DiagnosticFactory<*>.fixAction(vararg action: IntentionAction) {
            quickFixes.register(this, *action)
        }

        val removeAbstractModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD)
        val addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD)

        ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.fixFactory(removeAbstractModifierFactory)

        val removePartsFromPropertyFactory = RemovePartsFromPropertyFix.createFactory()
        ABSTRACT_PROPERTY_WITH_INITIALIZER.fixFactory(removeAbstractModifierFactory, removePartsFromPropertyFactory)
        ABSTRACT_PROPERTY_WITH_GETTER.fixFactory(removeAbstractModifierFactory, removePartsFromPropertyFactory)
        ABSTRACT_PROPERTY_WITH_SETTER.fixFactory(removeAbstractModifierFactory, removePartsFromPropertyFactory)

        PROPERTY_INITIALIZER_IN_TRAIT.fixFactory(removePartsFromPropertyFactory)

        MUST_BE_INITIALIZED_OR_BE_ABSTRACT.fixFactory(addAbstractModifierFactory)
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.fixFactory(addAbstractModifierFactory)
        MANY_IMPL_MEMBER_NOT_IMPLEMENTED.fixFactory(addAbstractModifierFactory)

        val removeFinalModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(FINAL_KEYWORD)
        val addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, javaClass<JetClass>())
        ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.fixFactory(removeAbstractModifierFactory, addAbstractToClassFactory)

        ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.fixFactory(removeAbstractModifierFactory, addAbstractToClassFactory)

        val removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory()
        ABSTRACT_FUNCTION_WITH_BODY.fixFactory(removeAbstractModifierFactory, removeFunctionBodyFactory)

        FINAL_PROPERTY_IN_TRAIT.fixFactory(removeFinalModifierFactory)
        FINAL_FUNCTION_WITH_NO_BODY.fixFactory(removeFinalModifierFactory)

        val addFunctionBodyFactory = AddFunctionBodyFix.createFactory()
        NON_ABSTRACT_FUNCTION_WITH_NO_BODY.fixFactory(addAbstractModifierFactory, addFunctionBodyFactory)

        NON_VARARG_SPREAD.fixFactory(RemovePsiElementSimpleFix.createRemoveSpreadFactory())

        MIXING_NAMED_AND_POSITIONED_ARGUMENTS.fixFactory(AddNameToArgumentFix.createFactory())

        NON_MEMBER_FUNCTION_NO_BODY.fixFactory(addFunctionBodyFactory)

        NOTHING_TO_OVERRIDE.fixFactory( RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD),
                                        ChangeMemberFunctionSignatureFix.createFactory(),
                                        AddFunctionToSupertypeFix.createFactory())
        VIRTUAL_MEMBER_HIDDEN.fixFactory(AddModifierFix.createFactory(OVERRIDE_KEYWORD))

        USELESS_CAST.fixFactory(RemoveRightPartOfBinaryExpressionFix.createRemoveTypeFromBinaryExpressionFactory("Remove cast"))
        DEPRECATED_STATIC_ASSERT.fixFactory(RemoveRightPartOfBinaryExpressionFix.createRemoveTypeFromBinaryExpressionFactory("Remove static type assertion"))

        val changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory()
        WRONG_SETTER_PARAMETER_TYPE.fixFactory(changeAccessorTypeFactory)
        WRONG_GETTER_RETURN_TYPE.fixFactory(changeAccessorTypeFactory)

        USELESS_ELVIS.fixFactory(RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory())

        val removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true)
        REDUNDANT_MODIFIER.fixFactory(removeRedundantModifierFactory)
        ABSTRACT_MODIFIER_IN_TRAIT.fixFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD, true))
        OPEN_MODIFIER_IN_TRAIT.fixFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD, true))
        TRAIT_CAN_NOT_BE_FINAL.fixFactory(removeFinalModifierFactory)
        DEPRECATED_TRAIT_KEYWORD.fixFactory(DeprecatedTraitSyntaxFix, DeprecatedTraitSyntaxFix.createWholeProjectFixFactory())

        REDUNDANT_PROJECTION.fixFactory(RemoveModifierFix.createRemoveProjectionFactory(true))
        INCOMPATIBLE_MODIFIERS.fixFactory(RemoveModifierFix.createRemoveModifierFactory(false))
        VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY.fixFactory(RemoveModifierFix.createRemoveVarianceFactory())

        val removeOpenModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD)
        NON_FINAL_MEMBER_IN_FINAL_CLASS.fixFactory(AddModifierFix.createFactory(OPEN_KEYWORD, javaClass<JetClass>()),
                                                   removeOpenModifierFactory)

        val removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory()
        GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY.fixFactory(removeModifierFactory)
        REDUNDANT_MODIFIER_IN_GETTER.fixFactory(removeRedundantModifierFactory)
        ILLEGAL_MODIFIER.fixFactory(removeModifierFactory)
        REPEATED_MODIFIER.fixFactory(removeModifierFactory)

        val removeInnerModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(INNER_KEYWORD)
        INNER_CLASS_IN_TRAIT.fixFactory(removeInnerModifierFactory)
        INNER_CLASS_IN_OBJECT.fixFactory(removeInnerModifierFactory)

        val changeToBackingFieldFactory = ChangeToBackingFieldFix.createFactory()
        INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER.fixFactory(changeToBackingFieldFactory)
        INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER.fixFactory(changeToBackingFieldFactory)

        val changeToPropertyNameFactory = ChangeToPropertyNameFix.createFactory()
        NO_BACKING_FIELD_ABSTRACT_PROPERTY.fixFactory(changeToPropertyNameFactory)
        NO_BACKING_FIELD_CUSTOM_ACCESSORS.fixFactory(changeToPropertyNameFactory)
        INACCESSIBLE_BACKING_FIELD.fixFactory(changeToPropertyNameFactory)

        val unresolvedReferenceFactory = AutoImportFix.createFactory()
        UNRESOLVED_REFERENCE.fixFactory(unresolvedReferenceFactory)
        UNRESOLVED_REFERENCE_WRONG_RECEIVER.fixFactory(unresolvedReferenceFactory)

        val removeImportFixFactory = RemovePsiElementSimpleFix.createRemoveImportFactory()
        CONFLICTING_IMPORT.fixFactory(removeImportFixFactory)

        SUPERTYPE_NOT_INITIALIZED.fixFactory(SuperClassNotInitialized)
        FUNCTION_CALL_EXPECTED.fixFactory(ChangeToFunctionInvocationFix.createFactory())

        CANNOT_CHANGE_ACCESS_PRIVILEGE.fixFactory(ChangeVisibilityModifierFix.createFactory())
        CANNOT_WEAKEN_ACCESS_PRIVILEGE.fixFactory(ChangeVisibilityModifierFix.createFactory())

        REDUNDANT_NULLABLE.fixFactory(RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.REDUNDANT))
        NULLABLE_SUPERTYPE.fixFactory(RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.SUPERTYPE))
        USELESS_NULLABLE_CHECK.fixFactory(RemoveNullableFix.createFactory(RemoveNullableFix.NullableKind.USELESS))


        val implementMethodsHandler = ImplementMethodsHandler()
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.fixAction(implementMethodsHandler)
        MANY_IMPL_MEMBER_NOT_IMPLEMENTED.fixAction(implementMethodsHandler)

        VAL_WITH_SETTER.fixFactory(ChangeVariableMutabilityFix.VAL_WITH_SETTER_FACTORY)
        VAL_REASSIGNMENT.fixFactory(ChangeVariableMutabilityFix.VAL_REASSIGNMENT_FACTORY)
        VAR_OVERRIDDEN_BY_VAL.fixFactory(ChangeVariableMutabilityFix.VAR_OVERRIDDEN_BY_VAL_FACTORY)

        val removeValVarFromParameterFixFactory = RemoveValVarFromParameterFix.createFactory()
        VAL_OR_VAR_ON_FUN_PARAMETER.fixFactory(removeValVarFromParameterFixFactory)
        VAL_OR_VAR_ON_LOOP_PARAMETER.fixFactory(removeValVarFromParameterFixFactory)
        VAL_OR_VAR_ON_CATCH_PARAMETER.fixFactory(removeValVarFromParameterFixFactory)
        VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER.fixFactory(removeValVarFromParameterFixFactory)

        VIRTUAL_MEMBER_HIDDEN.fixFactory(AddOverrideToEqualsHashCodeToStringFix.createFactory())

        UNUSED_VARIABLE.fixFactory(RemovePsiElementSimpleFix.createRemoveVariableFactory())

        UNNECESSARY_SAFE_CALL.fixFactory(ReplaceWithDotCallFix)
        UNSAFE_CALL.fixFactory(ReplaceWithSafeCallFix)

        UNSAFE_CALL.fixFactory(AddExclExclCallFix)
        UNNECESSARY_NOT_NULL_ASSERTION.fixFactory(RemoveExclExclCallFix)
        UNSAFE_INFIX_CALL.fixFactory(ReplaceInfixCallFix.createFactory())

        val removeProtectedModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(PROTECTED_KEYWORD)
        PACKAGE_MEMBER_CANNOT_BE_PROTECTED.fixFactory(removeProtectedModifierFactory)

        PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE.fixAction(SpecifyTypeExplicitlyFix())
        AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.fixAction(SpecifyTypeExplicitlyFix())

        ELSE_MISPLACED_IN_WHEN.fixFactory(MoveWhenElseBranchFix.createFactory())
        NO_ELSE_IN_WHEN.fixFactory(AddWhenElseBranchFix.createFactory())
        BREAK_OR_CONTINUE_IN_WHEN.fixFactory(AddLoopLabelFix)

        NO_TYPE_ARGUMENTS_ON_RHS.fixFactory(AddStarProjectionsFix.createFactoryForIsExpression())
        WRONG_NUMBER_OF_TYPE_ARGUMENTS.fixFactory(AddStarProjectionsFix.createFactoryForJavaClass())

        TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER.fixFactory(RemovePsiElementSimpleFix.createRemoveTypeArgumentsFactory())

        val changeToStarProjectionFactory = ChangeToStarProjectionFix.createFactory()
        UNCHECKED_CAST.fixFactory(changeToStarProjectionFactory)
        CANNOT_CHECK_FOR_ERASED.fixFactory(changeToStarProjectionFactory)

        INACCESSIBLE_OUTER_CLASS_EXPRESSION.fixFactory(AddModifierFix.createFactory(INNER_KEYWORD, javaClass<JetClass>()))

        val addOpenModifierToClassDeclarationFix = AddOpenModifierToClassDeclarationFix.createFactory()
        FINAL_SUPERTYPE.fixFactory(addOpenModifierToClassDeclarationFix)
        FINAL_UPPER_BOUND.fixFactory(addOpenModifierToClassDeclarationFix)

        OVERRIDING_FINAL_MEMBER.fixFactory(MakeOverriddenMemberOpenFix.createFactory())

        PARAMETER_NAME_CHANGED_ON_OVERRIDE.fixFactory(RenameParameterToMatchOverriddenMethodFix.createFactory())

        OPEN_MODIFIER_IN_ENUM.fixFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD))
        ABSTRACT_MODIFIER_IN_ENUM.fixFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD))
        ILLEGAL_ENUM_ANNOTATION.fixFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ENUM_KEYWORD))

        NESTED_CLASS_NOT_ALLOWED.fixFactory(AddModifierFix.createFactory(INNER_KEYWORD))

        CONFLICTING_PROJECTION.fixFactory(RemoveModifierFix.createRemoveProjectionFactory(false))
        PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.fixFactory(RemoveModifierFix.createRemoveProjectionFactory(false))
        PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.fixFactory(RemoveModifierFix.createRemoveProjectionFactory(false))

        NOT_AN_ANNOTATION_CLASS.fixFactory(MakeClassAnAnnotationClassFix.createFactory())

        val changeVariableTypeFix = ChangeVariableTypeFix.createFactoryForPropertyOrReturnTypeMismatchOnOverride()
        RETURN_TYPE_MISMATCH_ON_OVERRIDE.fixFactory(changeVariableTypeFix)
        PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.fixFactory(changeVariableTypeFix)
        COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.fixFactory(ChangeVariableTypeFix.createFactoryForComponentFunctionReturnTypeMismatch())

        val changeFunctionReturnTypeFix = ChangeFunctionReturnTypeFix.createFactoryForChangingReturnTypeToUnit()
        RETURN_TYPE_MISMATCH.fixFactory(changeFunctionReturnTypeFix)
        NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.fixFactory(changeFunctionReturnTypeFix)
        RETURN_TYPE_MISMATCH_ON_OVERRIDE.fixFactory(ChangeFunctionReturnTypeFix.createFactoryForReturnTypeMismatchOnOverride())
        COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.fixFactory(ChangeFunctionReturnTypeFix.createFactoryForComponentFunctionReturnTypeMismatch())
        HAS_NEXT_FUNCTION_TYPE_MISMATCH.fixFactory(ChangeFunctionReturnTypeFix.createFactoryForHasNextFunctionTypeMismatch())
        COMPARE_TO_TYPE_MISMATCH.fixFactory(ChangeFunctionReturnTypeFix.createFactoryForCompareToTypeMismatch())

        TOO_MANY_ARGUMENTS.fixFactory(ChangeFunctionSignatureFix.createFactory())
        NO_VALUE_FOR_PARAMETER.fixFactory(ChangeFunctionSignatureFix.createFactory())
        UNUSED_PARAMETER.fixFactory(ChangeFunctionSignatureFix.createFactoryForUnusedParameter())
        EXPECTED_PARAMETERS_NUMBER_MISMATCH.fixFactory(ChangeFunctionSignatureFix.createFactoryForParametersNumberMismatch())
        DEPRECATED_LAMBDA_SYNTAX.fixFactory(DeprecatedLambdaSyntaxFix, DeprecatedLambdaSyntaxFix.createWholeProjectFixFactory())

        EXPECTED_PARAMETER_TYPE_MISMATCH.fixFactory(ChangeTypeFix.createFactoryForExpectedParameterTypeMismatch())
        EXPECTED_RETURN_TYPE_MISMATCH.fixFactory(ChangeTypeFix.createFactoryForExpectedReturnTypeMismatch())

        val changeFunctionLiteralReturnTypeFix = ChangeFunctionLiteralReturnTypeFix.createFactoryForExpectedOrAssignmentTypeMismatch()
        EXPECTED_TYPE_MISMATCH.fixFactory(changeFunctionLiteralReturnTypeFix)
        ASSIGNMENT_TYPE_MISMATCH.fixFactory(changeFunctionLiteralReturnTypeFix)

        UNRESOLVED_REFERENCE.fixFactory(CreateUnaryOperationActionFactory)
        UNRESOLVED_REFERENCE_WRONG_RECEIVER.fixFactory(CreateUnaryOperationActionFactory)
        NO_VALUE_FOR_PARAMETER.fixFactory(CreateUnaryOperationActionFactory)

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.fixFactory(CreateBinaryOperationActionFactory)
        UNRESOLVED_REFERENCE.fixFactory(CreateBinaryOperationActionFactory)
        NONE_APPLICABLE.fixFactory(CreateBinaryOperationActionFactory)
        NO_VALUE_FOR_PARAMETER.fixFactory(CreateBinaryOperationActionFactory)
        TOO_MANY_ARGUMENTS.fixFactory(CreateBinaryOperationActionFactory)

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.fixFactory(CreateCallableFromCallActionFactory)
        UNRESOLVED_REFERENCE.fixFactory(CreateCallableFromCallActionFactory)
        NO_VALUE_FOR_PARAMETER.fixFactory(CreateCallableFromCallActionFactory)
        TOO_MANY_ARGUMENTS.fixFactory(CreateCallableFromCallActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.fixFactory(CreateCallableFromCallActionFactory)

        NO_VALUE_FOR_PARAMETER.fixFactory(CreateConstructorFromDelegationCallActionFactory)
        TOO_MANY_ARGUMENTS.fixFactory(CreateConstructorFromDelegationCallActionFactory)

        NO_VALUE_FOR_PARAMETER.fixFactory(CreateConstructorFromDelegatorToSuperCallActionFactory)
        TOO_MANY_ARGUMENTS.fixFactory(CreateConstructorFromDelegatorToSuperCallActionFactory)

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.fixFactory(CreateClassFromConstructorCallActionFactory)
        UNRESOLVED_REFERENCE.fixFactory(CreateClassFromConstructorCallActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.fixFactory(CreateClassFromConstructorCallActionFactory)

        UNRESOLVED_REFERENCE.fixFactory(CreateLocalVariableActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.fixFactory(CreateLocalVariableActionFactory)

        UNRESOLVED_REFERENCE.fixFactory(CreateParameterActionFactory)
        UNRESOLVED_REFERENCE_WRONG_RECEIVER.fixFactory(CreateParameterActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.fixFactory(CreateParameterActionFactory)

        NAMED_PARAMETER_NOT_FOUND.fixFactory(CreateParameterByNamedArgumentActionFactory)

        FUNCTION_EXPECTED.fixFactory(CreateInvokeFunctionActionFactory)

        val factoryForTypeMismatchError = QuickFixFactoryForTypeMismatchError()
        TYPE_MISMATCH.fixFactory(factoryForTypeMismatchError)
        NULL_FOR_NONNULL_TYPE.fixFactory(factoryForTypeMismatchError)
        CONSTANT_EXPECTED_TYPE_MISMATCH.fixFactory(factoryForTypeMismatchError)

        SMARTCAST_IMPOSSIBLE.fixFactory(CastExpressionFix.createFactoryForSmartCastImpossible())

        PLATFORM_CLASS_MAPPED_TO_KOTLIN.fixFactory(MapPlatformClassToKotlinFix.createFactory())

        MANY_CLASSES_IN_SUPERTYPE_LIST.fixFactory(RemoveSupertypeFix.createFactory())

        NO_GET_METHOD.fixFactory(CreateGetFunctionActionFactory)
        NO_SET_METHOD.fixFactory(CreateSetFunctionActionFactory)
        HAS_NEXT_MISSING.fixFactory(CreateHasNextFunctionActionFactory)
        HAS_NEXT_FUNCTION_NONE_APPLICABLE.fixFactory(CreateHasNextFunctionActionFactory)
        NEXT_MISSING.fixFactory(CreateNextFunctionActionFactory)
        NEXT_NONE_APPLICABLE.fixFactory(CreateNextFunctionActionFactory)
        ITERATOR_MISSING.fixFactory(CreateIteratorFunctionActionFactory)
        COMPONENT_FUNCTION_MISSING.fixFactory(CreateComponentFunctionActionFactory)

        DELEGATE_SPECIAL_FUNCTION_MISSING.fixFactory(CreatePropertyDelegateAccessorsActionFactory)
        DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE.fixFactory(CreatePropertyDelegateAccessorsActionFactory)

        UNRESOLVED_REFERENCE.fixFactory(CreateClassFromTypeReferenceActionFactory,
                                        CreateClassFromReferenceExpressionActionFactory,
                                        CreateClassFromCallWithConstructorCalleeActionFactory)

        PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.fixFactory(InsertDelegationCallQuickfix.InsertThisDelegationCallFactory)

        EXPLICIT_DELEGATION_CALL_REQUIRED.fixFactory(InsertDelegationCallQuickfix.InsertThisDelegationCallFactory)
        EXPLICIT_DELEGATION_CALL_REQUIRED.fixFactory(InsertDelegationCallQuickfix.InsertSuperDelegationCallFactory)

        ErrorsJvm.DEPRECATED_ANNOTATION_METHOD_CALL.fixFactory(MigrateAnnotationMethodCallFix,
                                                               MigrateAnnotationMethodCallInWholeFile)

        ENUM_ENTRY_USES_DEPRECATED_SUPER_CONSTRUCTOR.fixFactory(DeprecatedEnumEntrySuperConstructorSyntaxFix,
                                                                DeprecatedEnumEntrySuperConstructorSyntaxFix.createWholeProjectFixFactory())

        ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER.fixFactory(DeprecatedEnumEntryDelimiterSyntaxFix,
                                                              DeprecatedEnumEntryDelimiterSyntaxFix.createWholeProjectFixFactory())

        MISSING_CONSTRUCTOR_KEYWORD.fixFactory(MissingConstructorKeywordFix,
                                               MissingConstructorKeywordFix.createWholeProjectFixFactory())

        FUNCTION_EXPRESSION_WITH_NAME.fixFactory(RemoveNameFromFunctionExpressionFix,
                                                 RemoveNameFromFunctionExpressionFix.createWholeProjectFixFactory())

        UNRESOLVED_REFERENCE.fixFactory(ReplaceObsoleteLabelSyntaxFix,
                                        ReplaceObsoleteLabelSyntaxFix.createWholeProjectFixFactory())

        DEPRECATED_SYMBOL_WITH_MESSAGE.fixFactory(DeprecatedSymbolUsageFix,
                                                  DeprecatedSymbolUsageInWholeProjectFix)

        ErrorsJvm.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION.fixFactory(ReplaceJavaAnnotationPositionedArgumentsFix)
    }
}
