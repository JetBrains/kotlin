package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.PsiElementOnlyDiagnosticFactory;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;

/**
* @author svtk
*/
public class QuickFixes {
    private static final Multimap<PsiElementOnlyDiagnosticFactory, JetIntentionActionFactory> actionMap = HashMultimap.create();

    public static Collection<JetIntentionActionFactory> get(PsiElementOnlyDiagnosticFactory diagnosticFactory) {
        return actionMap.get(diagnosticFactory);
    }

    private QuickFixes() {}

    private static <T extends PsiElement> void add(PsiElementOnlyDiagnosticFactory<? extends T> diagnosticFactory, JetIntentionActionFactory<T> actionFactory) {
        actionMap.put(diagnosticFactory, actionFactory);
    }

    static {
        JetIntentionActionFactory<JetModifierListOwner> removeAbstractModifierFactory = RemoveModifierFix.createFactory(JetTokens.ABSTRACT_KEYWORD);
        JetIntentionActionFactory<JetModifierListOwner> addAbstractModifierFactory = AddModifierFix.createFactory(JetTokens.ABSTRACT_KEYWORD, new JetToken[]{JetTokens.OPEN_KEYWORD}, new JetToken[] {JetTokens.FINAL_KEYWORD});

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
        add(Errors.REDUNDANT_ABSTRACT, removeAbstractModifierFactory);

        JetIntentionActionFactory<PsiElement> addAbstractToClassFactory = QuickFixUtil.createFactoryRedirectingAdditionalInfoToAnotherFactory(addAbstractModifierFactory, DiagnosticParameters.CLASS);
        add(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        JetIntentionActionFactory<JetFunctionOrPropertyAccessor> removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory();
        add(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

        add(Errors.ABSTRACT_FUNCTION_WITH_BODY, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_FUNCTION_WITH_BODY, removeFunctionBodyFactory);

        JetIntentionActionFactory<JetFunctionOrPropertyAccessor> addFunctionBodyFactory = AddFunctionBodyFix.createFactory();
        add(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addAbstractModifierFactory);
        add(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addFunctionBodyFactory);

        add(Errors.NON_MEMBER_ABSTRACT_FUNCTION, removeAbstractModifierFactory);
        add(Errors.NON_MEMBER_ABSTRACT_ACCESSOR, removeAbstractModifierFactory);
        add(Errors.NON_MEMBER_FUNCTION_NO_BODY, addFunctionBodyFactory);

        add(Errors.NOTHING_TO_OVERRIDE, RemoveModifierFix.createFactory(JetTokens.OVERRIDE_KEYWORD));
        add(Errors.VIRTUAL_MEMBER_HIDDEN, AddModifierFix.createFactory(JetTokens.OVERRIDE_KEYWORD));

        add(Errors.VAL_WITH_SETTER, ChangeVariableMutabilityFix.createFactory());

        add(Errors.USELESS_CAST_STATIC_ASSERT_IS_FINE, ReplaceOperationInBinaryExpressionFix.createChangeCastToStaticAssertFactory());
        add(Errors.USELESS_CAST, RemoveRightPartOfBinaryExpressionFix.createRemoveCastFactory());

        JetIntentionActionFactory<JetPropertyAccessor> changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory();
        add(Errors.WRONG_SETTER_PARAMETER_TYPE, changeAccessorTypeFactory);
        add(Errors.WRONG_GETTER_RETURN_TYPE, changeAccessorTypeFactory);
        
        add(Errors.USELESS_ELVIS, RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory());
        
        add(Errors.UNNECESSARY_SAFE_CALL, ReplaceSafeCallToDotCall.createFactory());
        
        add(Errors.REDUNDANT_MODIFIER, RemoveRedundantModifierFix.createFactory());
        
        add(Errors.PROPERTY_INITIALIZER_NO_PRIMARY_CONSTRUCTOR, RemovePartsFromPropertyFix.createRemoveInitializerFactory());

        JetIntentionActionFactory<JetClass> addPrimaryConstructorFactory = AddPrimaryConstructorFix.createFactory();
        add(Errors.PROPERTY_INITIALIZER_NO_PRIMARY_CONSTRUCTOR, QuickFixUtil.createFactoryRedirectingAdditionalInfoToAnotherFactory(addPrimaryConstructorFactory, DiagnosticParameters.CLASS));
        add(Errors.PRIMARY_CONSTRUCTOR_MISSING_STATEFUL_PROPERTY, addPrimaryConstructorFactory);
    }
}

