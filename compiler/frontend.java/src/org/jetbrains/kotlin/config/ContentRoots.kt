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

package org.jetbrains.kotlin.config

interface ContentRoot

data class KotlinSourceRoot(val path: String): ContentRoot

fun CompilerConfiguration.addKotlinSourceRoot(source: String) {
    add(JVMConfigurationKeys.CONTENT_ROOTS, KotlinSourceRoot(source))
}

fun CompilerConfiguration.addKotlinSourceRoots(sources: List<String>): Unit =
        sources.forEach(this::addKotlinSourceRoot)

val CompilerConfiguration.kotlinSourceRoots: List<String>
    get() = get(JVMConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<KotlinSourceRoot>()?.map { it.path }.orEmpty()
