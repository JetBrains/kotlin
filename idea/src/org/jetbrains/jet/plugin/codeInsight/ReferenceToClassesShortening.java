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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

import java.util.List;

public class ReferenceToClassesShortening {
    private ReferenceToClassesShortening() {
    }

    public static void compactReferenceToClasses(List<? extends JetElement> elementsToCompact) {
        if (elementsToCompact.isEmpty()) {
            return;
        }
        PsiDocumentManager.getInstance(elementsToCompact.get(0).getProject()).commitAllDocuments();

        final JetFile file = (JetFile) elementsToCompact.get(0).getContainingFile();
        final BindingContext bc = AnalyzerFacadeWithCache.analyzeFileWithCache(file).getBindingContext();
        for (JetElement element : elementsToCompact) {
            element.accept(new JetVisitorVoid() {
                @Override
                public void visitJetElement(@NotNull JetElement element) {
                    element.acceptChildren(this);
                }

                @Override
                public void visitTypeReference(@NotNull JetTypeReference typeReference) {
                    super.visitTypeReference(typeReference);

                    JetTypeElement typeElement = typeReference.getTypeElement();
                    if (typeElement instanceof JetNullableType) {
                        typeElement = ((JetNullableType) typeElement).getInnerType();
                    }
                    if (typeElement instanceof JetUserType) {
                        JetUserType userType = (JetUserType) typeElement;
                        DeclarationDescriptor target = bc.get(BindingContext.REFERENCE_TARGET,
                                                              userType.getReferenceExpression());
                        if (target instanceof ClassDescriptor) {
                            ClassDescriptor targetClass = (ClassDescriptor) target;
                            ClassDescriptor targetTopLevelClass = ImportInsertHelper.getTopLevelClass(targetClass);

                            JetScope scope = bc.get(BindingContext.TYPE_RESOLUTION_SCOPE, typeReference);
                            ClassifierDescriptor classifier = scope.getClassifier(targetTopLevelClass.getName());
                            if (targetTopLevelClass == classifier) {
                                compactReferenceToClass(userType, targetClass);
                            }
                            else if (classifier == null) {
                                ImportInsertHelper.addImportDirectiveIfNeeded(DescriptorUtils.getFqNameSafe(targetTopLevelClass), file);
                                compactReferenceToClass(userType, targetClass);
                            }
                            else {
                                // leave FQ name
                            }
                        }
                    }
                }

                private void compactReferenceToClass(JetUserType userType, ClassDescriptor targetClass) {
                    String name = targetClass.getName().asString();
                    DeclarationDescriptor parent = targetClass.getContainingDeclaration();
                    while (parent instanceof ClassDescriptor) {
                        name = parent.getName() + "." + name;
                        parent = parent.getContainingDeclaration();
                    }
                    JetTypeArgumentList typeArgumentList = userType.getTypeArgumentList();
                    JetTypeElement typeElement = JetPsiFactory.createType(userType.getProject(),
                            name + (typeArgumentList == null ? "" : typeArgumentList.getText())).getTypeElement();
                    assert typeElement != null;
                    userType.replace(typeElement);
                }
            });
        }
    }
}
