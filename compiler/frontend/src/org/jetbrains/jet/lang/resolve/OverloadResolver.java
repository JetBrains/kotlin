package org.jetbrains.jet.lang.resolve;

import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;

import java.util.Collection;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.DELEGATED;

/**
 * @author Stepan Koltsov
 */
public class OverloadResolver {
    private final TopDownAnalysisContext context;

    public OverloadResolver(@NotNull TopDownAnalysisContext context) {
        this.context = context;
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
            this(DescriptorUtils.getFQName(namespaceDescriptor), name);
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

        MultiMap<Key, FunctionDescriptor> functionsByName = MultiMap.create();

        for (FunctionDescriptorImpl function : context.getFunctions().values()) {
            DeclarationDescriptor containingDeclaration = function.getContainingDeclaration();
            if (containingDeclaration instanceof NamespaceDescriptor) {
                NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) containingDeclaration;
                functionsByName.putValue(new Key(namespaceDescriptor, function.getName()), function);
            }
        }
        
        for (Map.Entry<Key, Collection<ConstructorDescriptor>> entry : inNamespaces.entrySet()) {
            functionsByName.putValues(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Key, Collection<FunctionDescriptor>> e : functionsByName.entrySet()) {
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
        MultiMap<String, FunctionDescriptor> functionsByName = MultiMap.create();
        
        for (FunctionDescriptor function : classDescriptor.getFunctions()) {
            functionsByName.putValue(function.getName(), function);
        }
        
        for (ConstructorDescriptor nestedClassConstructor : nestedClassConstructors) {
            functionsByName.putValue(nestedClassConstructor.getContainingDeclaration().getName(), nestedClassConstructor);
        }
        
        for (Map.Entry<String, Collection<FunctionDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsWithSameName(e.getKey(), e.getValue(), nameForErrorMessage(classDescriptor, klass));
        }

        // properties are checked elsewhere

        // Kotlin has no secondary constructors at this time

    }
    
    private void checkOverloadsWithSameName(String name, Collection<FunctionDescriptor> functions, @NotNull String functionContainer) {
        if (functions.size() == 1) {
            // microoptimization
            return;
        }
        
        for (FunctionDescriptor function : functions) {
            for (FunctionDescriptor function2 : functions) {
                if (function == function2) {
                    continue;
                }

                OverloadUtil.OverloadCompatibilityInfo overloadble = OverloadUtil.isOverloadble(function, function2);
                if (!overloadble.isSuccess()) {
                    JetDeclaration member = (JetDeclaration) context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, function);
                    if (member == null) {
                        assert context.getTrace().get(DELEGATED, function);
                        return;
                    }

                    context.getTrace().report(Errors.CONFLICTING_OVERLOADS.on(member, function, functionContainer));
                }
            }
        }
    }

}
