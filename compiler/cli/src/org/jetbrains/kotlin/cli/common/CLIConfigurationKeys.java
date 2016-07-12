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

import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.CompilerJarLocator;
import org.jetbrains.kotlin.config.CompilerConfigurationKey;

public class CLIConfigurationKeys {
    public static final CompilerConfigurationKey<MessageCollector> MESSAGE_COLLECTOR_KEY =
            CompilerConfigurationKey.create("message collector");
    public static final CompilerConfigurationKey<Boolean> ALLOW_KOTLIN_PACKAGE =
            CompilerConfigurationKey.create("allow kotlin package");
    public static final CompilerConfigurationKey<Boolean> REPORT_PERF =
            CompilerConfigurationKey.create("report performance information");
    public static final CompilerConfigurationKey<CompilerJarLocator> COMPILER_JAR_LOCATOR =
            CompilerConfigurationKey.create("compiler jar locator");

    private CLIConfigurationKeys() {
    }
}
