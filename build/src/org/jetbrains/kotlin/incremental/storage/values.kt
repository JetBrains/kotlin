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

package org.jetbrains.kotlin.incremental.storage

data class LookupSymbolKey(val nameHash: Int, val scopeHash: Int) : Comparable<LookupSymbolKey> {
    constructor(name: String, scope: String) : this(name.hashCode(), scope.hashCode())

    override fun compareTo(other: LookupSymbolKey): Int {
        val nameCmp = nameHash.compareTo(other.nameHash)

        if (nameCmp != 0) return nameCmp

        return scopeHash.compareTo(other.scopeHash)
    }
}

data class ProtoMapValue(val isPackageFacade: Boolean, val bytes: ByteArray, val strings: Array<String>)
