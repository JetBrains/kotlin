<!CONFLICTING_JVM_DECLARATIONS!>fun foo(vararg <!UNUSED_PARAMETER!>x<!>: Int)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(<!UNUSED_PARAMETER!>x<!>: IntArray)<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(vararg <!UNUSED_PARAMETER!>x<!>: Int?)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(<!UNUSED_PARAMETER!>x<!>: Array<Int>)<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(vararg <!UNUSED_PARAMETER!>nn<!>: Number)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(<!UNUSED_PARAMETER!>nn<!>: Array<out Number>)<!> {}
