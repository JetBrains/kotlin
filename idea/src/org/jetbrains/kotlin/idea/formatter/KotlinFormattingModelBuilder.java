/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.formatter;

import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.FormattingDocumentModelImpl;
import com.intellij.psi.formatter.PsiBasedFormattingModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;

public class KotlinFormattingModelBuilder implements FormattingModelBuilder {
    @NotNull
    @Override
    public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
        PsiFile containingFile = element.getContainingFile().getViewProvider().getPsi(KotlinLanguage.INSTANCE);
        KotlinBlock block = new KotlinBlock(
                containingFile.getNode(), NodeAlignmentStrategy.getNullStrategy(), Indent.getNoneIndent(), null, settings,
                KotlinSpacingRulesKt.createSpacingBuilder(settings, KotlinSpacingBuilderUtilImpl.INSTANCE));

        //TODO: this is temporary code to allow formatting non-physical files in non-UI thread (used by conversion from Java to Kotlin)
        // it's needed until IDEA's issue with this document being created with wrong threading policy is fixed
        if (!element.isPhysical()) {
            FormattingDocumentModelImpl formattingDocumentModel =
                    new FormattingDocumentModelImpl(new DocumentImpl(containingFile.getViewProvider().getContents(), true), containingFile);
            return new PsiBasedFormattingModel(containingFile, block, formattingDocumentModel);
        }

        if (element instanceof PsiFile) {
            FormattingModel collectChangesModel = CollectChangesWithoutApplyModelKt.createCollectFormattingChangesModel((PsiFile) element, block);
            if (collectChangesModel != null) {
                return collectChangesModel;
            }
        }

        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), block, settings);
    }

    @Override
    public TextRange getRangeAffectingIndent(PsiFile psiFile, int i, ASTNode astNode) {
        return null;
    }
}
