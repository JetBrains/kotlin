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

package org.jetbrains.jet.plugin.libraries;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelPropertiesFqnNameIndex;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;

import java.util.*;

public class JetSourceNavigationHelper {
    private JetSourceNavigationHelper() {
    }

    @Nullable
    private static<D extends ClassOrNamespaceDescriptor> Pair<BindingContext, D>
            getBindingContextAndClassOrNamespaceDescriptor(@NotNull ReadOnlySlice<FqName, D> slice,
                                                           @NotNull JetNamedDeclaration declaration,
                                                           @Nullable FqName fqName) {
        if (fqName == null || DumbService.isDumb(declaration.getProject())) {
            return null;
        }
        final Project project = declaration.getProject();
        final List<JetFile> libraryFiles = findAllSourceFilesWhichContainIdentifier(declaration);
        BindingContext bindingContext = AnalyzerFacadeForJVM.INSTANCE.analyzeFiles(
                project,
                libraryFiles,
                Collections.<AnalyzerScriptParameter>emptyList(),
                Predicates.<PsiFile>alwaysTrue()).getBindingContext();
        D descriptor = bindingContext.get(slice, fqName);
        if (descriptor != null) {
            return new Pair<BindingContext, D>(bindingContext, descriptor);
        }
        return null;
    }

    @Nullable
    private static Pair<BindingContext, ClassDescriptor> getBindingContextAndClassDescriptor(@NotNull JetClassOrObject decompiledClassOrObject) {
        JetNamedDeclaration asNamed = (JetNamedDeclaration) decompiledClassOrObject;
        return getBindingContextAndClassOrNamespaceDescriptor(
                BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, asNamed, JetPsiUtil.getFQName(asNamed));
    }

    @Nullable
    private static Pair<BindingContext, NamespaceDescriptor> getBindingContextAndNamespaceDescriptor(
            @NotNull JetNamedDeclaration declaration) {
        JetFile file = (JetFile) declaration.getContainingFile();
        return getBindingContextAndClassOrNamespaceDescriptor(
                BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, declaration, JetPsiUtil.getFQName(file));
    }

