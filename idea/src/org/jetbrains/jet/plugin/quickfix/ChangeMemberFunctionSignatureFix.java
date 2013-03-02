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

import com.google.common.collect.Maps;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.CodegenUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.actions.JetChangeFunctionSignatureAction;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;
import org.jetbrains.jet.plugin.codeInsight.OverrideUtil;

import java.util.*;

/**
 * Fix that changes member function's signature to match one of super functions' signatures.
 */
public class ChangeMemberFunctionSignatureFix extends JetHintAction<JetNamedFunction> {
    private final List<SimpleFunctionDescriptor> possibleSignatures;

    public ChangeMemberFunctionSignatureFix(@NotNull JetNamedFunction element) {
        super(element);
        this.possibleSignatures = computePossibleSignatures(element);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && !possibleSignatures.isEmpty();
    }

    @NotNull
    @Override
    public String getText() {
        if (possibleSignatures.size() == 1)
            return JetBundle.message("change.function.signature.action.single",
                                     getFunctionSignatureString(
                                             possibleSignatures.get(0),
                                             /* shortTypeNames = */ true));
        else
            return JetBundle.message("change.function.signature.action.multiple");
    }

    @NotNull
    private String getFunctionSignatureString(@NotNull SimpleFunctionDescriptor functionSignature, boolean shortTypeNames) {
        return OverrideUtil.createOverridedFunctionSignatureStringFromDescriptor(
                element.getProject(), functionSignature, shortTypeNames);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.function.signature.family");
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, PsiFile file)
            throws IncorrectOperationException {
        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                createAction(project, editor).execute();
            }
        });
    }

    @NotNull
    private JetChangeFunctionSignatureAction createAction(@NotNull Project project, @NotNull Editor editor) {
        return new JetChangeFunctionSignatureAction(project, editor, element, possibleSignatures);
    }

    /**
     * Computes all the signatures a 'functionElement' could be changed to in order to remove NOTHING_TO_OVERRIDE error.
     */
    @NotNull
    private List<SimpleFunctionDescriptor> computePossibleSignatures(JetNamedFunction functionElement) {
        Project project = functionElement.getProject();
        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject().getBindingContext();
        SimpleFunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, functionElement);
        assert functionDescriptor != null;
        List<SimpleFunctionDescriptor> superFunctions = getPossibleSuperFunctionsDescriptors(functionDescriptor);
        Map<String, SimpleFunctionDescriptor> possibleSignatures = Maps.newHashMap();
        for (SimpleFunctionDescriptor superFunction : superFunctions) {
            if (!superFunction.getKind().isReal()) continue;
            SimpleFunctionDescriptor signature = changeSignatureToMatch(functionDescriptor, superFunction);
            possibleSignatures.put(getFunctionSignatureString(signature, /* shortTypeNames = */ false), signature);
        }
        return new ArrayList<SimpleFunctionDescriptor>(possibleSignatures.values());
    }

    /**
     *  Changes function's signature to match superFunction's signature. Returns new descriptor.
     */
    private static SimpleFunctionDescriptor changeSignatureToMatch(SimpleFunctionDescriptor function, SimpleFunctionDescriptor superFunction) {
        List<ValueParameterDescriptor> superParameters = superFunction.getValueParameters();
        List<ValueParameterDescriptor> parameters = function.getValueParameters();
        List<ValueParameterDescriptor> newParameters = new ArrayList<ValueParameterDescriptor>(superParameters);

        // Parameters in superFunction, which are matched in new function signature:
        ArrayList<Boolean> matched = new ArrayList<Boolean>(Collections.nCopies(superParameters.size(), false));
        // Parameters in this function, which are used in new function signature:
        ArrayList<Boolean> used = new ArrayList<Boolean>(Collections.nCopies(parameters.size(), false));

        matchParametersWithTheSameName(superParameters, parameters, newParameters, matched, used);

        matchParametersWithTheSameType(superParameters, parameters, newParameters, matched, used);

        Visibility newVisibility = getVisibility(function, superFunction);

        return CodegenUtil.replaceFunctionParameters(
                superFunction.copy(
                        function.getContainingDeclaration(),
                        Modality.OPEN,
                        newVisibility,
                        CallableMemberDescriptor.Kind.DELEGATION,
                        /* copyOverrides = */ true),
                newParameters);
    }

    /**
     * Returns new visibility for 'function' modified to override 'superFunction'.
     */
    private static Visibility getVisibility(SimpleFunctionDescriptor function, SimpleFunctionDescriptor superFunction) {
        Visibility superVisibility = superFunction.getVisibility();
        Visibility visibility = function.getVisibility();
        Visibility newVisibility = superVisibility;
        // If function has greater visibility than super function, keep function's visibility:
        Integer compareVisibilities = Visibilities.compare(visibility, superVisibility);
        if (compareVisibilities != null && compareVisibilities > 0) {
            newVisibility = visibility;
        }
        return newVisibility;
    }

    /**
     * Match function's parameters with super function's parameters of the same type (but possibly different names). Preserve ordering.
     * @param superParameters - super function's parameters
     * @param parameters - function's parameters
     * @param newParameters - new parameters (may be modified by this function)
     * @param matched - true iff this parameter in super function is matched by some parameter in function (may be modified by this function)
     * @param used - true iff this parameter in function is used to match some parameter in super function (may be modified by this function)
     */
    private static void matchParametersWithTheSameType(
            List<ValueParameterDescriptor> superParameters,
            List<ValueParameterDescriptor> parameters,
            List<ValueParameterDescriptor> newParameters,
            ArrayList<Boolean> matched,
            ArrayList<Boolean> used
    ) {
        int superIdx = 0;
        for (ValueParameterDescriptor superParameter : superParameters) {
            if (!matched.get(superIdx)) {
                int idx = 0;
                JetType superParameterType = superParameter.getType();
                for (ValueParameterDescriptor parameter : parameters) {
                    JetType parameterType = parameter.getType();
                    if (!used.get(idx) && JetTypeChecker.INSTANCE.equalTypes(superParameterType, parameterType)) {
                        used.set(idx, true);
                        matched.set(superIdx, true);
                        newParameters.set(superIdx, parameter);
                        break;
                    }
                    idx++;
                }
            }
            superIdx++;
        }
    }

    /**
     * Match function's parameters with super function's parameters of the same name (but possibly different types). Don't preserver ordering.
     * @param superParameters - super function's parameters
     * @param parameters - function's parameters
     * @param newParameters - new parameters (may be modified by this function)
     * @param matched - true iff this parameter in super function is matched by some parameter in function (may be modified by this function)
     * @param used - true iff this parameter in function is used to match some parameter in super function (may be modified by this function)
     */
    private static void matchParametersWithTheSameName(
            List<ValueParameterDescriptor> superParameters,
            List<ValueParameterDescriptor> parameters,
            List<ValueParameterDescriptor> newParameters,
            ArrayList<Boolean> matched,
            ArrayList<Boolean> used
    ) {
        int superIdx = 0;
        for (ValueParameterDescriptor superParameter : superParameters) {
            if (!matched.get(superIdx)) {
                int idx = 0;
                Name superName = superParameter.getName();
                for (ValueParameterDescriptor parameter : parameters) {
                    Name name = parameter.getName();
                    if (!used.get(idx) && name.equals(superName)) {
                        used.set(idx, true);
                        matched.set(superIdx, true);
                        newParameters.set(superIdx, superParameter);
                        break;
                    }
                    idx++;
                }
            }
            superIdx++;
        }
    }

    /**
     * Returns all open functions in superclasses which have the same name as 'functionDescriptor' (but possibly
     * different parameters/return type).
     */
    @NotNull
    private static List<SimpleFunctionDescriptor> getPossibleSuperFunctionsDescriptors(@NotNull SimpleFunctionDescriptor functionDescriptor) {
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        List<SimpleFunctionDescriptor> superFunctions = new LinkedList<SimpleFunctionDescriptor>();
        if (!(containingDeclaration instanceof ClassDescriptor)) return superFunctions;
        ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

        Name name = functionDescriptor.getName();
        for (ClassDescriptor superclass : DescriptorUtils.getSuperclassDescriptors(classDescriptor)) {
            JetType type = superclass.getDefaultType();
            JetScope scope = type.getMemberScope();
            for (FunctionDescriptor function : scope.getFunctions(name)) {
                if (!function.getKind().isReal()) continue;
                assert function instanceof SimpleFunctionDescriptor;
                SimpleFunctionDescriptor simpleFunctionDescriptor = (SimpleFunctionDescriptor) function;
                if (simpleFunctionDescriptor.getModality().isOverridable()) 
                    superFunctions.add(simpleFunctionDescriptor);
            }
        }
        return superFunctions;
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetNamedFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetNamedFunction.class);
                return function == null ? null : new ChangeMemberFunctionSignatureFix(function);
            }
        };
    }

    @Override
    public boolean showHint(@NotNull Editor editor) {
        if (possibleSignatures.isEmpty()) {
            return false;
        }

        Project project = editor.getProject();
        if (project == null) {
            return false;
        }

        if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) {
            return false;
        }

        return true;
    }
}
