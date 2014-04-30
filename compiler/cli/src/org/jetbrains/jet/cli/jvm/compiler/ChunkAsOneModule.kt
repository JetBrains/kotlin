/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.jvm.compiler

import kotlin.modules.Module

class ChunkAsOneModule(private val chunk: ModuleChunk) : Module {
    override fun getModuleName(): String = "chunk" + chunk.getModules().map { it.getModuleName() }.toString()

    override fun getOutputDirectory(): String {
        throw UnsupportedOperationException("Each module in a chunk has its own output directory")
    }
    override fun getSourceFiles(): List<String> = chunk.getModules().flatMap { it.getSourceFiles() }

    override fun getClasspathRoots(): List<String> = chunk.getModules().flatMap { it.getClasspathRoots() }

    override fun getAnnotationsRoots(): List<String> = chunk.getModules().flatMap { it.getAnnotationsRoots() }

}