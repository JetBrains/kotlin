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

package org.jetbrains.kotlin.idea.internal;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtFile;

public class Location {
    @Nullable
    final Editor editor;

    @Nullable
    final KtFile ktFile;

    final long modificationStamp;

    final int startOffset;
    final int endOffset;

    private Location(@Nullable Editor editor, Project project) {
        this.editor = editor;

        if (editor != null) {
            modificationStamp = editor.getDocument().getModificationStamp();
            startOffset = editor.getSelectionModel().getSelectionStart();
            endOffset = editor.getSelectionModel().getSelectionEnd();

            VirtualFile vFile = ((EditorEx) editor).getVirtualFile();
            if (vFile == null) {
                ktFile = null;
            }
            else {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
                ktFile = psiFile instanceof KtFile ? (KtFile) psiFile : null;
            }
        }
        else {
            modificationStamp = 0;
            startOffset = 0;
            endOffset = 0;
            ktFile = null;
        }
    }

    public static Location fromEditor(Editor editor, Project project) {
        return new Location(editor, project);
    }

    @Nullable
    public KtFile getKFile() {
        return ktFile;
    }

    @Nullable
    public Editor getEditor() {
        return editor;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;

        Location location = (Location)o;

        if (modificationStamp != location.modificationStamp) return false;
        if (endOffset != location.endOffset) return false;
        if (startOffset != location.startOffset) return false;
        if (editor != null ? !editor.equals(location.editor) : location.editor != null) return false;
        if (ktFile != null ? !ktFile.equals(location.ktFile) : location.ktFile != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = editor != null ? editor.hashCode() : 0;
        result = 31 * result + (ktFile != null ? ktFile.hashCode() : 0);
        result = 31 * result + (int)(modificationStamp ^ (modificationStamp >>> 32));
        result = 31 * result + startOffset;
        result = 31 * result + endOffset;
        return result;
    }
}
