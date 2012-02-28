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
public class DiagnosticFactory<P extends PsiElement> extends DiagnosticFactoryWithPsiElement<P> {
    protected final String message;

    protected DiagnosticFactory(Severity severity, String message, PositioningStrategy<? super P> positioningStrategy) {
        super(severity, positioningStrategy);
        this.message = message;
    }

    public static <T extends PsiElement> DiagnosticFactory<T> create(Severity severity, String message) {
        return create(severity, message, PositioningStrategies.DEFAULT);
    }

    public static <T extends PsiElement> DiagnosticFactory<T> create(Severity severity, String message, PositioningStrategy<? super T> positioningStrategy) {
        return new DiagnosticFactory<T>(severity, message, positioningStrategy);
    }

    @NotNull
    public Diagnostic<P> on(@NotNull P element) {
        return new DiagnosticWithPsiElement<P>(element, this, severity, message);
    }
}