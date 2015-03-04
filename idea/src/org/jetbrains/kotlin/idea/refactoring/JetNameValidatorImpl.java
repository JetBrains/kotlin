/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.scopes.JetScope;

import java.util.HashSet;
import java.util.Set;

public class JetNameValidatorImpl extends JetNameValidator {
    public static enum Target {
        FUNCTIONS_AND_CLASSES,
        PROPERTIES
    }

    private final PsiElement myContainer;
    private final PsiElement myAnchor;
    private final Target myTarget;

    public JetNameValidatorImpl(PsiElement container, PsiElement anchor, Target target) {
        myContainer = container;
        myAnchor = anchor;
        myTarget = target;
    }

    @Override
    protected boolean validateInner(String name) {
        Set<JetScope> visitedScopes = new HashSet<JetScope>();

        PsiElement sibling;
        if (myAnchor != null) {
            sibling = myAnchor;
        }
        else {
            if (myContainer instanceof JetExpression) {
                return checkElement(name, myContainer, visitedScopes);
            }
            sibling = myContainer.getFirstChild();
        }

        while (sibling != null) {
            if (!checkElement(name, sibling, visitedScopes)) return false;
            sibling = sibling.getNextSibling();
        }

        return true;
    }

    private boolean checkElement(String name, PsiElement sibling, final Set<JetScope> visitedScopes) {
        if (!(sibling instanceof JetElement)) return true;

        AnalysisResult analysisResult = ResolvePackage.analyzeAndGetResult((JetElement) sibling);
        final BindingContext bindingContext = analysisResult.getBindingContext();
        final ModuleDescriptor module = analysisResult.getModuleDescriptor();
        final Name identifier = Name.identifier(name);

        final Ref<Boolean> result = new Ref<Boolean>(true);
        JetVisitorVoid visitor = new JetVisitorVoid() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (result.get()) {
                    element.acceptChildren(this);
                }
            }

            @Nullable
            private JetScope getScope(@NotNull JetExpression expression) {
                PsiElement parent = expression.getParent();

                if (parent instanceof JetClassBody) {
                    JetClassOrObject classOrObject = (JetClassOrObject) parent.getParent();
                    ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject);
                    return classDescriptor instanceof ClassDescriptorWithResolutionScopes
                           ? ((ClassDescriptorWithResolutionScopes) classDescriptor).getScopeForMemberDeclarationResolution()
                           : null;
                }

                if (parent instanceof JetFile) {
                    PackageViewDescriptor packageViewDescriptor = module.getPackage(((JetFile) parent).getPackageFqName());
                    return packageViewDescriptor != null ? packageViewDescriptor.getMemberScope() : null;
                }

                return bindingContext.get(BindingContext.RESOLUTION_SCOPE, expression);
            }

            @Override
            public void visitExpression(@NotNull JetExpression expression) {
                JetScope resolutionScope = getScope(expression);

                if (resolutionScope != null) {
                    if (!visitedScopes.add(resolutionScope)) return;

                    boolean noConflict;
                    if (myTarget == Target.PROPERTIES) {
                        noConflict = resolutionScope.getProperties(identifier).isEmpty()
                                     && resolutionScope.getLocalVariable(identifier) == null;
                    }
                    else {
                        noConflict = resolutionScope.getFunctions(identifier).isEmpty()
                                     && resolutionScope.getClassifier(identifier) == null;
                    }

                    if (!noConflict) {
                        result.set(false);
                        return;
                    }
                }

                super.visitExpression(expression);
            }
        };
        sibling.accept(visitor);
        return result.get();
    }
}
