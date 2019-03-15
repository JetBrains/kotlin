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

package org.jetbrains.kotlin.resolve.jvm;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFinderImpl;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.reference.SoftReference;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KtLightClassMarker;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.load.java.JavaClassFinder;
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class KotlinJavaPsiFacade {
    private volatile KotlinPsiElementFinderWrapper[] elementFinders;

    private static class PackageCache {
        final ConcurrentMap<Pair<String, GlobalSearchScope>, PsiPackage> packageInScopeCache = ContainerUtil.newConcurrentMap();
        final ConcurrentMap<String, Boolean> hasPackageInAllScopeCache = ContainerUtil.newConcurrentMap();
    }

    private volatile SoftReference<PackageCache> packageCache;

    private final Project project;
    private final LightModifierList emptyModifierList;

    public static KotlinJavaPsiFacade getInstance(Project project) {
        return ServiceManager.getService(project, KotlinJavaPsiFacade.class);
    }

    public KotlinJavaPsiFacade(@NotNull Project project) {
        this.project = project;

        emptyModifierList = new LightModifierList(PsiManager.getInstance(project), KotlinLanguage.INSTANCE);

        PsiModificationTracker modificationTracker = PsiManager.getInstance(project).getModificationTracker();
        MessageBus bus = project.getMessageBus();

        bus.connect().subscribe(PsiModificationTracker.TOPIC, new PsiModificationTracker.Listener() {
            private long lastTimeSeen = -1L;

            @Override
            public void modificationCountChanged() {
                long now = modificationTracker.getJavaStructureModificationCount();
                if (lastTimeSeen != now) {
                    lastTimeSeen = now;

                    packageCache = null;
                }
            }
        });
    }

    public void clearPackageCaches() {
        packageCache = null;
    }

    public LightModifierList getEmptyModifierList() {
        return emptyModifierList;
    }

    public JavaClass findClass(@NotNull JavaClassFinder.Request request, @NotNull GlobalSearchScope scope) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly

        ClassId classId = request.getClassId();
        String qualifiedName = classId.asSingleFqName().asString();

        if (shouldUseSlowResolve()) {
            PsiClass[] classes = findClassesInDumbMode(qualifiedName, scope);
            if (classes.length != 0) {
                return createJavaClass(classId, classes[0]);
            }
            return null;
        }

        for (KotlinPsiElementFinderWrapper finder : finders()) {
            if (finder instanceof CliFinder) {
                JavaClass aClass = ((CliFinder) finder).findClass(request, scope);
                if (aClass != null) return aClass;
            }
            else {
                PsiClass aClass = finder.findClass(qualifiedName, scope);
                if (aClass != null) {
                    if (scope instanceof JavaClassFinderImpl.FilterOutKotlinSourceFilesScope) {
                        GlobalSearchScope baseScope = ((JavaClassFinderImpl.FilterOutKotlinSourceFilesScope) scope).getBase();
                        boolean isSourcesScope = baseScope instanceof GlobalSearchScopeWithModuleSources;

                        if (!isSourcesScope) {
                            Object originalFinder = (finder instanceof KotlinPsiElementFinderWrapperImpl)
                                                    ? ((KotlinPsiElementFinderWrapperImpl) finder).getOriginal()
                                                    : finder;

                            // Temporary fix for #KT-12402
                            boolean isAndroidDataBindingClassWriter = originalFinder.getClass().getName()
                                    .equals("com.android.tools.idea.databinding.DataBindingClassFinder");
                            boolean isAndroidDataBindingComponentClassWriter = originalFinder.getClass().getName()
                                    .equals("com.android.tools.idea.databinding.DataBindingComponentClassFinder");

                            if (isAndroidDataBindingClassWriter || isAndroidDataBindingComponentClassWriter) {
                                continue;
                            }
                        }
                    }

                    return createJavaClass(classId, aClass);
                }
            }
        }

        return null;
    }

    @NotNull
    private static JavaClass createJavaClass(@NotNull ClassId classId, @NotNull PsiClass psiClass) {
        JavaClassImpl javaClass = new JavaClassImpl(psiClass);
        FqName fqName = classId.asSingleFqName();
        if (!fqName.equals(javaClass.getFqName())) {
            throw new IllegalStateException("Requested " + fqName + ", got " + javaClass.getFqName());
        }

        if (psiClass instanceof KtLightClassMarker) {
            throw new IllegalStateException("Kotlin light classes should not be found by JavaPsiFacade, resolving: " + fqName);
        }

        return javaClass;
    }

    @Nullable
    public Set<String> knownClassNamesInPackage(@NotNull FqName packageFqName) {
        KotlinPsiElementFinderWrapper[] finders = finders();

        if (finders.length == 1 && finders[0] instanceof CliFinder) {
            return ((CliFinder) finders[0]).knownClassNamesInPackage(packageFqName);
        }

        return null;
    }

    @NotNull
    private PsiClass[] findClassesInDumbMode(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        String packageName = StringUtil.getPackageName(qualifiedName);
        PsiPackage pkg = findPackage(packageName, scope);
        String className = StringUtil.getShortName(qualifiedName);
        if (pkg == null && packageName.length() < qualifiedName.length()) {
            PsiClass[] containingClasses = findClassesInDumbMode(packageName, scope);
            if (containingClasses.length == 1) {
                return PsiElementFinder.filterByName(className, containingClasses[0].getInnerClasses());
            }

            return PsiClass.EMPTY_ARRAY;
        }

        if (pkg == null || !pkg.containsClassNamed(className)) {
            return PsiClass.EMPTY_ARRAY;
        }

        return pkg.findClassByShortName(className, scope);
    }

    private boolean shouldUseSlowResolve() {
        DumbService dumbService = DumbService.getInstance(getProject());
        return dumbService.isDumb() && dumbService.isAlternativeResolveEnabled();
    }

    @NotNull
    private KotlinPsiElementFinderWrapper[] finders() {
        KotlinPsiElementFinderWrapper[] answer = elementFinders;
        if (answer == null) {
            answer = calcFinders();
            elementFinders = answer;
        }

        return answer;
    }

    @NotNull
    private KotlinPsiElementFinderWrapper[] calcFinders() {
        List<KotlinPsiElementFinderWrapper> elementFinders = new ArrayList<>();
        JavaFileManager javaFileManager = findJavaFileManager(project);
        elementFinders.add(
                javaFileManager instanceof KotlinCliJavaFileManager
                ? new CliFinder((KotlinCliJavaFileManager) javaFileManager)
                : new NonCliFinder(project, javaFileManager)
        );

        List<PsiElementFinder> nonKotlinFinders = ArraysKt.filter(
                getProject().getExtensions(PsiElementFinder.EP_NAME),
                finder -> (finder instanceof KotlinSafeClassFinder) ||
                          !(finder instanceof NonClasspathClassFinder ||
                            finder instanceof KotlinFinderMarker ||
                            finder instanceof PsiElementFinderImpl)
        );

        elementFinders.addAll(CollectionsKt.map(nonKotlinFinders, KotlinJavaPsiFacade::wrap));

        return elementFinders.toArray(new KotlinPsiElementFinderWrapper[elementFinders.size()]);
    }

    @NotNull
    private static JavaFileManager findJavaFileManager(@NotNull Project project) {
        JavaFileManager javaFileManager = ServiceManager.getService(project, JavaFileManager.class);
        if (javaFileManager == null) {
            throw new IllegalStateException("JavaFileManager component is not found in project");
        }
        return javaFileManager;
    }

    public PsiPackage findPackage(@NotNull String qualifiedName, GlobalSearchScope searchScope) {
        PackageCache cache = SoftReference.dereference(packageCache);
        if (cache == null) {
            packageCache = new SoftReference<>(cache = new PackageCache());
        }

        Pair<String, GlobalSearchScope> key = new Pair<>(qualifiedName, searchScope);
        PsiPackage aPackage = cache.packageInScopeCache.get(key);
        if (aPackage != null) {
            return aPackage;
        }

        KotlinPsiElementFinderWrapper[] finders = filteredFinders();

        Boolean packageFoundInAllScope = cache.hasPackageInAllScopeCache.get(qualifiedName);
        if (packageFoundInAllScope != null) {
            if (!packageFoundInAllScope.booleanValue()) return null;

            // Package was found in AllScope with some of finders but is absent in packageCache for current scope.
            // We check only finders that depend on scope.
            for (KotlinPsiElementFinderWrapper finder : finders) {
                if (!finder.isSameResultForAnyScope()) {
                    aPackage = finder.findPackage(qualifiedName, searchScope);
                    if (aPackage != null) {
                        return ConcurrencyUtil.cacheOrGet(cache.packageInScopeCache, key, aPackage);
                    }
                }
            }
        }
        else {
            for (KotlinPsiElementFinderWrapper finder : finders) {
                aPackage = finder.findPackage(qualifiedName, searchScope);

                if (aPackage != null) {
                    return ConcurrencyUtil.cacheOrGet(cache.packageInScopeCache, key, aPackage);
                }
            }

            boolean found = false;
            for (KotlinPsiElementFinderWrapper finder : finders) {
                if (!finder.isSameResultForAnyScope()) {
                    aPackage = finder.findPackage(qualifiedName, GlobalSearchScope.allScope(project));
                    if (aPackage != null) {
                        found = true;
                        break;
                    }
                }
            }

            cache.hasPackageInAllScopeCache.put(qualifiedName, found);
        }

        return null;
    }

    @NotNull
    private KotlinPsiElementFinderWrapper[] filteredFinders() {
        DumbService dumbService = DumbService.getInstance(getProject());
        KotlinPsiElementFinderWrapper[] finders = finders();
        if (dumbService.isDumb()) {
            List<KotlinPsiElementFinderWrapper> list = dumbService.filterByDumbAwareness(Arrays.asList(finders));
            finders = list.toArray(new KotlinPsiElementFinderWrapper[list.size()]);
        }
        return finders;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    public static KotlinPsiElementFinderWrapper wrap(PsiElementFinder finder) {
        return finder instanceof DumbAware
               ? new KotlinPsiElementFinderWrapperImplDumbAware(finder)
               : new KotlinPsiElementFinderWrapperImpl(finder);
    }

    interface KotlinPsiElementFinderWrapper {
        PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope);
        PsiPackage findPackage(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope);
        boolean isSameResultForAnyScope();
    }

    private static class KotlinPsiElementFinderWrapperImpl implements KotlinPsiElementFinderWrapper {
        private final PsiElementFinder finder;

        private KotlinPsiElementFinderWrapperImpl(@NotNull PsiElementFinder finder) {
            this.finder = finder;
        }

        public PsiElementFinder getOriginal() {
            return finder;
        }

        @Override
        public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return finder.findClass(qualifiedName, scope);
        }

        @Override
        public PsiPackage findPackage(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            // Original element finder can't search packages with scope
            return finder.findPackage(qualifiedName);
        }

        @Override
        public boolean isSameResultForAnyScope() {
            return true;
        }

        @Override
        public String toString() {
            return finder.toString();
        }
    }

    private static class KotlinPsiElementFinderWrapperImplDumbAware extends KotlinPsiElementFinderWrapperImpl implements DumbAware {
        private KotlinPsiElementFinderWrapperImplDumbAware(PsiElementFinder finder) {
            super(finder);
        }
    }

    private static class CliFinder implements KotlinPsiElementFinderWrapper, DumbAware {
        private final KotlinCliJavaFileManager javaFileManager;

        public CliFinder(@NotNull KotlinCliJavaFileManager javaFileManager) {
            this.javaFileManager = javaFileManager;
        }

        @Override
        public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return javaFileManager.findClass(qualifiedName, scope);
        }

        public JavaClass findClass(@NotNull JavaClassFinder.Request request, @NotNull GlobalSearchScope scope) {
            return javaFileManager.findClass(request, scope);
        }

        @Nullable
        public Set<String> knownClassNamesInPackage(@NotNull FqName packageFqName) {
            return javaFileManager.knownClassNamesInPackage(packageFqName);
        }

        @Override
        public PsiPackage findPackage(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return javaFileManager.findPackage(qualifiedName);
        }

        @Override
        public boolean isSameResultForAnyScope() {
            return false;
        }
    }

    private static class NonCliFinder implements KotlinPsiElementFinderWrapper, DumbAware {
        private final JavaFileManager javaFileManager;
        private final PsiManager psiManager;
        private final PackageIndex packageIndex;

        public NonCliFinder(@NotNull Project project, @NotNull JavaFileManager javaFileManager) {
            this.javaFileManager = javaFileManager;
            this.packageIndex = PackageIndex.getInstance(project);
            this.psiManager = PsiManager.getInstance(project);
        }

        @Override
        public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return javaFileManager.findClass(qualifiedName, scope);
        }

        @Override
        public PsiPackage findPackage(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            Query<VirtualFile> dirs = packageIndex.getDirsByPackageName(qualifiedName, true);
            return hasDirectoriesInScope(dirs, scope) ? new PsiPackageImpl(psiManager, qualifiedName) : null;
        }

        @Override
        public boolean isSameResultForAnyScope() {
            return false;
        }

        private static boolean hasDirectoriesInScope(Query<VirtualFile> dirs, GlobalSearchScope scope) {
            CommonProcessors.FindProcessor<VirtualFile> findProcessor = new CommonProcessors.FindProcessor<VirtualFile>() {
                @Override
                protected boolean accept(VirtualFile file) {
                    return scope.accept(file);
                }
            };

            dirs.forEach(findProcessor);
            return findProcessor.isFound();
        }
    }
}
