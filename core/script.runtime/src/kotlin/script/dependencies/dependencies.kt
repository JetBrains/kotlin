/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

@file:Suppress("unused")

package kotlin.script.dependencies

import java.io.File

// discuss

// Currently File is used to disambiguate between plain strings and paths
// One of the ideas is to use java.nio.Path but this requires JDK 7
// that means that script templates would require higher JDK (but since script are run by calling koltinc it seems ok to me after consideration)
// Andrey expressed the idea that File (or Path) does not support other protocols, should we use URL/URI? (is it viable to support non-file protocols in compiler and IDE?)
//
// Explicitly references javaHome, what if it's not a jvm targeted script?
// Currently it's convenient for IDE code and for the user because including jdk classes in classpath is a mess
data class ScriptDependencies(
        val javaHome: File? = null,
        val classpath: List<File> = emptyList(),
        val imports: List<String> = emptyList(),
        val sources: List<File> = emptyList(),
        val scripts: List<File> = emptyList()
) {
    companion object {
        val Empty = ScriptDependencies()
    }
}