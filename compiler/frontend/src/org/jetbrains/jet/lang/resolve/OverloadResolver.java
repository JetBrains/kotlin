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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.resolve.name.Name;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class OverloadResolver {
    private TopDownAnalysisContext context;
    private BindingTrace trace;


    @Inject
    public void setContext(TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }



    public void process() {
        checkOverloads();
    }

    private void checkOverloads() {
        Pair<MultiMap<ClassDescriptor, ConstructorDescriptor>, MultiMap<Key, ConstructorDescriptor>> pair = constructorsGrouped();
        MultiMap<ClassDescriptor, ConstructorDescriptor> inClasses = pair.first;
        MultiMap<Key, ConstructorDescriptor> inNamespaces = pair.second;

        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey(), inClasses.get(entry.getValue()));
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey(), inClasses.get(entry.getValue()));
        }
        checkOverloadsInANamespace(inNamespaces);
    }

    private static class Key extends Pair<String, Name> {
        Key(String namespace, Name name) {
            super(namespace, name);
        }
        
        Key(NamespaceDescriptor namespaceDescriptor, Name name) {
            this(DescriptorUtils.getFQName(namespaceDescriptor).getFqName(), name);
        }

        public String getNamespace() {
            return first;
        }

        public Name getFunctionName() {
            return second;
        }
    }

    
    private Pair<MultiMap<ClassDescriptor, ConstructorDescriptor>, MultiMap<Key, ConstructorDescriptor>>
            constructorsGrouped()
    {
        MultiMap<ClassDescriptor, ConstructorDescriptor> inClasses = MultiMap.create();
        MultiMap<Key, ConstructorDescriptor> inNamespaces = MultiMap.create();

        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            MutableClassDescriptor klass = entry.getValue();
            DeclarationDescriptor containingDeclaration = klass.getContainingDeclaration();
            if (containingDeclaration instanceof NamespaceDescriptor) {
                NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) containingDeclaration;
                inNamespaces.put(new Key(namespaceDescriptor, klass.getName()), klass.getConstructors());
            }
            else if (containingDeclaration instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
                inClasses.put(classDescriptor, klass.getConstructors());
            }
            else if (!(containingDeclaration instanceof FunctionDescriptor)) {
                throw new IllegalStateException();
            }
        }
        
        return Pair.create(inClasses, inNamespaces);
    }

    private void checkOverloadsInANamespace(MultiMap<Key, ConstructorDescriptor> inNamespaces) {

        MultiMap<Key, CallableMemberDescriptor> functionsByName = MultiMap.create();

        for (SimpleFunctionDescriptor function : context.getFunctions().values()) {
            DeclarationDescriptor containingDeclaration = function.getContainingDeclaration();
            if (containingDeclaration instanceof NamespaceDescriptor) {
                NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) containingDeclaration;
                functionsByName.putValue(new Key(namespaceDescriptor, function.getName()), function);
            }
        }
        
        for (PropertyDescriptor property : context.getProperties().values()) {
            DeclarationDescriptor containingDeclaration = property.getContainingDeclaration();
            if (containingDeclaration instanceof NamespaceDescriptor) {
                NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) containingDeclaration;
                functionsByName.putValue(new Key(namespaceDescriptor, property.getName()), property);
            }
        }
        
        for (Map.Entry<Key, Collection<ConstructorDescriptor>> entry : inNamespaces.entrySet()) {
            functionsByName.putValues(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Key, Collection<CallableMemberDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsWithSameName(e.getKey().getFunctionName(), e.getValue(), e.getKey().getNamespace());
        }
    }

    private String nameForErrorMessage(ClassDescriptor classDescriptor, JetClassOrObject jetClass) {
        String name = jetClass.getName();
        if (name != null) {
            return name;
        }
        if (jetClass instanceof JetObjectDeclaration) {
            // must be class object
            name = classDescriptor.getContainingDeclaration().getName().getName();
            return "class object " + name;
        }
        // safe
        return "<unknown>";
    }

    private void checkOverloadsInAClass(
            MutableClassDescriptor classDescriptor, JetClassOrObject klass,
            Collection<ConstructorDescriptor> nestedClassConstructors
    ) {
        MultiMap<Name, CallableMemberDescriptor> functionsByName = MultiMap.create();
        
        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS) {
            MutableClassDescriptor classObjectDescriptor = (MutableClassDescriptor) classDescriptor.getClassObjectDescriptor();
            assert classObjectDescriptor != null;
            for (CallableMemberDescriptor memberDescriptor : classObjectDescriptor.getDeclaredCallableMembers()) {
                functionsByName.putValue(memberDescriptor.getName(), memberDescriptor);
            }
        }

        for (CallableMemberDescriptor function : classDescriptor.getDeclaredCallableMembers()) {
            functionsByName.putValue(function.getName(), function);
        }
        
        for (ConstructorDescriptor nestedClassConstructor : nestedClassConstructors) {
            functionsByName.putValue(nestedClassConstructor.getContainingDeclaration().getName(), nestedClassConstructor);
        }
        
        for (Map.Entry<Name, Collection<CallableMemberDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsWithSameName(e.getKey(), e.getValue(), nameForErrorMessage(classDescriptor, klass));
        }

        // Kotlin has no secondary constructors at this time

    }
    
    private void checkOverloadsWithSameName(@NotNull Name name, Collection<CallableMemberDescriptor> functions, @NotNull String functionContainer) {
        if (functions.size() == 1) {
            // microoptimization
            return;
        }
        Set<Pair<JetDeclaration, CallableMemberDescriptor>> redeclarations = findRedeclarations(functions);
        reportRedeclarations(functionContainer, redeclarations);
    }

    @NotNull
    private Set<Pair<JetDeclaration, CallableMemberDescriptor>> findRedeclarations(@NotNull Collection<CallableMemberDescriptor> functions) {
        Set<Pair<JetDeclaration, CallableMemberDescriptor>> redeclarations = Sets.newHashSet();
        for (CallableMemberDescriptor member : functions) {
            for (CallableMemberDescriptor member2 : functions) {
                if (member == member2) {
                    continue;
                }

                OverloadUtil.OverloadCompatibilityInfo overloadable = OverloadUtil.isOverloadable(member, member2);
                if (!overloadable.isSuccess()) {
                    JetDeclaration jetDeclaration = (JetDeclaration) BindingContextUtils
                            .descriptorToDeclaration(trace.getBindingContext(), member);
                    if (jetDeclaration != null) {
                        redeclarations.add(Pair.create(jetDeclaration, member));
                    }
                }
            }
        }
        return redeclarations;
    }

    private void reportRedeclarations(@NotNull String functionContainer,
            @NotNull Set<Pair<JetDeclaration, CallableMemberDescriptor>> redeclarations) {
        for (Pair<JetDeclaration, CallableMemberDescriptor> redeclaration : redeclarations) {
            CallableMemberDescriptor memberDescriptor = redeclaration.getSecond();
            JetDeclaration jetDeclaration = redeclaration.getFirst();
            if (memberDescriptor instanceof PropertyDescriptor) {
                trace.report(Errors.REDECLARATION.on(jetDeclaration, memberDescriptor.getName().getName()));
            }
            else {
                trace.report(Errors.CONFLICTING_OVERLOADS.on(jetDeclaration, memberDescriptor, functionContainer));
            }
        }
    }
}
