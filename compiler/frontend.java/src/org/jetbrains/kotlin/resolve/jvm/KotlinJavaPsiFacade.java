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
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.name.ClassId;

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

        final PsiModificationTracker modificationTracker = PsiManager.getInstance(project).getModificationTracker();
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

    public LightModifierList getEmptyModifierList() {
        return emptyModifierList;
    }

    public PsiClass findClass(@NotNull ClassId classId, @NotNull GlobalSearchScope scope) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly

        String qualifiedName = classId.asSingleFqName().asString();

        if (shouldUseSlowResolve()) {
            PsiClass[] classes = findClassesInDumbMode(qualifiedName, scope);
            if (classes.length != 0) {
                return classes[0];
            }
            return null;
        }

        for (KotlinPsiElementFinderWrapper finder : finders()) {
            if (finder instanceof KotlinPsiElementFinderImpl) {
                PsiClass aClass = ((KotlinPsiElementFinderImpl) finder).findClass(classId, scope);
                if (aClass != null) return aClass;
            }
            else {
                PsiClass aClass = finder.findClass(qualifiedName, scope);
                if (aClass != null) return aClass;
            }
        }

        return null;
    }

    @Nullable
    public Set<String> knownClassNamesInPackage(@NotNull FqName packageFqName) {
        KotlinPsiElementFinderWrapper[] finders = finders();

        if (finders.length == 1) {
            return ((KotlinPsiElementFinderImpl) finders[0]).knownClassNamesInPackage(packageFqName);
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
    protected KotlinPsiElementFinderWrapper[] calcFinders() {
        List<KotlinPsiElementFinderWrapper> elementFinders = new ArrayList<KotlinPsiElementFinderWrapper>();
        elementFinders.add(new KotlinPsiElementFinderImpl(getProject()));

        List<PsiElementFinder> nonKotlinFinders = ArraysKt.filter(
                getProject().getExtensions(PsiElementFinder.EP_NAME), new Function1<PsiElementFinder, Boolean>() {
                    @Override
                    public Boolean invoke(PsiElementFinder finder) {
                        return (finder instanceof KotlinSafeClassFinder) ||
                               !(finder instanceof NonClasspathClassFinder || finder instanceof KotlinFinderMarker || finder instanceof PsiElementFinderImpl);
                    }
                });

        elementFinders.addAll(CollectionsKt.map(nonKotlinFinders, new Function1<PsiElementFinder, KotlinPsiElementFinderWrapper>() {
            @Override
            public KotlinPsiElementFinderWrapper invoke(PsiElementFinder finder) {
                return wrap(finder);
            }
        }));

        return elementFinders.toArray(new KotlinPsiElementFinderWrapper[elementFinders.size()]);
    }

    public PsiPackage findPackage(@NotNull String qualifiedName, GlobalSearchScope searchScope) {
        PackageCache cache = SoftReference.dereference(packageCache);
        if (cache == null) {
            packageCache = new SoftReference<PackageCache>(cache = new PackageCache());
        }

        Pair<String, GlobalSearchScope> key = new Pair<String, GlobalSearchScope>(qualifiedName, searchScope);
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

    static class KotlinPsiElementFinderImpl implements KotlinPsiElementFinderWrapper, DumbAware {
        private final JavaFileManager javaFileManager;
        private final boolean isCliFileManager;

        private final PsiManager psiManager;
        private final PackageIndex packageIndex;

        public KotlinPsiElementFinderImpl(Project project) {
            this.javaFileManager = findJavaFileManager(project);
            this.isCliFileManager = javaFileManager instanceof KotlinCliJavaFileManager;

            this.packageIndex = PackageIndex.getInstance(project);
            this.psiManager = PsiManager.getInstance(project);
        }

        @NotNull
        private static JavaFileManager findJavaFileManager(@NotNull Project project) {
            JavaFileManager javaFileManager = ServiceManager.getService(project, JavaFileManager.class);
            if (javaFileManager == null) {
                throw new IllegalStateException("JavaFileManager component is not found in project");
            }

            return javaFileManager;
        }


        @Override
        public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return javaFileManager.findClass(qualifiedName, scope);
        }

        public PsiClass findClass(@NotNull ClassId classId, @NotNull GlobalSearchScope scope) {
            if (isCliFileManager) {
                return ((KotlinCliJavaFileManager) javaFileManager).findClass(classId, scope);
            }
            return findClass(classId.asSingleFqName().asString(), scope);
        }

        @Nullable
        public Set<String> knownClassNamesInPackage(@NotNull FqName packageFqName) {
            if (isCliFileManager) {
                return ((KotlinCliJavaFileManager) javaFileManager).knownClassNamesInPackage(packageFqName);
            }

            return null;
        }

        @Override
        public PsiPackage findPackage(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            if (isCliFileManager) {
                return javaFileManager.findPackage(qualifiedName);
            }

            Query<VirtualFile> dirs = packageIndex.getDirsByPackageName(qualifiedName, true);
            return hasDirectoriesInScope(dirs, scope) ? new PsiPackageImpl(psiManager, qualifiedName) : null;
        }

        @Override
        public boolean isSameResultForAnyScope() {
            return false;
        }

        private static boolean hasDirectoriesInScope(Query<VirtualFile> dirs, final GlobalSearchScope scope) {
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
