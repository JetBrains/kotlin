// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

fun takeFnToAny(fn: () -> Any) {}
fun takeFnToUnit(fn: () -> Unit) {}
fun <P> takeFnToParameter(fn: () -> P) {}

fun testAny() {
    takeFnToAny {  }
    takeFnToAny { Unit }
    takeFnToAny { <!UNRESOLVED_REFERENCE!>unresolved<!>() }
    takeFnToAny { if (true) <!UNRESOLVED_REFERENCE!>unresolved<!>() }
    takeFnToAny { if (true) <!UNRESOLVED_REFERENCE!>unresolved<!>() else <!UNRESOLVED_REFERENCE!>unresolved<!>() }
    takeFnToAny(fun() = Unit)
    takeFnToAny(fun() {})
    takeFnToAny(fun() { return })
    takeFnToAny(fun() { return Unit })
    takeFnToAny(fun(): Unit {})
    takeFnToAny(fun(): Unit { return })
    takeFnToAny(fun(): Unit { return Unit })
    takeFnToAny(fun() { if (true) return })
    takeFnToAny(fun() { if (true) return Unit })
    takeFnToAny(fun() = <!UNRESOLVED_REFERENCE!>unresolved<!>())
    takeFnToAny(fun() { <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToAny(fun(): Unit { <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToAny(fun() { return <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToAny(fun() { if (true ) return <!UNRESOLVED_REFERENCE!>unresolved<!>() })
}

fun testUnit() {
    takeFnToUnit {  }
    takeFnToUnit { Unit }
    takeFnToUnit { <!UNRESOLVED_REFERENCE!>unresolved<!>() }
    takeFnToUnit { if (true) <!UNRESOLVED_REFERENCE!>unresolved<!>() }
    takeFnToUnit { if (true) <!UNRESOLVED_REFERENCE!>unresolved<!>() else <!UNRESOLVED_REFERENCE!>unresolved<!>() }
    takeFnToUnit(fun() = Unit)
    takeFnToUnit(fun() {})
    takeFnToUnit(fun() { return })
    takeFnToUnit(fun() { return Unit })
    takeFnToUnit(fun(): Unit {})
    takeFnToUnit(fun(): Unit { return })
    takeFnToUnit(fun(): Unit { return Unit })
    takeFnToUnit(fun() { if (true) return })
    takeFnToUnit(fun() { if (true) return Unit })
    takeFnToUnit(fun() = <!UNRESOLVED_REFERENCE!>unresolved<!>())
    takeFnToUnit(fun() { <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToUnit(fun(): Unit { <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToUnit(fun() { return <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToUnit(fun() { if (true ) return <!UNRESOLVED_REFERENCE!>unresolved<!>() })
}

fun testParameter() {
    takeFnToParameter {  }
    takeFnToParameter { Unit }
    takeFnToParameter { <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>unresolved<!>()<!> }
    takeFnToParameter { if (true) <!UNRESOLVED_REFERENCE!>unresolved<!>() }
    takeFnToParameter {
        if (true) <!UNRESOLVED_REFERENCE!>unresolved<!>() else <!UNRESOLVED_REFERENCE!>unresolved<!>()
    }
    takeFnToParameter(fun() = Unit)
    takeFnToParameter(fun() {})
    takeFnToParameter(fun() { return })
    takeFnToParameter(fun() { return Unit })
    takeFnToParameter(fun(): Unit {})
    takeFnToParameter(fun(): Unit { return })
    takeFnToParameter(fun(): Unit { return Unit })
    takeFnToParameter(fun() { if (true) return })
    takeFnToParameter(fun() { if (true) return Unit })
    takeFnToParameter(fun() = <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>unresolved<!>()<!>)
    takeFnToParameter(fun() { <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToParameter(fun(): Unit { <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToParameter(fun() { return <!UNRESOLVED_REFERENCE!>unresolved<!>() })
    takeFnToParameter(fun() { if (true ) return <!UNRESOLVED_REFERENCE!>unresolved<!>() })
}
