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

package org.jetbrains.jet.compiler;

import static com.intellij.openapi.components.StoragePathMacros.PROJECT_CONFIG_DIR;

public class SettingConstants {
    private SettingConstants() {}

    public static final String KOTLIN_COMMON_COMPILER_SETTINGS_SECTION = "KotlinCommonCompilerSettings";
    public static final String KOTLIN_TO_JS_COMPILER_SETTINGS_SECTION = "Kotlin2JsCompilerSettings";
    public static final String KOTLIN_TO_JVM_COMPILER_SETTINGS_SECTION = "Kotlin2JvmCompilerSettings";

    public static final String KOTLIN_COMPILER_SETTINGS_FILE = "kotlinc.xml";
    public static final String KOTLIN_COMPILER_SETTINGS_PATH = PROJECT_CONFIG_DIR + "/" + KOTLIN_COMPILER_SETTINGS_FILE;
}
