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

package org.jetbrains.jet.plugin.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.Collection;

/**
 * Contains all packages, i.e. if a file declares
 *   package a.b.c
 *
 * Three packages "a", "a.b" and "a.b.c" will be registered in this index
 */
public class JetAllPackagesIndex extends StringStubIndexExtension<JetFile> {
    private static final StubIndexKey<String, JetFile> KEY = KotlinIndexUtil.createIndexKey(JetAllPackagesIndex.class);

    private static final JetAllPackagesIndex ourInstance = new JetAllPackagesIndex();

    @NotNull
    public static JetAllPackagesIndex getInstance() {
        return ourInstance;
    }

    private JetAllPackagesIndex() {}

    @NotNull
    @Override
    public StubIndexKey<String, JetFile> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public Collection<JetFile> get(String fqName, Project project, @NotNull GlobalSearchScope scope) {
        return super.get(fqName, project, JetSourceFilterScope.kotlinSourcesAndLibraries(scope, project));
    }
}
