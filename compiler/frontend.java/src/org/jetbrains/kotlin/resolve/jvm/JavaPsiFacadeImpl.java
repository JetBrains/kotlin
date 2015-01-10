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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaPsiFacadeEx;
import com.intellij.psi.impl.PsiConstantEvaluationHelperImpl;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.impl.source.DummyHolderFactory;
import com.intellij.psi.impl.source.JavaDummyHolder;
import com.intellij.psi.impl.source.JavaDummyHolderFactory;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubTreeLoader;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.reference.SoftReference;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import com.intellij.util.messages.MessageBus;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Copy idea version of JavaPsiFacadeImpl
 * TODO Temporary class until {@link com.intellij.psi.impl.JavaPsiFacadeImpl} hacked.
 * @see com.intellij.psi.impl.JavaPsiFacadeImpl
 */
@SuppressWarnings({"UnnecessaryFinalOnLocalVariableOrParameter", "UnusedDeclaration", "NullableProblems"})
public class JavaPsiFacadeImpl extends JavaPsiFacadeEx {
    private volatile PsiElementFinder[] myElementFinders;
    private final PsiConstantEvaluationHelper myConstantEvaluationHelper;
    private volatile SoftReference<ConcurrentMap<String, PsiPackage>> myPackageCache;
    private final Project myProject;
    private final JavaFileManager myFileManager;

    public JavaPsiFacadeImpl(Project project,
            PsiManager psiManager,
            JavaFileManager javaFileManager,
            MessageBus bus) {
        myProject = project;
        myFileManager = javaFileManager;
        myConstantEvaluationHelper = new PsiConstantEvaluationHelperImpl();

        final PsiModificationTracker modificationTracker = psiManager.getModificationTracker();

        if (bus != null) {
            bus.connect().subscribe(PsiModificationTracker.TOPIC, new PsiModificationTracker.Listener() {
                private long lastTimeSeen = -1L;

                @Override
                public void modificationCountChanged() {
                    final long now = modificationTracker.getJavaStructureModificationCount();
                    if (lastTimeSeen != now) {
                        lastTimeSeen = now;
                        myPackageCache = null;
                    }
                }
            });
        }

        DummyHolderFactory.setFactory(new JavaDummyHolderFactory());
    }

    @Override
    public PsiClass findClass(@NotNull final String qualifiedName, @NotNull GlobalSearchScope scope) {
        ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly

        if (DumbService.getInstance(getProject()).isDumb()) {
            PsiClass[] classes = findClassesInDumbMode(qualifiedName, scope);
            if (classes.length != 0) {
                return classes[0];
            }
            return null;
        }

        for (PsiElementFinder finder : finders()) {
            PsiClass aClass = finder.findClass(qualifiedName, scope);
            if (aClass != null) return aClass;
        }

        return null;
    }

    @NotNull
    private PsiClass[] findClassesInDumbMode(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        final String packageName = StringUtil.getPackageName(qualifiedName);
        final PsiPackage pkg = findPackage(packageName);
        final String className = StringUtil.getShortName(qualifiedName);
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

    @Override
    @NotNull
    public PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        if (DumbService.getInstance(getProject()).isDumb()) {
            return findClassesInDumbMode(qualifiedName, scope);
        }

        List<PsiClass> classes = new SmartList<PsiClass>();
        for (PsiElementFinder finder : finders()) {
            PsiClass[] finderClasses = finder.findClasses(qualifiedName, scope);
            ContainerUtil.addAll(classes, finderClasses);
        }

        return classes.toArray(new PsiClass[classes.size()]);
    }

    @NotNull
    private PsiElementFinder[] finders() {
        PsiElementFinder[] answer = myElementFinders;
        if (answer == null) {
            answer = calcFinders();
            myElementFinders = answer;
        }

        return answer;
    }

