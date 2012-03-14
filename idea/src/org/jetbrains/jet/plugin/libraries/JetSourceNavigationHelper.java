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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Evgeny Gerashchenko
 * @since 3/13/12
 */
public class JetSourceNavigationHelper {
    private JetSourceNavigationHelper() {
    }

    @Nullable
    private static Tuple2<BindingContext, ClassDescriptor> getBindingContextAndClassDescriptor(@NotNull JetClass decompiledClass) {
        for (VirtualFile sourceDir : getAllSourceDirs(decompiledClass)) {
            BindingContext bindingContext = analyzeLibrary(sourceDir, decompiledClass.getProject());
            ClassDescriptor cd = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, JetPsiUtil.getFQName(decompiledClass));
            if (cd != null) {
                return new Tuple2<BindingContext, ClassDescriptor>(bindingContext, cd);
            }
        }
        return null;
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
    private static List<VirtualFile> getAllSourceDirs(@NotNull JetClass decompiledClass) {
        List<VirtualFile> allSourceDirs = new ArrayList<VirtualFile>();

        VirtualFile decompiledFile = decompiledClass.getContainingFile().getVirtualFile();
        if (decompiledFile != null) {
            ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(decompiledClass.getProject());
            List<OrderEntry> orderEntries = projectFileIndex.getOrderEntriesForFile(decompiledFile);
            for (OrderEntry orderEntry : orderEntries) {
                Collections.addAll(allSourceDirs, orderEntry.getFiles(OrderRootType.SOURCES));
            }
        }

        return allSourceDirs;
    }

    @Nullable
    public static JetDeclaration getSourceProperty(final @NotNull JetProperty decompiledProperty) {
        String propertyName = decompiledProperty.getName();
        if (propertyName == null) {
            return null;
        }

        PsiElement propertyContainer = decompiledProperty.getParent();
        if (propertyContainer instanceof JetFile) {
            // TODO global property, may be extension
            return null;
        }
        else if (propertyContainer instanceof JetClassBody) {
            Tuple2<BindingContext, ClassDescriptor> bindingContextAndClassDescriptor = getBindingContextAndClassDescriptor((JetClass) propertyContainer.getParent());
            if (bindingContextAndClassDescriptor != null) {
                BindingContext bindingContext = bindingContextAndClassDescriptor._1;
                ClassDescriptor classDescriptor = bindingContextAndClassDescriptor._2;
                for (VariableDescriptor vd : classDescriptor.getDefaultType().getMemberScope().getProperties(propertyName)) {
                    if (vd.getContainingDeclaration() == classDescriptor) {
                        JetDeclaration property = (JetDeclaration) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, vd);
                        if (property != null) {
                            return property;
                        }
                    }
                }
            }
        }
        return null;
    }
}
