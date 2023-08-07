/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.couldBeConvertedToExplicitExport
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.memoryOptimizedFilter
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

class ConvertImplicitExportToExplicitLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsExportCtor by lazy(LazyThreadSafetyMode.NONE) { context.intrinsics.jsExportAnnotationSymbol.constructors.single() }
    private val jsImplicitExportCtor by lazy(LazyThreadSafetyMode.NONE) { context.intrinsics.jsImplicitExportAnnotationSymbol.constructors.single() }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (context.incrementalCacheEnabled) {
            if (declaration is IrClass && declaration.couldBeConvertedToExplicitExport() == true) {
                declaration.upgradeToExplicitExport()
            }
        } else if (declaration.isExported(context)) {
            val typesToUpgradeExportability = when (declaration) {
                is IrFunction -> declaration.collectPotentiallyExportedTypes()
                is IrProperty -> declaration.collectPotentiallyExportedTypes()
                is IrClass -> declaration.collectPotentiallyExportedTypes()
                is IrField -> declaration.type.collectPotentiallyExportedTypes()
                else -> error("Unsupported exported declaration ${declaration::class.simpleName}")
            }

            typesToUpgradeExportability.forEach {
                it.upgradeToExplicitExport()
            }
        }
        return null

    }

    private fun IrClass.collectPotentiallyExportedTypes(): Set<IrClass> {
        return typeParameters.asSequence()
            .flatMap { it.superTypes }
            .flatMap { it.collectPotentiallyExportedTypes() }
            .toSet()
    }

    private fun IrFunction.collectPotentiallyExportedTypes(): Set<IrClass> {
        return valueParameters.asSequence()
            .map { it.type }
            .plus(typeParameters.asSequence().flatMap { it.superTypes })
            .plus(returnType)
            .flatMap { it.collectPotentiallyExportedTypes() }
            .toSet()
    }

    private fun IrProperty.collectPotentiallyExportedTypes(): Set<IrClass> {
        return backingField?.type?.collectPotentiallyExportedTypes() ?: getter!!.collectPotentiallyExportedTypes()
    }

    private fun IrClass.upgradeToExplicitExport() {
        annotations =
            annotations.filter { it.symbol != jsImplicitExportCtor } memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsExportCtor)
    }

    private fun IrType?.collectPotentiallyExportedTypes(): Set<IrClass> {
        val simpleType = this?.makeNotNull() as? IrSimpleType ?: return emptySet()
        return buildSet {
            addIfNotNull(simpleType.classOrNull?.owner?.takeIf { it.couldBeConvertedToExplicitExport() == true })
            simpleType.arguments.forEach {
                addAll(it.typeOrNull.collectPotentiallyExportedTypes())
            }
        }
    }
}

class RemoveImplicitExportIfItsNotReachableLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsImplicitExportCtor by lazy(LazyThreadSafetyMode.NONE) { context.intrinsics.jsImplicitExportAnnotationSymbol.constructors.single() }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrClass && declaration.couldBeConvertedToExplicitExport() == true) {
            declaration.annotations = declaration.annotations.memoryOptimizedFilter {
                it.symbol != jsImplicitExportCtor
            }
        }
        return null
    }
}