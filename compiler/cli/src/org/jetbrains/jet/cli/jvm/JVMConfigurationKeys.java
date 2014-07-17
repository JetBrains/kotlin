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

package org.jetbrains.jet.cli.jvm;

import org.jetbrains.jet.config.CompilerConfigurationKey;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.cache.IncrementalCacheProvider;

import java.io.File;
import java.util.List;

public class JVMConfigurationKeys {
    private JVMConfigurationKeys() {
    }

    public static final CompilerConfigurationKey<List<File>> CLASSPATH_KEY = CompilerConfigurationKey.create("classpath");
    public static final CompilerConfigurationKey<List<File>> ANNOTATIONS_PATH_KEY = CompilerConfigurationKey.create("annotations path");

    public static final CompilerConfigurationKey<List<AnalyzerScriptParameter>> SCRIPT_PARAMETERS = CompilerConfigurationKey.create("script");

    public static final CompilerConfigurationKey<Boolean> DISABLE_CALL_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null call assertions");
    public static final CompilerConfigurationKey<Boolean> DISABLE_PARAM_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null parameter assertions");
    public static final CompilerConfigurationKey<Boolean> DISABLE_INLINE =
            CompilerConfigurationKey.create("disable inline");
    public static final CompilerConfigurationKey<Boolean> DISABLE_OPTIMIZATION =
            CompilerConfigurationKey.create("disable optimization");

    public static final CompilerConfigurationKey<IncrementalCacheProvider> INCREMENTAL_CACHE_PROVIDER =
            CompilerConfigurationKey.create("incremental cache provider");

    public static final CompilerConfigurationKey<List<String>> MODULE_IDS =
            CompilerConfigurationKey.create("module id strings");

    public static final CompilerConfigurationKey<String> ANDROID_RES_PATH =
            CompilerConfigurationKey.create("android resources search path");
}
