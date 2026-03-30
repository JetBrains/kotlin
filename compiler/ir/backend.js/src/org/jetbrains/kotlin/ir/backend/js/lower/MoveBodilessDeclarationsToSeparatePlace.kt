/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.originalFileForExternalDeclaration
import org.jetbrains.kotlin.ir.backend.js.utils.getJsModule
import org.jetbrains.kotlin.ir.backend.js.utils.getJsQualifier
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

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

internal fun isBuiltInClass(declaration: IrDeclaration): Boolean =
    declaration is IrClass && declaration.fqNameWhenAvailable in BODILESS_BUILTIN_CLASSES

internal fun isJsStdLibClass(declaration: IrDeclaration): Boolean =
    declaration is IrClass && declaration.fqNameWhenAvailable?.isChildOf(StandardClassIds.BASE_JS_PACKAGE) != false

internal fun isStdLibClass(declaration: IrDeclaration): Boolean =
    declaration is IrClass && declaration.fqNameWhenAvailable?.isChildOf(StandardClassIds.BASE_KOTLIN_PACKAGE) != false

private val JsIntrinsicFqName = FqName("kotlin.js.JsIntrinsic")

private fun IrDeclaration.isPlacedInsideInternalPackage() =
    (parent as? IrPackageFragment)?.packageFqName == StandardClassIds.BASE_JS_PACKAGE

private fun isIntrinsic(declaration: IrDeclaration): Boolean =
    declaration is IrSimpleFunction && declaration.isPlacedInsideInternalPackage() &&
            declaration.annotations.any { it.symbol.owner.constructedClass.fqNameWhenAvailable == JsIntrinsicFqName }

fun moveBodilessDeclarationsToSeparatePlace(context: JsIrBackendContext, moduleFragment: IrModuleFragment) {
    MoveBodilessDeclarationsToSeparatePlaceLowering(context).let { moveBodiless ->
        moduleFragment.files.forEach {
            validateIsExternal(it)
            moveBodiless.lower(it)
        }
    }

    // Lower @JsStatic inside external declarations
    JsStaticLowering(context).let { jsStaticMembers ->
        context.externalPackageFragment.values.forEach(jsStaticMembers::lower)
    }
}

class MoveBodilessDeclarationsToSeparatePlaceLowering(private val context: JsIrBackendContext) : DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        val irFile = declaration.parent as? IrFile ?: return null

        val externalPackageFragment by lazy(LazyThreadSafetyMode.NONE) {
            context.externalPackageFragment.getOrPut(irFile.symbol) {
                IrFileImpl(fileEntry = irFile.fileEntry, fqName = irFile.packageFqName, symbol = IrFileSymbolImpl(), module = irFile.module).also {
                    it.annotations = it.annotations memoryOptimizedPlus irFile.annotations
                }
            }
        }

        return when (declaration) {
            is IrDeclarationWithName if (isBuiltInClass(declaration) || isIntrinsic(declaration)) -> {
                context.bodilessBuiltInsPackageFragment.addChild(declaration)
                emptyList()
            }
            is IrPossiblyExternalDeclaration if declaration.isEffectivelyExternal() -> {
                externalPackageFragment.declarations += declaration
                declaration.parent = externalPackageFragment

                if (irFile.getJsModule() != null || irFile.getJsQualifier() != null)
                    context.packageLevelJsModules += externalPackageFragment

                emptyList()
            }
            else -> null
        }
    }
}

fun validateIsExternal(file: IrFile) {
    for (declaration in file.declarations) {
        validateNestedExternalDeclarations(file, declaration, (declaration as? IrPossiblyExternalDeclaration)?.isExternal ?: false)
    }
}


fun validateNestedExternalDeclarations(file: IrFile, declaration: IrDeclaration, isExternalTopLevel: Boolean) {
    fun IrPossiblyExternalDeclaration.checkExternal() {
        if (isExternal != isExternalTopLevel) {
            compilationException(
                "isExternal validation failed for declaration",
                declaration,
            )
        }
        if (isExternal) {
            originalFileForExternalDeclaration = file
        }
    }

    if (declaration is IrPossiblyExternalDeclaration) {
        declaration.checkExternal()
    }
    if (declaration is IrProperty) {
        declaration.getter?.checkExternal()
        declaration.setter?.checkExternal()
        declaration.backingField?.checkExternal()
    }
    if (declaration is IrClass) {
        declaration.declarations.forEach {
            validateNestedExternalDeclarations(file, it, isExternalTopLevel)
        }
    }
}

