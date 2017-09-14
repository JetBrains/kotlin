/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.modules;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightJavaModule;
import com.intellij.psi.search.FilenameIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static com.intellij.psi.PsiJavaModule.MODULE_INFO_FILE;

// Copied from com.intellij.codeInsight.daemon.impl.analysis.ModuleHighlightUtil
public class ModuleHighlightUtil2 {
    private static final Attributes.Name MULTI_RELEASE = new Attributes.Name("Multi-Release");

    @Nullable
    static PsiJavaModule getModuleDescriptor(@NotNull VirtualFile file, @NotNull Project project) {
        ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
        if (index.isInLibrary(file)) {
            VirtualFile root;
            if ((root = index.getClassRootForFile(file)) != null) {
                VirtualFile descriptorFile = root.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE);
                if (descriptorFile == null) {
                    VirtualFile alt = root.findFileByRelativePath("META-INF/versions/9/" + PsiJavaModule.MODULE_INFO_CLS_FILE);
                    if (alt != null && isMultiReleaseJar(root)) {
                        descriptorFile = alt;
                    }
                }
                if (descriptorFile != null) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(descriptorFile);
                    if (psiFile instanceof PsiJavaFile) {
                        return ((PsiJavaFile) psiFile).getModuleDeclaration();
                    }
                }
                else if (root.getFileSystem() instanceof JarFileSystem && "jar".equalsIgnoreCase(root.getExtension())) {
                    return LightJavaModule.getModule(PsiManager.getInstance(project), root);
                }
            }
            else if ((root = index.getSourceRootForFile(file)) != null) {
                VirtualFile descriptorFile = root.findChild(MODULE_INFO_FILE);
                if (descriptorFile != null) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(descriptorFile);
                    if (psiFile instanceof PsiJavaFile) {
                        return ((PsiJavaFile) psiFile).getModuleDeclaration();
                    }
                }
            }
        }
        else {
            Module module = index.getModuleForFile(file);
            if (module != null) {
                boolean isTest = index.isInTestSourceContent(file);
                List<VirtualFile> files = FilenameIndex.getVirtualFilesByName(project, MODULE_INFO_FILE, module.getModuleScope()).stream()
                        .filter(f -> index.isInTestSourceContent(f) == isTest)
                        .collect(Collectors.toList());
                if (files.size() == 1) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(files.get(0));
                    if (psiFile instanceof PsiJavaFile) {
                        return ((PsiJavaFile) psiFile).getModuleDeclaration();
                    }
                }
            }
        }

        return null;
    }

    private static boolean isMultiReleaseJar(VirtualFile root) {
        if (root.getFileSystem() instanceof JarFileSystem) {
            VirtualFile manifest = root.findFileByRelativePath(JarFile.MANIFEST_NAME);
            if (manifest != null) {
                try (InputStream stream = manifest.getInputStream()) {
                    return Boolean.valueOf(new Manifest(stream).getMainAttributes().getValue(MULTI_RELEASE));
                }
                catch (IOException ignored) {
                }
            }
        }

        return false;
    }
}
