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

package org.jetbrains.kotlin.idea.caches;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex;
import org.jetbrains.kotlin.idea.stubindex.KotlinPropertyShortNameIndex;
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KotlinGotoSymbolContributor implements ChooseByNameContributor {
    @NotNull
    @Override
    public String[] getNames(Project project, boolean includeNonProjectItems) {
        Collection<String> items = StubIndex.getInstance().getAllKeys(KotlinFunctionShortNameIndex.getInstance().getKey(), project);
        items.addAll(StubIndex.getInstance().getAllKeys(KotlinPropertyShortNameIndex.getInstance().getKey(), project));

        return ArrayUtil.toStringArray(items);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
        GlobalSearchScope baseScope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);
        GlobalSearchScope noLibrarySourceScope = KotlinSourceFilterScope.kotlinSourceAndClassFiles(baseScope, project);

        Collection<? extends NavigationItem> functions = KotlinFunctionShortNameIndex.getInstance().get(name, project, noLibrarySourceScope);
        Collection<? extends NavigationItem> properties = KotlinPropertyShortNameIndex.getInstance().get(name, project, noLibrarySourceScope);

        List<NavigationItem> items = new ArrayList<NavigationItem>(Collections2.filter(functions, Predicates.notNull()));
        items.addAll(properties);

        return ArrayUtil.toObjectArray(items, NavigationItem.class);
    }
}
