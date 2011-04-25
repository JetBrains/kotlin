package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;

/**
 * @author abreslav
 */
public class SubstitutingScope implements JetScope {

    private final JetScope workerScope;
//    private final Map<TypeConstructor, TypeProjection> substitutionContext;
    private final TypeSubstitutor substitutor;

    public SubstitutingScope(JetScope workerScope, @NotNull TypeSubstitutor substitutor) {
        this.workerScope = workerScope;
        this.substitutor = substitutor;
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        VariableDescriptor variable = workerScope.getVariable(name);
        if (variable == null || substitutor.isEmpty()) {
            return variable;
        }

        return variable.substitute(substitutor);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassifierDescriptor descriptor = workerScope.getClassifier(name);
        if (descriptor == null) {
            return null;
        }
        if (descriptor instanceof ClassDescriptor) {
            return new LazySubstitutingClassDescriptor((ClassDescriptor) descriptor, substitutor);
        }
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return workerScope.getNamespace(name); // TODO
    }

    @NotNull
    @Override
    public JetType getThisType() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        FunctionGroup functionGroup = workerScope.getFunctionGroup(name);
        if (substitutor.isEmpty()) {
            return functionGroup;
        }
        return new LazySubstitutingFunctionGroup(substitutor, functionGroup);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return workerScope.getContainingDeclaration();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        throw new UnsupportedOperationException(); // TODO
    }
}
