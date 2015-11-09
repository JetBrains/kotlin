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

package org.jetbrains.kotlin.cli.common.modules

import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.JavaRootPath
import java.util.*

public class ModuleBuilder(
        private val name: String,
        private val outputDir: String,
        private val type: String
) : Module {
    private val sourceFiles = ArrayList<String>()
    private val classpathRoots = ArrayList<String>()
    private val javaSourceRoots = ArrayList<JavaRootPath>()
    private val friendDirs = ArrayList<String>()

    public fun addSourceFiles(pattern: String) {
        sourceFiles.add(pattern)
    }

    public fun addClasspathEntry(name: String) {
        classpathRoots.add(name)
    }

    public fun addJavaSourceRoot(rootPath: JavaRootPath) {
        javaSourceRoots.add(rootPath)
    }

    public fun addFriendDir(friendDir: String) {
        friendDirs.add(friendDir)
    }

    override fun getOutputDirectory(): String = outputDir
    override fun getFriendPaths(): List<String> = friendDirs
    override fun getJavaSourceRoots(): List<JavaRootPath> = javaSourceRoots
    override fun getSourceFiles(): List<String> = sourceFiles
    override fun getClasspathRoots(): List<String> = classpathRoots
    override fun getModuleName(): String = name
    override fun getModuleType(): String = type
}
