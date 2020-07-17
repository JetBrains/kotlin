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

package org.jetbrains.kotlin.android.tests;

import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;

public class PathManager {

    private final String tmpFolder;
    private final String rootFolder;

    public PathManager(String rootFolder, String tmpFolder) {
        this.tmpFolder = tmpFolder;
        this.rootFolder = rootFolder;
    }

    public String getPlatformFolderInAndroidSdk() {
        return getAndroidSdkRoot() + "/platforms";
    }

    public String getAndroidAvdRoot() {
        String androidEmulatorRoot = getAndroidSdkRoot() + "/compiler_box_test_avd";
        new File(androidEmulatorRoot).mkdirs();
        return androidEmulatorRoot;
    }

    public String getPlatformToolsFolderInAndroidSdk() {
        return getAndroidSdkRoot() + "/platform-tools";
    }

    public String getToolsFolderInAndroidSdk() {
        return getAndroidSdkRoot() + "/tools";
    }

    public String getEmulatorFolderInAndroidSdk() {
        return getAndroidSdkRoot() + "/emulator";
    }

    public String getOutputForCompiledFiles(String flavor) {
        return tmpFolder + "/libs/" + flavor;
    }

    public String getLibsFolderInAndroidTmpFolder() {
        return tmpFolder + "/libs";
    }

    public String getSrcFolderInAndroidTmpFolder() {
        return tmpFolder + "/src";
    }

    public String getAndroidTmpFolder() {
        return tmpFolder;
    }

    public String getAndroidSdkRoot() {
        return KotlinTestUtils.getAndroidSdkSystemIndependentPath();
    }

    public String getAndroidModuleRoot() {
        return rootFolder + "/compiler/android-tests/android-module";
    }

    public String getTmpFolder() {
        return tmpFolder;
    }
}
