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

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class JetTypeUtil {
    @NotNull
    public static String toString(@NotNull JetType type) {
        List<TypeProjection> arguments = type.getArguments();
        return type.getConstructor() +
               (arguments.isEmpty() ? "" : "<" + argumentsToString(arguments) + ">") +
               (type.isNullable() ? "?" : "");
    }

    @NotNull
    private static StringBuilder argumentsToString(@NotNull List<TypeProjection> arguments) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<TypeProjection> iterator = arguments.iterator(); iterator.hasNext();) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb;
    }
}
