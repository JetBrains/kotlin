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
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.idea.util.IjPlatformUtil;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.test.util.KtTestUtil;

import java.io.File;

public class PluginTestCaseBase {
    public static final String TEST_DATA_DIR = "idea/testData";
    public static final String TEST_DATA_PROJECT_RELATIVE = "/" + TEST_DATA_DIR;

    private PluginTestCaseBase() {
    }

    @NotNull
    public static String getTestDataPathBase() {
        return KtTestUtil.getHomeDirectory() + TEST_DATA_PROJECT_RELATIVE;
    }

    @NotNull
    @TestOnly
    private static Sdk createMockJdk(@NotNull String name, String path) {
        return IdeaTestUtil.createMockJdk(name, path, false);
    }

    @NotNull
    private static Sdk getSdk(String sdkHome, String name) {
        ProjectJdkTable table = IjPlatformUtil.getProjectJdkTableSafe();
        Sdk existing = table.findJdk(name);
        if (existing != null) {
            return existing;
        }
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
        return getSdk("compiler/testData/mockJDK9/jre", "9");
    }

    @NotNull
    public static Sdk fullJdk() {
        String javaHome = System.getProperty("java.home");
        assert new File(javaHome).isDirectory();
        return getSdk(javaHome, "Full JDK");
    }

    @NotNull
    public static Sdk addJdk(@NotNull Disposable disposable, @NotNull Function0<Sdk> getJdk) {
        Sdk jdk = getJdk.invoke();
        Sdk[] allJdks = IjPlatformUtil.getProjectJdkTableSafe().getAllJdks();
        for (Sdk existingJdk : allJdks) {
            if (existingJdk == jdk) {
                return existingJdk;
            }
        }
        ApplicationManager.getApplication().runWriteAction(() -> IjPlatformUtil.getProjectJdkTableSafe().addJdk(jdk, disposable));
        return jdk;
    }

    @NotNull
    public static Sdk jdk(@NotNull TestJdkKind kind) {
        switch (kind) {
            case MOCK_JDK:
                return mockJdk();
            case MODIFIED_MOCK_JDK:
                return jdkByVersion("MODIFIED MOCK", KtTestUtil.findMockJdkRtModified());
            case FULL_JDK_6:
                return jdkByVersion("6", KtTestUtil.getJdk6Home());
            case FULL_JDK_9:
                return jdkByVersion("9", KtTestUtil.getJdk9Home());
            case FULL_JDK:
                return fullJdk();
            case FULL_JDK_15:
                return jdkByVersion("15", KtTestUtil.getJdk15Home());
            default:
                throw new UnsupportedOperationException(kind.toString());
        }
    }

    @NotNull
    public static LanguageLevel getLanguageLevel(@NotNull TestJdkKind kind) {
        switch (kind) {
            case MOCK_JDK:
                return LanguageLevel.JDK_1_8;
            case MODIFIED_MOCK_JDK:
                return LanguageLevel.JDK_11;
            case FULL_JDK_6:
                return LanguageLevel.JDK_1_6;
            case FULL_JDK_9:
                return LanguageLevel.JDK_1_9;
            case FULL_JDK:
                return LanguageLevel.JDK_1_8;
            case FULL_JDK_15:
                return LanguageLevel.JDK_15_PREVIEW;
            default:
                throw new UnsupportedOperationException(kind.toString());
        }
    }

    private static Sdk jdkByVersion(String version, File jdkDir) {
        String path = jdkDir.getPath();
        VfsRootAccess.allowRootAccess(path);
        return getSdk(path, "Full JDK " + version);
    }

    public static boolean isAllFilesPresentTest(@NotNull String testName) {
        return StringUtil.startsWithIgnoreCase(testName, "allFilesPresentIn");
    }

    @TestOnly
    public static void clearSdkTable(@NotNull Disposable disposable) {
        Disposer.register(disposable, () -> ApplicationManager.getApplication().runWriteAction(() -> {
            ProjectJdkTable jdkTable = IjPlatformUtil.getProjectJdkTableSafe();
            for (Sdk sdk : jdkTable.getAllJdks()) {
                jdkTable.removeJdk(sdk);
            }
        }));
    }
}
