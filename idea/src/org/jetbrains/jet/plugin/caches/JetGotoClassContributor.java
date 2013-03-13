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
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
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
                return name.getFqName();
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
        return JetShortNamesCache.getKotlinInstance(project).getAllClassNames();
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        PsiClass[] classes = JetShortNamesCache.getKotlinInstance(project).getClassesByName(name, scope);

        Collection<String> javaQualifiedNames = new HashSet<String>();

        for (PsiClass aClass : classes) {
            String qualifiedName = aClass.getQualifiedName();
            if (qualifiedName != null) {
                javaQualifiedNames.add(qualifiedName);
            }
        }

        List<NavigationItem> items = new ArrayList<NavigationItem>();
        Collection<JetClassOrObject> classesOrObjects = JetShortClassNameIndex.getInstance().get(name, project, scope);

        for (JetClassOrObject classOrObject : classesOrObjects) {
            FqName fqName = JetPsiUtil.getFQName(classOrObject);
            if (fqName == null || javaQualifiedNames.contains(fqName.toString())) {
                continue;
            }

            if (classOrObject instanceof JetObjectDeclaration) {
                // items.add((JetObjectDeclaration) classOrObject);
            }
            else if (classOrObject instanceof JetClass) {
                items.add(classOrObject);
            }
            else {
                assert false;
            }
        }

        return ArrayUtil.toObjectArray(items, NavigationItem.class);
    }
}
