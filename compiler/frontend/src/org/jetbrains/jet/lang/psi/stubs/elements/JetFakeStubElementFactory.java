package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;

/**
 * @author Nikolay Krasko
 */
public class JetFakeStubElementFactory implements JetStubElementFactory {
    @Override
    public JetClassElementType getClassElementType() {
        return new JetClassElementType("CLASS") {
            @Override
            public void indexStub(PsiJetClassStub stub, IndexSink sink) {
            }
        };
    }

    @Override
    public JetFunctionElementType getFunctionElementType() {
        return new JetFunctionElementType("FUN") {
            @Override
            public void indexStub(PsiJetFunctionStub stub, IndexSink sink) {
            }
        };
    }
}
