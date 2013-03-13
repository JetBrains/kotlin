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

package org.jetbrains.jet.plugin.conversion.copy;

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.editor.JetEditorOptions;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

public class JavaCopyPastePostProcessor implements CopyPastePostProcessor<TextBlockTransferableData> {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.conversion.copy.JavaCopyPastePostProcessor");

    @Override
    public TextBlockTransferableData collectTransferableData(@NotNull PsiFile file, Editor editor, @NotNull int[] startOffsets, @NotNull int[] endOffsets) {
        try {
            if (!(file instanceof PsiJavaFile)) {
                return null;
            }
            String eol = System.getProperty("line.separator");
            List<PsiElement> buffer = getSelectedElements(file, startOffsets, endOffsets);
            StringBuilder result = new StringBuilder();

            for (PsiElement e : buffer) {
                String converted = new Converter(file.getProject()).elementToKotlin(e);
                if (!converted.isEmpty()) {
                    result.append(converted).append(eol);
                }
            }
            return new ConvertedCode(StringUtil.convertLineSeparators(result.toString()));
        } catch (Throwable t) {
            LOG.error(t);
        }
        return null;
    }

    @NotNull
    private static List<PsiElement> getSelectedElements(@NotNull PsiFile file, @NotNull int[] startOffsets, @NotNull int[] endOffsets) {
        ArrayList<PsiElement> buffer = new ArrayList<PsiElement>();

        assert startOffsets.length == endOffsets.length : "Must have the same length";

        for (int i = 0; i < startOffsets.length; i++) {
            int startOffset = startOffsets[i];
            int endOffset = endOffsets[i];
            PsiElement elem = file.findElementAt(startOffset);
            while (elem != null && elem.getParent() != null && !(elem.getParent() instanceof PsiFile) &&
                   elem.getParent().getTextRange().getEndOffset() <= endOffset) {
                elem = elem.getParent();
            }
            buffer.add(elem);

            while (elem != null && elem.getTextRange().getEndOffset() < endOffset) {
                elem = elem.getNextSibling();
                buffer.add(elem);
            }
        }
        return buffer;
    }

    @Override
    public TextBlockTransferableData extractTransferableData(@NotNull Transferable content) {
        try {
            if (content.isDataFlavorSupported(ConvertedCode.DATA_FLAVOR)) {
                return (TextBlockTransferableData) content.getTransferData(ConvertedCode.DATA_FLAVOR);
            }
        } catch (Throwable e) {
            LOG.error(e);
        }
        return null;
    }

    @Override
    public void processTransferableData(Project project, @NotNull final Editor editor, @NotNull final RangeMarker bounds, int caretColumn, Ref<Boolean> indented, @NotNull TextBlockTransferableData value) {
        try {
            final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (!(file instanceof JetFile)) {
                return;
            }
            JetEditorOptions jetEditorOptions = JetEditorOptions.getInstance();
            boolean needConvert = jetEditorOptions.isEnableJavaToKotlinConversion() && (jetEditorOptions.isDonTShowConversionDialog() || okFromDialog(project));
            if (needConvert) {
                if (value instanceof ConvertedCode) {
                    final String text = ((ConvertedCode) value).getData();
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            editor.getDocument().replaceString(bounds.getStartOffset(), bounds.getEndOffset(), text);
                            editor.getCaretModel().moveToOffset(bounds.getStartOffset() + text.length());
                            PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
                        }
                    });
                }
            }
        } catch (Throwable t) {
            LOG.error(t);
        }
    }

    private static boolean okFromDialog(@NotNull Project project) {
        KotlinPasteFromJavaDialog dialog = new KotlinPasteFromJavaDialog(project);
        dialog.show();
        return dialog.isOK();
    }
}

