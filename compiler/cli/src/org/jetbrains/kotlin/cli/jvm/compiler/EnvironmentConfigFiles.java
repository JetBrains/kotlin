/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

public enum EnvironmentConfigFiles {
    JVM_CONFIG_FILES("extensions/common.xml", "extensions/kotlin2jvm.xml"),
    JS_CONFIG_FILES("extensions/common.xml", "extensions/kotlin2js.xml"),
    NATIVE_CONFIG_FILES("extensions/common.xml"),
    METADATA_CONFIG_FILES("extensions/common.xml"),
    EMPTY();

    private final List<String> files;

    EnvironmentConfigFiles(String... fileArray) {
        files = Arrays.asList(fileArray);
    }

    public List<String> getFiles() {
        return files;
    }
}
