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

package org.jetbrains.kotlin.backend.konan.library

import org.jetbrains.kotlin.backend.konan.util.File
import org.jetbrains.kotlin.backend.konan.util.suffixIfNot
import org.jetbrains.kotlin.backend.konan.util.removeSuffixIfPresent

interface SearchPathResolver {
    val searchRoots: List<File>
    fun resolve(givenPath: String): File
}

class KonanLibrarySearchPathResolver(repositories: List<String>,
    val distributionKlib: String, val localKonanDir: String): SearchPathResolver {

    val localHead: File
        get() = File(localKonanDir).klib

    val distHead: File
        get() = File(distributionKlib)

    val currentDirHead: File
        get() = File.userDir

    private val repoRoots: List<File> by lazy {
        repositories.map{File(it).klib}
    }

    // This is the place where we specify the order of library search.
    override val searchRoots: List<File> by lazy {
        listOf(currentDirHead) + repoRoots + listOf(localHead, distHead)
    }

    private fun found(candidate: File): File? {
        val noSuffix = File(candidate.path.removeSuffixIfPresent(".klib"))
        val withSuffix = File(candidate.path.suffixIfNot(".klib"))
        if (withSuffix.exists) {
            // We always returing the name without suffix here.
            return noSuffix
        }
        if (noSuffix.exists) {
            return noSuffix
        }
        return null
    }

    override fun resolve(givenPath: String): File {
        val given = File(givenPath)
        if (given.isAbsolute) {
            found(given)?.apply{return this}
        } else {
            searchRoots.forEach{ 
                found(File(it, givenPath))?.apply{return this}
            }
        }
        error("Could not find \"$givenPath\" in any of the provided locations.")
    }

    private val File.klib 
        get() = File(this, "klib")
}

