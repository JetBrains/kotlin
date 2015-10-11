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

public class DiagnosticWithParameters2<E extends PsiElement, A, B> extends AbstractDiagnostic<E> {
    private final A a;
    private final B b;

    public DiagnosticWithParameters2(
            @NotNull E psiElement,
            @NotNull A a,
            @NotNull B b,
            @NotNull DiagnosticFactory2<E, A, B> factory,
            @NotNull Severity severity
    ) {
        super(psiElement, factory, severity);
        this.a = a;
        this.b = b;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DiagnosticFactory2<E, A, B> getFactory() {
        return (DiagnosticFactory2<E, A, B>) super.getFactory();
    }

    @NotNull
    public A getA() {
        return a;
    }

    @NotNull
    public B getB() {
        return b;
    }

    @Override
    public String toString() {
        return getFactory() + "(a = " + a + ", b = " + b + ")";
    }
}
