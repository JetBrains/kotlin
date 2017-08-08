/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.hierarchy.calls;

import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMemberReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.references.KtReference;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.KtProperty;

public abstract class CalleeReferenceProcessor extends ReadActionProcessor<PsiReference> {
    private final boolean kotlinOnly;

    public CalleeReferenceProcessor(boolean only) {
        kotlinOnly = only;
    }

    @Override
    public boolean processInReadAction(PsiReference ref) {
        // copied from Java
        if (!(ref instanceof PsiReferenceExpression || ref instanceof KtReference)) {
            if (!(ref instanceof PsiElement)) {
                return true;
            }

            PsiElement parent = ((PsiElement) ref).getParent();
            if (parent instanceof PsiNewExpression) {
                if (((PsiNewExpression) parent).getClassReference() != ref) {
                    return true;
                }
            }
            else if (parent instanceof PsiAnonymousClass) {
                if (((PsiAnonymousClass) parent).getBaseClassReference() != ref) {
                    return true;
                }
            }
            else if (ref instanceof LightMemberReference) {
                PsiElement refTarget = ref.resolve();
                // Accept implicit superclass constructor reference in Java code
                if (!(refTarget instanceof PsiMethod && ((PsiMethod) refTarget).isConstructor())) return true;
            }
            else {
                return true;
            }
        }

        PsiElement refElement = ref.getElement();
        if (PsiTreeUtil.getParentOfType(refElement, KtImportDirective.class, true) != null) return true;

        PsiElement element = (refElement instanceof KtElement)
                             ? CallHierarchyUtilsKt.getCallHierarchyElement(refElement)
                             : PsiTreeUtil.getParentOfType(refElement, PsiMethod.class, false);

        if (kotlinOnly && !(element instanceof KtNamedDeclaration)) return true;

        // If reference belongs to property initializer, show enclosing declaration instead
        if (element instanceof KtProperty) {
            KtProperty property = (KtProperty) element;
            if (PsiTreeUtil.isAncestor(property.getInitializer(), refElement, false)) {
                element = CallHierarchyUtilsKt.getCallHierarchyElement(element.getParent());
            }
        }

        if (element != null) {
            onAccept(ref, element);
        }

        return true;
    }

    protected abstract void onAccept(@NotNull PsiReference ref, @NotNull PsiElement element);
}