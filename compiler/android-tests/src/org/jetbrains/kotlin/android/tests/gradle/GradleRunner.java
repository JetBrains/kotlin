/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.tests.gradle;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.kotlin.android.tests.OutputUtils;
import org.jetbrains.kotlin.android.tests.PathManager;
import org.jetbrains.kotlin.android.tests.run.RunResult;
import org.jetbrains.kotlin.android.tests.run.RunUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.util.ArrayList;
import java.util.List;

public class GradleRunner {
    private final List<String> listOfCommands;

    public GradleRunner(PathManager pathManager) {
        listOfCommands = new ArrayList<String>();
        String cmdName = SystemInfo.isWindows ? "gradlew.bat" : "gradlew";
        listOfCommands.add(pathManager.getTmpFolder() + "/" + cmdName);
        listOfCommands.add("--build-file");
        listOfCommands.add(pathManager.getTmpFolder() + "/build.gradle");
    }


    public void clean() {
        System.out.println("Building gradle project...");
        RunResult result = RunUtils.execute(generateCommandLine("clean"));
        OutputUtils.checkResult(result);
    }

    public void build() {
        System.out.println("Building gradle project...");
        RunResult result = RunUtils.execute(generateCommandLine("build"));
        OutputUtils.checkResult(result);
    }


    public String connectedDebugAndroidTest() {
        System.out.println("Starting tests...");
        RunResult result = RunUtils.execute(generateCommandLine("connectedAndroidTest"));
        return result.getOutput();
    }

    private GeneralCommandLine generateCommandLine(String taskName) {
        GeneralCommandLine commandLine = new GeneralCommandLine(listOfCommands);
        commandLine.addParameter(taskName);
        return commandLine;
    }
}
