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

package org.jetbrains.kotlin.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JdkAndMockLibraryProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.TargetPlatform;

public abstract class AbstractJvmWithLibBasicCompletionTest extends JetFixtureCompletionBaseTestCase {
    private static final String TEST_PATH = PluginTestCaseBase.getTestDataPathBase() + "/completion/basic/custom";

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        if (PluginTestCaseBase.isAllFilesPresentTest(getTestName(true))) {
            return super.getProjectDescriptor();
        }
        return new JdkAndMockLibraryProjectDescriptor(TEST_PATH + "/" + getTestName(false) + "Src", false);
    }

    @Override
    public TargetPlatform getPlatform() {
        return TargetPlatform.JVM;
    }

    @Override
    protected LookupElement[] complete(int invocationCount) {
        return myFixture.complete(CompletionType.BASIC, invocationCount);
    }
}
