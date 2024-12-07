// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -PARAMETER_NAME_CHANGED_ON_OVERRIDE
// SCOPE_DUMP: KA:contains

// FILE: A.java
abstract public class A implements java.util.Collection<String> {
    public boolean contains(Object x) {return false;}
    public boolean contains(String x) {return false;}
}

// FILE: main.kt
abstract class KA : A()

fun foo(a: A, ka: KA) {
    ka.contains("")
    ka.contains(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    "" in ka
    <!ARGUMENT_TYPE_MISMATCH!>1<!> in ka
}
