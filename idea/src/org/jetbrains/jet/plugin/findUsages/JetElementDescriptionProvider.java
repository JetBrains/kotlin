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

package org.jetbrains.jet.plugin.findUsages;

import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.util.RefactoringDescriptionLocation;
import com.intellij.usageView.UsageViewLongNameLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

public class JetElementDescriptionProvider implements ElementDescriptionProvider {
    @Override
    public String getElementDescription(@NotNull PsiElement element, @NotNull ElementDescriptionLocation location) {
        if (location instanceof UsageViewLongNameLocation) {
          if (element instanceof PsiNamedElement && element instanceof JetElement) {
            return ((PsiNamedElement)element).getName();
          }
        }
        else if (location instanceof RefactoringDescriptionLocation) {
            if (element instanceof JetClass) {
                JetClass jetClass = (JetClass) element;
                return (jetClass.isTrait() ? "Trait " : "Class ") + jetClass.getName();
            }
            if (element instanceof JetObjectDeclaration || element instanceof JetObjectDeclarationName) {
                return "Object " + ((PsiNamedElement)element).getName();
            }
            if (element instanceof JetNamedFunction) {
                return "Function " + ((PsiNamedElement)element).getName();
            }
            if (element instanceof JetProperty) {
                return "Property " + ((PsiNamedElement)element).getName();
            }
        }
        return null;
    }
}
