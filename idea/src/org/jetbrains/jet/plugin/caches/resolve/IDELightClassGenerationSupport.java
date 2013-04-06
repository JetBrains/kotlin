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

package org.jetbrains.jet.plugin.caches.resolve;

import com.google.common.collect.Sets;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.LightClassConstructionContext;
import org.jetbrains.jet.asJava.LightClassGenerationSupport;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.stubindex.*;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.Collection;
import java.util.Set;

import static org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope.kotlinSources;

public class IDELightClassGenerationSupport extends LightClassGenerationSupport {

    public static IDELightClassGenerationSupport getInstanceForIDE(@NotNull Project project) {
        return (IDELightClassGenerationSupport) ServiceManager.getService(project, LightClassGenerationSupport.class);
    }

    private final Project project;

    public IDELightClassGenerationSupport(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public LightClassConstructionContext analyzeRelevantCode(@NotNull Collection<JetFile> files) {
        KotlinCacheManager cacheManager = KotlinCacheManager.getInstance(project);
        KotlinDeclarationsCache declarationsCache = cacheManager.getPossiblyIncompleteDeclarationsForLightClassGeneration();
        return new LightClassConstructionContext(declarationsCache.getBindingContext(), null);
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope) {
        return JetFullClassNameIndex.getInstance().get(fqName.getFqName(), project, kotlinSources(searchScope));
    }

    @NotNull
    @Override
    public Collection<JetFile> findFilesForPackage(@NotNull final FqName fqName, @NotNull GlobalSearchScope searchScope) {
        Collection<JetFile> files = JetAllPackagesIndex.getInstance().get(fqName.getFqName(), project, kotlinSources(searchScope));
        return ContainerUtil.filter(files, new Condition<JetFile>() {
            @Override
            public boolean value(JetFile file) {
                return fqName.equals(JetPsiUtil.getFQName(file));
            }
        });
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarationsInPackage(
            @NotNull FqName packageFqName, @NotNull GlobalSearchScope searchScope
    ) {
        return JetClassByPackageIndex.getInstance().get(packageFqName.getFqName(), project, kotlinSources(searchScope));
    }

    @Override
    public boolean packageExists(
            @NotNull FqName fqName, @NotNull GlobalSearchScope scope
    ) {
        return !JetAllPackagesIndex.getInstance().get(fqName.getFqName(), project, kotlinSources(scope)).isEmpty();
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackages(@NotNull FqName fqn, @NotNull GlobalSearchScope scope) {
        Collection<JetFile> files = JetAllPackagesIndex.getInstance().get(fqn.getFqName(), project, kotlinSources(scope));

        Set<FqName> result = Sets.newHashSet();
        for (JetFile file : files) {
            FqName fqName = JetPsiUtil.getFQName(file);

            assert QualifiedNamesUtil.isSubpackageOf(fqName, fqn) : "Registered package is not a subpackage of actually declared package:\n" +
                                                                    "in index: " + fqn + "\n" +
                                                                    "declared: " + fqName;
            FqName subpackage = QualifiedNamesUtil.plusOneSegment(fqn, fqName);
            if (subpackage != null) {
                result.add(subpackage);
            }
        }

        return result;
    }

    @NotNull
    public MultiMap<String, FqName> getAllPackageClasses(@NotNull GlobalSearchScope scope) {
        Collection<String> packageFqNames = JetAllPackagesIndex.getInstance().getAllKeys(project);

        MultiMap<String, FqName> result = new MultiMap<String, FqName>();
        for (String packageFqName : packageFqNames) {
            Collection<JetFile> files = findFilesForPackage(new FqName(packageFqName), scope);
            if (!files.isEmpty()) {
                FqName packageClassFqName = PackageClassUtils.getPackageClassFqName(new FqName(packageFqName));
                result.putValue(packageClassFqName.shortName().getName(), packageClassFqName);
            }

        }
        return result;
    }
}
