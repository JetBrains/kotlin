/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.diagnostics.rendering;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgeny Gerashchenko
 * @since 4/13/12
 */
public class DiagnosticFactoryToRendererMap {
    private final Map<AbstractDiagnosticFactory, DiagnosticRenderer<?>> map =
            new HashMap<AbstractDiagnosticFactory, DiagnosticRenderer<?>>();

    public final <E extends PsiElement> void put(SimpleDiagnosticFactory<E> factory, String message) {
        map.put(factory, new SimpleDiagnosticRenderer(message));
    }

    public final <E extends PsiElement, A> void put(DiagnosticFactory1<E, A> factory, String message, Renderer<? super A> rendererA) {
        map.put(factory, new DiagnosticWithParameters1Renderer<A>(message, rendererA));
    }

    public final <E extends PsiElement, A, B> void put(DiagnosticFactory2<E, A, B> factory,
            String message,
            Renderer<? super A> rendererA,
            Renderer<? super B> rendererB) {
        map.put(factory, new DiagnosticWithParameters2Renderer<A, B>(message, rendererA, rendererB));
    }

    public final <E extends PsiElement, A, B, C> void put(DiagnosticFactory3<E, A, B, C> factory,
            String message,
            Renderer<? super A> rendererA,
            Renderer<? super B> rendererB,
            Renderer<? super C> rendererC) {
        map.put(factory, new DiagnosticWithParameters3Renderer<A, B, C>(message, rendererA, rendererB, rendererC));
    }

    @Nullable
    public final DiagnosticRenderer<?> get(@NotNull AbstractDiagnosticFactory factory) {
        return map.get(factory);
    }
}
