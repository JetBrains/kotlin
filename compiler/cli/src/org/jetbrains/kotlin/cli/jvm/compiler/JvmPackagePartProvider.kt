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
import com.intellij.util.SmartList
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import java.io.EOFException

class JvmPackagePartProvider(val env: KotlinCoreEnvironment) : PackagePartProvider {
    private val notLoadedRoots by lazy(LazyThreadSafetyMode.NONE) {
        env.configuration.getList(CommonConfigurationKeys.CONTENT_ROOTS).
                filterIsInstance<JvmClasspathRoot>().
                mapNotNull {
                    env.contentRootToVirtualFile(it);
                }.filter { it.findChild("META-INF") != null }.toMutableList()
    }

    private val loadedModules: MutableList<ModuleMapping> = SmartList()

    @Synchronized
    override fun findPackageParts(packageFqName: String): List<String> {
        processNotLoadedRelevantRoots(packageFqName)

        return loadedModules.flatMap { it.findPackageParts(packageFqName)?.parts ?: emptySet<String>() }.distinct()
    }

    private fun processNotLoadedRelevantRoots(packageFqName: String) {
        if (notLoadedRoots.isEmpty()) return

        val pathParts = packageFqName.split('.')

        val relevantRoots = notLoadedRoots.filter {
            //filter all roots by package path existing
            pathParts.fold(it) {
                parent, part ->
                if (part.isEmpty()) parent
                else  parent.findChild(part) ?: return@filter false
            }
            true
        }
        notLoadedRoots.removeAll(relevantRoots)

        loadedModules.addAll(relevantRoots.mapNotNull {
            it.findChild("META-INF")
        }.flatMap {
            it.children.filter<VirtualFile> { it.name.endsWith(ModuleMapping.MAPPING_FILE_EXT) }
        }.map {
            try {
                ModuleMapping.create(it.contentsToByteArray())
            } catch (e: EOFException) {
                throw RuntimeException("Error on reading package parts for '$packageFqName' package in '$it', roots: $notLoadedRoots", e)
            }
        })
    }
}