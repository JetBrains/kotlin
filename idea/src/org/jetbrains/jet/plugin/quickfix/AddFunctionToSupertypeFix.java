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

import com.google.common.collect.Lists;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.actions.JetAddFunctionToClassifierAction;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AddFunctionToSupertypeFix extends JetHintAction<JetNamedFunction> {
    private final List<FunctionDescriptor> functionsToAdd;

    public AddFunctionToSupertypeFix(JetNamedFunction element) {
        super(element);
        functionsToAdd = generateFunctionsToAdd(element);
    }

    private static List<FunctionDescriptor> generateFunctionsToAdd(JetNamedFunction functionElement) {
        FunctionDescriptor functionDescriptor =
                (FunctionDescriptor) ResolvePackage.getLazyResolveSession(functionElement).resolveToDescriptor(functionElement);

        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return Collections.emptyList();

        List<FunctionDescriptor> functions = Lists.newArrayList();
        ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
        // TODO: filter out impossible supertypes (for example when argument's type isn't visible in a superclass).
        for (ClassDescriptor supertypeDescriptor : getSupertypes(classDescriptor)) {
            if (KotlinBuiltIns.getInstance().isAnyOrNullableAny(supertypeDescriptor.getDefaultType())) continue;
            functions.add(generateFunctionSignatureForType(functionDescriptor, supertypeDescriptor));
        }
        return functions;
    }

    private static List<ClassDescriptor> getSupertypes(ClassDescriptor classDescriptor) {
        List<JetType> supertypes = Lists.newArrayList(TypeUtils.getAllSupertypes(classDescriptor.getDefaultType()));
        Collections.sort(supertypes, new Comparator<JetType>() {
            @Override
            public int compare(JetType o1, JetType o2) {
                if (o1.equals(o2)) {
                    return 0;
                }
                if (JetTypeChecker.DEFAULT.isSubtypeOf(o1, o2)) {
                    return -1;
                }
                if (JetTypeChecker.DEFAULT.isSubtypeOf(o2, o1)) {
                    return 1;
                }
                return o1.toString().compareTo(o2.toString());
            }
        });
        List<ClassDescriptor> supertypesDescriptors = Lists.newArrayList();
        for (JetType supertype : supertypes) {
            supertypesDescriptors.add(DescriptorUtils.getClassDescriptorForType(supertype));
        }
        return supertypesDescriptors;
    }

    private static FunctionDescriptor generateFunctionSignatureForType(
            FunctionDescriptor functionDescriptor,
            ClassDescriptor typeDescriptor
    ) {
        // TODO: support for generics.
        Modality modality = typeDescriptor.getModality();
        if (typeDescriptor.getKind() == ClassKind.TRAIT) {
            modality = Modality.OPEN;
        }
        return functionDescriptor.copy(
                typeDescriptor,
                modality,
                functionDescriptor.getVisibility(),
                CallableMemberDescriptor.Kind.DECLARATION,
                /* copyOverrides = */ false);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && !functionsToAdd.isEmpty();
    }

    @Override
    public boolean showHint(Editor editor) {
        return false;
    }

    @NotNull
    @Override
    public String getText() {
        if (functionsToAdd.size() == 1) {
            FunctionDescriptor newFunction = functionsToAdd.get(0);
            ClassDescriptor supertype = (ClassDescriptor) newFunction.getContainingDeclaration();
            return JetBundle.message("add.function.to.type.action.single",
                                     CodeInsightUtils.createFunctionSignatureStringFromDescriptor(newFunction, /* shortTypeNames */ true),
                                     supertype.getName().toString());
        }
        else {
            return JetBundle.message("add.function.to.supertype.action.multiple");
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.function.to.supertype.family");
    }

    @Override
    protected void invoke(@NotNull final Project project, final Editor editor, final JetFile file) {
        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                createAction(project, editor, file).execute();
            }
        });
    }

    @NotNull
    private JetAddFunctionToClassifierAction createAction(Project project, Editor editor, JetFile file) {
        return new JetAddFunctionToClassifierAction(project, editor, functionsToAdd);
    }

    public static JetIntentionActionsFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetNamedFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetNamedFunction.class);
                return function == null ? null : new AddFunctionToSupertypeFix(function);
            }
        };
    }
}
