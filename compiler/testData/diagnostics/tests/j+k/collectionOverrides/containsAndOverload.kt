// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -PARAMETER_NAME_CHANGED_ON_OVERRIDE

// FILE: A.java
abstract public class A implements java.util.Collection<String> {
    public boolean contains(Object x) {return false;}
    public boolean contains(String x) {return false;}
}

// FILE: main.kt
abstract class <!CONFLICTING_JVM_DECLARATIONS!>KA<!> : A() {
    <!CONFLICTING_JVM_DECLARATIONS!>override fun contains(x: String)<!> = false
}

fun foo(a: A, ka: KA) {
    a.contains("")
    a.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    "" in a
    <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> in a

    ka.contains("")
    ka.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    "" in ka
    <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> in ka
}
