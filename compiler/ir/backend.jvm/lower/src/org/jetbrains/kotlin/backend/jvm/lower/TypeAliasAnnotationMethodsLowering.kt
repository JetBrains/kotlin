/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

internal val typeAliasAnnotationMethodsPhase = makeIrModulePhase(
    ::TypeAliasAnnotationMethodsLowering,
    name = "TypeAliasAnnotationMethodsLowering",
    description = "Generate method stubs for type alias annotations"
)

class TypeAliasAnnotationMethodsLowering(val context: CommonBackendContext) :
    ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        irClass.visitTypeAliases()
    }

    private val IrTypeAlias.syntheticAnnotationMethodName
        get() = Name.identifier(JvmAbi.getSyntheticMethodNameForAnnotatedTypeAlias(name))

    private fun IrClass.visitTypeAliases() {
        val annotatedAliases = declarations
            .filterIsInstance<IrTypeAlias>()
            .filter { it.annotations.isNotEmpty() }

        for (alias in annotatedAliases) {
            addFunction {
                name = alias.syntheticAnnotationMethodName
                visibility = alias.visibility
                returnType = context.irBuiltIns.unitType
                modality = Modality.OPEN
                origin = JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS
            }.apply {
                body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                annotations += alias.annotations
            }
        }
    }
}
