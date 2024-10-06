fun test() {
    val resultA = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>pcla<!> { otv1Owner ->
        val nestedResultA = pcla { otv2Owner ->
            otv1Owner.constrain(ScopeOwnerA())
            // should fix OTv1 := ScopeOwnerA for scope navigation
            otv1Owner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>functionA<!>()
            // expected: Interloper </: ScopeOwnerA
            otv1Owner.constrain(Interloper)

            otv2Owner.constrain(ScopeOwnerB())
            // should fix OTv2 := ScopeOwnerB for scope navigation
            otv2Owner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>functionB<!>()
            // expected: Interloper </: ScopeOwnerA
            otv2Owner.constrain(Interloper)
        }
        // expected: ScopeOwnerB
        <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>nestedResultA<!>
    }
    // expected: ScopeOwnerA
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_EXPRESSION_TYPE("[Error type: Not found recorded type for pcla { otv1Owner ->        val nestedResultA = pcla { otv2Owner ->            otv1Owner.constrain(ScopeOwnerA())            // should fix OTv1 := ScopeOwnerA for scope navigation            otv1Owner.provide().functionA()            // expected: Interloper </: ScopeOwnerA            otv1Owner.constrain(Interloper)            otv2Owner.constrain(ScopeOwnerB())            // should fix OTv2 := ScopeOwnerB for scope navigation            otv2Owner.provide().functionB()            // expected: Interloper </: ScopeOwnerA            otv2Owner.constrain(Interloper)        }        // expected: ScopeOwnerB        nestedResultA    }]")!>resultA<!>
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
