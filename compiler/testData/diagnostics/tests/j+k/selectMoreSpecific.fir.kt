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
    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>() <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><<!CANNOT_INFER_PARAMETER_TYPE!>String<!>>() }
    A.<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>() <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><<!CANNOT_INFER_PARAMETER_TYPE!>String<!>>() }
}
