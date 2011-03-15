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

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            List<Attribute> attributes,
            String name) {
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
                                                List<TypeParameterDescriptor> typeParameters,
                                                Collection<? extends JetType> superclasses, JetScope memberDeclarations) {
        this.typeConstructor = new TypeConstructor(getAttributes(), sealed, getName(), typeParameters, superclasses);
        this.memberDeclarations = memberDeclarations;
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
        if (typeConstructor.getParameters().isEmpty()) {
            return  memberDeclarations;
        }
        Map<TypeConstructor,TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
        return new SubstitutingScope(memberDeclarations, substitutionContext);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }
}