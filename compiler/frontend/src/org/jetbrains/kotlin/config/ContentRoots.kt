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

public trait ContentRoot

public data class KotlinSourceRoot(public val path: String): ContentRoot

public fun CompilerConfiguration.addKotlinSourceRoot(source: String) {
    add(CommonConfigurationKeys.CONTENT_ROOTS, KotlinSourceRoot(source))
}

public fun CompilerConfiguration.addKotlinSourceRoots(sources: List<String>): Unit = sources.forEach { addKotlinSourceRoot(it) }

