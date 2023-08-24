/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmFileFacadeClass
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource

internal val externalPackageParentPatcherPhase = makeIrModulePhase(
    ::createLowering,
    name = "ExternalPackageParentPatcherLowering",
    description = "Replace parent from package fragment to FileKt class for top-level callables (K2 only)"
)

private fun createLowering(context: CommonBackendContext): FileLoweringPass {
    require(context is JvmBackendContext)
    return when (context.configuration[CommonConfigurationKeys.USE_FIR]) {
        true -> ExternalPackageParentPatcherLowering(context)
        false, null -> FileLoweringPass.Empty
    }
}

class ExternalPackageParentPatcherLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(Visitor())
    }

    private inner class Visitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
            visitElement(expression)
            val callee = expression.symbol.owner as? IrMemberWithContainerSource ?: return
            if (callee.parent is IrExternalPackageFragment) {
                val parentClass = generateOrGetFacadeClass(callee) ?: return
                parentClass.parent = callee.parent
                callee.parent = parentClass
                when (callee) {
                    is IrProperty -> handleProperty(callee, parentClass)
                    is IrSimpleFunction -> callee.correspondingPropertySymbol?.owner?.let { handleProperty(it, parentClass) }
                }
            }
        }

        private fun generateOrGetFacadeClass(declaration: IrMemberWithContainerSource): IrClass? {
            val deserializedSource = declaration.containerSource ?: return null
            if (deserializedSource !is FacadeClassSource) return null
            val facadeName = deserializedSource.facadeClassName ?: deserializedSource.className
            return JvmFileFacadeClass(
                if (deserializedSource.facadeClassName != null) IrDeclarationOrigin.JVM_MULTIFILE_CLASS else IrDeclarationOrigin.FILE_CLASS,
                facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName(),
                deserializedSource,
                deserializeIr = { irClass -> deserializeTopLevelClass(irClass) }
            ).also {
                it.createParameterDeclarations()
                context.classNameOverride[it] = facadeName
            }
        }

        private fun deserializeTopLevelClass(irClass: IrClass): Boolean {
            return context.irDeserializer.deserializeTopLevelClass(
                irClass, context.irBuiltIns, context.symbolTable, context.irProviders, context.generatorExtensions
            )
        }

        private fun handleProperty(property: IrProperty, newParent: IrClass) {
            property.parent = newParent
            property.getter?.parent = newParent
            property.setter?.parent = newParent
            property.backingField?.parent = newParent
        }
    }
}
