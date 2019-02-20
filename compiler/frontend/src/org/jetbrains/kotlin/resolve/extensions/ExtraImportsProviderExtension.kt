/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportInfo

interface ExtraImportsProviderExtension {
    companion object : ProjectExtensionDescriptor<ExtraImportsProviderExtension>(
        "org.jetbrains.kotlin.extraImportsProviderExtension", ExtraImportsProviderExtension::class.java
    ) {

        private class CompoundExtraImportsProviderExtension(val instances: List<ExtraImportsProviderExtension>) : ExtraImportsProviderExtension {
            override fun getExtraImports(ktFile: KtFile): Collection<KtImportInfo> = instances.flatMap {
                withLinkageErrorLogger(it) { getExtraImports(ktFile) }
            }
        }

        fun getInstance(project: Project): ExtraImportsProviderExtension {
            val instances = getInstances(project)
            return instances.singleOrNull() ?: CompoundExtraImportsProviderExtension(instances)
        }
    }

    fun getExtraImports(ktFile: KtFile): Collection<KtImportInfo>
}