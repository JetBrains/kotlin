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

package org.jetbrains.jet.compiler.emulator;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.jet.compiler.OutputUtils;
import org.jetbrains.jet.compiler.PathManager;
import org.jetbrains.jet.compiler.run.RunUtils;
import org.jetbrains.jet.compiler.run.result.RunResult;

/**
 * @author Natalia.Ukhorskaya
 */

public class Emulator {

    private final PathManager pathManager;

    public Emulator(PathManager pathManager) {
        this.pathManager = pathManager;
    }

    private GeneralCommandLine getCreateCommand() {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        String androidCmdName = SystemInfo.isWindows ? "android.bat" : "android";
        commandLine.setExePath(pathManager.getToolsFolderInAndroidSdk() + "/" + androidCmdName);
        commandLine.addParameter("create");
        commandLine.addParameter("avd");
        commandLine.addParameter("--force");
        commandLine.addParameter("-n");
        commandLine.addParameter("my_avd");
        commandLine.addParameter("-p");
        commandLine.addParameter(pathManager.getAndroidEmulatorRoot());
        commandLine.addParameter("-t");
        commandLine.addParameter("1");
        return commandLine;
    }

    private GeneralCommandLine getStartCommand() {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        String emulatorCmdName = SystemInfo.isWindows ? "emulator.exe" : "emulator";
        commandLine.setExePath(pathManager.getToolsFolderInAndroidSdk() + "/" + emulatorCmdName);
        commandLine.addParameter("-avd");
        commandLine.addParameter("my_avd");
        commandLine.addParameter("-no-audio");
        commandLine.addParameter("-no-window");
        return commandLine;
    }

    private GeneralCommandLine getWaitCommand() {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        String adbCmdName = SystemInfo.isWindows ? "adb.exe" : "adb";
        commandLine.setExePath(pathManager.getPlatformToolsFolderInAndroidSdk() + "/" + adbCmdName);
        commandLine.addParameter("wait-for-device");
        return commandLine;
    }

    private GeneralCommandLine getStopCommandForAdb() {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        String adbCmdName = SystemInfo.isWindows ? "adb.exe" : "adb";
        commandLine.setExePath(pathManager.getPlatformToolsFolderInAndroidSdk() + "/" + adbCmdName);
        commandLine.addParameter("kill-server");
        return commandLine;
    }

    private GeneralCommandLine getStopCommand() {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        if (SystemInfo.isWindows) {
            commandLine.setExePath("taskkill");
            commandLine.addParameter("/F");
            commandLine.addParameter("/IM");
            commandLine.addParameter("emulator-arm.exe");
        }
        else {
            commandLine.setExePath(pathManager.getPlatformToolsFolderInAndroidSdk() + "/adb");
            commandLine.addParameter("emu");
            commandLine.addParameter("kill");
        }
        return commandLine;
    }

    public void createEmulator() {
        System.out.println("Creating emulator...");
        OutputUtils.checkResult(RunUtils.execute(getCreateCommand(), "no"));
    }


    public void startEmulator() {
        System.out.println("Starting emulator...");
        OutputUtils.stopRedundantEmulators(pathManager);
        OutputUtils.checkResult(RunUtils.executeOnSeparateThread(getStartCommand(), false));
    }


    public void waitEmulatorStart() {
        System.out.println("Waiting for emulator start...");
        OutputUtils.checkResult(RunUtils.execute(getWaitCommand()));
    }

    public void stopEmulator() {
        System.out.println("Stopping emulator...");
        OutputUtils.checkResult(RunUtils.execute(getStopCommand()));
        System.out.println("Stopping adb...");
        OutputUtils.checkResult(RunUtils.execute(getStopCommandForAdb()));
        if (SystemInfo.isUnix) {
            finishProcess("emulator-arm");
            finishProcess("adb");
            stopDdmsProcess();
        }
    }

    //Only for Unix
    private void stopDdmsProcess() {
        GeneralCommandLine listOfEmulatorProcess = new GeneralCommandLine();
        listOfEmulatorProcess.setExePath("sh");
        listOfEmulatorProcess.addParameter("-c");
        listOfEmulatorProcess.addParameter("ps aux | grep emulator");
        RunResult runResult = RunUtils.execute(listOfEmulatorProcess);
        OutputUtils.checkResult(runResult);
        String pidFromPsCommand = OutputUtils.getPidFromPsCommand(runResult.getOutput());
        if (pidFromPsCommand != null) {
            GeneralCommandLine killCommand = new GeneralCommandLine();
            killCommand.setExePath("kill");
            killCommand.addParameter(pidFromPsCommand);
            OutputUtils.checkResult(RunUtils.execute(killCommand));
        }
    }

    //Only for Unix
    private void finishProcess(String processName) {
        GeneralCommandLine pidOfProcess = new GeneralCommandLine();
        pidOfProcess.setExePath("pidof");
        pidOfProcess.addParameter(processName);
        RunResult runResult = RunUtils.execute(pidOfProcess);
        String pid = runResult.getOutput().substring(("pidof " + processName).length());
        if (pid.length() > 1) {
            GeneralCommandLine killCommand = new GeneralCommandLine();
            killCommand.setExePath("kill");
            killCommand.addParameter(pid);
            OutputUtils.checkResult(RunUtils.execute(killCommand));
        }
    }
}
