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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Processor;
import jet.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

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
            getBindingContextAndClassOrNamespaceDescriptor(@NotNull WritableSlice<FqName, D> slice,
                                                           @NotNull PsiElement psiElement,
                                                           @Nullable FqName fqName) {
        if (fqName == null) {
            return null;
        }
        for (VirtualFile sourceDir : getAllSourceDirs(psiElement)) {
            BindingContext bindingContext = analyzeLibrary(sourceDir, psiElement.getProject());
            D descriptor = bindingContext.get(slice, fqName);
            if (descriptor != null) {
                return new Tuple2<BindingContext, D>(bindingContext, descriptor);
            }
        }
        return null;
    }

    @Nullable
    private static Tuple2<BindingContext, ClassDescriptor> getBindingContextAndClassDescriptor(@NotNull JetClass decompiledClass) {
        return getBindingContextAndClassOrNamespaceDescriptor(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, decompiledClass, JetPsiUtil.getFQName(decompiledClass));
    }

    @Nullable
    private static Tuple2<BindingContext, NamespaceDescriptor> getBindingContextAndNamespaceDescriptor(@NotNull JetFile decompiledNamespaceFile) {
        return getBindingContextAndClassOrNamespaceDescriptor(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, decompiledNamespaceFile, JetPsiUtil.getFQName(decompiledNamespaceFile));
    }

    @Nullable
    public static JetClass getSourceClass(@NotNull JetClass decompiledClass) {
        Tuple2<BindingContext, ClassDescriptor> bindingContextAndClassDescriptor = getBindingContextAndClassDescriptor(decompiledClass);
        if (bindingContextAndClassDescriptor == null) return null;
        PsiElement declaration = bindingContextAndClassDescriptor._1.get(BindingContext.DESCRIPTOR_TO_DECLARATION, bindingContextAndClassDescriptor._2);
        assert declaration instanceof JetClass;
        return (JetClass) declaration;
    }

    private static BindingContext analyzeLibrary(VirtualFile sourceDir, final Project project) {
        final List<JetFile> libraryFiles = new ArrayList<JetFile>();
        VfsUtil.processFilesRecursively(sourceDir, new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {
                if (virtualFile.getFileType() == JetFileType.INSTANCE) {
                    libraryFiles.add((JetFile) PsiManager.getInstance(project).findFile(virtualFile));
                }
                return true;
            }
        });
        return AnalyzingUtils.analyzeFiles(project,
                                           ModuleConfiguration.EMPTY,
                                           libraryFiles,
                                           Predicates.<PsiFile>alwaysTrue(),
                                           JetControlFlowDataTraceFactory.EMPTY);
    }

    @NotNull
    private static List<VirtualFile> getAllSourceDirs(@NotNull PsiElement psiElement) {
        List<VirtualFile> allSourceDirs = new ArrayList<VirtualFile>();

        VirtualFile decompiledFile = psiElement.getContainingFile().getVirtualFile();
        if (decompiledFile != null) {
            ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(psiElement.getProject());
            List<OrderEntry> orderEntries = projectFileIndex.getOrderEntriesForFile(decompiledFile);
            for (OrderEntry orderEntry : orderEntries) {
                Collections.addAll(allSourceDirs, orderEntry.getFiles(OrderRootType.SOURCES));
            }
        }

        return allSourceDirs;
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
            Tuple2<BindingContext, NamespaceDescriptor> bindingContextAndNamespaceDescriptor = getBindingContextAndNamespaceDescriptor((JetFile) declarationContainer);
            if (bindingContextAndNamespaceDescriptor == null) return null;
            BindingContext bindingContext = bindingContextAndNamespaceDescriptor._1;
            NamespaceDescriptor namespaceDescriptor = bindingContextAndNamespaceDescriptor._2;
            if (receiverType == null) {
                // non-extension property
                for (Descr candidate : matcher.getCandidatesFromScope(namespaceDescriptor.getMemberScope(), entityName)) {
                    if (candidate.getReceiverParameter() == ReceiverDescriptor.NO_RECEIVER) {
                        if (matcher.areSame(decompiledDeclaration, candidate)) {
                            return (JetDeclaration) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, candidate);
                        }
                    }
                }
            } else {
                // extension property
                String expectedTypeString = receiverType.getText();
                for (Descr candidate : matcher.getCandidatesFromScope(namespaceDescriptor.getMemberScope(), entityName)) {
                    if (candidate.getReceiverParameter() != ReceiverDescriptor.NO_RECEIVER) {
                        String thisReceiverType = DescriptorRenderer.TEXT.renderType(candidate.getReceiverParameter().getType());
                        if (expectedTypeString.equals(thisReceiverType)) {
                            if (matcher.areSame(decompiledDeclaration, candidate)) {
                                return (JetDeclaration) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, candidate);
                            }
                        }
                    }
                }
            }
        }
        else if (declarationContainer instanceof JetClassBody) {
            Tuple2<BindingContext, ClassDescriptor> bindingContextAndClassDescriptor = getBindingContextAndClassDescriptor((JetClass) declarationContainer.getParent());
            if (bindingContextAndClassDescriptor != null) {
                BindingContext bindingContext = bindingContextAndClassDescriptor._1;
                ClassDescriptor classDescriptor = bindingContextAndClassDescriptor._2;
                for (Descr candidate : matcher.getCandidatesFromScope(classDescriptor.getDefaultType().getMemberScope(), entityName)) {
                    if (candidate.getContainingDeclaration() == classDescriptor) {
                        JetDeclaration property = (JetDeclaration) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, candidate);
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
        return getSourcePropertyOrFunction(decompiledProperty, decompiledProperty.getReceiverTypeRef(), new Matcher<JetProperty, VariableDescriptor>() {
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

    private interface Matcher<Decl extends JetDeclaration, Descr extends CallableDescriptor> {
        boolean areSame(Decl declaration, Descr descriptor);

        Set<Descr> getCandidatesFromScope(JetScope scope, String name);
    }
}
