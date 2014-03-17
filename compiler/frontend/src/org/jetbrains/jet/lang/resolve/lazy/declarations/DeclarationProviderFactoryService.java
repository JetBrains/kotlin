/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy.declarations;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.storage.StorageManager;

import java.util.Collection;

public abstract class DeclarationProviderFactoryService {
    @NotNull
    public abstract DeclarationProviderFactory create(
            @NotNull Project project,
            @NotNull StorageManager storageManager,
            @NotNull Collection<JetFile> files
    );

    @NotNull
    public static DeclarationProviderFactory createDeclarationProviderFactory(
            @NotNull Project project,
            @NotNull StorageManager storageManager,
            @NotNull Collection<JetFile> files
    ) {
        return ServiceManager.getService(DeclarationProviderFactoryService.class).create(project, storageManager, files);
    }
}
