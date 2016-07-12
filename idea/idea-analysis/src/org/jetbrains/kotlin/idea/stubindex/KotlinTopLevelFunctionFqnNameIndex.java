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

package org.jetbrains.kotlin.idea.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.util.Collection;

/**
 * Stores package top level function (both extension and non-extension) full qualified names.
 */
public class KotlinTopLevelFunctionFqnNameIndex extends StringStubIndexExtension<KtNamedFunction> {
    private static final StubIndexKey<String, KtNamedFunction> KEY = KotlinIndexUtil.createIndexKey(KotlinTopLevelFunctionFqnNameIndex.class);

    private static final KotlinTopLevelFunctionFqnNameIndex INSTANCE = new KotlinTopLevelFunctionFqnNameIndex();

    @NotNull
    public static KotlinTopLevelFunctionFqnNameIndex getInstance() {
        return INSTANCE;
    }

    private KotlinTopLevelFunctionFqnNameIndex() {}

    @NotNull
    @Override
    public StubIndexKey<String, KtNamedFunction> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public Collection<KtNamedFunction> get(@NotNull String s, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(KEY, s, project, KotlinSourceFilterScope.sourcesAndLibraries(scope, project), KtNamedFunction.class);
    }

    // temporary hack, see comments in findCandidateDeclarationsInIndex (findDecompiledDeclaration.kt)
    @NotNull
    public Collection<KtNamedFunction> getNoScopeWrap(@NotNull String s, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(KEY, s, project, scope, KtNamedFunction.class);
    }
}
