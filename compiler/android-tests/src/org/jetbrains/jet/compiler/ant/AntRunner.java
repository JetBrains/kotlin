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

package org.jetbrains.jet.compiler.ant;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.jet.compiler.OutputUtils;
import org.jetbrains.jet.compiler.PathManager;
import org.jetbrains.jet.compiler.run.RunUtils;
import org.jetbrains.jet.compiler.run.result.RunResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Natalia.Ukhorskaya
 */

public class AntRunner {
    private final List<String> listOfAntCommands;

    public AntRunner(PathManager pathManager) {
        listOfAntCommands = new ArrayList<String>();
        String antCmdName = SystemInfo.isWindows ? "ant.bat" : "ant";
        listOfAntCommands.add(pathManager.getAntBinDirectory() + "/" + antCmdName);
        listOfAntCommands.add("-Dsdk.dir=" + pathManager.getAndroidSdkRoot());
        listOfAntCommands.add("-buildfile");
        listOfAntCommands.add(pathManager.getTmpFolder() + "/build.xml");
    }

    /* Pack compiled sources on first step to one jar file */
    public void packLibraries() {
        System.out.println("Pack libraries...");
        RunResult result = RunUtils.execute(generateCommandLine("pack_libraries"));
        OutputUtils.checkResult(result);
    }

    /* Clean output directory */
    public void cleanOutput() {
        System.out.println("Clearing output directory...");
        RunResult result = RunUtils.execute(generateCommandLine("clean"));
        OutputUtils.checkResult(result);
    }

    public void compileSources() {
        System.out.println("Compiling sources...");
        RunResult result = RunUtils.execute(generateCommandLine("debug"));
        OutputUtils.checkResult(result);
    }

    public void installApplicationOnEmulator() {
        System.out.println("Installing apk...");
        RunResult result = RunUtils.execute(generateCommandLine("installt"));
        String resultOutput = result.getOutput();
        if (!isInstallSuccessful(resultOutput)) {
            installApplicationOnEmulator();
            return;
        }
        else {
            System.out.println(resultOutput);
        }
        OutputUtils.checkResult(result);
    }

    public String runTestsOnEmulator() {
        System.out.println("Running tests...");
        RunResult result = RunUtils.execute(generateCommandLine("test"));
        String resultOutput = result.getOutput();
        OutputUtils.checkResult(result);
        return resultOutput;
    }

    private static boolean isInstallSuccessful(String output) {
        if (output.contains("Is the system running?")) {
            try {
                System.out.println("Device not ready. Waiting for 20 sec.");
                Thread.sleep(20000);
                return false;
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    private GeneralCommandLine generateCommandLine(String taskName) {
        GeneralCommandLine commandLine = new GeneralCommandLine(listOfAntCommands);
        commandLine.addParameter(taskName);
        return commandLine;
    }
}
