/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.compiler.run;

import com.google.common.base.Charsets;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.OutputUtils;
import org.jetbrains.jet.compiler.ThreadUtils;
import org.jetbrains.jet.compiler.run.result.RunResult;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


public class RunUtils {
    private RunUtils() {
    }

    public static class RunSettings {
        public final GeneralCommandLine commandLine;
        public final String input;
        public final boolean waitForEnd;
        public final String outputPrefix;
        public final boolean printOutputAtAppearance;

        public RunSettings(
                GeneralCommandLine commandLine,
                @Nullable String input,
                boolean waitForEnd,
                @Nullable String outputPrefix,
                boolean printOutputAtAppearance
        ) {
            this.commandLine = commandLine;
            this.input = input;
            this.waitForEnd = waitForEnd;
            this.outputPrefix = outputPrefix;
            this.printOutputAtAppearance = printOutputAtAppearance;
        }

        public RunSettings(GeneralCommandLine commandLine) {
            this.commandLine = commandLine;
            this.input = null;
            this.waitForEnd = true;
            this.outputPrefix = null;
            this.printOutputAtAppearance = false;
        }

        @Override
        public String toString() {
            return "commandLine=" + commandLine.getCommandLineString() + " " +
                   "input=" + input + " " +
                   "waitForEnd=" + waitForEnd + " " +
                   "outputPrefix=" + outputPrefix + " " +
                   "printOutputAtAppearance=" + printOutputAtAppearance + " ";
        }
    }

    public static RunResult execute(@NotNull GeneralCommandLine commandLine) {
        return run(new RunSettings(commandLine));
    }

    public static RunResult execute(@NotNull RunSettings settings) {
        assert settings.waitForEnd == true : "Use executeOnSeparateThread() instead";
        return run(settings);
    }

    public static void executeOnSeparateThread(@NotNull final RunSettings settings) {
        assert settings.waitForEnd == false : "Use execute() instead";
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                RunUtils.run(settings);
            }
        });

        t.start();
    }

    private static RunResult run(final RunSettings settings) {
        System.out.println("RUN COMMAND: " + settings);
        final StringBuilder stdOut = new StringBuilder();
        final StringBuilder stdErr = new StringBuilder();

        final OSProcessHandler handler;
        try {
            handler = new OSProcessHandler(settings.commandLine.createProcess(), settings.commandLine.getCommandLineString(), Charsets.UTF_8);
            if (settings.input != null) {
                handler.getProcessInput().write(settings.input.getBytes());
            }
            close(handler.getProcessInput());
        }
        catch (ExecutionException e) {
            return new RunResult(false, getStackTrace(e));
        }
        catch (IOException e) {
            return new RunResult(false, getStackTrace(e));
        }

        handler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(ProcessEvent event) {
                System.out.println("TERMINATED: " + settings.commandLine);
                super.processTerminated(event);
            }

            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                String str = event.getText();
                if (outputType == ProcessOutputTypes.STDOUT || outputType == ProcessOutputTypes.SYSTEM) {
                    appendToContent(stdOut, str);
                }
                else if (outputType == ProcessOutputTypes.STDERR) {
                    appendToContent(stdErr, str);
                }
            }

            private synchronized void appendToContent(StringBuilder content, String line) {
                if (settings.printOutputAtAppearance) {
                    System.out.println(getPrefixString() + StringUtil.trimTrailing(line));
                    System.out.flush();
                }
                else {
                    content.append(getPrefixString());
                    content.append(StringUtil.trimTrailing(line));
                    content.append("\n");
                }
            }

            private String getPrefixString() {
                return (settings.outputPrefix != null) ? settings.outputPrefix + " " : "";
            }
        });

        handler.startNotify();

        if (settings.waitForEnd) {
            handler.waitFor(400000);

            if (!handler.isProcessTerminated()) {
                System.out.println("Output before handler.isProcessTerminated() " + settings.commandLine);
                System.out.println(stdOut);
                System.err.println(stdErr);
                return new RunResult(false, "Timeout exception: execution was terminated after ~7 min.");
            }
        }
        else {
            handler.waitFor();
        }

        int exitCode = handler.getProcess().exitValue();

        if (exitCode != 0) {
            return new RunResult(false, builderToString(stdOut) + builderToString(stdErr));
        }
        else {
            String output = builderToString(stdOut) + builderToString(stdErr);
            if (OutputUtils.isBuildFailed(output)) {
                return new RunResult(false, output);
            }
            if (!settings.commandLine.getCommandLineString().contains("install")) {
                System.out.print(output);
            }
            return new RunResult(true, output);
        }
    }

    private static String builderToString(StringBuilder builder) {
        return builder.length() > 0 ? builder.toString() : "";
    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getStackTrace(Throwable t) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        try {
            printWriter.write(t.getMessage());
            printWriter.write("\n");
            t.printStackTrace(printWriter);
        }
        finally {
            close(printWriter);
        }
        return writer.toString();
    }

}
