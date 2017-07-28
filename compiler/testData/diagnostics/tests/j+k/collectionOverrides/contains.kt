// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -PARAMETER_NAME_CHANGED_ON_OVERRIDE
// JAVAC_EXPECTED_FILE
// FILE: A.java

abstract public class A<T> implements java.util.Collection<T> {
    public boolean contains(Object x) {return false;}
}

// FILE: B.java

abstract public class B implements java.util.Collection<String> {
    public boolean contains(Object x) {return false;}
}

// FILE: IC.java
public interface IC extends java.util.Collection<String> {
    public boolean contains(Object x);
}

// FILE: main.kt
abstract class KA<T> : java.util.AbstractList<T>() {
    override fun contains(x: T) = false
}

abstract class KB : java.util.AbstractList<String>(), IC {
    override fun contains(element: String) = false
}


// Raw?

fun foo(
        a: A<String>, b: B, ic: IC,
        ka: KA<String>, kb: KB,
        al: java.util.ArrayList<String>
) {
    a.contains("")
    a.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    "" in a
    <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> in a

    b.contains("")
    b.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    "" in b
    <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> in b

    ic.contains("")
    ic.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    "" in ic
    <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> in ic

    ka.contains("")
    ka.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    "" in ka
    <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> in ka

    kb.contains("")
    kb.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    "" in kb
    <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> in kb

    al.contains("")
    al.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    "" in al
    <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> in al
}