    @Nullable
    public static JetClassOrObject getSourceClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        if (decompiledClassOrObject instanceof JetObjectDeclaration && decompiledClassOrObject.getParent() instanceof JetClassObject) {
            // class object case

            JetClass decompiledClass = PsiTreeUtil.getParentOfType(decompiledClassOrObject, JetClass.class);
            assert decompiledClass != null;

            JetClass sourceClass = (JetClass) getSourceForNamedClassOrObject(decompiledClass);
            if (sourceClass == null) {
                return null;
            }

            if (sourceClass.hasModifier(JetTokens.ENUM_KEYWORD)) {
                return sourceClass;
            }

            JetClassObject classObject = sourceClass.getClassObject();
            assert classObject != null;
            return classObject.getObjectDeclaration();
        }
        return getSourceForNamedClassOrObject(decompiledClassOrObject);
    }

    @NotNull
    private static GlobalSearchScope createLibrarySourcesScopeForFile(@NotNull VirtualFile libraryFile, @NotNull Project project) {
        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        GlobalSearchScope resultScope = GlobalSearchScope.EMPTY_SCOPE;
        for (OrderEntry orderEntry : projectFileIndex.getOrderEntriesForFile(libraryFile)) {
            for (VirtualFile sourceDir : orderEntry.getFiles(OrderRootType.SOURCES)) {
                resultScope = resultScope.uniteWith(GlobalSearchScopes.directoryScope(project, sourceDir, true));
            }
        }
        return resultScope;
    }

    @NotNull
    private static GlobalSearchScope createLibrarySourcesScope(@NotNull JetNamedDeclaration decompiledDeclaration) {
        JetFile containingFile = (JetFile) decompiledDeclaration.getContainingFile();
        VirtualFile libraryFile = containingFile.getVirtualFile();
        Project project = decompiledDeclaration.getProject();
        return libraryFile == null ? GlobalSearchScope.EMPTY_SCOPE : createLibrarySourcesScopeForFile(libraryFile, project);
    }

    private static List<JetFile> getContainingFiles(@NotNull Iterable<? extends JetNamedDeclaration> declarations) {
        Set<JetFile> result = Sets.newHashSet();
        for (JetNamedDeclaration declaration : declarations) {
            PsiFile containingFile = declaration.getContainingFile();
            if (containingFile instanceof JetFile) {
                result.add((JetFile) containingFile);
            }
        }
        return Lists.newArrayList(result);
    }

    @NotNull
    private static List<JetFile> findAllSourceFilesWhichContainIdentifier(@NotNull JetNamedDeclaration jetDeclaration) {
        VirtualFile libraryFile = jetDeclaration.getContainingFile().getVirtualFile();
        String name = jetDeclaration.getName();
        if (libraryFile == null || name == null) {
            return Collections.emptyList();
        }
        Project project = jetDeclaration.getProject();
        CacheManager cacheManager = CacheManager.SERVICE.getInstance(project);
        PsiFile[] filesWithWord = cacheManager.getFilesWithWord(name,
                                                                UsageSearchContext.IN_CODE,
                                                                createLibrarySourcesScopeForFile(libraryFile, project),
                                                                true);
        List<JetFile> jetFiles = new ArrayList<JetFile>();
        for (PsiFile psiFile : filesWithWord) {
            if (psiFile instanceof JetFile) {
                jetFiles.add((JetFile) psiFile);
            }
        }
        return jetFiles;
    }

    @Nullable
    private static <Decl extends JetNamedDeclaration> Pair<BindingContext, JetScope> getBindingContextAndMemberScopeForLibrarySources(
            @NotNull Decl decompiledDeclaration) {
        PsiElement declarationContainer = decompiledDeclaration.getParent();

        if (declarationContainer instanceof JetFile) {
            Pair<BindingContext, NamespaceDescriptor> contextAndNamespace = getBindingContextAndNamespaceDescriptor(decompiledDeclaration);
            if (contextAndNamespace == null) {
                return null;
            }
            return Pair.create(contextAndNamespace.first, contextAndNamespace.second.getMemberScope());
        }
        if (declarationContainer instanceof JetClassBody) {
            JetClassOrObject decompiledClassOrObject = (JetClassOrObject) declarationContainer.getParent();
            assert decompiledClassOrObject != null;

            if (decompiledClassOrObject instanceof JetObjectDeclaration && decompiledClassOrObject.getParent() instanceof JetClassObject) {
                // class object case

                JetClass klass = PsiTreeUtil.getParentOfType(decompiledClassOrObject, JetClass.class);
                assert klass != null;
                Pair<BindingContext, ClassDescriptor> contextAndClass = getBindingContextAndClassDescriptor(klass);

                if (contextAndClass == null) {
                    return null;
                }

                JetType classObjectType = contextAndClass.second.getClassObjectType();
                assert classObjectType != null;
                return Pair.create(contextAndClass.first, classObjectType.getMemberScope());
            }
            else {
                Pair<BindingContext, ClassDescriptor> contextAndClass = getBindingContextAndClassDescriptor(decompiledClassOrObject);

                if (contextAndClass == null) {
                    return null;
                }

                return Pair.create(contextAndClass.first, contextAndClass.second.getDefaultType().getMemberScope());
            }
        }

        throw new IllegalStateException("Unexpected container of decompiled declaration: "
                                        + declarationContainer.getClass().getSimpleName());
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

    private static String getTypeShortName(@NotNull JetTypeReference typeReference) {
        JetTypeElement typeElement = typeReference.getTypeElement();
        assert typeElement != null;
        return typeElement.accept(new JetVisitor<String, Void>() {
            @Override
            public String visitDeclaration(JetDeclaration declaration, Void data) {
                throw new IllegalStateException("This visitor shouldn't be invoked for " + declaration.getClass());
            }

            @Override
            public String visitUserType(JetUserType type, Void data) {
                JetSimpleNameExpression referenceExpression = type.getReferenceExpression();
                assert referenceExpression != null;
                return referenceExpression.getReferencedName();
            }

            @Override
            public String visitFunctionType(JetFunctionType type, Void data) {
                KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
                int parameterCount = type.getParameters().size();

                if (type.getReceiverTypeRef() == null) {
                    return builtIns.getFunction(parameterCount).getName().getName();
                }
                else {
                    return builtIns.getExtensionFunction(parameterCount).getName().getName();
                }
            }

            @Override
            public String visitNullableType(JetNullableType nullableType, Void data) {
                return nullableType.getInnerType().accept(this, null);
            }
        }, null);
    }

    private static boolean typesHaveSameShortName(@NotNull JetTypeReference a, @NotNull JetTypeReference b) {
        return getTypeShortName(a).equals(getTypeShortName(b));
    }

    @Nullable
    private static <Decl extends JetNamedDeclaration> Decl getUnambiguousCandidate(@NotNull Collection<Decl> candidates) {
        return candidates.isEmpty() ? null : candidates.iterator().next();
    }

    @Nullable
    private static JetNamedDeclaration findSpecialProperty(@NotNull Name memberName, @NotNull JetClass containingClass) {
        // property constructor parameters
        List<JetParameter> constructorParameters = containingClass.getPrimaryConstructorParameters();
        for (JetParameter constructorParameter : constructorParameters) {
            if (memberName.equals(constructorParameter.getNameAsName()) && constructorParameter.getValOrVarNode() != null) {
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
    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> JetNamedDeclaration
            getSourcePropertyOrFunction(
            @NotNull final Decl decompiledDeclaration,
            @NotNull final NavigationStrategy<Decl, Descr> navigationStrategy
    ) {
        String memberNameAsString = decompiledDeclaration.getName();
        assert memberNameAsString != null;
        Name memberName = Name.identifier(memberNameAsString);

        PsiElement decompiledContainer = decompiledDeclaration.getParent();

        Collection<Decl> candidates;
        if (decompiledContainer instanceof JetFile) {
            candidates = getInitialCandidates(decompiledDeclaration, navigationStrategy);
        }
        else if (decompiledContainer instanceof JetClassBody) {
            JetClassOrObject decompiledClassOrObject = (JetClassOrObject) decompiledContainer.getParent();
            JetClassOrObject sourceClassOrObject = getSourceClassOrObject(decompiledClassOrObject);

            candidates = sourceClassOrObject == null
                         ? Collections.<Decl>emptyList()
                         : getInitialCandidates(sourceClassOrObject, memberName, navigationStrategy.getDeclarationClass());

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

        candidates = filterByReceiverPresenceAndParametersCount(decompiledDeclaration, navigationStrategy, candidates);

        if (candidates.size() <= 1) {
            return getUnambiguousCandidate(candidates);
        }

        if (!haveRenamesInImports(getContainingFiles(candidates))) {
            candidates = filterByReceiverAndParameterTypes(decompiledDeclaration, navigationStrategy, candidates);

            if (candidates.size() <= 1) {
                return getUnambiguousCandidate(candidates);
            }
        }

        Pair<BindingContext, JetScope> contextAndMemberScope = getBindingContextAndMemberScopeForLibrarySources(decompiledDeclaration);
        if (contextAndMemberScope == null) return null;
        BindingContext bindingContext = contextAndMemberScope.first;
        JetScope memberScope = contextAndMemberScope.second;

        JetTypeReference receiverType = navigationStrategy.getReceiverType(decompiledDeclaration);
        DeclarationDescriptor expectedContainer = memberScope.getContainingDeclaration();
        for (Descr candidate : navigationStrategy.getCandidateDescriptors(contextAndMemberScope.second, memberName)) {
            if (candidate.getContainingDeclaration() == expectedContainer
                && receiversMatch(receiverType, candidate.getReceiverParameter())
                && navigationStrategy.declarationAndDescriptorMatch(decompiledDeclaration, candidate)) {
                return (JetNamedDeclaration) BindingContextUtils.descriptorToDeclaration(bindingContext, candidate);
            }
        }

        return null;
    }

    @Nullable
    private static JetClassOrObject getSourceForNamedClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        FqName classFqName = JetPsiUtil.getFQName((JetNamedDeclaration) decompiledClassOrObject);
        assert classFqName != null;

        GlobalSearchScope librarySourcesScope = createLibrarySourcesScope((JetNamedDeclaration) decompiledClassOrObject);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return null;
        }
        Collection<JetClassOrObject> classes = JetFullClassNameIndex.getInstance()
                .get(classFqName.getFqName(), decompiledClassOrObject.getProject(), librarySourcesScope);
        if (classes.isEmpty()) {
            return null;
        }
        return classes.iterator().next(); // if there are more than one class with this FQ, find first of them
    }

    @NotNull
    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> Collection<Decl> getInitialCandidates(
            @NotNull Decl decompiledDeclaration,
            @NotNull NavigationStrategy<Decl, Descr> navigationStrategy
    ) {
        FqName memberFqName = JetPsiUtil.getFQName(decompiledDeclaration);
        assert memberFqName != null;

        GlobalSearchScope librarySourcesScope = createLibrarySourcesScope(decompiledDeclaration);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return Collections.emptyList();
        }
        StringStubIndexExtension<Decl> index = navigationStrategy.getIndexForTopLevelMembers();
        return index.get(memberFqName.getFqName(), decompiledDeclaration.getProject(), librarySourcesScope);
    }

    @NotNull
    private static <Decl extends JetNamedDeclaration> List<Decl> getInitialCandidates(
            @NotNull JetClassOrObject sourceClassOrObject,
            @NotNull final Name name,
            @NotNull Class<Decl> declarationClass
    ) {
        List<Decl> allByClass = ContainerUtil.findAll(sourceClassOrObject.getDeclarations(), declarationClass);
        return ContainerUtil.filter(allByClass, new Condition<Decl>() {
            @Override
            public boolean value(Decl declaration) {
                return name.equals(declaration.getNameAsSafeName());
            }
        });
    }

    @NotNull
    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> List<Decl> filterByReceiverPresenceAndParametersCount(
            final @NotNull Decl decompiledDeclaration,
            final @NotNull NavigationStrategy<Decl, Descr> navigationStrategy,
            @NotNull Collection<Decl> candidates
    ) {
        final JetTypeReference decompiledReceiver = navigationStrategy.getReceiverType(decompiledDeclaration);

        return ContainerUtil.filter(candidates, new Condition<Decl>() {
            @Override
            public boolean value(Decl candidate) {
                boolean sameReceiverPresence = (navigationStrategy.getReceiverType(candidate) != null) ==
                                               (decompiledReceiver != null);
                boolean sameParameterCount = navigationStrategy.declarationsMatchByParameterCount(candidate, decompiledDeclaration);
                return sameReceiverPresence && sameParameterCount;
            }
        });
    }

    @NotNull
    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> List<Decl> filterByReceiverAndParameterTypes(
            final @NotNull Decl decompiledDeclaration,
            final @NotNull NavigationStrategy<Decl, Descr> navigationStrategy,
            @NotNull Collection<Decl> candidates
    ) {
        final JetTypeReference decompiledReceiver = navigationStrategy.getReceiverType(decompiledDeclaration);

        return ContainerUtil.filter(candidates, new Condition<Decl>() {
            @Override
            public boolean value(Decl candidate) {
                if (decompiledReceiver != null) {
                    JetTypeReference candidateReceiver = navigationStrategy.getReceiverType(candidate);
                    assert candidateReceiver != null;
                    if (!typesHaveSameShortName(decompiledReceiver, candidateReceiver)) {
                        return false;
                    }
                }

                return navigationStrategy.declarationsMatchByParameterTypes(candidate, decompiledDeclaration);
            }
        });
    }

    private static boolean receiversMatch(
            @Nullable JetTypeReference receiverTypeRef,
            @Nullable ReceiverParameterDescriptor receiverParameter
    ) {
        if (receiverTypeRef == null && receiverParameter == null) {
            return true;
        }
        if (receiverTypeRef != null && receiverParameter != null) {
            return receiverTypeRef.getText().equals(DescriptorRenderer.TEXT.renderType(receiverParameter.getType()));
        }
        return false;
    }

    @Nullable
    public static JetNamedDeclaration getSourceProperty(final @NotNull JetProperty decompiledProperty) {
        return getSourcePropertyOrFunction(decompiledProperty, new PropertyNavigationStrategy());
    }

    @Nullable
    public static JetNamedDeclaration getSourceFunction(final @NotNull JetNamedFunction decompiledFunction) {
        return getSourcePropertyOrFunction(decompiledFunction, new FunctionNavigationStrategy());
    }

    private interface NavigationStrategy<Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> {
        @NotNull
        Class<Decl> getDeclarationClass();

        boolean declarationsMatchByParameterCount(@NotNull Decl a, @NotNull Decl b);

        boolean declarationsMatchByParameterTypes(@NotNull Decl a, @NotNull Decl b);

        boolean declarationAndDescriptorMatch(@NotNull Decl declaration, @NotNull Descr descriptor);

        @NotNull Collection<Descr> getCandidateDescriptors(@NotNull JetScope scope, @NotNull Name name);

        @Nullable JetTypeReference getReceiverType(@NotNull Decl declaration);

        @NotNull
        StringStubIndexExtension<Decl> getIndexForTopLevelMembers();
    }

    private static class FunctionNavigationStrategy implements NavigationStrategy<JetNamedFunction, FunctionDescriptor> {
        @NotNull
        @Override
        public Class<JetNamedFunction> getDeclarationClass() {
            return JetNamedFunction.class;
        }

        @Override
        public boolean declarationsMatchByParameterCount(@NotNull JetNamedFunction a, @NotNull JetNamedFunction b) {
            return a.getValueParameters().size() == b.getValueParameters().size();
        }

        @Override
        public boolean declarationsMatchByParameterTypes(@NotNull JetNamedFunction a, @NotNull JetNamedFunction b) {
            List<JetParameter> aParameters = a.getValueParameters();
            List<JetParameter> bParameters = b.getValueParameters();
            if (aParameters.size() != bParameters.size()) {
                return false;
            }
            for (int i = 0; i < aParameters.size(); i++) {
                JetTypeReference aType = aParameters.get(i).getTypeReference();
                JetTypeReference bType = bParameters.get(i).getTypeReference();

                assert aType != null;
                assert bType != null;

                if (!typesHaveSameShortName(aType, bType)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean declarationAndDescriptorMatch(@NotNull JetNamedFunction declaration, @NotNull FunctionDescriptor descriptor) {
            List<JetParameter> declarationParameters = declaration.getValueParameters();
            List<ValueParameterDescriptor> descriptorParameters = descriptor.getValueParameters();
            if (descriptorParameters.size() != declarationParameters.size()) {
                return false;
            }

            for (int i = 0; i < descriptorParameters.size(); i++) {
                ValueParameterDescriptor descriptorParameter = descriptorParameters.get(i);
                JetParameter declarationParameter = declarationParameters.get(i);
                JetTypeReference typeReference = declarationParameter.getTypeReference();
                if (typeReference == null) {
                    return false;
                }
                JetModifierList modifierList = declarationParameter.getModifierList();
                boolean vararg = modifierList != null && modifierList.hasModifier(JetTokens.VARARG_KEYWORD);
                if (vararg != (descriptorParameter.getVarargElementType() != null)) {
                    return false;
                }
                String declarationTypeText = typeReference.getText();
                String descriptorParameterText = DescriptorRenderer.TEXT.renderType(vararg
                                                                                    ? descriptorParameter.getVarargElementType()
                                                                                    : descriptorParameter.getType());
                if (!declarationTypeText.equals(descriptorParameterText)) {
                    return false;
                }
            }
            return true;
        }

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getCandidateDescriptors(@NotNull JetScope scope, @NotNull Name name) {
            return scope.getFunctions(name);
        }

        @Nullable
        @Override
        public JetTypeReference getReceiverType(@NotNull JetNamedFunction declaration) {
            return declaration.getReceiverTypeRef();
        }

        @NotNull
        @Override
        public StringStubIndexExtension<JetNamedFunction> getIndexForTopLevelMembers() {
            return JetTopLevelFunctionsFqnNameIndex.getInstance();
        }
    }

    private static class PropertyNavigationStrategy implements NavigationStrategy<JetProperty, VariableDescriptor> {
        @NotNull
        @Override
        public Class<JetProperty> getDeclarationClass() {
            return JetProperty.class;
        }

        @Override
        public boolean declarationsMatchByParameterCount(@NotNull JetProperty a, @NotNull JetProperty b) {
            return true;
        }

        @Override
        public boolean declarationsMatchByParameterTypes(@NotNull JetProperty a, @NotNull JetProperty b) {
            return true;
        }

        @Override
        public boolean declarationAndDescriptorMatch(@NotNull JetProperty declaration, @NotNull VariableDescriptor descriptor) {
            return true;
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getCandidateDescriptors(@NotNull JetScope scope, @NotNull Name name) {
            return scope.getProperties(name);
        }

        @Nullable
        @Override
        public JetTypeReference getReceiverType(@NotNull JetProperty declaration) {
            return declaration.getReceiverTypeRef();
        }

        @NotNull
        @Override
        public StringStubIndexExtension<JetProperty> getIndexForTopLevelMembers() {
            return JetTopLevelPropertiesFqnNameIndex.getInstance();
        }
    }
}
