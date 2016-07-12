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

import org.jetbrains.kotlin.script.KotlinScriptDefinition;

import java.util.List;

public class CommonConfigurationKeys {
    private CommonConfigurationKeys() {
    }

    // roots, including dependencies and own source
    public static final CompilerConfigurationKey<List<ContentRoot>> CONTENT_ROOTS =
            CompilerConfigurationKey.create("content roots");

    public static final CompilerConfigurationKey<List<KotlinScriptDefinition>> SCRIPT_DEFINITIONS_KEY =
            CompilerConfigurationKey.create("script definitions");

    public static final CompilerConfigurationKey<Boolean> DISABLE_INLINE =
            CompilerConfigurationKey.create("disable inline");
}
