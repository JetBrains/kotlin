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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.NullableLazyValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.DELEGATION;
import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
import static org.jetbrains.jet.lang.resolve.DelegationResolver.generateDelegatedMembers;

public class LazyClassMemberScope extends AbstractLazyMemberScope<LazyClassDescriptor, ClassMemberDeclarationProvider> {

    @NotNull
    private static final Set<ClassKind> GENERATE_CONSTRUCTORS_FOR =
            ImmutableSet.of(ClassKind.CLASS, ClassKind.ANNOTATION_CLASS, ClassKind.OBJECT,
                            ClassKind.ENUM_CLASS, ClassKind.ENUM_ENTRY, ClassKind.CLASS_OBJECT);

    private interface MemberExtractor<T extends CallableMemberDescriptor> {
        MemberExtractor<FunctionDescriptor> EXTRACT_FUNCTIONS = new MemberExtractor<FunctionDescriptor>() {
            @NotNull
            @Override
            public Collection<FunctionDescriptor> extract(@NotNull JetType extractFrom, @NotNull Name name) {
                return extractFrom.getMemberScope().getFunctions(name);
            }
        };

        MemberExtractor<PropertyDescriptor> EXTRACT_PROPERTIES = new MemberExtractor<PropertyDescriptor>() {
            @NotNull
            @Override
            public Collection<PropertyDescriptor> extract(@NotNull JetType extractFrom, @NotNull Name name) {
                //noinspection unchecked
                return (Collection) extractFrom.getMemberScope().getProperties(name);
            }
        };

        @NotNull
        Collection<T> extract(@NotNull JetType extractFrom, @NotNull Name name);
    }

    private final NullableLazyValue<ConstructorDescriptor> primaryConstructor;

    public LazyClassMemberScope(
            @NotNull ResolveSession resolveSession,
            @NotNull ClassMemberDeclarationProvider declarationProvider,
            @NotNull LazyClassDescriptor thisClass
    ) {
        super(resolveSession, declarationProvider, thisClass);
        this.primaryConstructor = resolveSession.getStorageManager().createNullableLazyValue(new Function0<ConstructorDescriptor>() {
            @Override
            public ConstructorDescriptor invoke() {
                return resolvePrimaryConstructor();
            }
        });
    }

    @NotNull
    @Override
    protected JetScope getScopeForMemberDeclarationResolution(JetDeclaration declaration) {
        if (declaration instanceof JetProperty) {
            return thisDescriptor.getScopeForPropertyInitializerResolution();
        }
        return thisDescriptor.getScopeForMemberDeclarationResolution();
    }

