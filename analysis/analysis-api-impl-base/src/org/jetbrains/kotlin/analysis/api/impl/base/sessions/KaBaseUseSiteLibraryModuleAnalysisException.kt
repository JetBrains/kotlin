/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.sessions

import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule

/**
 * An exception thrown when analysis of a use-site [KaLibraryModule] is rejected by the Analysis API platform.
 *
 * @see org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings.allowUseSiteLibraryModuleAnalysis
 */
class KaBaseUseSiteLibraryModuleAnalysisException(
    libraryModule: KaLibraryModule,
) : IllegalStateException("Cannot analyze library module '$libraryModule' as a use-site module.")
