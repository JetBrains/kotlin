// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A
class B

<!CONFLICTING_JVM_DECLARATIONS!>context(a: A)
fun foo(){}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: A){}<!>

<!CONFLICTING_JVM_DECLARATIONS!>context(a: A, b: B)
fun bar(){}<!>

<!CONFLICTING_JVM_DECLARATIONS!>context(a: A)
fun bar(b: B){}<!>

