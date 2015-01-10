/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import org.jetbrains.kotlin.psi.stubs.*;

public interface StubIndexService {

    /**
     * Default implementation with no indexing.
     */
    StubIndexService NO_INDEX_SERVICE = new StubIndexService() {
        @Override
        public void indexFile(KotlinFileStub stub, IndexSink sink) {
        }

        @Override
        public void indexClass(KotlinClassStub stub, IndexSink sink) {
        }

        @Override
        public void indexFunction(KotlinFunctionStub stub, IndexSink sink) {
        }

        @Override
        public void indexObject(KotlinObjectStub stub, IndexSink sink) {
        }

        @Override
        public void indexProperty(KotlinPropertyStub stub, IndexSink sink) {
        }

        @Override
        public void indexAnnotation(KotlinAnnotationEntryStub stub, IndexSink sink) {
        }
    };

    void indexFile(KotlinFileStub stub, IndexSink sink);
    void indexClass(KotlinClassStub stub, IndexSink sink);
    void indexFunction(KotlinFunctionStub stub, IndexSink sink);
    void indexObject(KotlinObjectStub stub, IndexSink sink);
    void indexProperty(KotlinPropertyStub stub, IndexSink sink);
    void indexAnnotation(KotlinAnnotationEntryStub stub, IndexSink sink);
}
