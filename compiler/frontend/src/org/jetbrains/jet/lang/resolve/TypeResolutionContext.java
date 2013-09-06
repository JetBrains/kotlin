package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

public class TypeResolutionContext {
    public final JetScope scope;
    public final BindingTrace trace;
    public final boolean checkBounds;

    public TypeResolutionContext(@NotNull JetScope scope, @NotNull BindingTrace trace, boolean checkBounds) {
        this.scope = scope;
        this.trace = trace;
        this.checkBounds = checkBounds;
    }
}
