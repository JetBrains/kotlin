/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgeny Gerashchenko
 * @since 3/2/12
 */
@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class JetDecompiledData {
    private JetFile myJetFile;

    private Map<ClsElementImpl, JetDeclaration> myClsElementsToJetElements = new HashMap<ClsElementImpl, JetDeclaration>();

    private static final Object LOCK = new String("decompiled data lock");
    private static final Key<JetDecompiledData> USER_DATA_KEY = new Key<JetDecompiledData>("USER_DATA_KEY");

    JetDecompiledData(JetFile jetFile, Map<ClsElementImpl, JetDeclaration> clsElementJetDeclarationMap) {
        myJetFile = jetFile;
        myClsElementsToJetElements = clsElementJetDeclarationMap;
    }

    @NotNull
    public JetFile getJetFile() {
        return myJetFile;
    }

    public JetDeclaration getJetDeclarationByClsElement(ClsElementImpl clsElement) {
        return myClsElementsToJetElements.get(clsElement);
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
        if (clsFile == null) {
            return false;
        }
        PsiClass psiClass = clsFile.getClasses()[0];
        return DecompiledDataFactory.isKotlinNamespaceClass(psiClass) || DecompiledDataFactory.isKotlinClass(psiClass);
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
        return myClsElementsToJetElements;
    }
}
