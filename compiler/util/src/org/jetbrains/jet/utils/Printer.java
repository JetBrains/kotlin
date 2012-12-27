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

package org.jetbrains.jet.utils;

import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class Printer {
    private static final String INDENTATION_UNIT = "    ";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final StringBuilder out;
    private final int maxBlankLines;

    private String indent = "";
    private int blankLineCountIncludingCurrent = 0;

    public Printer(@NotNull StringBuilder out) {
        this(out, Integer.MAX_VALUE);
    }

    public Printer(@NotNull StringBuilder out, int maxBlankLines) {
        this.out = out;
        this.maxBlankLines = maxBlankLines;
    }

    @NotNull
    public Printer println(Object... objects) {
        print(objects);
        printLineSeparator();

        return this;
    }

    private void printLineSeparator() {
        if (blankLineCountIncludingCurrent <= maxBlankLines) {
            blankLineCountIncludingCurrent++;
            out.append(LINE_SEPARATOR);
        }
    }

    @NotNull
    public Printer print(Object... objects) {
        out.append(indent);
        printWithNoIndent(objects);

        return this;
    }

    @NotNull
    public Printer printWithNoIndent(Object... objects) {
        for (Object object : objects) {
            blankLineCountIncludingCurrent = 0;
            out.append(object);
        }

        return this;
    }

    @NotNull
    public Printer printlnWithNoIndent(Object... objects) {
        printWithNoIndent(objects);
        printLineSeparator();

        return this;
    }

    @NotNull
    public Printer pushIndent() {
        indent += INDENTATION_UNIT;

        return this;
    }

    @NotNull
    public Printer popIndent() {
        if (indent.length() < INDENTATION_UNIT.length()) {
            throw new IllegalStateException("No indentation to pop");
        }

        indent = indent.substring(INDENTATION_UNIT.length());

        return this;
    }
}
