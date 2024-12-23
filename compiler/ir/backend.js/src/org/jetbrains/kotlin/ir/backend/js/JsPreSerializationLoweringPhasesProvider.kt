/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.backend.js.lower.JsCodeOutliningLowering
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.PreSerializationLoweringPhasesProvider
import org.jetbrains.kotlin.ir.util.KotlinMangler

object JsPreSerializationLoweringPhasesProvider : PreSerializationLoweringPhasesProvider<JsPreSerializationLoweringContext>() {
    private fun jsCodeOutliningLoweringFactory(context: JsPreSerializationLoweringContext): JsCodeOutliningLowering {
        return JsCodeOutliningLowering(context, context.intrinsics, context.dynamicType)
    }

    override val jsCodeOutliningLowering: (JsPreSerializationLoweringContext) -> FileLoweringPass = ::jsCodeOutliningLoweringFactory

    override val irMangler: KotlinMangler.IrMangler
        get() = JsManglerIr

    override fun getLowerings(): List<SimpleNamedCompilerPhase<JsPreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> {
        return jsLoweringsOfTheFirstPhase
    }
}
