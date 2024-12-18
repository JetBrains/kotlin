// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-51258
// LANGUAGE: +ContextParameters

annotation class Ann

context(a: String)
@Ann
fun foo(): String {
    return a
}

@Ann
<!SYNTAX!>context<!><!SYNTAX!>(<!><!SYNTAX!>a<!><!SYNTAX!>:<!> <!SYNTAX!>String<!><!SYNTAX!>)<!>
fun bar() { }

context(a: String)
@Ann
val qux : String
    get() = ""

@Ann
<!SYNTAX!>context<!><!SYNTAX!>(<!><!SYNTAX!>a<!><!SYNTAX!>:<!> <!SYNTAX!>String<!><!SYNTAX!>)<!>
val buz : String
    get() = ""