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
import com.android.tools.idea.rendering.ResourceHelper;
import com.android.tools.idea.startup.ExternalAnnotationsSupport;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.PathManagerEx;
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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import org.jetbrains.android.dom.wrappers.LazyValueResourceElementWrapper;
import org.jetbrains.android.sdk.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;

/**
 *  Copied from AS 2.3 sources
 */
@SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors"})
public abstract class AndroidTestBase extends UsefulTestCase {

    protected JavaCodeInsightTestFixture myFixture;

    protected AndroidTestBase() {
        // IDEA14 seems to be stricter regarding validating accesses against known roots. By default, it contains the entire idea folder,
        // but it doesn't seem to include our custom structure tools/idea/../adt/idea where the android plugin is placed.
        // The following line explicitly adds that folder as an allowed root.
        VfsRootAccess.allowRootAccess(FileUtil.toCanonicalPath(getAndroidPluginHome()));
    }

    public String getAbsoluteTestDataPath() {
        // The following code doesn't work right now that the Android
        // plugin lives in a separate place:
        //String androidHomePath = System.getProperty("android.home.path");
        //if (androidHomePath == null) {
        //  androidHomePath = new File(PathManager.getHomePath(), "android/android").getPath();
        //}
        //return PathUtil.getCanonicalPath(androidHomePath + "/testData");

        String path = Paths.get(getTestDataPath()).toAbsolutePath().toString();
        assertTrue(new File(path).isAbsolute());
        return path;
    }

    public String getTestDataPath() {
        return getAndroidPluginHome() + "/testData";
    }

    public static String getAndroidPluginHome() {
        // Now that the Android plugin is kept in a separate place, we need to look in
        // a relative position instead
        String adtPath = PathManager.getHomePath() + "/../adt/idea/android";
        if (new File(adtPath).exists()) {
            return adtPath;
        }
        return PathManagerEx.findFileUnderCommunityHome("plugins/android").getPath();
    }

    protected static void addLatestAndroidSdk(Module module) {
        Sdk androidSdk = createLatestAndroidSdk();
        ModuleRootModificationUtil.setModuleSdk(module, androidSdk);
    }

    public static Sdk createLatestAndroidSdk() {
        String sdkPath = TestUtils.getSdk().toString();
        String platformDir = TestUtils.getLatestAndroidPlatform();

        Sdk sdk = ProjectJdkTable.getInstance().createSdk("android_test_sdk", AndroidSdkType.getInstance());
        SdkModificator sdkModificator = sdk.getSdkModificator();
        sdkModificator.setHomePath(sdkPath);

        VirtualFile androidJar = JarFileSystem.getInstance().findFileByPath(sdkPath + "/platforms/" + platformDir + "/android.jar!/");
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
        IAndroidTarget target = null;
        IAndroidTarget[] targets = sdkData.getTargets();
        for (IAndroidTarget t : targets) {
            if (t.getLocation().contains(platformDir)) {
                target = t;
                break;
            }
        }
        assertNotNull(target);
        data.setBuildTarget(target);
        sdkModificator.setSdkAdditionalData(data);
        ExternalAnnotationsSupport.attachJdkAnnotations(sdkModificator);
        sdkModificator.commitChanges();
        return sdk;
    }

    protected Project getProject() {
        return myFixture.getProject();
    }

    protected void ensureSdkManagerAvailable() {
        AndroidSdks androidSdks = AndroidSdks.getInstance();
        AndroidSdkData sdkData = androidSdks.tryToChooseAndroidSdk();
        if (sdkData == null) {
            sdkData = createTestSdkManager();
            if (sdkData != null) {
                androidSdks.setSdkData(sdkData);
            }
        }
        assertNotNull(sdkData);
    }

    @Nullable
    protected AndroidSdkData createTestSdkManager() {
        Sdk androidSdk = createLatestAndroidSdk();
        AndroidSdkAdditionalData data = AndroidSdks.getInstance().getAndroidSdkAdditionalData(androidSdk);
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
