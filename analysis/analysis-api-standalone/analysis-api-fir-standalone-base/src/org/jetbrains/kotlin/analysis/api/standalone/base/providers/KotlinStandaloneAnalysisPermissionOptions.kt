/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.providers

import org.jetbrains.kotlin.analysis.providers.permissions.KotlinAnalysisPermissionOptions

// TODO (KT-68386): Currently unused due to KT-68386, but will be used again.
class KotlinStandaloneAnalysisPermissionOptions : KotlinAnalysisPermissionOptions {
    override val defaultIsAnalysisAllowedOnEdt: Boolean get() = true
    override val defaultIsAnalysisAllowedInWriteAction: Boolean get() = true
}
