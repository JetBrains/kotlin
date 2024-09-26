// ISSUE: KT-71778

fun testRegularNavigation() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        <!UNRESOLVED_REFERENCE!>++<!>otvOwner.mutableReference
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        ++<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>otvOwner.immutableReference[Index]<!>
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>
}

fun testSafeNavigation() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>?) -> Unit): OT = null!!

    val resultA = pcla { otvOwner ->
        otvOwner?.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        <!UNRESOLVED_REFERENCE!>++<!>otvOwner?.mutableReference
        // expected: Interloper </: ScopeOwner
        otvOwner?.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner?.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        ++<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>otvOwner?.immutableReference[Index]<!>
        // expected: Interloper </: ScopeOwner
        otvOwner?.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    var mutableReference: T = null!!
    val immutableReference: T = null!!
}

interface BaseType

object Index

class ScopeOwner: BaseType {
    operator fun inc(): ScopeOwner = this
    operator fun get(index: Index): ScopeOwner = this
    operator fun set(index: Index, value: ScopeOwner) {}
}

object Interloper: BaseType
