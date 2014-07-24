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

package org.jetbrains.jet.completion.handlers;

import org.jetbrains.jet.plugin.KotlinCompletionTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

public class JavaCompletionHandlerTest extends KotlinCompletionTestCase {
    public void testClassAutoImport() {
        doTest();
    }

    public void doTest() {
        String fileName = getTestName(false);
        try {
            configureByFiles(null, fileName + ".java", fileName + ".kt");
            complete(2);
            checkResultByFile(fileName + ".after.java");
        } catch (@SuppressWarnings("CaughtExceptionImmediatelyRethrown") AssertionError assertionError) {
            throw assertionError;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/injava").getPath() + File.separator;
    }
}
