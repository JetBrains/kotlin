/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class OverloadResolver {
    @NotNull private final BindingTrace trace;
    @NotNull private final OverloadFilter overloadFilter;

    public OverloadResolver(
            @NotNull BindingTrace trace,
            @NotNull OverloadFilter overloadFilter
    ) {
        this.trace = trace;
        this.overloadFilter = overloadFilter;
    }

    public void process(@NotNull BodiesResolveContext c) {
        checkOverloads(c);
    }

    private void checkOverloads(@NotNull BodiesResolveContext c) {
        MultiMap<ClassDescriptor, ConstructorDescriptor> inClasses = findConstructorsInNestedClasses(c);

        for (Map.Entry<KtClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey(), inClasses.get(entry.getValue()));
        }
        checkOverloadsInPackages(c);
    }

    private static MultiMap<ClassDescriptor, ConstructorDescriptor> findConstructorsInNestedClasses(@NotNull BodiesResolveContext c) {
        MultiMap<ClassDescriptor, ConstructorDescriptor> inClasses = MultiMap.create();

        for (ClassDescriptorWithResolutionScopes klass : c.getDeclaredClasses().values()) {
            if (klass.getKind().isSingleton() || klass.getName().isSpecial()) {
                // Constructors of singletons or anonymous object aren't callable from the code, so they shouldn't participate in overload name checking
                continue;
            }
            DeclarationDescriptor containingDeclaration = klass.getContainingDeclaration();
            if (containingDeclaration instanceof ScriptDescriptor) {
                // TODO: check overload conflicts of functions with constructors in scripts
            }
            else if (containingDeclaration instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
                inClasses.putValues(classDescriptor, klass.getConstructors());
            }
            else if (!(containingDeclaration instanceof FunctionDescriptor ||
                       containingDeclaration instanceof PropertyDescriptor ||
                       containingDeclaration instanceof PackageFragmentDescriptor)) {
                throw new IllegalStateException("Illegal class container: " + containingDeclaration);
            }
        }

        return inClasses;
    }

    private void checkOverloadsInPackages(@NotNull BodiesResolveContext c) {
        MultiMap<FqNameUnsafe, CallableMemberDescriptor> membersByName =
                OverloadUtil.groupModulePackageMembersByFqName(c, overloadFilter);

        for (Map.Entry<FqNameUnsafe, Collection<CallableMemberDescriptor>> e : membersByName.entrySet()) {
            FqNameUnsafe fqName = e.getKey().parent();
            checkOverloadsInPackage(e.getValue(), fqName);
        }
    }

    private static String nameForErrorMessage(ClassDescriptor classDescriptor, KtClassOrObject jetClass) {
        String name = jetClass.getName();
        if (name != null) {
            return name;
        }
        if (jetClass instanceof KtObjectDeclaration) {
            // must be companion object
            name = classDescriptor.getContainingDeclaration().getName().asString();
            return "companion object " + name;
        }
        // safe
        return "<unknown>";
    }

    private void checkOverloadsInAClass(
            ClassDescriptorWithResolutionScopes classDescriptor, KtClassOrObject klass,
            Collection<ConstructorDescriptor> nestedClassConstructors
    ) {
        MultiMap<Name, CallableMemberDescriptor> functionsByName = MultiMap.create();
        
        for (CallableMemberDescriptor function : classDescriptor.getDeclaredCallableMembers()) {
            functionsByName.putValue(function.getName(), function);
        }
        
        for (ConstructorDescriptor nestedClassConstructor : nestedClassConstructors) {
            functionsByName.putValue(nestedClassConstructor.getContainingDeclaration().getName(), nestedClassConstructor);
        }
        
        for (Map.Entry<Name, Collection<CallableMemberDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsInClass(e.getValue(), classDescriptor, klass);
        }
    }
    
    private void checkOverloadsInPackage(
            @NotNull Collection<CallableMemberDescriptor> members,
            @NotNull FqNameUnsafe packageFQN
    ) {
        if (members.size() == 1) return;

        for (Collection<? extends CallableMemberDescriptor> redeclarationGroup : OverloadUtil.getPossibleRedeclarationGroups(members)) {
            Set<Pair<KtDeclaration, CallableMemberDescriptor>> redeclarations = findRedeclarations(redeclarationGroup);
            // TODO: don't render FQ name here, extract this logic to somewhere
            reportRedeclarations(packageFQN.isRoot() ? "root package" : packageFQN.asString(), redeclarations);
        }
    }

    private void checkOverloadsInClass(
            @NotNull Collection<CallableMemberDescriptor> members,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull KtClassOrObject ktClass
    ) {
        if (members.size() == 1) return;

        reportRedeclarations(nameForErrorMessage(classDescriptor, ktClass), findRedeclarations(members));
    }

    @NotNull
    private static Set<Pair<KtDeclaration, CallableMemberDescriptor>> findRedeclarations(@NotNull Collection<? extends CallableMemberDescriptor> members) {
        Set<Pair<KtDeclaration, CallableMemberDescriptor>> redeclarations = Sets.newLinkedHashSet();
        for (CallableMemberDescriptor member : members) {
            for (CallableMemberDescriptor member2 : members) {
                if (member == member2 || isConstructorsOfDifferentRedeclaredClasses(member, member2)) {
                    continue;
                }

                if (!OverloadUtil.isOverloadable(member, member2) && member.getKind() != CallableMemberDescriptor.Kind.SYNTHESIZED) {
                    if (isTopLevelMainInDifferentFiles(member, member2)) {
                        continue;
                    }

                    KtDeclaration ktDeclaration = (KtDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(member);
                    if (ktDeclaration != null) {
                        redeclarations.add(Pair.create(ktDeclaration, member));
                    }
                }
            }
        }
        return redeclarations;
    }

    private static boolean isConstructorsOfDifferentRedeclaredClasses(
            @NotNull CallableMemberDescriptor member, @NotNull CallableMemberDescriptor member2
    ) {
        if (!(member instanceof ConstructorDescriptor) || !(member2 instanceof ConstructorDescriptor)) return false;
        // ignore conflicting overloads for constructors of different classes because their redeclarations will be reported
        // but don't ignore if there's possibility that classes redeclarations will not be reported
        // (e.g. they're declared in different packages)
        assert member.getContainingDeclaration().getContainingDeclaration() != null : "Grandparent of constructor should not be null";
        return member.getContainingDeclaration() != member2.getContainingDeclaration() &&
               member.getContainingDeclaration().getContainingDeclaration().equals(member2.getContainingDeclaration().getContainingDeclaration());
    }

    private static boolean isTopLevelMainInDifferentFiles(@NotNull CallableMemberDescriptor member, @NotNull CallableMemberDescriptor member2) {
        if (!MainFunctionDetector.isMain(member) ||
            !MainFunctionDetector.isMain(member2)) {
            return false;
        }

        KtFile file = DescriptorToSourceUtils.getContainingFile(member);
        KtFile file2 = DescriptorToSourceUtils.getContainingFile(member2);
        return file == null || file2 == null || file != file2;
    }

    private void reportRedeclarations(
            @NotNull String functionContainer,
            @NotNull Set<Pair<KtDeclaration, CallableMemberDescriptor>> redeclarations
    ) {
        if (redeclarations.isEmpty()) return;

        Iterator<Pair<KtDeclaration, CallableMemberDescriptor>> redeclarationsIterator = redeclarations.iterator();
        CallableMemberDescriptor firstRedeclarationDescriptor = redeclarationsIterator.next().getSecond();
        CallableMemberDescriptor otherRedeclarationDescriptor = redeclarationsIterator.hasNext()
                                                                ? redeclarationsIterator.next().getSecond()
                                                                : null;

        for (Pair<KtDeclaration, CallableMemberDescriptor> redeclaration : redeclarations) {
            KtDeclaration ktDeclaration = redeclaration.getFirst();
            CallableMemberDescriptor memberDescriptor = redeclaration.getSecond();

            CallableMemberDescriptor redeclarationDescriptor;
            if (otherRedeclarationDescriptor == null) {
                redeclarationDescriptor = firstRedeclarationDescriptor;
            }
            else if (firstRedeclarationDescriptor == memberDescriptor) {
                redeclarationDescriptor = otherRedeclarationDescriptor;
            }
            else {
                redeclarationDescriptor = firstRedeclarationDescriptor;
            }

            if (memberDescriptor instanceof PropertyDescriptor) {
                trace.report(Errors.REDECLARATION.on(ktDeclaration, memberDescriptor.getName().asString()));
            }
            else {
                trace.report(Errors.CONFLICTING_OVERLOADS.on(ktDeclaration, memberDescriptor, redeclarationDescriptor));
            }
        }
    }
}
