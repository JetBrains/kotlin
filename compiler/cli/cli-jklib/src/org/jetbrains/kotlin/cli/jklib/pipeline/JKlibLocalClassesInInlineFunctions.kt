/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationPopupLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

// These lowerings are derived from the `LocalClasses.kt` lowerings that were removed in Kotlin 2.2.20.
//
// They hoist the local classes declared inside an inline function body out of that body, so that all the call sites of
// the inline function end up sharing a single class instead of each getting their own copy. That matches the Kotlin/JVM
// semantics:
//
//     inline fun f() = object : I {}
//     f()::class.java === f()::class.java  // true
//
// `loweringsOfTheFirstPhase()` does not run them, so the JS, Wasm and Native backends do duplicate those classes. Since
// JKlib performs inlining during the first phase, the extraction has to happen there, before the inliner runs.
internal class JKlibLocalClassesInInlineFunctionsLowering(val context: LoweringContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val function = container as? IrFunction ?: return
        val classesToExtract = mutableSetOf<IrClass>()
        function.collectExtractableLocalClassesInto(classesToExtract)
        if (classesToExtract.isEmpty()) return

        LocalDeclarationsLowering(context).lower(irBody, function)
    }
}

internal class JKlibLocalClassesExtractionFromInlineFunctionsLowering(context: LoweringContext) :
    LocalDeclarationPopupLowering(context) {

    private val classesToExtract = mutableSetOf<IrClass>()

    override fun lower(irFile: IrFile) {
        // The classes are popped up into the nearest declaration container, so the file has to be traversed with the
        // local declarations included.
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val function = container as? IrFunction ?: return

        function.collectExtractableLocalClassesInto(classesToExtract)
        if (classesToExtract.isEmpty()) return
        super.lower(irBody, container)

        // A class hoisted out of a non-private inline function ends up being referenced by the body that gets serialized
        // for cross-module inlining, so consumers of this KLIB must be able to link against it. Local classes are
        // effectively private, which both breaks that linkage and trips `InlineDeclarationCheckerLowering`.
        if (!function.isEffectivelyPrivate()) {
            classesToExtract.forEach { it.visibility = DescriptorVisibilities.PUBLIC }
        }

        classesToExtract.clear()
    }

    override fun shouldPopUp(declaration: IrDeclaration, currentScope: ScopeWithIr?): Boolean {
        return declaration is IrClass && classesToExtract.contains(declaration)
    }
}

private fun IrFunction.collectExtractableLocalClassesInto(classesToExtract: MutableSet<IrClass>) {
    // Unlike the original lowering, which only backs off on reified type parameters (so that they can be substituted at
    // the call site), back off on any type parameter: the consumer of the JKlib IR does not erase the type parameters
    // of inline functions, which effectively makes all of them reified.
    if (!isInline || typeParameters.isNotEmpty()) return

    val crossinlineParameters = parameters.filter { it.isCrossinline }.toSet()

    acceptChildrenVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            var canExtract = true
            if (crossinlineParameters.isNotEmpty()) {
                declaration.acceptVoid(object : IrVisitorVoid() {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitGetValue(expression: IrGetValue) {
                        if (expression.symbol.owner in crossinlineParameters) canExtract = false
                    }
                })
            }
            if (canExtract) classesToExtract.add(declaration)
        }
    })
}
