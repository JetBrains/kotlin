// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otv1Owner ->
        val nestedResultA = pcla { otv2Owner ->
            otv1Owner.constrain(ScopeOwnerA())
            // should fix OTv1 := ScopeOwnerA for scope navigation
            otv1Owner.provide().functionA()
            // expected: Interloper </: ScopeOwnerA
            otv1Owner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwnerA")!>Interloper<!>)

            otv2Owner.constrain(ScopeOwnerB())
            // should fix OTv2 := ScopeOwnerB for scope navigation
            otv2Owner.provide().functionB()
            // expected: Interloper </: ScopeOwnerA
            otv2Owner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwnerB")!>Interloper<!>)
        }
        // expected: ScopeOwnerB
        <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwnerB")!>nestedResultA<!>
    }
    // expected: ScopeOwnerA
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwnerA")!>resultA<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Any?): OT = null!!

interface BaseType

class ScopeOwnerA: BaseType {
    fun functionA() {}
}

class ScopeOwnerB: BaseType {
    fun functionB() {}
}

object Interloper: BaseType
