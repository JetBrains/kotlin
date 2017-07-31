/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementAsConstructorParameter
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.intentions.AddValVarToConstructorParameterAction
import org.jetbrains.kotlin.idea.intentions.ConvertPropertyInitializerToGetterIntention
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromCallWithConstructorCalleeActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromConstructorCallActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromReferenceExpressionActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromTypeReferenceActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeAlias.CreateTypeAliasFromTypeReferenceActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter.CreateTypeParameterByUnresolvedRefActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter.CreateTypeParameterUnmatchedTypeArgumentActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateLocalVariableActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterByNamedArgumentActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterByRefActionFactory
import org.jetbrains.kotlin.idea.quickfix.createImpl.CreateHeaderImplementationFix
import org.jetbrains.kotlin.idea.quickfix.migration.MigrateExternalExtensionFix
import org.jetbrains.kotlin.idea.quickfix.migration.MigrateTypeParameterListFix
import org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
import org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageInWholeProjectFix
import org.jetbrains.kotlin.idea.quickfix.replaceWith.ReplaceProtectedToPublishedApiCallFix
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.*

class QuickFixRegistrar : QuickFixContributor {
    override fun registerQuickFixes(quickFixes: QuickFixes) {
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

        PROPERTY_INITIALIZER_IN_INTERFACE.registerFactory(removePartsFromPropertyFactory, ConvertPropertyInitializerToGetterIntention)

        MUST_BE_INITIALIZED_OR_BE_ABSTRACT.registerFactory(addAbstractModifierFactory)
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerFactory(addAbstractModifierFactory)
        ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.registerFactory(addAbstractModifierFactory)

        MUST_BE_INITIALIZED_OR_BE_ABSTRACT.registerFactory(InitializePropertyQuickFixFactory)
        MUST_BE_INITIALIZED.registerFactory(InitializePropertyQuickFixFactory)

        val addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, KtClassOrObject::class.java)
        ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.registerFactory(removeAbstractModifierFactory, addAbstractToClassFactory)

        ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.registerFactory(removeAbstractModifierFactory, addAbstractToClassFactory)

        ABSTRACT_FUNCTION_WITH_BODY.registerFactory(removeAbstractModifierFactory, RemoveFunctionBodyFix)

        NON_ABSTRACT_FUNCTION_WITH_NO_BODY.registerFactory(addAbstractModifierFactory, AddFunctionBodyFix)

        NON_VARARG_SPREAD.registerFactory(RemovePsiElementSimpleFix.RemoveSpreadFactory)

        MIXING_NAMED_AND_POSITIONED_ARGUMENTS.registerFactory(AddNameToArgumentFix)

        NON_MEMBER_FUNCTION_NO_BODY.registerFactory(AddFunctionBodyFix)

