/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.android;

import com.intellij.codeInsight.completion.CompletionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.completion.AbstractJvmBasicCompletionTest;
import org.jetbrains.jet.completion.AbstractJvmSmartCompletionTest;
import org.jetbrains.jet.completion.AbstractMultiFileJvmBasicCompletionTest;
import org.jetbrains.jet.completion.JetFixtureCompletionBaseTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.android.TestConst;
import org.jetbrains.jet.plugin.project.TargetPlatform;
import org.jetbrains.jet.test.TestMetadata;

@TestMetadata("idea/testData/android/completion/")
public class AndroidPropertiesCompletionBasicTest extends AbstractJvmBasicCompletionTest {
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/android/completion/" + getTestName(false) + "/";
    }

    @TestMetadata("PropertiesSimple.kt")
    public void testPropertiesSimple() throws Exception {
        getProject().putUserData(TestConst.TESTDATA_PATH, getTestDataPath());
        doTest("idea/testData/android/completion/PropertiesSimple/PropertiesSimple.kt");
    }
}
