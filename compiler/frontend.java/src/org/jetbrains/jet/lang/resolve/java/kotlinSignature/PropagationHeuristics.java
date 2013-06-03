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

package org.jetbrains.jet.lang.resolve.java.kotlinSignature;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiSubstitutorImpl;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.intellij.psi.util.TypeConversionUtil.erasure;

// This class contains heuristics for processing corner cases in propagation
class PropagationHeuristics {
    // Checks for case when method returning Super[] is overridden with method returning Sub[]
    static void checkArrayInReturnType(
            @NotNull SignaturesPropagationData data,
            @NotNull JetType type,
            @NotNull List<SignaturesPropagationData.TypeAndVariance> typesFromSuper
    ) {
        List<SignaturesPropagationData.TypeAndVariance> arrayTypesFromSuper = ContainerUtil
                .filter(typesFromSuper, new Condition<SignaturesPropagationData.TypeAndVariance>() {
                    @Override
                    public boolean value(SignaturesPropagationData.TypeAndVariance typeAndVariance) {
                        return typeAndVariance.type.getConstructor().getDeclarationDescriptor() == KotlinBuiltIns.getInstance().getArray();
                    }
                });
        if (KotlinBuiltIns.getInstance().getArray() == type.getConstructor().getDeclarationDescriptor() && !arrayTypesFromSuper.isEmpty()) {
            assert type.getArguments().size() == 1;
            if (type.getArguments().get(0).getProjectionKind() == Variance.INVARIANT) {
                for (SignaturesPropagationData.TypeAndVariance typeAndVariance : arrayTypesFromSuper) {
                    JetType arrayTypeFromSuper = typeAndVariance.type;
                    assert arrayTypeFromSuper.getArguments().size() == 1;
                    JetType elementTypeInSuper = arrayTypeFromSuper.getArguments().get(0).getType();
                    JetType elementType = type.getArguments().get(0).getType();

                    if (JetTypeChecker.INSTANCE.isSubtypeOf(elementType, elementTypeInSuper)
                            && !JetTypeChecker.INSTANCE.equalTypes(elementType, elementTypeInSuper)) {
                        JetTypeImpl betterTypeInSuper = new JetTypeImpl(
                                arrayTypeFromSuper.getAnnotations(),
                                arrayTypeFromSuper.getConstructor(),
                                arrayTypeFromSuper.isNullable(),
                                Arrays.asList(new TypeProjection(Variance.OUT_VARIANCE, elementTypeInSuper)),
                                JetScope.EMPTY);

                        data.reportError("Return type is not a subtype of overridden method. " +
                                         "To fix it, add annotation with Kotlin signature to super method with type "
                                         + DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(arrayTypeFromSuper) + " replaced with "
                                         + DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(betterTypeInSuper) + " in return type");
                    }
                }
            }
        }
    }

    // Weird workaround for weird case. The sample code below is compiled by javac.
    // In this case, we try to replace "Any" parameter type with "T" to fix substitution principle.
    //
    //    public interface Super<T> {
    //        void foo(T t);
    //    }
    //
    //    public interface Sub<T> extends Super<T> {
    //        void foo(Object o);
    //    }
    //
    // This method is called from SignaturesPropagationData.
    @Nullable
    static ClassifierDescriptor tryToFixOverridingTWithRawType(
            @NotNull SignaturesPropagationData data,
            @NotNull List<SignaturesPropagationData.TypeAndVariance> typesFromSuper
    ) {
        List<TypeParameterDescriptor> typeParameterClassifiersFromSuper = Lists.newArrayList();
        for (SignaturesPropagationData.TypeAndVariance typeFromSuper : typesFromSuper) {
            ClassifierDescriptor classifierFromSuper = typeFromSuper.type.getConstructor().getDeclarationDescriptor();
            if (classifierFromSuper instanceof TypeParameterDescriptor) {
                typeParameterClassifiersFromSuper.add((TypeParameterDescriptor) classifierFromSuper);
            }
        }

        if (!typeParameterClassifiersFromSuper.isEmpty() && typeParameterClassifiersFromSuper.size() == typesFromSuper.size()) {
            for (TypeParameterDescriptor typeParameter : typeParameterClassifiersFromSuper) {
                if (typeParameter.getContainingDeclaration() == data.containingClass) {
                    return typeParameter;
                }
            }
        }

        return null;
    }

