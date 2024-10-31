// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
class C {
    <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
    constructor() {}
}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
fun f(): String = s + this@s

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(_: String)
val p: String get() = f()

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
var p2: String
    get() = s + this@s
    set(value) {
        s + this@s
    }

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: Any)
val p3: String
    get() = if (s is String) s else ""

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
fun f2() {
    length
}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
fun f3() = s.length

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(s: String)
val p4 get() = s

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(`_`: Any)
fun escapedBackTick() {}