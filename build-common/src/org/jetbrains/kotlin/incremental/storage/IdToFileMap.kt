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

import com.intellij.util.io.ExternalIntegerKeyDescriptor
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import java.io.File

class IdToFileMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : LazyStorageWrapper<Int, File, Int, String>(
    storage = createLazyStorage(storageFile, ExternalIntegerKeyDescriptor.INSTANCE, FilePathDescriptor, icContext),
    publicToInternalKey = { it },
    internalToPublicKey = { it },
    publicToInternalValue = icContext.pathConverterForSourceFiles::toPath,
    internalToPublicValue = icContext.pathConverterForSourceFiles::toFile,
), BasicMap<Int, File>
