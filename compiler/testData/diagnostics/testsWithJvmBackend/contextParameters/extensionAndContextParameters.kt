// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A
class B

context(a: A)
<!CONFLICTING_JVM_DECLARATIONS!>fun foo() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun A.foo() { }<!>

context(a: A, b: B)
<!CONFLICTING_JVM_DECLARATIONS!>fun bar(){}<!>

context(a: A)
<!CONFLICTING_JVM_DECLARATIONS!>fun B.bar(){}<!>

context(a: A, b: B)
<!CONFLICTING_JVM_DECLARATIONS!>fun qux(){}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun A.qux(b: B){}<!>

context(a: A)
val b: String
    <!CONFLICTING_JVM_DECLARATIONS!>get() = ""<!>

val A.b: String
    <!CONFLICTING_JVM_DECLARATIONS!>get() = ""<!>
