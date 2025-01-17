// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

val a: String
    <!UNSUPPORTED!>context(c: A)<!>
    get() = ""

<!MUST_BE_INITIALIZED!>var <!REDECLARATION!>b<!>: String<!>
    get() = ""

var <!REDECLARATION!>b<!>: String
    get() = ""
    <!UNSUPPORTED!>context(c: A)<!>
    set(value) { }
