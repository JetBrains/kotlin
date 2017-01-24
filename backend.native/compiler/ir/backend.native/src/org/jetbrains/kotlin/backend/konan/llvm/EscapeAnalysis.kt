package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

// Analysis we're implementing here is as following.
// We build graph with the following nodes:
//    * allocation set, keeping tuple of [local, ctor call, owner function], AS
//    * local store set, keeping pair [local, stored], LSS
//    * field store set, keeping tuple [local, stored], FSS
//    * global store set, [local, stored], GSS
// Function we're trying to compute is the following:
//   for each element of AS, could it be referred by someone, whose value is
//   alive on return from function, where element was allocated.
// Each element in RS is associated with few elements in AS, which it could refer to.
// TODO: exact algorithm TBD.
internal class EscapeAnalyzerVisitor(val allocHints: MutableMap<IrCall, Int>) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitModuleFragment(module: IrModuleFragment) {
        module.acceptChildrenVoid(this)
    }
}

fun prepareAllocHints(irModule: IrModuleFragment, allocHints: MutableMap<IrCall, Int>) {
    assert(allocHints.size == 0)

    irModule.acceptVoid(EscapeAnalyzerVisitor(allocHints))
}