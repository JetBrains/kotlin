/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFinderImpl;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Query;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBusConnection;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KtLightClassMarker;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.load.java.JavaClassFinder;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.java.structure.JavaElementsKt;
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class KotlinJavaPsiFacade implements Disposable {
    private volatile KotlinPsiElementFinderWrapper[] elementFinders;

    private static class PackageCache {
        // long term cache
        final ConcurrentMap<GlobalSearchScope, ConcurrentMap<String, PsiPackage>> packageInLibScopeCache = new ConcurrentHashMap<>();

        // short term caches
        final ConcurrentMap<GlobalSearchScope, ConcurrentMap<String, PsiPackage>> packageInScopeCache = new ConcurrentHashMap<>();
        final ConcurrentMap<String, Boolean> hasPackageInAllScopeCache = new ConcurrentHashMap<>();

        void clear() {
            packageInScopeCache.clear();
            hasPackageInAllScopeCache.clear();
        }
    }

    private static final PsiPackage NULL_PACKAGE = new PsiPackageImpl(null, "NULL_PACKAGE");

    private static @Nullable PsiPackage unwrap(@NotNull PsiPackage psiPackage) {
        return psiPackage == NULL_PACKAGE ? null : psiPackage;
    }

    private volatile PackageCache packageCache;
    private volatile NotFoundPackagesCachingStrategy notFoundPackagesCachingStrategy = NotFoundPackagesCachingStrategy.Default.INSTANCE;

    private final Project project;
    private final LightModifierList emptyModifierList;

    public static KotlinJavaPsiFacade getInstance(Project project) {
        return project.getService(KotlinJavaPsiFacade.class);
    }

    public KotlinJavaPsiFacade(@NotNull Project project) {
        this.project = project;

        emptyModifierList = new LightModifierList(PsiManager.getInstance(project), KotlinLanguage.INSTANCE);

        // drop entire cache when it is low free memory
        LowMemoryWatcher.register(this::clearPackageCaches, this);

        MessageBusConnection connection = project.getMessageBus().connect(this);

        // VFS changes like create/delete/copy/move directory are subject to clean up short term caches
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                boolean relevant = false;
                for (VFileEvent event : events) {
                    VirtualFile file = event.getFile();
                    relevant = ((event instanceof VFileCreateEvent && ((VFileCreateEvent) event).isDirectory()) ||
                                (file != null && file.isDirectory() &&
                                 (event instanceof VFileDeleteEvent ||
                                  event instanceof VFileMoveEvent ||
                                  event instanceof VFileCopyEvent)));

                    if (relevant) break;
                }
                if (relevant) {
                    clearPackageCaches(false);
                }
            }
        });

        // PSI changes (like in R files) could lead to creating virtual packages
        // therefore it has to clean up short term caches
        PsiModificationTracker modificationTracker = PsiManager.getInstance(project).getModificationTracker();
        connection.subscribe(PsiModificationTracker.TOPIC, new PsiModificationTracker.Listener() {
            private long lastTimeSeen = -1L;

            @Override
            public void modificationCountChanged() {
                long now = modificationTracker.getModificationCount();
                if (lastTimeSeen != now) {
                    lastTimeSeen = now;

                    clearPackageCaches(false);
                }
            }
        });
    }

    @Override
    public void dispose() {
        clearPackageCaches();
    }

    public void clearPackageCaches() {
        clearPackageCaches(true);
    }

    private void clearPackageCaches(boolean force) {
        elementFinders = null;
        if (force) {
            packageCache = null;
        } else {
            obtainPackageCache().clear();
        }
    }

    public void setNotFoundPackagesCachingStrategy(NotFoundPackagesCachingStrategy notFoundPackagesCachingStrategy) {
        this.notFoundPackagesCachingStrategy = notFoundPackagesCachingStrategy;
    }

    public LightModifierList getEmptyModifierList() {
        return emptyModifierList;
    }

    @Nullable
    public JavaClass findClass(@NotNull JavaClassFinder.Request request, @NotNull GlobalSearchScope scope) {
        if (SearchScope.isEmptyScope(scope)) return null;

        // We hope this method is being called often enough to cancel daemon processes smoothly
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        ClassId classId = request.getClassId();
        String qualifiedName = classId.asSingleFqName().asString();

        if (shouldUseSlowResolve()) {
            PsiClass[] classes = findClassesInDumbMode(qualifiedName, scope);
            for (PsiClass psiClass : classes) {
                JavaClass javaClass = tryCreateJavaClass(classId, psiClass);
                if (javaClass != null) return javaClass;
            }

            return null;
        }

        for (KotlinPsiElementFinderWrapper finder : finders()) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
            if (finder instanceof CliFinder) {
                JavaClass aClass = ((CliFinder) finder).findClass(request, scope);
                if (aClass != null) return aClass;
            }
            else {
                PsiClass aClass = finder.findClass(qualifiedName, scope);
                if (aClass == null) continue;

                JavaClass javaClass = tryCreateJavaClass(classId, aClass);
                if (javaClass != null) return javaClass;
            }
        }

        return null;
    }

    @NotNull
    public List<JavaClass> findClasses(@NotNull JavaClassFinder.Request request, @NotNull GlobalSearchScope scope) {
        if (SearchScope.isEmptyScope(scope)) return Collections.emptyList();

        // We hope this method is being called often enough to cancel daemon processes smoothly
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        assert !shouldUseSlowResolve() : "`findClasses` should not be called from dumb mode, as results may be incomplete.";

        ClassId classId = request.getClassId();
        String qualifiedName = classId.asSingleFqName().asString();

        List<JavaClass> javaClasses = new SmartList<>();

        for (KotlinPsiElementFinderWrapper finder : finders()) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

            for (PsiClass psiClass : finder.findClasses(qualifiedName, scope)) {
                JavaClass javaClass = tryCreateJavaClass(classId, psiClass);
                if (javaClass != null) javaClasses.add(javaClass);
            }
        }

        return javaClasses;
    }

    @Nullable
    private JavaClass tryCreateJavaClass(@NotNull ClassId classId, @NotNull PsiClass psiClass) {
        JavaClassImpl javaClass = new JavaClassImpl(JavaElementSourceFactory.getInstance(project).createPsiSource(psiClass));
        FqName fqName = classId.asSingleFqName();
        if (!fqName.equals(javaClass.getFqName())) {
            throw new IllegalStateException("Requested " + fqName + ", got " + javaClass.getFqName());
        }

        if (psiClass instanceof KtLightClassMarker) {
            throw new IllegalStateException("Kotlin light classes should not be found by JavaPsiFacade, resolving: " + fqName);
        }

        if (!classId.equals(JavaElementsKt.getClassId(javaClass))) {
            return null;
        }

        return javaClass;
    }

    /**
     * @return null in case the set of names is impossible to compute correctly
     */
    @Nullable
    public Set<String> knownClassNamesInPackage(@NotNull FqName packageFqName, @NotNull GlobalSearchScope scope) {
        if (SearchScope.isEmptyScope(scope)) return Collections.emptySet();

        KotlinPsiElementFinderWrapper[] finders = finders();

        if (canComputeKnownClassNamesInPackage()) {
            return ((CliFinder) finders[0]).knownClassNamesInPackage(packageFqName);
        }

        return null;
    }

    public Boolean canComputeKnownClassNamesInPackage() {
        KotlinPsiElementFinderWrapper[] finders = finders();
        return finders.length == 1 && finders[0] instanceof CliFinder;
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

        List<PsiElementFinder> nonKotlinFinders = new ArrayList<>();
        for (PsiElementFinder finder : PsiElementFinder.EP.getExtensions(getProject())) {
            if ((finder instanceof KotlinSafeClassFinder) ||
                !(finder instanceof NonClasspathClassFinder ||
                  finder instanceof KotlinFinderMarker ||
                  finder instanceof PsiElementFinderImpl)) {
                nonKotlinFinders.add(finder);
            }
        }

        elementFinders.addAll(CollectionsKt.map(nonKotlinFinders, KotlinJavaPsiFacade::wrap));

        return elementFinders.toArray(new KotlinPsiElementFinderWrapper[0]);
    }

    @NotNull
    private static JavaFileManager findJavaFileManager(@NotNull Project project) {
        JavaFileManager javaFileManager = project.getService(JavaFileManager.class);
        if (javaFileManager == null) {
            throw new IllegalStateException("JavaFileManager component is not found in project");
        }
        return javaFileManager;
    }

    @Nullable
    public PsiPackage findPackage(@NotNull String qualifiedName, @NotNull GlobalSearchScope searchScope) {
        if (SearchScope.isEmptyScope(searchScope)) return null;

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
        if (certainlyDoesNotExist(qualifiedName, searchScope)) return null;

        PackageCache cache = obtainPackageCache();

        Boolean packageFoundInAllScope = cache.hasPackageInAllScopeCache.get(qualifiedName);
        if (packageFoundInAllScope != null && !packageFoundInAllScope.booleanValue()) return null;

        ConcurrentMap<String, PsiPackage> packageInLibScope = cache.packageInLibScopeCache.get(searchScope);
        PsiPackage pkg = packageInLibScope != null ? packageInLibScope.get(qualifiedName) : null;
        if (pkg != null) {
            return unwrap(pkg);
        }
        ConcurrentMap<String, PsiPackage> packageInScope = cache.packageInScopeCache.get(searchScope);
        pkg = packageInScope != null ? packageInScope.get(qualifiedName) : null;
        if (pkg != null) {
            return unwrap(pkg);
        }

        boolean isALibrarySearchScope = isALibrarySearchScope(searchScope);
        NotFoundPackagesCachingStrategy.CacheType notFoundCacheType =
                notFoundPackagesCachingStrategy.chooseStrategy(isALibrarySearchScope, qualifiedName);

        {
            // store found package in a long term cache if package is found in library search scope
            ConcurrentMap<GlobalSearchScope, ConcurrentMap<String, PsiPackage>> existedPackageInScopeCache =
                    isALibrarySearchScope ? cache.packageInLibScopeCache : cache.packageInScopeCache;

            KotlinPsiElementFinderWrapper[] finders = filteredFinders();
            if (packageFoundInAllScope != null) {
                // Package was found in AllScope with some of finders but is absent in packageCache for current scope.
                // We check only finders that depend on scope.
                for (KotlinPsiElementFinderWrapper finder : finders) {
                    if (!finder.isSameResultForAnyScope()) {
                        PsiPackage aPackage = finder.findPackage(qualifiedName, searchScope);
                        if (aPackage != null) {
                            ConcurrentMap<String, PsiPackage> concurrentMap =
                                    ConcurrencyUtil.cacheOrGet(existedPackageInScopeCache, searchScope, new ConcurrentHashMap<>());
                            return unwrap(ConcurrencyUtil.cacheOrGet(concurrentMap, qualifiedName, aPackage));
                        }
                    }
                }
            }
            else {
                for (KotlinPsiElementFinderWrapper finder : finders) {
                    PsiPackage aPackage = finder.findPackage(qualifiedName, searchScope);

                    if (aPackage != null) {
                        ConcurrentMap<String, PsiPackage> concurrentMap =
                                ConcurrencyUtil.cacheOrGet(existedPackageInScopeCache, searchScope, new ConcurrentHashMap<>());
                        return unwrap(ConcurrencyUtil.cacheOrGet(concurrentMap, qualifiedName, aPackage));
                    }
                }

                boolean found = false;
                for (KotlinPsiElementFinderWrapper finder : finders) {
                    if (!finder.isSameResultForAnyScope()) {
                        PsiPackage aPackage = finder.findPackage(qualifiedName, GlobalSearchScope.allScope(project));
                        if (aPackage != null) {
                            found = true;
                            break;
                        }
                    }
                }

                if (found || notFoundCacheType != NotFoundPackagesCachingStrategy.CacheType.NO_CACHING)
                    cache.hasPackageInAllScopeCache.put(qualifiedName, found);
            }
        }

        ConcurrentMap<GlobalSearchScope, ConcurrentMap<String, PsiPackage>> notFoundPackageInScopeCache;
        switch (notFoundCacheType) {
            case LIB_SCOPE:
                notFoundPackageInScopeCache = cache.packageInLibScopeCache;
                break;
            case SCOPE:
                notFoundPackageInScopeCache = cache.packageInScopeCache;
                break;
            case NO_CACHING:
                return null;
            default:
                throw new IllegalStateException("Impossible enum value: " + notFoundCacheType.toString());
        }

        ConcurrentMap<String, PsiPackage> concurrentMap =
                ConcurrencyUtil.cacheOrGet(notFoundPackageInScopeCache, searchScope, new ConcurrentHashMap<>());
        return unwrap(ConcurrencyUtil.cacheOrGet(concurrentMap, qualifiedName, NULL_PACKAGE));
    }

    private PackageCache obtainPackageCache() {
        PackageCache cache = packageCache;
        if (cache == null) {
            packageCache = cache = new PackageCache();
        }
        return cache;
    }

    private static boolean isALibrarySearchScope(GlobalSearchScope searchScope) {
        return searchScope.isSearchInLibraries();
    }

    private static boolean certainlyDoesNotExist(@NotNull String qualifiedName, GlobalSearchScope searchScope) {
        if (searchScope instanceof TopPackageNamesProvider) {
            TopPackageNamesProvider topPackageAwareSearchScope = (TopPackageNamesProvider) searchScope;
            Set<String> topPackageNames = topPackageAwareSearchScope.getTopPackageNames();
            if (topPackageNames != null) {
                String topPackageName = qualifiedName;
                int index = topPackageName.indexOf('.');
                if (index > 0) {
                    topPackageName = topPackageName.substring(0, index);
                }
                if (!topPackageNames.contains(topPackageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    private KotlinPsiElementFinderWrapper[] filteredFinders() {
        DumbService dumbService = DumbService.getInstance(getProject());
        KotlinPsiElementFinderWrapper[] finders = finders();
        if (dumbService.isDumb()) {
            List<KotlinPsiElementFinderWrapper> list = dumbService.filterByDumbAwareness(Arrays.asList(finders));
            finders = list.toArray(new KotlinPsiElementFinderWrapper[0]);
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
        PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope);
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
        public PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return finder.findClasses(qualifiedName, scope);
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

        @Override
        public PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return javaFileManager.findClasses(qualifiedName, scope);
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
        public PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return javaFileManager.findClasses(qualifiedName, scope);
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
