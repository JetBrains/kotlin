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

import org.jetbrains.kotlin.jps.build.BaseKotlinJpsBuildTestCase;

import java.io.File;
import java.io.IOException;

public class AndroidJpsBuildTestCase extends BaseKotlinJpsBuildTestCase {
    private static final String PROJECT_NAME = "android-module";
    private static final String SDK_NAME = "Android_SDK";

    private final File workDir = new File(AndroidRunner.getPathManager().getTmpFolder());

    public void doTest() {
        initProject();
        rebuildAll();
        makeAll().assertSuccessful();
    }

    @Override
    protected String getProjectName() {
        return "android-module";
    }

    @Override
    protected void runTest() throws Throwable {
        doTest();
    }

    @Override
    public String getName() {
        return "AndroidJpsTest";
    }

    @Override
    protected File doGetProjectDir() throws IOException {
        return workDir;
    }

    private void initProject() {
        addJdk(SDK_NAME, AndroidRunner.getPathManager().getPlatformFolderInAndroidSdk() + "/android.jar");
        loadProject(workDir.getAbsolutePath() + File.separator + PROJECT_NAME + ".ipr");
    }
}
