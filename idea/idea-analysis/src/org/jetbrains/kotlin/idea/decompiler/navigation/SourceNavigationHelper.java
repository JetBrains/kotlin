/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.navigation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.context.ContextKt;
import org.jetbrains.kotlin.context.MutableModuleContext;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.frontend.di.InjectionKt;
import org.jetbrains.kotlin.idea.stubindex.*;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;
import static org.jetbrains.kotlin.idea.decompiler.navigation.MemberMatching.*;

public class SourceNavigationHelper {
    private static final Logger LOG = Logger.getInstance(SourceNavigationHelper.class);

    public enum NavigationKind {
        CLASS_FILES_TO_SOURCES,
        SOURCES_TO_CLASS_FILES
    }

    private static boolean forceResolve = false;

    private SourceNavigationHelper() {
    }

    @NotNull
    private static GlobalSearchScope createLibraryOrSourcesScope(
            @NotNull KtNamedDeclaration declaration,
            @NotNull NavigationKind navigationKind
    ) {
        KtFile containingFile = declaration.getContainingKtFile();
        VirtualFile libraryFile = containingFile.getVirtualFile();
        if (libraryFile == null) return GlobalSearchScope.EMPTY_SCOPE;

        boolean includeLibrarySources = navigationKind == NavigationKind.CLASS_FILES_TO_SOURCES;
        if (ProjectRootsUtil.isInContent(declaration, false, includeLibrarySources, !includeLibrarySources, true)) {
            return GlobalSearchScope.EMPTY_SCOPE;
        }

        Project project = declaration.getProject();
        return includeLibrarySources
               ? KotlinSourceFilterScope.librarySources(new EverythingGlobalScope(project), project)
               : KotlinSourceFilterScope.libraryClassFiles(new EverythingGlobalScope(project), project);
    }

    private static List<KtFile> getContainingFiles(@NotNull Iterable<KtNamedDeclaration> declarations) {
        Set<KtFile> result = Sets.newHashSet();
        for (KtNamedDeclaration declaration : declarations) {
            PsiFile containingFile = declaration.getContainingFile();
            if (containingFile instanceof KtFile) {
                result.add((KtFile) containingFile);
            }
        }
        return Lists.newArrayList(result);
    }

