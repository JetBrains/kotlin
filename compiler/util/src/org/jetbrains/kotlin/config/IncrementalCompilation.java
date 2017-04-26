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

package org.jetbrains.kotlin.config;

import org.jetbrains.annotations.TestOnly;

public class IncrementalCompilation {
    private static final String INCREMENTAL_COMPILATION_PROPERTY = "kotlin.incremental.compilation";

    public static boolean isEnabled() {
        return !"false".equals(System.getProperty(INCREMENTAL_COMPILATION_PROPERTY));
    }

    @TestOnly
    public static void setIsEnabled(boolean value) {
        System.setProperty(INCREMENTAL_COMPILATION_PROPERTY, String.valueOf(value));
    }
}