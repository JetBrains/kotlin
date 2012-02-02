package org.jetbrains.jet.plugin.stubindex;

import com.intellij.psi.stubs.IndexSink;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;
import org.jetbrains.jet.lang.psi.stubs.elements.StubIndexService;

/**
 * @author Nikolay Krasko
 */
public class StubIndexServiceImpl implements StubIndexService {

    @Override
    public void indexClass(PsiJetClassStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(JetIndexKeys.SHORT_NAME_KEY, name);
        }
        
        String fqn = stub.getQualifiedName();
        if (fqn != null) {
            sink.occurrence(JetIndexKeys.FQN_KEY, fqn);
        }
    }

    @Override
    public void indexFunction(PsiJetFunctionStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            if (!stub.isExtension()) {
                if (stub.isTopLevel()) {
                    sink.occurrence(JetIndexKeys.TOP_LEVEL_FUNCTION_SHORT_NAME_KEY, name);
                }
            }
            else {
                sink.occurrence(JetIndexKeys.EXTENSION_FUNCTION_SHORT_NAME_KEY, name);
            }
        }
    }
}
