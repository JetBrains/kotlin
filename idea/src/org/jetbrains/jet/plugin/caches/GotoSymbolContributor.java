/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.stubindex.JetIndexKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Nikolay Krasko
 */
public class GotoSymbolContributor implements ChooseByNameContributor {
    @NotNull
    @Override
    public String[] getNames(Project project, boolean includeNonProjectItems) {
        final Collection<String> items = StubIndex.getInstance().getAllKeys(JetIndexKeys.FUNCTIONS_SHORT_NAME_KEY, project);
        return ArrayUtil.toStringArray(items);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
        final GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);

        final Collection<? extends NavigationItem> functions = StubIndex.getInstance().get(
                JetIndexKeys.FUNCTIONS_SHORT_NAME_KEY, name, project, scope);

        final List<NavigationItem> items = new ArrayList<NavigationItem>(functions);
        return ArrayUtil.toObjectArray(items, NavigationItem.class);
    }
}
