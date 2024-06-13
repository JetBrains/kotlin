// DIAGNOSTICS: -UNUSED_PARAMETER -PARAMETER_NAME_CHANGED_ON_OVERRIDE
// FILE: A.java
abstract public class A implements java.util.Collection<String> {
    public boolean contains(Object x) {return false;}
    public boolean contains(String x) {return false;}
}

// FILE: main.kt
abstract class KA : A() {
    override fun <!ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE!>contains<!>(x: String) = false
}
