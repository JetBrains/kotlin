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

package org.jetbrains.kotlin.cli.jvm.repl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.KotlinVersion;
import org.jetbrains.kotlin.cli.jvm.repl.messages.*;
import org.jetbrains.kotlin.cli.jvm.repl.reader.ConsoleReplCommandReader;
import org.jetbrains.kotlin.cli.jvm.repl.reader.IdeReplCommandReader;
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommandReader;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class ReplFromTerminal {

    private ReplInterpreter replInterpreter;
    private Throwable replInitializationFailed;
    private final Object waitRepl = new Object();

    private final boolean ideMode;
    private ReplSystemInWrapper replReader;
    private final ReplWriter replWriter;
    private final ReplErrorLogger replErrorLogger;

    private ReplCommandReader commandReader;

    public ReplFromTerminal(
            @NotNull final Disposable disposable,
            @NotNull final CompilerConfiguration compilerConfiguration
    ) {
        String replIdeMode = System.getProperty("kotlin.repl.ideMode");
        ideMode = replIdeMode != null && replIdeMode.equals("true");

        // wrapper for `out` is required to escape every input in [ideMode];
        // if [ideMode == false] then just redirects all input to [System.out]
        // if user calls [System.setOut(...)] then undefined behaviour
        if (ideMode) {
            ReplSystemOutWrapperForIde soutWrapper = new ReplSystemOutWrapperForIde(System.out);
            replWriter = soutWrapper;
            System.setOut(soutWrapper);
        }
        else {
            replWriter = new ReplConsoleWriter();
        }

        // wrapper for `in` is required to give user possibility of calling
        // [readLine] from ide-console repl
        if (ideMode) {
            replReader = new ReplSystemInWrapper(System.in, replWriter);
            System.setIn(replReader);
        }

        replErrorLogger = new ReplErrorLogger(ideMode, replWriter);

        new Thread("initialize-repl") {
            @Override
            public void run() {
                try {
                    replInterpreter = new ReplInterpreter(disposable, compilerConfiguration, ideMode, replReader);
                }
                catch (Throwable e) {
                    replInitializationFailed = e;
                }
                synchronized (waitRepl) {
                    waitRepl.notifyAll();
                }
            }
        }.start();

        try {
            commandReader = createCommandReader();
        }
        catch (Exception e) {
            replErrorLogger.logException(e);
        }
    }

    @NotNull
    private ReplCommandReader createCommandReader() {
        return ideMode ? new IdeReplCommandReader()
                       : new ConsoleReplCommandReader();
    }

    private ReplInterpreter getReplInterpreter() {
        if (replInterpreter != null) {
            return replInterpreter;
        }
        synchronized (waitRepl) {
            while (replInterpreter == null && replInitializationFailed == null) {
                try {
                    waitRepl.wait();
                }
                catch (Throwable e) {
                    throw ExceptionUtilsKt.rethrow(e);
                }
            }
            if (replInterpreter != null) {
                return replInterpreter;
            }
            throw ExceptionUtilsKt.rethrow(replInitializationFailed);
        }
    }

    private void doRun() {
        try {
            replWriter.printlnWelcomeMessage("Welcome to Kotlin version " + KotlinVersion.VERSION +
                                             " (JRE " + System.getProperty("java.runtime.version") + ")");
            replWriter.printlnWelcomeMessage("Type :help for help, :quit for quit");
            WhatNextAfterOneLine next = WhatNextAfterOneLine.READ_LINE;
            while (true) {
                next = one(next);
                if (next == WhatNextAfterOneLine.QUIT) {
                    break;
                }
            }
        }
        catch (Exception e) {
            replErrorLogger.logException(e);
        }
        finally {
            try {
                commandReader.flushHistory();
            }
            catch (Exception e) {
                replErrorLogger.logException(e);
            }
        }
    }

    public enum WhatNextAfterOneLine {
        READ_LINE,
        INCOMPLETE,
        QUIT,
    }

    @NotNull
    private WhatNextAfterOneLine one(@NotNull WhatNextAfterOneLine next) {
        try {
            String line = commandReader.readLine(next);

            if (line == null) {
                return WhatNextAfterOneLine.QUIT;
            }

            line = UnescapeUtilsKt.unescapeLineBreaks(line);

            if (line.startsWith(":") && (line.length() == 1 || line.charAt(1) != ':')) {
                boolean notQuit = oneCommand(line.substring(1));
                return notQuit ? WhatNextAfterOneLine.READ_LINE : WhatNextAfterOneLine.QUIT;
            }

            ReplInterpreter.LineResultType lineResultType = eval(line);
            if (lineResultType == ReplInterpreter.LineResultType.INCOMPLETE) {
                return WhatNextAfterOneLine.INCOMPLETE;
            }
            else {
                return WhatNextAfterOneLine.READ_LINE;
            }
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    private ReplInterpreter.LineResultType eval(@NotNull String line) {
        ReplInterpreter.LineResult lineResult = getReplInterpreter().eval(line);
        if (lineResult.getType() == ReplInterpreter.LineResultType.SUCCESS) {
            replWriter.notifyCommandSuccess();
            if (!lineResult.isUnit()) {
                replWriter.outputCommandResult(lineResult.getValue());
            }
        }
        else if (lineResult.getType() == ReplInterpreter.LineResultType.INCOMPLETE) {
            replWriter.notifyIncomplete();
        }
        else if (lineResult.getType() == ReplInterpreter.LineResultType.COMPILE_ERROR) {
            replWriter.outputCompileError(lineResult.getErrorText());
        }
        else if (lineResult.getType() == ReplInterpreter.LineResultType.RUNTIME_ERROR) {
            replWriter.outputRuntimeError(lineResult.getErrorText());
        }
        else {
            throw new IllegalStateException("unknown line result type: " + lineResult);
        }
        return lineResult.getType();
    }

    private boolean oneCommand(@NotNull String command) throws Exception {
        List<String> split = splitCommand(command);
        if (split.size() >= 1 && command.equals("help")) {
            replWriter.printlnHelpMessage("Available commands:\n" +
                                          ":help                   show this help\n" +
                                          ":quit                   exit the interpreter\n" +
                                          ":dump bytecode          dump classes to terminal\n" +
                                          ":load <file>            load script from specified file"
            );
            return true;
        }
        else if (split.size() >= 2 && split.get(0).equals("dump") && split.get(1).equals("bytecode")) {
            getReplInterpreter().dumpClasses(new PrintWriter(System.out));
            return true;
        }
        else if (split.size() >= 1 && split.get(0).equals("quit")) {
            return false;
        }
        else if (split.size() >= 2 && split.get(0).equals("load")) {
            String fileName = split.get(1);
            String scriptText = FileUtil.loadFile(new File(fileName));
            eval(scriptText);
            return true;
        }
        else {
            replWriter.printlnHelpMessage("Unknown command\n" +
                                          "Type :help for help"
            );
            return true;
        }
    }

    private static List<String> splitCommand(@NotNull String command) {
        return Arrays.asList(command.split(" "));
    }

    public static void run(@NotNull Disposable disposable, @NotNull CompilerConfiguration configuration) {
        new ReplFromTerminal(disposable, configuration).doRun();
    }

}
