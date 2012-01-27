package org.jetbrains.jet.lang.psi.stubs.elements;

import org.jetbrains.jet.lang.parsing.JetParserDefinition;

/**
 * @author Nikolay Krasko
 */
public interface JetStubElementTypes {
    JetFileElementType FILE = new JetFileElementType();

    JetClassElementType CLASS = JetParserDefinition.getStubElementTypeFactory().getClassElementType();
    JetFunctionElementType FUNCTION = JetParserDefinition.getStubElementTypeFactory().getFunctionElementType();
}
