package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ChainedScope implements JetScope {
    private final DeclarationDescriptor containingDeclaration;
    private final JetScope[] scopeChain;
    private Collection<DeclarationDescriptor> allDescriptors;

    public ChainedScope(DeclarationDescriptor containingDeclaration, JetScope... scopes) {
        this.containingDeclaration = containingDeclaration;
        scopeChain = scopes.clone();
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        for (JetScope scope : scopeChain) {
            ClassifierDescriptor classifier = scope.getClassifier(name);
            if (classifier != null) return classifier;
        }
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        for (JetScope jetScope : scopeChain) {
            NamespaceDescriptor namespace = jetScope.getNamespace(name);
            if (namespace != null) {
                return namespace;
            }
        }
        return null;
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        for (JetScope jetScope : scopeChain) {
            VariableDescriptor variable = jetScope.getVariable(name);
            if (variable != null) {
                return variable;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        if (scopeChain.length == 0) {
            return FunctionGroup.EMPTY;
        }

        WritableFunctionGroup result = new WritableFunctionGroup(name);
        for (JetScope jetScope : scopeChain) {
            result.addAllFunctions(jetScope.getFunctionGroup(name));
        }

        return result;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        for (JetScope jetScope : scopeChain) {
            jetScope.getImplicitReceiversHierarchy(result);
        }
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        for (JetScope jetScope : scopeChain) {
            Collection<DeclarationDescriptor> declarationsByLabel = jetScope.getDeclarationsByLabel(labelName);
            if (!declarationsByLabel.isEmpty()) return declarationsByLabel; // TODO : merge?
        }
        return Collections.emptyList();
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        for (JetScope jetScope : scopeChain) {
            PropertyDescriptor propertyByFieldReference = jetScope.getPropertyByFieldReference(fieldName);
            if (propertyByFieldReference != null) {
                return propertyByFieldReference;
            }
        }
        return null;
    }

    @Override
    public DeclarationDescriptor getDeclarationDescriptorForUnqualifiedThis() {
        if (DescriptorUtils.definesItsOwnThis(getContainingDeclaration())) {
            return getContainingDeclaration();
        }

        for (JetScope jetScope : scopeChain) {
            DeclarationDescriptor containingDeclaration = jetScope.getContainingDeclaration();
            if (DescriptorUtils.definesItsOwnThis(containingDeclaration)) {
                return containingDeclaration;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();
            for (JetScope scope : scopeChain) {
                allDescriptors.addAll(scope.getAllDescriptors());
            }
        }
        return allDescriptors;
    }
}
