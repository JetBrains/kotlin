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

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinFindClassUsagesHandler;
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinFindFunctionUsagesHandler;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil;

import java.util.Collection;

public class KotlinFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    @Override
    public boolean canFindUsages(@NotNull PsiElement element) {
        return element instanceof JetClass || element instanceof JetNamedFunction;
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
        if (element instanceof JetClass) {
            return new KotlinFindClassUsagesHandler((JetClass) element, this);
        }
        if (element instanceof JetNamedFunction) {
            if (!forHighlightUsages) {
                Collection<? extends PsiElement> methods =
                        JetRefactoringUtil.checkSuperMethods((JetDeclaration) element, null, "super.methods.action.key.find.usages");

                if (methods == null || methods.isEmpty()) return FindUsagesHandler.NULL_HANDLER;
                if (methods.size() > 1) {
                    return new KotlinFindFunctionUsagesHandler((JetNamedFunction) element, methods, this);
                }
            }
            
            return new KotlinFindFunctionUsagesHandler((JetNamedFunction) element, this);
        }
        throw new IllegalArgumentException("unexpected element type: " + element);
    }
}
