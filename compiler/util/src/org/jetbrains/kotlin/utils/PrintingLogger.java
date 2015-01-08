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

package org.jetbrains.kotlin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class PrintingLogger extends Logger {

    public static final Logger SYSTEM_OUT = new PrintingLogger(System.out);
    public static final Logger SYSTEM_ERR = new PrintingLogger(System.err);

    private final PrintStream out;

    public PrintingLogger(@NotNull PrintStream out) {
        this.out = out;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(@NonNls String message) {
        out.println(message);
    }

    @Override
    public void debug(@Nullable Throwable t) {
        //noinspection ConstantConditions
        t.printStackTrace(out);
    }

    @Override
    public void debug(@NonNls String message, @Nullable Throwable t) {
        debug(message);
        debug(t);
    }

    @Override
    public void info(@NonNls String message) {
        debug(message);
    }

    @Override
    public void info(@NonNls String message, @Nullable Throwable t) {
        debug(message, t);
    }

    @Override
    public void warn(@NonNls String message, @Nullable Throwable t) {
        debug(message, t);
    }

    @Override
    public void error(@NonNls String message, @Nullable Throwable t, @NonNls @NotNull String... details) {
        debug(message, t);
        for (String detail : details) {
            debug(detail);
        }
    }

    @Override
    public void setLevel(Level level) {
    }
}
