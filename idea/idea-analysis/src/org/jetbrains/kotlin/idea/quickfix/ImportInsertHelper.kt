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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade

public trait ImportInsertHelper {

    public fun addImportDirectiveIfNeeded(importFqn: FqName, file: JetFile)

    public fun optimizeImportsOnTheFly(file: JetFile): Boolean

    public fun isImportedWithDefault(importPath: ImportPath, contextFile: JetFile): Boolean

    public fun needImport(fqName: FqName, file: JetFile): Boolean = needImport(ImportPath(fqName, false), file)

    public fun needImport(importPath: ImportPath, file: JetFile, importDirectives: List<JetImportDirective> = file.getImportDirectives()): Boolean

    public fun writeImportToFile(importPath: ImportPath, file: JetFile): JetImportDirective

    /**
     * Returns true, if the descriptor is imported (even if no import was added because it's not needed)
     * and false, if importing of this descriptor is either impossible or not allowed by code style.
     */
    public fun importDescriptor(file: JetFile, descriptor: DeclarationDescriptor): Boolean

    class object {
        public val INSTANCE: ImportInsertHelper
            get() = ServiceManager.getService<ImportInsertHelper>(javaClass<ImportInsertHelper>())
    }
}
