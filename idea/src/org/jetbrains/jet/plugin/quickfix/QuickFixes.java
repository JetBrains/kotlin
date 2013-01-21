/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.diagnostics.AbstractDiagnosticFactory;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler;

import java.util.Collection;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class QuickFixes {

    private static final Multimap<AbstractDiagnosticFactory, JetIntentionActionFactory> factories = HashMultimap.create();
    private static final Multimap<AbstractDiagnosticFactory, IntentionAction> actions = HashMultimap.create();

    public static Collection<JetIntentionActionFactory> getActionFactories(AbstractDiagnosticFactory diagnosticFactory) {
        return factories.get(diagnosticFactory);
    }

    public static Collection<IntentionAction> getActions(AbstractDiagnosticFactory diagnosticFactory) {
        return actions.get(diagnosticFactory);
    }

    private QuickFixes() {}

    static {
        JetIntentionActionFactory removeAbstractModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD);
        JetIntentionActionFactory addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD);

        factories.put(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS, removeAbstractModifierFactory);

        JetIntentionActionFactory removePartsFromPropertyFactory = RemovePartsFromPropertyFix.createFactory();
        factories.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, removeAbstractModifierFactory);
        factories.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, removePartsFromPropertyFactory);

        factories.put(ABSTRACT_PROPERTY_WITH_GETTER, removeAbstractModifierFactory);
        factories.put(ABSTRACT_PROPERTY_WITH_GETTER, removePartsFromPropertyFactory);

        factories.put(ABSTRACT_PROPERTY_WITH_SETTER, removeAbstractModifierFactory);
        factories.put(ABSTRACT_PROPERTY_WITH_SETTER, removePartsFromPropertyFactory);

        factories.put(PROPERTY_INITIALIZER_IN_TRAIT, removePartsFromPropertyFactory);

        factories.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, addAbstractModifierFactory);

        JetIntentionActionFactory removeFinalModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(FINAL_KEYWORD);

        JetIntentionActionFactory addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, JetClass.class);
        factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        JetIntentionActionFactory removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory();
        factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        factories.put(ABSTRACT_FUNCTION_WITH_BODY, removeAbstractModifierFactory);
        factories.put(ABSTRACT_FUNCTION_WITH_BODY, removeFunctionBodyFactory);

        factories.put(FINAL_PROPERTY_IN_TRAIT, removeFinalModifierFactory);
        factories.put(FINAL_FUNCTION_WITH_NO_BODY, removeFinalModifierFactory);

        JetIntentionActionFactory addFunctionBodyFactory = AddFunctionBodyFix.createFactory();
        factories.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addAbstractModifierFactory);
        factories.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addFunctionBodyFactory);

        factories.put(NON_MEMBER_FUNCTION_NO_BODY, addFunctionBodyFactory);

        factories.put(NOTHING_TO_OVERRIDE, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD));
        factories.put(VIRTUAL_MEMBER_HIDDEN, AddModifierFix.createFactory(OVERRIDE_KEYWORD));

        factories.put(USELESS_CAST_STATIC_ASSERT_IS_FINE, ReplaceOperationInBinaryExpressionFix.createChangeCastToStaticAssertFactory());
        factories.put(USELESS_CAST, RemoveRightPartOfBinaryExpressionFix.createRemoveCastFactory());

        JetIntentionActionFactory changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory();
        factories.put(WRONG_SETTER_PARAMETER_TYPE, changeAccessorTypeFactory);
        factories.put(WRONG_GETTER_RETURN_TYPE, changeAccessorTypeFactory);

        factories.put(USELESS_ELVIS, RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory());

        JetIntentionActionFactory removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true);
        factories.put(REDUNDANT_MODIFIER, removeRedundantModifierFactory);
        factories.put(ABSTRACT_MODIFIER_IN_TRAIT, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD, true));
        factories.put(OPEN_MODIFIER_IN_TRAIT, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD, true));
        factories.put(TRAIT_CAN_NOT_BE_FINAL, removeFinalModifierFactory);
        factories.put(REDUNDANT_PROJECTION, RemoveModifierFix.createRemoveProjectionFactory());

        JetIntentionActionFactory removeOpenModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD);
        factories.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, AddModifierFix.createFactory(OPEN_KEYWORD, JetClass.class));
        factories.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, removeOpenModifierFactory);

        JetIntentionActionFactory removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory();
        factories.put(GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, removeModifierFactory);
        factories.put(REDUNDANT_MODIFIER_IN_GETTER, removeRedundantModifierFactory);
        factories.put(ILLEGAL_MODIFIER, removeModifierFactory);

        JetIntentionActionFactory changeToBackingFieldFactory = ChangeToBackingFieldFix.createFactory();
        factories.put(INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER, changeToBackingFieldFactory);
        factories.put(INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER, changeToBackingFieldFactory);

        JetIntentionActionFactory unresolvedReferenceFactory = ImportClassAndFunFix.createFactory();
        factories.put(UNRESOLVED_REFERENCE, unresolvedReferenceFactory);

        factories.put(SUPERTYPE_NOT_INITIALIZED_DEFAULT, ChangeToConstructorInvocationFix.createFactory());
        factories.put(FUNCTION_CALL_EXPECTED, ChangeToFunctionInvocationFix.createFactory());
        
        factories.put(CANNOT_CHANGE_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());
        factories.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());

        factories.put(TUPLES_ARE_NOT_SUPPORTED, MigrateTuplesInProjectFix.createFactory());

        factories.put(UNRESOLVED_REFERENCE, MigrateSureInProjectFix.createFactory());

        ImplementMethodsHandler implementMethodsHandler = new ImplementMethodsHandler();
        actions.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);
        actions.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);

        ChangeVariableMutabilityFix changeVariableMutabilityFix = new ChangeVariableMutabilityFix();
        actions.put(VAL_WITH_SETTER, changeVariableMutabilityFix);
        actions.put(VAL_REASSIGNMENT, changeVariableMutabilityFix);

        actions.put(UNNECESSARY_SAFE_CALL, ReplaceCallFix.toDotCallFromSafeCall());
        actions.put(UNSAFE_CALL, ReplaceCallFix.toSafeCall());

        actions.put(UNSAFE_CALL, ExclExclCallFix.introduceExclExclCall());
        actions.put(UNNECESSARY_NOT_NULL_ASSERTION, ExclExclCallFix.removeExclExclCall());

        actions.put(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, new SpecifyTypeExplicitlyFix());

        factories.put(NO_ELSE_IN_WHEN, AddWhenElseBranchFix.createFactory());

        factories.put(NO_TYPE_ARGUMENTS_ON_RHS_OF_IS_EXPRESSION, AddStarProjectionsFix.createFactoryForIsExpression());
        factories.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS, AddStarProjectionsFix.createFactoryForJavaClass());
    }
}
