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

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.container.DefaultImplementation
import java.io.Serializable

@DefaultImplementation(LookupTracker.DO_NOTHING::class)
interface LookupTracker {
    // used in tests for more accurate checks
    val requiresPosition: Boolean

    fun record(
            filePath: String,
            position: Position,
            scopeFqName: String,
            scopeKind: ScopeKind,
            name: String
    )

    object DO_NOTHING : LookupTracker {
        override val requiresPosition: Boolean
            get() = false

        override fun record(filePath: String, position: Position, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        }
    }
}

enum class ScopeKind {
    PACKAGE,
    CLASSIFIER
}

data class LookupInfo(
        val filePath: String,
        val position: Position,
        val scopeFqName: String,
        val scopeKind: ScopeKind,
        val name: String
) : Serializable
