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

package org.jetbrains.jet.plugin.refactoring.rename;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.Map;

/**
 * User: Alefas
 * Date: 21.02.12
 */
public class RenameJetClassProcessor extends RenamePsiElementProcessor {
    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        return element instanceof JetClassOrObject;
    }

    @Override
    public void prepareRenaming(PsiElement element, String newName, Map<PsiElement, String> allRenames) {
        JetClassOrObject clazz = (JetClassOrObject) element;
        JetFile file = (JetFile) clazz.getContainingFile();

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            String nameWithoutExtensions = virtualFile.getNameWithoutExtension();
            if (nameWithoutExtensions.equals(clazz.getName())) {
                allRenames.put(file, newName + "." + virtualFile.getExtension());
            }
        }
        super.prepareRenaming(element, newName, allRenames);
    }
}
