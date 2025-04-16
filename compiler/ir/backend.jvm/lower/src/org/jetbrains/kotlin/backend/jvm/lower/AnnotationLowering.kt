/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.parentClassId
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Removes constructors of annotation classes.
 */
@PhaseDescription(name = "Annotation")
internal class AnnotationLowering(@Suppress("UNUSED_PARAMETER", "unused") context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (irClass.isAnnotationClass) {
            irClass.declarations.removeIf { it is IrConstructor }
        }
    }
}

@PhaseDescription(name = "MustUsePlacement")
internal class MustUseValuePlacementLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {

    fun IrElement.createAnnotationCallWithoutArgs(annotationSymbol: IrClassSymbol): IrConstructorCall {
        val annotationCtor = annotationSymbol.constructors.single { it.owner.isPrimary }
        val annotationType = annotationSymbol.defaultType

        return IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, annotationType, annotationCtor)
    }

    override fun lower(irFile: IrFile) {
        if (context.configuration.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) != ReturnValueCheckerMode.FULL) return

        irFile.transformChildren(this, null)
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val symbol = context.irPluginContext!!.referenceClass(StandardClassIds.Annotations.MustUseReturnValue)
        if (declaration.annotations.none { it.symbol.owner.parentClassId == StandardClassIds.Annotations.MustUseReturnValue })
            declaration.annotations += declaration.createAnnotationCallWithoutArgs(symbol!!)
        declaration.transformChildren(this, null)
        return declaration
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val symbol = context.irPluginContext!!.referenceClass(StandardClassIds.Annotations.MustUseReturnValue)
        if (declaration.annotations.none { it.symbol.owner.parentClassId == StandardClassIds.Annotations.MustUseReturnValue })
            declaration.annotations += declaration.createAnnotationCallWithoutArgs(symbol!!)
        return declaration
    }
}
