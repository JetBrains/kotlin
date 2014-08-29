/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.jet.android;

import com.android.sdklib.IAndroidTarget;
import com.android.tools.idea.rendering.ResourceHelper;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import org.jetbrains.android.dom.wrappers.LazyValueResourceElementWrapper;
import org.jetbrains.android.sdk.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

@SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors"})
public abstract class KotlinAndroidTestCaseBase extends UsefulTestCase {
    /** Environment variable or system property containing the full path to an SDK install */
    public static final String SDK_PATH_PROPERTY = "ADT_TEST_SDK_PATH";

    /** Environment variable or system property pointing to the directory name of the platform inside $sdk/platforms, e.g. "android-17" */
    public static final String PLATFORM_DIR_PROPERTY = "ADT_TEST_PLATFORM";

    protected JavaCodeInsightTestFixture myFixture;

    protected KotlinAndroidTestCaseBase() {
        IdeaTestCase.initPlatformPrefix();
    }

    public String getAbsoluteTestDataPath() {
        // The following code doesn't work right now that the Android
        // plugin lives in a separate place:
        //String androidHomePath = System.getProperty("android.home.path");
        //if (androidHomePath == null) {
        //  androidHomePath = new File(PathManager.getHomePath(), "android/android").getPath();
        //}
        //return PathUtil.getCanonicalPath(androidHomePath + "/testData");

        // getTestDataPath already gives the absolute path anyway:
        String path = getTestDataPath();
        assertTrue(new File(path).isAbsolute());
        return path;
    }

    public String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    public static String getAndroidPluginHome() {
        // Now that the Android plugin is kept in a separate place, we need to look in
        // a relative position instead
        String adtPath = PathManager.getHomePath() + "/../adt/idea/android";
        if (new File(adtPath).exists()) {
            return adtPath;
        }
        return new File(PathManager.getHomePath(), "android/android").getPath();
    }

    public String getDefaultTestSdkPath() {
        return getTestDataPath() + "/sdk1.5";
    }

    public String getDefaultPlatformDir() {
        return "android-1.5";
    }

    protected String getTestSdkPath() {
        if (requireRecentSdk()) {
            String override = System.getProperty(SDK_PATH_PROPERTY);
            if (override != null) {
                assertTrue("Must also define " + PLATFORM_DIR_PROPERTY, System.getProperty(PLATFORM_DIR_PROPERTY) != null);
                assertTrue(override, new File(override).exists());
                return override;
            }
            override = System.getenv(SDK_PATH_PROPERTY);
            if (override != null) {
                assertTrue("Must also define " + PLATFORM_DIR_PROPERTY, System.getenv(PLATFORM_DIR_PROPERTY) != null);
                return override;
            }
            fail("This unit test requires " + SDK_PATH_PROPERTY + " and " + PLATFORM_DIR_PROPERTY + " to be defined.");
        }

        return getDefaultTestSdkPath();
    }

    protected String getPlatformDir() {
        if (requireRecentSdk()) {
            String override = System.getProperty(PLATFORM_DIR_PROPERTY);
            if (override != null) {
                return override;
            }
            override = System.getenv(PLATFORM_DIR_PROPERTY);
            if (override != null) {
                return override;
            }
            fail("This unit test requires " + SDK_PATH_PROPERTY + " and " + PLATFORM_DIR_PROPERTY + " to be defined.");
        }
        return getDefaultPlatformDir();
    }

    /** Is the bundled (incomplete) SDK install adequate or do we need to find a valid install? */
    protected boolean requireRecentSdk() {
        return false;
    }

    protected void addAndroidSdk(Module module, String sdkPath, String platformDir) {
        Sdk androidSdk = createAndroidSdk(sdkPath, platformDir);
        ModuleRootModificationUtil.setModuleSdk(module, androidSdk);
    }

    public Sdk createAndroidSdk(String sdkPath, String platformDir) {
        Sdk sdk = ProjectJdkTable.getInstance().createSdk("android_test_sdk", AndroidSdkType.getInstance());
        SdkModificator sdkModificator = sdk.getSdkModificator();
        sdkModificator.setHomePath(sdkPath);

        VirtualFile androidJar;
        if (platformDir.equals(getDefaultPlatformDir())) {
            // Compatibility: the unit tests were using android.jar outside the sdk1.5 install;
            // we need to use that one, rather than the real one in sdk1.5, in order for the
            // tests to pass. Longer term, we should switch the unit tests over to all using
            // a valid SDK.
            String androidJarPath = sdkPath + "/../android.jar!/";
            androidJar = JarFileSystem.getInstance().findFileByPath(androidJarPath);
        } else {
            androidJar = LocalFileSystem.getInstance().findFileByPath(sdkPath + "/platforms/" + platformDir + "/android.jar");
        }
        sdkModificator.addRoot(androidJar, OrderRootType.CLASSES);

        VirtualFile resFolder = LocalFileSystem.getInstance().findFileByPath(sdkPath + "/platforms/" + platformDir + "/data/res");
        sdkModificator.addRoot(resFolder, OrderRootType.CLASSES);

        VirtualFile docsFolder = LocalFileSystem.getInstance().findFileByPath(sdkPath + "/docs/reference");
        if (docsFolder != null) {
            sdkModificator.addRoot(docsFolder, JavadocOrderRootType.getInstance());
        }

        AndroidSdkAdditionalData data = new AndroidSdkAdditionalData(sdk);
        AndroidSdkData sdkData = AndroidSdkData.getSdkData(sdkPath);
        assertNotNull(sdkData);
        IAndroidTarget target = sdkData.findTargetByName("Android 4.2"); // TODO: Get rid of this hardcoded version number
        if (target == null) {
            IAndroidTarget[] targets = sdkData.getTargets();
            for (IAndroidTarget t : targets) {
                if (t.getLocation().contains(platformDir)) {
                    target = t;
                    break;
                }
            }
            if (target == null && targets.length > 0) {
                target = targets[targets.length - 1];
            }
        }
        assertNotNull(target);
        data.setBuildTarget(target);
        sdkModificator.setSdkAdditionalData(data);
        sdkModificator.commitChanges();
        return sdk;
    }

