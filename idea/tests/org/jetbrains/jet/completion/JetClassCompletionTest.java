/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.completion;

import com.intellij.codeInsight.completion.CompletionType;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

public class JetClassCompletionTest extends JetCompletionTestBase {

    public void testInExpressionNoPrefix() {

        doTest();
    }

    public void testInExtendTypeAnnotation() {
        doTest();
    }

    @Override
    protected CompletionType getCompletionType(String testName, String fileText) {
        return CompletionType.BASIC;
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/class").getPath() +
               File.separator;
    }
}
