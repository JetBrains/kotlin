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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPropertyDescriptor;
import org.jetbrains.jet.lang.resolve.java.scope.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumClassObject;
import static org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils.resolveOverrides;

public final class JavaPropertyResolver {
    private JavaTypeTransformer typeTransformer;
    private JavaResolverCache cache;
    private JavaAnnotationResolver annotationResolver;
    private ExternalSignatureResolver externalSignatureResolver;
    private ErrorReporter errorReporter;

    @Inject
    public void setTypeTransformer(@NotNull JavaTypeTransformer javaTypeTransformer) {
        this.typeTransformer = javaTypeTransformer;
    }

    @Inject
    public void setCache(JavaResolverCache cache) {
        this.cache = cache;
    }

    @Inject
    public void setAnnotationResolver(JavaAnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setExternalSignatureResolver(ExternalSignatureResolver externalSignatureResolver) {
        this.externalSignatureResolver = externalSignatureResolver;
    }

    @Inject
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @NotNull
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull NamedMembers members, @NotNull ClassOrPackageFragmentDescriptor owner) {
        if (isEnumClassObject(owner)) {
            return Collections.emptySet();
        }

        Name propertyName = members.getName();

        List<JavaField> fields = members.getFields();

        Set<PropertyDescriptor> propertiesFromCurrent = new HashSet<PropertyDescriptor>(1);
        assert fields.size() <= 1;
        if (fields.size() == 1) {
            JavaField field = fields.iterator().next();
            if (!field.isEnumEntry()) {
                propertiesFromCurrent.add(resolveProperty(owner, propertyName, field));
            }
        }

        Set<PropertyDescriptor> properties = new HashSet<PropertyDescriptor>();
        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            Collection<PropertyDescriptor> propertiesFromSupertypes = getPropertiesFromSupertypes(propertyName, classDescriptor);

            properties.addAll(resolveOverrides(propertyName, propertiesFromSupertypes, propertiesFromCurrent, classDescriptor,
                                               errorReporter));
        }

        properties.addAll(propertiesFromCurrent);

        return new HashSet<VariableDescriptor>(properties);
    }

    @NotNull
    private PropertyDescriptor resolveProperty(@NotNull ClassOrPackageFragmentDescriptor owner, @NotNull Name name, @NotNull JavaField field) {
        assert !field.isEnumEntry() : "Enum entries are resolved into classes, not into properties: " + name;

        boolean isVar = !field.isFinal();

        PropertyDescriptorImpl propertyDescriptor =
                new JavaPropertyDescriptor(owner, annotationResolver.resolveAnnotations(field), field.getVisibility(), isVar, name);
        propertyDescriptor.initialize(null, null);

        TypeVariableResolver typeVariableResolver =
                new TypeVariableResolverImpl(Collections.<TypeParameterDescriptor>emptyList(), propertyDescriptor);

        JetType propertyType = getPropertyType(field, typeVariableResolver);

        ExternalSignatureResolver.AlternativeFieldSignature effectiveSignature =
                externalSignatureResolver.resolveAlternativeFieldSignature(field, propertyType, isVar);
        List<String> signatureErrors = effectiveSignature.getErrors();
        if (!signatureErrors.isEmpty()) {
            externalSignatureResolver.reportSignatureErrors(propertyDescriptor, signatureErrors);
        }

        propertyDescriptor.setType(
                effectiveSignature.getReturnType(),
                Collections.<TypeParameterDescriptor>emptyList(),
                DescriptorUtils.getExpectedThisObjectIfNeeded(owner),
                (JetType) null
        );

        cache.recordField(field, propertyDescriptor);

        return propertyDescriptor;
    }

    @NotNull
    private JetType getPropertyType(@NotNull JavaField field, @NotNull TypeVariableResolver typeVariableResolver) {
        JetType propertyType = typeTransformer.transformToType(field.getType(), typeVariableResolver);

        if (annotationResolver.hasNotNullAnnotation(field) || isStaticFinalField(field) /* TODO: WTF? */) {
            return TypeUtils.makeNotNullable(propertyType);
        }

        return propertyType;
    }

    @NotNull
    public static Set<PropertyDescriptor> getPropertiesFromSupertypes(@NotNull Name name, @NotNull ClassDescriptor descriptor) {
        Set<PropertyDescriptor> result = new HashSet<PropertyDescriptor>();
        for (JetType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            for (VariableDescriptor property : supertype.getMemberScope().getProperties(name)) {
                result.add((PropertyDescriptor) property);
            }
        }

        return result;
    }

    public static boolean isStaticFinalField(@NotNull JavaField field) {
        return field.isFinal() && field.isStatic();
    }
}
