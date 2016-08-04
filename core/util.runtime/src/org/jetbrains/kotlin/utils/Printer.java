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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class Printer {
    private static final String DEFAULT_INDENTATION_UNIT = "    ";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    protected final Appendable out;
    private final int maxBlankLines;

    private String indent = "";
    private final String indentUnit;
    private int blankLineCountIncludingCurrent = 0;
    private boolean withholdIndentOnce = false;

    public Printer(@NotNull Appendable out) {
        this(out, Integer.MAX_VALUE);
    }

    public Printer(@NotNull Appendable out, @NotNull String indentUnit) {
        this(out, Integer.MAX_VALUE, indentUnit);
    }

    public Printer(@NotNull Appendable out, int maxBlankLines) {
        this(out, maxBlankLines, DEFAULT_INDENTATION_UNIT);
    }

    public Printer(@NotNull Appendable out, int maxBlankLines, @NotNull String indentUnit) {
        this.out = out;
        this.maxBlankLines = maxBlankLines;
        this.indentUnit = indentUnit;
    }

    private void append(Object o) {
        try {
            out.append(o.toString());
        }
        catch (IOException e) {
            // Do nothing
        }
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
            append(LINE_SEPARATOR);
        }
    }

    @NotNull
    public Printer print(Object... objects) {
        if (withholdIndentOnce) {
            withholdIndentOnce = false;
        }
        else if (objects.length > 0) {
            printIndent();
        }
        printWithNoIndent(objects);

        return this;
    }

    public void printIndent() {
        append(indent);
    }

    @NotNull
    public Printer printWithNoIndent(Object... objects) {
        for (Object object : objects) {
            blankLineCountIncludingCurrent = 0;
            append(object);
        }

        return this;
    }

    @NotNull
    public Printer withholdIndentOnce() {
        withholdIndentOnce = true;
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
        indent += indentUnit;

        return this;
    }

    @NotNull
    public Printer popIndent() {
        if (indent.length() < indentUnit.length()) {
            throw new IllegalStateException("No indentation to pop");
        }

        indent = indent.substring(indentUnit.length());

        return this;
    }

    @NotNull
    public Printer separated(Object separator, Object... items) {
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                printlnWithNoIndent(separator);
            }
            printlnWithNoIndent(items[i]);
        }
        return this;
    }

    @NotNull
    public Printer separated(Object separator, Collection<?> items) {
        for (Iterator<?> iterator = items.iterator(); iterator.hasNext(); ) {
            printlnWithNoIndent(iterator.next());
            if (iterator.hasNext()) {
                printlnWithNoIndent(separator);
            }
        }
        return this;
    }
}
