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
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.MutableModuleContext;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.frontend.di.DiPackage;
import org.jetbrains.kotlin.idea.stubindex.JetFullClassNameIndex;
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope;
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelFunctionFqnNameIndex;
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelPropertyFqnNameIndex;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.kotlin.types.DynamicTypesSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.descriptors.DescriptorsPackage.ModuleParameters;
import static org.jetbrains.kotlin.idea.decompiler.navigation.MemberMatching.*;

public class JetSourceNavigationHelper {
    public enum NavigationKind {
        CLASS_FILES_TO_SOURCES,
        SOURCES_TO_CLASS_FILES
    }

    private static boolean forceResolve = false;

    private JetSourceNavigationHelper() {
    }

    @NotNull
    private static GlobalSearchScope createLibraryOrSourcesScope(
            @NotNull JetNamedDeclaration declaration,
            @NotNull NavigationKind navigationKind
    ) {
        JetFile containingFile = declaration.getContainingJetFile();
        VirtualFile libraryFile = containingFile.getVirtualFile();
        if (libraryFile == null) return GlobalSearchScope.EMPTY_SCOPE;

        boolean includeLibrarySources = navigationKind == NavigationKind.CLASS_FILES_TO_SOURCES;
        if (ProjectRootsUtil.isInContent(declaration, false, includeLibrarySources, !includeLibrarySources)) {
            return GlobalSearchScope.EMPTY_SCOPE;
        }

        Project project = declaration.getProject();
        return includeLibrarySources
               ? JetSourceFilterScope.kotlinLibrarySources(GlobalSearchScope.allScope(project), project)
               : JetSourceFilterScope.kotlinLibraryClassFiles(GlobalSearchScope.allScope(project), project);
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
            if (memberName.equals(constructorParameter.getNameAsName()) && constructorParameter.hasValOrVar()) {
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
    private static JetNamedDeclaration convertPropertyOrFunction(
            @NotNull JetNamedDeclaration declaration,
            @NotNull NavigationKind navigationKind
    ) {
        if (declaration instanceof JetPrimaryConstructor) {
            JetClassOrObject sourceClassOrObject =
                    convertNamedClassOrObject(((JetPrimaryConstructor) declaration).getContainingClassOrObject(), navigationKind);
            JetPrimaryConstructor primaryConstructor = sourceClassOrObject != null ? sourceClassOrObject.getPrimaryConstructor() : null;
            return primaryConstructor != null ? primaryConstructor : sourceClassOrObject;
        }

        String memberNameAsString = declaration.getName();
        assert memberNameAsString != null;
        Name memberName = Name.identifier(memberNameAsString);

        PsiElement decompiledContainer = declaration.getParent();

        Collection<JetNamedDeclaration> candidates;
        if (decompiledContainer instanceof JetFile) {
            candidates = getInitialTopLevelCandidates(declaration, navigationKind);
        }
        else if (decompiledContainer instanceof JetClassBody) {
            JetClassOrObject decompiledClassOrObject = (JetClassOrObject) decompiledContainer.getParent();
            JetClassOrObject sourceClassOrObject = convertNamedClassOrObject(decompiledClassOrObject, navigationKind);

            //noinspection unchecked
            candidates = sourceClassOrObject == null
                         ? Collections.<JetNamedDeclaration>emptyList()
                         : getInitialMemberCandidates(sourceClassOrObject, memberName,
                                                      (Class<JetNamedDeclaration>) declaration.getClass());

            if (candidates.isEmpty()) {
                if (declaration instanceof JetProperty && sourceClassOrObject instanceof JetClass) {
                    return findSpecialProperty(memberName, (JetClass) sourceClassOrObject);
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

        for (JetNamedDeclaration candidate : candidates) {
            //noinspection unchecked
            CallableDescriptor candidateDescriptor = (CallableDescriptor) analyzer.resolveToDescriptor(candidate);
            if (receiversMatch(declaration, candidateDescriptor)
                    && valueParametersTypesMatch(declaration, candidateDescriptor)
                    && typeParametersMatch((JetTypeParameterListOwner) declaration, candidateDescriptor.getTypeParameters())) {
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

        MutableModuleContext newModuleContext = ContextPackage.ContextForNewModule(
                project, Name.special("<library module>"),
                ModuleParameters(
                        TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS,
                        PlatformToKotlinClassMap.EMPTY
                )
        );

        newModuleContext.setDependencies(newModuleContext.getModule(), KotlinBuiltIns.getInstance().getBuiltInsModule());

        FileBasedDeclarationProviderFactory providerFactory = new FileBasedDeclarationProviderFactory(
                newModuleContext.getStorageManager(),
                getContainingFiles(candidates)
        );

        ResolveSession resolveSession = DiPackage.createLazyResolveSession(
                newModuleContext,
                providerFactory,
                new BindingTraceContext(),
                TargetPlatform.Default.platformConfigurator,
                new DynamicTypesSettings()
        );

        newModuleContext.initializeModuleContents(resolveSession.getPackageFragmentProvider());
        return resolveSession;
    }

    @Nullable
    private static JetClassOrObject convertNamedClassOrObject(
            @NotNull JetClassOrObject classOrObject,
            @NotNull NavigationKind navigationKind
    ) {
        FqName classFqName = classOrObject.getFqName();
        assert classFqName != null;

        GlobalSearchScope librarySourcesScope = createLibraryOrSourcesScope(classOrObject, navigationKind);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return null;
        }
        Collection<JetClassOrObject> classes = JetFullClassNameIndex.getInstance()
                .get(classFqName.asString(), classOrObject.getProject(), librarySourcesScope);
        if (classes.isEmpty()) {
            return null;
        }
        return classes.iterator().next(); // if there are more than one class with this FQ, find first of them
    }

    @NotNull
    private static Collection<JetNamedDeclaration> getInitialTopLevelCandidates(
            @NotNull JetNamedDeclaration declaration,
            @NotNull NavigationKind navigationKind
    ) {
        FqName memberFqName = declaration.getFqName();
        assert memberFqName != null;

        GlobalSearchScope librarySourcesScope = createLibraryOrSourcesScope(declaration, navigationKind);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return Collections.emptyList();
        }
        //noinspection unchecked
        StringStubIndexExtension<JetNamedDeclaration> index =
                (StringStubIndexExtension<JetNamedDeclaration>) getIndexForTopLevelPropertyOrFunction(declaration);
        return index.get(memberFqName.asString(), declaration.getProject(), librarySourcesScope);
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

            ClassId javaClassId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(DescriptorUtils.getFqName(classDescriptor));
            if (javaClassId != null) {
                return JavaPsiFacade.getInstance(classOrObject.getProject()).findClass(
                        javaClassId.asSingleFqName().asString(),
                        GlobalSearchScope.allScope(classOrObject.getProject())
                );
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
    public static JetDeclaration getNavigationElement(@NotNull JetDeclaration declaration) {
        return navigateToDeclaration(declaration, NavigationKind.CLASS_FILES_TO_SOURCES);
    }

    @NotNull
    public static JetDeclaration getOriginalElement(@NotNull JetDeclaration declaration) {
        return navigateToDeclaration(declaration, NavigationKind.SOURCES_TO_CLASS_FILES);
    }

    @NotNull
    public static JetDeclaration navigateToDeclaration(
            @NotNull JetDeclaration from,
            @NotNull NavigationKind navigationKind) {
        if (DumbService.isDumb(from.getProject())) return from;

        switch (navigationKind) {
            case CLASS_FILES_TO_SOURCES:
                if (!from.getContainingJetFile().isCompiled()) return from;
                break;
            case SOURCES_TO_CLASS_FILES:
                if (from.getContainingJetFile().isCompiled()) return from;
                if (!ProjectRootsUtil.isInContent(from, false, true, false)) return from;
                if (JetPsiUtil.isLocal(from)) return from;
                break;
        }

        JetDeclaration result = from.accept(new SourceAndDecompiledConversionVisitor(navigationKind), null);
        return result != null ? result : from;
    }

    private static class SourceAndDecompiledConversionVisitor extends JetVisitor<JetDeclaration, Void> {
        private final NavigationKind navigationKind;

        public SourceAndDecompiledConversionVisitor(@NotNull NavigationKind navigationKind) {
            this.navigationKind = navigationKind;
        }

        @Override
        public JetDeclaration visitNamedFunction(@NotNull JetNamedFunction function, Void data) {
            return convertPropertyOrFunction(function, navigationKind);
        }

        @Override
        public JetDeclaration visitProperty(@NotNull JetProperty property, Void data) {
            return convertPropertyOrFunction(property, navigationKind);
        }

        @Override
        public JetDeclaration visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, Void data) {
            return convertNamedClassOrObject(declaration, navigationKind);
        }

        @Override
        public JetDeclaration visitClass(@NotNull JetClass klass, Void data) {
            return convertNamedClassOrObject(klass, navigationKind);
        }

        @Override
        public JetDeclaration visitParameter(@NotNull JetParameter parameter, Void data) {
            JetCallableDeclaration callableDeclaration = (JetCallableDeclaration) parameter.getParent().getParent();
            List<JetParameter> parameters = callableDeclaration.getValueParameters();
            int index = parameters.indexOf(parameter);

            JetCallableDeclaration sourceCallable = (JetCallableDeclaration) callableDeclaration.accept(this, null);
            if (sourceCallable == null) return null;
            List<JetParameter> sourceParameters = sourceCallable.getValueParameters();
            if (sourceParameters.size() != parameters.size()) return null;
            return sourceParameters.get(index);
        }

        @Override
        public JetDeclaration visitPrimaryConstructor(@NotNull JetPrimaryConstructor constructor, Void data) {
            return convertPropertyOrFunction(constructor, navigationKind);
        }

        @Override
        public JetDeclaration visitSecondaryConstructor(@NotNull JetSecondaryConstructor constructor, Void data) {
            return convertPropertyOrFunction(constructor, navigationKind);
        }
    }
}
