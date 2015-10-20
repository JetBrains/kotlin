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
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtSecondaryConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;

public class OverloadResolver {
    @NotNull private final BindingTrace trace;
    @NotNull private final MainFunctionDetector mainFunctionDetector;

    public OverloadResolver(@NotNull BindingTrace trace) {
        this.trace = trace;
        mainFunctionDetector = new MainFunctionDetector(trace.getBindingContext());
    }

    public void process(@NotNull BodiesResolveContext c) {
        checkOverloads(c);
    }

    private void checkOverloads(@NotNull BodiesResolveContext c) {
        MultiMap<ClassDescriptor, ConstructorDescriptor> inClasses = MultiMap.create();
        MultiMap<FqNameUnsafe, ConstructorDescriptor> inPackages = MultiMap.create();
        fillGroupedConstructors(c, inClasses, inPackages);

        for (Map.Entry<KtClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey(), inClasses.get(entry.getValue()));
        }
        checkOverloadsInPackages(c, inPackages);
    }

    private static void fillGroupedConstructors(
            @NotNull BodiesResolveContext c,
            @NotNull MultiMap<ClassDescriptor, ConstructorDescriptor> inClasses,
            @NotNull MultiMap<FqNameUnsafe, ConstructorDescriptor> inPackages
    ) {
        for (ClassDescriptorWithResolutionScopes klass : c.getDeclaredClasses().values()) {
            if (klass.getKind().isSingleton() || klass.getName().isSpecial()) {
                // Constructors of singletons or anonymous object aren't callable from the code, so they shouldn't participate in overload name checking
                continue;
            }
            DeclarationDescriptor containingDeclaration = klass.getContainingDeclaration();
            if (containingDeclaration instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
                inClasses.putValues(classDescriptor, klass.getConstructors());
            }
            else if (containingDeclaration instanceof PackageFragmentDescriptor) {
                inPackages.putValues(getFqName(klass), klass.getConstructors());
            }
            else if (containingDeclaration instanceof ScriptDescriptor) {
                // TODO: check overload conflicts of functions with constructors in scripts
            }
            else if (!(containingDeclaration instanceof FunctionDescriptor)) {
                throw new IllegalStateException("Illegal class container: " + containingDeclaration);
            }
        }
    }

    private void checkOverloadsInPackages(
            @NotNull BodiesResolveContext c,
            @NotNull MultiMap<FqNameUnsafe, ConstructorDescriptor> inPackages
    ) {

        MultiMap<FqNameUnsafe, CallableMemberDescriptor> functionsByName = MultiMap.create();

        for (SimpleFunctionDescriptor function : c.getFunctions().values()) {
            if (function.getContainingDeclaration() instanceof PackageFragmentDescriptor) {
                functionsByName.putValue(getFqName(function), function);
            }
        }
        
        for (PropertyDescriptor property : c.getProperties().values()) {
            if (property.getContainingDeclaration() instanceof PackageFragmentDescriptor) {
                functionsByName.putValue(getFqName(property), property);
            }
        }
        
        for (Map.Entry<FqNameUnsafe, Collection<ConstructorDescriptor>> entry : inPackages.entrySet()) {
            functionsByName.putValues(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<FqNameUnsafe, Collection<CallableMemberDescriptor>> e : functionsByName.entrySet()) {
            // TODO: don't render FQ name here, extract this logic to somewhere
            FqNameUnsafe fqName = e.getKey().parent();
            checkOverloadsWithSameName(e.getValue(), fqName.isRoot() ? "root package" : fqName.asString());
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
            checkOverloadsWithSameName(e.getValue(), nameForErrorMessage(classDescriptor, klass));
        }
    }
    
    private void checkOverloadsWithSameName(
            Collection<CallableMemberDescriptor> functions,
            @NotNull String functionContainer
    ) {
        if (functions.size() == 1) {
            // micro-optimization
            return;
        }
        reportRedeclarations(functionContainer, findRedeclarations(functions));
    }

    @NotNull
    private Set<Pair<KtDeclaration, CallableMemberDescriptor>> findRedeclarations(@NotNull Collection<CallableMemberDescriptor> functions) {
        Set<Pair<KtDeclaration, CallableMemberDescriptor>> redeclarations = Sets.newLinkedHashSet();
        for (CallableMemberDescriptor member : functions) {
            for (CallableMemberDescriptor member2 : functions) {
                if (member == member2 || isConstructorsOfDifferentRedeclaredClasses(member, member2)) {
                    continue;
                }

                OverloadUtil.OverloadCompatibilityInfo overloadable = OverloadUtil.isOverloadable(member, member2);
                if (!overloadable.isSuccess() && member.getKind() != CallableMemberDescriptor.Kind.SYNTHESIZED) {
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

    private void reportRedeclarations(@NotNull String functionContainer,
            @NotNull Set<Pair<KtDeclaration, CallableMemberDescriptor>> redeclarations) {
        for (Pair<KtDeclaration, CallableMemberDescriptor> redeclaration : redeclarations) {
            CallableMemberDescriptor memberDescriptor = redeclaration.getSecond();
            if (DescriptorToSourceUtils.isTopLevelMainFunction(memberDescriptor, mainFunctionDetector)) return;

            KtDeclaration ktDeclaration = redeclaration.getFirst();
            if (memberDescriptor instanceof PropertyDescriptor) {
                trace.report(Errors.REDECLARATION.on(ktDeclaration, memberDescriptor.getName().asString()));
            }
            else {
                String containingClassName = ktDeclaration instanceof KtSecondaryConstructor ?
                                             ((KtSecondaryConstructor) ktDeclaration).getContainingClassOrObject().getName() : null;

                trace.report(Errors.CONFLICTING_OVERLOADS.on(
                        ktDeclaration, memberDescriptor,
                        containingClassName != null ? containingClassName : functionContainer));
            }
        }
    }
}
