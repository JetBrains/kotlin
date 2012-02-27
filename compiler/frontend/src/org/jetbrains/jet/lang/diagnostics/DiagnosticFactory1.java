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
public class DiagnosticFactory1<P extends PsiElement, A> extends DiagnosticFactoryWithMessageFormat<P> {
    private final Renderer<? super A> renderer;

    protected String makeMessage(@NotNull A argument) {
        return messageFormat.format(new Object[]{makeMessageFor(argument)});
    }

    protected String makeMessageFor(@NotNull A argument) {
        return renderer.render(argument);
    }
    
    @NotNull
    public DiagnosticWithPsiElement<P> on(@NotNull P element, @NotNull A argument) {
        return new DiagnosticWithPsiElement<P>(element, this, severity, makeMessage(argument));
    }

    protected DiagnosticFactory1(Severity severity, String message, PositioningStrategy<? super P> positioningStrategy, Renderer<? super A> renderer) {
        super(severity, message, positioningStrategy);
        this.renderer = renderer;
    }

    public static <T extends PsiElement, A> DiagnosticFactory1<T, A> create(Severity severity, String message, PositioningStrategy<? super T> positioningStrategy, Renderer<? super A> renderer) {
        return new DiagnosticFactory1<T, A>(severity, message, positioningStrategy, renderer);
    }

    public static <T extends PsiElement, A> DiagnosticFactory1<T, A> create(Severity severity, String message, PositioningStrategy<? super T> positioningStrategy) {
        return create(severity, message, positioningStrategy, Renderers.TO_STRING);
    }

    public static <T extends PsiElement, A> DiagnosticFactory1<T, A> create(Severity severity, String message, Renderer<? super A> renderer) {
        return create(severity, message, PositioningStrategies.DEFAULT, renderer);
    }

    public static <T extends PsiElement, A> DiagnosticFactory1<T, A> create(Severity severity, String message) {
        return create(severity, message, PositioningStrategies.DEFAULT, Renderers.TO_STRING);
    }
}