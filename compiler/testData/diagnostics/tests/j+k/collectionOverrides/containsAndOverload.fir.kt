// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -PARAMETER_NAME_CHANGED_ON_OVERRIDE
// SCOPE_DUMP: KA:contains

// FILE: A.java
abstract public class A implements java.util.Collection<String> {
    public boolean contains(Object x) {return false;}
    public boolean contains(String x) {return false;}
}

// FILE: main.kt
abstract class KA : A() {
    override fun contains(x: String) = false
}

fun foo(a: A, ka: KA) {
    a.contains("")
    a.contains(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    "" in a
    <!ARGUMENT_TYPE_MISMATCH!>1<!> in a

    ka.<!UNRESOLVED_REFERENCE!>contains<!>("")
    ka.<!UNRESOLVED_REFERENCE!>contains<!>(1)
    "" <!UNRESOLVED_REFERENCE!>in<!> ka
    1 <!UNRESOLVED_REFERENCE!>in<!> ka
}
