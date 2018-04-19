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

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.dumpCollection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

internal class SourceToJvmNameMap(storageFile: File) : AbstractSourceToOutputMap<JvmClassName>(JvmClassNameTransformer, storageFile)
internal class SourceToFqNameMap(storageFile: File) : AbstractSourceToOutputMap<FqName>(FqNameTransformer, storageFile)

internal abstract class AbstractSourceToOutputMap<Name>(
        private val nameTransformer: NameTransformer<Name>,
        storageFile: File
) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {
    fun clearOutputsForSource(sourceFile: File) {
        remove(sourceFile.absolutePath)
    }

    fun add(sourceFile: File, className: Name) {
        storage.append(sourceFile.absolutePath, nameTransformer.asString(className))
    }

    fun contains(sourceFile: File): Boolean =
            sourceFile.absolutePath in storage

    operator fun get(sourceFile: File): Collection<Name> =
            storage[sourceFile.absolutePath].orEmpty().map(nameTransformer::asName)

    fun getFqNames(sourceFile: File): Collection<FqName> =
        storage[sourceFile.absolutePath].orEmpty().map(nameTransformer::asFqName)

    override fun dumpValue(value: Collection<String>) =
            value.dumpCollection()

    private fun remove(path: String) {
        storage.remove(path)
    }
}