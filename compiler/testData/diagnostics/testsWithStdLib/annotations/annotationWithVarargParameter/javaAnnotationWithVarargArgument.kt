// !WITH_NEW_INFERENCE

// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
@A(*<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{NI}!>arrayOf(<!CONSTANT_EXPECTED_TYPE_MISMATCH{NI}!>1<!>, "b")<!>)
fun test() {
}
