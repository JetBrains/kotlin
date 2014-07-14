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

package org.jetbrains.jet.lang.psi;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.ImportPath;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class JetImportsFactory {
    @NotNull
    private Project project;

    private final Map<ImportPath, JetImportDirective> importsCache = Maps.newHashMap();

    @Inject
    public void setProject(@NotNull Project project) {
        importsCache.clear();
        this.project = project;
    }

    @NotNull
    public JetImportDirective createImportDirective(@NotNull ImportPath importPath) {
        JetImportDirective directive = importsCache.get(importPath);
        if (directive != null) {
            return directive;
        }

        JetImportDirective createdDirective = JetPsiFactory(project).createImportDirective(importPath);
        importsCache.put(importPath, createdDirective);

        return createdDirective;
    }

    @NotNull
    public Collection<JetImportDirective> createImportDirectives(@NotNull Collection<ImportPath> importPaths) {
        return Collections2.transform(importPaths,
                                      new Function<ImportPath, JetImportDirective>() {
                                          @Override
                                          public JetImportDirective apply(@Nullable ImportPath path) {
                                              assert path != null;
                                              return createImportDirective(path);
                                          }
                                      });
    }
}
