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

package org.jetbrains.kotlin.load.kotlin.reflect

import org.jetbrains.kotlin.load.java.lazy.PackageMappingProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.PackageFacades
import java.io.ByteArrayOutputStream

class RuntimePackageMappingProvider(val moduleName: String, val classLoader : ClassLoader) : PackageMappingProvider {

    val mapping: ModuleMapping by lazy {
        print("finding metainf for $moduleName")
        val resourceAsStream = classLoader.getResourceAsStream("META-INF/$moduleName.kotlin_module") ?: return@lazy ModuleMapping("")

        print("OK")

        try {
            val out = ByteArrayOutputStream(4096);
            val buffer = ByteArray(4096);
            while (true) {
                val r = resourceAsStream.read(buffer);
                if (r == -1) break;
                out.write(buffer, 0, r);
            }

            val ret = out.toByteArray();
            return@lazy ModuleMapping(String(ret, "UTF-8"))
        } finally {
            resourceAsStream.close()
        }
    }

    override fun findPackageMembers(packageName: String): List<String> {
        print("finding $packageName")
        return mapping.package2MiniFacades.getOrElse (packageName, { PackageFacades("default") }).parts.toList()
    }
}