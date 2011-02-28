package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableDeclarationDescriptor implements ClassDescriptor {
    private final WritableScope unsubstitutedMemberScope;
    private TypeConstructor typeConstructor;

    public MutableClassDescriptor(@NotNull JetScope outerScope) {
        this.unsubstitutedMemberScope = new WritableScope(outerScope);
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public void setTypeConstructor(@NotNull TypeConstructor typeConstructor) {
        this.typeConstructor = typeConstructor;
    }

    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        List<TypeParameterDescriptor> typeParameters = getTypeConstructor().getParameters();
        Map<TypeConstructor,TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.buildSubstitutionContext(typeParameters, typeArguments);
        return new SubstitutingScope(unsubstitutedMemberScope, substitutionContext);
    }

    public WritableScope getUnsubstitutedMemberScope() {
        return unsubstitutedMemberScope;
    }
}
