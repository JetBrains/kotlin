/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.permissions

import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions

class KotlinStandaloneAnalysisPermissionOptions : KotlinAnalysisPermissionOptions {
    override val defaultIsAnalysisAllowedOnEdt: Boolean get() = true
    override val defaultIsAnalysisAllowedInWriteAction: Boolean get() = true
}