    @NotNull
    protected PsiElementFinder[] calcFinders() {
        List<PsiElementFinder> elementFinders = new ArrayList<PsiElementFinder>();
        elementFinders.add(new PsiElementFinderImpl());
        ContainerUtil.addAll(elementFinders, myProject.getExtensions(PsiElementFinder.EP_NAME));
        return elementFinders.toArray(new PsiElementFinder[elementFinders.size()]);
    }

    @Override
    @NotNull
    public PsiConstantEvaluationHelper getConstantEvaluationHelper() {
        return myConstantEvaluationHelper;
    }

    @Override
    public PsiPackage findPackage(@NotNull String qualifiedName) {
        ConcurrentMap<String, PsiPackage> cache = SoftReference.dereference(myPackageCache);
        if (cache == null) {
            myPackageCache = new SoftReference<ConcurrentMap<String, PsiPackage>>(cache = new ConcurrentHashMap<String, PsiPackage>());
        }

        PsiPackage aPackage = cache.get(qualifiedName);
        if (aPackage != null) {
            return aPackage;
        }

        for (PsiElementFinder finder : filteredFinders()) {
            aPackage = finder.findPackage(qualifiedName);
            if (aPackage != null) {
                return ConcurrencyUtil.cacheOrGet(cache, qualifiedName, aPackage);
            }
        }

        return null;
    }

    @NotNull
    private PsiElementFinder[] filteredFinders() {
        DumbService dumbService = DumbService.getInstance(getProject());
        PsiElementFinder[] finders = finders();
        if (dumbService.isDumb()) {
            List<PsiElementFinder> list = dumbService.filterByDumbAwareness(Arrays.asList(finders));
            finders = list.toArray(new PsiElementFinder[list.size()]);
        }
        return finders;
    }

    @Override
    @NotNull
    public PsiJavaParserFacade getParserFacade() {
        return getElementFactory(); // TODO: lighter implementation which doesn't mark all the elements as generated.
    }

    @Override
    @NotNull
    public PsiResolveHelper getResolveHelper() {
        return PsiResolveHelper.SERVICE.getInstance(myProject);
    }

    @Override
    @NotNull
    public PsiNameHelper getNameHelper() {
        return PsiNameHelper.getInstance(myProject);
    }

