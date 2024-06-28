/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.project.structure

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.withDanglingFileResolutionMode
import org.jetbrains.kotlin.psi.KtFile

@Deprecated(
    "Use 'org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode' instead.",
    ReplaceWith(
        "KaDanglingFileResolutionMode",
        imports = ["org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode"],
    ),
)
public typealias DanglingFileResolutionMode = KaDanglingFileResolutionMode

@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.isDangling' instead.")
public val KtFile.isDangling: Boolean
    get() = isDanglingFile(this)

@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.danglingFileResolutionMode' instead.")
public val KtFile.danglingFileResolutionMode: DanglingFileResolutionMode?
    get() = getDanglingFileResolutionMode(this)

// Try to preserve binary compatibility of code which has inlined `analyzeCopy`.
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.withDanglingFileResolutionMode' instead.")
@KaAnalysisApiInternals
public fun <R> withDanglingFileResolutionMode(file: KtFile, mode: DanglingFileResolutionMode, action: () -> R): R =
    withDanglingFileResolutionMode(file, mode) { action() }
