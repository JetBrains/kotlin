/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.*;

/**
 * @author Nikolay Krasko
 */
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

                final List<JetImportDirective> sortedDirectives = jetFile.getImportDirectives();
                Collections.sort(sortedDirectives, new Comparator<JetImportDirective>() {
                    @Override
                    public int compare(JetImportDirective directive1, JetImportDirective directive2) {
                        ImportPath firstPath = JetPsiUtil.getImportPath(directive1);
                        ImportPath secondPath = JetPsiUtil.getImportPath(directive2);

                        if (firstPath == null || secondPath == null) {
                            return firstPath == null && secondPath == null ? 0 :
                                   firstPath == null ? -1 :
                                   1;
                        }

                        // import bla.bla.bla.* should be before import bla.bla.bla.something
                        if (firstPath.isAllUnder() && !secondPath.isAllUnder() && firstPath.fqnPart().equals(secondPath.fqnPart().parent())) {
                            return -1;
                        }

                        if (!firstPath.isAllUnder() && secondPath.isAllUnder() && secondPath.fqnPart().equals(firstPath.fqnPart().parent())) {
                            return 1;
                        }

                        return firstPath.getPathStr().compareTo(secondPath.getPathStr());
                    }
                });

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        // Remove imports
                        List<JetImportDirective> imports = jetFile.getImportDirectives();
                        if (!imports.isEmpty()) {
                            jetFile.deleteChildRange(imports.get(0), imports.get(imports.size() - 1));
                        }

                        // Insert back only necessary imports in correct order
                        for (JetImportDirective anImport : sortedDirectives) {
                            ImportPath importPath = JetPsiUtil.getImportPath(anImport);
                            if (importPath == null) {
                                continue;
                            }

                            if (isUseful(importPath, anImport.getAliasName(), usedQualifiedNames)) {
                                ImportInsertHelper.addImportDirective(importPath, anImport.getAliasName(), jetFile);
                            }
                        }
                    }
                });
            }
        };
    }

    public static boolean isUseful(ImportPath importPath, @Nullable String aliasName, Collection<FqName> usedNames) {
        if (aliasName != null) {
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
            public void visitReferenceExpression(JetReferenceExpression expression) {
                if (PsiTreeUtil.getParentOfType(expression, JetImportDirective.class) == null) {
                    PsiReference reference = expression.getReference();
                    if (reference != null) {
                        List<PsiElement> references = new ArrayList<PsiElement>();
                        PsiElement resolve = reference.resolve();
                        if (resolve != null) {
                            references.add(resolve);
                        }

                        if (references.isEmpty() && reference instanceof PsiPolyVariantReference) {
                            for (ResolveResult resolveResult : ((PsiPolyVariantReference) reference).multiResolve(true)) {
                                references.add(resolveResult.getElement());
                            }
                        }

                        for (PsiElement psiReference : references) {
                            FqName fqName = getElementUsageFQName(psiReference);
                            if (fqName != null) {
                                usedQualifiedNames.add(fqName);
                            }
                        }
                    }

                }

                super.visitReferenceExpression(expression);
            }
        });

        return usedQualifiedNames;
    }


    @Nullable
    public static FqName getElementUsageFQName(PsiElement element) {
        if (element instanceof JetClassOrObject) {
            return JetPsiUtil.getFQName((JetClassOrObject) element);
        }
        if (element instanceof JetNamedFunction) {
            return JetPsiUtil.getFQName((JetNamedFunction) element);
        }
        if (element instanceof PsiClass) {
            String qualifiedName = ((PsiClass) element).getQualifiedName();
            if (qualifiedName != null) {
                return new FqName(qualifiedName);
            }
        }
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;

            PsiClass containingClass = method.getContainingClass();

            if (containingClass != null) {
                String classFQNStr = containingClass.getQualifiedName();
                if (classFQNStr != null) {
                    if (method.isConstructor()) {
                        return new FqName(classFQNStr);
                    }

                    FqName classFQN = new FqName(classFQNStr);
                    if (classFQN.shortName().equals(JvmAbi.PACKAGE_CLASS)) {
                        return QualifiedNamesUtil.combine(classFQN.parent(), method.getName());
                    }
                    else {
                        return QualifiedNamesUtil.combine(classFQN, method.getName());
                    }
                }
            }
        }

        return null;
    }
}
