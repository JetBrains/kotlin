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

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.impl.java.stubs.index.JavaMethodNameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import jet.runtime.typeinfo.JetClass;
import jet.runtime.typeinfo.JetValueParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.JetValueParameterAnnotation;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Number of helper methods for searching jet element prototypes in java. Methods use java indices for search.
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
        /* Will iterate through short name caches
           Kotlin namespaces from jar a class files will be collected from java cache
           Kotlin namespaces classes from sources will be collected with JetShortNamesCache.getClassesByName */
        return PsiShortNamesCache.getInstance(project).getClassesByName(JvmAbi.PACKAGE_CLASS, scope);
    }

    /**
     * Get names that could have jet descriptor equivalents. It could be inaccurate and return more results than necessary.
     */
    static Collection<String> getPossiblePackageDeclarationsNames(Project project, GlobalSearchScope scope) {
        Collection<String> result = new ArrayList<String>();

        for (PsiClass jetNamespaceClass : getClassesForJetNamespaces(project, scope)) {
            for (PsiMethod psiMethod : jetNamespaceClass.getMethods()) {
                if (psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    result.add(psiMethod.getName());
                }
            }
        }

        return result;
    }

    static Collection<PsiClass> getCompiledClassesForTopLevelObjects(Project project, GlobalSearchScope scope) {
        Set<PsiClass> jetObjectClasses = Sets.newHashSet();

        Collection<PsiAnnotation> annotations = JavaAnnotationIndex.getInstance().get(JetClass.class.getSimpleName(), project, scope);
        for (PsiAnnotation annotation : annotations) {
            PsiModifierList modifierList = (PsiModifierList)annotation.getParent();
            final PsiElement owner = modifierList.getParent();
            if (owner instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) owner;
                JetClassAnnotation jetAnnotation = JetClassAnnotation.get(psiClass);
                if (psiClass.getContainingClass() == null && jetAnnotation.kind() == JvmStdlibNames.FLAG_CLASS_KIND_OBJECT) {
                    jetObjectClasses.add(psiClass);
                }
            }
        }

        return jetObjectClasses;
    }
    
    static Collection<String> getTopExtensionFunctionNames(Project project, GlobalSearchScope scope) {

        // Extension function should have an parameter of type JetValueParameter with explicit receiver parameter.

        Set<String> extensionNames = new HashSet<String>();

        Collection<PsiAnnotation> valueParametersAnnotations = JavaAnnotationIndex.getInstance().get(
                JetValueParameter.class.getSimpleName(), project, scope);

        for (PsiAnnotation parameterAnnotation : valueParametersAnnotations) {
            PsiParameter parameter = PsiTreeUtil.getParentOfType(parameterAnnotation, PsiParameter.class);
            if (parameter == null) {
                continue;
            }

            if (!JetValueParameterAnnotation.get(parameter).receiver()) {
                continue;
            }

            PsiMethod psiMethod = PsiTreeUtil.getParentOfType(parameter, PsiMethod.class);
            if (psiMethod != null) {
                extensionNames.add(psiMethod.getName());
            }
        }

        return extensionNames;
    }

    static Collection<PsiMethod> getTopExtensionFunctionPrototypesByName(String name, Project project, GlobalSearchScope scope) {
        return filterJetJavaPrototypesByName(
                name, project, scope,
                new Predicate<JetValueParameterAnnotation>() {
                    @Override
                    public boolean apply(@Nullable JetValueParameterAnnotation jetValueParameterAnnotation) {
                        assert jetValueParameterAnnotation != null;
                        return jetValueParameterAnnotation.receiver();
                    }
                });
    }

    static Collection<PsiMethod> getTopLevelFunctionPrototypesByName(String name, Project project, GlobalSearchScope scope) {
        return filterJetJavaPrototypesByName(
                name, project, scope,
                new Predicate<JetValueParameterAnnotation>() {
                    @Override
                    public boolean apply(@Nullable JetValueParameterAnnotation jetValueParameterAnnotation) {
                        assert jetValueParameterAnnotation != null;
                        return !jetValueParameterAnnotation.receiver();
                    }
                });
    }

    @Nullable
    static FqName getJetTopLevelDeclarationFQN(@NotNull PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();

        if (containingClass != null) {
            final String qualifiedName = containingClass.getQualifiedName();
            assert qualifiedName != null;

            FqName classFQN = new FqName(qualifiedName);

            if (classFQN.shortName().toString().equals(JvmAbi.PACKAGE_CLASS)) {
                FqName classParentFQN = QualifiedNamesUtil.withoutLastSegment(classFQN);
                return QualifiedNamesUtil.combine(classParentFQN, Name.identifier(method.getName()));
            }
        }

        return null;
    }

    private static Collection<PsiMethod> filterJetJavaPrototypesByName(
            String name, Project project, GlobalSearchScope scope,
            Predicate<JetValueParameterAnnotation> filterPredicate) {
        Set<PsiMethod> selectedMethods = new HashSet<PsiMethod>();

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
                if (filterPredicate.apply(JetValueParameterAnnotation.get(parameter))) {
                    selectedMethods.add(psiMethod);
                }
            }
        }

        return selectedMethods;
    }
}
