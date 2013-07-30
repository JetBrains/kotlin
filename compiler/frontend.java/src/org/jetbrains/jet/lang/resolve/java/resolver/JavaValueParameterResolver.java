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

import com.intellij.psi.PsiAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.TypeUsage;
import org.jetbrains.jet.lang.resolve.java.TypeVariableResolver;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.*;

public final class JavaValueParameterResolver {
    private JavaTypeTransformer typeTransformer;

    public JavaValueParameterResolver() {
    }

    @NotNull
    private ValueParameterDescriptor resolveParameterDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            int i,
            @NotNull JavaValueParameter parameter,
            @NotNull TypeVariableResolver typeVariableResolver,
            boolean isVararg
    ) {
        // TODO: must be very slow, make it lazy?
        Name name = getParameterName(i, parameter);

        TypeUsage typeUsage = JavaTypeTransformer
                .adjustTypeUsageWithMutabilityAnnotations(parameter.getPsi(), TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT);
        JetType outType = typeTransformer.transformToType(parameter.getType(), typeUsage, typeVariableResolver);

        JetType varargElementType;
        if (isVararg) {
            // TODO: test this code
            varargElementType = KotlinBuiltIns.getInstance().getArrayElementType(TypeUtils.makeNotNullable(outType));
            outType = TypeUtils.makeNotNullable(outType);
        }
        else {
            varargElementType = null;
        }

        JetType transformedType;
        PsiAnnotation notNullAnnotation = JavaAnnotationResolver
                .findAnnotationWithExternal(parameter.getPsi(), JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION);
        if (notNullAnnotation != null) {
            transformedType = TypeUtils.makeNullableAsSpecified(outType, false);
        }
        else {
            transformedType = outType;
        }
        return new ValueParameterDescriptorImpl(
                containingDeclaration,
                i,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                name,
                transformedType,
                false,
                varargElementType
        );
    }

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @NotNull
    private static Name getParameterName(int number, @NotNull JavaValueParameter parameter) {
        Name psiParameterName = parameter.getName();
        return psiParameterName != null ? psiParameterName : Name.identifier("p" + number);
    }

    @NotNull
    public JavaDescriptorResolver.ValueParameterDescriptors resolveParameterDescriptors(
            @NotNull DeclarationDescriptor container,
            @NotNull JavaMethod method,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        Collection<JavaValueParameter> parameters = method.getValueParameters();
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(parameters.size());
        int index = 0;
        for (Iterator<JavaValueParameter> iterator = parameters.iterator(); iterator.hasNext(); ) {
            JavaValueParameter parameter = iterator.next();

            boolean isVararg = method.isVararg() && !iterator.hasNext();
            ValueParameterDescriptor descriptor = resolveParameterDescriptor(container, index, parameter, typeVariableResolver, isVararg);
            result.add(descriptor);
            index++;
        }
        return new JavaDescriptorResolver.ValueParameterDescriptors(null, result);
    }
}