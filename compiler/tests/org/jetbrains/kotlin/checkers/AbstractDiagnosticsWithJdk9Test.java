/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class AbstractDiagnosticsWithJdk9Test extends AbstractDiagnosticsTest {
    @NotNull
    @Override
    protected ConfigurationKind getConfigurationKind() {
        return ConfigurationKind.ALL;
    }

    @NotNull
    @Override
    protected TestJdkKind getTestJdkKind() {
        return TestJdkKind.FULL_JDK_9;
    }

    @Override
    protected void doMultiFileTest(
            @NotNull File file,
            @NotNull Map<String, ModuleAndDependencies> modules,
            @NotNull List<TestFile> testFiles
    ) {
        if (System.getenv("JDK_19") == null) {
            // Skip this test if no environment variable JDK_19 is set up
            return;
        }
        super.doMultiFileTest(file, modules, testFiles);
    }
}
