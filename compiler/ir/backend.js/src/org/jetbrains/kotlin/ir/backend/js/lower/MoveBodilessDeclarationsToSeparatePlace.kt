/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.getJsModule
import org.jetbrains.kotlin.ir.backend.js.utils.getJsQualifier
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName

private val BODILESS_BUILTIN_CLASSES = listOf(
    "kotlin.String",
    "kotlin.Nothing",
    "kotlin.Array",
    "kotlin.Any",
    "kotlin.ByteArray",
    "kotlin.CharArray",
    "kotlin.ShortArray",
    "kotlin.IntArray",
    "kotlin.LongArray",
    "kotlin.FloatArray",
    "kotlin.DoubleArray",
    "kotlin.BooleanArray",
    "kotlin.Boolean",
    "kotlin.Byte",
    "kotlin.Short",
    "kotlin.Int",
    "kotlin.Float",
    "kotlin.Double",
    "kotlin.Function"
).map { FqName(it) }.toSet()

private class DescriptorlessIrFileSymbol : IrFileSymbol {
    override fun bind(owner: IrFile) {
        _owner = owner
    }

    @DescriptorBasedIr
    override val descriptor: PackageFragmentDescriptor
        get() = error("Operation is unsupported")

    private var _owner: IrFile? = null
    override val owner get() = _owner!!

    override val isBound get() = _owner != null

    override val isPublicApi: Boolean
        get() = error("Operation is unsupported")

    override val signature: IdSignature
        get() = error("Operation is unsupported")
}

private fun isBuiltInClass(declaration: IrDeclaration): Boolean =
    declaration is IrClass && declaration.fqNameWhenAvailable in BODILESS_BUILTIN_CLASSES

fun moveBodilessDeclarationsToSeparatePlace(context: JsIrBackendContext, moduleFragment: IrModuleFragment) {
    MoveBodilessDeclarationsToSeparatePlaceLowering(context).let { moveBodiless ->
        moduleFragment.files.forEach {
            moveBodiless.lower(it)
        }
    }
}

class MoveBodilessDeclarationsToSeparatePlaceLowering(private val context: JsIrBackendContext) : DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        val irFile = declaration.parent as? IrFile ?: return null

        val externalPackageFragment by lazy {
            context.externalPackageFragment.getOrPut(irFile.symbol) {
                IrFileImpl(fileEntry = irFile.fileEntry, fqName = irFile.fqName, symbol = DescriptorlessIrFileSymbol()).also {
                    it.annotations += irFile.annotations
                }
            }
        }

        if (irFile.getJsModule() != null || irFile.getJsQualifier() != null) {
            externalPackageFragment.declarations += declaration
            declaration.parent = externalPackageFragment

            context.packageLevelJsModules += externalPackageFragment

            declaration.collectAllExternalDeclarations()

            return emptyList()
        } else {
            val d = declaration as? IrDeclarationWithName ?: return null

            if (isBuiltInClass(d)) {
                context.bodilessBuiltInsPackageFragment.addChild(d)
                d.collectAllExternalDeclarations()

                return emptyList()
            } else if (d.isEffectivelyExternal()) {
                if (d.getJsModule() != null)
                    context.declarationLevelJsModules.add(d)

                externalPackageFragment.declarations += d
                d.parent = externalPackageFragment

                d.collectAllExternalDeclarations()

                return emptyList()
            }

            return null
        }
    }

    private fun IrDeclaration.collectAllExternalDeclarations() {
        this.accept(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclaration(declaration: IrDeclaration) {
                context.externalDeclarations.add(declaration)
                super.visitDeclaration(declaration)
            }
        }, null)
    }
}