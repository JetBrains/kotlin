/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.ir.inline.PreSerializationLoweringPhasesProvider

object JsPreSerializationLoweringPhasesProvider : PreSerializationLoweringPhasesProvider<PreSerializationLoweringContext>() {

    override val jsCodeOutliningLowering: ((PreSerializationLoweringContext) -> FileLoweringPass)?
        get() = null // TODO(KT-71415): Return the actual lowering here

    override val allowExternalInlineFunctions: Boolean
        get() = true
}
