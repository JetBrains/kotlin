// ISSUE: KT-71845

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // expected: CapturedType(*)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>otvOwner.provide()<!>
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>
}


class TypeVariableOwner<T, CT: T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): CT = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT, *>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
