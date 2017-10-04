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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;

public class PluginTestCaseBase {
    public static final String TEST_DATA_DIR = "idea/testData";
    public static final String TEST_DATA_PROJECT_RELATIVE = "/" + TEST_DATA_DIR;

    private PluginTestCaseBase() {
    }

    @NotNull
    public static String getTestDataPathBase() {
        return KotlinTestUtils.getHomeDirectory() + TEST_DATA_PROJECT_RELATIVE;
    }

    @NotNull
    @TestOnly
    private static Sdk createMockJdk(@NotNull String name, String path) {
        return getSdk(path, name);
    }

    @NotNull
    private static Sdk getSdk(String sdkHome, String name) {
        return JavaSdk.getInstance().createJdk(name, sdkHome, true);
    }

    @NotNull
    public static Sdk mockJdk() {
        return getSdk("compiler/testData/mockJDK/jre", "Mock JDK");
    }

    @NotNull
    public static Sdk mockJdk6() {
        return getSdk("compiler/testData/mockJDK/jre", "1.6");
    }

    @NotNull
    public static Sdk mockJdk8() {
        // Using JDK 6, but with version 1.8
        return getSdk("compiler/testData/mockJDK/jre", "1.8");
    }

    @TestOnly
    @NotNull
    public static Sdk mockJdk9() {
        return createMockJdk("9", "compiler/testData/mockJDK9/jre");
    }

    @NotNull
    public static Sdk fullJdk() {
        String javaHome = System.getProperty("java.home");
        assert new File(javaHome).isDirectory();
        return getSdk(javaHome, "Full JDK");
    }

    @NotNull
    public static Sdk jdk(@NotNull TestJdkKind kind) {
        switch (kind) {
            case MOCK_JDK:
                return mockJdk();
            case FULL_JDK_9:
                File jre9 = KotlinTestUtils.getJdk9HomeIfPossible();
                assert jre9 != null : "JDK_19 environment variable is not set";
                VfsRootAccess.allowRootAccess(jre9.getPath());
                return getSdk(jre9.getPath(), "Full JDK 9");
            case FULL_JDK:
                return fullJdk();
            default:
                throw new UnsupportedOperationException(kind.toString());
        }
    }

    public static boolean isAllFilesPresentTest(@NotNull String testName) {
        return StringUtil.startsWithIgnoreCase(testName, "allFilesPresentIn");
    }

    @TestOnly
    public static void clearSdkTable(@NotNull Disposable disposable) {
        Disposer.register(disposable, () -> ApplicationManager.getApplication().runWriteAction(() -> {
            ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
            for (Sdk sdk : jdkTable.getAllJdks()) {
                jdkTable.removeJdk(sdk);
            }
        }));
    }
}
