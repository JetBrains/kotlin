package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.Attribute;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class LazySubstitutingClassDescriptor implements ClassDescriptor {

    private final ClassDescriptor original;
    private final Map<TypeConstructor,TypeProjection> substitutionContext;

    public LazySubstitutingClassDescriptor(ClassDescriptor descriptor, Map<TypeConstructor, TypeProjection> substitutionContext) {
        this.original = descriptor;
        this.substitutionContext = substitutionContext;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        if (substitutionContext.isEmpty()) {
            return original.getTypeConstructor();
        }
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        JetScope memberScope = original.getMemberScope(typeArguments);
        if (substitutionContext.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, substitutionContext);
    }

    @Override
    public List<Attribute> getAttributes() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public String getName() {
        return original.getName();
    }
}
