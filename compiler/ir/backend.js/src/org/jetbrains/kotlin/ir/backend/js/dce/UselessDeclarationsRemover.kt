/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.dce

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.defaultConstructorForReflection
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.utils.associatedObject
import org.jetbrains.kotlin.ir.backend.js.utils.findDefaultConstructorForReflection
import org.jetbrains.kotlin.ir.backend.js.utils.prependFunctionCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.utils.memoryOptimizedFilter
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class UselessDeclarationsRemover(
    private val removeUnusedAssociatedObjects: Boolean,
    private val usefulDeclarations: Set<IrDeclaration>,
    private val context: JsIrBackendContext,
    private val dceRuntimeDiagnostic: RuntimeDiagnostic?,
) : IrVisitorVoid() {
    private val savedTypesCache = hashMapOf<IrClassSymbol, Set<IrClassSymbol>>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        process(declaration)
    }

    /**
     * Whether an `@AssociatedObjectKey`-annotated annotation may stay on the declaration.
     *
     * It has to be dropped as soon as [org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsClassGenerator]
     * would be unable to emit the `associatedObjects` entry for it: that entry references the
     * associated object's `getInstance` function, and the namer cannot produce a name for a declaration
     * DCE has already removed (KT-88571).
     *
     * The object itself being useful is *not* sufficient. A companion object's instance is created in the
     * containing class's `static_init`, not in its own `getInstance`, so the object may well survive DCE
     * while `getInstance` does not — e.g. an enum whose entries force `static_init` to be retained, but
     * whose companion is never accessed from Kotlin code.
     *
     * This mirrors [JsUsefulDeclarationProcessor.handleAssociatedObjects], which enqueues `getInstance`
     * only for classes reachable as JS classes. Whenever it declines to, the annotation must go too,
     * otherwise the two decisions contradict each other.
     */
    private fun IrAnnotation.shouldKeepAnnotation(): Boolean {
        val obj = associatedObject() ?: return true
        if (obj !in usefulDeclarations) return false
        val getInstance = obj.objectGetInstanceFunction
        return getInstance == null || getInstance in usefulDeclarations
    }

    override fun visitClass(declaration: IrClass) {
        process(declaration)
        // Drop `findAssociatedObject` annotations whose association can no longer be emitted. See `shouldKeepAnnotation`.
        if (removeUnusedAssociatedObjects && declaration.annotations.any { !it.shouldKeepAnnotation() }) {
            declaration.annotations = declaration.annotations.memoryOptimizedFilter { it.shouldKeepAnnotation() }
        }

        declaration.superTypes = declaration.superTypes
            .flatMap { it.classOrNull?.collectUsedSuperTypes() ?: emptyList() }
            .distinct()
            .memoryOptimizedMap { it.defaultType }

        // Remove default constructor if the class was never constructed
        val defaultConstructor = declaration.findDefaultConstructorForReflection()
        if (defaultConstructor != null && defaultConstructor !in usefulDeclarations) {
            declaration.defaultConstructorForReflection = null
        }
    }

    private fun IrClassSymbol.collectUsedSuperTypes(): Set<IrClassSymbol> {
        return savedTypesCache.getOrPut(this) {
            if (owner in usefulDeclarations || context.keeper.shouldKeep(owner)) {
                setOf(this)
            } else {
                owner.superTypes
                    .flatMap { it.takeIf { !it.isAny() }?.classOrNull?.collectUsedSuperTypes() ?: emptyList() }
                    .toHashSet()
            }
        }
    }

    // TODO bring back the primary constructor fix
    private fun process(container: IrDeclarationContainer) {
        container.declarations.transformFlat { member ->
            if (member !in usefulDeclarations) {
                member.processUselessDeclaration()
            } else {
                member.acceptVoid(this)
                null
            }
        }
    }

    private fun IrDeclaration.processUselessDeclaration(): List<IrDeclaration>? {
        return when {
            dceRuntimeDiagnostic != null -> {
                processWithDiagnostic(dceRuntimeDiagnostic)
                null
            }
            else -> emptyList()
        }
    }

    private fun RuntimeDiagnostic.removingBody(): Boolean =
        this != RuntimeDiagnostic.LOG

    private fun IrDeclaration.processWithDiagnostic(dceRuntimeDiagnostic: RuntimeDiagnostic) {
        when (this) {
            is IrFunction -> processFunctionWithDiagnostic(dceRuntimeDiagnostic)
            is IrField -> processFieldWithDiagnostic()
            is IrDeclarationContainer -> declarations.forEach { it.processWithDiagnostic(dceRuntimeDiagnostic) }
        }
    }

    private fun IrFunction.processFunctionWithDiagnostic(dceRuntimeDiagnostic: RuntimeDiagnostic) {
        val isRemovingBody = dceRuntimeDiagnostic.removingBody()
        val targetMethod = dceRuntimeDiagnostic.unreachableDeclarationMethod(context)
        val call = JsIrBuilder.buildCall(
            target = targetMethod,
            type = targetMethod.owner.returnType
        )

        if (isRemovingBody) {
            body = context.irFactory.createBlockBody(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
            )
        }

        body?.prependFunctionCall(call)
    }

    private fun IrField.processFieldWithDiagnostic() {
        if (initializer != null && isKotlinPackage()) {
            initializer = null
        }
    }
}
