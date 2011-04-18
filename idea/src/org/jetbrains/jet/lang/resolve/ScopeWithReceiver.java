package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public class ScopeWithReceiver extends JetScopeImpl {

    private final JetScope receiverTypeScope;
    private final JetScope outerScope;

    public ScopeWithReceiver(JetScope outerScope, JetType receiverType) {
        this.outerScope = outerScope;
        this.receiverTypeScope = receiverType.getMemberScope();
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        return receiverTypeScope.getFunctionGroup(name); // TODO
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return super.getClassifier(name); // TODO
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        return receiverTypeScope.getVariable(name);
        // TODO : extension properties
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return receiverTypeScope.getNamespace(name);
    }

    @NotNull
    @Override
    public JetType getThisType() {
        return receiverTypeScope.getThisType();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return outerScope.getContainingDeclaration();
    }
}