    @NotNull
    static List<PsiMethod> getSuperMethods(@NotNull PsiMethod method) {
        List<PsiMethod> superMethods = Lists.newArrayList();
        for (PsiMethod superMethod : new SuperMethodCollector(method).collect()) {
            CallableMemberDescriptor.Kind kindFromFlags =
                    DescriptorKindUtils.flagsToKind(new PsiMethodWrapper(superMethod).getJetMethodAnnotation().kind());
            if (kindFromFlags == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                // This is for the case when a Kotlin class inherits a non-abstract method from a trait:
                // from Kotlin's point of view it is fake override, from Java's it is normal override.
                // We replace this "fake override" method with original method from the trait.
                superMethods.addAll(getSuperMethods(superMethod));
            }
            else {
                superMethods.add(superMethod);
            }
        }
        return superMethods;
    }

    private PropagationHeuristics() {
    }

    private static class SuperMethodCollector {
        private final PsiMethod initialMethod;
        private final String initialMethodName;
        private final List<PsiType> initialParametersErasure;

        private final List<PsiClass> visitedSuperclasses = Lists.newArrayList();
        private final List<PsiMethod> collectedMethods = Lists.newArrayList();

        private SuperMethodCollector(@NotNull PsiMethod initialMethod) {
            this.initialMethod = initialMethod;
            initialMethodName = initialMethod.getName();

            PsiParameterList parameterList = initialMethod.getParameterList();
            initialParametersErasure = Lists.newArrayListWithExpectedSize(parameterList.getParametersCount());
            for (PsiParameter parameter : parameterList.getParameters()) {
                initialParametersErasure.add(erasureNoEllipsis(parameter.getType()));
            }
        }

        public List<PsiMethod> collect() {
            if (!canHaveSuperMethod(initialMethod)) {
                return Collections.emptyList();
            }

            PsiClass containingClass = initialMethod.getContainingClass();
            assert containingClass != null : " containing class is null for " + initialMethod;

            for (PsiClassType superType : containingClass.getSuperTypes()) {
                collectFromSupertype(superType);
            }

            return collectedMethods;
        }

        private void collectFromSupertype(PsiClassType type) {
            PsiClass klass = type.resolve();
            if (klass == null) {
                return;
            }
            if (!visitedSuperclasses.add(klass)) {
                return;
            }

            PsiSubstitutor supertypeSubstitutor = getErasedSubstitutor(type);
            for (PsiMethod methodFromSuper : klass.getMethods()) {
                if (isSubMethodOf(methodFromSuper, supertypeSubstitutor)) {
                    collectedMethods.add(methodFromSuper);
                    return;
                }
            }

            for (PsiType superType : type.getSuperTypes()) {
                assert superType instanceof PsiClassType : "supertype is not a PsiClassType for " + type + ": " + superType;
                collectFromSupertype((PsiClassType) superType);
            }
        }

        private boolean isSubMethodOf(@NotNull PsiMethod methodFromSuper, @NotNull PsiSubstitutor supertypeSubstitutor) {
            if (!methodFromSuper.getName().equals(initialMethodName)) {
                return false;
            }

            PsiParameterList fromSuperParameterList = methodFromSuper.getParameterList();

            if (fromSuperParameterList.getParametersCount() != initialParametersErasure.size()) {
                return false;
            }

            for (int i = 0; i < initialParametersErasure.size(); i++) {
                PsiType originalType = initialParametersErasure.get(i);
                PsiType typeFromSuper = fromSuperParameterList.getParameters()[i].getType();
                PsiType typeFromSuperErased = erasureNoEllipsis(supertypeSubstitutor.substitute(typeFromSuper));

                if (!Comparing.equal(originalType, typeFromSuperErased)) {
                    return false;
                }
            }

            return true;
        }

        private static PsiType erasureNoEllipsis(PsiType type) {
            if (type instanceof PsiEllipsisType) {
                return erasureNoEllipsis(((PsiEllipsisType) type).toArrayType());
            }
            return erasure(type);
        }

        private static PsiSubstitutor getErasedSubstitutor(PsiClassType type) {
            Map<PsiTypeParameter, PsiType> unerasedMap = type.resolveGenerics().getSubstitutor().getSubstitutionMap();
            Map<PsiTypeParameter, PsiType> erasedMap = Maps.newHashMapWithExpectedSize(unerasedMap.size());
            for (Map.Entry<PsiTypeParameter, PsiType> entry : unerasedMap.entrySet()) {
                erasedMap.put(entry.getKey(), erasure(entry.getValue()));
            }
            return PsiSubstitutorImpl.createSubstitutor(erasedMap);
        }

        private static boolean canHaveSuperMethod(PsiMethod method) {
            if (method.isConstructor()) return false;
            if (method.hasModifierProperty(PsiModifier.STATIC)) return false;
            if (method.hasModifierProperty(PsiModifier.PRIVATE)) return false;
            PsiClass containingClass = method.getContainingClass();
            return containingClass != null && !CommonClassNames.JAVA_LANG_OBJECT.equals(containingClass.getQualifiedName());
        }
    }
}
