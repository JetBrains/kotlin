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
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.util.PsiTreeUtil;
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
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JetSourceNavigationHelper {
    private JetSourceNavigationHelper() {
    }

    @Nullable
    private static<D extends ClassOrNamespaceDescriptor> Pair<BindingContext, D>
            getBindingContextAndClassOrNamespaceDescriptor(@NotNull ReadOnlySlice<FqName, D> slice,
                                                           @NotNull JetDeclaration declaration,
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
        return getBindingContextAndClassOrNamespaceDescriptor(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, decompiledClassOrObject,
                                                              JetPsiUtil.getFQName((JetNamedDeclaration) decompiledClassOrObject));
    }

    @Nullable
    private static Pair<BindingContext, NamespaceDescriptor> getBindingContextAndNamespaceDescriptor(
            @NotNull JetDeclaration declaration) {
        JetFile file = (JetFile) declaration.getContainingFile();
        return getBindingContextAndClassOrNamespaceDescriptor(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, declaration,
                                                              JetPsiUtil.getFQName(file));
    }

    @Nullable
    public static JetClassOrObject getSourceClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        Pair<BindingContext, ClassDescriptor> bindingContextAndClassDescriptor = getBindingContextAndClassDescriptor(
                decompiledClassOrObject);
        if (bindingContextAndClassDescriptor == null) return null;
        PsiElement declaration = BindingContextUtils.classDescriptorToDeclaration(
                bindingContextAndClassDescriptor.first, bindingContextAndClassDescriptor.second);
        if (declaration == null) {
            throw new IllegalStateException("class not found by " + bindingContextAndClassDescriptor.second);
        }
        return (JetClassOrObject) declaration;
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

    private static List<JetFile> findAllSourceFilesWhichContainIdentifier(@NotNull JetDeclaration jetDeclaration) {
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

    private static <Decl extends JetDeclaration> Pair<BindingContext, JetScope> getBindingContextAndMemberScopeForLibrarySources(
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

    @Nullable
    private static <Decl extends JetDeclaration, Descr extends CallableDescriptor> JetDeclaration
            getSourcePropertyOrFunction(
            final @NotNull Decl decompiledDeclaration,
            NavigationStrategy<Decl, Descr> navigationStrategy
    ) {
        String memberNameAsString = decompiledDeclaration.getName();
        assert memberNameAsString != null;
        Name memberName = Name.identifier(memberNameAsString);

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
                return (JetDeclaration) BindingContextUtils.descriptorToDeclaration(bindingContext, candidate);
            }
        }

        return null;
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
    public static JetDeclaration getSourceProperty(final @NotNull JetProperty decompiledProperty) {
        return getSourcePropertyOrFunction(decompiledProperty, new PropertyNavigationStrategy());
    }

    @Nullable
    public static JetDeclaration getSourceFunction(final @NotNull JetFunction decompiledFunction) {
        return getSourcePropertyOrFunction(decompiledFunction, new FunctionNavigationStrategy());
    }

    private interface NavigationStrategy<Decl extends JetDeclaration, Descr extends CallableDescriptor> {
        boolean declarationAndDescriptorMatch(Decl declaration, Descr descriptor);

        Collection<Descr> getCandidateDescriptors(JetScope scope, Name name);

        @Nullable JetTypeReference getReceiverType(@NotNull Decl declaration);
    }

    private static class FunctionNavigationStrategy implements NavigationStrategy<JetFunction, FunctionDescriptor> {
        @Override
        public boolean declarationAndDescriptorMatch(JetFunction declaration, FunctionDescriptor descriptor) {
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

        @Override
        public Collection<FunctionDescriptor> getCandidateDescriptors(JetScope scope, Name name) {
            return scope.getFunctions(name);
        }

        @Nullable
        @Override
        public JetTypeReference getReceiverType(@NotNull JetFunction declaration) {
            return declaration.getReceiverTypeRef();
        }
    }

    private static class PropertyNavigationStrategy implements NavigationStrategy<JetProperty, VariableDescriptor> {
        @Override
        public boolean declarationAndDescriptorMatch(JetProperty declaration, VariableDescriptor descriptor) {
            return true;
        }

        @Override
        public Collection<VariableDescriptor> getCandidateDescriptors(JetScope scope, Name name) {
            return scope.getProperties(name);
        }

        @Nullable
        @Override
        public JetTypeReference getReceiverType(@NotNull JetProperty declaration) {
            return declaration.getReceiverTypeRef();
        }
    }
}
