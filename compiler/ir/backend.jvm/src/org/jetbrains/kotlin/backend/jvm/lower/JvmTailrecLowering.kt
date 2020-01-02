/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.TailrecLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings

class JvmTailrecLowering(context: JvmBackendContext) : TailrecLowering(context) {
    override fun useProperComputationOrderOfTailrecDefaultParameters(): Boolean =
        context.ir.context.configuration.languageVersionSettings.supportsFeature(LanguageFeature.ProperComputationOrderOfTailrecDefaultParameters)
}