    protected Project getProject() {
        return myFixture.getProject();
    }

    protected void ensureSdkManagerAvailable() {
        AndroidSdkData sdkData = AndroidSdkUtils.tryToChooseAndroidSdk();
        if (sdkData == null) {
            sdkData = createTestSdkManager();
            if (sdkData != null) {
                AndroidSdkUtils.setSdkData(sdkData);
            }
        }
        assertNotNull(sdkData);
    }

    @Nullable
    protected AndroidSdkData createTestSdkManager() {
        Sdk androidSdk = createAndroidSdk(getTestSdkPath(), getPlatformDir());
        AndroidSdkAdditionalData data = (AndroidSdkAdditionalData)androidSdk.getSdkAdditionalData();
        if (data != null) {
            AndroidPlatform androidPlatform = data.getAndroidPlatform();
            if (androidPlatform != null) {
                // Put default platforms in the list before non-default ones so they'll be looked at first.
                return androidPlatform.getSdkData();
            } else {
                fail("No getAndroidPlatform() associated with the AndroidSdkAdditionalData: " + data);
            }
        } else {
            fail("Could not find data associated with the SDK: " + androidSdk.getName());
        }
        return null;
    }

    /** Returns a description of the given elements, suitable as unit test golden file output */
    public static String describeElements(@Nullable PsiElement[] elements) {
        if (elements == null) {
            return "Empty";
        }
        StringBuilder sb = new StringBuilder();
        for (PsiElement target : elements) {
            appendElementDescription(sb, target);
        }
        return sb.toString();
    }

    /** Appends a description of the given element, suitable as unit test golden file output */
    public static void appendElementDescription(@NotNull StringBuilder sb, @NotNull PsiElement element) {
        if (element instanceof LazyValueResourceElementWrapper) {
            LazyValueResourceElementWrapper wrapper = (LazyValueResourceElementWrapper)element;
            XmlAttributeValue value = wrapper.computeElement();
            if (value != null) {
                element = value;
            }
        }
        PsiFile file = element.getContainingFile();
        int offset = element.getTextOffset();
        TextRange segment = element.getTextRange();
        appendSourceDescription(sb, file, offset, segment);
    }

    /** Appends a description of the given elements, suitable as unit test golden file output */
    public static void appendSourceDescription(@NotNull StringBuilder sb, @Nullable PsiFile file, int offset, @Nullable Segment segment) {
        if (file != null && segment != null) {
            if (ResourceHelper.getFolderType(file) != null) {
                assertNotNull(file.getParent());
                sb.append(file.getParent().getName());
                sb.append("/");
            }
            sb.append(file.getName());
            sb.append(':');
            String text = file.getText();
            int lineNumber = 1;
            for (int i = 0; i < offset; i++) {
                if (text.charAt(i) == '\n') {
                    lineNumber++;
                }
            }
            sb.append(lineNumber);
            sb.append(":");
            sb.append('\n');
            int startOffset = segment.getStartOffset();
            int endOffset = segment.getEndOffset();
            assertTrue(offset == -1 || offset >= startOffset);
            assertTrue(offset == -1 || offset <= endOffset);

            int lineStart = startOffset;
            while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
                lineStart--;
            }

            // Skip over leading whitespace
            while (lineStart < startOffset && Character.isWhitespace(text.charAt(lineStart))) {
                lineStart++;
            }

            int lineEnd = startOffset;
            while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') {
                lineEnd++;
            }
            String indent = "  ";
            sb.append(indent);
            sb.append(text.substring(lineStart, lineEnd));
            sb.append('\n');
            sb.append(indent);
            for (int i = lineStart; i < lineEnd; i++) {
                if (i == offset) {
                    sb.append('|');
                } else if (i >= startOffset && i <= endOffset) {
                    sb.append('~');
                } else {
                    sb.append(' ');
                }
            }
        } else {
            sb.append(offset);
            sb.append(":?");
        }
        sb.append('\n');
    }
}

