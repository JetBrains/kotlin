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

import com.intellij.psi.*;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.java.kt.PsiAnnotationWithFlags;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMemberWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.intellij.psi.util.PsiFormatUtilBase.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassObjectName;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumClassObject;

public final class DescriptorResolverUtils {
    public static final FqName OBJECT_FQ_NAME = new FqName("java.lang.Object");

    private DescriptorResolverUtils() {
    }

    public static boolean isKotlinClass(@NotNull PsiClass psiClass) {
        PsiClassWrapper wrapper = new PsiClassWrapper(psiClass);
        return wrapper.getJetClass().isDefined() ||  wrapper.getJetPackageClass().isDefined();
    }

    @NotNull
    public static Collection<JetType> getSupertypes(@NotNull ClassOrNamespaceDescriptor classOrNamespaceDescriptor) {
        if (classOrNamespaceDescriptor instanceof ClassDescriptor) {
            return ((ClassDescriptor) classOrNamespaceDescriptor).getTypeConstructor().getSupertypes();
        }
        return Collections.emptyList();
    }

    public static Modality resolveModality(PsiMemberWrapper memberWrapper, boolean isFinal) {
        if (memberWrapper instanceof PsiMethodWrapper) {
            PsiMethodWrapper method = (PsiMethodWrapper) memberWrapper;
            if (method.getJetMethodAnnotation().hasForceOpenFlag()) {
                return Modality.OPEN;
            }
            if (method.getJetMethodAnnotation().hasForceFinalFlag()) {
                return Modality.FINAL;
            }
        }

        return Modality.convertFromFlags(memberWrapper.isAbstract(), !isFinal);
    }

    public static Visibility resolveVisibility(
            @NotNull PsiModifierListOwner modifierListOwner,
            @Nullable PsiAnnotationWithFlags annotation
    ) {
        if (annotation != null) {
            if (annotation.hasPrivateFlag()) {
                return Visibilities.PRIVATE;
            }
            else if (annotation.hasInternalFlag()) {
                return Visibilities.INTERNAL;
            }
        }

        if (modifierListOwner.hasModifierProperty(PsiModifier.PUBLIC)) {
            return Visibilities.PUBLIC;
        }
        if (modifierListOwner.hasModifierProperty(PsiModifier.PRIVATE)) {
            return Visibilities.PRIVATE;
        }
        if (modifierListOwner.hasModifierProperty(PsiModifier.PROTECTED)) {
            if (modifierListOwner.hasModifierProperty(PsiModifier.STATIC)) {
                return JavaDescriptorResolver.PROTECTED_STATIC_VISIBILITY;
            }
            return JavaDescriptorResolver.PROTECTED_AND_PACKAGE;
        }
        return JavaDescriptorResolver.PACKAGE_VISIBILITY;
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

    public static Visibility getConstructorVisibility(ClassDescriptor classDescriptor) {
        Visibility containingClassVisibility = classDescriptor.getVisibility();
        if (containingClassVisibility == JavaDescriptorResolver.PROTECTED_STATIC_VISIBILITY) {
            return JavaDescriptorResolver.PROTECTED_AND_PACKAGE;
        }
        return containingClassVisibility;
    }

    public static void checkPsiClassIsNotJet(@Nullable PsiClass psiClass) {
        if (psiClass instanceof JetJavaMirrorMarker) {
            throw new IllegalStateException("trying to resolve fake jet PsiClass as regular PsiClass: " + psiClass.getQualifiedName());
        }
    }

    @NotNull
    public static FqNameUnsafe getFqNameForClassObject(@NotNull PsiClass psiClass) {
        String psiClassQualifiedName = psiClass.getQualifiedName();
        assert psiClassQualifiedName != null : "Reading java class with no qualified name";
        return new FqNameUnsafe(psiClassQualifiedName + "." + getClassObjectName(psiClass.getName()).getName());
    }

    @NotNull
    public static AnnotationDescriptor getAnnotationDescriptorForJavaLangDeprecated(ClassDescriptor classDescriptor) {
        AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor();
        annotationDescriptor.setAnnotationType(classDescriptor.getDefaultType());
        ValueParameterDescriptor value = getValueParameterDescriptorForAnnotationParameter(Name.identifier("value"), classDescriptor);
        assert value != null : "jet.deprecated must have one parameter called value";
        annotationDescriptor.setValueArgument(value, new StringValue("Deprecated in Java"));
        return annotationDescriptor;
    }

    /**
     * @return true if {@code member} is a static member of enum class, which is to be put into its class object (and not into the
     * corresponding package). This applies to enum entries, values() and valueOf(String) methods
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
}
