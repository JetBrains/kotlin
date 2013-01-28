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

package org.jetbrains.jet.lang.types.ref;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SimpleParser {

    private final String input;
    private int position = 0;

    public SimpleParser(@NotNull String input) {
        this.input = input;
    }

    public void checkEof() {
        if (!lookingAtEof()) {
            throw new IllegalStateException("expecting EOF");
        }
    }

    public void checkNotEof() {
        if (lookingAtEof()) {
            throw new IllegalStateException("unexpected EOF");
        }
    }

    public boolean lookingAtEof() {
        return position == input.length();
    }

    public char peek() {
        checkNotEof();
        return input.charAt(position);
    }

    public boolean lookingAt(char c) {
        return !lookingAtEof() && peek() == c;
    }

    public char next() {
        checkNotEof();
        return input.charAt(position++);
    }

    public void consume(char c) {
        char next = next();
        if (next != c) {
            throw new IllegalStateException("invalid next, expecting " + c + ", actual " + next);
        }
    }

    public boolean consumeIfLookingAt(char c) {
        if (lookingAt(c)) {
            consume(c);
            return true;
        }
        else {
            return false;
        }
    }

    @NotNull
    public String consume(@NotNull Pattern pattern) {
        Matcher matcher = pattern.matcher(input.substring(position));
        if (!matcher.lookingAt()) {
            throw new IllegalStateException("expecting " + pattern + " at pos " + position);
        }
        String r = matcher.group();
        position += r.length();
        return r;
    }
}
