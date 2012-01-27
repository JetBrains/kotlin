package org.jetbrains.jet.lang.psi.stubs.elements;

/**
 * @author Nikolay Krasko
 */
public interface JetStubElementFactory {
    JetClassElementType getClassElementType();
    JetFunctionElementType getFunctionElementType();
}
