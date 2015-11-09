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

package org.jetbrains.kotlin.modules

public interface Module {
    public fun getModuleName(): String

    public fun getModuleType(): String

    public fun getOutputDirectory(): String

    public fun getFriendPaths(): List<String>

    public fun getSourceFiles(): List<String>

    public fun getClasspathRoots(): List<String>

    public fun getJavaSourceRoots(): List<JavaRootPath>
}

data class JavaRootPath(val path: String, val packagePrefix: String? = null)
