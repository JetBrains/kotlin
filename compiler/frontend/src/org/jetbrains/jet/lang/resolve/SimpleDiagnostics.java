package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;

import java.util.Collection;
import java.util.Iterator;

public class SimpleDiagnostics implements Diagnostics {
    private final Collection<Diagnostic> diagnostics;

    public SimpleDiagnostics(@NotNull Collection<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    @NotNull
    @Override
    public Collection<Diagnostic> all() {
        return diagnostics;
    }

    @NotNull
    @Override
    public Diagnostics noSuppression() {
        return this;
    }

    @NotNull
    @Override
    public Iterator<Diagnostic> iterator() {
        return all().iterator();
    }

    @Override
    public boolean isEmpty() {
        return all().isEmpty();
    }
}
