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

/**
 * @author Nikolay Krasko
 */
public class JetGotoClassContributor implements GotoClassContributor {
    @Override
    public String getQualifiedName(NavigationItem item) {
        return "Hello";
    }

    @Override
    public String getQualifiedNameSeparator() {
        return ".";
    }

    @NotNull
    @Override
    public String[] getNames(Project project, boolean includeNonProjectItems) {
        return JetCacheManager.getInstance(project).getNamesCache().getAllClassNames();
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
        final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        PsiClass[] classes = JetCacheManager.getInstance(project).getNamesCache().getClassesByName(name, scope);

        Collection<String> javaQualifiedNames = new HashSet<String>();

        for (PsiClass aClass : classes) {
            String qualifiedName = aClass.getQualifiedName();
            if (qualifiedName != null) {
                javaQualifiedNames.add(qualifiedName);
            }
        }

        ArrayList<NavigationItem> items = new ArrayList<NavigationItem>();
        Collection<JetClassOrObject> classesOrObjects = JetShortClassNameIndex.getInstance().get(name, project, scope);

        for (JetClassOrObject classOrObject : classesOrObjects) {
            if (classOrObject instanceof JetNamedDeclaration) {
                FqName fqName = JetPsiUtil.getFQName((JetNamedDeclaration) classOrObject);
                if (fqName == null || javaQualifiedNames.contains(fqName.toString())) {
                    continue;
                }

                if (classOrObject instanceof JetObjectDeclaration) {
                    // items.add((JetObjectDeclaration) classOrObject);
                }
                else if (classOrObject instanceof JetClass) {
                    items.add((JetClass) classOrObject);
                }
                else {
                    assert false;
                }
            }
        }

        return ArrayUtil.toObjectArray(items, NavigationItem.class);
    }
}
