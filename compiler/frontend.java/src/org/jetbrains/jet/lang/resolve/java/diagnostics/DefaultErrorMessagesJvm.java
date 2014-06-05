/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.jetbrains.jet.renderer.Renderer;

public class DefaultErrorMessagesJvm implements DefaultErrorMessages.Extension {

    private static final Renderer<ConflictingJvmDeclarationsData> CONFLICTING_JVM_DECLARATIONS_DATA = new Renderer<ConflictingJvmDeclarationsData>() {
        @NotNull
        @Override
        public String render(@NotNull ConflictingJvmDeclarationsData element) {
            return element.getSignature().getName() + element.getSignature().getDesc();
        }
    };

    public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap();
    static {
        MAP.put(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash: ''{0}''", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ErrorsJvm.ACCIDENTAL_OVERRIDE, "Accidental override: ''{0}''", CONFLICTING_JVM_DECLARATIONS_DATA);
    }


    @NotNull
    @Override
    public DiagnosticFactoryToRendererMap getMap() {
        return MAP;
    }
}
