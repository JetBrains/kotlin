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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author svtk
 */
public class AddReturnTypeFix extends JetIntentionAction<JetNamedDeclaration> {
    public AddReturnTypeFix(@NotNull JetNamedDeclaration element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("add.return.type");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.return.type");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        JetType type = QuickFixUtil.getDeclarationReturnType(element);
        return super.isAvailable(project, editor, file) && type != null && !ErrorUtils.isErrorType(type);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!(file instanceof JetFile)) return;
        PsiElement newElement;
        JetType type = QuickFixUtil.getDeclarationReturnType(element);
        if (type == null) return;
        if (element instanceof JetProperty) {
            newElement = addPropertyType(project, (JetProperty) element, type);
        }
        else {
            assert element instanceof JetFunction;
            newElement = addFunctionType(project, (JetFunction) element, type);
        }
        ImportClassHelper.addImportDirectiveIfNeeded(type, (JetFile) file);
        element.replace(newElement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static JetProperty addPropertyType(@NotNull Project project, @NotNull JetProperty property, @NotNull JetType type) {
        JetProperty newProperty = (JetProperty) property.copy();
        JetTypeReference typeReference = JetPsiFactory.createType(project, type.toString());
        Pair<PsiElement, PsiElement> colon = JetPsiFactory.createColon(project);
        PsiElement nameIdentifier = newProperty.getNameIdentifier();
        addTypeReference(newProperty, typeReference, colon, nameIdentifier);
        return newProperty;
    }

    public static JetFunction addFunctionType(@NotNull Project project, @NotNull JetFunction function, @NotNull JetType type) {
        JetFunction newFunction = (JetFunction) function.copy();
        JetTypeReference typeReference = JetPsiFactory.createType(project, type.toString());
        Pair<PsiElement, PsiElement> colon = JetPsiFactory.createColon(project);
        JetParameterList valueParameterList = newFunction.getValueParameterList();
        addTypeReference(newFunction, typeReference, colon, valueParameterList);
        return newFunction;
    }

    private static void addTypeReference(JetNamedDeclaration element, JetTypeReference typeReference, Pair<PsiElement, PsiElement> colon, PsiElement anchor) {
        assert anchor != null;
        element.addAfter(typeReference, anchor);
        element.addRangeAfter(colon.getFirst(), colon.getSecond(), anchor);
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetNamedDeclaration> createAction(Diagnostic diagnostic) {
                JetNamedDeclaration declaration = QuickFixUtil.getParentElementOfType(diagnostic, JetNamedDeclaration.class);
                if (declaration == null) return null;
                return new AddReturnTypeFix(declaration);
            }
        };
    }
}
