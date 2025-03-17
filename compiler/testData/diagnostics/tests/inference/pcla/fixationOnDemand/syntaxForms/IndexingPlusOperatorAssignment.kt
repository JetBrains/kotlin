// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71754

fun testRegularNavigation() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>otvOwner.instance<!NO_GET_METHOD!><!UNRESOLVED_REFERENCE!>[<!>Index<!UNRESOLVED_REFERENCE!>]<!><!><!> <!UNRESOLVED_REFERENCE!>+=<!> ScopeOwner()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>
}

fun testSafeNavigation() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>?) -> Unit): OT = null!!

    val resultA = pcla { otvOwner ->
        otvOwner?.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>otvOwner?.instance<!NO_GET_METHOD!><!UNRESOLVED_REFERENCE!>[<!>Index<!UNRESOLVED_REFERENCE!>]<!><!><!> <!UNRESOLVED_REFERENCE!>+=<!> ScopeOwner()
        // expected: Interloper </: ScopeOwner
        otvOwner?.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    val instance: T = null!!
}

interface BaseType

object Index

class ScopeOwner: BaseType {
    operator fun get(index: Index): ScopeOwner = this
    operator fun plus(other: ScopeOwner): ScopeOwner = this
    operator fun set(index: Index, value: ScopeOwner) {}
}

object Interloper: BaseType
