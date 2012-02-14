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
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.PsiElementOnlyDiagnosticFactory;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler;

import java.util.Collection;

/**
* @author svtk
*/
public class QuickFixes {
    private static final Multimap<PsiElementOnlyDiagnosticFactory, JetIntentionActionFactory> jetActionMap = HashMultimap.create();
    private static final Multimap<DiagnosticFactory, IntentionAction> actionMap = HashMultimap.create();

    public static Collection<JetIntentionActionFactory> get(PsiElementOnlyDiagnosticFactory diagnosticFactory) {
        return jetActionMap.get(diagnosticFactory);
    }

    public static Collection<IntentionAction> get(DiagnosticFactory diagnosticFactory) {
        return actionMap.get(diagnosticFactory);
    }

    private QuickFixes() {}

    private static <T extends PsiElement> void add(PsiElementOnlyDiagnosticFactory<? extends T> diagnosticFactory, JetIntentionActionFactory<T> actionFactory) {
        jetActionMap.put(diagnosticFactory, actionFactory);
    }

    static {
        JetIntentionActionFactory<JetModifierListOwner> removeAbstractModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(JetTokens.ABSTRACT_KEYWORD);
        JetIntentionActionFactory<JetModifierListOwner> addAbstractModifierFactory = AddModifierFix.createFactory(JetTokens.ABSTRACT_KEYWORD, new JetToken[]{JetTokens.OPEN_KEYWORD, JetTokens.FINAL_KEYWORD});

        add(Errors.ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_PROPERTY_NOT_IN_CLASS, removeAbstractModifierFactory);
        
        JetIntentionActionFactory<JetProperty> removePartsFromPropertyFactory = RemovePartsFromPropertyFix.createFactory();
        add(Errors.ABSTRACT_PROPERTY_WITH_INITIALIZER, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_PROPERTY_WITH_INITIALIZER, removePartsFromPropertyFactory);

        add(Errors.ABSTRACT_PROPERTY_WITH_GETTER, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_PROPERTY_WITH_GETTER, removePartsFromPropertyFactory);

        add(Errors.ABSTRACT_PROPERTY_WITH_SETTER, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_PROPERTY_WITH_SETTER, removePartsFromPropertyFactory);

        add(Errors.PROPERTY_INITIALIZER_IN_TRAIT, removePartsFromPropertyFactory);

        add(Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT, addAbstractModifierFactory);

        JetIntentionActionFactory<PsiElement> addAbstractToClassFactory = QuickFixUtil.createFactoryRedirectingAdditionalInfoToAnotherFactory(addAbstractModifierFactory, DiagnosticParameters.CLASS);
        add(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        JetIntentionActionFactory<JetFunction> removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory();
        add(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        add(Errors.ABSTRACT_FUNCTION_WITH_BODY, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_FUNCTION_WITH_BODY, removeFunctionBodyFactory);

        JetIntentionActionFactory<JetFunction> addFunctionBodyFactory = AddFunctionBodyFix.createFactory();
        add(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addAbstractModifierFactory);
        add(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addFunctionBodyFactory);

        add(Errors.NON_MEMBER_ABSTRACT_FUNCTION, removeAbstractModifierFactory);
        add(Errors.NON_MEMBER_FUNCTION_NO_BODY, addFunctionBodyFactory);

        add(Errors.NOTHING_TO_OVERRIDE, RemoveModifierFix.createRemoveModifierFromListFactory(JetTokens.OVERRIDE_KEYWORD));
        add(Errors.VIRTUAL_MEMBER_HIDDEN, AddModifierFix.createFactory(JetTokens.OVERRIDE_KEYWORD, new JetToken[] {JetTokens.OPEN_KEYWORD}));

        add(Errors.USELESS_CAST_STATIC_ASSERT_IS_FINE, ReplaceOperationInBinaryExpressionFix.createChangeCastToStaticAssertFactory());
        add(Errors.USELESS_CAST, RemoveRightPartOfBinaryExpressionFix.createRemoveCastFactory());

        JetIntentionActionFactory<JetPropertyAccessor> changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory();
        add(Errors.WRONG_SETTER_PARAMETER_TYPE, changeAccessorTypeFactory);
        add(Errors.WRONG_GETTER_RETURN_TYPE, changeAccessorTypeFactory);

        add(Errors.USELESS_ELVIS, RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory());

        JetIntentionActionFactory<JetModifierList> removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFromListFactory(true);
        add(Errors.REDUNDANT_MODIFIER, removeRedundantModifierFactory);
        add(Errors.REDUNDANT_MODIFIER_IN_TRAIT, removeRedundantModifierFactory);
        add(Errors.TRAIT_CAN_NOT_BE_FINAL, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(JetTokens.FINAL_KEYWORD));

        JetIntentionActionFactory<JetModifierListOwner> addOpenModifierFactory = AddModifierFix.createFactory(JetTokens.OPEN_KEYWORD, new JetToken[]{JetTokens.FINAL_KEYWORD});
        JetIntentionActionFactory<JetModifierListOwner> removeOpenModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(JetTokens.OPEN_KEYWORD);
        add(Errors.NON_FINAL_MEMBER_IN_FINAL_CLASS, QuickFixUtil.createFactoryRedirectingAdditionalInfoToAnotherFactory(addOpenModifierFactory, DiagnosticParameters.CLASS));
        add(Errors.NON_FINAL_MEMBER_IN_FINAL_CLASS, removeOpenModifierFactory);

        JetIntentionActionFactory<JetModifierList> removeModifierFactory = RemoveModifierFix.createRemoveModifierFromListFactory();
        add(Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, removeModifierFactory);
        add(Errors.REDUNDANT_MODIFIER_IN_GETTER, removeRedundantModifierFactory);
        add(Errors.ILLEGAL_MODIFIER, removeModifierFactory);

        add(Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, AddReturnTypeFix.createFactory());

        JetIntentionActionFactory<JetSimpleNameExpression> changeToBackingFieldFactory = ChangeToBackingFieldFix.createFactory();
        add(Errors.INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER, changeToBackingFieldFactory);
        add(Errors.INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER, changeToBackingFieldFactory);

        JetIntentionActionFactory<JetSimpleNameExpression> unresolvedReferenceFactory = ImportClassAndFunFix.createFactory();
        add(Errors.UNRESOLVED_REFERENCE, unresolvedReferenceFactory);

        add(Errors.SUPERTYPE_NOT_INITIALIZED_DEFAULT, ChangeToInvocationFix.createFactory());
        
        ImplementMethodsHandler implementMethodsHandler = new ImplementMethodsHandler();
        actionMap.put(Errors.ABSTRACT_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);
        actionMap.put(Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);

        ChangeVariableMutabilityFix changeVariableMutabilityFix = new ChangeVariableMutabilityFix();
        actionMap.put(Errors.VAL_WITH_SETTER, changeVariableMutabilityFix);
        actionMap.put(Errors.VAL_REASSIGNMENT, changeVariableMutabilityFix);

        actionMap.put(Errors.UNNECESSARY_SAFE_CALL, new ReplaceCallFix(false));
        actionMap.put(Errors.UNSAFE_CALL, new ReplaceCallFix(true));
    }
}
