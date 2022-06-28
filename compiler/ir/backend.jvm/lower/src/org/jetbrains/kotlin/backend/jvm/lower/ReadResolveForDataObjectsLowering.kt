/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal val readResolveForDataObjectsPhase = makeIrFilePhase(
    ::ReadResolveForDataObjectsLowering,
    name = "ReadResolveForDataObjectsLowering",
    description = "Generate readResolve for serializable data objects"
)

private class ReadResolveForDataObjectsLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!context.state.languageVersionSettings.supportsFeature(LanguageFeature.DataObjects)) return

        if (!irClass.isData || irClass.kind != ClassKind.OBJECT || !irClass.isSerializable()) return

        context.irFactory.buildFun {
            name = Name.identifier("readResolve")
            modality = Modality.FINAL
            origin = IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER
            returnType = context.irBuiltIns.anyType
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            addDispatchReceiver { type = irClass.defaultType }
            parent = irClass
            body = context.createJvmIrBuilder(symbol).run {
                val instanceField = irClass.fields.single { it.name.asString() == JvmAbi.INSTANCE_FIELD }
                irExprBody(irGetField(null, instanceField))
            }
            irClass.declarations.add(this)
        }
    }
}

private val SERIALIZABLE_FQ_NAME = FqName("java.io.Serializable")

private fun IrClass.isSerializable(): Boolean =
    getAllSuperclasses().any { it.hasEqualFqName(SERIALIZABLE_FQ_NAME) }
