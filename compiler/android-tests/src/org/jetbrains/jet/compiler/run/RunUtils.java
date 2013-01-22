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

package org.jetbrains.jet.compiler.run;

import com.google.common.base.Charsets;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
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

    public static RunResult execute(final GeneralCommandLine commandLine) {
        return run(commandLine, null);
    }

    public static RunResult execute(final GeneralCommandLine commandLine, @Nullable String input) {
        return run(commandLine, input);
    }

    public static RunResult executeOnSeparateThread(final GeneralCommandLine commandLine, boolean waitForEnd) {
        return executeOnSeparateThread(commandLine, waitForEnd, null);
    }

    public static RunResult executeOnSeparateThread(final GeneralCommandLine commandLine,
            boolean waitForEnd,
            @Nullable final String input) {
        final Ref<RunResult> resultRef = new Ref<RunResult>();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                resultRef.set(RunUtils.run(commandLine, input));
            }
        });

        t.start();

        if (waitForEnd) {
            ThreadUtils.wait(t, 600);
            return resultRef.get();
        }
        return new RunResult(true, "OK");
    }

    private static RunResult run(final GeneralCommandLine commandLine, @Nullable final String input) {
        System.out.println("RUN COMMAND: " + commandLine.getCommandLineString());
        final StringBuilder stdOut = new StringBuilder();
        final StringBuilder stdErr = new StringBuilder();

        final OSProcessHandler handler;
        try {
            handler = new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString(), Charsets.UTF_8);
            if (input != null) {
                handler.getProcessInput().write(input.getBytes());
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
                content.append(StringUtil.trimTrailing(line));
                content.append("\n");
            }
        });

        handler.startNotify();

        try {
            handler.waitFor(300000);
        }
        catch (ProcessCanceledException e) {
            return new RunResult(false, getStackTrace(e));
        }

        if (!handler.isProcessTerminated()) {
            return new RunResult(false, "Timeout exception: execution was terminated after 5 min.");
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
            if (!commandLine.getCommandLineString().contains("install")) {
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
