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

package org.jetbrains.kotlin.idea.test;

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.JetTestUtils;

import java.io.File;

public class PluginTestCaseBase {
    public static final String TEST_DATA_PROJECT_RELATIVE = "/idea/testData";

    private PluginTestCaseBase() {
    }

    @NotNull
    public static String getTestDataPathBase() {
        return JetTestUtils.getHomeDirectory() + TEST_DATA_PROJECT_RELATIVE;
    }

    @NotNull
    private static Sdk getSdk(String sdkHome) {
        return JavaSdk.getInstance().createJdk("JDK", sdkHome, true);
    }

    @NotNull
    public static Sdk mockJdk() {
        return getSdk("compiler/testData/mockJDK/jre");
    }

    @NotNull
    public static Sdk fullJdk() {
        String javaHome = System.getProperty("java.home");
        assert new File(javaHome).isDirectory();
        return getSdk(javaHome);
    }

    public static boolean isAllFilesPresentTest(@NotNull String testName) {
        return StringUtil.startsWithIgnoreCase(testName, "allFilesPresentIn");
    }
}
