/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals

@LLFirInternals
interface LLFirSessionConfigurator {
    @LLFirInternals
    companion object {
        const val name = "org.jetbrains.kotlin.llFirSessionConfigurator"
        val extensionPointName: ExtensionPointName<LLFirSessionConfigurator> = ExtensionPointName.create(name)

        @TestOnly
        fun registerExtension(project: Project, extension: LLFirSessionConfigurator) {
            val extensionPoint = project.extensionArea.getExtensionPoint(extensionPointName)
            if (extension !in extensionPoint.extensions) {
                extensionPoint.registerExtension(extension, project)
            }
        }
    }

    fun configure(session: LLFirSession)
}
