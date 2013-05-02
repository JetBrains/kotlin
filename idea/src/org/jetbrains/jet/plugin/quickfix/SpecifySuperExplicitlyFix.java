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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.template.*;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.inplace.MyLookupExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManagerUtil;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class SpecifySuperExplicitlyFix extends JetIntentionAction<JetSuperExpression> {
    private PsiElement elementToReplace;
    private final LinkedHashSet<String> options;


    public SpecifySuperExplicitlyFix(@NotNull JetSuperExpression element, @NotNull LinkedHashSet<String> options) {
        super(element);
        elementToReplace = element;
        this.options = options;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("specify.super.explicitly");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("specify.super.explicitly.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return !options.isEmpty();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        elementToReplace = elementToReplace.replace(JetPsiFactory.createExpression(project, options.iterator().next()));
        buildAndShowTemplate(project, editor, file, elementToReplace, options);
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetSuperExpression superExp = QuickFixUtil.getParentElementOfType(diagnostic, JetSuperExpression.class);
                if (superExp == null) {
                    return null;
                }

                JetClass klass = QuickFixUtil.getParentElementOfType(diagnostic, JetClass.class);
                if (klass == null) {
                    return null;
                }

                JetDelegationSpecifierList superClasses = PsiTreeUtil.getChildOfType(klass, JetDelegationSpecifierList.class);
                if (superClasses == null) {
                    return null;
                }

                //Used for checking visibility
                BindingContext contextClasses = KotlinCacheManagerUtil.getDeclarationsFromProject(superExp).getBindingContext();
                ClassDescriptor fromClass = contextClasses.get(BindingContext.CLASS, klass);
                if (fromClass == null) {
                    return null;
                }

                //Fetch class descriptors for all super classes
                LinkedHashSet<ClassDescriptor> superClassDescs = new LinkedHashSet<ClassDescriptor>();
                for (JetDelegationSpecifier delSpec : superClasses.getDelegationSpecifiers()) {
                    JetSimpleNameExpression jetRef = PsiTreeUtil.findChildOfType(delSpec.getTypeReference(), JetSimpleNameExpression.class);
                    if (jetRef == null) {
                        continue;
                    }
                    ClassDescriptor classDesc = resolveToClass(jetRef, contextClasses);
                    if (classDesc != null) {
                        superClassDescs.add(classDesc);
                    }
                }
                if (superClassDescs.isEmpty()) {
                    return null;
                }

                //Get the name of the member in question and other access information. The super expression
                //MUST be a part of a dot qualified expression.
                JetDotQualifiedExpression dotExp = QuickFixUtil.getParentElementOfType(diagnostic, JetDotQualifiedExpression.class);
                if (dotExp == null) {
                    return null;
                }
                JetExpression nextExp = dotExp;
                JetExpression currentExp = null;

                ArrayList<String> memberNames = new ArrayList<String>();
                //contains a null in the index where the memberName at that same index is of a property.
                ArrayList<JetValueArgumentList> argsForFunction = new ArrayList<JetValueArgumentList>();
                while (nextExp != null && (nextExp instanceof JetDotQualifiedExpression || nextExp instanceof JetArrayAccessExpression)) {
                    currentExp = nextExp;
                    String memberName;
                    JetValueArgumentList valArgs;

                    if (currentExp instanceof JetDotQualifiedExpression) {
                        JetCallExpression call = PsiTreeUtil.getChildOfType(currentExp, JetCallExpression.class);
                        if (call != null) {
                            JetSimpleNameExpression name = PsiTreeUtil.getChildOfType(call, JetSimpleNameExpression.class);
                            if (name == null) {
                                return null;
                            }
                            memberName = name.getText();
                            valArgs = call.getValueArgumentList();
                        }
                        else {
                            JetSimpleNameExpression name = PsiTreeUtil.getChildOfType(currentExp, JetSimpleNameExpression.class);
                            if (name == null) {
                                return null;
                            }
                            memberName = name.getText();
                            valArgs = null;
                        }
                    }
                    else {
                        //array indexing not supported for now.
                        return null;
                        /**
                        memberName = "get"; //array indexing is the same as get function call

                        JetContainerNode indexNode =  ((JetArrayAccessExpression) currentExp).getIndicesNode();
                        JetConstantExpression constant = PsiTreeUtil.getChildOfType(indexNode, JetConstantExpression.class);
                        JetReferenceExpression refIndex = PsiTreeUtil.getChildOfType(indexNode, JetReferenceExpression.class);
                        if (constant == null && refIndex == null) {
                            return null;
                        }
                        if (constant != null) {
                            valArgs = JetPsiFactory.createCallArguments(diagnostic.getPsiFile()
                            .getProject(), "(" + constant.getText() + ")");
                        }
                        else {
                            valArgs = JetPsiFactory.createCallArguments(diagnostic.getPsiFile()
                            .getProject(), "(" + refIndex.getText() + ")");
                        }**/
                    }
                    memberNames.add(memberName);
                    argsForFunction.add(valArgs);
                    nextExp = PsiTreeUtil.getParentOfType(currentExp, JetExpression.class);
                }
                if (memberNames.isEmpty()) {
                    return null;
                }

                //Right now, code has unexpected behavior with chained calls e.g. super.foo().bar.baz() so we just return null.
                if (memberNames.size() > 1) {
                    return null;
                }


                //Get the expected type of the expression if applicable (e.g. var a : Int = super.foo)
                BindingContext contextExpressions = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) diagnostic.getPsiFile())
                        .getBindingContext();
                JetProperty assignment = PsiTreeUtil.getParentOfType(currentExp, JetProperty.class);
                JetType expectedJetType = null;
                if (assignment != null) {
                    expectedJetType = contextExpressions.get(BindingContext.TYPE, assignment.getTypeRef());
                }
                //TODO with Expected Type, if it is part of a return statement, look at return type of the function.

                LinkedHashSet<String> options = new LinkedHashSet<String>();
                for (ClassDescriptor classDesc : superClassDescs) {
                    //ClassDescriptor currentClassDesc = classDesc;

                    boolean failed = false;
                    JetType returnType;
                    //for (int i = 0; i < memberNames.size(); i++) {
                                                            //should be currentClassDesc and .get(i)'s
                        returnType = memberNameAndArgsFound(classDesc, fromClass, memberNames.get(0),
                                                                    argsForFunction.get(0), contextExpressions);
                        /*
                        if (returnType == null) {
                            failed = true;
                            break;
                        }
                        //Update the class from which we see the next member.
                        fromClass = currentClassDesc;
                        TODO deal with generic types (especially in arrays) as the return type.
                        currentClassDesc = DescriptorUtils.getClassDescriptorForType(returnType);
                    }*/
                    if (!failed && returnType != null &&
                        (expectedJetType == null || JetTypeChecker.INSTANCE.isSubtypeOf(returnType, expectedJetType))) {
                        options.add("super<" + classDesc.getName().getIdentifier() + ">");
                    }
                }
                return new SpecifySuperExplicitlyFix(superExp, options);
            }
        };
    }

    /*returns null if false or error occured*/
    private static JetType memberNameAndArgsFound(@NotNull ClassDescriptor classDesc, @NotNull ClassDescriptor fromClass,
            @NotNull String memberName,
            @Nullable JetValueArgumentList valArgs, @NotNull BindingContext contextExpressions) {
        if (valArgs == null) {
            Collection<VariableDescriptor> varDescs = classDesc.getMemberScope(classDesc.getDefaultType().getArguments())
                    .getProperties(Name.identifier(memberName));
            return memberNamePropFound(classDesc, fromClass, varDescs);
        }
        else {
            Collection<FunctionDescriptor> funDescs = classDesc.getMemberScope(classDesc.getDefaultType().getArguments())
                    .getFunctions(Name.identifier(memberName));
            return memberNameFuncFound(fromClass, funDescs, valArgs, contextExpressions);

        }
    }

    private static JetType memberNameFuncFound(@NotNull ClassDescriptor fromClass,
            @NotNull Collection<FunctionDescriptor> funDescs,
            @NotNull JetValueArgumentList valArgs, @NotNull BindingContext contextExpressions) {
        for (FunctionDescriptor fun : funDescs) {
            if (fun.getModality() == Modality.ABSTRACT || !fun.getKind().isReal() ||
                    !Visibilities.isVisible(fun, fromClass)) {
                continue;
            }
            List<ValueParameterDescriptor> valParams = fun.getValueParameters();
            List<JetValueArgument> jValArgs = valArgs.getArguments();
            if (jValArgs.size() != valParams.size()) {
                continue;
            }
            boolean argTypesMatch = true;
            for (int j = 0; j < valParams.size(); j++) {
                JetExpression expr = jValArgs.get(j).getArgumentExpression();
                JetType subtype = contextExpressions.get(BindingContext.EXPRESSION_TYPE, expr);
                if (subtype == null || !JetTypeChecker.INSTANCE.isSubtypeOf(subtype, valParams.get(j).getType())) {
                    argTypesMatch = false;
                    break;
                }
            }
            if (!argTypesMatch) {
                continue;
            }
            return fun.getReturnType();
        }
        return null;
    }

    private static JetType memberNamePropFound(@NotNull ClassDescriptor classDesc, @NotNull ClassDescriptor fromClass,
            @NotNull Collection<VariableDescriptor> varDescs) {
        for (VariableDescriptor var : varDescs) {
            if (classDesc.getKind() != ClassKind.TRAIT && Visibilities.isVisible(var, fromClass)) {
                return var.getType();
            }
        }
        return null;
    }

    /*Taken and modified from MapPlatformClassToKotlinFix*/
    private static void buildAndShowTemplate(
            Project project, Editor editor, PsiFile file,
            PsiElement replacedElement, LinkedHashSet<String> options
    ) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        final CaretModel caretModel = editor.getCaretModel();
        final int oldOffset = caretModel.getOffset();
        caretModel.moveToOffset(file.getNode().getStartOffset());

        TemplateBuilderImpl builder = new TemplateBuilderImpl(file);
        Expression expression = new MyLookupExpression(replacedElement.getText(), options, null, false,
                                                       JetBundle.message("specify.super.explicitly.advertisement"));

        builder.replaceElement(replacedElement, null, expression, true);
        TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), new TemplateEditingAdapter() {
            @Override
            public void templateFinished(Template template, boolean brokenOff) {
                caretModel.moveToOffset(oldOffset);
            }
        });
    }

    /*Taken from MapPlatformClassToKotlinFix*/
    @Nullable
    private static ClassDescriptor resolveToClass(@NotNull JetReferenceExpression referenceExpression, @NotNull BindingContext context) {
        DeclarationDescriptor descriptor = context.get(BindingContext.REFERENCE_TARGET, referenceExpression);
        Collection<? extends DeclarationDescriptor> ambiguousTargets =
                context.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression);
        if (descriptor instanceof ClassDescriptor) {
            return (ClassDescriptor) descriptor;
        }
        else if (ambiguousTargets != null) {
            for (DeclarationDescriptor target : ambiguousTargets) {
                if (target instanceof ClassDescriptor) {
                    return (ClassDescriptor) target;
                }
            }
        }
        return null;
    }
}
