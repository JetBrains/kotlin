package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;

/**
 * @author svtk
 */
public interface DiagnosticParameters {
    DiagnosticParameter<JetKeywordToken> MODIFIER = new DiagnosticParameterImpl<JetKeywordToken>("MODIFIER");
    DiagnosticParameter<JetClass> CLASS = new DiagnosticParameterImpl<JetClass>("CLASS");
    DiagnosticParameter<JetType> TYPE = new DiagnosticParameterImpl<JetType>("TYPE");
    DiagnosticParameter<JetProperty> PROPERTY = new DiagnosticParameterImpl<JetProperty>("TYPE");
}
