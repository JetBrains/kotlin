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

package org.jetbrains.kotlin.idea.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.psi.KtCodeFragment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;

public class TargetPlatformDetector {
    public static final TargetPlatformDetector INSTANCE = new TargetPlatformDetector();
    private static final Logger LOG = Logger.getInstance(TargetPlatformDetector.class);

    private TargetPlatformDetector() {
    }

    @NotNull
    public static TargetPlatform getPlatform(@NotNull KtFile file) {
        if (file instanceof KtCodeFragment) {
            KtFile contextFile = ((KtCodeFragment) file).getContextContainingFile();
            if (contextFile != null) {
                return getPlatform(contextFile);
            }
        }

        PsiElement context = KtPsiFactoryKt.getAnalysisContext(file);
        if (context != null) {
            PsiFile contextFile = context.getContainingFile();
            return contextFile instanceof KtFile ? getPlatform((KtFile) contextFile) : JvmPlatform.INSTANCE;
        }

        VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
        if (virtualFile == null) {
            return getDefaultPlatform();
        }
        Module moduleForFile = ProjectFileIndex.SERVICE.getInstance(file.getProject()).getModuleForFile(virtualFile);
        if (moduleForFile == null) {
            return getDefaultPlatform();
        }

        return getPlatform(moduleForFile);
    }

    @NotNull
    public static TargetPlatform getPlatform(@NotNull Module module) {
        if (ProjectStructureUtil.isJsKotlinModule(module)) {
            return JsPlatform.INSTANCE;
        }
        return JvmPlatform.INSTANCE;
    }

    @NotNull
    public static TargetPlatform getDefaultPlatform() {
        LOG.info("Using default platform");
        return JvmPlatform.INSTANCE;
    }
}
