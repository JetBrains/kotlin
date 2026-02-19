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

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.name.FqName
import java.io.DataInput
import java.io.DataOutput
import java.io.File

internal open class ClassOneToManyMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AppendableSetBasicMap<FqName, FqName>(
    storageFile,
    LegacyFqNameExternalizer.toDescriptor(),
    LegacyFqNameExternalizer,
    icContext
) {

    @Synchronized
    override operator fun set(key: FqName, value: Set<FqName>) {
        if (value.isNotEmpty()) {
            super.set(key, value)
        } else {
            remove(key)
        }
    }

    @Synchronized
    fun removeValues(key: FqName, removed: Set<FqName>) {
        this[key] = this[key].orEmpty() - removed
    }

}

internal class SubtypesMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : ClassOneToManyMap(storageFile, icContext)

internal class SupertypesMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : ClassOneToManyMap(storageFile, icContext)

/**
 * Use [LegacyFqNameExternalizer] instead of [FqNameExternalizer] for [SubtypesMap] for now because they internally use different types of
 * `DataExternalizer<String>`, and the `compiler-reference-index` module in the Kotlin IDEA plugin currently can only read the old data
 * format (see KTIJ-27258).
 *
 * Once we fix that bug, we can remove this class and use [FqNameExternalizer].
 */
private object LegacyFqNameExternalizer : DataExternalizer<FqName> {

    override fun save(output: DataOutput, fqName: FqName) {
        EnumeratorStringDescriptor.INSTANCE.save(output, fqName.asString())
    }

    override fun read(input: DataInput): FqName {
        return FqName(EnumeratorStringDescriptor.INSTANCE.read(input))
    }

}
