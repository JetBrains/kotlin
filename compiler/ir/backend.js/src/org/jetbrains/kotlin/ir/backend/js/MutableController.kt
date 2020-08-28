/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrBodyBase
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrDeclarationBase
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.isLocal

open class MutableController(val context: JsIrBackendContext, val lowerings: List<Lowering>) : StageController() {

    override var currentStage: Int = 0

    override fun lazyLower(declaration: IrDeclaration) {
        if (declaration is PersistentIrDeclarationBase<*>) {
            while (declaration.loweredUpTo + 1 < currentStage) {
                val i = declaration.loweredUpTo + 1
                withStage(i) {
                    // TODO a better way to skip declarations in external package fragments
                    if (declaration.removedOn > i && declaration !in context.externalDeclarations) {

                        when (val lowering = lowerings[i - 1]) {
                            is DeclarationLowering -> lowering.doApplyLoweringTo(declaration)
                            is BodyLowering -> {
                                // Handle local declarations in case they leak through types
                                if (declaration.isLocal) {
                                    declaration.enclosingBody()?.let {
                                        withStage(i + 1) { lazyLower(it) }
                                    }
                                }
                            }
                        }
                    }
                    declaration.loweredUpTo = i
                }
            }
        }
    }

    override fun lazyLower(body: IrBody) {
        if (body is PersistentIrBodyBase<*>) {
            for (i in (body.loweredUpTo + 1) until currentStage) {
                withStage(i) {
                    if (body.container !in context.externalDeclarations) {
                        val lowering = lowerings[i - 1]

                        if (lowering is BodyLowering) {
                            bodyLowering {
                                lowering.bodyLowering(context).lower(body, body.container)
                            }
                        }
                    }
                    body.loweredUpTo = i
                }
            }
        }
    }

    // Launches a lowering and applies it's results
    private fun DeclarationLowering.doApplyLoweringTo(declaration: PersistentIrDeclarationBase<*>) {
        val parentBefore = declaration.parent
        val result = restrictTo(declaration) { this.declarationTransformer(context).transformFlat(declaration) }
        if (result != null) {
            result.forEach {
                // Some of our lowerings rely on transformDeclarationsFlat
                it.parent = parentBefore
            }

            if (parentBefore is IrDeclarationContainer) {
                unrestrictDeclarationListsAccess {

                    // Field order matters for top level property initialization
                    val correspondingProperty = when (declaration) {
                        is IrSimpleFunction -> declaration.correspondingPropertySymbol?.owner
                        is IrField -> declaration.correspondingPropertySymbol?.owner
                        else -> null
                    }

                    var index = -1
                    parentBefore.declarations.forEachIndexed { i, v ->
                        if (v == declaration || index == -1 && v == correspondingProperty) {
                            index = i
                        }
                    }

                    if (index != -1 && declaration !is IrProperty) {
                        if (parentBefore.declarations[index] == declaration) {
                            parentBefore.declarations.removeAt(index)
                        }
                        parentBefore.declarations.addAll(index, result)
                    } else {
                        parentBefore.declarations.addAll(result)
                    }

                    if (declaration.parent == parentBefore && declaration !in result) {
                        declaration.removedOn = currentStage
                    }
                }
            }
        }
    }

    // Finds outermost body, containing the declarations
    // Doesn't work in case of local declarations inside default arguments
    // That might be fine as those shouldn't leak
    private fun IrDeclaration.enclosingBody(): IrBody? {
        var lastBodyContainer: IrDeclaration? = null
        var parent = this.parent
        while (parent is IrDeclaration) {
            if (parent !is IrClass) {
                lastBodyContainer = parent
            }
            parent = parent.parent
        }
        return lastBodyContainer?.run {
            when (this) {
                is IrFunction -> body // TODO What about local declarations inside default arguments?
                is IrField -> initializer
                else -> null
            }
        }
    }

    override fun <T> withStage(stage: Int, fn: () -> T): T {
        val prevStage = currentStage
        currentStage = stage
        try {
            return fn()
        } finally {
            currentStage = prevStage
        }
    }

    override fun <T> withInitialIr(block: () -> T): T = { withStage(0, block) }.withRestrictions(newRestrictedToDeclaration = null)

    override fun <T> restrictTo(declaration: IrDeclaration, fn: () -> T): T = fn.withRestrictions(newRestrictedToDeclaration = declaration)

    override fun <T> bodyLowering(fn: () -> T): T = fn.withRestrictions(newBodiesEnabled = true, newRestricted = true, newDeclarationListsRestricted = true)

    override fun <T> unrestrictDeclarationListsAccess(fn: () -> T): T = fn.withRestrictions(newDeclarationListsRestricted = false)

    override fun canModify(element: IrElement): Boolean {
        return true
        // TODO fix and enable
//        return !restricted || restrictedToDeclaration === element || element is IrPersistingElementBase<*> && element.createdOn == currentStage
    }

    override fun canAccessDeclarationsOf(irClass: IrClass): Boolean {
        return !declarationListsRestricted || irClass.visibility == Visibilities.LOCAL && irClass !in context.extractedLocalClasses
    }

    private var restrictedToDeclaration: IrDeclaration? = null
    // TODO flags?
    override var bodiesEnabled: Boolean = true
    private var restricted: Boolean = false
    private var declarationListsRestricted = false

    private fun <T> (() -> T).withRestrictions(
        newRestrictedToDeclaration: IrDeclaration? = null,
        newBodiesEnabled: Boolean? = null,
        newRestricted: Boolean? = null,
        newDeclarationListsRestricted: Boolean? = null
    ): T {
        val prev = restrictedToDeclaration
        restrictedToDeclaration = newRestrictedToDeclaration
        val wereBodiesEnabled = bodiesEnabled
        bodiesEnabled = newBodiesEnabled ?: bodiesEnabled
        val wasRestricted = restricted
        restricted = newRestricted ?: restricted
        val wereDeclarationListsRestricted = declarationListsRestricted
        declarationListsRestricted = newDeclarationListsRestricted ?: declarationListsRestricted
        try {
            return this.invoke()
        } finally {
            restrictedToDeclaration = prev
            bodiesEnabled = wereBodiesEnabled
            restricted = wasRestricted
            declarationListsRestricted = wereDeclarationListsRestricted
        }
    }
}
