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

import com.intellij.util.io.BooleanDataDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

internal class DirtyClassesJvmNameMap(storageFile: File) : AbstractDirtyClassesMap<JvmClassName>(JvmClassNameTransformer, storageFile)
internal class DirtyClassesFqNameMap(storageFile: File) : AbstractDirtyClassesMap<FqName>(FqNameTransformer, storageFile)

internal abstract class AbstractDirtyClassesMap<Name>(
    private val nameTransformer: NameTransformer<Name>, storageFile: File
) : BasicStringMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE) {
    fun markDirty(className: Name) {
        storage[nameTransformer.asString(className)] = true
    }

    fun notDirty(className: Name) {
        storage.remove(nameTransformer.asString(className))
    }

    fun getDirtyOutputClasses(): Collection<Name> =
        storage.keys.map { nameTransformer.asName(it) }

    fun isDirty(className: Name): Boolean =
        storage.contains(nameTransformer.asString(className))

    override fun dumpValue(value: Boolean) = ""
}
