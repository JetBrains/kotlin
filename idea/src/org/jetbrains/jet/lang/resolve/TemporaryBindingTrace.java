package org.jetbrains.jet.lang.resolve;

/**
 * @author abreslav
 */
public class TemporaryBindingTrace extends DelegatingBindingTrace {

    public static TemporaryBindingTrace create(BindingTrace trace) {
        return new TemporaryBindingTrace(trace);
    }

    private final BindingTrace trace;

    private TemporaryBindingTrace(BindingTrace trace) {
        super(trace.getBindingContext());
        this.trace = trace;
    }

    public void commit() {
        addAllMyDataTo(trace);
    }
}
