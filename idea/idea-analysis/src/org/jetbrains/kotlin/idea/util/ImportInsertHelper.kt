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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.ServiceManager
import kotlin.platform.platformStatic
import java.util.*

public abstract class ImportInsertHelper {

    public abstract fun optimizeImportsOnTheFly(file: JetFile): Boolean

    /*TODO: implementation is not quite correct*/
    public abstract fun isImportedWithDefault(importPath: ImportPath, contextFile: JetFile): Boolean

    public abstract fun mayImportByCodeStyle(descriptor: DeclarationDescriptor): Boolean

    public abstract val importSortComparator: Comparator<ImportPath>

    public enum class ImportDescriptorResult {
        FAIL
        IMPORT_ADDED
        ALREADY_IMPORTED
    }

    public abstract fun importDescriptor(file: JetFile, descriptor: DeclarationDescriptor): ImportDescriptorResult

    default object {
        [platformStatic]
        public fun getInstance(project: Project): ImportInsertHelper
            = ServiceManager.getService<ImportInsertHelper>(project, javaClass<ImportInsertHelper>())
    }
}
