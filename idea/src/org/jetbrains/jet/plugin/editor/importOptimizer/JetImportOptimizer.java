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
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                JetFile jetFile = (JetFile) file;
                Set<String> usedQualifiedNames = extractUsedQualifiedNames(jetFile);

            }
        };

    }

    public static Set<String> extractUsedQualifiedNames(JetFile jetFile) {

        final Set<String> usedQualifiedNames = new HashSet<String>();
        jetFile.accept(new JetVisitorVoid() {
            @Override
            public void visitElement(PsiElement element) {
                ProgressIndicatorProvider.checkCanceled();
                element.acceptChildren(this);
            }

            @Override
            public void visitReferenceExpression(JetReferenceExpression expression) {
                if (PsiTreeUtil.getParentOfType(expression, JetQualifiedExpression.class) == null) {
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
                            String fqName = getElementFQName(psiReference);
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
    public static String getElementFQName(PsiElement element) {
        if (element instanceof JetClassOrObject) {
            return JetPsiUtil.getFQName((JetClassOrObject) element);
        }
        if (element instanceof JetNamedFunction) {
            return JetPsiUtil.getFQName((JetNamedFunction) element);
        }
        if (element instanceof PsiClass) {
            return ((PsiClass) element).getQualifiedName();
        }
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;

            PsiClass containingClass = method.getContainingClass();

            if (containingClass != null) {
                String classFQN = containingClass.getQualifiedName();

                if (classFQN != null) {
                    if (QualifiedNamesUtil.fqnToShortName(classFQN).equals(JvmAbi.PACKAGE_CLASS)) {
                        String classParentFQN = QualifiedNamesUtil.withoutLastSegment(classFQN);
                        return QualifiedNamesUtil.combine(classParentFQN, method.getName());
                    }
                    else {
                        return QualifiedNamesUtil.combine(containingClass.getQualifiedName(), method.getName());
                    }
                }
            }
        }

        return null;
    }


}
