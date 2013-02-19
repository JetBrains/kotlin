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
import com.intellij.codeInsight.template.TemplateBuilder;
import com.intellij.codeInsight.template.TemplateBuilderFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.inplace.MyLookupExpression;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.Collection;
import java.util.LinkedHashSet;

public class MapPlatformClassToKotlinFix extends JetIntentionAction<JetUserType> {
    LinkedHashSet<String> possibleTypes;

    public MapPlatformClassToKotlinFix(@NotNull JetUserType element, LinkedHashSet<String> possibleTypes) {
        super(element);
        this.possibleTypes = possibleTypes;
    }

    @NotNull
    @Override
    public String getText() {
        return possibleTypes.size() == 1
               ? JetBundle.message("map.platform.class.to.kotlin", element.getText(), possibleTypes.iterator().next())
               : JetBundle.message("map.platform.class.to.kotlin.multiple", element.getText());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("map.platform.class.to.kotlin.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetTypeReference newType = JetPsiFactory.createType(project, possibleTypes.iterator().next());
        JetTypeElement newTypeElement = newType.getTypeElement();
        assert newTypeElement != null;
        PsiElement replacedElement = element.replace(newTypeElement);

        if (possibleTypes.size() > 1) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
            TemplateBuilder builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(replacedElement);
            builder.replaceElement(replacedElement, new MyLookupExpression(replacedElement.getText(), possibleTypes, null, false, getText()));
            builder.run(editor, true);
        }
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetUserType type = QuickFixUtil.getParentElementOfType(diagnostic, JetUserType.class);
                if (type == null) return null;

                if (!(diagnostic instanceof DiagnosticWithParameters1)) return null;
                DiagnosticWithParameters1 parametrizedDiagnostic = (DiagnosticWithParameters1) diagnostic;
                Object a = parametrizedDiagnostic.getA();
                if (!(a instanceof Collection)) return null;

                Collection mappedClasses = (Collection)a;
                JetTypeArgumentList typeArgumentList = type.getTypeArgumentList();
                String typeArgumentsString = typeArgumentList == null ? "" : typeArgumentList.getText();
                LinkedHashSet<String> possibleTypes = new LinkedHashSet<String>();
                for (Object o : mappedClasses) {
                    if (!(o instanceof ClassDescriptor)) return null;
                    ClassDescriptor klass = (ClassDescriptor)o;
                    possibleTypes.add(klass.getName().toString() + typeArgumentsString);
                }
                if (possibleTypes.isEmpty()) return null;

                return new MapPlatformClassToKotlinFix(type, possibleTypes);
            }
        };
    }
}
