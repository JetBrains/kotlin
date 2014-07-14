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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class MapPlatformClassToKotlinFix extends JetIntentionAction<JetReferenceExpression> {
    private static final String PRIMARY_USAGE = "PrimaryUsage";
    private static final String OTHER_USAGE = "OtherUsage";

    private final ClassDescriptor platformClass;
    private final Collection<ClassDescriptor> possibleClasses;

    public MapPlatformClassToKotlinFix(@NotNull JetReferenceExpression element, @NotNull ClassDescriptor platformClass,
            @NotNull Collection<ClassDescriptor> possibleClasses) {
        super(element);
        this.platformClass = platformClass;
        this.possibleClasses = possibleClasses;
    }

    @NotNull
    @Override
    public String getText() {
        String platformClassQualifiedName = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(platformClass.getDefaultType());
        return possibleClasses.size() == 1
               ? JetBundle.message("map.platform.class.to.kotlin", platformClassQualifiedName,
                                   DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(possibleClasses.iterator().next().getDefaultType()))
               : JetBundle.message("map.platform.class.to.kotlin.multiple", platformClassQualifiedName);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("map.platform.class.to.kotlin.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        BindingContext context = ResolvePackage.getBindingContext(file);
        Iterable<Diagnostic> diagnostics = context.getDiagnostics();
        List<JetImportDirective> imports = new ArrayList<JetImportDirective>();
        List<JetUserType> usages = new ArrayList<JetUserType>();

        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getFactory() != Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN) continue;
            JetReferenceExpression refExpr = getImportOrUsageFromDiagnostic(diagnostic);
            if (refExpr == null) continue;
            ClassDescriptor descriptor = resolveToClass(refExpr, context);
            if (descriptor == null || !(descriptor.equals(platformClass))) continue;
            JetImportDirective imp = PsiTreeUtil.getParentOfType(refExpr, JetImportDirective.class);
            if (imp == null) {
                JetUserType type = PsiTreeUtil.getParentOfType(refExpr, JetUserType.class);
                if (type == null) continue;
                usages.add(type);
            }
            else {
                imports.add(imp);
            }
        }

        for (JetImportDirective imp : imports) {
            imp.delete();
        }

        if (usages.isEmpty()) { // if we are not going to replace any usages, there's no reason to continue at all
            return;
        }

        List<PsiElement> replacedElements = replaceUsagesWithFirstClass(project, usages);

        if (possibleClasses.size() > 1) {
            LinkedHashSet<String> possibleTypes = new LinkedHashSet<String>();
            for (ClassDescriptor klass : possibleClasses) {
                possibleTypes.add(klass.getName().asString());
            }
            buildAndShowTemplate(project, editor, file, replacedElements, possibleTypes);
        }
    }

    private List<PsiElement> replaceUsagesWithFirstClass(Project project, List<JetUserType> usages) {
        ClassDescriptor replacementClass = possibleClasses.iterator().next();
        String replacementClassName = replacementClass.getName().asString();
        List<PsiElement> replacedElements = new ArrayList<PsiElement>();
        for (JetUserType usage : usages) {
            JetTypeArgumentList typeArguments = usage.getTypeArgumentList();
            String typeArgumentsString = typeArguments == null ? "" : typeArguments.getText();
            JetTypeReference replacementType = JetPsiFactory(project).createType(replacementClassName + typeArgumentsString);
            JetTypeElement replacementTypeElement = replacementType.getTypeElement();
            assert replacementTypeElement != null;
            PsiElement replacedElement = usage.replace(replacementTypeElement);
            PsiElement replacedExpression = replacedElement.getFirstChild();
            assert replacedExpression instanceof JetSimpleNameExpression; // assumption: the Kotlin class requires no imports
            replacedElements.add(replacedExpression);
        }
        return replacedElements;
    }

    private static void buildAndShowTemplate(
            Project project, Editor editor, PsiFile file,
            Collection<PsiElement> replacedElements, LinkedHashSet<String> options
    ) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        PsiElement primaryReplacedExpression = replacedElements.iterator().next();

        final CaretModel caretModel = editor.getCaretModel();
        final int oldOffset = caretModel.getOffset();
        caretModel.moveToOffset(file.getNode().getStartOffset());

        TemplateBuilderImpl builder = new TemplateBuilderImpl(file);
        Expression expression = new MyLookupExpression(primaryReplacedExpression.getText(), options, null, null, false,
                                                       JetBundle.message("map.platform.class.to.kotlin.advertisement"));

        builder.replaceElement(primaryReplacedExpression, PRIMARY_USAGE, expression, true);
        for (PsiElement replacedExpression : replacedElements) {
            if (replacedExpression == primaryReplacedExpression) continue;
            builder.replaceElement(replacedExpression, OTHER_USAGE, PRIMARY_USAGE, false);
        }
        TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), new TemplateEditingAdapter() {
            @Override
            public void templateFinished(Template template, boolean brokenOff) {
                caretModel.moveToOffset(oldOffset);
            }
        });
    }

    private static JetReferenceExpression getImportOrUsageFromDiagnostic(@NotNull Diagnostic diagnostic) {
        JetImportDirective imp = QuickFixUtil.getParentElementOfType(diagnostic, JetImportDirective.class);
        JetReferenceExpression typeExpr;
        if (imp == null) {
            JetUserType type = QuickFixUtil.getParentElementOfType(diagnostic, JetUserType.class);
            if (type == null) return null;
            typeExpr = type.getReferenceExpression();
        } else {
            JetExpression importRef = imp.getImportedReference();
            if (importRef == null || !(importRef instanceof JetDotQualifiedExpression)) return null;
            JetExpression refExpr = ((JetDotQualifiedExpression) importRef).getSelectorExpression();
            if (refExpr == null || !(refExpr instanceof JetReferenceExpression)) return null;
            typeExpr = (JetReferenceExpression) refExpr;
        }
        return typeExpr;
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetReferenceExpression typeExpr = getImportOrUsageFromDiagnostic(diagnostic);
                if (typeExpr == null) return null;

                PsiFile psiFile = diagnostic.getPsiFile();
                if (!(psiFile instanceof JetFile)) return null;

                BindingContext context = ResolvePackage.getBindingContext((JetFile) psiFile);
                ClassDescriptor platformClass = resolveToClass(typeExpr, context);
                if (platformClass == null) return null;

                DiagnosticWithParameters1<JetElement, Collection<ClassDescriptor>> parametrizedDiagnostic =
                        Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN.cast(diagnostic);

                return new MapPlatformClassToKotlinFix(typeExpr, platformClass, parametrizedDiagnostic.getA());
            }
        };
    }

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
