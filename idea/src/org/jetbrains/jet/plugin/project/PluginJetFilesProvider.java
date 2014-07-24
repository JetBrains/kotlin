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

package org.jetbrains.jet.plugin.project;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.stubindex.JetAllPackagesIndex;
import org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope;

import java.util.Collection;

public class PluginJetFilesProvider  {

    @NotNull
    public static Collection<JetFile> allFilesInProject(@NotNull Project project) {
        return JetAllPackagesIndex.getInstance().get(FqName.ROOT.asString(), project,
                                                     JetSourceFilterScope.kotlinSources(GlobalSearchScope.allScope(project)));
    }

    private PluginJetFilesProvider() {
    }
}
