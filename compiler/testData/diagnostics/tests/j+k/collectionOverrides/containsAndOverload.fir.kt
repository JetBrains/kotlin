// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -PARAMETER_NAME_CHANGED_ON_OVERRIDE

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
    a.<!AMBIGUITY!>contains<!>("")
    a.<!NONE_APPLICABLE!>contains<!>(1)
    "" <!AMBIGUITY!>in<!> a
    1 <!NONE_APPLICABLE!>in<!> a

    ka.contains("")
    ka.contains(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    "" in ka
    <!ARGUMENT_TYPE_MISMATCH!>1<!> in ka
}
