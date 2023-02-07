/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.dce

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.web.config.WebConfigurationKeys
import org.jetbrains.kotlin.utils.addIfNotNull

fun eliminateDeadDeclarations(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext,
    removeUnusedAssociatedObjects: Boolean = true,
) {
    val allRoots = buildRoots(modules, context)

    val printReachabilityInfo =
        context.configuration.getBoolean(WebConfigurationKeys.PRINT_REACHABILITY_INFO) ||
                java.lang.Boolean.getBoolean("kotlin.js.ir.dce.print.reachability.info")

    val usefulDeclarationProcessor = JsUsefulDeclarationProcessor(context, printReachabilityInfo, removeUnusedAssociatedObjects)
    val usefulDeclarations = usefulDeclarationProcessor.collectDeclarations(allRoots)

    val uselessDeclarationsProcessor =
        UselessDeclarationsRemover(removeUnusedAssociatedObjects, usefulDeclarations, context, context.dceRuntimeDiagnostic)

    modules.forEach { module ->
        module.files.forEach {
            it.acceptVoid(uselessDeclarationsProcessor)
            context.polyfills.saveOnlyIntersectionOfNextDeclarationsFor(it, usefulDeclarationProcessor.usefulPolyfilledDeclarations)
        }
    }
}

private fun IrField.isConstant(): Boolean =
    correspondingPropertySymbol?.owner?.isConst ?: false

private fun IrDeclaration.addRootsTo(
    nestedVisitor: IrElementVisitorVoid,
    context: JsIrBackendContext
) {
    when {
        this is IrProperty -> {
            backingField?.addRootsTo(nestedVisitor, context)
            getter?.addRootsTo(nestedVisitor, context)
            setter?.addRootsTo(nestedVisitor, context)
        }

        isEffectivelyExternal() -> {
            val correspondingProperty = when (this) {
                is IrField -> correspondingPropertySymbol?.owner
                is IrSimpleFunction -> correspondingPropertySymbol?.owner
                else -> null
            }

            if (!hasJsPolyfill() && correspondingProperty?.hasJsPolyfill() != true) {
                acceptVoid(nestedVisitor)
            }
        }

        isExported(context) -> {
            acceptVoid(nestedVisitor)
        }

        this is IrField -> {
            // TODO: simplify
            if ((initializer != null && !isKotlinPackage() || correspondingPropertySymbol?.owner?.isExported(context) == true) && !isConstant()) {
                acceptVoid(nestedVisitor)
            }
        }

        this is IrSimpleFunction -> {
            val correspondingProperty = correspondingPropertySymbol?.owner ?: return
            if (correspondingProperty.isExported(context)) {
                acceptVoid(nestedVisitor)
            }
        }
    }
}

private fun buildRoots(modules: Iterable<IrModuleFragment>, context: JsIrBackendContext): List<IrDeclaration> = buildList {
    val declarationsCollector = object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement): Unit = element.acceptChildrenVoid(this)
        override fun visitBody(body: IrBody): Unit = Unit // Skip

        override fun visitDeclaration(declaration: IrDeclarationBase) {
            super.visitDeclaration(declaration)
            add(declaration)
        }
    }

    val allFiles = (modules.flatMap { it.files } + context.packageLevelJsModules + context.externalPackageFragment.values)
    allFiles.forEach { file ->
        file.declarations.forEach { declaration ->
            declaration.addRootsTo(declarationsCollector, context)
        }
    }

    val dceRuntimeDiagnostic = context.dceRuntimeDiagnostic
    if (dceRuntimeDiagnostic != null) {
        dceRuntimeDiagnostic.unreachableDeclarationMethod(context).owner.acceptVoid(declarationsCollector)
    }

    // TODO: Generate calls to main as IR->IR lowering and reference coroutineEmptyContinuation directly
    JsMainFunctionDetector(context).getMainFunctionOrNull(modules.last())?.let { mainFunction ->
        add(mainFunction)
        if (mainFunction.isLoweredSuspendFunction(context)) {
            context.coroutineEmptyContinuation.owner.acceptVoid(declarationsCollector)
        }
    }

    addIfNotNull(context.intrinsics.void.owner.backingField)
    addAll(context.testFunsPerFile.values)
    addAll(context.additionalExportedDeclarations)
}

internal fun RuntimeDiagnostic.unreachableDeclarationMethod(context: JsIrBackendContext) =
    when (this) {
        RuntimeDiagnostic.LOG -> context.intrinsics.jsUnreachableDeclarationLog
        RuntimeDiagnostic.EXCEPTION -> context.intrinsics.jsUnreachableDeclarationException
    }

internal fun IrField.isKotlinPackage() =
    fqNameWhenAvailable?.asString()?.startsWith("kotlin") == true