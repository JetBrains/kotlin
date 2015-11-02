/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNullableType;
import org.jetbrains.kotlin.psi.KtTypeElement;

public class RemoveNullableFix extends KotlinQuickFixAction<KtNullableType> {
    public enum NullableKind {
        REDUNDANT, SUPERTYPE, USELESS
    }
    private final NullableKind typeOfError;

    public RemoveNullableFix(@NotNull KtNullableType element, @NotNull NullableKind type) {
        super(element);
        typeOfError = type;
    }

    @NotNull
    @Override
    public String getText() {
        switch (typeOfError) {
            case REDUNDANT:
                return KotlinBundle.message("remove.redundant.nullable");
            case SUPERTYPE:
                return KotlinBundle.message("remove.supertype.nullable");
            default:
                return KotlinBundle.message("remove.useless.nullable");
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("remove.nullable.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        KtTypeElement type = super.getElement().getInnerType();
        assert type != null : "No inner type " + getElement().getText() + ", should have been rejected in createFactory()";
        super.getElement().replace(type);
    }

    public static KotlinSingleIntentionActionFactory createFactory(final NullableKind typeOfError) {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtNullableType> createAction(Diagnostic diagnostic) {
                KtNullableType nullType = QuickFixUtil.getParentElementOfType(diagnostic, KtNullableType.class);
                if (nullType == null || nullType.getInnerType() == null) return null;
                return new RemoveNullableFix(nullType, typeOfError);
            }
        };
    }
}
