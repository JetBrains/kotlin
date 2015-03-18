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

package org.jetbrains.kotlin.idea.decompiler.navigation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.GlobalContextImpl;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.di.InjectorForLazyResolve;
import org.jetbrains.kotlin.idea.caches.resolve.JsProjectDetector;
import org.jetbrains.kotlin.idea.stubindex.JetFullClassNameIndex;
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelFunctionFqnNameIndex;
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelPropertyFqnNameIndex;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.resolve.jvm.types.KotlinToJavaTypesMap;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.kotlin.types.DynamicTypesSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.idea.decompiler.navigation.MemberMatching.*;

public class JetSourceNavigationHelper {
    private static boolean forceResolve = false;

    private JetSourceNavigationHelper() {
    }

    @Nullable
    public static JetClassOrObject getSourceClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        return getSourceForNamedClassOrObject(decompiledClassOrObject);
    }

    @NotNull
    private static GlobalSearchScope createLibrarySourcesScope(@NotNull JetNamedDeclaration decompiledDeclaration) {
        JetFile containingFile = decompiledDeclaration.getContainingJetFile();
        VirtualFile libraryFile = containingFile.getVirtualFile();
        if (libraryFile == null) {
            return GlobalSearchScope.EMPTY_SCOPE;
        }

        Project project = decompiledDeclaration.getProject();
        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        boolean isJsProject = JsProjectDetector.isJsProject(project);

        if (!isJsProject && !projectFileIndex.isInLibraryClasses(libraryFile)) {
            return GlobalSearchScope.EMPTY_SCOPE;
        }

        Set<VirtualFile> sourceRootSet = Sets.newLinkedHashSet();
        for (OrderEntry entry : projectFileIndex.getOrderEntriesForFile(libraryFile)) {
            if (entry instanceof LibraryOrSdkOrderEntry) {
                KotlinPackage.addAll(sourceRootSet, entry.getFiles(OrderRootType.SOURCES));
            }
        }

        if (isJsProject) {
            Library library = NavigationPackage.getKotlinJavascriptLibrary(libraryFile, project);
            if (library != null) {
                KotlinPackage.addAll(sourceRootSet, library.getFiles(OrderRootType.SOURCES));
            }
        }

        return new LibrarySourcesScope(project, sourceRootSet);
    }

    private static List<JetFile> getContainingFiles(@NotNull Iterable<JetNamedDeclaration> declarations) {
        Set<JetFile> result = Sets.newHashSet();
        for (JetNamedDeclaration declaration : declarations) {
            PsiFile containingFile = declaration.getContainingFile();
            if (containingFile instanceof JetFile) {
                result.add((JetFile) containingFile);
            }
        }
        return Lists.newArrayList(result);
    }

    private static boolean haveRenamesInImports(@NotNull List<JetFile> files) {
        for (JetFile file : files) {
            for (JetImportDirective importDirective : file.getImportDirectives()) {
                if (importDirective.getAliasName() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static JetNamedDeclaration findSpecialProperty(@NotNull Name memberName, @NotNull JetClass containingClass) {
        // property constructor parameters
        List<JetParameter> constructorParameters = containingClass.getPrimaryConstructorParameters();
        for (JetParameter constructorParameter : constructorParameters) {
            if (memberName.equals(constructorParameter.getNameAsName()) && constructorParameter.hasValOrVarNode()) {
                return constructorParameter;
            }
        }

        // enum entries
        if (containingClass.hasModifier(JetTokens.ENUM_KEYWORD)) {
            for (JetEnumEntry enumEntry : ContainerUtil.findAll(containingClass.getDeclarations(), JetEnumEntry.class)) {
                if (memberName.equals(enumEntry.getNameAsName())) {
                    return enumEntry;
                }
            }
        }
        return null;
    }

    @Nullable
    private static JetNamedDeclaration getSourcePropertyOrFunction(@NotNull JetNamedDeclaration decompiledDeclaration) {
        String memberNameAsString = decompiledDeclaration.getName();
        assert memberNameAsString != null;
        Name memberName = Name.identifier(memberNameAsString);

        PsiElement decompiledContainer = decompiledDeclaration.getParent();

        Collection<JetNamedDeclaration> candidates;
        if (decompiledContainer instanceof JetFile) {
            candidates = getInitialTopLevelCandidates(decompiledDeclaration);
        }
        else if (decompiledContainer instanceof JetClassBody) {
            JetClassOrObject decompiledClassOrObject = (JetClassOrObject) decompiledContainer.getParent();
            JetClassOrObject sourceClassOrObject = getSourceClassOrObject(decompiledClassOrObject);

            //noinspection unchecked
            candidates = sourceClassOrObject == null
                         ? Collections.<JetNamedDeclaration>emptyList()
                         : getInitialMemberCandidates(sourceClassOrObject, memberName, (Class<JetNamedDeclaration>) decompiledDeclaration.getClass());

            if (candidates.isEmpty()) {
                if (decompiledDeclaration instanceof JetProperty && sourceClassOrObject instanceof JetClass) {
                    return findSpecialProperty(memberName, (JetClass) sourceClassOrObject);
                }
            }
        }
        else {
            throw new IllegalStateException("Unexpected container of decompiled declaration: "
                                            + decompiledContainer.getClass().getSimpleName());
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (!forceResolve) {
            candidates = filterByReceiverPresenceAndParametersCount(decompiledDeclaration, candidates);

            if (candidates.size() <= 1) {
                return candidates.isEmpty() ? null : candidates.iterator().next();
            }

            if (!haveRenamesInImports(getContainingFiles(candidates))) {
                candidates = filterByReceiverAndParameterTypes(decompiledDeclaration, candidates);

                if (candidates.size() <= 1) {
                    return candidates.isEmpty() ? null : candidates.iterator().next();
                }
            }
        }

        KotlinCodeAnalyzer analyzer = createAnalyzer(candidates, decompiledDeclaration.getProject());

        for (JetNamedDeclaration candidate : candidates) {
            //noinspection unchecked
            CallableDescriptor candidateDescriptor = (CallableDescriptor) analyzer.resolveToDescriptor(candidate);
            if (receiversMatch(decompiledDeclaration, candidateDescriptor)
                    && valueParametersTypesMatch(decompiledDeclaration, candidateDescriptor)
                    && typeParametersMatch((JetTypeParameterListOwner) decompiledDeclaration, candidateDescriptor.getTypeParameters())) {
                return candidate;
            }
        }

        return null;
    }

    @NotNull
    private static KotlinCodeAnalyzer createAnalyzer(
            @NotNull Collection<JetNamedDeclaration> candidates,
            @NotNull Project project
    ) {
        GlobalContextImpl globalContext = ContextPackage.GlobalContext();
        FileBasedDeclarationProviderFactory providerFactory = new FileBasedDeclarationProviderFactory(
                globalContext.getStorageManager(),
                getContainingFiles(candidates)
        );

        ModuleDescriptorImpl moduleDescriptor = new ModuleDescriptorImpl(Name.special("<library module>"),
                                                                         TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS,
                                                                         PlatformToKotlinClassMap.EMPTY);

        moduleDescriptor.addDependencyOnModule(moduleDescriptor);
        moduleDescriptor.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        moduleDescriptor.seal();

        ResolveSession resolveSession = new InjectorForLazyResolve(
                project,
                globalContext,
                moduleDescriptor,
                providerFactory,
                new BindingTraceContext(),
                AdditionalCheckerProvider.DefaultProvider.INSTANCE$,
                new DynamicTypesSettings()).getResolveSession();

        moduleDescriptor.initialize(resolveSession.getPackageFragmentProvider());
        return resolveSession;
    }

    @Nullable
    private static JetClassOrObject getSourceForNamedClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        FqName classFqName = decompiledClassOrObject.getFqName();
        assert classFqName != null;

        GlobalSearchScope librarySourcesScope = createLibrarySourcesScope(decompiledClassOrObject);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return null;
        }
        Collection<JetClassOrObject> classes = JetFullClassNameIndex.getInstance()
                .get(classFqName.asString(), decompiledClassOrObject.getProject(), librarySourcesScope);
        if (classes.isEmpty()) {
            return null;
        }
        return classes.iterator().next(); // if there are more than one class with this FQ, find first of them
    }

    @NotNull
    private static Collection<JetNamedDeclaration> getInitialTopLevelCandidates(@NotNull JetNamedDeclaration decompiledDeclaration) {
        FqName memberFqName = decompiledDeclaration.getFqName();
        assert memberFqName != null;

        GlobalSearchScope librarySourcesScope = createLibrarySourcesScope(decompiledDeclaration);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return Collections.emptyList();
        }
        //noinspection unchecked
        StringStubIndexExtension<JetNamedDeclaration> index =
                (StringStubIndexExtension<JetNamedDeclaration>) getIndexForTopLevelPropertyOrFunction(decompiledDeclaration);
        return index.get(memberFqName.asString(), decompiledDeclaration.getProject(), librarySourcesScope);
    }

    private static StringStubIndexExtension<? extends JetNamedDeclaration> getIndexForTopLevelPropertyOrFunction(
            @NotNull JetNamedDeclaration decompiledDeclaration
    ) {
        if (decompiledDeclaration instanceof JetNamedFunction) {
            return JetTopLevelFunctionFqnNameIndex.getInstance();
        }
        if (decompiledDeclaration instanceof JetProperty) {
            return JetTopLevelPropertyFqnNameIndex.getInstance();
        }
        throw new IllegalArgumentException("Neither function nor declaration: " + decompiledDeclaration.getClass().getName());
    }

    @NotNull
    private static List<JetNamedDeclaration> getInitialMemberCandidates(
            @NotNull JetClassOrObject sourceClassOrObject,
            @NotNull final Name name,
            @NotNull Class<JetNamedDeclaration> declarationClass
    ) {
        List<JetNamedDeclaration> allByClass = ContainerUtil.findAll(sourceClassOrObject.getDeclarations(), declarationClass);
        return ContainerUtil.filter(allByClass, new Condition<JetNamedDeclaration>() {
            @Override
            public boolean value(JetNamedDeclaration declaration) {
                return name.equals(declaration.getNameAsSafeName());
            }
        });
    }

    @NotNull
    private static List<JetNamedDeclaration> filterByReceiverPresenceAndParametersCount(
            final @NotNull JetNamedDeclaration decompiledDeclaration,
            @NotNull Collection<JetNamedDeclaration> candidates
    ) {
        return ContainerUtil.filter(candidates, new Condition<JetNamedDeclaration>() {
            @Override
            public boolean value(JetNamedDeclaration candidate) {
                return sameReceiverPresenceAndParametersCount(candidate, decompiledDeclaration);
            }
        });
    }

    @NotNull
    private static List<JetNamedDeclaration> filterByReceiverAndParameterTypes(
            final @NotNull JetNamedDeclaration decompiledDeclaration,
            @NotNull Collection<JetNamedDeclaration> candidates
    ) {
        return ContainerUtil.filter(candidates, new Condition<JetNamedDeclaration>() {
            @Override
            public boolean value(JetNamedDeclaration candidate) {
                return receiverAndParametersShortTypesMatch(candidate, decompiledDeclaration);
            }
        });
    }

    @Nullable
    public static JetNamedDeclaration getSourceProperty(@NotNull JetProperty decompiledProperty) {
        return getSourcePropertyOrFunction(decompiledProperty);
    }

    @Nullable
    public static JetNamedDeclaration getSourceFunction(@NotNull JetNamedFunction decompiledFunction) {
        return getSourcePropertyOrFunction(decompiledFunction);
    }

    @TestOnly
    public static void setForceResolve(boolean forceResolve) {
        JetSourceNavigationHelper.forceResolve = forceResolve;
    }

    @Nullable
    public static PsiClass getOriginalPsiClassOrCreateLightClass(@NotNull JetClassOrObject classOrObject) {
        if (LightClassUtil.belongsToKotlinBuiltIns(classOrObject.getContainingJetFile())) {
            Name className = classOrObject.getNameAsName();
            assert className != null : "Class from BuiltIns should have a name";
            ClassDescriptor classDescriptor = KotlinBuiltIns.getInstance().getBuiltInClassByName(className);

            FqNameUnsafe fqName = DescriptorUtils.getFqName(classDescriptor);
            if (fqName.isSafe()) {
                FqName javaFqName = KotlinToJavaTypesMap.getInstance().getKotlinToJavaFqName(fqName.toSafe());
                if (javaFqName != null) {
                    return JavaPsiFacade.getInstance(classOrObject.getProject()).findClass(
                            javaFqName.asString(), GlobalSearchScope.allScope(classOrObject.getProject()));
                }
            }
        }
        return LightClassUtil.getPsiClass(classOrObject);
    }

    @Nullable
    public static PsiClass getOriginalClass(@NotNull JetClassOrObject classOrObject) {
        // Copied from JavaPsiImplementationHelperImpl:getOriginalClass()
        String internalName = PsiCodegenPredictor.getPredefinedJvmInternalName(classOrObject);
        if (internalName == null) {
            return null;
        }
        String fqName = JvmClassName.byInternalName(internalName).getFqNameForClassNameWithoutDollars().asString();

        JetFile file = classOrObject.getContainingJetFile();

        VirtualFile vFile = file.getVirtualFile();
        Project project = file.getProject();

        final ProjectFileIndex idx = ProjectRootManager.getInstance(project).getFileIndex();

        if (vFile == null || !idx.isInLibrarySource(vFile)) return null;
        final Set<OrderEntry> orderEntries = new THashSet<OrderEntry>(idx.getOrderEntriesForFile(vFile));

        return JavaPsiFacade.getInstance(project).findClass(fqName, new GlobalSearchScope(project) {
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
    public static JetDeclaration replaceBySourceDeclarationIfPresent(@NotNull JetDeclaration original) {
        JetDeclaration sourceElement = original.accept(new SourceForDecompiledExtractingVisitor(), null);
        return sourceElement != null ? sourceElement : original;
    }

    private static class SourceForDecompiledExtractingVisitor extends JetVisitor<JetDeclaration, Void> {
        @Override
        public JetDeclaration visitNamedFunction(@NotNull JetNamedFunction function, Void data) {
            return getSourceFunction(function);
        }

        @Override
        public JetDeclaration visitProperty(@NotNull JetProperty property, Void data) {
            return getSourceProperty(property);
        }

        @Override
        public JetDeclaration visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, Void data) {
            return getSourceClassOrObject(declaration);
        }

        @Override
        public JetDeclaration visitClass(@NotNull JetClass klass, Void data) {
            return getSourceClassOrObject(klass);
        }
    }

    private static class LibrarySourcesScope extends GlobalSearchScope {
        private final Set<VirtualFile> sources;
        private final ProjectFileIndex fileIndex;

        public LibrarySourcesScope(Project project, Set<VirtualFile> sources) {
            super(project);
            fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            this.sources = sources;
        }

        @Override
        public boolean contains(@NotNull VirtualFile file) {
            if (fileIndex.isInLibrarySource(file)) {
                return sources.contains(fileIndex.getSourceRootForFile(file));
            }
            return false;
        }

        @Override
        public int compare(@NotNull VirtualFile file1, @NotNull VirtualFile file2) {
            return 0;
        }

        @Override
        public boolean isSearchInModuleContent(@NotNull Module aModule) {
            return false;
        }

        @Override
        public boolean isSearchInLibraries() {
            return true;
        }
    }

}
