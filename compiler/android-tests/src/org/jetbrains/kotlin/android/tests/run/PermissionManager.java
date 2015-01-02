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

package org.jetbrains.kotlin.android.tests.run;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.kotlin.android.tests.PathManager;

public class PermissionManager {
    private PermissionManager() {
    }

    public static void setPermissions(PathManager pathManager) {
        if (!SystemInfo.isWindows) {
            RunUtils.execute(generateChmodCmd(pathManager.getPlatformToolsFolderInAndroidSdk() + "/aapt"));
            RunUtils.execute(generateChmodCmd(pathManager.getPlatformToolsFolderInAndroidSdk() + "/adb"));
            RunUtils.execute(generateChmodCmd(pathManager.getPlatformToolsFolderInAndroidSdk() + "/dx"));
            RunUtils.execute(generateChmodCmd(pathManager.getToolsFolderInAndroidSdk() + "/emulator"));
            RunUtils.execute(generateChmodCmd(pathManager.getToolsFolderInAndroidSdk() + "/ddms"));
            RunUtils.execute(generateChmodCmd(pathManager.getToolsFolderInAndroidSdk() + "/android"));
            RunUtils.execute(generateChmodCmd(pathManager.getToolsFolderInAndroidSdk() + "/emulator-arm"));
            RunUtils.execute(generateChmodCmd(pathManager.getToolsFolderInAndroidSdk() + "/zipalign"));
            RunUtils.execute(generateChmodCmd(pathManager.getAntBinDirectory() + "/ant"));
        }
    }

    private static GeneralCommandLine generateChmodCmd(String path) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath("chmod");
        commandLine.addParameter("u+x");
        commandLine.addParameter(path);
        return commandLine;
    }
}
