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

package org.jetbrains.kotlin.cli.jvm.compiler;

import com.intellij.codeInsight.BaseExternalAnnotationsManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CoreExternalAnnotationsManager extends BaseExternalAnnotationsManager {
    static {
        // This is an ugly workaround for JDOM 1.1 used from application started from Ant 1.8 without forking
        System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    }

    private final List<VirtualFile> externalAnnotationsRoots = new ArrayList<VirtualFile>();

    public CoreExternalAnnotationsManager(@NotNull PsiManager psiManager) {
        super(psiManager);
    }

    public void addExternalAnnotationsRoot(VirtualFile externalAnnotationsRoot) {
        externalAnnotationsRoots.add(externalAnnotationsRoot);
    }

    @Override
    protected boolean hasAnyAnnotationsRoots() {
        return true;
    }

    @NotNull
    @Override
    protected List<VirtualFile> getExternalAnnotationsRoots(@NotNull VirtualFile libraryFile) {
        return externalAnnotationsRoots;
    }

    @Override
    public void annotateExternally(@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQName, @NotNull PsiFile fromFile,
            PsiNameValuePair[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deannotate(@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQN) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean editExternalAnnotation(@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQN,
            @Nullable PsiNameValuePair[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AnnotationPlace chooseAnnotationsPlace(@NotNull PsiElement element) {
        throw new UnsupportedOperationException();
    }
}
