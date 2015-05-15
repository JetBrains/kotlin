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

package org.jetbrains.kotlin.idea.refactoring.copy;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler;
import com.intellij.refactoring.copy.CopyHandlerDelegateBase;
import org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories.KotlinMoveFilesOrDirectoriesHandler;
import org.jetbrains.kotlin.psi.JetClassOrObject;

import java.util.ArrayList;

public class JetCopyClassHandler extends CopyHandlerDelegateBase {
    private CopyFilesOrDirectoriesHandler delegate = new CopyFilesOrDirectoriesHandler();

    private static PsiElement[] replaceElements(PsiElement[] elements) {
        ArrayList<PsiElement> result = new ArrayList<PsiElement>();
        for (PsiElement element : elements) {
            result.add(replaceElement(element));
        }
        return result.toArray(new PsiElement[result.size()]);
    }

    private static PsiElement replaceElement(PsiElement element) {
        if (element instanceof JetClassOrObject &&
            KotlinMoveFilesOrDirectoriesHandler.Companion.isMovableClass((JetClassOrObject) element)) {
            return element.getContainingFile();
        }
        else {
            return element;
        }
    }


    @Override
    public boolean canCopy(PsiElement[] elements, boolean fromUpdate) {
        return delegate.canCopy(replaceElements(elements), fromUpdate);
    }

    @Override
    public void doCopy(PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
        delegate.doCopy(replaceElements(elements), defaultTargetDirectory);
    }

    @Override
    public void doClone(PsiElement element) {
        delegate.doClone(replaceElement(element));
    }
}