        NOTHING_TO_OVERRIDE.registerFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD),
                                            ChangeMemberFunctionSignatureFix,
                                            AddFunctionToSupertypeFix)
        VIRTUAL_MEMBER_HIDDEN.registerFactory(AddModifierFix.createFactory(OVERRIDE_KEYWORD))

        USELESS_CAST.registerFactory(RemoveUselessCastFix)

        USELESS_IS_CHECK.registerFactory(RemoveUselessIsCheckFix, RemoveUselessIsCheckFixForWhen)

        WRONG_SETTER_PARAMETER_TYPE.registerFactory(ChangeAccessorTypeFix)
        WRONG_GETTER_RETURN_TYPE.registerFactory(ChangeAccessorTypeFix)

        USELESS_ELVIS.registerFactory(RemoveUselessElvisFix)
        USELESS_ELVIS_RIGHT_IS_NULL.registerFactory(RemoveUselessElvisFix)

        val removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true)
        REDUNDANT_MODIFIER.registerFactory(removeRedundantModifierFactory)
        REDUNDANT_OPEN_IN_INTERFACE.registerFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD, true))
        UNNECESSARY_LATEINIT.registerFactory(RemoveModifierFix.createRemoveModifierFromListOwnerFactory(LATEINIT_KEYWORD))

        REDUNDANT_PROJECTION.registerFactory(RemoveModifierFix.createRemoveProjectionFactory(true))
        INCOMPATIBLE_MODIFIERS.registerFactory(RemoveModifierFix.createRemoveModifierFactory(false))
        VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED.registerFactory(RemoveModifierFix.createRemoveVarianceFactory())

        val removeOpenModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD)
        NON_FINAL_MEMBER_IN_FINAL_CLASS.registerFactory(AddModifierFix.createFactory(OPEN_KEYWORD, KtClass::class.java),
                                                        removeOpenModifierFactory)
        NON_FINAL_MEMBER_IN_OBJECT.registerFactory(removeOpenModifierFactory)

        val removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory()
        GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY.registerFactory(removeModifierFactory)
        SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY.registerFactory(removeModifierFactory)
        PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY.registerFactory(removeModifierFactory)
        PRIVATE_SETTER_FOR_OPEN_PROPERTY.registerFactory(AddModifierFix.createFactory(FINAL_KEYWORD, KtProperty::class.java),
                                                         removeModifierFactory)
        REDUNDANT_MODIFIER_IN_GETTER.registerFactory(removeRedundantModifierFactory)
        WRONG_MODIFIER_TARGET.registerFactory(removeModifierFactory)
        REDUNDANT_MODIFIER_FOR_TARGET.registerFactory(removeModifierFactory)
        WRONG_MODIFIER_CONTAINING_DECLARATION.registerFactory(removeModifierFactory)
        REPEATED_MODIFIER.registerFactory(removeModifierFactory)
        NON_PRIVATE_CONSTRUCTOR_IN_ENUM.registerFactory(removeModifierFactory)
        NON_PRIVATE_CONSTRUCTOR_IN_SEALED.registerFactory(removeModifierFactory)

        DEPRECATED_BINARY_MOD.registerFactory(removeModifierFactory)
        DEPRECATED_BINARY_MOD.registerFactory(RenameModToRemFix.Factory)


        UNRESOLVED_REFERENCE.registerFactory(ImportFix)
        UNRESOLVED_REFERENCE.registerFactory(ImportConstructorReferenceFix)

        TOO_MANY_ARGUMENTS.registerFactory(ImportForMismatchingArgumentsFix)
        NO_VALUE_FOR_PARAMETER.registerFactory(ImportForMismatchingArgumentsFix)
        TYPE_MISMATCH.registerFactory(ImportForMismatchingArgumentsFix)
        CONSTANT_EXPECTED_TYPE_MISMATCH.registerFactory(ImportForMismatchingArgumentsFix)
        NAMED_PARAMETER_NOT_FOUND.registerFactory(ImportForMismatchingArgumentsFix)
        NONE_APPLICABLE.registerFactory(ImportForMismatchingArgumentsFix)

        UNRESOLVED_REFERENCE.registerFactory(AddTestLibQuickFix)

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(ImportFix)

        FUNCTION_EXPECTED.registerFactory(InvokeImportFix)

        DELEGATE_SPECIAL_FUNCTION_MISSING.registerFactory(DelegateAccessorsImportFix)
        COMPONENT_FUNCTION_MISSING.registerFactory(ComponentsImportFix)

        NO_GET_METHOD.registerFactory(ArrayAccessorImportFix)
        NO_SET_METHOD.registerFactory(ArrayAccessorImportFix)

        CONFLICTING_IMPORT.registerFactory(RemovePsiElementSimpleFix.RemoveImportFactory)

        SUPERTYPE_NOT_INITIALIZED.registerFactory(SuperClassNotInitialized)
        FUNCTION_CALL_EXPECTED.registerFactory(ChangeToFunctionInvocationFix)
        FUNCTION_EXPECTED.registerFactory(ChangeToPropertyAccessFix)

        CANNOT_CHANGE_ACCESS_PRIVILEGE.registerFactory(ChangeVisibilityModifierFix)
        CANNOT_WEAKEN_ACCESS_PRIVILEGE.registerFactory(ChangeVisibilityModifierFix)

        INVISIBLE_REFERENCE.registerFactory(MakeVisibleFactory)
        INVISIBLE_MEMBER.registerFactory(MakeVisibleFactory)
        INVISIBLE_SETTER.registerFactory(MakeVisibleFactory)

        for (exposed in listOf(EXPOSED_FUNCTION_RETURN_TYPE, EXPOSED_PARAMETER_TYPE, EXPOSED_PROPERTY_TYPE, EXPOSED_RECEIVER_TYPE,
                               EXPOSED_SUPER_CLASS, EXPOSED_SUPER_INTERFACE, EXPOSED_TYPE_PARAMETER_BOUND)) {
            exposed.registerFactory(ChangeVisibilityOnExposureFactory)
        }

        REDUNDANT_NULLABLE.registerFactory(RemoveNullableFix.Factory(RemoveNullableFix.NullableKind.REDUNDANT))
        NULLABLE_SUPERTYPE.registerFactory(RemoveNullableFix.Factory(RemoveNullableFix.NullableKind.SUPERTYPE))
        USELESS_NULLABLE_CHECK.registerFactory(RemoveNullableFix.Factory(RemoveNullableFix.NullableKind.USELESS))


        val implementMembersHandler = ImplementMembersHandler()
        val implementMembersAsParametersHandler = ImplementAsConstructorParameter()
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerActions(implementMembersHandler, implementMembersAsParametersHandler)
        ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.registerActions(implementMembersHandler, implementMembersAsParametersHandler)
        MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.registerActions(implementMembersHandler, implementMembersAsParametersHandler)

        VAL_WITH_SETTER.registerFactory(ChangeVariableMutabilityFix.VAL_WITH_SETTER_FACTORY)
        VAL_REASSIGNMENT.registerFactory(ChangeVariableMutabilityFix.VAL_REASSIGNMENT_FACTORY)
        VAL_REASSIGNMENT.registerFactory(LiftAssignmentOutOfTryFix)
        CAPTURED_VAL_INITIALIZATION.registerFactory(ChangeVariableMutabilityFix.CAPTURED_VAL_INITIALIZATION_FACTORY)
        CAPTURED_MEMBER_VAL_INITIALIZATION.registerFactory(ChangeVariableMutabilityFix.CAPTURED_MEMBER_VAL_INITIALIZATION_FACTORY)
        VAR_OVERRIDDEN_BY_VAL.registerFactory(ChangeVariableMutabilityFix.VAR_OVERRIDDEN_BY_VAL_FACTORY)
        VAR_ANNOTATION_PARAMETER.registerFactory(ChangeVariableMutabilityFix.VAR_ANNOTATION_PARAMETER_FACTORY)

        VAL_OR_VAR_ON_FUN_PARAMETER.registerFactory(RemoveValVarFromParameterFix)
        VAL_OR_VAR_ON_LOOP_PARAMETER.registerFactory(RemoveValVarFromParameterFix)
        VAL_OR_VAR_ON_CATCH_PARAMETER.registerFactory(RemoveValVarFromParameterFix)
        VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER.registerFactory(RemoveValVarFromParameterFix)

        UNUSED_VARIABLE.registerFactory(RemovePsiElementSimpleFix.RemoveVariableFactory)
        UNUSED_VARIABLE.registerFactory(RenameToUnderscoreFix.Factory)

        UNUSED_DESTRUCTURED_PARAMETER_ENTRY.registerFactory(RenameToUnderscoreFix.Factory)

        SENSELESS_COMPARISON.registerFactory(SimplifyComparisonFix)

        UNNECESSARY_SAFE_CALL.registerFactory(ReplaceWithDotCallFix)
        UNSAFE_CALL.registerFactory(ReplaceWithSafeCallFix)

        UNSAFE_CALL.registerFactory(SurroundWithNullCheckFix)
        UNSAFE_IMPLICIT_INVOKE_CALL.registerFactory(SurroundWithNullCheckFix)
        UNSAFE_INFIX_CALL.registerFactory(SurroundWithNullCheckFix)
        UNSAFE_OPERATOR_CALL.registerFactory(SurroundWithNullCheckFix)
        ITERATOR_ON_NULLABLE.registerFactory(SurroundWithNullCheckFix.IteratorOnNullableFactory)
        TYPE_MISMATCH.registerFactory(SurroundWithNullCheckFix.TypeMismatchFactory)

        UNSAFE_CALL.registerFactory(WrapWithSafeLetCallFix.UnsafeFactory)
        UNSAFE_IMPLICIT_INVOKE_CALL.registerFactory(WrapWithSafeLetCallFix.UnsafeFactory)
        UNSAFE_INFIX_CALL.registerFactory(WrapWithSafeLetCallFix.UnsafeFactory)
        UNSAFE_OPERATOR_CALL.registerFactory(WrapWithSafeLetCallFix.UnsafeFactory)
        TYPE_MISMATCH.registerFactory(WrapWithSafeLetCallFix.TypeMismatchFactory)

        UNSAFE_CALL.registerFactory(AddExclExclCallFix)
        UNSAFE_INFIX_CALL.registerFactory(AddExclExclCallFix)
        UNSAFE_OPERATOR_CALL.registerFactory(AddExclExclCallFix)
        UNNECESSARY_NOT_NULL_ASSERTION.registerFactory(RemoveExclExclCallFix)
        UNSAFE_INFIX_CALL.registerFactory(ReplaceInfixOrOperatorCallFix)
        UNSAFE_OPERATOR_CALL.registerFactory(ReplaceInfixOrOperatorCallFix)
        UNSAFE_CALL.registerFactory(ReplaceInfixOrOperatorCallFix) // [] only
        UNSAFE_IMPLICIT_INVOKE_CALL.registerFactory(ReplaceInfixOrOperatorCallFix)
        UNSAFE_CALL.registerFactory(ReplaceWithSafeCallForScopeFunctionFix)

        AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.registerActions(SpecifyTypeExplicitlyFix())
        PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.registerActions(SpecifyTypeExplicitlyFix())
        MUST_BE_INITIALIZED.registerActions(SpecifyTypeExplicitlyFix())

        ELSE_MISPLACED_IN_WHEN.registerFactory(MoveWhenElseBranchFix)
        NO_ELSE_IN_WHEN.registerFactory(AddWhenElseBranchFix)
        NO_ELSE_IN_WHEN.registerFactory(AddWhenRemainingBranchesFix)
        REDUNDANT_ELSE_IN_WHEN.registerFactory(RemoveWhenElseBranchFix)
        NON_EXHAUSTIVE_WHEN.registerFactory(AddWhenElseBranchFix)
        NON_EXHAUSTIVE_WHEN.registerFactory(AddWhenRemainingBranchesFix)
        NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS.registerFactory(AddWhenElseBranchFix)
        NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS.registerFactory(AddWhenRemainingBranchesFix)
        BREAK_OR_CONTINUE_IN_WHEN.registerFactory(AddLoopLabelFix)

        NO_TYPE_ARGUMENTS_ON_RHS.registerFactory(AddStarProjectionsFix.IsExpressionFactory)

        TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER.registerFactory(RemovePsiElementSimpleFix.RemoveTypeArgumentsFactory)

        UNCHECKED_CAST.registerFactory(ChangeToStarProjectionFix)
        CANNOT_CHECK_FOR_ERASED.registerFactory(ChangeToStarProjectionFix)

        INACCESSIBLE_OUTER_CLASS_EXPRESSION.registerFactory(AddModifierFix.createFactory(INNER_KEYWORD, KtClass::class.java))

        FINAL_SUPERTYPE.registerFactory(AddModifierFix.MakeClassOpenFactory)
        FINAL_UPPER_BOUND.registerFactory(AddModifierFix.MakeClassOpenFactory)

        OVERRIDING_FINAL_MEMBER.registerFactory(MakeOverriddenMemberOpenFix)

        PARAMETER_NAME_CHANGED_ON_OVERRIDE.registerFactory(RenameParameterToMatchOverriddenMethodFix)

        NESTED_CLASS_NOT_ALLOWED.registerFactory(AddModifierFix.createFactory(INNER_KEYWORD))

        CONFLICTING_PROJECTION.registerFactory(RemoveModifierFix.createRemoveProjectionFactory(false))
        PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.registerFactory(RemoveModifierFix.createRemoveProjectionFactory(false))
        PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.registerFactory(RemoveModifierFix.createRemoveProjectionFactory(false))

        NOT_AN_ANNOTATION_CLASS.registerFactory(MakeClassAnAnnotationClassFix)

        val changeVariableTypeFix = ChangeVariableTypeFix.PropertyOrReturnTypeMismatchOnOverrideFactory
        RETURN_TYPE_MISMATCH_ON_OVERRIDE.registerFactory(changeVariableTypeFix)
        PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.registerFactory(changeVariableTypeFix)
        VAR_TYPE_MISMATCH_ON_OVERRIDE.registerFactory(changeVariableTypeFix)
        COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.registerFactory(ChangeVariableTypeFix.ComponentFunctionReturnTypeMismatchFactory)

        val changeFunctionReturnTypeFix = ChangeCallableReturnTypeFix.ChangingReturnTypeToUnitFactory
        RETURN_TYPE_MISMATCH.registerFactory(changeFunctionReturnTypeFix)
        NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.registerFactory(changeFunctionReturnTypeFix)
        RETURN_TYPE_MISMATCH_ON_OVERRIDE.registerFactory(ChangeCallableReturnTypeFix.ReturnTypeMismatchOnOverrideFactory)
        COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.registerFactory(ChangeCallableReturnTypeFix.ComponentFunctionReturnTypeMismatchFactory)
        HAS_NEXT_FUNCTION_TYPE_MISMATCH.registerFactory(ChangeCallableReturnTypeFix.HasNextFunctionTypeMismatchFactory)
        COMPARE_TO_TYPE_MISMATCH.registerFactory(ChangeCallableReturnTypeFix.CompareToTypeMismatchFactory)
        IMPLICIT_NOTHING_RETURN_TYPE.registerFactory(ChangeCallableReturnTypeFix.ChangingReturnTypeToNothingFactory)

        TOO_MANY_ARGUMENTS.registerFactory(ChangeFunctionSignatureFix)
        NO_VALUE_FOR_PARAMETER.registerFactory(ChangeFunctionSignatureFix)
        UNUSED_PARAMETER.registerFactory(RemoveUnusedFunctionParameterFix)
        UNUSED_ANONYMOUS_PARAMETER.registerFactory(RenameToUnderscoreFix.Factory)
        UNUSED_ANONYMOUS_PARAMETER.registerFactory(RemoveSingleLambdaParameterFix)
        EXPECTED_PARAMETERS_NUMBER_MISMATCH.registerFactory(ChangeFunctionLiteralSignatureFix)

        EXPECTED_PARAMETER_TYPE_MISMATCH.registerFactory(ChangeTypeFix)

        EXTENSION_PROPERTY_WITH_BACKING_FIELD.registerFactory(ConvertExtensionPropertyInitializerToGetterFix)

        EXPECTED_TYPE_MISMATCH.registerFactory(ChangeFunctionLiteralReturnTypeFix)
        ASSIGNMENT_TYPE_MISMATCH.registerFactory(ChangeFunctionLiteralReturnTypeFix)

        UNRESOLVED_REFERENCE.registerFactory(CreateUnaryOperationActionFactory)
        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(CreateUnaryOperationActionFactory)
        NO_VALUE_FOR_PARAMETER.registerFactory(CreateUnaryOperationActionFactory)

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(CreateBinaryOperationActionFactory)
        UNRESOLVED_REFERENCE.registerFactory(CreateBinaryOperationActionFactory)
        NONE_APPLICABLE.registerFactory(CreateBinaryOperationActionFactory)
        NO_VALUE_FOR_PARAMETER.registerFactory(CreateBinaryOperationActionFactory)
        TOO_MANY_ARGUMENTS.registerFactory(CreateBinaryOperationActionFactory)
        TYPE_MISMATCH_ERRORS.forEach { it.registerFactory(CreateBinaryOperationActionFactory) }

        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        UNRESOLVED_REFERENCE.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        UNRESOLVED_REFERENCE.registerFactory(CreateFunctionFromCallableReferenceActionFactory)
        NO_VALUE_FOR_PARAMETER.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        TOO_MANY_ARGUMENTS.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        NONE_APPLICABLE.registerFactory(*CreateCallableFromCallActionFactory.INSTANCES)
        TYPE_MISMATCH.registerFactory(*CreateCallableFromCallActionFactory.FUNCTIONS)

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
        TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.registerFactory(factoryForTypeMismatchError)

        SMARTCAST_IMPOSSIBLE.registerFactory(SmartCastImpossibleExclExclFixFactory)
        SMARTCAST_IMPOSSIBLE.registerFactory(CastExpressionFix.SmartCastImpossibleFactory)

        PLATFORM_CLASS_MAPPED_TO_KOTLIN.registerFactory(MapPlatformClassToKotlinFix)

        MANY_CLASSES_IN_SUPERTYPE_LIST.registerFactory(RemoveSupertypeFix)

        NO_GET_METHOD.registerFactory(CreateGetFunctionActionFactory)
        TYPE_MISMATCH_ERRORS.forEach { it.registerFactory(CreateGetFunctionActionFactory) }
        NO_SET_METHOD.registerFactory(CreateSetFunctionActionFactory)
        TYPE_MISMATCH_ERRORS.forEach { it.registerFactory(CreateSetFunctionActionFactory) }
        HAS_NEXT_MISSING.registerFactory(CreateHasNextFunctionActionFactory)
        HAS_NEXT_FUNCTION_NONE_APPLICABLE.registerFactory(CreateHasNextFunctionActionFactory)
        NEXT_MISSING.registerFactory(CreateNextFunctionActionFactory)
        NEXT_NONE_APPLICABLE.registerFactory(CreateNextFunctionActionFactory)
        ITERATOR_MISSING.registerFactory(CreateIteratorFunctionActionFactory)
        ITERATOR_ON_NULLABLE.registerFactory(MissingIteratorExclExclFixFactory)
        COMPONENT_FUNCTION_MISSING.registerFactory(CreateComponentFunctionActionFactory, CreateDataClassPropertyFromDestructuringActionFactory)

        DELEGATE_SPECIAL_FUNCTION_MISSING.registerFactory(CreatePropertyDelegateAccessorsActionFactory)
        DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE.registerFactory(CreatePropertyDelegateAccessorsActionFactory)

        UNRESOLVED_REFERENCE.registerFactory(CreateClassFromTypeReferenceActionFactory,
                                             CreateClassFromReferenceExpressionActionFactory,
                                             CreateClassFromCallWithConstructorCalleeActionFactory)

        UNRESOLVED_REFERENCE.registerFactory(CreateTypeAliasFromTypeReferenceActionFactory)

        UNRESOLVED_REFERENCE.registerFactory(PlatformUnresolvedProvider)

        PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.registerFactory(InsertDelegationCallQuickfix.InsertThisDelegationCallFactory)

        EXPLICIT_DELEGATION_CALL_REQUIRED.registerFactory(InsertDelegationCallQuickfix.InsertThisDelegationCallFactory)
        EXPLICIT_DELEGATION_CALL_REQUIRED.registerFactory(InsertDelegationCallQuickfix.InsertSuperDelegationCallFactory)

        MISSING_CONSTRUCTOR_KEYWORD.registerFactory(MissingConstructorKeywordFix)

        MISSING_CONSTRUCTOR_BRACKETS.registerFactory(MissingConstructorBracketsFix)

        ANONYMOUS_FUNCTION_WITH_NAME.registerFactory(RemoveNameFromFunctionExpressionFix)

        UNRESOLVED_REFERENCE.registerFactory(ReplaceObsoleteLabelSyntaxFix)

        DEPRECATION.registerFactory(DeprecatedSymbolUsageFix, DeprecatedSymbolUsageInWholeProjectFix, MigrateExternalExtensionFix)
        DEPRECATION_ERROR.registerFactory(DeprecatedSymbolUsageFix, DeprecatedSymbolUsageInWholeProjectFix, MigrateExternalExtensionFix)
        TYPEALIAS_EXPANSION_DEPRECATION.registerFactory(DeprecatedSymbolUsageFix, DeprecatedSymbolUsageInWholeProjectFix, MigrateExternalExtensionFix)
        TYPEALIAS_EXPANSION_DEPRECATION_ERROR.registerFactory(DeprecatedSymbolUsageFix, DeprecatedSymbolUsageInWholeProjectFix, MigrateExternalExtensionFix)
        PROTECTED_CALL_FROM_PUBLIC_INLINE.registerFactory(ReplaceProtectedToPublishedApiCallFix)

        POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION.registerFactory(ReplaceJavaAnnotationPositionedArgumentsFix)

        NO_REFLECTION_IN_CLASS_PATH.registerFactory(AddReflectionQuickFix)

        ErrorsJvm.JAVA_TYPE_MISMATCH.registerFactory(CastExpressionFix.GenericVarianceConversion)

        UPPER_BOUND_VIOLATED.registerFactory(AddGenericUpperBoundFix.Factory)
        TYPE_INFERENCE_UPPER_BOUND_VIOLATED.registerFactory(AddGenericUpperBoundFix.Factory)

        TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.registerFactory(ConvertClassToKClassFix)

        NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION.registerFactory(ConstFixFactory)

        OPERATOR_MODIFIER_REQUIRED.registerFactory(AddModifierFixFactory(KtTokens.OPERATOR_KEYWORD))
        OPERATOR_MODIFIER_REQUIRED.registerFactory(ImportForMissingOperatorFactory)

        INFIX_MODIFIER_REQUIRED.registerFactory(AddModifierFixFactory(KtTokens.INFIX_KEYWORD))
        INFIX_MODIFIER_REQUIRED.registerFactory(InfixCallFixActionFactory)

        UNDERSCORE_IS_RESERVED.registerFactory(RenameUnderscoreFix)

        CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS.registerFactory(AddTypeToLHSOfCallableReferenceFix)

        DEPRECATED_TYPE_PARAMETER_SYNTAX.registerFactory(MigrateTypeParameterListFix)

        UNRESOLVED_REFERENCE.registerFactory(KotlinAddOrderEntryActionFactory)

        UNRESOLVED_REFERENCE.registerFactory(RenameUnresolvedReferenceActionFactory)
        EXPRESSION_EXPECTED_PACKAGE_FOUND.registerFactory(RenameUnresolvedReferenceActionFactory)

        MISPLACED_TYPE_PARAMETER_CONSTRAINTS.registerFactory(MoveTypeParameterConstraintFix)

        COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT.registerFactory(CommaInWhenConditionWithoutArgumentFix)

        DATA_CLASS_NOT_PROPERTY_PARAMETER.registerFactory(AddValVarToConstructorParameterAction.QuickFixFactory)

        NON_LOCAL_RETURN_NOT_ALLOWED.registerFactory(AddInlineModifierFix.CrossInlineFactory)
        USAGE_IS_NOT_INLINABLE.registerFactory(AddInlineModifierFix.NoInlineFactory)

        UNRESOLVED_REFERENCE.registerFactory(MakeConstructorParameterPropertyFix)
        DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE.registerFactory(SpecifyOverrideExplicitlyFix)

        SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE.registerFactory(ConvertExtensionToFunctionTypeFix)

        UNUSED_LAMBDA_EXPRESSION.registerFactory(AddRunToLambdaFix)

        WRONG_LONG_SUFFIX.registerFactory(WrongLongSuffixFix)

        REIFIED_TYPE_PARAMETER_NO_INLINE.registerFactory(AddInlineToFunctionWithReifiedFix)

        VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION.registerFactory(AddTypeAnnotationToValueParameterFix)

        UNRESOLVED_REFERENCE.registerFactory(CreateTypeParameterByUnresolvedRefActionFactory)
        WRONG_NUMBER_OF_TYPE_ARGUMENTS.registerFactory(CreateTypeParameterUnmatchedTypeArgumentActionFactory)

        FINAL_UPPER_BOUND.registerFactory(InlineTypeParameterFix)
        FINAL_UPPER_BOUND.registerFactory(RemoveFinalUpperBoundFix)

        TYPE_PARAMETER_AS_REIFIED.registerFactory(AddReifiedToTypeParameterOfFunctionFix)

        TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL.registerFactory(TooLongCharLiteralToStringFix)

        UNUSED_VALUE.registerFactory(RemoveUnusedValueFix)

        ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE.registerFactory(AddNewLineAfterAnnotationsFix)

        INAPPLICABLE_LATEINIT_MODIFIER.registerFactory(ChangeVariableMutabilityFix.LATEINIT_VAL_FACTORY)
        INAPPLICABLE_LATEINIT_MODIFIER.registerFactory(RemoveNullableFix.LATEINIT_FACTORY)

        OVERLOADS_ABSTRACT.registerFactory(RemoveAnnotationFix.JvmOverloads)
        OVERLOADS_INTERFACE.registerFactory(RemoveAnnotationFix.JvmOverloads)
        OVERLOADS_PRIVATE.registerFactory(RemoveAnnotationFix.JvmOverloads)
        OVERLOADS_LOCAL.registerFactory(RemoveAnnotationFix.JvmOverloads)
        OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS.registerFactory(RemoveAnnotationFix.JvmOverloads)

        HEADER_WITHOUT_IMPLEMENTATION.registerFactory(CreateHeaderImplementationFix)

        CAST_NEVER_SUCCEEDS.registerFactory(ReplacePrimitiveCastWithNumberConversionFix)

        ErrorsJs.WRONG_EXTERNAL_DECLARATION.registerFactory(MigrateExternalExtensionFix)

        ILLEGAL_SUSPEND_FUNCTION_CALL.registerFactory(AddSuspendModifierFix)
        INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED.registerFactory(AddInlineModifierFix.SuspendFactory)

        UNRESOLVED_REFERENCE.registerFactory(AddSuspendModifierFix.UnresolvedReferenceFactory)
        UNRESOLVED_REFERENCE_WRONG_RECEIVER.registerFactory(AddSuspendModifierFix.UnresolvedReferenceFactory)

        UNSUPPORTED_FEATURE.registerFactory(EnableUnsupportedFeatureFix)

        EXPERIMENTAL_FEATURE_ERROR.registerFactory(ChangeCoroutineSupportFix)
        EXPERIMENTAL_FEATURE_WARNING.registerFactory(ChangeCoroutineSupportFix)

        UNRESOLVED_REFERENCE.registerFactory(CreateLabelFix)
        YIELD_IS_RESERVED.registerFactory(UnsupportedYieldFix)
        INVALID_TYPE_OF_ANNOTATION_MEMBER.registerFactory(TypeOfAnnotationMemberFix)

        ILLEGAL_INLINE_PARAMETER_MODIFIER.registerFactory(AddInlineToFunctionFix)

        INAPPLICABLE_JVM_FIELD.registerFactory(ReplaceJvmFieldWithConstFix)

        CONFLICTING_OVERLOADS.registerFactory(ChangeSuspendInHierarchyFix)

        MUST_BE_INITIALIZED_OR_BE_ABSTRACT.registerFactory(AddModifierFix.AddLateinitFactory)

        RETURN_NOT_ALLOWED.registerFactory(ChangeToLabeledReturnFix)

        INAPPLICABLE_RECEIVER_TARGET.registerFactory(MoveReceiverAnnotationFix)

        NO_CONSTRUCTOR.registerFactory(RemoveNoConstructorFix)

        ANNOTATION_USED_AS_ANNOTATION_ARGUMENT.registerFactory(RemoveAtFromAnnotationArgument)
    }
}
