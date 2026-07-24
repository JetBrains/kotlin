/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaExperimentalApi::class, KaImplementationDetail::class)

package org.jetbrains.kotlin.analysis.api.fir.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.resolution.KaContextSensitiveResolutionStatus

internal object KaContextSensitiveResolutionNotAvailableImpl : KaContextSensitiveResolutionStatus.NotAvailable {
    override fun toString(): String = "NotAvailable"
}

internal object KaContextSensitiveResolutionUsedImpl : KaContextSensitiveResolutionStatus.Used {
    override fun toString(): String = "Used"
}

internal object KaContextSensitiveResolutionQualifierCanBeRemovedImpl : KaContextSensitiveResolutionStatus.QualifierCanBeRemoved {
    override fun toString(): String = "QualifierCanBeRemoved"
}

internal object KaContextSensitiveResolutionImportCanBeRemovedImpl : KaContextSensitiveResolutionStatus.ImportCanBeRemoved {
    override fun toString(): String = "ImportCanBeRemoved"
}
