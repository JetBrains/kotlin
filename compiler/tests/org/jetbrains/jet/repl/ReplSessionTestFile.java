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

package org.jetbrains.jet.repl;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class ReplSessionTestFile {

    @NotNull
    private final List<Pair<String, String>> lines;

    public ReplSessionTestFile(@NotNull List<Pair<String, String>> lines) {
        this.lines = lines;
    }

    @NotNull
    public List<Pair<String, String>> getLines() {
        return lines;
    }

    public static ReplSessionTestFile load(@NotNull File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                return load(reader);
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ReplSessionTestFile load(@NotNull BufferedReader reader) throws IOException {
        List<Pair<String, String>> list = Lists.newArrayList();
        while (true) {
            String odd = reader.readLine();
            if (odd == null) {
                return new ReplSessionTestFile(list);
            }

            if (!odd.startsWith(">>> ")) {
                throw new IllegalStateException("odd lines must start with >>>");
            }
            String code = odd.substring(4);

            String even = reader.readLine();
            if (even == null) {
                throw new IllegalStateException("expecting even");
            }

            list.add(Pair.create(code, even));
        }
    }

}
