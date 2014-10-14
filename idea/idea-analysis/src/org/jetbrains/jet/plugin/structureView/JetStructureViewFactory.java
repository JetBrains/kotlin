/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.structureView;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;

public class JetStructureViewFactory implements PsiStructureViewFactory {
    @Override
    public StructureViewBuilder getStructureViewBuilder(PsiFile psiFile) {
        if (psiFile instanceof JetFile) {
            final JetFile file = (JetFile) psiFile;

            return new TreeBasedStructureViewBuilder() {
                @NotNull
                @Override
                public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                    return new JetStructureViewModel(file);
                }
            };
        }

        return null;
    }
}
