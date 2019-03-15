/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile

interface CollectAdditionalSourcesExtension {
    companion object : ProjectExtensionDescriptor<CollectAdditionalSourcesExtension>(
        "org.jetbrains.kotlin.collectAdditionalSourcesExtension",
        CollectAdditionalSourcesExtension::class.java
    )

    fun collectAdditionalSourcesAndUpdateConfiguration(
        knownSources: Collection<KtFile>,
        configuration: CompilerConfiguration,
        project: Project
    ): Collection<KtFile>
}
