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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class DiagnosticFactory1<E extends PsiElement, A> extends DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters1<E, A>> {
    @NotNull
    public ParametrizedDiagnostic<E> on(@NotNull E element, @NotNull A argument) {
        return new DiagnosticWithParameters1<>(element, argument, this, getSeverity());
    }

    protected DiagnosticFactory1(Severity severity, PositioningStrategy<? super E> positioningStrategy) {
        super(severity, positioningStrategy);
    }

    public static <T extends PsiElement, A> DiagnosticFactory1<T, A> create(Severity severity, PositioningStrategy<? super T> positioningStrategy) {
        return new DiagnosticFactory1<>(severity, positioningStrategy);
    }

    public static <T extends PsiElement, A> DiagnosticFactory1<T, A> create(Severity severity) {
        return create(severity, PositioningStrategies.DEFAULT);
    }
}
