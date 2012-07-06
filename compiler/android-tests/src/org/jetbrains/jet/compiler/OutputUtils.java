/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.compiler;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.jet.compiler.run.RunUtils;
import org.jetbrains.jet.compiler.run.result.RunResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Natalia.Ukhorskaya
 */

public class OutputUtils {

    private final static Pattern EMULATOR_PATTERN = Pattern.compile("emulator-([0-9])*");

    public static boolean isResultOk(String output) {
        return !(output.contains("BUILD FAILED") || output.contains("Build failed"));
    }

    public static void checkResult(RunResult result) {
        if (!result.getStatus()) {
            throw new RuntimeException(result.getOutput());
        }
    }

    public static void stopRedundantEmulators(PathManager pathManager) {
        GeneralCommandLine commandLineForListOfDevices = new GeneralCommandLine();
        String adbCmdName = SystemInfo.isWindows ? "adb.exe" : "adb";
        commandLineForListOfDevices.setExePath(pathManager.getPlatformToolsFolderInAndroidSdk() + "/" + adbCmdName);
        commandLineForListOfDevices.addParameter("devices");
        RunResult runResult = RunUtils.execute(commandLineForListOfDevices);
        checkResult(runResult);
        
        Matcher matcher = EMULATOR_PATTERN.matcher(runResult.getOutput());
        while (matcher.find()) {
            System.out.println("Stopping redundant emulator...");
            GeneralCommandLine commandLineForStoppingEmulators = new GeneralCommandLine();
            if (SystemInfo.isWindows) {
                commandLineForStoppingEmulators.setExePath("taskkill");
                commandLineForStoppingEmulators.addParameter("/F");
                commandLineForStoppingEmulators.addParameter("/IM");
                commandLineForStoppingEmulators.addParameter("emulator-arm.exe");
                checkResult(RunUtils.execute(commandLineForStoppingEmulators));
                break;
            }
            else {
                commandLineForStoppingEmulators.setExePath(pathManager.getPlatformToolsFolderInAndroidSdk() + "/adb");
                commandLineForStoppingEmulators.addParameter("emu");
                commandLineForStoppingEmulators.addParameter("kill");
                commandLineForStoppingEmulators.addParameter("-s");
                commandLineForStoppingEmulators.addParameter(matcher.group());
                checkResult(RunUtils.execute(commandLineForStoppingEmulators));
            }
        }
    }

    private OutputUtils() {
    }
}
