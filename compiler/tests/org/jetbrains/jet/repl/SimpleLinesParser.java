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

package org.jetbrains.jet.repl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see org.jetbrains.jet.lang.types.ref.SimpleParser
 */
class SimpleLinesParser {

    @NotNull
    private final List<String> lines;
    private int position;

    SimpleLinesParser(@NotNull List<String> lines) {
        this.lines = lines;
    }

    public boolean lookingAtEof() {
        return position == lines.size();
    }

    public void checkNotEof() {
        if (lookingAtEof()) {
            throw new IllegalStateException("unexpected EOF");
        }
    }

    @NotNull
    public String lookahead() {
        checkNotEof();
        return lines.get(position);
    }

    @Nullable
    public Matcher lookingAt(@NotNull Pattern pattern) {
        if (lookingAtEof()) {
            return null;
        }
        Matcher matcher = pattern.matcher(lookahead());
        if (matcher.matches()) {
            return matcher;
        }
        else {
            return null;
        }
    }

    @NotNull
    public Matcher next(@NotNull Pattern pattern) {
        Matcher r = lookingAt(pattern);
        if (r == null) {
            throw new IllegalStateException("line " + position + " must match " + pattern);
        }
        ++position;
        return r;
    }

    @NotNull
    public String next() {
        String r = lookahead();
        ++position;
        return r;
    }
}
