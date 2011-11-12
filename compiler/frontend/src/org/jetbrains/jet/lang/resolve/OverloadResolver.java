package org.jetbrains.jet.lang.resolve;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey());
        }
        checkOverloadsInANamespace();
    }

    private void checkOverloadsInANamespace() {
        class Key extends Pair<String, String> {
            Key(String namespace, String name) {
                super(namespace, name);
            }
            
            public String getNamespace() {
                return first;
            }
            
            public String getFunctionName() {
                return second;
            }
        }

        MultiMap<Key, FunctionDescriptor> functionsByName = MultiMap.create();

        for (FunctionDescriptorImpl function : context.getFunctions().values()) {
            DeclarationDescriptor containingDeclaration = function.getContainingDeclaration();
            if (containingDeclaration instanceof NamespaceDescriptor) {
                NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) containingDeclaration;
                functionsByName.putValue(new Key(DescriptorRenderer.getFQName(namespaceDescriptor), function.getName()), function);
            }
        }

        for (Map.Entry<Key, Collection<FunctionDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsWithSameName(e.getKey().getFunctionName(), e.getValue(), e.getKey().getNamespace());
        }
    }

    private void checkOverloadsInAClass(MutableClassDescriptor classDescriptor, JetClassOrObject klass) {
        MultiMap<String, FunctionDescriptor> functionsByName = MultiMap.create();
        
        for (FunctionDescriptor function : classDescriptor.getFunctions()) {
            functionsByName.putValue(function.getName(), function);
        }
        
        for (Map.Entry<String, Collection<FunctionDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsWithSameName(e.getKey(), e.getValue(), klass.getName());
        }

        // properties are checked elsewhere

        // Kotlin has no secondary constructors at this time

    }
    
    private void checkOverloadsWithSameName(String name, Collection<FunctionDescriptor> functions, String functionContainer) {
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
                    JetNamedFunction member = (JetNamedFunction) context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, function);
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
