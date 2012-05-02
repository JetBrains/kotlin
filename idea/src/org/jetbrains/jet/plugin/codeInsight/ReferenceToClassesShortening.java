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

package org.jetbrains.jet.plugin.codeInsight;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

import java.util.List;

public class ReferenceToClassesShortening {
    private ReferenceToClassesShortening() {
    }

    public static void compactReferenceToClasses(List<? extends JetElement> elementsToCompact) {
        if (elementsToCompact.isEmpty()) {
            return;
        }
        final JetFile file = (JetFile) elementsToCompact.get(0).getContainingFile();
        final BindingContext bc = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file).getBindingContext();
        for (JetElement element : elementsToCompact) {
            element.accept(new JetVisitorVoid() {
                @Override
                public void visitJetElement(JetElement element) {
                    element.acceptChildren(this);
                }

                @Override
                public void visitTypeReference(JetTypeReference typeReference) {
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
                                ImportInsertHelper.addImportDirective(DescriptorUtils.getFQName(targetTopLevelClass).toSafe(), file);
                                compactReferenceToClass(userType, targetClass);
                            }
                            else {
                                // leave FQ name
                            }
                        }
                    }
                }

                private void compactReferenceToClass(JetUserType userType, ClassDescriptor targetClass) {
                    if (targetClass == JetStandardClasses.getUnitType().getConstructor().getDeclarationDescriptor()) {
                        // do not replace "Unit" with "Tuple0"
                        return;
                    }
                    String name = targetClass.getName();
                    DeclarationDescriptor parent = targetClass.getContainingDeclaration();
                    while (parent instanceof ClassDescriptor) {
                        name = parent.getName() + "." + name;
                        parent = parent.getContainingDeclaration();
                    }
                    JetTypeArgumentList typeArgumentList = userType.getTypeArgumentList();
                    userType.replace(JetPsiFactory.createType(userType.getProject(), name + (typeArgumentList == null ? "" : typeArgumentList.getText())));
                }
            });
        }
    }
}
