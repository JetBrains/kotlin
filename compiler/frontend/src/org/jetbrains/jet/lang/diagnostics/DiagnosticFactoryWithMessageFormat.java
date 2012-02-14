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

import java.text.MessageFormat;

/**
* @author abreslav
*/
public abstract class DiagnosticFactoryWithMessageFormat extends DiagnosticFactoryWithSeverity {
    protected final MessageFormat messageFormat;
    protected final Renderer renderer;

    public DiagnosticFactoryWithMessageFormat(Severity severity, String message) {
        this(severity, message, Renderer.TO_STRING);
    }

    public DiagnosticFactoryWithMessageFormat(Severity severity, String message, Renderer renderer) {
        super(severity);
        this.messageFormat = new MessageFormat(message);
        this.renderer = renderer;
    }

}
