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
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;

public class ChangeAccessorTypeFix extends KotlinQuickFixAction<KtPropertyAccessor> {
    private KotlinType type;

    public ChangeAccessorTypeFix(@NotNull KtPropertyAccessor element) {
        super(element);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        KtProperty property = PsiTreeUtil.getParentOfType(getElement(), KtProperty.class);
        if (property == null) return false;
        KotlinType type = QuickFixUtil.getDeclarationReturnType(property);
        if (super.isAvailable(project, editor, file) && type != null && !type.isError()) {
            this.type = type;
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message(
                getElement().isGetter() ? "change.getter.type" : "change.setter.type",
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        );
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("change.accessor.type");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        KtTypeReference newTypeReference = KtPsiFactoryKt
                .KtPsiFactory(file).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type));

        KtTypeReference typeReference;
        if (getElement().isGetter()) {
            typeReference = getElement().getReturnTypeReference();
        }
        else {
            KtParameter parameter = getElement().getParameter();
            assert parameter != null;
            typeReference = parameter.getTypeReference();
        }
        assert typeReference != null;

        newTypeReference = (KtTypeReference) typeReference.replace(newTypeReference);
        ShortenReferences.DEFAULT.process(newTypeReference);
    }

    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtPropertyAccessor> createAction(@NotNull Diagnostic diagnostic) {
                KtPropertyAccessor accessor = QuickFixUtil.getParentElementOfType(diagnostic, KtPropertyAccessor.class);
                if (accessor == null) return null;
                return new ChangeAccessorTypeFix(accessor);
            }
        };
    }
}
