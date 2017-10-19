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

package org.jetbrains.kotlin.generators.tests.generator;

import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class TestGeneratorUtil {
    @NotNull
    public static String escapeForJavaIdentifier(String fileName) {
        // A file name may contain characters (like ".") that can't be a part of method name
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);

            if (Character.isJavaIdentifierPart(c)) {
                result.append(c);
            }
            else {
                result.append("_");
            }
        }
        return result.toString();
    }

    @NotNull
    public static String fileNameToJavaIdentifier(@NotNull File file) {
        return StringsKt.capitalize(escapeForJavaIdentifier(file.getName()));
    }
}
