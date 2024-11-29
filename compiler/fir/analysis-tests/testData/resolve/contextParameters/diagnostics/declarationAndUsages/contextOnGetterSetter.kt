// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

<!MUST_BE_INITIALIZED!>val a: String<!>
    context(c: A)
    <!SYNTAX!>get<!><!SYNTAX!>(<!><!SYNTAX!>)<!> <!SYNTAX!>=<!> <!SYNTAX!>"<!><!SYNTAX!>"<!>

<!MUST_BE_INITIALIZED!>var b: String<!>
    get() = ""
    context(c: A)
    <!SYNTAX!>set<!><!SYNTAX!>(<!>value<!SYNTAX!>)<!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{ }<!>
