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
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPropertyDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPropertyDescriptorForObject;
import org.jetbrains.jet.lang.resolve.java.scope.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import javax.inject.Inject;
import java.util.*;

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
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor owner) {
        Name propertyName = members.getName();

        List<JavaField> fields = members.getFields();

        Set<PropertyDescriptor> propertiesFromCurrent = new HashSet<PropertyDescriptor>(1);
        assert fields.size() <= 1;
        if (fields.size() == 1) {
            JavaField field = fields.iterator().next();
            if (DescriptorResolverUtils.isCorrectOwnerForEnumMember(owner, field)) {
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
    private PropertyDescriptor resolveProperty(@NotNull ClassOrNamespaceDescriptor owner, @NotNull Name name, @NotNull JavaField field) {
        boolean isVar = !field.isFinal();

        PropertyDescriptorImpl propertyDescriptor = createPropertyDescriptor(owner, name, field, isVar);
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
    private PropertyDescriptorImpl createPropertyDescriptor(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull Name propertyName,
            @NotNull JavaField field,
            boolean isVar
    ) {
        List<AnnotationDescriptor> annotations = annotationResolver.resolveAnnotations(field);
        Visibility visibility = field.getVisibility();

        if (field.isEnumEntry()) {
            assert !isVar : "Enum entries should be immutable.";
            assert DescriptorUtils.isEnumClassObject(owner) : "Enum entries should be put into class object of enum only: " + owner;
            //TODO: this is a hack to indicate that this enum entry is an object
            // class descriptor for enum entries is not used by backends so for now this should be safe to use
            ClassDescriptorImpl dummyClassDescriptorForEnumEntryObject =
                    new ClassDescriptorImpl(owner, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, propertyName);
            dummyClassDescriptorForEnumEntryObject.initialize(
                    true,
                    Collections.<TypeParameterDescriptor>emptyList(),
                    Collections.<JetType>emptyList(), JetScope.EMPTY,
                    Collections.<ConstructorDescriptor>emptySet(), null,
                    false);
            return new JavaPropertyDescriptorForObject(owner, annotations, visibility, propertyName, dummyClassDescriptorForEnumEntryObject);
        }

        return new JavaPropertyDescriptor(owner, annotations, visibility, isVar, propertyName);
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
    private static Set<PropertyDescriptor> getPropertiesFromSupertypes(@NotNull Name name, @NotNull ClassDescriptor descriptor) {
        Set<PropertyDescriptor> result = new HashSet<PropertyDescriptor>();
        for (JetType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            for (VariableDescriptor property : supertype.getMemberScope().getProperties(name)) {
                result.add((PropertyDescriptor) property);
            }
        }

        return result;
    }

    private static boolean isStaticFinalField(@NotNull JavaField field) {
        return field.isFinal() && field.isStatic();
    }
}
