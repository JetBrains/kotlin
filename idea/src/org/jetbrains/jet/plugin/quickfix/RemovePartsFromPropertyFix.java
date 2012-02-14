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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author svtk
 */
public class RemovePartsFromPropertyFix extends JetIntentionAction<JetProperty> {
    private final JetType type;
    private final boolean removeInitializer;
    private final boolean removeGetter;
    private final boolean removeSetter;
    
    private RemovePartsFromPropertyFix(@NotNull JetProperty element, @Nullable JetType type, boolean removeInitializer, boolean removeGetter, boolean removeSetter) {
        super(element);
        this.type = type;
        this.removeInitializer = removeInitializer;
        this.removeGetter = removeGetter;
        this.removeSetter = removeSetter;
    }
    
    private RemovePartsFromPropertyFix(@NotNull JetProperty element, @Nullable JetType type) {
        this(element, type, element.getInitializer() != null,
             element.getGetter() != null && element.getGetter().getBodyExpression() != null,
             element.getSetter() != null && element.getSetter().getBodyExpression() != null);
    }

    private RemovePartsFromPropertyFix(@NotNull JetProperty element, boolean removeInitializer, boolean removeGetter, boolean removeSetter) {
        this(element, null, removeInitializer, removeGetter, removeSetter);
    }

    private static String partsToRemove(boolean getter, boolean setter, boolean initializer) {
        StringBuilder sb = new StringBuilder();
        if (getter) {
            sb.append("getter");
            if (setter && initializer) {
                sb.append(", ");
            }
            else if (setter || initializer) {
                sb.append(" and ");
            }
        }
        if (setter) {
            sb.append("setter");
            if (initializer) {
                sb.append(" and ");
            }
        }
        if (initializer) {
            sb.append("initializer");
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.parts.from.property", partsToRemove(removeGetter, removeSetter, removeInitializer));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.parts.from.property.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!(file instanceof JetFile)) return;
        JetProperty newElement = (JetProperty) element.copy();
        JetPropertyAccessor getter = newElement.getGetter();
        if (removeGetter && getter != null) {
            newElement.deleteChildInternal(getter.getNode());
        }
        JetPropertyAccessor setter = newElement.getSetter();
        if (removeSetter && setter != null) {
            newElement.deleteChildInternal(setter.getNode());
        }
        JetExpression initializer = newElement.getInitializer();
        boolean needImport = false;
        if (removeInitializer && initializer != null) {
            PsiElement nameIdentifier = newElement.getNameIdentifier();
            assert nameIdentifier != null;
            PsiElement nextSibling = nameIdentifier.getNextSibling();
            assert nextSibling != null;
            newElement.deleteChildRange(nextSibling, initializer);

            if (newElement.getPropertyTypeRef() == null && type != null) {
                newElement = AddReturnTypeFix.addPropertyType(project, newElement, type);
                needImport = true;
            }
        }
        if (needImport) {
            ImportClassHelper.addImportDirectiveIfNeeded(type, (JetFile)file);
        }
        element.replace(newElement);
    }

    public static JetIntentionActionFactory<JetProperty> createFactory() {
        return new JetIntentionActionFactory<JetProperty>() {
            @Override
            public JetIntentionAction<JetProperty> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetProperty;
                DiagnosticWithParameters<PsiElement> diagnosticWithParameters = assertAndCastToDiagnosticWithParameters(diagnostic, DiagnosticParameters.TYPE);
                JetType type = diagnosticWithParameters.getParameter(DiagnosticParameters.TYPE);
                return new RemovePartsFromPropertyFix((JetProperty) diagnostic.getPsiElement(), type);
            }
        };
    }

    public static JetIntentionActionFactory<JetProperty> createRemoveInitializerFactory() {
        return new JetIntentionActionFactory<JetProperty>() {
            @Override
            public JetIntentionAction<JetProperty> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetProperty;
                return new RemovePartsFromPropertyFix((JetProperty) diagnostic.getPsiElement(), true, false, false);
            }
        };
    }
}
