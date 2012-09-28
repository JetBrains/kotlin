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

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolveData.ResolverScopeData;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.kt.JetMethodAnnotation;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.*;

public final class JavaDescriptorPropertiesResolver {
    private final JavaDescriptorResolver javaDescriptorResolver;
    private JavaSemanticServices semanticServices;
    private JavaDescriptorSignatureResolver javaDescriptorSignatureResolver;
    private BindingTrace trace;

    /* internal */ JavaDescriptorPropertiesResolver(JavaDescriptorResolver resolver) {
        this.javaDescriptorResolver = resolver;
    }

    /* internal */ void setSemanticServices(JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
    }

    /* internal */ void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    /* internal */ void setJavaDescriptorSignatureResolver(JavaDescriptorSignatureResolver javaDescriptorSignatureResolver) {
        this.javaDescriptorSignatureResolver = javaDescriptorSignatureResolver;
    }

    void resolveNamedGroupProperties(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull ResolverScopeData scopeData,
            @NotNull NamedMembers namedMembers, @NotNull Name propertyName,
            @NotNull String context
    ) {
        JavaDescriptorResolver.getResolverScopeData(scopeData);

        if (namedMembers.propertyDescriptors != null) {
            return;
        }

        if (namedMembers.propertyAccessors == null) {
            namedMembers.propertyAccessors = Collections.emptyList();
        }

        class GroupingValue {
            PropertyAccessorData getter;
            PropertyAccessorData setter;
            PropertyAccessorData field;
            boolean ext;
        }

        Map<Object, GroupingValue> map = new HashMap<Object, GroupingValue>();

        for (PropertyAccessorData propertyAccessor : namedMembers.propertyAccessors) {

            Object key = propertyKeyForGrouping(propertyAccessor);

            GroupingValue value = map.get(key);
            if (value == null) {
                value = new GroupingValue();
                value.ext = propertyAccessor.getReceiverType() != null;
                map.put(key, value);
            }

            if (value.ext != (propertyAccessor.getReceiverType() != null)) {
                throw new IllegalStateException("internal error, incorrect key");
            }

            if (propertyAccessor.isGetter()) {
                if (value.getter != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.getter = propertyAccessor;
            }
            else if (propertyAccessor.isSetter()) {
                if (value.setter != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.setter = propertyAccessor;
            }
            else if (propertyAccessor.isField()) {
                if (value.field != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.field = propertyAccessor;
            }
            else {
                throw new IllegalStateException();
            }
        }


        Set<PropertyDescriptor> propertiesFromCurrent = new HashSet<PropertyDescriptor>(1);

        int regularProperitesCount = 0;
        for (GroupingValue members : map.values()) {
            if (!members.ext) {
                ++regularProperitesCount;
            }
        }

        for (GroupingValue members : map.values()) {

            // we cannot have more then one property with given name even if java code
            // has several fields, getters and setter of different types
            if (!members.ext && regularProperitesCount > 1) {
                continue;
            }

            boolean isFinal;
            if (!scopeData.kotlin) {
                isFinal = true;
            }
            else if (members.setter == null && members.getter == null) {
                isFinal = false;
            }
            else if (members.getter != null) {
                isFinal = members.getter.getMember().isFinal();
            }
            else if (members.setter != null) {
                isFinal = members.setter.getMember().isFinal();
            }
            else {
                isFinal = false;
            }

            PropertyAccessorData anyMember;
            if (members.getter != null) {
                anyMember = members.getter;
            }
            else if (members.field != null) {
                anyMember = members.field;
            }
            else if (members.setter != null) {
                anyMember = members.setter;
            }
            else {
                throw new IllegalStateException();
            }

            boolean isVar;
            if (members.getter == null && members.setter == null) {
                isVar = !members.field.getMember().isFinal();
            }
            else {
                isVar = members.setter != null;
            }

            Visibility visibility = JavaDescriptorResolver.resolveVisibility(anyMember.getMember().psiMember, null);
            CallableMemberDescriptor.Kind kind = CallableMemberDescriptor.Kind.DECLARATION;

            if (members.getter != null && members.getter.getMember() instanceof PsiMethodWrapper) {
                JetMethodAnnotation jetMethod = ((PsiMethodWrapper) members.getter.getMember()).getJetMethod();
                visibility = JavaDescriptorResolver.resolveVisibility(anyMember.getMember().psiMember, jetMethod);
                kind = DescriptorKindUtils.flagsToKind(jetMethod.kind());
            }

            DeclarationDescriptor realOwner = getRealOwner(owner, scopeData, anyMember.getMember().isStatic());

            boolean isEnumEntry = DescriptorUtils.isEnumClassObject(realOwner);
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                    realOwner,
                    javaDescriptorResolver.resolveAnnotations(anyMember.getMember().psiMember),
                    JavaDescriptorResolver.resolveModality(anyMember.getMember(), isFinal || isEnumEntry),
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
            if (members.getter != null) {
                getterDescriptor = new PropertyGetterDescriptor(
                        propertyDescriptor,
                        javaDescriptorResolver.resolveAnnotations(members.getter.getMember().psiMember),
                        Modality.OPEN,
                        visibility,
                        true,
                        false,
                        kind);
            }
            if (members.setter != null) {
                Visibility setterVisibility = JavaDescriptorResolver.resolveVisibility(members.setter.getMember().psiMember, null);
                if (members.setter.getMember() instanceof PsiMethodWrapper) {
                    setterVisibility = JavaDescriptorResolver.resolveVisibility(members.setter.getMember().psiMember,
                                                                                ((PsiMethodWrapper) members.setter.getMember())
                                                                                        .getJetMethod());
                }
                setterDescriptor = new PropertySetterDescriptor(
                        propertyDescriptor,
                        javaDescriptorResolver.resolveAnnotations(members.setter.getMember().psiMember),
                        Modality.OPEN,
                        setterVisibility,
                        true,
                        false,
                        kind);
            }

            propertyDescriptor.initialize(getterDescriptor, setterDescriptor);

            List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(0);

            if (members.setter != null) {
                PsiMethodWrapper method = (PsiMethodWrapper) members.setter.getMember();

                if (anyMember == members.setter) {
                    typeParameters = javaDescriptorSignatureResolver.resolveMethodTypeParameters(method, propertyDescriptor);
                }
            }
            if (members.getter != null) {
                PsiMethodWrapper method = (PsiMethodWrapper) members.getter.getMember();

                if (anyMember == members.getter) {
                    typeParameters = javaDescriptorSignatureResolver.resolveMethodTypeParameters(method, propertyDescriptor);
                }
            }

            TypeVariableResolver typeVariableResolverForPropertyInternals = TypeVariableResolvers.typeVariableResolverFromTypeParameters(typeParameters, propertyDescriptor, "property " + propertyName + " in " + context);

            JetType propertyType;
            if (anyMember.getType().getTypeString().length() > 0) {
                propertyType = semanticServices.getTypeTransformer().transformToType(anyMember.getType().getTypeString(), typeVariableResolverForPropertyInternals);
            }
            else {
                propertyType = semanticServices.getTypeTransformer().transformToType(anyMember.getType().getPsiType(), typeVariableResolverForPropertyInternals);
                if (JavaDescriptorResolver.findAnnotation(anyMember.getType().getPsiNotNullOwner(), JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null) {
                    propertyType = TypeUtils.makeNullableAsSpecified(propertyType, false);
                }
                else if (members.getter == null && members.setter == null && members.field.getMember().isFinal() && members.field.getMember().isStatic()) {
                    // http://youtrack.jetbrains.com/issue/KT-1388
                    propertyType = TypeUtils.makeNotNullable(propertyType);
                }
            }

            JetType receiverType;
            if (anyMember.getReceiverType() == null) {
                receiverType = null;
            }
            else if (anyMember.getReceiverType().getTypeString().length() > 0) {
                receiverType = semanticServices.getTypeTransformer().transformToType(anyMember.getReceiverType().getTypeString(), typeVariableResolverForPropertyInternals);
            }
            else {
                receiverType = semanticServices.getTypeTransformer().transformToType(anyMember.getReceiverType().getPsiType(), typeVariableResolverForPropertyInternals);
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
                setterDescriptor.initialize(new ValueParameterDescriptorImpl(setterDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("p0") /*TODO*/, false, propertyDescriptor.getType(), false, null));
            }

            if (kind == CallableMemberDescriptor.Kind.DECLARATION) {
                trace.record(BindingContext.VARIABLE, anyMember.getMember().psiMember, propertyDescriptor);
            }

            propertiesFromCurrent.add(propertyDescriptor);
        }


        Set<PropertyDescriptor> propertiesFromSupertypes = getPropertiesFromSupertypes(scopeData, propertyName);

        final Set<PropertyDescriptor> properties = Sets.newHashSet();

        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            OverrideResolver
                    .generateOverridesInFunctionGroup(propertyName, propertiesFromSupertypes, propertiesFromCurrent, classDescriptor,
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

        namedMembers.propertyDescriptors = Sets.<VariableDescriptor>newHashSet(properties);
    }

    private static Set<PropertyDescriptor> getPropertiesFromSupertypes(ResolverScopeData scopeData, Name propertyName) {
        Set<PropertyDescriptor> r = new HashSet<PropertyDescriptor>();
        for (JetType supertype : JavaDescriptorResolver.getSupertypes(scopeData)) {
            for (VariableDescriptor property : supertype.getMemberScope().getProperties(propertyName)) {
                r.add((PropertyDescriptor) property);
            }
        }
        return r;
    }

    private static Object key(TypeSource typeSource) {
        if (typeSource == null) {
            return "";
        }
        else if (typeSource.getTypeString().length() > 0) {
            return typeSource.getTypeString();
        }
        else {
            return psiTypeToKey(typeSource.getPsiType());
        }
    }

    private static Object psiTypeToKey(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            return ((PsiClassType) psiType).getClassName();
        }
        else if (psiType instanceof PsiPrimitiveType) {
            return psiType.getPresentableText();
        }
        else if (psiType instanceof PsiArrayType) {
            return Pair.create("[", psiTypeToKey(((PsiArrayType) psiType).getComponentType()));
        }
        else {
            throw new IllegalStateException(psiType.getClass().toString());
        }
    }

    private static Object propertyKeyForGrouping(PropertyAccessorData propertyAccessor) {
        Object type = key(propertyAccessor.getType());
        Object receiverType = key(propertyAccessor.getReceiverType());
        return Pair.create(type, receiverType);
    }

    @NotNull
    private ClassOrNamespaceDescriptor getRealOwner(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull ResolverScopeData scopeData,
            boolean isStatic
    ) {
        final PsiClass psiClass = scopeData.psiClass;
        assert psiClass != null;

        boolean isEnum = psiClass.isEnum();
        if (isEnum && isStatic) {
            final String qualifiedName = psiClass.getQualifiedName();
            assert qualifiedName != null;
            final ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(new FqName(qualifiedName));
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
