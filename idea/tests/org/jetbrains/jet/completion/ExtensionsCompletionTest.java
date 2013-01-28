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

import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

public class ExtensionsCompletionTest extends JetCompletionTestBase {

    public void testExtensionInExtendedClass() {
        doTest();
    }

    public void testExtensionInExtendedClassThis() {
        doTest();
    }

    public void testExtensionInExtension() {
        doTest();
    }

    public void testExtensionInExtensionThis() {
        doTest();
    }

    public void testInvalidTypeParameters() {
        doTest();
    }

    public void testIrrelevantExtension() {
        doTest();
    }

    public void testJavaTypeExtension() {
        doTest();
    }

    public void testKotlinGenericTypeExtension() {
        doTest();
    }

    public void testKotlinTypeExtension() {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/basic/extensions").getPath() +
               File.separator;
    }
}
