/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class ManglerChecker(vararg _manglers: KotlinMangler<IrDeclaration>) : IrElementVisitorVoid {

    private val manglers = _manglers.toList()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }


    private val skipper = object : IrElementVisitor<Boolean, Nothing?> {
        override fun visitElement(element: IrElement, data: Nothing?): Boolean {
            error("unexpected element: ${element.render()}")
        }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?): Boolean {
            return declaration.descriptor is IrBasedDeclarationDescriptor<*>
        }
        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): Boolean = true
        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): Boolean = true
        override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean = true
        override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): Boolean = true
        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): Boolean = true
    }

    private fun IrDeclaration.shouldBeSkipped(): Boolean = accept(skipper, null)
    private fun KotlinMangler<IrDeclaration>.isExportCheck(declaration: IrDeclaration) = declaration.isExported(false)
    private fun KotlinMangler<IrDeclaration>.stringMangle(declaration: IrDeclaration) =
        declaration.mangleString { (it as IrDeclarationWithName).name.asString() }

    private fun KotlinMangler<IrDeclaration>.signatureMangle(declaration: IrDeclaration) =
        declaration.signatureString { (it as IrDeclarationWithName).name.asString() }

    private fun KotlinMangler<IrDeclaration>.fqnMangle(declaration: IrDeclaration) =
        declaration.fqnString { (it as IrDeclarationWithName).name.asString() }

    private fun <T : Any, R> Iterable<T>.checkAllEqual(init: R, op: T.() -> R, onError: (T, R, T, R) -> Unit): R {
        var prev: T? = null

        var r = init

        for (it in this) {
            if (prev == null) {
                r = it.op()
                prev = it
            } else {
                val tmp = it.op()
                if (r != tmp) {
                    onError(prev, r, it, tmp)
                }
                prev = it
                r = tmp
            }
        }

        return r
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {

        if (declaration is IrErrorDeclaration) return

        if (declaration.shouldBeSkipped()) return

//        val exported = manglers.checkAllEqual(false, { isExportCheck(declaration) }) { m1, r1, m2, r2 ->
//            error("${declaration.render()}\n ${m1.manglerName}: $r1\n ${m2.manglerName}: $r2\n")
//        }
//
//        if (!exported) return

        manglers.checkAllEqual("", { stringMangle(declaration) }) { m1, r1, m2, r2 ->
            error("FULL: ${declaration.render()}\n ${m1.manglerName}: $r1\n ${m2.manglerName}: $r2\n")
        }

        manglers.checkAllEqual("", { signatureMangle(declaration) }) { m1, r1, m2, r2 ->
            error("SIG: ${declaration.render()}\n ${m1.manglerName}: $r1\n ${m2.manglerName}: $r2\n")
        }

        manglers.checkAllEqual("", { fqnMangle(declaration) }) { m1, r1, m2, r2 ->
            error("FQN: ${declaration.render()}\n ${m1.manglerName}: $r1\n ${m2.manglerName}: $r2\n")
        }

        declaration.acceptChildrenVoid(this)
    }
}