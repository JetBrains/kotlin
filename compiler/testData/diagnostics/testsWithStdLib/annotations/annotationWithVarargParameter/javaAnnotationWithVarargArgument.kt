// !WITH_NEW_INFERENCE

// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
@A(*<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH("Array<out String>", "IGNORE")!>arrayOf(1, "b")<!>)
fun test() {
}
