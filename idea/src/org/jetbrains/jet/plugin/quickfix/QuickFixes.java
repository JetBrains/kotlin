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
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler;

import java.util.Collection;

import static org.jetbrains.jet.lexer.JetTokens.*;

/**
* @author svtk
*/
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

        factories.put(Errors.ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS, removeAbstractModifierFactory);
        factories.put(Errors.ABSTRACT_PROPERTY_NOT_IN_CLASS, removeAbstractModifierFactory);

        JetIntentionActionFactory removePartsFromPropertyFactory = RemovePartsFromPropertyFix.createFactory();
        factories.put(Errors.ABSTRACT_PROPERTY_WITH_INITIALIZER, removeAbstractModifierFactory);
        factories.put(Errors.ABSTRACT_PROPERTY_WITH_INITIALIZER, removePartsFromPropertyFactory);

        factories.put(Errors.ABSTRACT_PROPERTY_WITH_GETTER, removeAbstractModifierFactory);
        factories.put(Errors.ABSTRACT_PROPERTY_WITH_GETTER, removePartsFromPropertyFactory);

        factories.put(Errors.ABSTRACT_PROPERTY_WITH_SETTER, removeAbstractModifierFactory);
        factories.put(Errors.ABSTRACT_PROPERTY_WITH_SETTER, removePartsFromPropertyFactory);

        factories.put(Errors.PROPERTY_INITIALIZER_IN_TRAIT, removePartsFromPropertyFactory);

        factories.put(Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT, addAbstractModifierFactory);

        JetIntentionActionFactory addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, JetClass.class);
        factories.put(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        factories.put(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        JetIntentionActionFactory removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory();
        factories.put(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        factories.put(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        factories.put(Errors.ABSTRACT_FUNCTION_WITH_BODY, removeAbstractModifierFactory);
        factories.put(Errors.ABSTRACT_FUNCTION_WITH_BODY, removeFunctionBodyFactory);

        JetIntentionActionFactory addFunctionBodyFactory = AddFunctionBodyFix.createFactory();
        factories.put(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addAbstractModifierFactory);
        factories.put(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addFunctionBodyFactory);

        factories.put(Errors.NON_MEMBER_ABSTRACT_FUNCTION, removeAbstractModifierFactory);
        factories.put(Errors.NON_MEMBER_FUNCTION_NO_BODY, addFunctionBodyFactory);

        factories.put(Errors.NOTHING_TO_OVERRIDE, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD));
        factories.put(Errors.VIRTUAL_MEMBER_HIDDEN, AddModifierFix.createFactory(OVERRIDE_KEYWORD));

        factories.put(Errors.USELESS_CAST_STATIC_ASSERT_IS_FINE, ReplaceOperationInBinaryExpressionFix.createChangeCastToStaticAssertFactory());
        factories.put(Errors.USELESS_CAST, RemoveRightPartOfBinaryExpressionFix.createRemoveCastFactory());

        JetIntentionActionFactory changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory();
        factories.put(Errors.WRONG_SETTER_PARAMETER_TYPE, changeAccessorTypeFactory);
        factories.put(Errors.WRONG_GETTER_RETURN_TYPE, changeAccessorTypeFactory);

        factories.put(Errors.USELESS_ELVIS, RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory());

        JetIntentionActionFactory removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true);
        factories.put(Errors.REDUNDANT_MODIFIER, removeRedundantModifierFactory);
        factories.put(Errors.ABSTRACT_MODIFIER_IN_TRAIT, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD, true));
        factories.put(Errors.OPEN_MODIFIER_IN_TRAIT, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD, true));
        factories.put(Errors.TRAIT_CAN_NOT_BE_FINAL, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(FINAL_KEYWORD));

        JetIntentionActionFactory removeOpenModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD);
        factories.put(Errors.NON_FINAL_MEMBER_IN_FINAL_CLASS, AddModifierFix.createFactory(OPEN_KEYWORD, JetClass.class));
        factories.put(Errors.NON_FINAL_MEMBER_IN_FINAL_CLASS, removeOpenModifierFactory);

        JetIntentionActionFactory removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory();
        factories.put(Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, removeModifierFactory);
        factories.put(Errors.REDUNDANT_MODIFIER_IN_GETTER, removeRedundantModifierFactory);
        factories.put(Errors.ILLEGAL_MODIFIER, removeModifierFactory);

        factories.put(Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, AddReturnTypeFix.createFactory());

        JetIntentionActionFactory changeToBackingFieldFactory = ChangeToBackingFieldFix.createFactory();
        factories.put(Errors.INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER, changeToBackingFieldFactory);
        factories.put(Errors.INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER, changeToBackingFieldFactory);

        JetIntentionActionFactory unresolvedReferenceFactory = ImportClassAndFunFix.createFactory();
        factories.put(Errors.UNRESOLVED_REFERENCE, unresolvedReferenceFactory);

        factories.put(Errors.SUPERTYPE_NOT_INITIALIZED_DEFAULT, ChangeToInvocationFix.createFactory());

        ImplementMethodsHandler implementMethodsHandler = new ImplementMethodsHandler();
        actions.put(Errors.ABSTRACT_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);
        actions.put(Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);

        ChangeVariableMutabilityFix changeVariableMutabilityFix = new ChangeVariableMutabilityFix();
        actions.put(Errors.VAL_WITH_SETTER, changeVariableMutabilityFix);
        actions.put(Errors.VAL_REASSIGNMENT, changeVariableMutabilityFix);

        actions.put(Errors.UNNECESSARY_SAFE_CALL, new ReplaceCallFix(false));
        actions.put(Errors.UNSAFE_CALL, new ReplaceCallFix(true));
    }
}