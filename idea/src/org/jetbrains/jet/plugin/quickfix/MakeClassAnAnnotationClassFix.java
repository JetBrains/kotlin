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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;

import static org.jetbrains.jet.lexer.JetTokens.ANNOTATION_KEYWORD;

public class MakeClassAnAnnotationClassFix extends JetIntentionAction<JetAnnotationEntry> {
    private final JetAnnotationEntry annotationEntry;
    private JetType annotationType;
    private JetClass annotationClass;

    public MakeClassAnAnnotationClassFix(@NotNull JetAnnotationEntry annotationEntry) {
        super(annotationEntry);
        this.annotationEntry = annotationEntry;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject().getBindingContext();
        AnnotationDescriptor annotationDescriptor = context.get(BindingContext.ANNOTATION, annotationEntry);
        if (annotationDescriptor == null) {
            return false;
        }
        annotationType = annotationDescriptor.getType();
        DeclarationDescriptor declarationDescriptor = annotationType.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor == null) {
            return false;
        }
        PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context, declarationDescriptor);
        if (declaration instanceof JetClass) {
            annotationClass = (JetClass) declaration;
            return annotationClass.isWritable();
        }
        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("make.class.annotation.class", annotationType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("make.class.annotation.class.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        annotationClass.replace(AddModifierFix.addModifier(annotationClass, ANNOTATION_KEYWORD, null, project, false));
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetAnnotationEntry annotation = QuickFixUtil.getParentElementOfType(diagnostic, JetAnnotationEntry.class);
                return annotation == null ? null : new MakeClassAnAnnotationClassFix(annotation);
            }
        };
    }
}

