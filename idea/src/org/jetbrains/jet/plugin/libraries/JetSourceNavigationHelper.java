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
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Processor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Evgeny Gerashchenko
 * @since 3/13/12
 */
public class JetSourceNavigationHelper {
    private JetSourceNavigationHelper() {
    }

    @Nullable
    public static JetClass getSourceClass(final JetClass decompiledClass) {
        VirtualFile decompiledFile = decompiledClass.getContainingFile().getVirtualFile();
        if (decompiledFile == null) {
            return null;
        }
        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(decompiledClass.getProject());
        List<OrderEntry> orderEntries = projectFileIndex.getOrderEntriesForFile(decompiledFile);
        for (OrderEntry orderEntry : orderEntries) {
            // TODO this is too slow, needs optimizing
            VirtualFile[] sourceDirs = orderEntry.getFiles(OrderRootType.SOURCES);
            for (VirtualFile sourceDir : sourceDirs) {
                final List<JetFile> libraryFiles = new ArrayList<JetFile>();
                VfsUtil.processFilesRecursively(sourceDir, new Processor<VirtualFile>() {
                    @Override
                    public boolean process(VirtualFile virtualFile) {
                        if (virtualFile.getFileType() == JetFileType.INSTANCE) {
                            libraryFiles.add((JetFile) decompiledClass.getManager().findFile(virtualFile));
                        }
                        return true;
                    }
                });
                BindingContext bindingContext = AnalyzingUtils.analyzeFiles(decompiledClass.getProject(),
                                                                            ModuleConfiguration.EMPTY,
                                                                            libraryFiles,
                                                                            Predicates.<PsiFile>alwaysTrue(),
                                                                            JetControlFlowDataTraceFactory.EMPTY);
                ClassDescriptor cd = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, JetPsiUtil.getFQName(decompiledClass));
                if (cd != null) {
                    PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, cd);
                    assert declaration instanceof JetClass;
                    return (JetClass) declaration;
                }
            }
        }
        return null;
    }
}
