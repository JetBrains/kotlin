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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifier;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public final class JavaSupertypeResolver {
    public static final FqName OBJECT_FQ_NAME = new FqName("java.lang.Object");

    private JavaTypeTransformer typeTransformer;
    private JavaClassResolver classResolver;

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @NotNull
    public Collection<JetType> getSupertypes(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JavaClass javaClass,
            @NotNull List<TypeParameterDescriptor> typeParameters
    ) {
        TypeVariableResolver typeVariableResolver = new TypeVariableResolverImpl(typeParameters, classDescriptor);

        List<JetType> result = transformSupertypeList(javaClass.getSupertypes(), typeVariableResolver);

        return result.isEmpty() ? Collections.singletonList(getDefaultSupertype(javaClass)) : result;
    }

    @NotNull
    private JetType getDefaultSupertype(@NotNull JavaClass javaClass) {
        if (OBJECT_FQ_NAME.equals(javaClass.getFqName()) || javaClass.isAnnotationType()) {
            return KotlinBuiltIns.getInstance().getAnyType();
        }
        else {
            ClassDescriptor object = classResolver.resolveClass(OBJECT_FQ_NAME, IGNORE_KOTLIN_SOURCES);
            if (object != null) {
                return object.getDefaultType();
            }
            else {
                //TODO: hack here
                return KotlinBuiltIns.getInstance().getAnyType();
                // throw new IllegalStateException("Could not resolve java.lang.Object");
            }
        }
    }

    @NotNull
    private List<JetType> transformSupertypeList(
            @NotNull Collection<JavaClassifierType> supertypes,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        List<JetType> result = new ArrayList<JetType>(supertypes.size());
        for (JavaClassifierType type : supertypes) {
            JavaClassifier resolved = type.getClassifier();
            if (resolved != null) {
                assert resolved instanceof JavaClass : "Supertype should be a class: " + resolved;
                FqName fqName = ((JavaClass) resolved).getFqName();
                assert fqName != null : "Unresolved supertype: " + resolved;
                if (JvmAbi.JET_OBJECT.getFqName().equals(fqName)) {
                    continue;
                }
            }

            JetType transformed = typeTransformer.transformToType(type, TypeUsage.SUPERTYPE, typeVariableResolver);
            if (transformed.isError()) {
                // TODO: report INCOMPLETE_HIERARCHY
            }
            else {
                result.add(TypeUtils.makeNotNullable(transformed));
            }
        }
        return result;
    }
}
