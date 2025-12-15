/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

abstract class FirProcessSourcesBeforeCompilingExtension {
    companion object : ProjectExtensionDescriptor<FirProcessSourcesBeforeCompilingExtension>(
        "org.jetbrains.kotlin.fir.firProcessSourcesBeforeCompilingExtension",
        FirProcessSourcesBeforeCompilingExtension::class.java
    ) {
        /**
         * Applies [FirProcessSourcesBeforeCompilingExtension] instances to a project
         * @param project the project that sources belong to
         * @param configuration compiler configuration
         * @param sources sources to process
         * @return null if no applicable extensions were found or no processing was performed, otherwise returns processed sources
         *
         * @see FirProcessSourcesBeforeCompilingExtension.isApplicable
         * @see FirProcessSourcesBeforeCompilingExtension.doProcessSources
         */
        fun processSources(
            project: Project,
            environment: Any, // TODO: actually the VfsBasedProjectEnvironment is needed here, so we need to refactor dependencies to allow it
            configuration: CompilerConfiguration,
            sources: Iterable<KtSourceFile>
        ): Iterable<KtSourceFile>? {
            val extensions = getInstances(project).filter { it.isApplicable(configuration) }
            return if (extensions.isEmpty()) sources
            else extensions.fold(sources) { res, ext -> ext.doProcessSources(environment, configuration, res) }
        }
    }

    /**
     * Checks whether [doProcessSources] should be called
     * @param configuration compiler configuration
     * @return true if [doProcessSources] should be called
     */
    abstract fun isApplicable(configuration: CompilerConfiguration): Boolean

    /**
     * Process sources potentially converting, adding, or removing initial ones
     * @param configuration compiler configuration
     * @param sources sources to process
     * @return null if no applicable extensions were found or no processing was performed, otherwise returns processed sources
     */
    abstract fun doProcessSources(
        environment: Any, // see the comment to [processSources] above
        configuration: CompilerConfiguration,
        sources: Iterable<KtSourceFile>,
    ): Iterable<KtSourceFile>
}
