/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.cli.pipeline.web.WebIrLoadingPipelinePhase
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC

object JsIrLoadingPipelinePhase : WebIrLoadingPipelinePhase("JsIrLoadingPipelinePhase") {
    override fun createIrFactory(): IrFactory = IrFactoryImplForJsIC(WholeWorldStageController())
}