    private <D extends CallableMemberDescriptor> void generateFakeOverrides(
            @NotNull Name name,
            @NotNull Collection<D> fromSupertypes,
            @NotNull final Collection<D> result,
            @NotNull final Class<? extends D> exactDescriptorClass
    ) {
        OverridingUtil.generateOverridesInFunctionGroup(
                name,
                fromSupertypes,
                Lists.newArrayList(result),
                thisDescriptor,
                new OverridingUtil.DescriptorSink() {
                    @Override
                    public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                        assert exactDescriptorClass.isInstance(fakeOverride) : "Wrong descriptor type in an override: " +
                                                                               fakeOverride +
                                                                               " while expecting " +
                                                                               exactDescriptorClass.getSimpleName();
                        //noinspection unchecked
                        result.add((D) fakeOverride);
                    }

                    @Override
                    public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                        BindingTrace trace = resolveSession.getTrace();
                        JetDeclaration declaration = (JetDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(),
                                                                                                                  fromCurrent);
                        assert declaration != null : "fromCurrent can not be a fake override";
                        trace.report(Errors.CONFLICTING_OVERLOADS
                                             .on(declaration, fromCurrent, fromCurrent.getContainingDeclaration().getName().asString()));
                    }
                }
        );
        OverrideResolver.resolveUnknownVisibilities(result, resolveSession.getTrace());
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull Name name) {
        // TODO: this should be handled by lazy function descriptors
        Set<FunctionDescriptor> functions = super.getFunctions(name);
        for (FunctionDescriptor functionDescriptor : functions) {
            if (functionDescriptor.getKind() != FAKE_OVERRIDE && functionDescriptor.getKind() != DELEGATION) {
                OverrideResolver.resolveUnknownVisibilityForMember(functionDescriptor, resolveSession.getTrace());
            }
        }
        return functions;
    }

    @Override
    protected void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<FunctionDescriptor> result) {
        Collection<FunctionDescriptor> fromSupertypes = Lists.newArrayList();
        for (JetType supertype : thisDescriptor.getTypeConstructor().getSupertypes()) {
            fromSupertypes.addAll(supertype.getMemberScope().getFunctions(name));
        }
        result.addAll(generateDelegatingDescriptors(name, MemberExtractor.EXTRACT_FUNCTIONS, result));
        generateEnumClassObjectMethods(result, name);
        generateDataClassMethods(result, name);
        generateFakeOverrides(name, fromSupertypes, result, FunctionDescriptor.class);
    }

    private void generateDataClassMethods(@NotNull Collection<FunctionDescriptor> result, @NotNull Name name) {
        if (!KotlinBuiltIns.getInstance().isData(thisDescriptor)) return;

        ConstructorDescriptor constructor = getPrimaryConstructor();
        if (constructor == null) return;

        int parameterIndex = 0;
        for (ValueParameterDescriptor parameter : constructor.getValueParameters()) {
            if (parameter.getType().isError()) continue;
            Set<VariableDescriptor> properties = getProperties(parameter.getName());
            if (properties.isEmpty()) continue;
            assert properties.size() == 1 : "A constructor parameter is resolved to more than one (" + properties.size() + ") property: " + parameter;
            PropertyDescriptor property = (PropertyDescriptor) properties.iterator().next();
            if (property == null) continue;
            ++parameterIndex;

            if (name.equals(Name.identifier(DescriptorResolver.COMPONENT_FUNCTION_NAME_PREFIX + parameterIndex))) {
                SimpleFunctionDescriptor functionDescriptor =
                        DescriptorResolver.createComponentFunctionDescriptor(parameterIndex, property,
                                                                             parameter, thisDescriptor, resolveSession.getTrace());
                result.add(functionDescriptor);
                break;
            }
        }
        if (!constructor.getValueParameters().isEmpty() && name.equals(DescriptorResolver.COPY_METHOD_NAME)) {
            SimpleFunctionDescriptor copyFunctionDescriptor = DescriptorResolver.createCopyFunctionDescriptor(
                    constructor.getValueParameters(),
                    thisDescriptor, resolveSession.getTrace());
            result.add(copyFunctionDescriptor);
        }
    }

    private void generateEnumClassObjectMethods(@NotNull Collection<? super FunctionDescriptor> result, @NotNull Name name) {
        if (!DescriptorUtils.isEnumClassObject(thisDescriptor)) return;

        if (name.equals(DescriptorFactory.VALUES_METHOD_NAME)) {
            SimpleFunctionDescriptor valuesMethod = DescriptorResolver
                    .createEnumClassObjectValuesMethod(thisDescriptor, resolveSession.getTrace());
            result.add(valuesMethod);
        }
        else if (name.equals(DescriptorFactory.VALUE_OF_METHOD_NAME)) {
            SimpleFunctionDescriptor valueOfMethod = DescriptorResolver
                    .createEnumClassObjectValueOfMethod(thisDescriptor, resolveSession.getTrace());
            result.add(valueOfMethod);
        }
    }

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        // TODO: this should be handled by lazy property descriptors
        Set<VariableDescriptor> properties = super.getProperties(name);
        for (VariableDescriptor variableDescriptor : properties) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
            if (propertyDescriptor.getKind() == FAKE_OVERRIDE || propertyDescriptor.getKind() == DELEGATION) continue;
            OverrideResolver.resolveUnknownVisibilityForMember(propertyDescriptor, resolveSession.getTrace());
        }
        return properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result) {
        JetClassLikeInfo classInfo = declarationProvider.getOwnerInfo();

        // From primary constructor parameters
        ConstructorDescriptor primaryConstructor = getPrimaryConstructor();
        if (primaryConstructor != null) {
            List<ValueParameterDescriptor> valueParameterDescriptors = primaryConstructor.getValueParameters();
            List<? extends JetParameter> primaryConstructorParameters = classInfo.getPrimaryConstructorParameters();
            assert valueParameterDescriptors.size() == primaryConstructorParameters.size() : "From descriptor: " + valueParameterDescriptors.size() + " but from PSI: " + primaryConstructorParameters.size();
            for (ValueParameterDescriptor valueParameterDescriptor : valueParameterDescriptors) {
                JetParameter parameter = primaryConstructorParameters.get(valueParameterDescriptor.getIndex());
                if (parameter.getValOrVarNode() != null && name.equals(parameter.getNameAsName())) {
                    PropertyDescriptor propertyDescriptor =
                            resolveSession.getInjector().getDescriptorResolver().resolvePrimaryConstructorParameterToAProperty(
                                    thisDescriptor,
                                    valueParameterDescriptor,
                                    thisDescriptor.getScopeForClassHeaderResolution(),
                                    parameter, resolveSession.getTrace()
                            );
                    result.add(propertyDescriptor);
                }
            }
        }

        // Members from supertypes
        Collection<PropertyDescriptor> fromSupertypes = Lists.newArrayList();
        for (JetType supertype : thisDescriptor.getTypeConstructor().getSupertypes()) {
            fromSupertypes.addAll((Collection) supertype.getMemberScope().getProperties(name));
        }
        result.addAll(generateDelegatingDescriptors(name, MemberExtractor.EXTRACT_PROPERTIES, result));
        generateFakeOverrides(name, fromSupertypes, (Collection) result, PropertyDescriptor.class);
    }

    @NotNull
    private <T extends CallableMemberDescriptor> Collection<T> generateDelegatingDescriptors(
            @NotNull final Name name,
            @NotNull final MemberExtractor<T> extractor,
            @NotNull Collection<? extends CallableDescriptor> existingDescriptors
    ) {
        JetClassOrObject classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
        if (classOrObject == null) {
            // Enum class objects do not have delegated members
            return Collections.emptySet();
        }

        DelegationResolver.TypeResolver lazyTypeResolver = new DelegationResolver.TypeResolver() {
            @Nullable
            @Override
            public JetType resolve(@NotNull JetTypeReference reference) {
                return resolveSession.getInjector().getTypeResolver().resolveType(
                        thisDescriptor.getScopeForClassHeaderResolution(),
                        reference,
                        resolveSession.getTrace(),
                        false);
            }
        };
        DelegationResolver.MemberExtractor<T> lazyMemberExtractor = new DelegationResolver.MemberExtractor<T>() {
            @NotNull
            @Override
            public Collection<T> getMembersByType(@NotNull JetType type) {
                return extractor.extract(type, name);
            }
        };
        return generateDelegatedMembers(classOrObject, thisDescriptor, existingDescriptors, resolveSession.getTrace(), lazyMemberExtractor,
                                        lazyTypeResolver);
    }

    @Override
    protected void addExtraDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        for (JetType supertype : thisDescriptor.getTypeConstructor().getSupertypes()) {
            for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                if (descriptor instanceof FunctionDescriptor) {
                    result.addAll(getFunctions(descriptor.getName()));
                }
                else if (descriptor instanceof PropertyDescriptor) {
                    result.addAll(getProperties(descriptor.getName()));
                }
                // Nothing else is inherited
            }
        }

        result.addAll(getFunctions(DescriptorFactory.VALUES_METHOD_NAME));
        result.addAll(getFunctions(DescriptorFactory.VALUE_OF_METHOD_NAME));

        addDataClassMethods(result);
    }

    private void addDataClassMethods(@NotNull Collection<DeclarationDescriptor> result) {
        if (!KotlinBuiltIns.getInstance().isData(thisDescriptor)) return;

        ConstructorDescriptor constructor = getPrimaryConstructor();
        if (constructor == null) return;

        // Generate componentN functions until there's no such function for some n
        int n = 1;
        while (true) {
            Name componentName = Name.identifier(DescriptorResolver.COMPONENT_FUNCTION_NAME_PREFIX + n);
            Set<FunctionDescriptor> functions = getFunctions(componentName);
            if (functions.isEmpty()) break;

            result.addAll(functions);

            n++;
        }
        result.addAll(getFunctions(Name.identifier("copy")));
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        return null;
    }

    @NotNull
    @Override
    protected ReceiverParameterDescriptor getImplicitReceiver() {
        return thisDescriptor.getThisAsReceiverParameter();
    }

    @NotNull
    public Set<ConstructorDescriptor> getConstructors() {
        ConstructorDescriptor constructor = getPrimaryConstructor();
        return constructor == null ? Collections.<ConstructorDescriptor>emptySet() : Collections.singleton(constructor);
    }

    @Nullable
    public ConstructorDescriptor getPrimaryConstructor() {
        return primaryConstructor.invoke();
    }

    @Nullable
    private ConstructorDescriptor resolvePrimaryConstructor() {
        ConstructorDescriptor primaryConstructor = null;
        if (GENERATE_CONSTRUCTORS_FOR.contains(thisDescriptor.getKind())) {
            JetClassOrObject classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
            if (!thisDescriptor.getKind().isSingleton()) {
                JetClass jetClass = (JetClass) classOrObject;
                ConstructorDescriptorImpl constructor = resolveSession.getInjector().getDescriptorResolver()
                        .resolvePrimaryConstructorDescriptor(thisDescriptor.getScopeForClassHeaderResolution(),
                                                             thisDescriptor,
                                                             jetClass,
                                                             resolveSession.getTrace());
                primaryConstructor = constructor;
                setDeferredReturnType(constructor);
            }
            else {
                ConstructorDescriptorImpl constructor =
                        DescriptorResolver.createAndRecordPrimaryConstructorForObject(classOrObject, thisDescriptor, resolveSession.getTrace());
                setDeferredReturnType(constructor);
                primaryConstructor = constructor;
            }
        }
        return primaryConstructor;
    }

    private void setDeferredReturnType(@NotNull ConstructorDescriptorImpl descriptor) {
        descriptor.setReturnType(DeferredType.create(resolveSession.getTrace(), resolveSession.getStorageManager().createLazyValue(
                new Function0<JetType>() {
                    @Override
                    public JetType invoke() {
                        return thisDescriptor.getDefaultType();
                    }
                })
        ));
    }

    @Override
    public String toString() {
        // Do not add details here, they may compromise the laziness during debugging
        return "lazy scope for class " + thisDescriptor.getName();
    }
}
