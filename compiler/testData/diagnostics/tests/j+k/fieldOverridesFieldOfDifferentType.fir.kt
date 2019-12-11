// !CHECK_TYPE

// FILE: A.java

public class A {
    public int size = 1;
}

// FILE: B.java

public class B extends A {
    public String size = 1;
}

// FILE: main.kt

fun foo() {
    B().<!AMBIGUITY!>size<!>.<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
}
