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

package org.jetbrains.jet.cli.jvm.repl;

import com.intellij.openapi.Disposable;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class ReplFromTerminal {

    private ReplInterpreter replInterpreter;
    private Throwable replInitializationFailed;
    private final Object waitRepl = new Object();

    private final ConsoleReader consoleReader;

    public ReplFromTerminal(@NotNull final Disposable disposable, @NotNull final CompilerDependencies compilerDependencies) {
        final List<File> extraClasspath = Collections.<File>emptyList();
        new Thread("initialize-repl") {
            @Override
            public void run() {
                try {
                    replInterpreter = new ReplInterpreter(disposable, compilerDependencies, extraClasspath);
                } catch (Throwable e) {
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
            consoleReader.setHistory(new FileHistory(new File(new File(System.getProperty("user.home")), ".kotlin_history")));
        } catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
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
                } catch (Throwable e) {
                    throw ExceptionUtils.rethrow(e);
                }
            }
            if (replInterpreter != null) {
                return replInterpreter;
            }
            throw ExceptionUtils.rethrow(replInitializationFailed);
        }
    }

    private void doRun() {
        try {
            System.out.println("Kotlin interactive shell");
            System.out.println("Type :help for help, :quit for quit");
            while (true) {
                boolean next = one();
                if (!next) {
                    break;
                }
            }
        } catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        } finally {
            try {
                ((FileHistory) consoleReader.getHistory()).flush();
            } catch (Exception e) {
                System.err.println("failed to flush history: " + e);
            }
        }
    }

    private boolean one() {
        try {
            String line = consoleReader.readLine(">>> ");
            if (line == null) {
                return false;
            }

            if (line.startsWith(":")) {
                return oneCommand(line.substring(1));
            }

            ReplInterpreter.LineResult lineResult = getReplInterpreter().eval(line);
            if (!lineResult.isSuccessful()) {
                System.out.print(lineResult.getErrorText());
            }
            else if (!lineResult.isUnit()) {
                System.out.println(lineResult.getValue());
            }
            return true;
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    private boolean oneCommand(@NotNull String command) {
        if (command.equals("help")) {
            System.out.println("This is Kotlin REPL help");
            System.out.println("Available commands are:");
            System.out.println(":help                   show this help");
            System.out.println(":quit                   exit the interpreter");
            System.out.println(":dump bytecode          dump classes to terminal");
            return true;
        }
        else if (command.equals("dump bytecode")) {
            getReplInterpreter().dumpClasses(new PrintWriter(System.out));
            return true;
        }
        else if (command.equals("quit")) {
            return false;
        }
        else {
            System.out.println("Unknown command");
            System.out.println("Type :help for help");
            return true;
        }
    }

    public static void run(@NotNull Disposable disposable, @NotNull CompilerDependencies compilerDependencies) {
        new ReplFromTerminal(disposable, compilerDependencies).doRun();
    }

}
