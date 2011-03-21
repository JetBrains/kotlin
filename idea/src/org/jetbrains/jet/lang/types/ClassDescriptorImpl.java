package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.SubstitutingScope;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class ClassDescriptorImpl extends DeclarationDescriptorImpl implements ClassDescriptor {
    private TypeConstructor typeConstructor;

    private JetScope memberDeclarations;
    private FunctionGroup constructors;

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Attribute> attributes,
            @NotNull String name) {
        super(containingDeclaration, attributes, name);
    }

//    public ClassDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, String name, JetScope memberDeclarations) {
//        this(containingDeclaration, Collections.<Attribute>emptyList(), name);
//        this.initialize(Collections.<Attribute>emptyList(), true,
//                name, Collections.<TypeParameterDescriptor>emptyList(),
//                Collections.<Type>singleton(JetStandardClasses.getAnyType()), memberDeclarations);
//    }
//
    public final ClassDescriptorImpl initialize(boolean sealed,
                                                @NotNull List<TypeParameterDescriptor> typeParameters,
                                                @NotNull Collection<? extends JetType> superclasses,
                                                @NotNull JetScope memberDeclarations,
                                                @NotNull FunctionGroup constructors) {
        this.typeConstructor = new TypeConstructorImpl(this, getAttributes(), sealed, getName(), typeParameters, superclasses);
        this.memberDeclarations = memberDeclarations;
        this.constructors = constructors;
        return this;
    }

    @Override
    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    @NotNull
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == typeConstructor.getParameters().size();
        if (typeConstructor.getParameters().isEmpty()) {
            return  memberDeclarations;
        }
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
        return new SubstitutingScope(memberDeclarations, substitutionContext);
    }

    @NotNull
    @Override
    public FunctionGroup getConstructors(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == getTypeConstructor().getParameters().size();
        if (typeArguments.size() == 0) {
            return constructors;
        }
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.buildSubstitutionContext(getTypeConstructor().getParameters(), typeArguments);
        return new LazySubstitutingFunctionGroup(substitutionContext, constructors);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }
}