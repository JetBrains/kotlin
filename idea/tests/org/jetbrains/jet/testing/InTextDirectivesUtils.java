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

package org.jetbrains.jet.testing;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikolay Krasko
 */
public final class InTextDirectivesUtils {

    private InTextDirectivesUtils() {
    }

    @Nullable
    public static Integer getPrefixedInt(String fileText, String prefix) {
        final String[] numberStrings = findListWithPrefix(prefix, fileText);
        if (numberStrings.length > 0) {
            return Integer.parseInt(numberStrings[0]);
        }

        return null;
    }

    @NotNull
    public static String[] findListWithPrefix(String prefix, String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        for (String line : findLinesWithPrefixRemoved(prefix, fileText)) {
            String[] variants = line.split(",");

            for (String variant : variants) {
                result.add(StringUtil.unquoteString(variant.trim()));
            }
        }

        return ArrayUtil.toStringArray(result);
    }

    @NotNull
    private static List<String> findLinesWithPrefixRemoved(String prefix, String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        for (String line : fileNonEmptyLines(fileText)) {
            if (line.startsWith(prefix)) {
                result.add(line.substring(prefix.length()).trim());
            }
        }

        return result;
    }


    @NotNull
    private static List<String> fileNonEmptyLines(String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        try {
            BufferedReader reader = new BufferedReader(new StringReader(fileText));
            try {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        result.add(line.trim());
                    }
                }
            } finally {
                reader.close();
            }
        } catch(IOException e) {
            throw new AssertionError(e);
        }

        return result;
    }
}
