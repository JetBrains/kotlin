/**
 * Copyright 2010-2019 JetBrains s.r.o.
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

package org.jetbrains.kotlin.library

fun String.parseKotlinAbiVersion(): KotlinAbiVersion {
    val values = this.split(".").map { it.toInt() }

    return when (values.size) {
        3 -> KotlinAbiVersion(values[0], values[1], values[2])
        1 -> KotlinAbiVersion(values[0])
        else -> error("Could not parse abi version: $this")
    }
}

// We still enumerate klib abi_version with a single number,
// but we don't break if in the future we switch to triples.
data class KotlinAbiVersion(val major: Int, val minor: Int, val patch: Int) {
    constructor(single: Int) : this(0, single, 0)
    companion object {
        val CURRENT = KotlinAbiVersion(26)
    }

    override fun toString() = if (major == 0 && patch == 0) "$minor" else "$major.$minor.$patch"
}