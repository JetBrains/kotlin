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

import com.google.common.collect.Sets;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorForObjectImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.JavaBindingContext;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeFieldSignatureData;
import org.jetbrains.jet.lang.resolve.java.scope.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JavaPropertyResolver {
    private JavaTypeTransformer typeTransformer;
    private BindingTrace trace;
    private JavaAnnotationResolver annotationResolver;

    public JavaPropertyResolver() {
    }

    @Inject
    public void setTypeTransformer(@NotNull JavaTypeTransformer javaTypeTransformer) {
        this.typeTransformer = javaTypeTransformer;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setAnnotationResolver(JavaAnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @NotNull
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor ownerDescriptor) {
        Name propertyName = members.getName();

        List<JavaField> fields = members.getFields();

        Set<PropertyDescriptor> propertiesFromCurrent = new HashSet<PropertyDescriptor>(1);
        assert fields.size() <= 1;
        if (fields.size() == 1) {
            JavaField field = fields.iterator().next();
            if (DescriptorResolverUtils.isCorrectOwnerForEnumMember(ownerDescriptor, field)) {
                propertiesFromCurrent.add(resolveProperty(ownerDescriptor, propertyName, field));
            }
        }

        Set<PropertyDescriptor> propertiesFromSupertypes = getPropertiesFromSupertypes(propertyName, ownerDescriptor);
        Set<PropertyDescriptor> properties = Sets.newHashSet();

        generateOverrides(ownerDescriptor, propertyName, propertiesFromCurrent, propertiesFromSupertypes, properties);
        OverrideResolver.resolveUnknownVisibilities(properties, trace);

        properties.addAll(propertiesFromCurrent);

        return Sets.<VariableDescriptor>newHashSet(properties);
    }

    private static void generateOverrides(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull Name propertyName,
            @NotNull Set<PropertyDescriptor> propertiesFromCurrent,
            @NotNull Set<PropertyDescriptor> propertiesFromSupertypes,
            @NotNull final Set<PropertyDescriptor> properties
    ) {
        if (!(owner instanceof ClassDescriptor)) {
            return;
        }
        ClassDescriptor classDescriptor = (ClassDescriptor) owner;

        OverrideResolver.generateOverridesInFunctionGroup(
                propertyName, propertiesFromSupertypes, propertiesFromCurrent, classDescriptor,
                new OverrideResolver.DescriptorSink() {
                    @Override
                    public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                        properties.add((PropertyDescriptor) fakeOverride);
                    }

                    @Override
                    public void conflict(
                            @NotNull CallableMemberDescriptor fromSuper,
                            @NotNull CallableMemberDescriptor fromCurrent
                    ) {
                        // nop
                    }
                });
    }

    @NotNull
    private PropertyDescriptor resolveProperty(@NotNull ClassOrNamespaceDescriptor owner, @NotNull Name name, @NotNull JavaField field) {
        boolean isVar = !field.isFinal();

        PropertyDescriptorImpl propertyDescriptor = createPropertyDescriptor(owner, name, field, isVar);
        propertyDescriptor.initialize(null, null);

        TypeVariableResolver typeVariableResolver =
                new TypeVariableResolver(Collections.<TypeParameterDescriptor>emptyList(), propertyDescriptor);

        JetType propertyType = getPropertyType(field, typeVariableResolver);

        propertyType = getAlternativeSignatureData(isVar, field, propertyDescriptor, propertyType);

        propertyDescriptor.setType(
                propertyType,
                Collections.<TypeParameterDescriptor>emptyList(),
                DescriptorUtils.getExpectedThisObjectIfNeeded(owner),
                (JetType) null
        );

        trace.record(BindingContext.VARIABLE, field.getPsi(), propertyDescriptor);

        trace.record(JavaBindingContext.IS_DECLARED_IN_JAVA, propertyDescriptor);

        if (AnnotationUtils.isPropertyAcceptableAsAnnotationParameter(propertyDescriptor)) {
            PsiExpression initializer = field.getPsi().getInitializer();
            if (initializer instanceof PsiLiteralExpression) {
                CompileTimeConstant<?> constant = JavaAnnotationArgumentResolver
                        .resolveCompileTimeConstantValue(((PsiLiteralExpression) initializer).getValue(), propertyType);
                if (constant != null) {
                    trace.record(BindingContext.COMPILE_TIME_INITIALIZER, propertyDescriptor, constant);
                }
            }
        }
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
            return new PropertyDescriptorForObjectImpl(
                    owner,
                    annotations,
                    visibility,
                    propertyName,
                    dummyClassDescriptorForEnumEntryObject);
        }
        return new PropertyDescriptorImpl(
                owner,
                annotations,
                Modality.FINAL,
                visibility,
                isVar,
                propertyName,
                CallableMemberDescriptor.Kind.DECLARATION);
    }

    @NotNull
    private JetType getAlternativeSignatureData(
            boolean isVar,
            @NotNull JavaField field,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetType propertyType
    ) {
        AlternativeFieldSignatureData signatureData = new AlternativeFieldSignatureData(field, propertyType, isVar);
        if (signatureData.hasErrors()) {
            trace.record(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, propertyDescriptor,
                         Collections.singletonList(signatureData.getError()));
        }
        else if (signatureData.isAnnotated()) {
            return signatureData.getReturnType();
        }
        return propertyType;
    }

    @NotNull
    private JetType getPropertyType(@NotNull JavaField field, @NotNull TypeVariableResolver typeVariableResolver) {
        JetType propertyType = typeTransformer.transformToType(field.getType(), typeVariableResolver);

        if (JavaAnnotationResolver.hasNotNullAnnotation(field) || isStaticFinalField(field) /* TODO: WTF? */) {
            return TypeUtils.makeNotNullable(propertyType);
        }

        return propertyType;
    }

    @NotNull
    private static Set<PropertyDescriptor> getPropertiesFromSupertypes(
            @NotNull Name propertyName, @NotNull ClassOrNamespaceDescriptor ownerDescriptor
    ) {
        Set<PropertyDescriptor> r = new HashSet<PropertyDescriptor>();
        for (JetType supertype : DescriptorResolverUtils.getSupertypes(ownerDescriptor)) {
            for (VariableDescriptor property : supertype.getMemberScope().getProperties(propertyName)) {
                r.add((PropertyDescriptor) property);
            }
        }
        return r;
    }

    private static boolean isStaticFinalField(@NotNull JavaField field) {
        return field.isFinal() && field.isStatic();
    }
}
