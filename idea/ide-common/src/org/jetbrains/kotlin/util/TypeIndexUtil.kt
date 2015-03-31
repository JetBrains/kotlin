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

package org.jetbrains.kotlin.util

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.psi.JetUserType

public fun JetFile.aliasImportMap(): Multimap<String, String> {
    val cached = getUserData(ALIAS_IMPORT_DATA_KEY)
    val modificationStamp = getModificationStamp()
    if (cached != null && modificationStamp == cached.fileModificationStamp) {
        return cached.map
    }

    val data = CachedAliasImportData(buildAliasImportMap(), modificationStamp)
    putUserData(ALIAS_IMPORT_DATA_KEY, cached)
    return data.map
}

private fun JetFile.buildAliasImportMap(): Multimap<String, String> {
    val map = HashMultimap.create<String, String>()
    val importList = getImportList() ?: return map
    for (import in importList.getImports()) {
        val aliasName = import.getAliasName() ?: continue
        val name = import.getImportPath()?.fqnPart()?.shortName()?.asString() ?: continue
        map.put(aliasName, name)
    }
    return map
}

private class CachedAliasImportData(val map: Multimap<String, String>, val fileModificationStamp: Long)

private val ALIAS_IMPORT_DATA_KEY = Key<CachedAliasImportData>("ALIAS_IMPORT_MAP_KEY")

public fun JetTypeReference?.isProbablyNothing(): Boolean {
    val userType = this?.getTypeElement() as? JetUserType ?: return false
    return userType.isProbablyNothing()
}

public fun JetUserType?.isProbablyNothing(): Boolean
        = this?.getReferencedName() == "Nothing"

