// !WITH_NEW_INFERENCE

// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
@A(*<!NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>arrayOf(1, "b")<!>)
fun test() {
}
