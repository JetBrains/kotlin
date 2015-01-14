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

package org.jetbrains.kotlin.cli.jvm.compiler;

import java.util.Arrays;
import java.util.List;

public class EnvironmentConfigFiles {

    private static final String EXTENSIONS_DIR = "extensions/";

    private static final String COMMON_CONFIG_FILE = EXTENSIONS_DIR + "common.xml";
    private static final String JVM_CONFIG_FILE = EXTENSIONS_DIR + "kotlin2jvm.xml";
    private static final String JS_CONFIG_FILE = EXTENSIONS_DIR + "kotlin2js.xml";

    public static final List<String> JVM_CONFIG_FILES = Arrays.asList(COMMON_CONFIG_FILE, JVM_CONFIG_FILE);
    public static final List<String> JS_CONFIG_FILES = Arrays.asList(COMMON_CONFIG_FILE, JS_CONFIG_FILE);

    private EnvironmentConfigFiles() {}
}
