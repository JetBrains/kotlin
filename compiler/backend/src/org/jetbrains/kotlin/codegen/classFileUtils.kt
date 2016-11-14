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

package org.jetbrains.kotlin.codegen

import com.intellij.util.io.DataOutputStream
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.PackageParts
import org.jetbrains.kotlin.serialization.jvm.JvmPackageTable
import java.io.ByteArrayOutputStream

fun ClassFileFactory.getClassFiles(): Iterable<OutputFile> {
    return asList().filterClassFiles()
}

fun List<OutputFile>.filterClassFiles(): Iterable<OutputFile> {
    return filter { it.relativePath.endsWith(".class") }
}

fun Iterable<PackageParts>.addCompiledPartsAndSort(state: GenerationState): List<PackageParts> =
        addCompiledParts(state).sortedBy { it.packageFqName }

private fun Iterable<PackageParts>.addCompiledParts(state: GenerationState): List<PackageParts> {
    val incrementalCache = state.incrementalCacheForThisTarget ?: return this.toList()
    val moduleMappingData = incrementalCache.getModuleMappingData() ?: return this.toList()

    val mapping = ModuleMapping.create(moduleMappingData, "<incremental>")

    incrementalCache.getObsoletePackageParts().forEach {
        val i = it.lastIndexOf('/')
        val qualifier = if (i == -1) "" else it.substring(0, i).replace('/', '.')
        val name = it.substring(i + 1)
        mapping.findPackageParts(qualifier)?.run { parts.remove(name) }
    }

    return (this + mapping.packageFqName2Parts.values)
            .groupBy { it.packageFqName }
            .map {
                val (packageFqName, packageParts) = it
                val newPackageParts = PackageParts(packageFqName)
                packageParts.forEach { newPackageParts.parts.addAll(it.parts) }
                newPackageParts
            }
}

fun JvmPackageTable.PackageTable.Builder.serializeToByteArray(): ByteArray {
    val moduleMapping = ByteArrayOutputStream(4096)
    val out = DataOutputStream(moduleMapping)
    val version = JvmMetadataVersion.INSTANCE.toArray()
    out.writeInt(version.size)
    for (number in version) {
        out.writeInt(number)
    }
    build().writeTo(out)
    out.flush()
    return moduleMapping.toByteArray()
}
