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

package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Nikolay Krasko
 */
public final class JetModifiableBlockHelper {
    private JetModifiableBlockHelper() {
    }

    // TODO: Need tests for this method
    // TODO: Consider for FUNCTION_LITERALS
    public static boolean shouldChangeModificationCount(PsiElement place) {
        JetDeclaration declaration = PsiTreeUtil.getParentOfType(place, JetDeclaration.class, true);
        if (declaration != null) {
            if (declaration instanceof JetNamedFunction) {
                JetNamedFunction function = (JetNamedFunction) declaration;
                if (function.hasDeclaredReturnType() || function.hasBlockBody()) {
                    return false;
                }

                return shouldChangeModificationCount(function);
            }
            else if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                if (property.getPropertyTypeRef() != null) {
                    return false;
                }

                return shouldChangeModificationCount(property);
            }
        }

        return true;
    }
}
