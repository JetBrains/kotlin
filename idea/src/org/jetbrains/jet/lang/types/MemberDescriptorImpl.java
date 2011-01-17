package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class MemberDescriptorImpl extends NamedAnnotatedImpl {
    public MemberDescriptorImpl(List<Annotation> annotations, String name) {
        super(annotations, name);
    }
}
