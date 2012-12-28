/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.ClsFileDecompiledPsiFileProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JetClsFileDecompiledPsiFileProvider implements ClsFileDecompiledPsiFileProvider {
    @Nullable
    @Override
    public PsiFile getDecompiledPsiFile(@NotNull PsiJavaFile psiFile) {
        ClsFileImpl clsFile = (ClsFileImpl)psiFile;
        if (JetDecompiledData.isKotlinFile(clsFile) && !DumbService.isDumb(psiFile.getProject())) {
            return JetDecompiledData.getDecompiledData(clsFile).getJetFile();
        }
        return null;
    }
}
