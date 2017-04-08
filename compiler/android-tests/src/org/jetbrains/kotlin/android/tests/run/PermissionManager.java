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
import org.jetbrains.kotlin.android.tests.download.SDKDownloader;

import java.io.File;

public class PermissionManager {
    private PermissionManager() {
    }

    public static void setPermissions(PathManager pathManager) {
        if (!SystemInfo.isWindows) {
            setExecPermissionForSimpleNamedFiles(new File(pathManager.getToolsFolderInAndroidSdk()));
            setExecPermissionForSimpleNamedFiles(new File(pathManager.getToolsFolderInAndroidSdk() + "/bin64"));
            setExecPermissionForSimpleNamedFiles(new File(pathManager.getBuildToolsFolderInAndroidSdk() + "/" + SDKDownloader.BUILD_TOOLS));
            setExecPermissionForSimpleNamedFiles(new File(pathManager.getPlatformToolsFolderInAndroidSdk()));
            RunUtils.execute(generateChmodCmd(pathManager.getGradleBinFolder() + "/gradle"));
        }
    }

    private static void setExecPermissionForSimpleNamedFiles(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().contains(".")) {
                    RunUtils.execute(generateChmodCmd(file.getAbsolutePath()));
                }
            }
        }
    }

    private static GeneralCommandLine generateChmodCmd(String path) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath("chmod");
        commandLine.addParameter("a+x");
        commandLine.addParameter(path);
        return commandLine;
    }
}
