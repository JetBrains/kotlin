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

import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.JvmClassName
import org.jetbrains.jet.lang.psi.JetFile
import java.util.HashMap
import java.io.File

public trait IncrementalCache {
    public fun getRemovedPackageParts(moduleId: String, compiledSourceFilesToFqName: Map<File, String>): Collection<String>

    public fun getPackageData(moduleId: String, fqName: String): ByteArray?

    public fun close()
}

public fun IncrementalCache.getPackagesWithRemovedFiles(moduleId: String, compiledSourceFiles: Collection<JetFile>): Collection<FqName> {
    return getRemovedPackageParts(moduleId, compiledSourceFiles).map { it.getFqNameForClassNameWithoutDollars().parent() }
}

public fun IncrementalCache.getRemovedPackageParts(moduleId: String, compiledSourceFiles: Collection<JetFile>): Collection<JvmClassName> {
    val compiledSourceFilesToFqName = HashMap<File, String>()
    for (sourceFile in compiledSourceFiles) {
        compiledSourceFilesToFqName[File(sourceFile.getVirtualFile()!!.getPath())] = sourceFile.getPackageFqName().asString()
    }

    return getRemovedPackageParts(moduleId, compiledSourceFilesToFqName).map { JvmClassName.byInternalName(it) }
}

public fun IncrementalCache.getPackageData(moduleId: String, fqName: FqName): ByteArray? {
    return getPackageData(moduleId, fqName.asString())
}
