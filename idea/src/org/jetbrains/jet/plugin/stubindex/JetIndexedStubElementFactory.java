package org.jetbrains.jet.plugin.stubindex;

import com.intellij.psi.stubs.IndexSink;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetClassElementType;
import org.jetbrains.jet.lang.psi.stubs.elements.JetFunctionElementType;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementFactory;

/**
 * @author Nikolay Krasko
 */
public class JetIndexedStubElementFactory implements JetStubElementFactory {
    @Override
    public JetClassElementType getClassElementType() {
        return new JetClassElementType("CLASS") {
            @Override
            public void indexStub(PsiJetClassStub stub, IndexSink sink) {
                String name = stub.getName();
                if (name != null) {
                    sink.occurrence(JetIndexKeys.SHORT_NAME_KEY, name);
                }
            }
        };
    }

    @Override
    public JetFunctionElementType getFunctionElementType() {
        return new JetFunctionElementType("FUN") {
            @Override
            public void indexStub(PsiJetFunctionStub stub, IndexSink sink) {
                String name = stub.getName();
                // TODO: Check that function defined in top level
                if (name != null) {
                    sink.occurrence(JetIndexKeys.TOP_LEVEL_FUNCTION_SHORT_NAME_KEY, name);
                }
            }
        };
    }
}
