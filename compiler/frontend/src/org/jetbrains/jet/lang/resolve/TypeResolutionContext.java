package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

public class TypeResolutionContext {
    public final JetScope scope;
    public final BindingTrace trace;
    public final boolean checkBounds;
    public final boolean allowBareTypes;

    public TypeResolutionContext(@NotNull JetScope scope, @NotNull BindingTrace trace, boolean checkBounds, boolean allowBareTypes) {
        this.scope = scope;
        this.trace = trace;
        this.checkBounds = checkBounds;
        this.allowBareTypes = allowBareTypes;
    }

    public TypeResolutionContext noBareTypes() {
        return new TypeResolutionContext(scope, trace, checkBounds, false);
    }
}
