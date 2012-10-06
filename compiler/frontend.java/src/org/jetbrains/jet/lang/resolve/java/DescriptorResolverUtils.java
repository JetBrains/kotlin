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

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.kt.PsiAnnotationWithFlags;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMemberWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class DescriptorResolverUtils {
    private DescriptorResolverUtils() {
    }

    public static boolean isKotlinClass(@NotNull PsiClass psiClass) {
        return new PsiClassWrapper(psiClass).getJetClass().isDefined() || psiClass.getName().equals(JvmAbi.PACKAGE_CLASS);
    }

    public static boolean isInnerEnum(@NotNull PsiClass innerClass, DeclarationDescriptor owner) {
        if (!innerClass.isEnum()) return false;
        if (!(owner instanceof ClassDescriptor)) return false;

        ClassKind kind = ((ClassDescriptor) owner).getKind();
        return kind == ClassKind.CLASS || kind == ClassKind.TRAIT || kind == ClassKind.ENUM_CLASS;
    }

    public static Collection<JetType> getSupertypes(JavaDescriptorResolveData.ResolverScopeData scope) {
        if (scope instanceof JavaDescriptorResolveData.ResolverClassData) {
            return ((JavaDescriptorResolveData.ResolverClassData) scope).getClassDescriptor().getSupertypes();
        }
        else if (scope instanceof JavaDescriptorResolveData.ResolverNamespaceData) {
            return Collections.emptyList();
        }
        else {
            throw new IllegalStateException();
        }
    }

    public static Modality resolveModality(PsiMemberWrapper memberWrapper, boolean isFinal) {
        if (memberWrapper instanceof PsiMethodWrapper) {
            PsiMethodWrapper method = (PsiMethodWrapper) memberWrapper;
            if (method.getJetMethod().hasForceOpenFlag()) {
                return Modality.OPEN;
            }
            if (method.getJetMethod().hasForceFinalFlag()) {
                return Modality.FINAL;
            }
        }

        return Modality.convertFromFlags(memberWrapper.isAbstract(), !isFinal);
    }

    public static Visibility resolveVisibility(
            PsiModifierListOwner modifierListOwner,
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
        return modifierListOwner.hasModifierProperty(PsiModifier.PUBLIC) ? Visibilities.PUBLIC :
               (modifierListOwner.hasModifierProperty(PsiModifier.PRIVATE) ? Visibilities.PRIVATE :
                (modifierListOwner.hasModifierProperty(PsiModifier.PROTECTED) ? Visibilities.PROTECTED :
                 //Visibilities.PUBLIC));
                 JavaDescriptorResolver.PACKAGE_VISIBILITY));
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

    public static void getResolverScopeData(@NotNull JavaDescriptorResolveData.ResolverScopeData scopeData) {
        if (scopeData.getNamedMembersMap() == null) {
            scopeData.setNamedMembersMap(JavaDescriptorResolverHelper.getNamedMembers(scopeData));
        }
    }

    public static void checkPsiClassIsNotJet(PsiClass psiClass) {
        if (psiClass instanceof JetJavaMirrorMarker) {
            throw new IllegalStateException("trying to resolve fake jet PsiClass as regular PsiClass: " + psiClass.getQualifiedName());
        }
    }
}
