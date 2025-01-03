// RUN_PIPELINE_TILL: FRONTEND
fun testStandardNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().memberFunction()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().extensionFunction()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultB<!>
}

fun testSafeNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // should fix OTv := ScopeOwner? for scope navigation
        otvOwner.provide()?.memberFunction()
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner?")!>Interloper<!>)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner?")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // should fix OTv := ScopeOwner? for scope navigation
        otvOwner.provide()?.extensionFunction()
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner?")!>Interloper<!>)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner?")!>resultB<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun memberFunction() {}
    companion object {
        fun Nullable(): ScopeOwner? = null
    }
}

fun ScopeOwner.extensionFunction() {}

object Interloper: BaseType
