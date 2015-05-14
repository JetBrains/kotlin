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
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
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

    private static Sdk getSdk(String sdkHome) {
        Sdk sdk = JavaSdk.getInstance().createJdk("JDK", sdkHome, true);
        SdkModificator modificator = sdk.getSdkModificator();
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(JetTestUtils.getJdkAnnotationsJar());
        assert file != null;
        modificator.addRoot(JarFileSystem.getInstance().getJarRootForLocalFile(file), AnnotationOrderRootType.getInstance());
        modificator.commitChanges();
        return sdk;
    }

    public static Sdk mockJdk() {
        return getSdk("compiler/testData/mockJDK/jre");
    }

    public static Sdk fullJdk() {
        String javaHome = System.getProperty("java.home");
        assert new File(javaHome).isDirectory();
        return getSdk(javaHome);
    }

    public static boolean isAllFilesPresentTest(@NotNull String testName) {
        return StringUtil.startsWithIgnoreCase(testName, "allFilesPresentIn");
    }
}