    private static boolean haveRenamesInImports(@NotNull List<KtFile> files) {
        for (KtFile file : files) {
            for (KtImportDirective importDirective : file.getImportDirectives()) {
                if (importDirective.getAliasName() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static KtNamedDeclaration findSpecialProperty(@NotNull Name memberName, @NotNull KtClass containingClass) {
        // property constructor parameters
        List<KtParameter> constructorParameters = containingClass.getPrimaryConstructorParameters();
        for (KtParameter constructorParameter : constructorParameters) {
            if (memberName.equals(constructorParameter.getNameAsName()) && constructorParameter.hasValOrVar()) {
                return constructorParameter;
            }
        }

        // enum entries
        if (containingClass.hasModifier(KtTokens.ENUM_KEYWORD)) {
            for (KtEnumEntry enumEntry : ContainerUtil.findAll(containingClass.getDeclarations(), KtEnumEntry.class)) {
                if (memberName.equals(enumEntry.getNameAsName())) {
                    return enumEntry;
                }
            }
        }
        return null;
    }

    @Nullable
    private static KtNamedDeclaration convertPropertyOrFunction(
            @NotNull KtNamedDeclaration declaration,
            @NotNull NavigationKind navigationKind
    ) {
        if (declaration instanceof KtPrimaryConstructor) {
            KtClassOrObject sourceClassOrObject =
                    findClassOrObject(((KtPrimaryConstructor) declaration).getContainingClassOrObject(), navigationKind);
            KtPrimaryConstructor primaryConstructor = sourceClassOrObject != null ? sourceClassOrObject.getPrimaryConstructor() : null;
            return primaryConstructor != null ? primaryConstructor : sourceClassOrObject;
        }

        String memberNameAsString = declaration.getName();
        if (memberNameAsString == null) {
            LOG.debug("JetSourceNavigationHelper.convertPropertyOrFunction(): null name for declaration " + declaration);
            return null;
        }
        Name memberName = Name.identifier(memberNameAsString);

        PsiElement decompiledContainer = declaration.getParent();

        Collection<KtNamedDeclaration> candidates;
        if (decompiledContainer instanceof KtFile) {
            candidates = getInitialTopLevelCandidates(declaration, navigationKind);
        }
        else if (decompiledContainer instanceof KtClassBody) {
            KtClassOrObject decompiledClassOrObject = (KtClassOrObject) decompiledContainer.getParent();
            KtClassOrObject sourceClassOrObject = findClassOrObject(decompiledClassOrObject, navigationKind);

            //noinspection unchecked
            candidates = sourceClassOrObject == null
                         ? Collections.<KtNamedDeclaration>emptyList()
                         : getInitialMemberCandidates(sourceClassOrObject, memberName,
                                                      (Class<KtNamedDeclaration>) declaration.getClass());

            if (candidates.isEmpty()) {
                if (declaration instanceof KtProperty && sourceClassOrObject instanceof KtClass) {
                    return findSpecialProperty(memberName, (KtClass) sourceClassOrObject);
                }
            }
        }
        else {
            throw new IllegalStateException("Unexpected container of " +
                                            (navigationKind == NavigationKind.CLASS_FILES_TO_SOURCES ? "decompiled" : "source") +
                                            " declaration: " +
                                            decompiledContainer.getClass().getSimpleName());
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates = filterByOrderEntries(declaration, candidates);

        if (!forceResolve) {
            candidates = filterByReceiverPresenceAndParametersCount(declaration, candidates);

            if (candidates.size() <= 1) {
                return candidates.isEmpty() ? null : candidates.iterator().next();
            }

            if (!haveRenamesInImports(getContainingFiles(candidates))) {
                candidates = filterByReceiverAndParameterTypes(declaration, candidates);

                if (candidates.size() <= 1) {
                    return candidates.isEmpty() ? null : candidates.iterator().next();
                }
            }
        }

        KotlinCodeAnalyzer analyzer = createAnalyzer(candidates, declaration.getProject());

        for (KtNamedDeclaration candidate : candidates) {
            //noinspection unchecked
            CallableDescriptor candidateDescriptor = (CallableDescriptor) analyzer.resolveToDescriptor(candidate);
            if (receiversMatch(declaration, candidateDescriptor)
                && valueParametersTypesMatch(declaration, candidateDescriptor)
                && typeParametersMatch((KtTypeParameterListOwner) declaration, candidateDescriptor.getTypeParameters())) {
                return candidate;
            }
        }

        return null;
    }

    @NotNull
    private static KotlinCodeAnalyzer createAnalyzer(
            @NotNull Collection<KtNamedDeclaration> candidates,
            @NotNull Project project
    ) {
        MutableModuleContext context = ContextKt.ContextForNewModule(
                ContextKt.ProjectContext(project), Name.special("<library module>"), DefaultBuiltIns.getInstance(), null
        );
        context.setDependencies(context.getModule(), context.getModule().getBuiltIns().getBuiltInsModule());
        ResolveSession resolveSession = InjectionKt.createLazyResolveSession(context, getContainingFiles(candidates));
        context.initializeModuleContents(resolveSession.getPackageFragmentProvider());
        return resolveSession;
    }

    @Nullable
    private static <T extends KtNamedDeclaration> T findFirstMatchingInIndex(
            @NotNull T entity,
            @NotNull NavigationKind navigationKind,
            @NotNull StringStubIndexExtension<T> index
    ) {
        FqName classFqName = entity.getFqName();
        assert classFqName != null;

        GlobalSearchScope librarySourcesScope = createLibraryOrSourcesScope(entity, navigationKind);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return null;
        }
        Collection<T> classes = index.get(classFqName.asString(), entity.getProject(), librarySourcesScope);
        if (classes.isEmpty()) {
            return null;
        }
        return classes.iterator().next(); // if there are more than one with this FQ, find first of them
    }

    @Nullable
    private static KtClassOrObject findClassOrObject(
            @NotNull KtClassOrObject decompiledClassOrObject,
            @NotNull NavigationKind navigationKind
    ) {
        return findFirstMatchingInIndex(decompiledClassOrObject, navigationKind, KotlinFullClassNameIndex.getInstance());
    }

    @NotNull
    private static Collection<KtNamedDeclaration> getInitialTopLevelCandidates(
            @NotNull KtNamedDeclaration declaration,
            @NotNull NavigationKind navigationKind
    ) {
        FqName memberFqName = declaration.getFqName();
        assert memberFqName != null;

        GlobalSearchScope librarySourcesScope = createLibraryOrSourcesScope(declaration, navigationKind);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return Collections.emptyList();
        }
        //noinspection unchecked
        StringStubIndexExtension<KtNamedDeclaration> index =
                (StringStubIndexExtension<KtNamedDeclaration>) getIndexForTopLevelPropertyOrFunction(declaration);
        return index.get(memberFqName.asString(), declaration.getProject(), librarySourcesScope);
    }

    private static StringStubIndexExtension<? extends KtNamedDeclaration> getIndexForTopLevelPropertyOrFunction(
            @NotNull KtNamedDeclaration decompiledDeclaration
    ) {
        if (decompiledDeclaration instanceof KtNamedFunction) {
            return KotlinTopLevelFunctionFqnNameIndex.getInstance();
        }
        if (decompiledDeclaration instanceof KtProperty) {
            return KotlinTopLevelPropertyFqnNameIndex.getInstance();
        }
        throw new IllegalArgumentException("Neither function nor declaration: " + decompiledDeclaration.getClass().getName());
    }

    @NotNull
    private static List<KtNamedDeclaration> getInitialMemberCandidates(
            @NotNull KtClassOrObject sourceClassOrObject,
            @NotNull final Name name,
            @NotNull Class<KtNamedDeclaration> declarationClass
    ) {
        List<KtNamedDeclaration> allByClass = ContainerUtil.findAll(sourceClassOrObject.getDeclarations(), declarationClass);
        return ContainerUtil.filter(allByClass, new Condition<KtNamedDeclaration>() {
            @Override
            public boolean value(KtNamedDeclaration declaration) {
                return name.equals(declaration.getNameAsSafeName());
            }
        });
    }

    @NotNull
    private static List<KtNamedDeclaration> filterByOrderEntries(
            @NotNull KtNamedDeclaration declaration,
            @NotNull Collection<KtNamedDeclaration> candidates
    ) {
        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(declaration.getProject()).getFileIndex();
        final List<OrderEntry> orderEntries = fileIndex.getOrderEntriesForFile(declaration.getContainingFile().getVirtualFile());

        return CollectionsKt.filter(
                candidates,
                new Function1<KtNamedDeclaration, Boolean>() {
                    @Override
                    public Boolean invoke(KtNamedDeclaration candidate) {
                        List<OrderEntry> candidateOrderEntries = fileIndex.getOrderEntriesForFile(candidate.getContainingFile().getVirtualFile());
                        return ContainerUtil.intersects(orderEntries, candidateOrderEntries);
                    }
                }
        );
    }

    @NotNull
    private static List<KtNamedDeclaration> filterByReceiverPresenceAndParametersCount(
            final @NotNull KtNamedDeclaration decompiledDeclaration,
            @NotNull Collection<KtNamedDeclaration> candidates
    ) {
        return ContainerUtil.filter(candidates, new Condition<KtNamedDeclaration>() {
            @Override
            public boolean value(KtNamedDeclaration candidate) {
                return sameReceiverPresenceAndParametersCount(candidate, decompiledDeclaration);
            }
        });
    }

    @NotNull
    private static List<KtNamedDeclaration> filterByReceiverAndParameterTypes(
            final @NotNull KtNamedDeclaration decompiledDeclaration,
            @NotNull Collection<KtNamedDeclaration> candidates
    ) {
        return ContainerUtil.filter(candidates, new Condition<KtNamedDeclaration>() {
            @Override
            public boolean value(KtNamedDeclaration candidate) {
                return receiverAndParametersShortTypesMatch(candidate, decompiledDeclaration);
            }
        });
    }

    @TestOnly
    public static void setForceResolve(boolean forceResolve) {
        SourceNavigationHelper.forceResolve = forceResolve;
    }

    @Nullable
    public static PsiClass getOriginalPsiClassOrCreateLightClass(@NotNull KtClassOrObject classOrObject) {
        FqName fqName = classOrObject.getFqName();
        if (fqName != null) {
            ClassId javaClassId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(fqName.toUnsafe());
            if (javaClassId != null) {
                return JavaPsiFacade.getInstance(classOrObject.getProject()).findClass(
                        javaClassId.asSingleFqName().asString(),
                        GlobalSearchScope.allScope(classOrObject.getProject())
                );
            }
        }
        return toLightClass(classOrObject);
    }

    @Nullable
    public static PsiClass getOriginalClass(@NotNull KtClassOrObject classOrObject) {
        // Copied from JavaPsiImplementationHelperImpl:getOriginalClass()
        FqName fqName = classOrObject.getFqName();
        if (fqName == null) {
            return null;
        }

        KtFile file = classOrObject.getContainingKtFile();

        VirtualFile vFile = file.getVirtualFile();
        Project project = file.getProject();

        final ProjectFileIndex idx = ProjectRootManager.getInstance(project).getFileIndex();

        if (vFile == null || !idx.isInLibrarySource(vFile)) return null;
        final Set<OrderEntry> orderEntries = new THashSet<OrderEntry>(idx.getOrderEntriesForFile(vFile));

        return JavaPsiFacade.getInstance(project).findClass(fqName.asString(), new GlobalSearchScope(project) {
            @Override
            public int compare(@NotNull VirtualFile file1, @NotNull VirtualFile file2) {
                return 0;
            }

            @Override
            public boolean contains(@NotNull VirtualFile file) {
                List<OrderEntry> entries = idx.getOrderEntriesForFile(file);
                for (OrderEntry entry : entries) {
                    if (orderEntries.contains(entry)) return true;
                }
                return false;
            }

            @Override
            public boolean isSearchInModuleContent(@NotNull Module aModule) {
                return false;
            }

            @Override
            public boolean isSearchInLibraries() {
                return true;
            }
        });
    }

    @NotNull
    public static KtDeclaration getNavigationElement(@NotNull KtDeclaration declaration) {
        return navigateToDeclaration(declaration, NavigationKind.CLASS_FILES_TO_SOURCES);
    }

    @NotNull
    public static KtDeclaration getOriginalElement(@NotNull KtDeclaration declaration) {
        return navigateToDeclaration(declaration, NavigationKind.SOURCES_TO_CLASS_FILES);
    }

    @NotNull
    private static KtDeclaration navigateToDeclaration(
            @NotNull KtDeclaration from,
            @NotNull NavigationKind navigationKind
    ) {
        if (DumbService.isDumb(from.getProject())) return from;

        switch (navigationKind) {
            case CLASS_FILES_TO_SOURCES:
                if (!from.getContainingKtFile().isCompiled()) return from;
                break;
            case SOURCES_TO_CLASS_FILES:
                if (from.getContainingKtFile().isCompiled()) return from;
                if (!ProjectRootsUtil.isInContent(from, false, true, false, true)) return from;
                if (KtPsiUtil.isLocal(from)) return from;
                break;
        }

        KtDeclaration result = from.accept(new SourceAndDecompiledConversionVisitor(navigationKind), null);
        return result != null ? result : from;
    }

    private static class SourceAndDecompiledConversionVisitor extends KtVisitor<KtDeclaration, Void> {
        private final NavigationKind navigationKind;

        public SourceAndDecompiledConversionVisitor(@NotNull NavigationKind navigationKind) {
            this.navigationKind = navigationKind;
        }

        @Override
        public KtDeclaration visitNamedFunction(@NotNull KtNamedFunction function, Void data) {
            return convertPropertyOrFunction(function, navigationKind);
        }

        @Override
        public KtDeclaration visitProperty(@NotNull KtProperty property, Void data) {
            return convertPropertyOrFunction(property, navigationKind);
        }

        @Override
        public KtDeclaration visitObjectDeclaration(@NotNull KtObjectDeclaration declaration, Void data) {
            return findClassOrObject(declaration, navigationKind);
        }

        @Override
        public KtDeclaration visitClass(@NotNull KtClass klass, Void data) {
            return findClassOrObject(klass, navigationKind);
        }

        @Override
        public KtDeclaration visitTypeAlias(@NotNull KtTypeAlias typeAlias, Void data) {
            return findFirstMatchingInIndex(typeAlias, navigationKind, KotlinTopLevelTypeAliasFqNameIndex.getInstance());
        }

        @Override
        public KtDeclaration visitParameter(@NotNull KtParameter parameter, Void data) {
            KtCallableDeclaration callableDeclaration = (KtCallableDeclaration) parameter.getParent().getParent();
            List<KtParameter> parameters = callableDeclaration.getValueParameters();
            int index = parameters.indexOf(parameter);

            KtCallableDeclaration sourceCallable = (KtCallableDeclaration) callableDeclaration.accept(this, null);
            if (sourceCallable == null) return null;
            List<KtParameter> sourceParameters = sourceCallable.getValueParameters();
            if (sourceParameters.size() != parameters.size()) return null;
            return sourceParameters.get(index);
        }

        @Override
        public KtDeclaration visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor, Void data) {
            return convertPropertyOrFunction(constructor, navigationKind);
        }

        @Override
        public KtDeclaration visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor, Void data) {
            return convertPropertyOrFunction(constructor, navigationKind);
        }
    }
}
