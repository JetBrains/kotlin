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

package org.jetbrains.kotlin.diagnostics.rendering;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.*;

import java.util.HashMap;
import java.util.Map;

public final class DiagnosticFactoryToRendererMap {
    private final Map<DiagnosticFactory<?>, DiagnosticRenderer<?>> map = new HashMap<>();
    private boolean immutable = false;

    // TO catch EA-75872
    private final String name;
    public DiagnosticFactoryToRendererMap(String name) {
        this.name = name;
    }
    public DiagnosticFactoryToRendererMap() {
        this("<unnamed>");
    }
    @Override
    public String toString() {
        return "DiagnosticFactory#" + name;
    }

    private void checkMutability() {
        if (immutable) {
            throw new IllegalStateException("factory to renderer map is already immutable");
        }
    }

    public <E extends PsiElement> void put(@NotNull DiagnosticFactory0<E> factory, @NotNull String message) {
        checkMutability();
        map.put(factory, new SimpleDiagnosticRenderer(message));
    }

    public <E extends PsiElement, A> void put(@NotNull DiagnosticFactory1<E, A> factory, @NotNull String message, @Nullable DiagnosticParameterRenderer<? super A> rendererA) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters1Renderer<A>(message, rendererA));
    }

    public <E extends PsiElement, A> void put(@NotNull DiagnosticFactory1<E, A> factory, @NotNull String message, @NotNull MultiRenderer<? super A> rendererA) {
        checkMutability();
        map.put(factory, new DiagnosticWithParametersMultiRenderer<A>(message, rendererA));
    }

    public <E extends PsiElement, A, B> void put(@NotNull DiagnosticFactory2<E, A, B> factory,
            @NotNull String message,
            @Nullable DiagnosticParameterRenderer<? super A> rendererA,
            @Nullable DiagnosticParameterRenderer<? super B> rendererB) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters2Renderer<A, B>(message, rendererA, rendererB));
    }

    public <E extends PsiElement, A, B, C> void put(@NotNull DiagnosticFactory3<E, A, B, C> factory,
            @NotNull String message,
            @Nullable DiagnosticParameterRenderer<? super A> rendererA,
            @Nullable DiagnosticParameterRenderer<? super B> rendererB,
            @Nullable  DiagnosticParameterRenderer<? super C> rendererC) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters3Renderer<A, B, C>(message, rendererA, rendererB, rendererC));
    }

    @Nullable
    public DiagnosticRenderer<?> get(@NotNull DiagnosticFactory<?> factory) {
        return map.get(factory);
    }

    public void setImmutable() {
        immutable = false;
    }
}
