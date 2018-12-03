/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CheckDeclarationParentsVisitor
import org.jetbrains.kotlin.backend.common.DefaultIrPhaseRunner
import org.jetbrains.kotlin.backend.common.IrValidator
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

private fun validationCallback(module: IrModuleFragment, context: JsIrBackendContext) {
    val validatorConfig = IrValidatorConfig(
        abortOnError = true,
        ensureAllNodesAreDifferent = true,
        checkTypes = false,
        checkDescriptors = false
    )
    module.accept(IrValidator(context, validatorConfig), null)
    module.accept(CheckDeclarationParentsVisitor, null)
}

object JsPhaseRunner : DefaultIrPhaseRunner<JsIrBackendContext, IrModuleFragment>(::validationCallback) {
    override val startPhaseMarker = IrModuleStartPhase
    override val endPhaseMarker = IrModuleEndPhase

    override fun phases(context: JsIrBackendContext) = context.phases
    override fun elementName(input: IrModuleFragment) = input.name.asString()
    override fun configuration(context: JsIrBackendContext) = context.configuration
}