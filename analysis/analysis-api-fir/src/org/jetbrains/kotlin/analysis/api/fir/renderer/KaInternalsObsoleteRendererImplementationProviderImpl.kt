/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.renderer

import org.jetbrains.kotlin.analysis.api.internals.KaInternalsObsoleteRendererImplementationProvider
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.name.FqName

internal class KaInternalsObsoleteRendererImplementationProviderImpl : KaInternalsObsoleteRendererImplementationProvider {
    override val knownNullabilityAnnotations: Set<FqName>
        get() = NULLABILITY_ANNOTATIONS
}
