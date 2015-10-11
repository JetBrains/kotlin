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

public class DiagnosticWithParameters3<E extends PsiElement, A, B, C> extends AbstractDiagnostic<E> {
    private final A a;
    private final B b;
    private final C c;

    public DiagnosticWithParameters3(
            @NotNull E psiElement,
            @NotNull A a,
            @NotNull B b,
            @NotNull C c,
            @NotNull DiagnosticFactory3<E, A, B, C> factory,
            @NotNull Severity severity
    ) {
        super(psiElement, factory, severity);
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DiagnosticFactory3<E, A, B, C> getFactory() {
        return (DiagnosticFactory3<E, A, B, C>) super.getFactory();
    }

    @NotNull
    public A getA() {
        return a;
    }

    @NotNull
    public B getB() {
        return b;
    }

    @NotNull
    public C getC() {
        return c;
    }

    @Override
    public String toString() {
        return getFactory() + "(a = " + a + ", b = " + b + ", c = " + c + ")";
    }
}
