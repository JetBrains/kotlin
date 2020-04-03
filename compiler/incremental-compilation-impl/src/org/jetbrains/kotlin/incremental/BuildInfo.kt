/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental

import java.io.*

data class BuildInfo(val startTS: Long) : Serializable {
    companion object {
        fun read(file: File): BuildInfo? =
            try {
                ObjectInputStream(FileInputStream(file)).use {
                    it.readObject() as BuildInfo
                }
            } catch (e: Exception) {
                null
            }

        fun write(buildInfo: BuildInfo, file: File) {
            ObjectOutputStream(FileOutputStream(file)).use {
                it.writeObject(buildInfo)
            }
        }
    }
}