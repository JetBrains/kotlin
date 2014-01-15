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

package org.jetbrains.jet.plugin;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class JetLightCodeInsightFixtureTestCase extends LightCodeInsightFixtureTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return getProjectDescriptorFromFileDirective();
    }

    protected LightProjectDescriptor getProjectDescriptorFromFileDirective() {
        if (!getTestName(false).startsWith("AllFilesPresentIn")) {
            try {
                String fileText = FileUtil.loadFile(new File(getTestDataPath(), fileName()));

                List<String> withLibraryDirective = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "WITH_LIBRARY:");
                if (!withLibraryDirective.isEmpty()) {
                    return new JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/" + withLibraryDirective.get(0), true);
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME")) {
                    return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "JS")) {
                    return JetStdJSProjectDescriptor.INSTANCE;
                }
            }
            catch (IOException e) {
                throw ExceptionUtils.rethrow(e);
            }
        }

        return JetLightProjectDescriptor.INSTANCE;
    }

    protected String fileName() {
        return getTestName(false) + ".kt";
    }
}
