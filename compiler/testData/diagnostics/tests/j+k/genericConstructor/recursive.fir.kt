// !WITH_NEW_INFERENCE
// FILE: C.java

// See KT-10410
public class C {
    public <T extends T> C(T t) {
    }
}

// FILE: main.kt

fun foo() = <!INAPPLICABLE_CANDIDATE!>C<!>()
