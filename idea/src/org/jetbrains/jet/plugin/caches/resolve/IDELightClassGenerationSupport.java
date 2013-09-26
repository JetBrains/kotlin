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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.KotlinLightClassForExplicitDeclaration;
import org.jetbrains.jet.asJava.LightClassConstructionContext;
import org.jetbrains.jet.asJava.LightClassGenerationSupport;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.libraries.JetSourceNavigationHelper;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;
import org.jetbrains.jet.plugin.stubindex.JetAllPackagesIndex;
import org.jetbrains.jet.plugin.stubindex.JetClassByPackageIndex;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;
import org.jetbrains.jet.util.QualifiedNamesUtil;
import org.jetbrains.jet.utils.Profiler;

import java.util.*;

import static org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope.kotlinSources;

public class IDELightClassGenerationSupport extends LightClassGenerationSupport {

    private static final Logger LOG = Logger.getInstance(IDELightClassGenerationSupport.class);

    private static boolean USE_LAZY = true;

    public static IDELightClassGenerationSupport getInstanceForIDE(@NotNull Project project) {
        return (IDELightClassGenerationSupport) ServiceManager.getService(project, LightClassGenerationSupport.class);
    }

    private final Project project;

    private final Comparator<JetFile> jetFileComparator;

    public IDELightClassGenerationSupport(@NotNull Project project) {
        this.project = project;
        final GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
        this.jetFileComparator = new Comparator<JetFile>() {
            @Override
            public int compare(@NotNull JetFile o1, @NotNull JetFile o2) {
                VirtualFile f1 = o1.getVirtualFile();
                VirtualFile f2 = o2.getVirtualFile();
                if (f1 == f2) return 0;
                if (f1 == null) return -1;
                if (f2 == null) return 1;
                return searchScope.compare(f1, f2);
            }
        };
    }

    @NotNull
    @Override
    public LightClassConstructionContext analyzeRelevantCode(@NotNull Collection<JetFile> files) {
        if (files.isEmpty()) {
            return new LightClassConstructionContext(BindingContext.EMPTY, null);
        }

        List<JetFile> sortedFiles = new ArrayList<JetFile>(files);
        Collections.sort(sortedFiles, jetFileComparator);

        Profiler p = Profiler.create((USE_LAZY ? "lazy" : "eager") + "analyze", LOG).start();
        try {
            if (USE_LAZY) {
                ResolveSessionForBodies session = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(sortedFiles.get(0));
                session.forceResolveAll();
                return new LightClassConstructionContext(session.getBindingContext(), null);
            }
            else {
                KotlinCacheManager cacheManager = KotlinCacheManager.getInstance(project);
                KotlinDeclarationsCache declarationsCache = cacheManager.getPossiblyIncompleteDeclarationsForLightClassGeneration();
                return new LightClassConstructionContext(declarationsCache.getBindingContext(), null);
            }
        }
        finally {
            p.end();
        }
    }

    @NotNull
    @Override
    public LightClassConstructionContext analyzeRelevantCode(@NotNull JetClassOrObject classOrObject) {
        KotlinCacheManager cacheManager = KotlinCacheManager.getInstance(project);
        return new LightClassConstructionContext(cacheManager.getLightClassContextCache().getLightClassContext(classOrObject), null);
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope) {
        return JetFullClassNameIndex.getInstance().get(fqName.asString(), project, kotlinSources(searchScope));
    }

    @NotNull
    @Override
    public Collection<JetFile> findFilesForPackage(@NotNull final FqName fqName, @NotNull GlobalSearchScope searchScope) {
        Collection<JetFile> files = JetAllPackagesIndex.getInstance().get(fqName.asString(), project, kotlinSources(searchScope));
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
        return JetClassByPackageIndex.getInstance().get(packageFqName.asString(), project, kotlinSources(searchScope));
    }

    @Override
    public boolean packageExists(
            @NotNull FqName fqName, @NotNull GlobalSearchScope scope
    ) {
        return !JetAllPackagesIndex.getInstance().get(fqName.asString(), project, kotlinSources(scope)).isEmpty();
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackages(@NotNull FqName fqn, @NotNull GlobalSearchScope scope) {
        Collection<JetFile> files = JetAllPackagesIndex.getInstance().get(fqn.asString(), project, kotlinSources(scope));

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

    @Nullable
    @Override
    public PsiClass getPsiClass(@NotNull JetClassOrObject classOrObject) {
        VirtualFile virtualFile = classOrObject.getContainingFile().getVirtualFile();
        if (virtualFile != null && LibraryUtil.findLibraryEntry(virtualFile, classOrObject.getProject()) != null) {
            return JetSourceNavigationHelper.getOriginalClass(classOrObject);
        }

        return  KotlinLightClassForExplicitDeclaration.create(classOrObject.getManager(), classOrObject);
    }

    @NotNull
    public MultiMap<String, FqName> getAllPossiblePackageClasses(@NotNull GlobalSearchScope scope) {
        Collection<String> packageFqNames = JetAllPackagesIndex.getInstance().getAllKeys(project);

        MultiMap<String, FqName> result = new MultiMap<String, FqName>();
        for (String packageFqName : packageFqNames) {
            FqName packageClassFqName = PackageClassUtils.getPackageClassFqName(new FqName(packageFqName));
            result.putValue(packageClassFqName.shortName().asString(), packageClassFqName);
        }

        return result;
    }
}
