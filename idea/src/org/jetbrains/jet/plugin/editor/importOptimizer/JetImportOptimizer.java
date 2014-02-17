/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.editor.importOptimizer;

import com.google.common.collect.Lists;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightPackage;
import org.jetbrains.jet.plugin.references.JetReference;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.plugin.quickfix.ImportInsertHelper.needImport;

public class JetImportOptimizer implements ImportOptimizer {
    @Override
    public boolean supports(PsiFile file) {
        return file instanceof JetFile;
    }

    @NotNull
    @Override
    public Runnable processFile(final PsiFile file) {
        return new Runnable() {

            @Override
            public void run() {
                final JetFile jetFile = (JetFile) file;
                final Set<FqName> usedQualifiedNames = extractUsedQualifiedNames(jetFile);

                final List<JetImportDirective> directives = jetFile.getImportDirectives();

                final List<JetImportDirective> directivesBeforeCurrent = Lists.newArrayList();
                final List<JetImportDirective> directivesAfterCurrent = jetFile.getImportDirectives();

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        // Remove only unnecessary imports
                        for (JetImportDirective anImport : directives) {
                            directivesAfterCurrent.remove(anImport);

                            ImportPath importPath = JetPsiUtil.getImportPath(anImport);
                            if (importPath == null) {
                                continue;
                            }

                            if (isUseful(importPath, usedQualifiedNames) &&
                                    needImport(importPath, jetFile, directivesBeforeCurrent) &&
                                    needImport(importPath, jetFile, directivesAfterCurrent)) {
                                directivesBeforeCurrent.add(anImport);
                            }
                            else {
                               anImport.delete();
                            }
                        }
                    }
                });
            }
        };
    }

    public static boolean isUseful(ImportPath importPath, Collection<FqName> usedNames) {
        if (importPath.hasAlias()) {
            // TODO: Add better analysis for aliases
            return true;
        }

        for (FqName usedName : usedNames) {
            if (QualifiedNamesUtil.isImported(importPath, usedName)) {
                return true;
            }
        }

        return false;
    }

    public static Set<FqName> extractUsedQualifiedNames(JetFile jetFile) {
        final Set<FqName> usedQualifiedNames = new HashSet<FqName>();
        jetFile.accept(new JetVisitorVoid() {
            @Override
            public void visitElement(PsiElement element) {
                ProgressIndicatorProvider.checkCanceled();
                element.acceptChildren(this);
            }

            @Override
            public void visitUserType(@NotNull JetUserType type) {
                if (type.getQualifier() == null) {
                    super.visitUserType(type);
                }
                else {
                    JetTypeArgumentList argumentList = type.getTypeArgumentList();
                    if (argumentList != null) {
                        super.visitTypeArgumentList(argumentList);
                    }
                    visitUserType(type.getQualifier());
                }
            }

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                if (PsiTreeUtil.getParentOfType(element, JetImportDirective.class) != null ||
                    PsiTreeUtil.getParentOfType(element, JetPackageDirective.class) != null) {
                    return;
                }
                PsiReference reference = element.getReference();
                if (reference instanceof JetReference) {
                    Collection<DeclarationDescriptor> referencedDescriptors = ((JetReference) reference).resolveToDescriptors();
                    for (DeclarationDescriptor descriptor : referencedDescriptors) {
                        FqName importableFqName = CodeInsightPackage.getImportableFqName(descriptor);
                        if (importableFqName != null) {
                            usedQualifiedNames.add(importableFqName);
                        }
                    }
                }
                super.visitJetElement(element);
            }
        });

        return usedQualifiedNames;
    }
}
