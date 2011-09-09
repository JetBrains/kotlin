package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.LazyValue;

/**
 * @author abreslav
 */
public class LazyScopeAdapter extends AbstractScopeAdapter {

    private final LazyValue<JetScope> scope;

    public LazyScopeAdapter(LazyValue<JetScope> scope) {
        this.scope = scope;
    }

    @NotNull
    @Override
    protected JetScope getWorkerScope() {
        return scope.get();
    }
}
