package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

// Analysis we're implementing here is as following.
// We build graph with the following nodes:
//    * allocation set, keeping tuple of [local, ctor call, owner function], AS
//    * local store set, keeping pair [local, stored], LSS
//    * field store set, keeping tuple [local, object to store, stored field id], FSS
//    * global store set, [local, stored global address], GSS
// Function we're trying to compute is the following:
//   for each element of AS, could it be referred by someone, whose value is
//   alive on return from function, where element was allocated.
// Each element in RS is associated with few elements in AS, which it could refer to.
//
internal class EscapeAnalyzerVisitor(
        val lifetimes: MutableMap<IrMemberAccessExpression, Lifetime>) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitModuleFragment(module: IrModuleFragment) {
        module.acceptChildrenVoid(this)
    }
}

internal fun computeLifetimes(irModule: IrModuleFragment,
                              lifetimes: MutableMap<IrMemberAccessExpression, Lifetime>) {
    assert(lifetimes.size == 0)

    irModule.acceptVoid(EscapeAnalyzerVisitor(lifetimes))
}