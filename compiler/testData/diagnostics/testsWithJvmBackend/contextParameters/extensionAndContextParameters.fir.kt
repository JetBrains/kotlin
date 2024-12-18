// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A
class B

<!CONFLICTING_JVM_DECLARATIONS!>context(a: A)
fun foo() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun A.foo() { }<!>

<!CONFLICTING_JVM_DECLARATIONS!>context(a: A, b: B)
fun bar(){}<!>

<!CONFLICTING_JVM_DECLARATIONS!>context(a: A)
fun B.bar(){}<!>

<!CONFLICTING_JVM_DECLARATIONS!>context(a: A, b: B)
fun qux(){}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun A.qux(b: B){}<!>

context(a: A)
val b: String
    <!CONFLICTING_JVM_DECLARATIONS!>get() = ""<!>

val A.b: String
    <!CONFLICTING_JVM_DECLARATIONS!>get() = ""<!>