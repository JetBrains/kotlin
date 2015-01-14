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

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;

public final class JetNamedDeclarationUtil {
    @Nullable
    public static FqNameUnsafe getUnsafeFQName(@NotNull JetNamedDeclaration namedDeclaration) {
        FqName fqName = namedDeclaration.getFqName();
        return fqName != null ? fqName.toUnsafe() : null;
    }

    @Nullable
    //NOTE: use JetNamedDeclaration#getFqName instead
    /*package private*/ static FqName getFQName(@NotNull JetNamedDeclaration namedDeclaration) {
        Name name = namedDeclaration.getNameAsName();
        if (name == null) {
            return null;
        }

        FqName parentFqName = getParentFqName(namedDeclaration);

        if (parentFqName == null) {
            return null;
        }

        return parentFqName.child(name);
    }

    @Nullable
    public static FqName getParentFqName(@NotNull JetNamedDeclaration namedDeclaration) {
        PsiElement parent = namedDeclaration.getParent();
        if (parent instanceof JetClassObject) {
            parent = parent.getParent();
        }
        if (parent instanceof JetClassBody) {
            // One nesting to JetClassBody doesn't affect to qualified name
            parent = parent.getParent();
        }

        if (parent instanceof JetFile) {
            return ((JetFile) parent).getPackageFqName();
        }
        else if (parent instanceof JetNamedFunction || parent instanceof JetClass) {
            return getFQName((JetNamedDeclaration) parent);
        }
        else if (namedDeclaration instanceof JetParameter) {
            JetClass constructorClass = JetPsiUtil.getClassIfParameterIsProperty((JetParameter) namedDeclaration);
            if (constructorClass != null) {
                return getFQName(constructorClass);
            }
        }
        else if (parent instanceof JetObjectDeclaration) {
             return getFQName((JetNamedDeclaration) parent);
        }
        return null;
    }

    private JetNamedDeclarationUtil() {
    }
}
