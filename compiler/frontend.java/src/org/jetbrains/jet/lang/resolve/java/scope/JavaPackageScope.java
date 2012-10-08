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

package org.jetbrains.jet.lang.resolve.java.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.data.ResolverScopeData;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * @author abreslav
 */
public class JavaPackageScope extends JavaClassOrPackageScope {

    @NotNull
    private final FqName packageFQN;

    public JavaPackageScope(
            @NotNull FqName packageFQN,
            @NotNull JavaSemanticServices semanticServices,
            @NotNull ResolverScopeData resolverNamespaceData) {
        super(semanticServices, resolverNamespaceData);
        this.packageFQN = packageFQN;

        if (!resolverNamespaceData.isStaticMembers()) {
            throw new IllegalArgumentException("instance members should be resolved using " + JavaClassMembersScope.class);
        }
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveClass(packageFQN.child(name), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveClass(packageFQN.child(name), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveNamespace(packageFQN.child(name), DescriptorSearchRule.INCLUDE_KOTLIN);
    }
}
