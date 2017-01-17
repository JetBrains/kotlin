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

package org.jetbrains.kotlin.idea.core

import com.intellij.codeInsight.JavaProjectCodeInsightSettings
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName


private val exclusions =
        listOf(
                "kotlin.jvm.internal",
                "kotlin.coroutines.intrinsics"
        )

private fun shouldBeHiddenAsInternalImplementationDetail(fqName: String) = exclusions.any { fqName.startsWith(it) }

fun DeclarationDescriptor.isExcludedFromAutoImport(project: Project): Boolean {
    val fqName = importableFqName?.asString() ?: return false
    return JavaProjectCodeInsightSettings.getSettings(project).isExcluded(fqName) ||
           shouldBeHiddenAsInternalImplementationDetail(fqName)
}
