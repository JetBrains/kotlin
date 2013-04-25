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

package org.jetbrains.jet.lang.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;

public class JetCodeFragmentImpl extends JetFile implements PsiCodeFragment {
    protected PsiElement context;
    private GlobalSearchScope resolveScope;

    public JetCodeFragmentImpl(Project project, String name, CharSequence text, IElementType elementType, PsiElement context) {
        super(((PsiManagerEx) PsiManager.getInstance(project)).getFileManager().createFileViewProvider(
                new LightVirtualFile(name, JetFileType.INSTANCE, text), true));
        ((SingleRootFileViewProvider)getViewProvider()).forceCachedPsi(this);
        init(TokenType.CODE_FRAGMENT, elementType);
        this.context = context;
    }

    @Override
    public void forceResolveScope(GlobalSearchScope scope) {
        resolveScope = scope;
    }

    @Override
    public GlobalSearchScope getForcedResolveScope() {
        return resolveScope;
    }

    @Override
    public boolean isPhysical() {
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public PsiElement getContext() {
        return context;
    }

    @Override
    @NotNull
    public GlobalSearchScope getResolveScope() {
        if (resolveScope != null) return resolveScope;
        return super.getResolveScope();
    }
}
