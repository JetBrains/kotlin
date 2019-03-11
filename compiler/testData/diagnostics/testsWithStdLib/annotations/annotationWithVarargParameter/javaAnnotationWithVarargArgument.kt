// !WITH_NEW_INFERENCE

// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
@A(*<!NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>arrayOf(<!NI;CONSTANT_EXPECTED_TYPE_MISMATCH, NI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, "b")<!>)
fun test() {
}
