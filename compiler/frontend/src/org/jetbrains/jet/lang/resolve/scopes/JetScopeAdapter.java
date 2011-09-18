package org.jetbrains.jet.lang.resolve.scopes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.AbstractScopeAdapter;

/**
 * @author abreslav
 */
public class JetScopeAdapter extends AbstractScopeAdapter {
    @NotNull
    private final JetScope scope;

    public JetScopeAdapter(@NotNull JetScope scope) {
        this.scope = scope;
    }

    @NotNull
    @Override
    protected final JetScope getWorkerScope() {
        return scope;
    }
}