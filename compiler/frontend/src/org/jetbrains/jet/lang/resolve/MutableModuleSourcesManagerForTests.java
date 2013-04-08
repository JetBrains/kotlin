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

import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.PackageFragmentKind;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.Map;

public class MutableModuleSourcesManagerForTests extends MutableModuleSourcesManager {
    // for tests only
    private final Map<JetFile, MutableSubModuleDescriptor> extraFiles = Maps.newHashMap();

    public MutableModuleSourcesManagerForTests(@NotNull Project project) {
        super(project);
    }

    @NotNull
    @Override
    public MutableSubModuleDescriptor getSubModuleForFile(@NotNull PsiFile file) {
        assert ApplicationManager.getApplication().isUnitTestMode() : "Shouldn't be run in production";

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null || roots.getDataForFile(virtualFile) == null) {
            MutableSubModuleDescriptor subModule = extraFiles.get(file);
            assert subModule != null : "No sub-module for file " + file;
            return subModule;
        }

        return super.getSubModuleForFile(file);
    }

    @TestOnly
    public void addExtraFile(@NotNull MutableSubModuleDescriptor subModule, @NotNull JetFile jetFile) {
        extraFiles.put(jetFile, subModule);
        addJetFile(subModule, PackageFragmentKind.SOURCE, jetFile);
    }
}
