package org.jetbrains.jet.lang.types;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public interface Type extends Annotated {
    TypeConstructor getConstructor();
    List<TypeProjection> getArguments();

    Collection<MemberDescriptor> getMembers();
}
