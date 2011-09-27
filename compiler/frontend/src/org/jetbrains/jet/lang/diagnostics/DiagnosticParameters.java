package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;

/**
 * @author svtk
 */
public interface DiagnosticParameters {
    DiagnosticParameter<JetKeywordToken> MODIFIER = new DiagnosticParameterImpl<JetKeywordToken>("MODIFIER");
    DiagnosticParameter<JetModifierListOwner> CLASS = new DiagnosticParameterImpl<JetModifierListOwner>("CLASS");
    DiagnosticParameter<JetType> TYPE = new DiagnosticParameterImpl<JetType>("TYPE");
}
