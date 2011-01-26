package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class MemberDescriptorImpl extends NamedAnnotatedImpl {
    public MemberDescriptorImpl(List<Attribute> attributes, String name) {
        super(attributes, name);
    }
}
