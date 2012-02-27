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
/**
 * @author svtk
 */
public class UnusedElementDiagnosticFactory<T extends PsiElement, A> extends DiagnosticFactory1<T, A> {
    private UnusedElementDiagnosticFactory(Severity severity, String message, PositioningStrategy<? super T> positioningStrategy, Renderer<? super A> renderer) {
        super(severity, message, positioningStrategy, renderer);
    }

    public static <T extends PsiElement, A> UnusedElementDiagnosticFactory<T, A> create(Severity severity, String message, PositioningStrategy<? super T> positioningStrategy, Renderer<? super A> renderer) {
        return new UnusedElementDiagnosticFactory<T, A>(severity, message, positioningStrategy, renderer);
    }

    public static <T extends PsiElement, A> UnusedElementDiagnosticFactory<T, A> create(Severity severity, String message, PositioningStrategy<? super T> positioningStrategy) {
        return create(severity, message, positioningStrategy, Renderers.TO_STRING);
    }

    public static <T extends PsiElement, A> UnusedElementDiagnosticFactory<T, A> create(Severity severity, String message, Renderer<? super A> renderer) {
        return create(severity, message, PositioningStrategies.DEFAULT, renderer);
    }

    public static <T extends PsiElement, A> UnusedElementDiagnosticFactory<T, A> create(Severity severity, String message) {
        return create(severity, message, PositioningStrategies.DEFAULT, Renderers.TO_STRING);
    }
}
