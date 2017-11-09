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

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @deprecated use the -Xintellij-plugin-root command line argument instead
 */
// TODO: drop as soon as Eclipse plugin starts using the new API
@Deprecated
public interface CompilerJarLocator {
    @NotNull
    File getCompilerJar();
}
