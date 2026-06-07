/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isFinalClass
import org.jetbrains.kotlin.ir.util.superClass
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * For full value class primary constructors with a non-Any superclass, moves field-from-parameter
 * initializations before the super constructor call.
 *
 * This lowering must run after [InitializersLowering]. It partitions the initializer statements
 * produced by [InitializersLowering] and reorders field-from-parameter initializations to appear
 * before the delegating super call.
 */
open class FullValueClassFieldInitBeforeSuperCallLowering(
    val context: CommonBackendContext,
) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrConstructor || !container.isPrimary) return
        val irClass = container.constructedClass
        if (!irClass.isFullValueClass || irClass.superClass == null || !irClass.isFinalClass) return

        val body = container.body as? IrBlockBody
        require(body?.statements?.firstOrNull() is IrDelegatingConstructorCall) {
            "Unexpected constructor body: ${container.dump()}"
        }

        // After InitializersLowering, initializer statements are placed after the delegating call,
        // either wrapped in an IrBlock (common backend) or inline (native backend).
        val initBlock = body.statements.getOrNull(1) as? IrBlock
        val initStatements = initBlock?.takeIf { it.origin == null }?.statements ?: body.statements

        val [fieldInitializations, otherInitializationStatements] = initStatements.partition { it.isFieldFromParameterInit() }
        require(fieldInitializations.size == irClass.valueClassRepresentation?.underlyingPropertyNamesToTypes?.size) {
            "Unexpected number of initialization statements: ${fieldInitializations.size} vs ${irClass.valueClassRepresentation?.underlyingPropertyNamesToTypes?.size}"
        }
        if (fieldInitializations.isEmpty()) return

        initStatements.assignFrom(otherInitializationStatements)
        body.statements.addAll(0, fieldInitializations)
    }

    private fun IrStatement.isFieldFromParameterInit(): Boolean {
        val setField = when (this) {
            is IrSetField -> this
            is IrBlock -> statements.singleOrNull() as? IrSetField
            else -> null
        } ?: return false
        return (setField.value as? IrGetValue)?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
    }
}
