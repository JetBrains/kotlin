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

import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.stubindex.JetClassShortNameIndex;
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetEnumEntry;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JetGotoClassContributor implements GotoClassContributor {
    @Override
    public String getQualifiedName(NavigationItem item) {
        if (item instanceof JetNamedDeclaration) {
            JetNamedDeclaration jetClass = (JetNamedDeclaration) item;
            FqName name = jetClass.getFqName();
            if (name != null) {
                return name.asString();
            }
        }

        return null;
    }

    @Override
    public String getQualifiedNameSeparator() {
        return ".";
    }

    @NotNull
    @Override
    public String[] getNames(Project project, boolean includeNonProjectItems) {
        return ArrayUtil.toObjectArray(JetClassShortNameIndex.getInstance().getAllKeys(project), String.class);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
        GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);
        Collection<JetClassOrObject> classesOrObjects =
                JetClassShortNameIndex.getInstance().get(name, project, JetSourceFilterScope.kotlinSourceAndClassFiles(scope, project));

        if (classesOrObjects.isEmpty()) {
            return NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY;
        }

        List<NavigationItem> items = new ArrayList<NavigationItem>();
        for (JetClassOrObject classOrObject : classesOrObjects) {
            if (classOrObject != null && !(classOrObject instanceof JetEnumEntry)) {
                items.add(classOrObject);
            }
        }

        return ArrayUtil.toObjectArray(items, NavigationItem.class);
    }
}
