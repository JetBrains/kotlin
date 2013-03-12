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

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;

import java.util.Map;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class JetDecompiledData {
    private final JetFile jetFile;
    private final Map<ClsElementImpl, JetDeclaration> clsElementsToJetElements;

    private static final Object LOCK = new String("decompiled data lock");
    private static final Key<JetDecompiledData> USER_DATA_KEY = new Key<JetDecompiledData>("USER_DATA_KEY");

    JetDecompiledData(JetFile jetFile, Map<ClsElementImpl, JetDeclaration> clsElementJetDeclarationMap) {
        this.jetFile = jetFile;
        clsElementsToJetElements = clsElementJetDeclarationMap;
    }

    @NotNull
    public JetFile getJetFile() {
        return jetFile;
    }

    public JetDeclaration getJetDeclarationByClsElement(ClsElementImpl clsElement) {
        return clsElementsToJetElements.get(clsElement);
    }

    @Nullable
    public static ClsFileImpl getClsFile(@NotNull Project project, @NotNull VirtualFile vFile) {
        if (!FileTypeManager.getInstance().isFileOfType(vFile, JavaClassFileType.INSTANCE)) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
        if (!(psiFile instanceof ClsFileImpl)) {
            return null;
        }
        ClsFileImpl clsFile = (ClsFileImpl) psiFile;
        if (clsFile.getClasses().length != 1) {
            return null;
        }
        return clsFile;
    }

    public static boolean isKotlinFile(@NotNull Project project, @NotNull VirtualFile vFile) {
        ClsFileImpl clsFile = getClsFile(project, vFile);
        return clsFile != null && isKotlinFile(clsFile);
    }

    public static boolean isKotlinFile(@NotNull ClsFileImpl clsFile) {
        return DescriptorResolverUtils.isKotlinClass(clsFile.getClasses()[0]);
    }

    @NotNull
    public static JetDecompiledData getDecompiledData(@NotNull ClsFileImpl clsFile) {
        synchronized (LOCK) {
            if (clsFile.getUserData(USER_DATA_KEY) == null) {
                clsFile.putUserData(USER_DATA_KEY, DecompiledDataFactory.createDecompiledData(clsFile));
            }
            JetDecompiledData decompiledData = clsFile.getUserData(USER_DATA_KEY);
            assert decompiledData != null;
            return decompiledData;
        }
    }

    @TestOnly
    Map<ClsElementImpl, JetDeclaration> getClsElementsToJetElements() {
        return clsElementsToJetElements;
    }
}
