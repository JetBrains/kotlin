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
    public static boolean optionToBooleanFlag(@Nullable String option, boolean defaultValue) {
        boolean enabled = "on".equalsIgnoreCase(option) || "true".equalsIgnoreCase(option);
        boolean disabled = "off".equalsIgnoreCase(option) || "false".equalsIgnoreCase(option);
        return (enabled || disabled) ? enabled : defaultValue;
    }

    public static boolean checkOption(@Nullable String option)  {
        return option == null ||
               "on".equalsIgnoreCase(option) ||
               "off".equalsIgnoreCase(option) ||
               "true".equalsIgnoreCase(option) ||
               "false".equalsIgnoreCase(option);
    }

    public static String getWrongCheckOptionErrorMessage(@Nullable String option, @Nullable String value) {
        return "Wrong value for " + option + " option: '" + value + "'. Should be 'on'/'off' or 'true'/'false'";
    }
}
