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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.util.PsiTreeUtil;
import jet.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeProvider;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Evgeny Gerashchenko
 * @since 3/13/12
 */
public class JetSourceNavigationHelper {
    private JetSourceNavigationHelper() {
    }

    @Nullable
    private static<D extends ClassOrNamespaceDescriptor> Tuple2<BindingContext, D>
            getBindingContextAndClassOrNamespaceDescriptor(@NotNull ReadOnlySlice<FqName, D> slice,
                                                           @NotNull JetDeclaration declaration,
                                                           @Nullable FqName fqName) {
        if (fqName == null || DumbService.isDumb(declaration.getProject())) {
            return null;
        }
        final Project project = declaration.getProject();
        final List<JetFile> libraryFiles = findAllSourceFilesWhichContainIdentifier(declaration);
        BindingContext bindingContext = AnalyzerFacadeProvider.getAnalyzerFacadeForProject(project).analyzeFiles(
                project,
                libraryFiles,
                Predicates.<PsiFile>alwaysTrue(),
                JetControlFlowDataTraceFactory.EMPTY).getBindingContext();
        D descriptor = bindingContext.get(slice, fqName);
        if (descriptor != null) {
            return new Tuple2<BindingContext, D>(bindingContext, descriptor);
        }
        return null;
    }

    @Nullable
    private static Tuple2<BindingContext, ClassDescriptor> getBindingContextAndClassDescriptor(@NotNull JetClass decompiledClass) {
        return getBindingContextAndClassOrNamespaceDescriptor(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, decompiledClass,
                                                              JetPsiUtil.getFQName(decompiledClass));
    }

    @Nullable
    private static Tuple2<BindingContext, NamespaceDescriptor> getBindingContextAndNamespaceDescriptor(
            @NotNull JetDeclaration declaration) {
        JetFile file = (JetFile) declaration.getContainingFile();
        return getBindingContextAndClassOrNamespaceDescriptor(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, declaration,
                                                              JetPsiUtil.getFQName(file));
    }

    @Nullable
    public static JetClassOrObject getSourceClass(@NotNull JetClass decompiledClass) {
        Tuple2<BindingContext, ClassDescriptor> bindingContextAndClassDescriptor = getBindingContextAndClassDescriptor(decompiledClass);
        if (bindingContextAndClassDescriptor == null) return null;
        PsiElement declaration = BindingContextUtils.classDescriptorToDeclaration(
                bindingContextAndClassDescriptor._1, bindingContextAndClassDescriptor._2);
        if (declaration == null) {
            throw new IllegalStateException("class not found by " + bindingContextAndClassDescriptor._2);
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

    @Nullable
    private static <Decl extends JetDeclaration, Descr extends CallableDescriptor> JetDeclaration
            getSourcePropertyOrFunction(final @NotNull Decl decompiledDeclaration,
                                        JetTypeReference receiverType,
                                        Matcher<Decl, Descr> matcher) {
        String entityName = decompiledDeclaration.getName();
        if (entityName == null) {
            return null;
        }

        PsiElement declarationContainer = decompiledDeclaration.getParent();
        if (declarationContainer instanceof JetFile) {
            Tuple2<BindingContext, NamespaceDescriptor> bindingContextAndNamespaceDescriptor =
                    getBindingContextAndNamespaceDescriptor(decompiledDeclaration);
            if (bindingContextAndNamespaceDescriptor == null) return null;
            BindingContext bindingContext = bindingContextAndNamespaceDescriptor._1;
            NamespaceDescriptor namespaceDescriptor = bindingContextAndNamespaceDescriptor._2;
            if (receiverType == null) {
                // non-extension property
                for (Descr candidate : matcher.getCandidatesFromScope(namespaceDescriptor.getMemberScope(), entityName)) {
                    if (candidate.getReceiverParameter() == ReceiverDescriptor.NO_RECEIVER) {
                        if (matcher.areSame(decompiledDeclaration, candidate)) {
                            return (JetDeclaration) BindingContextUtils.descriptorToDeclaration(bindingContext, candidate);
                        }
                    }
                }
            }
            else {
                // extension property
                String expectedTypeString = receiverType.getText();
                for (Descr candidate : matcher.getCandidatesFromScope(namespaceDescriptor.getMemberScope(), entityName)) {
                    if (candidate.getReceiverParameter() != ReceiverDescriptor.NO_RECEIVER) {
                        String thisReceiverType = DescriptorRenderer.TEXT.renderType(candidate.getReceiverParameter().getType());
                        if (expectedTypeString.equals(thisReceiverType)) {
                            if (matcher.areSame(decompiledDeclaration, candidate)) {
                                return (JetDeclaration) BindingContextUtils.descriptorToDeclaration(bindingContext, candidate);
                            }
                        }
                    }
                }
            }
        }
        else if (declarationContainer instanceof JetClassBody) {
            JetClassOrObject parent = (JetClassOrObject)declarationContainer.getParent();
            boolean isClassObject = parent instanceof JetObjectDeclaration;
            JetClass jetClass =
                isClassObject ? PsiTreeUtil.getParentOfType(parent, JetClass.class) : (JetClass)parent;
            if (jetClass == null) {
                return null;
            }
            Tuple2<BindingContext, ClassDescriptor> bindingContextAndClassDescriptor = getBindingContextAndClassDescriptor(jetClass);
            if (bindingContextAndClassDescriptor != null) {
                BindingContext bindingContext = bindingContextAndClassDescriptor._1;
                ClassDescriptor classDescriptor = bindingContextAndClassDescriptor._2;
                JetScope memberScope = classDescriptor.getDefaultType().getMemberScope();
                if (isClassObject) {
                    JetType classObjectType = classDescriptor.getClassObjectType();
                    if (classObjectType == null) {
                        return null;
                    }
                    memberScope = classObjectType.getMemberScope();
                }

                ClassDescriptor expectedContainer = isClassObject ? classDescriptor.getClassObjectDescriptor() : classDescriptor;
                for (Descr candidate : matcher.getCandidatesFromScope(memberScope, entityName)) {
                    if (candidate.getContainingDeclaration() == expectedContainer) {
                        JetDeclaration property = (JetDeclaration) BindingContextUtils.descriptorToDeclaration(bindingContext, candidate);
                        if (property != null) {
                            return property;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static JetDeclaration getSourceProperty(final @NotNull JetProperty decompiledProperty) {
        return getSourcePropertyOrFunction(decompiledProperty, decompiledProperty.getReceiverTypeRef(),
                                           new Matcher<JetProperty, VariableDescriptor>() {
            @Override
            public boolean areSame(JetProperty declaration, VariableDescriptor descriptor) {
                return true;
            }

            @Override
            public Set<VariableDescriptor> getCandidatesFromScope(JetScope scope, String name) {
                return scope.getProperties(name);
            }
        });
    }

    @Nullable
    public static JetDeclaration getSourceFunction(final @NotNull JetFunction decompiledFunction) {
        return getSourcePropertyOrFunction(decompiledFunction, decompiledFunction.getReceiverTypeRef(),
                                           new Matcher<JetFunction, FunctionDescriptor>() {
            @Override
            public boolean areSame(JetFunction declaration, FunctionDescriptor descriptor) {
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
            public Set<FunctionDescriptor> getCandidatesFromScope(JetScope scope, String name) {
                return scope.getFunctions(name);
            }
        });
    }

    private interface Matcher<Decl extends JetDeclaration, Descr extends CallableDescriptor> {
        boolean areSame(Decl declaration, Descr descriptor);

        Set<Descr> getCandidatesFromScope(JetScope scope, String name);
    }
}
