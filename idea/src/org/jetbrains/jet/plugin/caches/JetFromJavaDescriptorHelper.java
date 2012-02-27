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

package org.jetbrains.jet.plugin.caches;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.impl.java.stubs.index.JavaMethodNameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import jet.runtime.typeinfo.JetValueParameter;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Get jet declarations from java that could be used in completion. Unlike the real jet resolver this helper is allowed
 * to return partially unresolved descriptors in exchange of execution speed.
 *
 * @author Nikolay Krasko
 */
class JetFromJavaDescriptorHelper {

    private JetFromJavaDescriptorHelper() {
    }

    /**
     * Get java equivalents for jet top level classes.
     */
    static PsiClass[] getClassesForJetNamespaces(Project project, GlobalSearchScope scope) {
        return PsiShortNamesCache.getInstance(project).getClassesByName(JvmAbi.PACKAGE_CLASS, scope);
    }

    /**
     * Get names that could have jet descriptor equivalents. It could be inaccurate and return more results than necessary.
     */
    static Collection<String> getPossiblePackageDeclarationsNames(Project project, GlobalSearchScope scope) {
        final ArrayList<String> result = new ArrayList<String>();

        for (PsiClass jetNamespaceClass : getClassesForJetNamespaces(project, scope)) {
            for (PsiMethod psiMethod : jetNamespaceClass.getMethods()) {
                if (psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    result.add(psiMethod.getName());
                }
            }
        }

        return result;
    }
    
    static Collection<String> getTopExtensionFunctionNames(Project project, GlobalSearchScope scope) {

        // Extension function should have an parameter of type JetValueParameter with explicit receiver parameter.

        HashSet<String> extensionNames = new HashSet<String>();

        Collection<PsiAnnotation> valueParametersAnnotations = JavaAnnotationIndex.getInstance().get(
                JetValueParameter.class.getSimpleName(), project, scope);

        for (PsiAnnotation parameterAnnotation : valueParametersAnnotations) {
            String qualifiedName = parameterAnnotation.getQualifiedName();

            if (qualifiedName == null || !qualifiedName.equals(JetValueParameter.class.getCanonicalName())) {
                continue;
            }

            if (!getAnnotationAttribute(parameterAnnotation, "receiver", false)) {
                continue;
            }

            PsiMethod psiMethod = PsiTreeUtil.getParentOfType(parameterAnnotation, PsiMethod.class);
            if (psiMethod != null) {
                extensionNames.add(psiMethod.getName());
            }
        }

        return extensionNames;
    }

    static Collection<PsiMethod> getTopExtensionFunctionByName(String name, Project project, GlobalSearchScope scope) {

        HashSet<PsiMethod> selectedMethods = new HashSet<PsiMethod>();

        Collection<PsiMethod> psiMethods = JavaMethodNameIndex.getInstance().get(name, project, scope);
        for (PsiMethod psiMethod : psiMethods) {
            if (psiMethod == null) {
                continue;
            }

            // Check this is top level function
            PsiClass containingClass = psiMethod.getContainingClass();
            if (containingClass == null || !JvmAbi.PACKAGE_CLASS.equals(containingClass.getName())) {
                continue;
            }

            // Should be parameter with JetValueParameter.receiver == true
            for (PsiParameter parameter : psiMethod.getParameterList().getParameters()) {
                PsiModifierList modifierList = parameter.getModifierList();
                if (modifierList != null) {
                    for (PsiAnnotation psiAnnotation : modifierList.getAnnotations()) {
                        if (!JetValueParameter.class.getCanonicalName().equals(psiAnnotation.getQualifiedName())) {
                            continue;
                        }

                        if (getAnnotationAttribute(psiAnnotation, "receiver", false)) {
                            selectedMethods.add(psiMethod);
                        }
                    }
                }
            }
        }

        return selectedMethods;
    }

    private static boolean getAnnotationAttribute(PsiAnnotation annotation, String attributeName, boolean defaultValue) {
        // Check that parameter is receiver
        PsiAnnotationMemberValue attributeValue = annotation.findAttributeValue(attributeName);
        if (!(attributeValue instanceof PsiLiteralExpression)) {
            return defaultValue;
        }

        // Every extension function will have parameter marked with attribute where receiver == true
        Object value = ((PsiLiteralExpression) attributeValue).getValue();
        if (value == null) {
            return defaultValue;
        }

        return (Boolean) value;
    }
}
