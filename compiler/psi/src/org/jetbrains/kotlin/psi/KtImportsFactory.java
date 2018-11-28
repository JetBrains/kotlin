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

package org.jetbrains.kotlin.psi;

import com.intellij.openapi.project.Project;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.resolve.ImportPath;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated This class is not used in the kotlin plugin/compiler and will be removed soon
 */
@Deprecated
public class KtImportsFactory {
    @NotNull private final Project project;

    private final Map<ImportPath, KtImportDirective> importsCache = new HashMap<>();

    public KtImportsFactory(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    private KtImportDirective createImportDirective(@NotNull ImportPath importPath) {
        KtImportDirective directive = importsCache.get(importPath);
        if (directive != null) {
            return directive;
        }

        KtImportDirective createdDirective = KtPsiFactoryKt.KtPsiFactory(project, false).createImportDirective(importPath);
        importsCache.put(importPath, createdDirective);

        return createdDirective;
    }

    @NotNull
    public Collection<KtImportDirective> createImportDirectives(@NotNull Collection<ImportPath> importPaths) {
        return CollectionsKt.map(importPaths, this::createImportDirective);
    }

    @NotNull
    public Collection<KtImportDirective> createImportDirectivesNotCached(@NotNull Collection<ImportPath> importPaths) {
        return KtPsiFactoryKt.KtPsiFactory(project, false).createImportDirectives(importPaths);
    }
}
