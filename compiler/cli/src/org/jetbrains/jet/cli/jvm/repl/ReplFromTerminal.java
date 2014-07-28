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

package org.jetbrains.jet.cli.jvm.repl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class ReplFromTerminal {

    private ReplInterpreter replInterpreter;
    private Throwable replInitializationFailed;
    private final Object waitRepl = new Object();

    private final ConsoleReader consoleReader;

    public ReplFromTerminal(
            @NotNull final Disposable disposable,
            @NotNull final CompilerConfiguration compilerConfiguration) {
        new Thread("initialize-repl") {
            @Override
            public void run() {
                try {
                    replInterpreter = new ReplInterpreter(disposable, compilerConfiguration);
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
            consoleReader = new ConsoleReader("kotlin", System.in, System.out, null);
            consoleReader.setHistoryEnabled(true);
            consoleReader.setExpandEvents(false);
            consoleReader.setHistory(new FileHistory(new File(new File(System.getProperty("user.home")), ".kotlin_history")));
        }
        catch (Exception e) {
            throw UtilsPackage.rethrow(e);
        }
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
                    throw UtilsPackage.rethrow(e);
                }
            }
            if (replInterpreter != null) {
                return replInterpreter;
            }
            throw UtilsPackage.rethrow(replInitializationFailed);
        }
    }

    private void doRun() {
        try {
            System.out.println("Kotlin interactive shell");
            System.out.println("Type :help for help, :quit for quit");
            WhatNextAfterOneLine next = WhatNextAfterOneLine.READ_LINE;
            while (true) {
                next = one(next);
                if (next == WhatNextAfterOneLine.QUIT) {
                    break;
                }
            }
        }
        catch (Exception e) {
            throw UtilsPackage.rethrow(e);
        }
        finally {
            try {
                ((FileHistory) consoleReader.getHistory()).flush();
            }
            catch (Exception e) {
                System.err.println("failed to flush history: " + e);
            }
        }
    }

    private enum WhatNextAfterOneLine {
        READ_LINE,
        INCOMPLETE,
        QUIT,
    }

    @NotNull
    private WhatNextAfterOneLine one(@NotNull WhatNextAfterOneLine next) {
        try {
            String line = consoleReader.readLine(next == WhatNextAfterOneLine.INCOMPLETE ? "... " : ">>> ");
            if (line == null) {
                return WhatNextAfterOneLine.QUIT;
            }

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
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    private ReplInterpreter.LineResultType eval(@NotNull String line) {
        ReplInterpreter.LineResult lineResult = getReplInterpreter().eval(line);
        if (lineResult.getType() == ReplInterpreter.LineResultType.SUCCESS) {
            if (!lineResult.isUnit()) {
                System.out.println(lineResult.getValue());
            }
        }
        else if (lineResult.getType() == ReplInterpreter.LineResultType.INCOMPLETE) {
        }
        else if (lineResult.getType() == ReplInterpreter.LineResultType.ERROR) {
            System.out.print(lineResult.getErrorText());
        }
        else {
            throw new IllegalStateException("unknown line result type: " + lineResult);
        }
        return lineResult.getType();
    }

    private boolean oneCommand(@NotNull String command) throws Exception {
        List<String> split = splitCommand(command);
        if (split.size() >= 1 && command.equals("help")) {
            System.out.println("This is Kotlin REPL help");
            System.out.println("Available commands are:");
            System.out.println(":help                   show this help");
            System.out.println(":quit                   exit the interpreter");
            System.out.println(":dump bytecode          dump classes to terminal");
            System.out.println(":load <file>            load script from specified file");
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
            System.out.println("Unknown command");
            System.out.println("Type :help for help");
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
