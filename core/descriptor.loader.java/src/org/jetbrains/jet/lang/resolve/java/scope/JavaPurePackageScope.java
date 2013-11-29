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

package org.jetbrains.jet.lang.resolve.java.scope;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptor;
import org.jetbrains.jet.lang.resolve.java.resolver.*;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public final class JavaPurePackageScope extends JavaBaseScope implements JavaPackageFragmentScope {
    @NotNull
    private final JavaPackage javaPackage;
    @NotNull
    private final FqName packageFQN;
    private final boolean includeCompiledKotlinClasses;

    public JavaPurePackageScope(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull JavaPackage javaPackage,
            @NotNull FqName packageFQN,
            @NotNull JavaMemberResolver memberResolver,
            boolean includeCompiledKotlinClasses
    ) {
        super(descriptor, memberResolver, MembersProvider.forPackage(javaPackage));
        this.javaPackage = javaPackage;
        this.packageFQN = packageFQN;
        this.includeCompiledKotlinClasses = includeCompiledKotlinClasses;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return memberResolver.resolveClass(packageFQN.child(name), IGNORE_KOTLIN_SOURCES);
    }

    @NotNull
    @Override
    protected Collection<DeclarationDescriptor> computeAllDescriptors() {
        Collection<DeclarationDescriptor> result = super.computeAllDescriptors();
        result.addAll(computeAllPackageDeclarations());
        return result;
    }

    @NotNull
    private Collection<DeclarationDescriptor> computeAllPackageDeclarations() {
        Collection<DeclarationDescriptor> result = new HashSet<DeclarationDescriptor>();

        for (JavaClass javaClass : DescriptorResolverUtils.getClassesInPackage(javaPackage)) {
            if (DescriptorResolverUtils.isCompiledKotlinPackageClass(javaClass)) continue;
            if (!includeCompiledKotlinClasses && DescriptorResolverUtils.isCompiledKotlinClass(javaClass)) continue;

            if (javaClass.getOriginKind() == JavaClass.OriginKind.KOTLIN_LIGHT_CLASS) continue;

            if (javaClass.getVisibility() == Visibilities.PRIVATE) continue;

            ProgressChecker.getInstance().checkCanceled();

            FqName fqName = javaClass.getFqName();
            if (fqName == null) continue;

            ClassDescriptor classDescriptor = memberResolver.resolveClass(fqName, IGNORE_KOTLIN_SOURCES);
            if (classDescriptor != null) {
                result.add(classDescriptor);
            }
        }

        return result;
    }

    @Override
    @NotNull
    protected Set<FunctionDescriptor> computeFunctionDescriptor(@NotNull Name name) {
        NamedMembers members = membersProvider.get(name);
        if (members == null) {
            return Collections.emptySet();
        }
        SamConstructorDescriptor samConstructor = JavaFunctionResolver.resolveSamConstructor((JavaPackageFragmentDescriptor) descriptor, members);
        if (samConstructor == null) {
            return Collections.emptySet();
        }
        return Collections.<FunctionDescriptor>singleton(samConstructor);
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Collection<FqName> getSubPackages() {
        List<FqName> result = Lists.newArrayList();
        for (JavaPackage subPackage : javaPackage.getSubPackages()) {
            result.add(subPackage.getFqName());
        }
        for (JavaClass javaClass : DescriptorResolverUtils.getClassesInPackage(javaPackage)) {
            if (DescriptorResolverUtils.isJavaClassVisibleAsPackage(javaClass)) {
                result.add(javaClass.getFqName());
            }
        }
        return result;
    }
}
