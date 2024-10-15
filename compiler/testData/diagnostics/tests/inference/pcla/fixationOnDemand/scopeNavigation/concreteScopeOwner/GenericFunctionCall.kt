// RUN_PIPELINE_TILL: FRONTEND
fun testStandardNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>memberFunction<!>(TypeArgument)
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.extensionFunction(TypeArgument)
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!TYPE_MISMATCH, TYPE_MISMATCH!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.fix()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultC<!>
}

fun testSafeNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // should fix OTv := ScopeOwner? for scope navigation
        otvOwner.provide()?.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>memberFunction<!>(TypeArgument)
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType?")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // should fix OTv := ScopeOwner? for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>?.extensionFunction(TypeArgument)
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(<!TYPE_MISMATCH, TYPE_MISMATCH!>Interloper<!>)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner?")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // should fix OTv := ScopeOwner? for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>?.fix()
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType?")!>resultC<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun <A> memberFunction(arg: A) {}
    companion object {
        fun Nullable(): ScopeOwner? = null
    }
}

fun <B> ScopeOwner.extensionFunction(arg: B) {}

object TypeArgument

fun <C> C.fix() {}

object Interloper: BaseType
