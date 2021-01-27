// !CHECK_TYPE
// FILE: A.java
public class A {
    public String foo() {}
    public CharSequence foo() {}

    public static String bar() {}
    public static CharSequence bar() {}
}

// FILE: main.kt

fun foo(a: A) {
    a.<!AMBIGUITY!>foo<!>() <!UNSAFE_CALL!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    A.<!AMBIGUITY!>bar<!>() <!UNSAFE_CALL!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
}
