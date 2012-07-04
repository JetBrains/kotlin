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
        checkResult(RunUtils.execute(getCreateCommand(), "no"));
    }


    public void startEmulator() {
        System.out.println("Starting emulator...");
        checkResult(RunUtils.executeOnSeparateThread(getStartCommand(), false));
    }


    public void waitEmulatorStart() {
        System.out.println("Waiting for emulator start...");
        checkResult(RunUtils.execute(getWaitCommand()));
    }

    public void stopEmulator() {
        System.out.println("Stopping emulator...");
        checkResult(RunUtils.execute(getStopCommand()));
        System.out.println("Stopping adb...");
        checkResult(RunUtils.execute(getStopCommandForAdb()));
    }

    private static void checkResult(RunResult result) {
        if (!result.getStatus()) {
            throw new RuntimeException(result.getOutput());
        }
    }
}
