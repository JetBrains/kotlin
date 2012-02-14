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

package org.jetbrains.jet.formatter;

import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class JetTypingIndentationTest extends LightCodeInsightTestCase {

    public void testAfterImport() {
        doFileNewlineTest();
    }

    public void testConsecutiveCallsAfterDot() {
        doFileNewlineTest();
    }

    public void testDoInFun() {
        doFileNewlineTest();
    }

    // TODO
    public void enabletestEmptyParameters() {
        doFileNewlineTest();
    }

    public void testFor() {
        doFileNewlineTest();
    }

    // TODO
    public void enabletestFunctionBlock() {
        doFileNewlineTest();
    }

    public void testIf() {
        doFileNewlineTest();
    }

    // TODO
    public void enabletestNotFirstParameter() {
        doFileNewlineTest();
    }

    public void testWhile() {
        doFileNewlineTest();
    }

    public void doFileNewlineTest() {
        configureByFile(getTestName(false) + ".kt");
        type('\n');
        checkResultByFile(getTestName(false) + ".after.kt");
    }

    @Override
    protected String getTestDataPath() {

        final String testRelativeDir = "formatter/IndentationOnNewline";
        return new File(PluginTestCaseBase.getTestDataPathBase(), testRelativeDir).getPath() +
               File.separator;
    }
}
