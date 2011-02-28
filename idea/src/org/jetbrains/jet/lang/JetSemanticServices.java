package org.jetbrains.jet.lang;

import org.jetbrains.jet.lang.resolve.ClassDescriptorResolver;
import org.jetbrains.jet.lang.types.BindingTrace;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetTypeInferrer;

/**
 * @author abreslav
 */
public class JetSemanticServices {
    private final JetStandardLibrary standardLibrary;
    private final JetTypeInferrer typeInferrer;
    private final ClassDescriptorResolver classDescriptorResolver;

    public JetSemanticServices(JetStandardLibrary standardLibrary) {
        this.standardLibrary = standardLibrary;
        this.typeInferrer = new JetTypeInferrer(BindingTrace.DUMMY, this);
        this.classDescriptorResolver = new ClassDescriptorResolver(this);
    }

    public JetStandardLibrary getStandardLibrary() {
        return standardLibrary;
    }

    public JetTypeInferrer getTypeInferrer() {
        return typeInferrer;
    }

    public ClassDescriptorResolver getClassDescriptorResolver() {
        return classDescriptorResolver;
    }

    public JetTypeInferrer getTypeInferrer(BindingTrace trace) {
        return new JetTypeInferrer(trace, this);
    }
}
