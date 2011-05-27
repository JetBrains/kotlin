package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;

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