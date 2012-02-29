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
            if (stub.isTopLevel()) {
                // Collection only top level functions as only they are expected in completion without explicit import
                if (!stub.isExtension()) {
                    sink.occurrence(JetIndexKeys.TOP_LEVEL_FUNCTION_SHORT_NAME_KEY, name);
                    // sink.occurrence(JetIndexKeys.TOP_LEVEL_FUNCTION_FQNAME_KEY, name);
                } else {
                    sink.occurrence(JetIndexKeys.EXTENSION_FUNCTION_SHORT_NAME_KEY, name);
                    // sink.occurrence(JetIndexKeys.EXTENSION_FUNCTION_FQNAME_KEY, name);
                }
            }
        }
    }
}
