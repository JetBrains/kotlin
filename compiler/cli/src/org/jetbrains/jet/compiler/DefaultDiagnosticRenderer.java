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

package org.jetbrains.jet.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.AbstractDiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Renderer;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticRenderer;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticWithParameters1Renderer;
import org.jetbrains.jet.lang.diagnostics.rendering.SimpleDiagnosticRenderer;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

/**
 * @author Evgeny Gerashchenko
 * @since 4/12/12
 */
public class DefaultDiagnosticRenderer implements DiagnosticRenderer<Diagnostic> {
    public static final DefaultDiagnosticRenderer INSTANCE = new DefaultDiagnosticRenderer();

    private final Map<AbstractDiagnosticFactory, DiagnosticRenderer<?>> factoryToRenderer = new HashMap<AbstractDiagnosticFactory, DiagnosticRenderer<?>>();

    private DefaultDiagnosticRenderer() {
        factoryToRenderer.put(EXCEPTION_WHILE_ANALYZING, new DiagnosticWithParameters1Renderer<Throwable>("{0}", new Renderer<Throwable>() {
            @NotNull
            @Override
            public String render(@NotNull Throwable e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }));
    }

    @NotNull
    @Override
    public String render(@NotNull Diagnostic diagnostic) {
        DiagnosticRenderer renderer = factoryToRenderer.get(diagnostic.getFactory());
        if (renderer == null) {
            return diagnostic.getMessage(); // TODO throw IllegalArgumentException instead
        }
        //noinspection unchecked
        return renderer.render(diagnostic);
    }
}
