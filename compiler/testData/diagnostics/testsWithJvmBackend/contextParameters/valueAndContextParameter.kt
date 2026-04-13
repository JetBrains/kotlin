// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A
class B

context(a: A)
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(){}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: A){}<!>

context(a: A, b: B)
<!CONFLICTING_JVM_DECLARATIONS!>fun bar(){}<!>

context(a: A)
<!CONFLICTING_JVM_DECLARATIONS!>fun bar(b: B){}<!>

