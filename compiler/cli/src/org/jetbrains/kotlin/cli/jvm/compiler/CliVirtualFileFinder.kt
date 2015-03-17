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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClassFinder
import org.jetbrains.kotlin.name.ClassId

public class CliVirtualFileFinder(private val index: JvmDependenciesIndex) : VirtualFileKotlinClassFinder(), VirtualFileFinder {

    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? {
        val classFileName = classId.getRelativeClassName().asString().replace('.', '$')
        return index.findClass(classId, acceptedRootTypes = JavaRoot.OnlyBinary) { dir, _ ->
            dir.findChild("$classFileName.class")?.let {
                if (it.isValid()) it else null
            }
        }
    }
}
