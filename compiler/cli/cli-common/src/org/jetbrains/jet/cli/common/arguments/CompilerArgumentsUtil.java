/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.common.arguments;

import org.jetbrains.annotations.Nullable;

public class CompilerArgumentsUtil {
    public static final boolean DEFAULT_INLINE_FLAG = true;
    public static final boolean DEFAULT_INLINE_FLAG_FOR_TEST = true;

    public static boolean optionToInlineFlag(@Nullable String option) {
        boolean enableInline = "on".equalsIgnoreCase(option) || "true".equalsIgnoreCase(option);
        return (enableInline || "off".equalsIgnoreCase(option) || "false".equalsIgnoreCase(option)) ? enableInline : DEFAULT_INLINE_FLAG;
    }

    public static boolean checkInlineOption(@Nullable String option)  {
        if (option == null ||
            "on".equalsIgnoreCase(option) ||
            "off".equalsIgnoreCase(option) ||
            "true".equalsIgnoreCase(option) ||
            "false".equalsIgnoreCase(option)) {
            return true;
        }

        return false;
    }

    public static String getWrongOptionErrorMessage(@Nullable String inline) {
        return "Wrong value for inline option: '" + inline + "'. Should be 'on'/'off' or 'true'/'false'";
    }
}
