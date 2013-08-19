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
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaSupertypeResolver;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils.erasure;

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
    static List<JavaMethod> getSuperMethods(@NotNull JavaMethod method) {
        return new SuperMethodCollector(method).collect();
    }

    private PropagationHeuristics() {
    }

    private static class SuperMethodCollector {
        private final JavaMethod initialMethod;
        private final Name initialMethodName;
        private final List<JavaType> initialParametersErasure;

        private final Set<JavaClass> visitedSuperclasses = Sets.newHashSet();
        private final List<JavaMethod> collectedMethods = Lists.newArrayList();

        private SuperMethodCollector(@NotNull JavaMethod initialMethod) {
            this.initialMethod = initialMethod;
            initialMethodName = initialMethod.getName();

            Collection<JavaValueParameter> valueParameters = initialMethod.getValueParameters();
            initialParametersErasure = Lists.newArrayListWithExpectedSize(valueParameters.size());
            for (JavaValueParameter parameter : valueParameters) {
                initialParametersErasure.add(erasure(varargToArray(parameter.getType(), parameter.isVararg())));
            }
        }

        @NotNull
        public List<JavaMethod> collect() {
            if (!canHaveSuperMethod(initialMethod)) {
                return Collections.emptyList();
            }

            for (JavaClassifierType supertype : initialMethod.getContainingClass().getSupertypes()) {
                collectFromSupertype(supertype);
            }

            return collectedMethods;
        }

        private void collectFromSupertype(@NotNull JavaClassifierType type) {
            JavaClassifier classifier = type.getClassifier();
            if (!(classifier instanceof JavaClass)) return;

            JavaClass klass = (JavaClass) classifier;
            if (!visitedSuperclasses.add(klass)) return;

            JavaTypeSubstitutor supertypeSubstitutor = getErasedSubstitutor(type);
            for (JavaMethod methodFromSuper : klass.getMethods()) {
                if (isSubMethodOf(methodFromSuper, supertypeSubstitutor)) {
                    collectedMethods.add(methodFromSuper);
                    return;
                }
            }

            for (JavaClassifierType supertype : type.getSupertypes()) {
                collectFromSupertype(supertype);
            }
        }

        private boolean isSubMethodOf(@NotNull JavaMethod methodFromSuper, @NotNull JavaTypeSubstitutor supertypeSubstitutor) {
            if (!methodFromSuper.getName().equals(initialMethodName)) {
                return false;
            }

            Collection<JavaValueParameter> fromSuperParameters = methodFromSuper.getValueParameters();
            if (fromSuperParameters.size() != initialParametersErasure.size()) {
                return false;
            }

            Iterator<JavaType> originalIterator = initialParametersErasure.iterator();
            Iterator<JavaValueParameter> superIterator = fromSuperParameters.iterator();
            while (originalIterator.hasNext()) {
                JavaType originalType = originalIterator.next();
                JavaValueParameter parameterFromSuper = superIterator.next();

                JavaType typeFromSuper = erasure(varargToArray(
                        supertypeSubstitutor.substitute(parameterFromSuper.getType()),
                        parameterFromSuper.isVararg()
                ));

                if (!Comparing.equal(originalType, typeFromSuper)) {
                    return false;
                }
            }

            return true;
        }

        @NotNull
        private static JavaType varargToArray(@NotNull JavaType type, boolean isVararg) {
            return isVararg ? JavaElementFactory.getInstance().createArrayType(((JavaArrayType) type).getComponentType()) : type;
        }

        @NotNull
        private static JavaTypeSubstitutor getErasedSubstitutor(@NotNull JavaClassifierType type) {
            Map<JavaTypeParameter, JavaType> unerasedMap = type.getSubstitutor().getSubstitutionMap();
            Map<JavaTypeParameter, JavaType> erasedMap = Maps.newHashMapWithExpectedSize(unerasedMap.size());
            for (Map.Entry<JavaTypeParameter, JavaType> entry : unerasedMap.entrySet()) {
                JavaType value = entry.getValue();
                erasedMap.put(entry.getKey(), value == null ? null : erasure(value));
            }
            return JavaTypeSubstitutorImpl.create(erasedMap);
        }

        private static boolean canHaveSuperMethod(@NotNull JavaMethod method) {
            return !method.isConstructor() &&
                   !method.isStatic() &&
                   method.getVisibility() != Visibilities.PRIVATE &&
                   !JavaSupertypeResolver.OBJECT_FQ_NAME.equals(method.getContainingClass().getFqName());
        }
    }
}
