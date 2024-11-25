// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

<!UNSUPPORTED!>context(<!REDECLARATION!>s<!>: String)<!>
class C {
    <!UNSUPPORTED!>context(<!REDECLARATION!>s<!>: String)<!>
    constructor() {}

    context(s: String)
    fun f(){}

    context(_: String)
    val p: String get() = ""
}

context(s: String)
fun f(){}

context(_: String)
val p: String get() = ""
