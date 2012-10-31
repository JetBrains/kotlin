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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.data.ResolverScopeData;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeFieldSignatureData;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.kt.JetMethodAnnotation;
import org.jetbrains.jet.lang.resolve.java.wrapper.PropertyPsiData;
import org.jetbrains.jet.lang.resolve.java.wrapper.PropertyPsiDataElement;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiFieldWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import javax.inject.Inject;
import java.util.*;

public final class JavaPropertyResolver {

    private JavaSemanticServices semanticServices;
    private JavaSignatureResolver javaSignatureResolver;
    private BindingTrace trace;
    private JavaAnnotationResolver annotationResolver;
    private JavaClassResolver classResolver;

    public JavaPropertyResolver() {
    }

    @Inject
    public void setSemanticServices(JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setJavaSignatureResolver(JavaSignatureResolver javaSignatureResolver) {
        this.javaSignatureResolver = javaSignatureResolver;
    }

    @Inject
    public void setAnnotationResolver(JavaAnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @NotNull
    public Set<VariableDescriptor> resolveFieldGroupByName(
            @NotNull Name fieldName,
            @NotNull ResolverScopeData scopeData
    ) {

        PsiClass psiClass = scopeData.getPsiClass();

        NamedMembers namedMembers = scopeData.getMembersCache().get(fieldName);
        if (namedMembers == null) {
            return Collections.emptySet();
        }

        //noinspection ConstantConditions
        String qualifiedName = psiClass == null ? scopeData.getPsiPackage().getQualifiedName() : psiClass.getQualifiedName();
        return resolveNamedGroupProperties(scopeData.getClassOrNamespaceDescriptor(), scopeData, namedMembers, fieldName,
                                    "class or namespace " + qualifiedName);
    }

    @NotNull
    private Set<VariableDescriptor> resolveNamedGroupProperties(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull ResolverScopeData scopeData,
            @NotNull NamedMembers namedMembers,
            @NotNull Name propertyName,
            @NotNull String context
    ) {
        Collection<PropertyPsiData> psiDataCollection = PropertyPsiData.collectGroupingValuesFromAccessors(namedMembers.getPropertyPsiDataElements());

        Set<PropertyDescriptor> propertiesFromCurrent = new HashSet<PropertyDescriptor>(1);

        int regularPropertiesCount = getNumberOfNonExtensionProperties(psiDataCollection);

        for (PropertyPsiData members : psiDataCollection) {

            // we cannot have more then one property with given name even if java code
            // has several fields, getters and setter of different types
            if (!members.isExtension() && regularPropertiesCount > 1) {
                continue;
            }

            boolean isFinal = isPropertyFinal(scopeData, members);
            boolean isVar = members.isVar();

            PropertyPsiDataElement characteristicMember = members.getCharacteristicMember();

            Visibility visibility = DescriptorResolverUtils.resolveVisibility(members.getCharacteristicPsi(), null);
            CallableMemberDescriptor.Kind kind = CallableMemberDescriptor.Kind.DECLARATION;

            PropertyPsiDataElement getter = members.getGetter();
            if (getter != null) {
                JetMethodAnnotation jetMethod = ((PsiMethodWrapper) getter.getMember()).getJetMethodAnnotation();
                visibility = DescriptorResolverUtils.resolveVisibility(members.getCharacteristicPsi(), jetMethod);
                kind = DescriptorKindUtils.flagsToKind(jetMethod.kind());
            }

            DeclarationDescriptor realOwner = getRealOwner(owner, scopeData, members.isStatic());
            boolean isEnumEntry = DescriptorUtils.isEnumClassObject(realOwner);
            boolean isPropertyForNamedObject = members.getField() != null && JvmAbi.INSTANCE_FIELD.equals(members.getField().getMember().getName());
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                    realOwner,
                    annotationResolver.resolveAnnotations(characteristicMember.getMember().getPsiMember()),
                    DescriptorResolverUtils
                            .resolveModality(characteristicMember.getMember(), isFinal || isEnumEntry || isPropertyForNamedObject),
                    visibility,
                    isVar,
                    propertyName,
                    kind);

            //TODO: this is a hack to indicate that this enum entry is an object
            // class descriptor for enum entries is not used by backends so for now this should be safe to use
            // remove this when JavaDescriptorResolver gets rewritten
            if (isEnumEntry) {
                ClassDescriptorImpl dummyClassDescriptorForEnumEntryObject =
                        new ClassDescriptorImpl(realOwner, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, propertyName);
                dummyClassDescriptorForEnumEntryObject.initialize(
                        true,
                        Collections.<TypeParameterDescriptor>emptyList(),
                        Collections.<JetType>emptyList(), JetScope.EMPTY,
                        Collections.<ConstructorDescriptor>emptySet(), null);
                trace.record(BindingContext.OBJECT_DECLARATION_CLASS, propertyDescriptor, dummyClassDescriptorForEnumEntryObject);
            }

            PropertyGetterDescriptor getterDescriptor = null;
            PropertySetterDescriptor setterDescriptor = null;

            if (getter != null) {
                getterDescriptor = new PropertyGetterDescriptor(
                        propertyDescriptor,
                        annotationResolver.resolveAnnotations(getter.getMember().getPsiMember()),
                        Modality.OPEN,
                        visibility,
                        true,
                        false,
                        kind);
            }

            PropertyPsiDataElement setter = members.getSetter();
            if (setter != null) {
                Visibility setterVisibility = DescriptorResolverUtils.resolveVisibility(setter.getMember().getPsiMember(), null);
                if (setter.getMember() instanceof PsiMethodWrapper) {
                    setterVisibility = DescriptorResolverUtils.resolveVisibility(
                            setter.getMember().getPsiMember(),
                            ((PsiMethodWrapper) setter.getMember())
                                    .getJetMethodAnnotation());
                }
                setterDescriptor = new PropertySetterDescriptor(
                        propertyDescriptor,
                        annotationResolver.resolveAnnotations(setter.getMember().getPsiMember()),
                        Modality.OPEN,
                        setterVisibility,
                        true,
                        false,
                        kind);
            }

            propertyDescriptor.initialize(getterDescriptor, setterDescriptor);

            List<TypeParameterDescriptor> typeParameters = resolvePropertyTypeParameters(members, characteristicMember, propertyDescriptor);

            TypeVariableResolver typeVariableResolverForPropertyInternals = TypeVariableResolvers.typeVariableResolverFromTypeParameters(
                    typeParameters, propertyDescriptor, "property " + propertyName + " in " + context);

            JetType propertyType = getPropertyType(members, characteristicMember, typeVariableResolverForPropertyInternals);
            JetType receiverType = getReceiverType(characteristicMember, typeVariableResolverForPropertyInternals);


            if (characteristicMember.isField()) {
                AlternativeFieldSignatureData signatureData =
                        new AlternativeFieldSignatureData((PsiFieldWrapper) characteristicMember.getMember(), propertyType, isVar);
                if (!signatureData.hasErrors()) {
                    if (signatureData.isAnnotated()) {
                        propertyType = signatureData.getReturnType();
                    }
                }
                else {
                    trace.record(BindingContext.ALTERNATIVE_SIGNATURE_DATA_ERROR, propertyDescriptor, signatureData.getError());
                }
            }

            propertyDescriptor.setType(
                    propertyType,
                    typeParameters,
                    DescriptorUtils.getExpectedThisObjectIfNeeded(realOwner),
                    receiverType
            );
            if (getterDescriptor != null) {
                getterDescriptor.initialize(propertyType);
            }
            if (setterDescriptor != null) {
                setterDescriptor.initialize(new ValueParameterDescriptorImpl(
                        setterDescriptor,
                        0,
                        Collections.<AnnotationDescriptor>emptyList(),
                        Name.identifier("p0") /*TODO*/,
                        false,
                        propertyDescriptor.getType(),
                        false,
                        null));
            }

            if (kind == CallableMemberDescriptor.Kind.DECLARATION) {
                trace.record(BindingContext.VARIABLE, characteristicMember.getMember().getPsiMember(), propertyDescriptor);
            }

            if (isPropertyForNamedObject) {
                ClassDescriptor objectDescriptor = (ClassDescriptor) propertyType.getConstructor().getDeclarationDescriptor();

                assert objectDescriptor.getKind() == ClassKind.OBJECT;
                assert objectDescriptor.getContainingDeclaration() == realOwner;

                trace.record(BindingContext.OBJECT_DECLARATION_CLASS, propertyDescriptor, objectDescriptor);
            }

            if (!scopeData.isKotlin()) {
                trace.record(BindingContext.IS_DECLARED_IN_JAVA, propertyDescriptor);
            }

            propertiesFromCurrent.add(propertyDescriptor);
        }

        Set<PropertyDescriptor> propertiesFromSupertypes = getPropertiesFromSupertypes(scopeData, propertyName);

        final Set<PropertyDescriptor> properties = Sets.newHashSet();

        if (owner instanceof ClassDescriptor) {
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

        OverrideResolver.resolveUnknownVisibilities(properties, trace);
        properties.addAll(propertiesFromCurrent);

        return Sets.<VariableDescriptor>newHashSet(properties);
    }

    private List<TypeParameterDescriptor> resolvePropertyTypeParameters(
            @NotNull PropertyPsiData members,
            @NotNull PropertyPsiDataElement characteristicMember,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        // TODO: Can't get type parameters from field - only from accessors
        if (characteristicMember == members.getSetter() || characteristicMember == members.getGetter()) {
            PsiMethodWrapper method = (PsiMethodWrapper) characteristicMember.getMember();
            return javaSignatureResolver.resolveMethodTypeParameters(method, propertyDescriptor);
        }

        return Collections.emptyList();
    }

    private JetType getPropertyType(
            PropertyPsiData members,
            PropertyPsiDataElement characteristicMember,
            TypeVariableResolver typeVariableResolverForPropertyInternals
    ) {
        JetType propertyType;
        if (!characteristicMember.getType().getTypeString().isEmpty()) {
            propertyType = semanticServices.getTypeTransformer().transformToType(characteristicMember.getType().getTypeString(), typeVariableResolverForPropertyInternals);
        }
        else {
            propertyType = semanticServices.getTypeTransformer().transformToType(characteristicMember.getType().getPsiType(), typeVariableResolverForPropertyInternals);
            if (JavaAnnotationResolver.findAnnotation(characteristicMember.getType().getPsiNotNullOwner(),
                                                      JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null) {
                propertyType = TypeUtils.makeNullableAsSpecified(propertyType, false);
            }
            else if (members.getGetter() == null && members.getSetter() == null && members.getField().getMember().isFinal() && members.getField().getMember().isStatic()) {
                // http://youtrack.jetbrains.com/issue/KT-1388
                propertyType = TypeUtils.makeNotNullable(propertyType);
            }
        }
        return propertyType;
    }

    @Nullable
    private JetType getReceiverType(
            PropertyPsiDataElement characteristicMember,
            TypeVariableResolver typeVariableResolverForPropertyInternals
    ) {
        JetType receiverType;
        if (characteristicMember.getReceiverType() == null) {
            receiverType = null;
        }
        else if (characteristicMember.getReceiverType().getTypeString().length() > 0) {
            receiverType = semanticServices.getTypeTransformer().transformToType(characteristicMember.getReceiverType().getTypeString(), typeVariableResolverForPropertyInternals);
        }
        else {
            receiverType = semanticServices.getTypeTransformer().transformToType(characteristicMember.getReceiverType().getPsiType(), typeVariableResolverForPropertyInternals);
        }
        return receiverType;
    }

    private static int getNumberOfNonExtensionProperties(@NotNull Collection<PropertyPsiData> propertyPsiDataCollection) {
        int regularPropertiesCount = 0;
        for (PropertyPsiData members : propertyPsiDataCollection) {
            if (!members.isExtension()) {
                ++regularPropertiesCount;
            }
        }
        return regularPropertiesCount;
    }

    private static boolean isPropertyFinal(ResolverScopeData scopeData, PropertyPsiData members) {
        if (!scopeData.isKotlin()) {
            return true;
        }

        if (members.getGetter() != null) {
            return members.getGetter().getMember().isFinal();
        }

        if (members.getSetter() != null) {
            return members.getSetter().getMember().isFinal();
        }

        return false;
    }

    private static Set<PropertyDescriptor> getPropertiesFromSupertypes(ResolverScopeData scopeData, Name propertyName) {
        Set<PropertyDescriptor> r = new HashSet<PropertyDescriptor>();
        for (JetType supertype : DescriptorResolverUtils.getSupertypes(scopeData)) {
            for (VariableDescriptor property : supertype.getMemberScope().getProperties(propertyName)) {
                r.add((PropertyDescriptor) property);
            }
        }
        return r;
    }

    @NotNull
    private ClassOrNamespaceDescriptor getRealOwner(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull ResolverScopeData scopeData,
            boolean isStatic
    ) {
        PsiClass psiClass = scopeData.getPsiClass();
        if (psiClass != null && psiClass.isEnum() && isStatic) {
            final String qualifiedName = psiClass.getQualifiedName();
            assert qualifiedName != null;
            final ClassDescriptor classDescriptor = classResolver.resolveClass(new FqName(qualifiedName));
            assert classDescriptor != null;
            final ClassDescriptor classObjectDescriptor = classDescriptor.getClassObjectDescriptor();
            assert classObjectDescriptor != null;
            return classObjectDescriptor;
        }
        else {
            return owner;
        }
    }
}
