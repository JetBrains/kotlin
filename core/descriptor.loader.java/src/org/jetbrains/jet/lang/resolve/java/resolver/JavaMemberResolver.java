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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.scope.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;

public class JavaMemberResolver {
    private JavaClassResolver classResolver;
    private JavaFunctionResolver functionResolver;
    private JavaPropertyResolver propertyResolver;
    private JavaConstructorResolver constructorResolver;

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setFunctionResolver(JavaFunctionResolver functionResolver) {
        this.functionResolver = functionResolver;
    }

    @Inject
    public void setPropertyResolver(JavaPropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @Inject
    public void setConstructorResolver(JavaConstructorResolver constructorResolver) {
        this.constructorResolver = constructorResolver;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        return classResolver.resolveClass(qualifiedName, searchRule);
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroupForClass(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor owner) {
        return functionResolver.resolveFunctionGroupForClass(members, owner);
    }

    @NotNull
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor ownerDescriptor) {
        return propertyResolver.resolveFieldGroup(members, ownerDescriptor);
    }

    @NotNull
    public Collection<ConstructorDescriptor> resolveConstructors(@NotNull JavaClass javaClass, @NotNull ClassDescriptor classDescriptor) {
        return constructorResolver.resolveConstructors(javaClass, classDescriptor);
    }
}
