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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.provider.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.resolver.*;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public class JavaDescriptorResolver implements DependencyClassByQualifiedNameResolver {

    public static final Name JAVA_ROOT = Name.special("<java_root>");

    private JavaPropertyResolver propertiesResolver;
    private JavaClassResolver classResolver;
    private JavaConstructorResolver constructorResolver;
    private JavaFunctionResolver functionResolver;
    private JavaNamespaceResolver namespaceResolver;

    @Inject
    public void setFunctionResolver(JavaFunctionResolver functionResolver) {
        this.functionResolver = functionResolver;
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setNamespaceResolver(JavaNamespaceResolver namespaceResolver) {
        this.namespaceResolver = namespaceResolver;
    }

    @Inject
    public void setPropertiesResolver(JavaPropertyResolver propertiesResolver) {
        this.propertiesResolver = propertiesResolver;
    }

    @Inject
    public void setConstructorResolver(JavaConstructorResolver constructorResolver) {
        this.constructorResolver = constructorResolver;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        return classResolver.resolveClass(qualifiedName, searchRule);
    }

    @Override
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return classResolver.resolveClass(qualifiedName, IGNORE_KOTLIN_SOURCES);
    }

    @NotNull
    public Collection<ConstructorDescriptor> resolveConstructors(@NotNull JavaClass javaClass, @NotNull ClassDescriptor classDescriptor) {
        return constructorResolver.resolveConstructors(javaClass, classDescriptor);
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        return namespaceResolver.resolveNamespace(qualifiedName, searchRule);
    }

    @Nullable
    public JetScope getJavaPackageScope(@NotNull NamespaceDescriptor namespaceDescriptor) {
        return namespaceResolver.getJavaPackageScopeForExistingNamespaceDescriptor(namespaceDescriptor);
    }

    @NotNull
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor ownerDescriptor) {
        return propertiesResolver.resolveFieldGroup(members, ownerDescriptor);
    }

    public static class ValueParameterDescriptors {
        private final JetType receiverType;
        private final List<ValueParameterDescriptor> descriptors;

        public ValueParameterDescriptors(@Nullable JetType receiverType, @NotNull List<ValueParameterDescriptor> descriptors) {
            this.receiverType = receiverType;
            this.descriptors = descriptors;
        }

        @Nullable
        public JetType getReceiverType() {
            return receiverType;
        }

        @NotNull
        public List<ValueParameterDescriptor> getDescriptors() {
            return descriptors;
        }
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroupForClass(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor owner) {
        return functionResolver.resolveFunctionGroupForClass(members, owner);
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroupForPackage(@NotNull NamedMembers members, @NotNull NamespaceDescriptor owner) {
        return functionResolver.resolveFunctionGroupForPackage(members, owner);
    }
}
