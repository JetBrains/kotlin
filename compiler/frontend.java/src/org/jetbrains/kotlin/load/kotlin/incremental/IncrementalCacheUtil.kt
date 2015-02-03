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

package org.jetbrains.kotlin.load.kotlin.incremental

import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCache
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.HashMap
import java.io.File
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils

// TODO JetFiles are redundant
public fun IncrementalCache.getPackagesWithObsoleteParts(sourceFilesToCompile: Collection<JetFile>): Collection<FqName> {
    return getObsoletePackageParts(sourceFilesToCompile).map { it.getPackageFqName() }
}

public fun IncrementalCache.getObsoletePackageParts(sourceFilesToCompile: Collection<JetFile>): Collection<JvmClassName> {
    return getObsoletePackageParts(sourceFilesToCompile.map { File(it.getVirtualFile()!!.getPath()) }).map { JvmClassName.byInternalName(it) }
}

public fun IncrementalCache.getPackageData(fqName: FqName): ByteArray? {
    return getPackageData(fqName.asString())
}
