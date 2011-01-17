package org.jetbrains.jet.lang.types;


import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TypeVariable extends TypeImpl {


    public TypeVariable(List<Annotation> annotations, TypeParameterDescriptor typeParameter) {
        super(annotations, typeParameter.getTypeConstructor(), Collections.<TypeProjection>emptyList());
    }

    @Override
    public Collection<MemberDescriptor> getMembers() {
        throw new javax.help.UnsupportedOperationException(); // TODO
    }
}
