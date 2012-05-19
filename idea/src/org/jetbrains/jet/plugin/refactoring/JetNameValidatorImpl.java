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

package org.jetbrains.jet.plugin.refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.jvm.compiler.TipsManager;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetVisitorVoid;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.Collection;

/**
 * User: Alefas
 * Date: 07.02.12
 */
public class JetNameValidatorImpl implements JetNameValidator {
    public static JetNameValidator getEmptyValidator(final Project project) {
        return new JetNameValidator() {
            @Override
            public String validateName(String name) {
                return name;
            }

            @Override
            public Project getProject() {
                return project;
            }
        };
    }

    private final PsiElement myContainer;
    private PsiElement myAnchor;
    BindingContext myBindingContext;

    public JetNameValidatorImpl(PsiElement container, PsiElement anchor) {
        myContainer = container;
        myAnchor = anchor;
    }

    @Nullable
    public String validateName(String name) {
        if (validateInner(name)) return name;
        int i = 1;
        while (true) {
            if (validateInner(name + i)) return name + i;
            ++i;
        }
    }

    private boolean validateInner(String name) {
        PsiElement sibling;
        if (myAnchor != null) {
            sibling = myAnchor;
        }
        else {
            if (myContainer instanceof JetExpression) {
                return checkElement(name, myContainer);
            }
            sibling = myContainer.getFirstChild();
        }

        while (sibling != null) {
            if (!checkElement(name, sibling)) return false;
            sibling = sibling.getNextSibling();
        }

        return true;
    }

    private boolean checkElement(final String name, PsiElement sibling) {
        if (myBindingContext == null) {
            myBindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(
                    (JetFile) myContainer.getContainingFile()).getBindingContext();
        }
        final Ref<Boolean> result = new Ref<Boolean>(true);
        JetVisitorVoid visitor = new JetVisitorVoid() {
            @Override
            public void visitElement(PsiElement element) {
                if (result.get()) {
                    element.acceptChildren(this);
                }
            }

            @Override
            public void visitExpression(JetExpression expression) {
                Collection<DeclarationDescriptor> variants =
                        TipsManager.getVariantsNoReceiver(expression, myBindingContext);
                for (DeclarationDescriptor variant : variants) {
                    if (variant.getName().equals(name)) {
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

    public Project getProject() {
        return myContainer.getProject();
    }
}