    @NotNull
    public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        Set<String> result = new THashSet<String>();
        for (PsiElementFinder finder : filteredFinders()) {
            result.addAll(finder.getClassNames(psiPackage, scope));
        }
        return result;
    }

    @NotNull
    public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        List<PsiClass> result = null;
        for (PsiElementFinder finder : filteredFinders()) {
            PsiClass[] classes = finder.getClasses(psiPackage, scope);
            if (classes.length == 0) continue;
            if (result == null) result = new ArrayList<PsiClass>();
            ContainerUtil.addAll(result, classes);
        }

        return result == null ? PsiClass.EMPTY_ARRAY : result.toArray(new PsiClass[result.size()]);
    }

    public boolean processPackageDirectories(@NotNull PsiPackage psiPackage,
            @NotNull GlobalSearchScope scope,
            @NotNull Processor<PsiDirectory> consumer,
            boolean includeLibrarySources) {
        for (PsiElementFinder finder : filteredFinders()) {
            if (!finder.processPackageDirectories(psiPackage, scope, consumer, includeLibrarySources)) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public PsiPackage[] getSubPackages(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        LinkedHashSet<PsiPackage> result = new LinkedHashSet<PsiPackage>();
        for (PsiElementFinder finder : filteredFinders()) {
            PsiPackage[] packages = finder.getSubPackages(psiPackage, scope);
            ContainerUtil.addAll(result, packages);
        }

        return result.toArray(new PsiPackage[result.size()]);
    }

    public PsiClass[] findClassByShortName(String name, PsiPackage psiPackage, GlobalSearchScope scope) {
        List<PsiClass> result = null;
        for (PsiElementFinder finder : filteredFinders()) {
            PsiClass[] classes = finder.getClasses(name, psiPackage, scope);
            if (classes.length == 0) continue;
            if (result == null) result = new ArrayList<PsiClass>();
            ContainerUtil.addAll(result, classes);
        }

        return result == null ? PsiClass.EMPTY_ARRAY : result.toArray(new PsiClass[result.size()]);
    }

    private class PsiElementFinderImpl extends PsiElementFinder implements DumbAware {
        @Override
        public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return myFileManager.findClass(qualifiedName, scope);
        }

        @Override
        @NotNull
        public PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
            return myFileManager.findClasses(qualifiedName, scope);
        }

        @Override
        public PsiPackage findPackage(@NotNull String qualifiedName) {
            return myFileManager.findPackage(qualifiedName);
        }

        @Override
        @NotNull
        public PsiPackage[] getSubPackages(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
            final Map<String, PsiPackage> packagesMap = new HashMap<String, PsiPackage>();
            final String qualifiedName = psiPackage.getQualifiedName();
            for (PsiDirectory dir : psiPackage.getDirectories(scope)) {
                PsiDirectory[] subDirs = dir.getSubdirectories();
                for (PsiDirectory subDir : subDirs) {
                    final PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(subDir);
                    if (aPackage != null) {
                        final String subQualifiedName = aPackage.getQualifiedName();
                        if (subQualifiedName.startsWith(qualifiedName) && !packagesMap.containsKey(subQualifiedName)) {
                            packagesMap.put(aPackage.getQualifiedName(), aPackage);
                        }
                    }
                }
            }

            packagesMap.remove(qualifiedName);    // avoid SOE caused by returning a package as a subpackage of itself
            return packagesMap.values().toArray(new PsiPackage[packagesMap.size()]);
        }

        @Override
        @NotNull
        public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull final GlobalSearchScope scope) {
            return getClasses(null, psiPackage, scope);
        }

        @Override
        @NotNull
        public PsiClass[] getClasses(@Nullable String shortName, @NotNull PsiPackage psiPackage, @NotNull final GlobalSearchScope scope) {
            List<PsiClass> list = null;
            String packageName = psiPackage.getQualifiedName();
            for (PsiDirectory dir : psiPackage.getDirectories(scope)) {
                PsiClass[] classes = JavaDirectoryService.getInstance().getClasses(dir);
                if (classes.length == 0) continue;
                if (list == null) list = new ArrayList<PsiClass>();
                for (PsiClass aClass : classes) {
                    // class file can be located in wrong place inside file system
                    String qualifiedName = aClass.getQualifiedName();
                    if (qualifiedName != null) qualifiedName = StringUtil.getPackageName(qualifiedName);
                    if (Comparing.strEqual(qualifiedName, packageName)) {
                        if (shortName == null || shortName.equals(aClass.getName())) list.add(aClass);
                    }
                }
            }
            if (list == null) {
                return PsiClass.EMPTY_ARRAY;
            }

            if (list.size() > 1) {
                ContainerUtil.quickSort(list, new Comparator<PsiClass>() {
                    @Override
                    public int compare(PsiClass o1, PsiClass o2) {
                        VirtualFile file1 = PsiUtilCore.getVirtualFile(o1);
                        VirtualFile file2 = PsiUtilCore.getVirtualFile(o2);
                        return file1 == null ? file2 == null ? 0 : -1 : file2 == null ? 1 : scope.compare(file2, file1);
                    }
                });
            }

            return list.toArray(new PsiClass[list.size()]);
        }

        @NotNull
        @Override
        public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
            Set<String> names = null;
            FileIndexFacade facade = FileIndexFacade.getInstance(myProject);
            for (PsiDirectory dir : psiPackage.getDirectories(scope)) {
                for (PsiFile file : dir.getFiles()) {
                    if (file instanceof PsiClassOwner && file.getViewProvider().getLanguages().size() == 1) {
                        VirtualFile vFile = file.getVirtualFile();
                        if (vFile != null &&
                            !(file instanceof PsiCompiledElement) &&
                            !facade.isInSourceContent(vFile) &&
                            (!scope.isForceSearchingInLibrarySources() ||
                             !StubTreeLoader.getInstance().canHaveStub(vFile))) {
                            continue;
                        }

                        Set<String> inFile = file instanceof PsiClassOwnerEx ? ((PsiClassOwnerEx)file).getClassNames() : getClassNames(((PsiClassOwner)file).getClasses());

                        if (inFile.isEmpty()) continue;
                        if (names == null) names = new HashSet<String>();
                        names.addAll(inFile);
                    }
                }

            }
            return names == null ? Collections.<String>emptySet() : names;
        }

        @Override
        public boolean processPackageDirectories(@NotNull PsiPackage psiPackage,
                @NotNull final GlobalSearchScope scope,
                @NotNull final Processor<PsiDirectory> consumer,
                boolean includeLibrarySources) {
            final PsiManager psiManager = PsiManager.getInstance(getProject());
            return PackageIndex.getInstance(getProject()).getDirsByPackageName(psiPackage.getQualifiedName(), includeLibrarySources)
                    .forEach(new ReadActionProcessor<VirtualFile>() {
                        @Override
                        public boolean processInReadAction(final VirtualFile dir) {
                            if (!scope.contains(dir)) return true;
                            PsiDirectory psiDir = psiManager.findDirectory(dir);
                            return psiDir == null || consumer.process(psiDir);
                        }
                    });
        }
    }

    @Override
    public boolean isPartOfPackagePrefix(@NotNull String packageName) {
        final Collection<String> packagePrefixes = myFileManager.getNonTrivialPackagePrefixes();
        for (final String subpackageName : packagePrefixes) {
            if (PsiNameHelper.isSubpackageOf(subpackageName, packageName)) return true;
        }
        return false;
    }

    @Override
    public boolean isInPackage(@NotNull PsiElement element, @NotNull PsiPackage aPackage) {
        final PsiFile file = FileContextUtil.getContextFile(element);
        if (file instanceof JavaDummyHolder) {
            return ((JavaDummyHolder) file).isInPackage(aPackage);
        }
        if (file instanceof PsiJavaFile) {
            final String packageName = ((PsiJavaFile) file).getPackageName();
            return packageName.equals(aPackage.getQualifiedName());
        }
        return false;
    }

    @Override
    public boolean arePackagesTheSame(@NotNull PsiElement element1, @NotNull PsiElement element2) {
        PsiFile file1 = FileContextUtil.getContextFile(element1);
        PsiFile file2 = FileContextUtil.getContextFile(element2);
        if (Comparing.equal(file1, file2)) return true;
        if (file1 instanceof JavaDummyHolder && file2 instanceof JavaDummyHolder) return true;
        if (file1 instanceof JavaDummyHolder || file2 instanceof JavaDummyHolder) {
            JavaDummyHolder dummyHolder = (JavaDummyHolder) (file1 instanceof JavaDummyHolder ? file1 : file2);
            PsiElement other = file1 instanceof JavaDummyHolder ? file2 : file1;
            return dummyHolder.isSamePackage(other);
        }
        if (!(file1 instanceof PsiClassOwner)) return false;
        if (!(file2 instanceof PsiClassOwner)) return false;
        String package1 = ((PsiClassOwner) file1).getPackageName();
        String package2 = ((PsiClassOwner) file2).getPackageName();
        return Comparing.equal(package1, package2);
    }

    @Override
    @NotNull
    public Project getProject() {
        return myProject;
    }

    @Override
    @NotNull
    public PsiElementFactory getElementFactory() {
        return PsiElementFactory.SERVICE.getInstance(myProject);
    }

    @TestOnly
    @Override
    public void setAssertOnFileLoadingFilter(@NotNull final VirtualFileFilter filter, Disposable parentDisposable) {
        ((PsiManagerImpl)PsiManager.getInstance(myProject)).setAssertOnFileLoadingFilter(filter, parentDisposable);
    }
}
