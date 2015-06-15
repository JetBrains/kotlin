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

package org.jetbrains.kotlin.idea.versions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.ExternalIntegerKeyDescriptor

/**
 * Important! This is not a stub-based index. And it has its own version
 */
abstract class KotlinAbiVersionIndexBase<T>(private val classOfIndex: Class<T>) : ScalarIndexExtension<Int>() {

    override fun getName() = ID.create<Int, Void>(classOfIndex.getCanonicalName())

    override fun getKeyDescriptor() = ExternalIntegerKeyDescriptor()

    override fun dependsOnFileContent() = true

    protected val LOG: Logger = Logger.getInstance(classOfIndex)

    protected inline fun tryBlock(inputData: FileContent, body: () -> Unit) {
        try {
            body()
        }
        catch (e: Throwable) {
            LOG.warn("Could not index ABI version for file " + inputData.getFile() + ": " + e.getMessage())
        }
    }
}
