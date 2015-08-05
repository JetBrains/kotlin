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

package org.jetbrains.kotlin.resolve.lazy

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.load.java.lazy.PackageMappingProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping

public class JvmPackageMappingProvider(val env: KotlinCoreEnvironment) : PackageMappingProvider {
    override fun findPackageMembers(packageName: String): List<String> {
        val res = env.configuration.getList(CommonConfigurationKeys.CONTENT_ROOTS).
                filterIsInstance<JvmClasspathRoot>().
                map {
                    env.contentRootToVirtualFile(it);
                }.filterNotNull()

        //TODO additional filtering by package existing
        //val path = packageName.split("/")

        val mappings = res.map {
            it.findChild("META-INF")
        }.filterNotNull().flatMap {
            it.children.filter { it.name.endsWith(ModuleMapping.MAPPING_FILE_EXT) }.toList<VirtualFile>()
        }.map {
            ModuleMapping(String(it.contentsToByteArray(), "UTF-8"))
        }

        return mappings.map { it.findPackageParts(packageName) }.filterNotNull().flatMap { it.parts }.distinct()
    }
}