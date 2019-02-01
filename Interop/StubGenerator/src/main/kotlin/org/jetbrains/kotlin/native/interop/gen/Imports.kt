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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

interface Imports {
    fun getPackage(location: Location): String?
}

class ImportsImpl(internal val headerIdToPackage: Map<HeaderId, String>) : Imports {

    override fun getPackage(location: Location): String? =
            headerIdToPackage[location.headerId]

}

class HeaderInclusionPolicyImpl(private val nameGlobs: List<String>) : HeaderInclusionPolicy {

    override fun excludeUnused(headerName: String?): Boolean {
        if (nameGlobs.isEmpty()) {
            return false
        }

        if (headerName == null) {
            // Builtins; included only if no globs are specified:
            return true
        }

        return nameGlobs.all { !headerName.matchesToGlob(it) }
    }
}

class HeaderExclusionPolicyImpl(
        private val importsImpl: ImportsImpl
) : HeaderExclusionPolicy {

    override fun excludeAll(headerId: HeaderId): Boolean {
        return headerId in importsImpl.headerIdToPackage
    }

}

private fun String.matchesToGlob(glob: String): Boolean =
        java.nio.file.FileSystems.getDefault()
                .getPathMatcher("glob:$glob").matches(java.nio.file.Paths.get(this))
