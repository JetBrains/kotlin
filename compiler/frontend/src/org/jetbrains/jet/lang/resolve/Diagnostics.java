package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public interface Diagnostics extends Iterable<Diagnostic> {
    @NotNull
    Collection<Diagnostic> all();

    boolean isEmpty();

    @NotNull
    Diagnostics noSuppression();

    Diagnostics EMPTY = new Diagnostics() {
        @NotNull
        @Override
        public Collection<Diagnostic> all() {
            return Collections.emptyList();
        }

        @Override
        public boolean isEmpty() {
            return true;
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
    };
}
