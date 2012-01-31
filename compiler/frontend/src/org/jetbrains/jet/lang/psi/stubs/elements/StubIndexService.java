package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;

/**
 * @author Nikolay Krasko
 */
public interface StubIndexService {

    /**
     * Default implementation with no indexing.
     */
    StubIndexService NO_INDEX_SERVICE = new StubIndexService() {
        @Override
        public void indexClass(PsiJetClassStub stub, IndexSink sink) {
        }

        @Override
        public void indexFunction(PsiJetFunctionStub stub, IndexSink sink) {
        }
    };

    void indexClass(PsiJetClassStub stub, IndexSink sink);
    void indexFunction(PsiJetFunctionStub stub, IndexSink sink);
}
