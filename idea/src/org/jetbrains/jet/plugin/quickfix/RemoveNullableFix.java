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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetNullableType;
import org.jetbrains.jet.lang.psi.JetTypeElement;
import org.jetbrains.jet.plugin.JetBundle;

public class RemoveNullableFix extends JetIntentionAction<JetNullableType> {
    public enum NullableKind {
        REDUNDANT, SUPERTYPE, USELESS
    }
    private final NullableKind typeOfError;

    public RemoveNullableFix(@NotNull JetNullableType element, @NotNull NullableKind type) {
        super(element);
        typeOfError = type;
    }

    @NotNull
    @Override
    public String getText() {
        switch (typeOfError) {
            case REDUNDANT:
                return JetBundle.message("remove.redundant.nullable");
            case SUPERTYPE:
                return JetBundle.message("remove.supertype.nullable");
            default:
                return JetBundle.message("remove.useless.nullable");
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.nullable.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetTypeElement type = super.element.getInnerType();
        super.element.replace(type);
    }

    public static JetIntentionActionFactory createFactory(final NullableKind typeOfError) {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetNullableType> createAction(Diagnostic diagnostic) {
                JetNullableType nullType = QuickFixUtil.getParentElementOfType(diagnostic, JetNullableType.class);
                if (nullType == null) return null;
                return new RemoveNullableFix(nullType, typeOfError);
            }
        };
    }
}
