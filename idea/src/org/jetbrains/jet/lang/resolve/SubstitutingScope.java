package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.Map;

/**
 * @author abreslav
 */
public class SubstitutingScope implements JetScope {

    private final JetScope workerScope;
    private final Map<TypeConstructor, TypeProjection> substitutionContext;

    public SubstitutingScope(JetScope workerScope, Map<TypeConstructor, TypeProjection> substitutionContext) {
        this.workerScope = workerScope;
        this.substitutionContext = substitutionContext;
    }

    @Override
    public PropertyDescriptor getProperty(@NotNull String name) {
        PropertyDescriptor property = workerScope.getProperty(name);
        if (property == null || substitutionContext.isEmpty()) {
            return property;
        }

        JetType inType = substitute(property.getInType(), Variance.IN_VARIANCE);
        JetType outType = substitute(property.getOutType(), Variance.OUT_VARIANCE);
        if (inType == null && outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }
        return new PropertyDescriptorImpl(
                property.getContainingDeclaration(),
                property.getAttributes(), // TODO
                property.getName(),
                inType,
                outType
        );
    }

    @Nullable
    private JetType substitute(@Nullable JetType originalType, @NotNull Variance variance) {
        if (originalType == null) return null;

        return TypeSubstitutor.INSTANCE.substitute(substitutionContext, originalType, variance);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassifierDescriptor descriptor = workerScope.getClassifier(name);
        if (descriptor == null) {
            return null;
        }
        if (descriptor instanceof ClassDescriptor) {
            return new LazySubstitutingClassDescriptor((ClassDescriptor) descriptor, substitutionContext);
        }
        throw new UnsupportedOperationException();
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
        if (substitutionContext.isEmpty()) {
            return functionGroup;
        }
        return new LazySubstitutingFunctionGroup(substitutionContext, functionGroup);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return workerScope.getContainingDeclaration();
    }
}
