/*
 * Copyright 2010-2020 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.application.ApplicationManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PluginStartupApplicationService {

    public static PluginStartupApplicationService getInstance() {
        return ApplicationManager.getApplication().getService(PluginStartupApplicationService.class);
    }

    private String aliveFlagPath;

    public synchronized String getAliveFlagPath() {
        if (this.aliveFlagPath == null) {
            try {
                Path flagFile = Files.createTempFile("kotlin-idea-", "-is-running");
                flagFile.toFile().deleteOnExit();
                this.aliveFlagPath = flagFile.toAbsolutePath().toString();
            }
            catch (IOException e) {
                this.aliveFlagPath = "";
            }
        }
        return this.aliveFlagPath;
    }

    public synchronized void resetAliveFlag() {
        if (this.aliveFlagPath != null) {
            File flagFile = new File(this.aliveFlagPath);
            if (flagFile.exists()) {
                if (flagFile.delete()) {
                    this.aliveFlagPath = null;
                }
            }
        }
    }
}
