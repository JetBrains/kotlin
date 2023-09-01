/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.getExportCandidate
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedFilter
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

class AnnotateKeptAndVisibleForInteropDeclarationsLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    override fun lower(irFile: IrFile) {
        if (irFile.hasJsExport()) {
            irFile.declarations.forEach { it.splitJsExportAnnotation() }
        }
        super.lower(irFile)
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrDeclarationWithName) {
            when {
                declaration.hasJsExport() -> declaration.splitJsExportAnnotation()
                declaration.fqNameWhenAvailable in context.additionalExportedDeclarationNames -> declaration.splitJsExportAnnotation()
                declaration.fqNameWhenAvailable.toString() in context.keep -> declaration.keepDeclarationAndItsParent()
            }
        }

        return null
    }

    private fun IrDeclaration.splitJsExportAnnotation() {
        // Formally, user have no ability to annotate EnumEntry as exported, without Enum Class
        // But, when we add @file:JsExport, the annotation appears on the all of enum entries
        // what make a wrong behaviour on non-exported members inside Enum Entry (check exportEnumClass and exportFileWithEnumClass tests)
        if (hasJsExportIgnore() || (this is IrClass && kind == ClassKind.ENUM_ENTRY)) return

        addAllNeededAnnotations()

        if (this is IrClass) {
            declarations.forEach { it.addAllNeededAnnotations() }
        }
    }

    private fun IrDeclaration.addAllNeededAnnotations() {
        if (!isExported()) return

        if (this is IrOverridableDeclaration<*> && parentClassOrNull != null && getJsNameForOverriddenDeclaration() == null) {
            addJsNameWith(name.toString(), context)
        }

        addJsKeep(context)
        addJsVisibleForInterop(context)
    }

    /** Keep declarations can work both ways
     * if member of a class is in keep, the class should be also kept
     * if a class is kept, members of the class should be also kept
     * but there can be nested classes, and for nested classes we need only to propagate "keep" from top-level to nested (not vice versa)
     * because we have 2 directions, we need 2 boolean flags
     */
    private fun IrDeclaration.keepDeclarationAndItsParent() {
        addJsKeep(context)
        parentClassOrNull?.keepDeclarationAndItsParent()

        if (this is IrClass) {
            declarations.forEach { it.addJsKeep(context) }
        }
    }

    private fun IrDeclaration.isExported(): Boolean {
        val candidate = getExportCandidate(this) ?: return false
        return shouldDeclarationBeExported(candidate)
    }
    private fun shouldDeclarationBeExported(declaration: IrDeclarationWithName): Boolean {
        // Formally, user have no ability to annotate EnumEntry as exported, without Enum Class
        // But, when we add @file:JsExport, the annotation appears on the all of enum entries
        // what make a wrong behaviour on non-exported members inside Enum Entry (check exportEnumClass and exportFileWithEnumClass tests)
        if (declaration is IrClass && declaration.kind == ClassKind.ENUM_ENTRY)
            return false

        if (context.additionalExportedDeclarationNames.contains(declaration.fqNameWhenAvailable))
            return true

        if (declaration.hasJsExportIgnore())
            return false

        if (declaration is IrOverridableDeclaration<*>) {
            val overriddenNonEmpty = declaration
                .overriddenSymbols
                .isNotEmpty()

            if (overriddenNonEmpty) {
                return (declaration as? IrSimpleFunction)?.isMethodOfAny() == true // Handle names for special functions
                        || declaration.isAllowedFakeOverriddenDeclaration()
            }
        }

        if (declaration.hasJsExport())
            return true

        return when (val parent = declaration.parent) {
            is IrDeclarationWithName -> shouldDeclarationBeExported(parent)
            is IrAnnotationContainer -> parent.hasJsExport()
            else -> false
        }
    }


    private fun IrOverridableDeclaration<*>.isAllowedFakeOverriddenDeclaration(): Boolean {
        val firstExportedRealOverride = runIf(isFakeOverride) {
            resolveFakeOverrideOrNull(allowAbstract = true) { it === this || it.parentClassOrNull?.isExported() != true }
        }

        if (firstExportedRealOverride?.parentClassOrNull.isExportedInterface()) {
            return true
        }

        return overriddenSymbols
            .asSequence()
            .map { it.owner }
            .filterIsInstance<IrOverridableDeclaration<*>>()
            .filter { it.overriddenSymbols.isEmpty() }
            .mapNotNull { it.parentClassOrNull }
            .map { it.symbol }
            .any { it == context.irBuiltIns.enumClass }
    }

    private fun IrDeclaration?.isExportedInterface() =
        this is IrClass && kind.isInterface && hasJsExport()

}

class RemoveJsExportAnnotationUsageLowering : DeclarationTransformer {
    override fun lower(irFile: IrFile) {
        irFile.removeJsExport()
        super.lower(irFile)
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        declaration.removeJsExport()
        return null
    }

    private fun IrMutableAnnotationContainer.removeJsExport() {
        annotations = annotations.memoryOptimizedFilter {
            !it.isAnnotation(JsAnnotations.jsExportFqn) && !it.isAnnotation(JsAnnotations.jsExportIgnoreFqn)
        }
    }
}

fun IrMutableAnnotationContainer.addJsKeep(context: JsIrBackendContext) {
    if (hasJsKeep()) return
    val jsKeepConstructor = context.intrinsics.jsKeepAnnotationSymbol.owner.primaryConstructor!!
    annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsKeepConstructor.symbol)
}

fun IrMutableAnnotationContainer.addJsNameWith(name: String, context: JsIrBackendContext) {
    if (hasJsName()) return
    val jsNameConstructor = context.intrinsics.jsNameAnnotationSymbol.owner.primaryConstructor!!

    annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsNameConstructor.symbol)
        .apply { putValueArgument(0, name.toIrConst(context.irBuiltIns.stringType)) }
}

fun IrMutableAnnotationContainer.addJsVisibleForInterop(context: JsIrBackendContext) {
    if (hasJsVisibleForInterop()) return
    val jsVisibleForInteropConstructor = context.intrinsics.jsVisibleForInteropAnnotationSymbol.owner.primaryConstructor!!
    annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsVisibleForInteropConstructor.symbol)
}
