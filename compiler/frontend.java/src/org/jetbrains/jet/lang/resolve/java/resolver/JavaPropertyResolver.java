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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeFieldSignatureData;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.kt.JetMethodAnnotation;
import org.jetbrains.jet.lang.resolve.java.provider.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.wrapper.PropertyPsiData;
import org.jetbrains.jet.lang.resolve.java.wrapper.PropertyPsiDataElement;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiFieldWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.JAVA;

public final class JavaPropertyResolver {

    private JavaSemanticServices semanticServices;
    private JavaSignatureResolver javaSignatureResolver;
    private BindingTrace trace;
    private JavaAnnotationResolver annotationResolver;

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

    @NotNull
    public Set<VariableDescriptor> resolveFieldGroupByName(
            @NotNull Name fieldName,
            @NotNull PsiDeclarationProvider scopeData,
            @NotNull ClassOrNamespaceDescriptor ownerDescriptor
    ) {
        NamedMembers namedMembers = scopeData.getMembersCache().get(fieldName);
        if (namedMembers == null) {
            return Collections.emptySet();
        }

        return resolveNamedGroupProperties(ownerDescriptor, scopeData, namedMembers, fieldName,
                                           "class or namespace " + DescriptorUtils.getFQName(ownerDescriptor));
    }

    @NotNull
    private Set<VariableDescriptor> resolveNamedGroupProperties(
            @NotNull ClassOrNamespaceDescriptor ownerDescriptor,
            @NotNull PsiDeclarationProvider scopeData,
            @NotNull NamedMembers namedMembers,
            @NotNull Name propertyName,
            @NotNull String context
    ) {
        Collection<PropertyPsiData> psiDataCollection = PropertyPsiData.assemblePropertyPsiDataFromElements(
                namedMembers.getPropertyPsiDataElements());

        Set<PropertyDescriptor> propertiesFromCurrent = new HashSet<PropertyDescriptor>(1);

        int regularPropertiesCount = getNumberOfNonExtensionProperties(psiDataCollection);

        for (PropertyPsiData propertyPsiData : psiDataCollection) {

            // we cannot have more then one property with given name even if java code
            // has several fields, getters and setter of different types
            if (!propertyPsiData.isExtension() && regularPropertiesCount > 1) {
                continue;
            }

            propertiesFromCurrent.add(resolveProperty(ownerDescriptor, scopeData, propertyName, context, propertyPsiData));
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
    private PropertyDescriptor resolveProperty(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull PsiDeclarationProvider scopeData,
            @NotNull Name propertyName,
            @NotNull String context,
            @NotNull PropertyPsiData psiData
    ) {
        boolean isFinal = isPropertyFinal(scopeData, psiData);
        boolean isVar = psiData.isVar();

        PropertyPsiDataElement characteristicMember = psiData.getCharacteristicMember();

        Visibility visibility = DescriptorResolverUtils.resolveVisibility(psiData.getCharacteristicPsi(), null);
        CallableMemberDescriptor.Kind kind = CallableMemberDescriptor.Kind.DECLARATION;

        PropertyPsiDataElement getter = psiData.getGetter();
        if (getter != null) {
            JetMethodAnnotation methodAnnotation = ((PsiMethodWrapper) getter.getMember()).getJetMethodAnnotation();
            visibility = DescriptorResolverUtils.resolveVisibility(psiData.getCharacteristicPsi(), methodAnnotation);
            kind = DescriptorKindUtils.flagsToKind(methodAnnotation.kind());
        }

        boolean isEnumEntry = DescriptorUtils.isEnumClassObject(owner);
        PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(
                owner,
                annotationResolver.resolveAnnotations(psiData.getCharacteristicPsi()),
                DescriptorResolverUtils.resolveModality(characteristicMember.getMember(),
                                                        isFinal || isEnumEntry || psiData.isPropertyForNamedObject()),
                visibility,
                isVar,
                propertyName,
                kind);

        //TODO: this is a hack to indicate that this enum entry is an object
        // class descriptor for enum entries is not used by backends so for now this should be safe to use
        // remove this when JavaDescriptorResolver gets rewritten
        if (isEnumEntry) {
            ClassDescriptorImpl dummyClassDescriptorForEnumEntryObject =
                    new ClassDescriptorImpl(owner, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, propertyName);
            dummyClassDescriptorForEnumEntryObject.initialize(
                    true,
                    Collections.<TypeParameterDescriptor>emptyList(),
                    Collections.<JetType>emptyList(), JetScope.EMPTY,
                    Collections.<ConstructorDescriptor>emptySet(), null,
                    false);
            trace.record(BindingContext.OBJECT_DECLARATION_CLASS, propertyDescriptor, dummyClassDescriptorForEnumEntryObject);
        }

        PropertyGetterDescriptorImpl getterDescriptor = resolveGetter(visibility, kind, getter, propertyDescriptor);
        PropertySetterDescriptorImpl setterDescriptor = resolveSetter(psiData, kind, propertyDescriptor);

        propertyDescriptor.initialize(getterDescriptor, setterDescriptor);

        List<TypeParameterDescriptor> typeParameters = resolvePropertyTypeParameters(psiData, characteristicMember, propertyDescriptor);

        TypeVariableResolver typeVariableResolverForPropertyInternals = TypeVariableResolvers.typeVariableResolverFromTypeParameters(
                typeParameters, propertyDescriptor, "property " + propertyName + " in " + context);

        JetType propertyType = getPropertyType(psiData, characteristicMember, typeVariableResolverForPropertyInternals);
        JetType receiverType = getReceiverType(characteristicMember, typeVariableResolverForPropertyInternals);


        propertyType = getAlternativeSignatureData(isVar, characteristicMember, propertyDescriptor, propertyType);

        propertyDescriptor.setType(
                propertyType,
                typeParameters,
                DescriptorUtils.getExpectedThisObjectIfNeeded(owner),
                receiverType
        );
        initializeSetterAndGetter(propertyDescriptor, getterDescriptor, setterDescriptor, propertyType);

        if (kind == CallableMemberDescriptor.Kind.DECLARATION) {
            trace.record(BindingContext.VARIABLE, psiData.getCharacteristicPsi(), propertyDescriptor);
        }

        recordObjectDeclarationClassIfNeeded(psiData, owner, propertyDescriptor, propertyType);

        if (scopeData.getDeclarationOrigin() == JAVA) {
            trace.record(BindingContext.IS_DECLARED_IN_JAVA, propertyDescriptor);
        }
        return propertyDescriptor;
    }

    @NotNull
    private JetType getAlternativeSignatureData(
            boolean isVar,
            PropertyPsiDataElement characteristicMember,
            PropertyDescriptor propertyDescriptor,
            JetType propertyType
    ) {
        if (!characteristicMember.isField()) {
            return propertyType;
        }
        AlternativeFieldSignatureData signatureData =
                new AlternativeFieldSignatureData((PsiFieldWrapper) characteristicMember.getMember(), propertyType, isVar);
        if (!signatureData.hasErrors()) {
            if (signatureData.isAnnotated()) {
                return signatureData.getReturnType();
            }
        }
        else {
            trace.record(BindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, propertyDescriptor,
                         Collections.singletonList(signatureData.getError()));
        }
        return propertyType;
    }

    private static void initializeSetterAndGetter(
            PropertyDescriptor propertyDescriptor,
            PropertyGetterDescriptorImpl getterDescriptor,
            PropertySetterDescriptorImpl setterDescriptor,
            JetType propertyType
    ) {
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
    }

    private void recordObjectDeclarationClassIfNeeded(
            PropertyPsiData psiData,
            DeclarationDescriptor realOwner,
            PropertyDescriptor propertyDescriptor,
            JetType propertyType
    ) {
        if (!psiData.isPropertyForNamedObject()) {
            return;
        }
        ClassDescriptor objectDescriptor = (ClassDescriptor) propertyType.getConstructor().getDeclarationDescriptor();

        assert objectDescriptor != null;
        assert objectDescriptor.getKind() == ClassKind.OBJECT;
        assert objectDescriptor.getContainingDeclaration() == realOwner;

        trace.record(BindingContext.OBJECT_DECLARATION_CLASS, propertyDescriptor, objectDescriptor);
    }

    @Nullable
    private PropertyGetterDescriptorImpl resolveGetter(
            Visibility visibility,
            CallableMemberDescriptor.Kind kind,
            PropertyPsiDataElement getter,
            PropertyDescriptor propertyDescriptor
    ) {
        if (getter == null) {
            return null;
        }
        return new PropertyGetterDescriptorImpl(
                propertyDescriptor,
                annotationResolver.resolveAnnotations(getter.getMember().getPsiMember()),
                Modality.OPEN,
                visibility,
                true,
                false,
                kind);
    }

    @Nullable
    private PropertySetterDescriptorImpl resolveSetter(
            PropertyPsiData psiData,
            CallableMemberDescriptor.Kind kind,
            PropertyDescriptor propertyDescriptor
    ) {
        PropertyPsiDataElement setter = psiData.getSetter();
        if (setter == null) {
            return null;
        }
        Visibility setterVisibility = DescriptorResolverUtils.resolveVisibility(setter.getMember().getPsiMember(), null);
        if (setter.getMember() instanceof PsiMethodWrapper) {
            setterVisibility = DescriptorResolverUtils.resolveVisibility(
                    setter.getMember().getPsiMember(),
                    ((PsiMethodWrapper) setter.getMember())
                            .getJetMethodAnnotation());
        }
        return new PropertySetterDescriptorImpl(
                propertyDescriptor,
                annotationResolver.resolveAnnotations(setter.getMember().getPsiMember()),
                Modality.OPEN,
                setterVisibility,
                true,
                false,
                kind);
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

    @NotNull
    private JetType getPropertyType(
            PropertyPsiData members,
            PropertyPsiDataElement characteristicMember,
            TypeVariableResolver typeVariableResolverForPropertyInternals
    ) {
        if (!characteristicMember.getType().getTypeString().isEmpty()) {
            return semanticServices.getTypeTransformer().transformToType(
                    characteristicMember.getType().getTypeString(), typeVariableResolverForPropertyInternals);
        }
        JetType propertyType = semanticServices.getTypeTransformer().transformToType(
                characteristicMember.getType().getPsiType(), typeVariableResolverForPropertyInternals);

        boolean hasNotNullAnnotation = JavaAnnotationResolver.findAnnotationWithExternal(
                characteristicMember.getType().getPsiNotNullOwner(),
                JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null;

        if (hasNotNullAnnotation || members.isStaticFinalField()) {
            propertyType = TypeUtils.makeNotNullable(propertyType);
        }
        return propertyType;
    }

    @Nullable
    private JetType getReceiverType(
            PropertyPsiDataElement characteristicMember,
            TypeVariableResolver typeVariableResolverForPropertyInternals
    ) {
        if (characteristicMember.getReceiverType() == null) {
            return null;
        }
        if (!characteristicMember.getReceiverType().getTypeString().isEmpty()) {
            return semanticServices.getTypeTransformer().transformToType(characteristicMember.getReceiverType().getTypeString(), typeVariableResolverForPropertyInternals);
        }
        return semanticServices.getTypeTransformer().transformToType(characteristicMember.getReceiverType().getPsiType(), typeVariableResolverForPropertyInternals);
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

    private static boolean isPropertyFinal(PsiDeclarationProvider scopeData, PropertyPsiData psiData) {
        if (scopeData.getDeclarationOrigin() == JAVA) {
            return true;
        }
        return psiData.isFinal();
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
}
