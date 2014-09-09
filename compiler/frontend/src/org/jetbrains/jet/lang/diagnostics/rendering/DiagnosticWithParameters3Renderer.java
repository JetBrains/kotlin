/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters3;
import org.jetbrains.jet.renderer.Renderer;

import static org.jetbrains.jet.lang.diagnostics.rendering.RenderingPackage.renderParameter;

public class DiagnosticWithParameters3Renderer<A, B, C> extends AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters3<?, A, B, C>> {
    private final Renderer<? super A> rendererForA;
    private final Renderer<? super B> rendererForB;
    private final Renderer<? super C> rendererForC;

    public DiagnosticWithParameters3Renderer(@NotNull String message,
            @Nullable Renderer<? super A> rendererForA,
            @Nullable Renderer<? super B> rendererForB,
            @Nullable Renderer<? super C> rendererForC) {
        super(message);
        this.rendererForA = rendererForA;
        this.rendererForB = rendererForB;
        this.rendererForC = rendererForC;
    }

    @NotNull
    @Override
    public Object[] renderParameters(@NotNull DiagnosticWithParameters3<?, A, B, C> diagnostic) {
        return new Object[]{
                renderParameter(diagnostic.getA(), rendererForA),
                renderParameter(diagnostic.getB(), rendererForB),
                renderParameter(diagnostic.getC(), rendererForC)};
    }
}
