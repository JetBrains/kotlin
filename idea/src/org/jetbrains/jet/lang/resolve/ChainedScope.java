package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public class ChainedScope implements JetScope {
    private final DeclarationDescriptor containingDeclaration;
    private final JetScope[] scopeChain;

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
        for (JetScope jetScope : scopeChain) {
            FunctionGroup functionGroup = jetScope.getFunctionGroup(name);
            if (!functionGroup.isEmpty()) return functionGroup;
        }
        return FunctionGroup.EMPTY;
    }

    @NotNull
    @Override
    public JetType getThisType() {
        throw new UnsupportedOperationException(); // TODO
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
}
