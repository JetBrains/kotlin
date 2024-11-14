// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
class C {
    <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
    constructor() {}
}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
fun f(): String = <!UNRESOLVED_REFERENCE!>s<!> + this@s

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(_: String)
val p: String get() = f()

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
var p2: String
    get() = <!UNRESOLVED_REFERENCE!>s<!> + this@s
    set(value) {
        <!UNRESOLVED_REFERENCE!>s<!> + this@s
    }

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: Any)
val p3: String
    get() = if (<!UNRESOLVED_REFERENCE!>s<!> is String) <!UNRESOLVED_REFERENCE!>s<!> else ""

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
fun f2() {
    length
}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(`_`: Any)
fun escapedBackTick() {}