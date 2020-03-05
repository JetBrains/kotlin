/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer;

import java.util.Arrays;
import java.util.Collection;

public abstract class DiagnosticFactory<D extends Diagnostic> {

    private String name = null;
    private final Severity severity;

    private DiagnosticRenderer<D> defaultRenderer;

    protected DiagnosticFactory(@NotNull Severity severity) {
        this.severity = severity;
    }

    protected DiagnosticFactory(@NotNull String name, @NotNull Severity severity) {
        this.name = name;
        this.severity = severity;
    }

    /*package*/ void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Severity getSeverity() {
        return severity;
    }

    @Nullable
    public DiagnosticRenderer<D> getDefaultRenderer() {
        return defaultRenderer;
    }

    void setDefaultRenderer(@Nullable DiagnosticRenderer<D> defaultRenderer) {
        this.defaultRenderer = defaultRenderer;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public D cast(@NotNull Diagnostic diagnostic) {
        if (diagnostic.getFactory() != this) {
            throw new IllegalArgumentException("Factory mismatch: expected " + this + " but was " + diagnostic.getFactory());
        }

        return (D) diagnostic;
    }

    @NotNull
    @SafeVarargs
    public static <D extends Diagnostic> D cast(@NotNull Diagnostic diagnostic, @NotNull DiagnosticFactory<? extends D>... factories) {
        return cast(diagnostic, Arrays.asList(factories));
    }

    @NotNull
    public static <D extends Diagnostic> D cast(@NotNull Diagnostic diagnostic, @NotNull Collection<? extends DiagnosticFactory<? extends D>> factories) {
        for (DiagnosticFactory<? extends D> factory : factories) {
            if (diagnostic.getFactory() == factory) return factory.cast(diagnostic);
        }

        throw new IllegalArgumentException("Factory mismatch: expected one of " + factories + " but was " + diagnostic.getFactory());
    }

    @Override
    public String toString() {
        return getName();
    }
}
