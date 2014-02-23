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

package org.jetbrains.jet.plugin.caches;

import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.stubindex.JetShortClassNameIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class JetGotoClassContributor implements GotoClassContributor {
    @Override
    public String getQualifiedName(NavigationItem item) {
        if (item instanceof JetNamedDeclaration) {
            JetNamedDeclaration jetClass = (JetNamedDeclaration) item;
            FqName name = JetPsiUtil.getFQName(jetClass);
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
        return ArrayUtil.toObjectArray(JetShortClassNameIndex.getInstance().getAllKeys(project), String.class);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
        GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);
        Collection<JetClassOrObject> classesOrObjects = JetShortClassNameIndex.getInstance().get(name, project, scope);

        if (classesOrObjects.isEmpty()) {
            return NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY;
        }

        PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(name, scope);
        Collection<String> javaQualifiedNames = new HashSet<String>();
        for (PsiClass aClass : classes) {
            String qualifiedName = aClass.getQualifiedName();
            if (qualifiedName != null) {
                javaQualifiedNames.add(qualifiedName);
            }
        }

        List<NavigationItem> items = new ArrayList<NavigationItem>();
        for (JetClassOrObject classOrObject : classesOrObjects) {
            FqName fqName = JetPsiUtil.getFQName(classOrObject);
            if (fqName == null || javaQualifiedNames.contains(fqName.toString())) {
                // Elements will be added by Java class contributor
                continue;
            }

            if (classOrObject instanceof JetClass) {
                items.add(classOrObject);
            }
        }

        return ArrayUtil.toObjectArray(items, NavigationItem.class);
    }
}
