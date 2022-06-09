/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtSignatureSubstitutorImpl

internal class KtFe10SignatureSubstitutor(
    override val analysisSession: KtFe10AnalysisSession
) : AbstractKtSignatureSubstitutorImpl(), Fe10KtAnalysisSessionComponent