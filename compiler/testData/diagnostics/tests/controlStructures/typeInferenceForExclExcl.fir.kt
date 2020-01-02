// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// FILE: A.java
public class A {
    public static String foo() {
        return "";
    }
}

// FILE: b.kt
fun <T: Any> exclExcl(t: T?): T = t!!

fun test11() {
    // not 'String!'
    exclExcl(A.foo()) checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    exclExcl(A.foo()) checkType { <!UNRESOLVED_REFERENCE!>_<!><String?>() }

    // not 'String!'
    A.foo()!! checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    A.foo()!! checkType { <!UNRESOLVED_REFERENCE!>_<!><String?>() }
}