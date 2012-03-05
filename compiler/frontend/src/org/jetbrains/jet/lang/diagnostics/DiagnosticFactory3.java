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

package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public class DiagnosticFactory3<E extends PsiElement, A, B, C> extends DiagnosticFactoryWithMessageFormat<E> {
    private final Renderer<? super A> rendererForA;
    private final Renderer<? super B> rendererForB;
    private final Renderer<? super C> rendererForC;
    
    protected DiagnosticFactory3(Severity severity, String messageStub, PositioningStrategy<? super E> positioningStrategy, Renderer<? super A> rendererForA, Renderer<? super B> rendererForB, Renderer<? super C> rendererForC) {
        super(severity, messageStub, positioningStrategy);
        this.rendererForA = rendererForA;
        this.rendererForB = rendererForB;
        this.rendererForC = rendererForC;
    }

    public static <T extends PsiElement, A, B, C> DiagnosticFactory3<T, A, B, C> create(Severity severity, String messageStub) {
        return create(severity, messageStub, PositioningStrategies.DEFAULT);
    }

    public static <T extends PsiElement, A, B, C> DiagnosticFactory3<T, A, B, C> create(Severity severity, String messageStub, PositioningStrategy<? super T> positioningStrategy) {
        return create(severity, messageStub, positioningStrategy, Renderers.TO_STRING, Renderers.TO_STRING, Renderers.TO_STRING);
    }

    public static <T extends PsiElement, A, B, C> DiagnosticFactory3<T, A, B, C> create(Severity severity, String messageStub, Renderer<? super A> rendererForA, Renderer<? super B> rendererForB, Renderer<? super C> rendererForC) {
        return create(severity, messageStub, PositioningStrategies.DEFAULT, rendererForA, rendererForB, rendererForC);
    }

    public static <T extends PsiElement, A, B, C> DiagnosticFactory3<T, A, B, C> create(Severity severity, String messageStub, PositioningStrategy<? super T> positioningStrategy, Renderer<? super A> rendererForA, Renderer<? super B> rendererForB, Renderer<? super C> rendererForC) {
        return new DiagnosticFactory3<T, A, B, C>(severity, messageStub, positioningStrategy, rendererForA, rendererForB, rendererForC);
    }

    private String makeMessage(@NotNull A a, @NotNull B b, @NotNull C c) {
        return messageFormat.format(new Object[]{makeMessageForA(a), makeMessageForB(b), makeMessageForC(c)});
    }

    private String makeMessageForA(@NotNull A a) {
        return rendererForA.render(a);
    }

    private String makeMessageForB(@NotNull B b) {
        return rendererForB.render(b);
    }

    private String makeMessageForC(@NotNull C c) {
        return rendererForC.render(c);
    }
    @NotNull
    public ParametrizedDiagnostic<E> on(@NotNull E element, @NotNull A a, @NotNull B b, @NotNull C c) {
        return new DiagnosticWithPsiElement<E>(element, this, severity, makeMessage(a, b, c));
    }
}
