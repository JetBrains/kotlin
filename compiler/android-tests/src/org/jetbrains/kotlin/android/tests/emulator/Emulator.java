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

package org.jetbrains.kotlin.android.tests.emulator;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.android.tests.OutputUtils;
import org.jetbrains.kotlin.android.tests.PathManager;
import org.jetbrains.kotlin.android.tests.run.RunResult;
import org.jetbrains.kotlin.android.tests.run.RunUtils;
import org.junit.Assert;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Emulator {

    public static final String ARM = "arm";
    public static final String X86 = "x86";
    private static final String AVD_NAME = "kotlin_box_test_avd";

    private final static Pattern EMULATOR_PATTERN = Pattern.compile("emulator-([0-9])*");

    private final PathManager pathManager;
    private final String platform;

    public Emulator(PathManager pathManager, String platform) {
        this.pathManager = pathManager;
        this.platform = platform;
    }

    private GeneralCommandLine getCreateCommand() {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        String androidCmdName = SystemInfo.isWindows ? "avdmanager.bat" : "avdmanager";
        commandLine.setExePath(pathManager.getToolsFolderInAndroidSdk() + "/bin/" + androidCmdName);
        commandLine.addParameter("create");
        commandLine.addParameter("avd");
        commandLine.addParameter("--force");
        commandLine.addParameter("-n");
        commandLine.addParameter(AVD_NAME);
        commandLine.addParameter("-p");
        commandLine.addParameter(pathManager.getAndroidAvdRoot());
        commandLine.addParameter("-k");
        if (platform == X86) {
            commandLine.addParameter("system-images;android-19;default;x86");
        } else {
            commandLine.addParameter("system-images;android-19;default;armeabi-v7a");
        }
        return commandLine;
    }

    private GeneralCommandLine getStartCommand() {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(pathManager.getEmulatorFolderInAndroidSdk() + "/" + "emulator");
        commandLine.addParameter("-avd");
        commandLine.addParameter(AVD_NAME);
        commandLine.addParameter("-no-audio");
        commandLine.addParameter("-no-window");
        return commandLine;
    }

    private GeneralCommandLine getWaitCommand() {
        GeneralCommandLine commandLine = createAdbCommand();
        commandLine.addParameter("wait-for-device");
        return commandLine;
    }

    private GeneralCommandLine getStopCommandForAdb() {
        GeneralCommandLine commandLine = createAdbCommand();
        commandLine.addParameter("kill-server");
        return commandLine;
    }

    @Nullable
    private GeneralCommandLine getStopCommand() {
        if (SystemInfo.isWindows) {
            GeneralCommandLine commandLine = new GeneralCommandLine();
            commandLine.setExePath("taskkill");
            commandLine.addParameter("/F");
            commandLine.addParameter("/IM");
            commandLine.addParameter("emulator-" + platform + ".exe");
            return commandLine;
        }
        return null;
    }

    public void createEmulator() {
        System.out.println("Creating emulator...");
        OutputUtils.checkResult(RunUtils.execute(new RunUtils.RunSettings(getCreateCommand(), "no", true, null, false)));
    }

    private GeneralCommandLine createAdbCommand() {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(pathManager.getPlatformToolsFolderInAndroidSdk() + "/" + "adb");
        return commandLine;
    }

    public void startServer() {
        GeneralCommandLine commandLine = createAdbCommand();
        commandLine.addParameter("start-server");
        System.out.println("Start adb server...");
        OutputUtils.checkResult(RunUtils.execute(new RunUtils.RunSettings(commandLine, null, true, "ADB START:", true)));
    }

    public void startEmulator() {
        startServer();
        System.out.println("Starting emulator...");
        RunUtils.executeOnSeparateThread(new RunUtils.RunSettings(getStartCommand(), null, false, "START: ", true));
        printLog();
    }

    public void printLog() {
        GeneralCommandLine commandLine = createAdbCommand();
        commandLine.addParameter("logcat");
        commandLine.addParameter("-v");
        commandLine.addParameter("time");
        commandLine.addParameter("-s");
        commandLine.addParameter("dalvikvm:W");
        commandLine.addParameter("TestRunner:I");
        RunUtils.executeOnSeparateThread(new RunUtils.RunSettings(commandLine, null, false, "LOGCAT: ", true));
    }

    public void waitEmulatorStart() {
        System.out.println("Waiting for emulator start...");
        OutputUtils.checkResult(RunUtils.execute(getWaitCommand()));
        GeneralCommandLine bootCheckCommand = createAdbCommand();
        bootCheckCommand.addParameter("shell");
        bootCheckCommand.addParameter("getprop");
        bootCheckCommand.addParameter("sys.boot_completed");
        int counter = 0;
        RunResult execute = RunUtils.execute(bootCheckCommand);
        while (counter < 20) {
            String output = execute.getOutput();
            if (output.trim().endsWith("1")) {
                System.out.println("Emulator fully booted!");
                return;
            }
            System.out.println("Waiting for emulator boot (" + counter + ")...");
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            counter++;
            execute = RunUtils.execute(bootCheckCommand);
        }
        Assert.fail("Can't find booted emulator: " + execute.getOutput());
    }

    public void stopEmulator() {
        System.out.println("Stopping emulator...");

        GeneralCommandLine command = createAdbCommand();
        command.addParameter("-s");
        command.addParameter("emulator-5554");
        command.addParameter("emu");
        command.addParameter("kill");
        RunUtils.execute(command);

        finishProcess("emulator64-" + platform);
        finishProcess("emulator-" + platform);
    }

    //Only for Unix
    private static void stopDdmsProcess() {
        if (SystemInfo.isUnix) {
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
                RunUtils.execute(killCommand);
            }
        }
    }

    public void finishEmulatorProcesses() {
        System.out.println("Stopping adb...");
        OutputUtils.checkResult(RunUtils.execute(getStopCommandForAdb()));
        finishProcess("adb");
        stopDdmsProcess();
    }

    //Only for Unix
    private static void finishProcess(String processName) {
        if (SystemInfo.isUnix) {
            GeneralCommandLine pidOfProcess = new GeneralCommandLine();
            pidOfProcess.setExePath("pidof");
            pidOfProcess.addParameter(processName);
            RunResult runResult = RunUtils.execute(pidOfProcess);
            String processIdsStr = runResult.getOutput().substring(("pidof " + processName).length());
            List<String> processIds = StringUtil.getWordsIn(processIdsStr);
            for (String pid : processIds) {
                GeneralCommandLine killCommand = new GeneralCommandLine();
                killCommand.setExePath("kill");
                killCommand.addParameter("-s");
                killCommand.addParameter("9");
                killCommand.addParameter(pid);
                RunUtils.execute(killCommand);
            }
        }
    }

    public String runTestsViaAdb() {
        System.out.println("Running tests via adb...");
        GeneralCommandLine adbCommand = createAdbCommand();
        //adb shell am instrument -w -r org.jetbrains.kotlin.android.tests/android.test.InstrumentationTestRunner
        adbCommand.addParameters("shell", "am", "instrument", "-w", "-r", "org.jetbrains.kotlin.android.tests/android.test.InstrumentationTestRunner");
        RunResult execute = RunUtils.execute(adbCommand);
        return execute.getOutput();
    }

    private void stopRedundantEmulators(PathManager pathManager) {
        GeneralCommandLine commandLineForListOfDevices = createAdbCommand();
        commandLineForListOfDevices.addParameter("devices");
        RunResult runResult = RunUtils.execute(commandLineForListOfDevices);
        OutputUtils.checkResult(runResult);

        Matcher matcher = EMULATOR_PATTERN.matcher(runResult.getOutput());
        boolean isDdmsStopped = false;
        while (matcher.find()) {
            System.out.println("Stopping redundant emulator...");
            GeneralCommandLine commandLineForStoppingEmulators = new GeneralCommandLine();
            if (SystemInfo.isWindows) {
                commandLineForStoppingEmulators.setExePath("taskkill");
                commandLineForStoppingEmulators.addParameter("/F");
                commandLineForStoppingEmulators.addParameter("/IM");
                commandLineForStoppingEmulators.addParameter("emulator-arm.exe");
                OutputUtils.checkResult(RunUtils.execute(commandLineForStoppingEmulators));
                break;
            }
            else {
                if (!isDdmsStopped && SystemInfo.isUnix) {
                    stopEmulator();
                    stopDdmsProcess();
                    isDdmsStopped = true;
                }
                commandLineForStoppingEmulators.setExePath(pathManager.getPlatformToolsFolderInAndroidSdk() + "/adb");
                commandLineForStoppingEmulators.addParameter("-s");
                commandLineForStoppingEmulators.addParameter(matcher.group());
                commandLineForStoppingEmulators.addParameter("emu");
                commandLineForStoppingEmulators.addParameter("kill");
                OutputUtils.checkResult(RunUtils.execute(commandLineForStoppingEmulators));
            }
        }
        OutputUtils.checkResult(RunUtils.execute(commandLineForListOfDevices));
    }
}
