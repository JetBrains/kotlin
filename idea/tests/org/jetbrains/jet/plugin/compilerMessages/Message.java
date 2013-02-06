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

package org.jetbrains.jet.plugin.compilerMessages;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ConstantConditions")
public final class Message {

    @NotNull
    public static Message warning() {
        return new Message(CompilerMessageCategory.WARNING);
    }

    @NotNull
    public static Message error() {
        return new Message(CompilerMessageCategory.ERROR);
    }

    @NotNull
    public static Message info() {
        return new Message(CompilerMessageCategory.INFORMATION);
    }

    @NotNull
    public static Message stats() {
        return new Message(CompilerMessageCategory.STATISTICS);
    }

    @NotNull
    public CompilerMessageCategory category;
    @Nullable
    public String url = null;
    @Nullable
    public String message = null;
    @Nullable
    public String textStartsWith = null;
    @Nullable
    private String textMatchesRegexp = null;
    public int column = -1;
    public int line = -1;

    public Message(CompilerMessageCategory category) {
        this.category = category;
    }

    public Message(CompilerMessageCategory category, String message, String url, int num, int num1) {
        this(category);
        text(message).at(url, num, num1);
    }


    public Message at(String url, int line, int column) {
        this.url = url;
        this.line = line;
        this.column = column;
        return this;
    }

    public Message text(String message) {
        this.message = message;
        return this;
    }

    public Message textStartsWith(String message) {
        this.textStartsWith = message;
        return this;
    }

    public Message textMatchesRegexp(String regexp) {
        this.textMatchesRegexp = regexp;
        return this;
    }

    public void check(@NotNull Message other) {
        checkMessages(other);
        Assert.assertEquals(other.category, this.category);
        Assert.assertEquals(other.line, this.line);
        Assert.assertEquals(other.column, this.column);
        if (this.url != null) {
            Assert.assertTrue(other.url.endsWith(this.url));
        }
    }

    private void checkMessages(Message other) {
        if (textStartsWith != null) {
            Assert.assertTrue("Message should start with:\n" + textStartsWith + "\nBut it is:\n" + other.message,
                              other.message.startsWith(textStartsWith));
            return;
        }
        if (textMatchesRegexp != null) {
            Assert.assertTrue("Message should match regexp:\n" + textMatchesRegexp + "\nBut it is:\n" + other.message,
                              other.message.matches(textMatchesRegexp));
            return;
        }
        Assert.assertEquals(this.message, other.message);
    }

    @Override
    public String toString() {
        return category + ": " + message + " at " + line + ":" + column + " in " + url;
    }
}
