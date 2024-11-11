// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

<!UNSUPPORTED!>context(<!REDECLARATION!>s<!>: String)<!>
class C {
    <!UNSUPPORTED!>context(<!REDECLARATION!>s<!>: String)<!>
    constructor() {}
}

context(s: String)
fun f(): String = s + this<!UNRESOLVED_LABEL!>@s<!>

context(_: String)
val p: String get() = f()

context(s: String)
var p2: String
    get() = s + this<!UNRESOLVED_LABEL!>@s<!>
    set(value) {
        s + this<!UNRESOLVED_LABEL!>@s<!>
    }

context(s: Any)
val p3: String
    get() = if (s is String) s + f() else ""

context(s: String)
fun f2() {
    <!UNRESOLVED_REFERENCE!>length<!>
}

context(s: String)
fun f3() = s.length

context(s: String)
val p4 get() = s

context(`_`: Any)
fun escapedBackTick() {}
