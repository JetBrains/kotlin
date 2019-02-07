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

package org.jetbrains.kotlin.utils

import java.io.File

interface KotlinPaths {
    val homePath: File

    val libPath: File

    val stdlibPath: File

    val reflectPath: File

    val scriptRuntimePath: File

    val kotlinTestPath: File

    val stdlibSourcesPath: File

    val jsStdLibJarPath: File

    val jsStdLibSrcJarPath: File

    val jsKotlinTestJarPath: File

    val allOpenPluginJarPath: File

    val noArgPluginJarPath: File

    val samWithReceiverJarPath: File

    val trove4jJarPath: File

    val compilerClasspath: List<File>

    val compilerPath: File

}
