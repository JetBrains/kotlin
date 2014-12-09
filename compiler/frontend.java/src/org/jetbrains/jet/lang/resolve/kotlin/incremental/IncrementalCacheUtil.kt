/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.kotlin.incremental

import org.jetbrains.jet.lang.resolve.kotlin.incremental.cache.IncrementalCache
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.JvmClassName
import java.util.HashMap
import java.io.File
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils

public fun IncrementalCache.getPackagesWithRemovedFiles(sourceFilesToCompile: Collection<JetFile>): Collection<FqName> {
    return getRemovedPackageParts(sourceFilesToCompile).map { it.getPackageFqName() }
}

public fun IncrementalCache.getRemovedPackageParts(sourceFilesToCompile: Collection<JetFile>): Collection<JvmClassName> {
    val sourceFilesToFqName = HashMap<File, String?>()
    for (sourceFile in sourceFilesToCompile) {
        sourceFilesToFqName[File(sourceFile.getVirtualFile()!!.getPath())] =
                if (PackagePartClassUtils.fileHasCallables(sourceFile))
                    sourceFile.getPackageFqName().asString()
                else
                    null
    }

    return getRemovedPackageParts(sourceFilesToFqName).map { JvmClassName.byInternalName(it) }
}

public fun IncrementalCache.getPackageData(fqName: FqName): ByteArray? {
    return getPackageData(fqName.asString())
}
