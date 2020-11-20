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
    a.<!INAPPLICABLE_CANDIDATE!>contains<!>(1)
    "" in a
    1 <!INAPPLICABLE_CANDIDATE!>in<!> a

    b.contains("")
    b.contains(1)
    "" in b
    1 in b

    ic.contains("")
    ic.contains(1)
    "" in ic
    1 in ic

    ka.contains("")
    ka.<!INAPPLICABLE_CANDIDATE!>contains<!>(1)
    "" in ka
    1 <!INAPPLICABLE_CANDIDATE!>in<!> ka

    kb.contains("")
    kb.contains(1)
    "" in kb
    1 in kb

    al.contains("")
    al.<!INAPPLICABLE_CANDIDATE!>contains<!>(1)
    "" in al
    1 <!INAPPLICABLE_CANDIDATE!>in<!> al
}
