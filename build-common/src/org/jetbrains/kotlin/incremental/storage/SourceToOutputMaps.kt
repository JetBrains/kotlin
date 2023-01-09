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

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.dumpCollection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

internal class SourceToJvmNameMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractSourceToOutputMap<JvmClassName>(JvmClassNameTransformer, storageFile, icContext)

internal class SourceToFqNameMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractSourceToOutputMap<FqName>(FqNameTransformer, storageFile, icContext)

internal abstract class AbstractSourceToOutputMap<Name>(
    private val nameTransformer: NameTransformer<Name>,
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer, icContext) {
    fun clearOutputsForSource(sourceFile: File) {
        remove(pathConverter.toPath(sourceFile))
    }

    fun add(sourceFile: File, className: Name) {
        storage.append(pathConverter.toPath(sourceFile), listOf(nameTransformer.asString(className)))
    }

    fun contains(sourceFile: File): Boolean =
        pathConverter.toPath(sourceFile) in storage

    operator fun get(sourceFile: File): Collection<Name> =
        storage[pathConverter.toPath(sourceFile)].orEmpty().map(nameTransformer::asName)

    fun getFqNames(sourceFile: File): Collection<FqName> =
        storage[pathConverter.toPath(sourceFile)].orEmpty().map(nameTransformer::asFqName)

    override fun dumpValue(value: Collection<String>) =
        value.dumpCollection()

    private fun remove(path: String) {
        storage.remove(path)
    }
}