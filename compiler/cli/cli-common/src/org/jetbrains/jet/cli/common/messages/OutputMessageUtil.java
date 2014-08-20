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

package org.jetbrains.jet.cli.common.messages;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

public class OutputMessageUtil {
    private static final String SOURCE_FILES_PREFIX = "Sources:";
    private static final String OUTPUT_FILES_PREFIX = "Output:";

    @NotNull
    public static String renderException(@NotNull Throwable e) {
        StringWriter out = new StringWriter();
        e.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    @NotNull
    public static String formatOutputMessage(Collection<File> sourceFiles, File outputFile) {
        return OUTPUT_FILES_PREFIX + "\n" + outputFile.getPath() + "\n" +
               SOURCE_FILES_PREFIX + "\n" + StringUtil.join(sourceFiles, "\n");
    }

    @Nullable
    public static Output parseOutputMessage(@NotNull String message) {
        String[] strings = message.split("\n");

        // Must have at least one line per prefix
        if (strings.length <= 2) return null;

        if (!OUTPUT_FILES_PREFIX.equals(strings[0])) return null;

        if (SOURCE_FILES_PREFIX.equals(strings[1])) {
            // Output:
            // Sources:
            // ...
            return new Output(parseSourceFiles(strings, 2), null);
        }
        else {
            File outputFile = new File(strings[1]);

            if (!SOURCE_FILES_PREFIX.equals(strings[2])) return null;

            return new Output(parseSourceFiles(strings, 3), outputFile);
        }
    }

    private static Collection<File> parseSourceFiles(String[] strings, int start) {
        Collection<File> sourceFiles = ContainerUtil.newArrayList();
        for (int i = start; i < strings.length; i++) {
            sourceFiles.add(new File(strings[i]));
        }
        return sourceFiles;
    }

    public static class Output {
        @NotNull
        public final Collection<File> sourceFiles;
        @Nullable
        public final File outputFile;

        public Output(@NotNull Collection<File> sourceFiles, @Nullable File outputFile) {
            this.sourceFiles = sourceFiles;
            this.outputFile = outputFile;
        }
    }
}
