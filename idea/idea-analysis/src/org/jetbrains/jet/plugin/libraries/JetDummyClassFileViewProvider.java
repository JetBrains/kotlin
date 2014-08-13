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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.SharedPsiElementImplUtil;
import com.intellij.psi.impl.source.tree.LeafElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class JetDummyClassFileViewProvider extends UserDataHolderBase implements FileViewProvider {
    private final String text;
    private final PsiManager manager;
    private JetFile jetFile = null;
    private final VirtualFile virtualFile;

    public JetDummyClassFileViewProvider(@NotNull PsiManager manager, @NotNull VirtualFile file, String text) {
        this.manager = manager;
        virtualFile = file;
        this.text = text;

        jetFile = new JetFile(this, true) {
            @Override
            public VirtualFile getVirtualFile() {
                return virtualFile; // overridden to return not-null; original method returns null when isEventSystemEnabled() = false
            }
        };
    }

    @Override
    @NotNull
    public PsiManager getManager() {
        return manager;
    }

    @Override
    @Nullable
    public Document getDocument() {
        return null;
    }

    @Override
    @NotNull
    public String getContents() {
        return text;
    }

    @Override
    @NotNull
    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    @Override
    @NotNull
    public Language getBaseLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    @NotNull
    public Set<Language> getLanguages() {
        return Collections.singleton(getBaseLanguage());
    }

    @Override
    public JetFile getPsi(@NotNull Language target) {
        if (JetLanguage.INSTANCE != target) {
            return null;
        }
        return jetFile;
    }

    @Override
    @NotNull
    public List<PsiFile> getAllFiles() {
        return Collections.<PsiFile>singletonList(jetFile);
    }

    @Override
    public void beforeContentsSynchronized() {
    }

    @Override
    public void contentsSynchronized() {
    }

    @Override
    public boolean isEventSystemEnabled() {
        return false;
    }

    @Override
    public boolean isPhysical() {
        return false;
    }

    @Override
    public long getModificationStamp() {
        return 0;
    }

    @Override
    public boolean supportsIncrementalReparse(@NotNull Language rootLanguage) {
        return true;
    }

    @Override
    public void rootChanged(@NotNull PsiFile psiFile) {
    }

    @Override
    public FileViewProvider clone(){
        throw new UnsupportedOperationException();
    }

    @Override
    public PsiReference findReferenceAt(int offset) {
        return SharedPsiElementImplUtil.findReferenceAt(getPsi(getBaseLanguage()), offset);
    }

    @Override
    @Nullable
    public PsiElement findElementAt(int offset, @NotNull Language language) {
        return language == getBaseLanguage() ? findElementAt(offset) : null;
    }


    @Override
    public PsiElement findElementAt(int offset, @NotNull Class<? extends Language> lang) {
        if (JetLanguage.class != lang) {
            return null;
        }
        return findElementAt(offset);
    }

    @Override
    public PsiElement findElementAt(int offset) {
        LeafElement element = jetFile.calcTreeElement().findLeafElementAt(offset);
        return element != null ? element.getPsi() : null;
    }

    @Override
    public PsiReference findReferenceAt(int offsetInElement, @NotNull Language language) {
        if (JetLanguage.INSTANCE != language) {
            return null;
        }
        return findReferenceAt(offsetInElement);
    }

    @NotNull
    @Override
    public FileViewProvider createCopy(@NotNull VirtualFile copy) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public PsiFile getStubBindingRoot() {
        PsiFile psi = getPsi(getBaseLanguage());
        assert psi != null;
        return psi;
    }

    @NotNull
    public static JetFile createJetFile(PsiManager psiManager, VirtualFile file, String text) {
        return new JetDummyClassFileViewProvider(psiManager, file, text).getPsi(JetLanguage.INSTANCE);
    }
}
