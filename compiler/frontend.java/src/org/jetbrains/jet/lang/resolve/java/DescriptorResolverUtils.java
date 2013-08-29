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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.ImmutableSet;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.intellij.psi.util.PsiFormatUtilBase.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumClassObject;

public final class DescriptorResolverUtils {
    private static final ImmutableSet<String> OBJECT_METHODS = ImmutableSet.of("hashCode()", "equals(java.lang.Object)", "toString()");

    private DescriptorResolverUtils() {
    }

    public static boolean isCompiledKotlinPackageClass(@NotNull PsiClass psiClass) {
        if (psiClass instanceof ClsClassImpl) {
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName != null && PackageClassUtils.isPackageClassFqName(new FqName(qualifiedName))) {
                return hasAnnotation(psiClass, JvmAnnotationNames.KOTLIN_PACKAGE.getFqName());
            }
        }
        return false;
    }

    public static boolean isCompiledKotlinClass(@NotNull PsiClass psiClass) {
        if (psiClass instanceof ClsClassImpl) {
            return hasAnnotation(psiClass, JvmAnnotationNames.KOTLIN_CLASS.getFqName());
        }
        return false;
    }

    public static boolean hasAnnotation(@NotNull PsiClass psiClass, @NotNull FqName annotationFqName) {
        PsiModifierList list = psiClass.getModifierList();
        if (list != null) {
            return list.findAnnotation(annotationFqName.asString()) != null;
        }
        return false;
    }

    public static boolean isCompiledKotlinClassOrPackageClass(@NotNull PsiClass psiClass) {
        return isCompiledKotlinClass(psiClass) || isCompiledKotlinPackageClass(psiClass);
    }

    @NotNull
    public static Collection<JetType> getSupertypes(@NotNull ClassOrNamespaceDescriptor classOrNamespaceDescriptor) {
        if (classOrNamespaceDescriptor instanceof ClassDescriptor) {
            return ((ClassDescriptor) classOrNamespaceDescriptor).getTypeConstructor().getSupertypes();
        }
        return Collections.emptyList();
    }

    public static Visibility resolveVisibility(@NotNull PsiModifierListOwner modifierListOwner) {
        if (modifierListOwner.hasModifierProperty(PsiModifier.PUBLIC)) {
            return Visibilities.PUBLIC;
        }
        if (modifierListOwner.hasModifierProperty(PsiModifier.PRIVATE)) {
            return Visibilities.PRIVATE;
        }
        if (modifierListOwner.hasModifierProperty(PsiModifier.PROTECTED)) {
            if (modifierListOwner.hasModifierProperty(PsiModifier.STATIC)) {
                return JavaVisibilities.PROTECTED_STATIC_VISIBILITY;
            }
            return JavaVisibilities.PROTECTED_AND_PACKAGE;
        }
        return JavaVisibilities.PACKAGE_VISIBILITY;
    }

    @Nullable
    public static ValueParameterDescriptor getValueParameterDescriptorForAnnotationParameter(
            Name argumentName,
            ClassDescriptor classDescriptor
    ) {
        Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
        assert constructors.size() == 1 : "Annotation class descriptor must have only one constructor";
        List<ValueParameterDescriptor> valueParameters = constructors.iterator().next().getValueParameters();

        for (ValueParameterDescriptor parameter : valueParameters) {
            Name parameterName = parameter.getName();
            if (parameterName.equals(argumentName)) {
                return parameter;
            }
        }
        return null;
    }

    /**
     * @return true if {@code member} is a static member of enum class, which is to be put into its class object (and not into the
     *         corresponding package). This applies to enum entries, values() and valueOf(String) methods
     */
    public static boolean shouldBeInEnumClassObject(@NotNull PsiMember member) {
        PsiClass psiClass = member.getContainingClass();
        if (psiClass == null || !psiClass.isEnum()) return false;

        if (member instanceof PsiEnumConstant) return true;

        if (!(member instanceof PsiMethod)) return false;
        String signature = PsiFormatUtil.formatMethod((PsiMethod) member,
                                                      PsiSubstitutor.EMPTY, SHOW_NAME | SHOW_PARAMETERS, SHOW_TYPE | SHOW_FQ_CLASS_NAMES);

        return "values()".equals(signature) ||
               "valueOf(java.lang.String)".equals(signature);
    }

    public static boolean isCorrectOwnerForEnumMember(
            @NotNull ClassOrNamespaceDescriptor ownerDescriptor,
            @NotNull PsiMember member
    ) {
        return isEnumClassObject(ownerDescriptor) == shouldBeInEnumClassObject(member);
    }

    public static boolean isObjectMethodInInterface(@NotNull PsiMember member) {
        if (!(member instanceof PsiMethod)) {
            return false;
        }
        PsiClass containingClass = member.getContainingClass();
        assert containingClass != null : "containing class is null for " + member;

        if (!containingClass.isInterface()) {
            return false;
        }

        return isObjectMethod((PsiMethod) member);
    }

    public static boolean isObjectMethod(@NotNull PsiMethod method) {
        String formattedMethod = PsiFormatUtil.formatMethod(
                method, PsiSubstitutor.EMPTY, SHOW_NAME | SHOW_PARAMETERS, SHOW_TYPE | SHOW_FQ_CLASS_NAMES);
        return OBJECT_METHODS.contains(formattedMethod);
    }
}
