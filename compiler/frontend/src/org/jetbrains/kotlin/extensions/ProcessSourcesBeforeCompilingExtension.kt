/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile

interface ProcessSourcesBeforeCompilingExtension {

    companion object : ProjectExtensionDescriptor<ProcessSourcesBeforeCompilingExtension>(
        "org.jetbrains.kotlin.processSourcesBeforeCompilingExtension",
        ProcessSourcesBeforeCompilingExtension::class.java
    )

    fun processSources(
        sources: Collection<KtFile>,
        configuration: CompilerConfiguration
    ): Collection<KtFile>
}