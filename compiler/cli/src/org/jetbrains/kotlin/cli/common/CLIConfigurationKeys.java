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

package org.jetbrains.kotlin.cli.common;

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig;
import org.jetbrains.kotlin.cli.common.config.ContentRoot;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.config.CompilerConfigurationKey;

import java.io.File;
import java.util.List;

public class CLIConfigurationKeys {
    // Roots, including dependencies and own sources
    public static final CompilerConfigurationKey<List<ContentRoot>> CONTENT_ROOTS =
            CompilerConfigurationKey.create("content roots");

    public static final CompilerConfigurationKey<MessageCollector> MESSAGE_COLLECTOR_KEY =
            CompilerConfigurationKey.create("message collector");
    public static final CompilerConfigurationKey<Boolean> ALLOW_KOTLIN_PACKAGE =
            CompilerConfigurationKey.create("allow kotlin package");
    public static final CompilerConfigurationKey<CommonCompilerPerformanceManager> PERF_MANAGER =
            CompilerConfigurationKey.create("performance manager");

    // Used in Eclipse plugin (see KotlinCLICompiler)
    public static final CompilerConfigurationKey<String> INTELLIJ_PLUGIN_ROOT =
            CompilerConfigurationKey.create("intellij plugin root");

    // See K2MetadataCompilerArguments

    public static final CompilerConfigurationKey<File> METADATA_DESTINATION_DIRECTORY =
            CompilerConfigurationKey.create("metadata destination directory");

    public static final CompilerConfigurationKey<PhaseConfig> PHASE_CONFIG =
            CompilerConfigurationKey.create("phase configuration");

    private CLIConfigurationKeys() {
    }
}
