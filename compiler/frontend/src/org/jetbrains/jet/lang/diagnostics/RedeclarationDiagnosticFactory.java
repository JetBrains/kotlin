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
* @author abreslav
*/
public class RedeclarationDiagnosticFactory extends AbstractDiagnosticFactory {
    
    private final String name;
    final Severity severity;
    private final String messagePrefix;

    public static final RedeclarationDiagnosticFactory REDECLARATION = new RedeclarationDiagnosticFactory(
            "REDECLARATION", Severity.ERROR, "Redeclaration: ");
    public static final RedeclarationDiagnosticFactory NAME_SHADOWING = new RedeclarationDiagnosticFactory(
            "NAME_SHADOWING", Severity.WARNING, "Name shadowed: ");

    public RedeclarationDiagnosticFactory(String name, Severity severity, String messagePrefix) {
        this.name = name;
        this.severity = severity;
        this.messagePrefix = messagePrefix;
    }

    public RedeclarationDiagnostic on(@NotNull PsiElement duplicatingElement, @NotNull String name) {
        return new RedeclarationDiagnostic.SimpleRedeclarationDiagnostic(duplicatingElement, name, this);
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }
    
    public String makeMessage(String identifier) {
        return messagePrefix + identifier;
    }

    @Override
    public String toString() {
        return getName();
    }
}
