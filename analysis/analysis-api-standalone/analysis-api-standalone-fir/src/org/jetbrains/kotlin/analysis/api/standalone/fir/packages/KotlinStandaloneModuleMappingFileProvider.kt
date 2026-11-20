/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.packages

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinModuleMappingFileProvider
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping

@KaImplementationDetail
class KotlinStandaloneModuleMappingFileProvider(binaryRoots: Collection<VirtualFile>) : KotlinModuleMappingFileProvider {
    private val binaryRoots: Set<VirtualFile> = binaryRoots.toSet()

    override fun findModuleMappingFiles(scope: GlobalSearchScope): Collection<VirtualFile> = buildList {
        for (root in binaryRoots) {
            if (root !in scope) continue

            val metaInf = root.findChild("META-INF") ?: continue
            metaInf.children.filterTo(this) { it.name.endsWith(ModuleMapping.MAPPING_FILE_EXT) }
        }
    }
}
