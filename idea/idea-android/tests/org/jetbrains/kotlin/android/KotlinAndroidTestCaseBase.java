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
package org.jetbrains.kotlin.android;

import com.android.sdklib.IAndroidTarget;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import org.jetbrains.android.sdk.AndroidSdkAdditionalData;
import org.jetbrains.android.sdk.AndroidSdkData;
import org.jetbrains.android.sdk.AndroidSdkType;
import org.jetbrains.kotlin.idea.test.RunnableWithException;
import org.jetbrains.kotlin.idea.test.TestUtilsKt;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;

@SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors"})
public abstract class KotlinAndroidTestCaseBase extends UsefulTestCase {
    /** Environment variable or system property containing the full path to an SDK install */
    public static final String SDK_PATH_PROPERTY = "ADT_TEST_SDK_PATH";

    /** Environment variable or system property pointing to the directory name of the platform inside $sdk/platforms, e.g. "android-17" */
    public static final String PLATFORM_DIR_PROPERTY = "ADT_TEST_PLATFORM";

    protected CodeInsightTestFixture myFixture;

    protected Sdk androidSdk;
    protected VirtualFile androidJar;

    private static final String TEST_DATA_PROJECT_RELATIVE = "/plugins/android-extensions/android-extensions-idea/testData/android";

    protected KotlinAndroidTestCaseBase() {
        IdeaTestCase.initPlatformPrefix();
    }

    public static String getPluginTestDataPathBase() {
        return KotlinTestUtils.getHomeDirectory() + TEST_DATA_PROJECT_RELATIVE;
    }

    public String getTestDataPath() {
        return getPluginTestDataPathBase();
    }

    public String getDefaultTestSdkPath() {
        return getTestDataPath() + "/sdk1.5";
    }

    @Override
    protected void tearDown() throws Exception {
        TestUtilsKt.unInvalidateBuiltinsAndStdLib(getProject(), new RunnableWithException() {
            @Override
            public void run() throws Exception {
                KotlinAndroidTestCaseBase.super.tearDown();
                androidJar = null;
                androidSdk = null;
            }
        });
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
        return true;
    }

    protected void addAndroidSdk(Module module, String sdkPath, String platformDir) {
        assert androidSdk != null : "android sdk must be initialized";
        ModuleRootModificationUtil.setModuleSdk(module, androidSdk);
    }

    public Sdk createAndroidSdk(String sdkPath, String platformDir) {
        Sdk sdk = ProjectJdkTable.getInstance().createSdk("android_test_sdk", AndroidSdkType.getInstance());
        SdkModificator sdkModificator = sdk.getSdkModificator();
        sdkModificator.setHomePath(sdkPath);

        if (platformDir.equals(getDefaultPlatformDir())) {
            // Compatibility: the unit tests were using android.jar outside the sdk1.5 install;
            // we need to use that one, rather than the real one in sdk1.5, in order for the
            // tests to pass. Longer term, we should switch the unit tests over to all using
            // a valid SDK.
            String androidJarPath = sdkPath + "/../android.jar!/";
            androidJar = VirtualFileManager.getInstance().findFileByUrl("jar://" + androidJarPath);
        } else {
            androidJar = VirtualFileManager.getInstance().findFileByUrl("jar://" + sdkPath + "/platforms/" + platformDir + "/android.jar!/");
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
        IAndroidTarget target = sdkData.findTargetByName("Android 5.0"); // TODO: Get rid of this hardcoded version number
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
}

