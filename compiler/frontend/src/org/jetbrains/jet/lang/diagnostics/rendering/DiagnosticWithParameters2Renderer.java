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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters2;

import java.text.MessageFormat;

import static org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticRendererUtil.renderParameter;

public class DiagnosticWithParameters2Renderer<A, B> implements DiagnosticRenderer<DiagnosticWithParameters2<?, A, B>> {
    private final MessageFormat messageFormat;
    private final Renderer<? super A> rendererForA;
    private final Renderer<? super B> rendererForB;

    public DiagnosticWithParameters2Renderer(@NotNull String message, @Nullable Renderer<? super A> rendererForA, @Nullable Renderer<? super B> rendererForB) {
        this.messageFormat = new MessageFormat(message);
        this.rendererForA = rendererForA;
        this.rendererForB = rendererForB;
    }

    @NotNull
    @Override
    public String render(@NotNull DiagnosticWithParameters2<?, A, B> diagnostic) {
        return messageFormat.format(new Object[]{
                renderParameter(diagnostic.getA(), rendererForA),
                renderParameter(diagnostic.getB(), rendererForB)});
    }
}
