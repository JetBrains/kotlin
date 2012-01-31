package org.jetbrains.jet.lang.psi.stubs.elements;

/**
 * @author Nikolay Krasko
 */
public interface JetStubElementTypes {
    JetFileElementType FILE = new JetFileElementType();

    JetClassElementType CLASS = new JetClassElementType("CLASS");
    JetFunctionElementType FUNCTION = new JetFunctionElementType("FUN");
}
