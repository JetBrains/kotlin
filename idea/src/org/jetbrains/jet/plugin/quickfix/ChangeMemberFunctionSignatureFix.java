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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.FunctionDescriptorUtil;
import org.jetbrains.jet.lang.resolve.VisibilityUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.actions.JetChangeFunctionSignatureAction;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;

import java.util.*;

/**
 * Fix that changes member function's signature to match one of super functions' signatures.
 */
public class ChangeMemberFunctionSignatureFix extends JetHintAction<JetNamedFunction> {
    private final List<FunctionDescriptor> possibleSignatures;

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
                                     getFunctionSignatureString(possibleSignatures.get(0)));
        else
            return JetBundle.message("change.function.signature.action.multiple");
    }

    @NotNull
    private static String getFunctionSignatureString(@NotNull FunctionDescriptor functionSignature) {
        return JetChangeFunctionSignatureAction.SIGNATURE_RENDERER.render(functionSignature);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.function.signature.family");
    }

    @Override
    protected void invoke(@NotNull final Project project, @NotNull final Editor editor, JetFile file)
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
    private static List<FunctionDescriptor> computePossibleSignatures(@NotNull JetNamedFunction functionElement) {
        if (functionElement.getValueParameterList() == null) { // we won't be able to modify its signature
            return Collections.emptyList();
        }

        BindingContext context = ResolvePackage.getBindingContext(functionElement);
        FunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, functionElement);
        if (functionDescriptor == null) return Lists.newArrayList();
        List<FunctionDescriptor> superFunctions = getPossibleSuperFunctionsDescriptors(functionDescriptor);
        final Map<String, FunctionDescriptor> possibleSignatures = Maps.newHashMap();
        for (FunctionDescriptor superFunction : superFunctions) {
            if (!superFunction.getKind().isReal()) continue;
            FunctionDescriptor signature = changeSignatureToMatch(functionDescriptor, superFunction);
            possibleSignatures.put(getFunctionSignatureString(signature), signature);
        }
        List<String> keys = new ArrayList<String>(possibleSignatures.keySet());
        Collections.sort(keys);
        return new ArrayList<FunctionDescriptor>(Collections2.transform(keys, new Function<String, FunctionDescriptor>() {
            @Override
            public FunctionDescriptor apply(String key) {
                return possibleSignatures.get(key);
            }
        }));
    }

    /**
     *  Changes function's signature to match superFunction's signature. Returns new descriptor.
     */
    private static FunctionDescriptor changeSignatureToMatch(FunctionDescriptor function, FunctionDescriptor superFunction) {
        List<ValueParameterDescriptor> superParameters = superFunction.getValueParameters();
        List<ValueParameterDescriptor> parameters = function.getValueParameters();
        List<ValueParameterDescriptor> newParameters = Lists.newArrayList(superParameters);

        // Parameters in superFunction, which are matched in new function signature:
        BitSet matched = new BitSet(superParameters.size());
        // Parameters in this function, which are used in new function signature:
        BitSet used = new BitSet(superParameters.size());

        matchParameters(MATCH_NAMES, superParameters, parameters, newParameters, matched, used);
        matchParameters(MATCH_TYPES, superParameters, parameters, newParameters, matched, used);

        FunctionDescriptor newFunction = FunctionDescriptorUtil.replaceFunctionParameters(
                superFunction.copy(
                        function.getContainingDeclaration(),
                        Modality.OPEN,
                        getVisibility(function, superFunction),
                        CallableMemberDescriptor.Kind.DELEGATION,
                        /* copyOverrides = */ true),
                newParameters);
        newFunction.addOverriddenDescriptor(superFunction);
        return newFunction;
    }

    /**
     * Returns new visibility for 'function' modified to override 'superFunction'.
     */
    private static Visibility getVisibility(FunctionDescriptor function, FunctionDescriptor superFunction) {
        ArrayDeque<CallableMemberDescriptor> descriptors =
                Queues.<CallableMemberDescriptor>newArrayDeque(Arrays.asList(superFunction, function));
        return VisibilityUtil.findMemberWithMaxVisibility(descriptors).getVisibility();
    }

    /** Helper interface for matchParameters(..) method. */
    private interface ParameterChooser {
        /**
         * Checks if 'parameter' may be used to match 'superParameter'.
         * If so, returns (possibly modified) descriptor to be used as the new parameter.
         * If not, returns null.
         */
        @Nullable
        ValueParameterDescriptor choose(@NotNull ValueParameterDescriptor parameter, @NotNull ValueParameterDescriptor superParameter);
    }

    private static final ParameterChooser MATCH_NAMES = new ParameterChooser() {
        @Nullable
        @Override
        public ValueParameterDescriptor choose(
                @NotNull ValueParameterDescriptor parameter,
                @NotNull ValueParameterDescriptor superParameter
        ) {
            return parameter.getName().equals(superParameter.getName()) ? superParameter : null;
        }
    };

    private static final ParameterChooser MATCH_TYPES = new ParameterChooser() {
        @Nullable
        @Override
        public ValueParameterDescriptor choose(
                @NotNull ValueParameterDescriptor parameter,
                @NotNull ValueParameterDescriptor superParameter
        ) {
            // TODO: support for generic functions
            if (JetTypeChecker.DEFAULT.equalTypes(parameter.getType(), superParameter.getType())) {
                return superParameter.copy(parameter.getContainingDeclaration(), parameter.getName());
            }
            else {
                return null;
            }
        }
    };

    /**
     * Match function's parameters with super function's parameters using parameterChooser.
     * Doesn't have to preserve ordering, parameter names or types.
     * @param superParameters - super function's parameters
     * @param parameters - function's parameters
     * @param newParameters - new parameters (may be modified by this function)
     * @param matched - true iff this parameter in super function is matched by some parameter in function (may be modified by this function)
     * @param used - true iff this parameter in function is used to match some parameter in super function (may be modified by this function)
     */
    private static void matchParameters(
            @NotNull ParameterChooser parameterChooser,
            @NotNull List<ValueParameterDescriptor> superParameters,
            @NotNull List<ValueParameterDescriptor> parameters,
            @NotNull List<ValueParameterDescriptor> newParameters,
            @NotNull BitSet matched,
            @NotNull BitSet used
    ) {
        for (ValueParameterDescriptor superParameter : superParameters) {
            if (!matched.get(superParameter.getIndex())) {
                for (ValueParameterDescriptor parameter : parameters) {
                    ValueParameterDescriptor choice = parameterChooser.choose(parameter, superParameter);
                    if (!used.get(parameter.getIndex()) && choice != null) {
                        used.set(parameter.getIndex(), true);
                        matched.set(superParameter.getIndex(), true);
                        newParameters.set(superParameter.getIndex(), choice);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns all open functions in superclasses which have the same name as 'functionDescriptor' (but possibly
     * different parameters/return type).
     */
    @NotNull
    private static List<FunctionDescriptor> getPossibleSuperFunctionsDescriptors(@NotNull FunctionDescriptor functionDescriptor) {
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        List<FunctionDescriptor> superFunctions = Lists.newArrayList();
        if (!(containingDeclaration instanceof ClassDescriptor)) return superFunctions;
        ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

        Name name = functionDescriptor.getName();
        for (JetType type : TypeUtils.getAllSupertypes(classDescriptor.getDefaultType())) {
            JetScope scope = type.getMemberScope();
            for (FunctionDescriptor function : scope.getFunctions(name)) {
                if (!function.getKind().isReal()) continue;
                if (function.getModality().isOverridable())
                    superFunctions.add(function);
            }
        }
        return superFunctions;
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
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
