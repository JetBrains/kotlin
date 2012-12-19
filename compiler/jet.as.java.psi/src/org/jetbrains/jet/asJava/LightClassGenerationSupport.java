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

package org.jetbrains.jet.asJava;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public abstract class LightClassGenerationSupport {

    @NotNull
    public static LightClassGenerationSupport getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, LightClassGenerationSupport.class);
    }

    @NotNull
    public abstract LightClassConstructionContext analyzeRelevantCode(@NotNull JetFile file);

    @NotNull
    public abstract Collection<JetClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope);

    /*
     * Finds files whose package declaration is exactly {@code fqName}. For example, if a file declares
     *     package a.b.c
     * it will not be returned for fqName "a.b"
     *
     * If the resulting collection is empty, it means that this package has not other declarations than sub-packages
     */
    @NotNull
    public abstract Collection<JetFile> findFilesForPackage(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope);

    // Returns only immediately declared classes/objects, package classes are not included (they have no declarations)
    @NotNull
    public abstract Collection<JetClassOrObject> findClassOrObjectDeclarationsInPackage(
            @NotNull FqName packageFqName,
            @NotNull GlobalSearchScope searchScope
    );

    public abstract boolean packageExists(@NotNull FqName fqName, @NotNull GlobalSearchScope scope);

    @NotNull
    public abstract Collection<FqName> getSubPackages(@NotNull FqName fqn, @NotNull GlobalSearchScope scope);
}
