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

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.DataInput
import java.io.DataOutput
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
) : AppendableSetBasicMap<File, Name>(
    storageFile,
    icContext.fileDescriptorForSourceFiles,
    NameExternalizer(nameTransformer),
    icContext
) {

    @Synchronized
    fun getFqNames(sourceFile: File): Collection<FqName>? =
        this[sourceFile]?.map { nameTransformer.asFqName(nameTransformer.asString(it)) }

}

private class NameExternalizer<Name>(private val nameTransformer: NameTransformer<Name>) : DataExternalizer<Name> {

    override fun save(output: DataOutput, name: Name) {
        StringExternalizer.save(output, nameTransformer.asString(name))
    }

    override fun read(input: DataInput): Name {
        return nameTransformer.asName(StringExternalizer.read(input))
    }

}