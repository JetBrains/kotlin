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

package org.jetbrains.jet.lang.resolve;

import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import javax.inject.Inject;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;

import java.util.Collection;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.DELEGATED;

/**
 * @author Stepan Koltsov
 */
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

    private static class Key extends Pair<String, String> {
        Key(String namespace, String name) {
            super(namespace, name);
        }
        
        Key(NamespaceDescriptor namespaceDescriptor, String name) {
            this(DescriptorUtils.getFQName(namespaceDescriptor).getFqName(), name);
        }

        public String getNamespace() {
            return first;
        }

        public String getFunctionName() {
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
            } else if (containingDeclaration instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
                inClasses.put(classDescriptor, klass.getConstructors());
            } else {
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
            name = classDescriptor.getContainingDeclaration().getName();
            return "class object " + name;
        }
        // safe
        return "<unknown>";
    }

    private void checkOverloadsInAClass(
            MutableClassDescriptor classDescriptor, JetClassOrObject klass,
            Collection<ConstructorDescriptor> nestedClassConstructors)
    {
        MultiMap<String, CallableMemberDescriptor> functionsByName = MultiMap.create();
        
        for (CallableMemberDescriptor function : classDescriptor.getCallableMembers()) {
            functionsByName.putValue(function.getName(), function);
        }
        
        for (ConstructorDescriptor nestedClassConstructor : nestedClassConstructors) {
            functionsByName.putValue(nestedClassConstructor.getContainingDeclaration().getName(), nestedClassConstructor);
        }
        
        for (Map.Entry<String, Collection<CallableMemberDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsWithSameName(e.getKey(), e.getValue(), nameForErrorMessage(classDescriptor, klass));
        }

        // properties are checked elsewhere

        // Kotlin has no secondary constructors at this time

    }
    
    private void checkOverloadsWithSameName(String name, Collection<CallableMemberDescriptor> functions, @NotNull String functionContainer) {
        if (functions.size() == 1) {
            // microoptimization
            return;
        }
        
        for (CallableMemberDescriptor function : functions) {
            for (CallableMemberDescriptor function2 : functions) {
                if (function == function2) {
                    continue;
                }

                OverloadUtil.OverloadCompatibilityInfo overloadable = OverloadUtil.isOverloadable(function, function2);
                if (!overloadable.isSuccess()) {
                    JetDeclaration member = (JetDeclaration) trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, function);
                    if (member == null) {
                        assert trace.get(DELEGATED, function);
                        return;
                    }

                    if (function instanceof PropertyDescriptor) {
                        trace.report(Errors.REDECLARATION.on(function, trace.getBindingContext()));
                    } else {
                        trace.report(Errors.CONFLICTING_OVERLOADS.on(member, function, functionContainer));
                    }
                }
            }
        }
    }

}
