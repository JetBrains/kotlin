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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentKind;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.HashMap;

public class MutableModuleSourcesManager implements ModuleSourcesManager {

    private final Project project;
    protected final RootBasedFileMap<RootData> roots = new RootBasedFileMap<RootData>(new HashMap<VirtualFile, RootData>());
    private final Multimap<PackageFragmentDescriptor, JetFile> sourceFiles = LinkedHashMultimap.create();

    public MutableModuleSourcesManager(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Collection<JetFile> getPackageFragmentSources(@NotNull PackageFragmentDescriptor packageFragment) {
        return sourceFiles.get(packageFragment);
    }

    @NotNull
    @Override
    public MutableSubModuleDescriptor getSubModuleForFile(@NotNull PsiFile file) {
        return getRootForFile(file).subModule;
    }

    @NotNull
    private RootData getRootForFile(@NotNull PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        assert virtualFile != null : "No virtual file for " + file;
        RootData root = roots.getDataForFile(virtualFile);
        assert root != null : "File " + file + " not found ";
        return root;
    }

    @NotNull
    public Collection<JetFile> getAllSourceFiles() {
        return sourceFiles.values();
    }

    public void registerRoot(@NotNull MutableSubModuleDescriptor subModule, @NotNull PackageFragmentKind kind, @NotNull VirtualFile root) {
        RootData rootData = roots.getDataForRoot(root);
        if (rootData == null) {
            roots.putDataForRoot(root, new RootData(root, subModule, kind));
            PsiManager psiManager = PsiManager.getInstance(project);
            PsiDirectory directory = psiManager.findDirectory(root);
            if (directory != null) {
                indexDirectory(subModule, directory, kind);
            }
            else {
                PsiFile file = psiManager.findFile(root);
                assert file != null : "Can't find file or directory: " + root;

                indexFile(subModule, file, kind);
            }
        }
        else {
            assert rootData.subModule == subModule && rootData.rootKind == kind
                    : "Same root " + root + " added twice: \n" +
                      "    as " + rootData.rootKind + " in " + rootData.subModule + "\n" +
                      "    as " + kind + " in " + subModule;
        }
    }

    private void indexDirectory(
            @NotNull MutableSubModuleDescriptor subModule,
            @NotNull PsiDirectory directory,
            @NotNull PackageFragmentKind kind
    ) {
        for (PsiFile file : directory.getFiles()) {
            indexFile(subModule, file, kind);
        }

        for (PsiDirectory subdirectory : directory.getSubdirectories()) {
            indexDirectory(subModule, subdirectory, kind);
        }
    }

    private void indexFile(@NotNull MutableSubModuleDescriptor subModule, @NotNull PsiFile file, @NotNull PackageFragmentKind kind) {
        if (!(file instanceof JetFile)) return;

        JetFile jetFile = (JetFile) file;
        addJetFile(subModule, kind, jetFile);
    }

    public void addJetFile(@NotNull MutableSubModuleDescriptor subModule, @NotNull PackageFragmentKind kind, @NotNull JetFile jetFile) {
        FqName fqName = JetPsiUtil.getFQName(jetFile);

        MutablePackageFragmentDescriptor fragment = subModule.getPackageFragmentProviderForKotlinSources().addPackageFragment(kind, fqName);
        sourceFiles.put(fragment, jetFile);
    }

    private static final class RootData {
        private final VirtualFile root;
        private final MutableSubModuleDescriptor subModule;
        private final PackageFragmentKind rootKind;

        private RootData(
                @NotNull VirtualFile root,
                @NotNull MutableSubModuleDescriptor subModule,
                @NotNull PackageFragmentKind kind
        ) {
            this.root = root;
            this.subModule = subModule;
            rootKind = kind;
        }
    }
}
