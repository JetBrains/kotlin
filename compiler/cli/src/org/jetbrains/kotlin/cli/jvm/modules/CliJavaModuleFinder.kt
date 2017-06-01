/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.vfs.VirtualFileSystem
//import com.intellij.psi.PsiJavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo

internal class CliJavaModuleFinder(val jrtFileSystem: VirtualFileSystem?) : JavaModuleFinder {
    internal fun computeAllSystemModules(): Map<String, JavaModuleInfo> {
        return emptyMap()
/*
        return jrtFileSystem?.findFileByPath("/modules")?.children.orEmpty()
                .mapNotNull { root -> root.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE) }
                .mapNotNull((JavaModuleInfo)::read)
                .associateBy { moduleInfo -> moduleInfo.moduleName }
*/
    }

    override fun findModule(name: String): JavaModuleInfo? {
        /*
        val file = jrtFileSystem?.findFileByPath("/modules/$name/${PsiJavaModule.MODULE_INFO_CLS_FILE}")
        if (file != null) {
            val moduleInfo = JavaModuleInfo.read(file)
            if (moduleInfo != null) {
                return moduleInfo
            }
        }
        */
        return null
    }
}
