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

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import org.jetbrains.jet.lang.psi.stubs.*;

public interface StubIndexService {

    /**
     * Default implementation with no indexing.
     */
    StubIndexService NO_INDEX_SERVICE = new StubIndexService() {
        @Override
        public void indexFile(PsiJetFileStub stub, IndexSink sink) {
        }

        @Override
        public void indexClass(PsiJetClassStub stub, IndexSink sink) {
        }

        @Override
        public void indexFunction(PsiJetFunctionStub stub, IndexSink sink) {
        }

        @Override
        public void indexObject(PsiJetObjectStub stub, IndexSink sink) {
        }

        @Override
        public void indexProperty(PsiJetPropertyStub stub, IndexSink sink) {
        }

        @Override
        public void indexAnnotation(PsiJetAnnotationStub stub, IndexSink sink) {
        }
    };

    void indexFile(PsiJetFileStub stub, IndexSink sink);
    void indexClass(PsiJetClassStub stub, IndexSink sink);
    void indexFunction(PsiJetFunctionStub stub, IndexSink sink);
    void indexObject(PsiJetObjectStub stub, IndexSink sink);
    void indexProperty(PsiJetPropertyStub stub, IndexSink sink);
    void indexAnnotation(PsiJetAnnotationStub stub, IndexSink sink);
}
