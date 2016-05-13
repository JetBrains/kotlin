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
import org.jetbrains.kotlin.psi.KtAnnotationEntry;

import java.util.Collection;

public class KotlinAnnotationsIndex extends StringStubIndexExtension<KtAnnotationEntry> {
    private static final StubIndexKey<String, KtAnnotationEntry> KEY = KotlinIndexUtil.createIndexKey(KotlinAnnotationsIndex.class);

    private static final KotlinAnnotationsIndex ourInstance = new KotlinAnnotationsIndex();

    public static KotlinAnnotationsIndex getInstance() {
        return ourInstance;
    }

    private KotlinAnnotationsIndex() {}

    @NotNull
    @Override
    public StubIndexKey<String, KtAnnotationEntry> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public Collection<KtAnnotationEntry> get(@NotNull String s, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(KEY, s, project, scope, KtAnnotationEntry.class);
    }

    @Override
    public int getVersion() {
        return super.getVersion() + 1;
    }
}
