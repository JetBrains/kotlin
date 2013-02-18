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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.psi.*;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// originally from com.intellij.codeInsight.daemon.impl.analysis.HighlightMethodUtil
class JavaMethodSignatureUtil {
    // This and following methods are originally from com.intellij.codeInsight.daemon.impl.analysis.HighlightMethodUtil
    static boolean isMethodReturnTypeCompatible(@NotNull PsiMethod method) {
        HierarchicalMethodSignature methodSignature = method.getHierarchicalMethodSignature();
        List<HierarchicalMethodSignature> superSignatures = methodSignature.getSuperSignatures();

        PsiType returnType = methodSignature.getSubstitutor().substitute(method.getReturnType());
        PsiClass aClass = method.getContainingClass();
        if (aClass == null) return false;
        for (MethodSignatureBackedByPsiMethod superMethodSignature : superSignatures) {
            PsiMethod superMethod = superMethodSignature.getMethod();
            PsiType declaredReturnType = superMethod.getReturnType();
            PsiType superReturnType = declaredReturnType;
            if (superMethodSignature.isRaw()) superReturnType = TypeConversionUtil.erasure(declaredReturnType);
            if (returnType == null || superReturnType == null || method == superMethod) continue;
            PsiClass superClass = superMethod.getContainingClass();
            if (superClass == null) continue;
            if (!areMethodsReturnTypesCompatible(superMethodSignature, superReturnType, method, methodSignature, returnType)) return false;
        }

        return true;
    }

    private static boolean areMethodsReturnTypesCompatible(
            MethodSignatureBackedByPsiMethod superMethodSignature,
            PsiType superReturnType,
            PsiMethod method,
            MethodSignatureBackedByPsiMethod methodSignature,
            PsiType returnType
    ) {
        if (superReturnType == null) return false;
        PsiType substitutedSuperReturnType;
        final boolean isJdk15 = PsiUtil.isLanguageLevel5OrHigher(method);
        if (isJdk15 && !superMethodSignature.isRaw() && superMethodSignature.equals(methodSignature)) { //see 8.4.5
            PsiSubstitutor unifyingSubstitutor = MethodSignatureUtil.getSuperMethodSignatureSubstitutor(methodSignature,
                                                                                                        superMethodSignature);
            substitutedSuperReturnType = unifyingSubstitutor == null
                                         ? superReturnType
                                         : unifyingSubstitutor.substitute(superReturnType);
        }
        else {
            substitutedSuperReturnType = TypeConversionUtil.erasure(superMethodSignature.getSubstitutor().substitute(superReturnType));
        }

        if (returnType.equals(substitutedSuperReturnType)) return true;
        if (!(returnType instanceof PsiPrimitiveType) && substitutedSuperReturnType.getDeepComponentType() instanceof PsiClassType) {
            if (isJdk15 && TypeConversionUtil.isAssignable(substitutedSuperReturnType, returnType)) {
                return true;
            }
        }

        return false;
    }

    private JavaMethodSignatureUtil() {
    }
}
