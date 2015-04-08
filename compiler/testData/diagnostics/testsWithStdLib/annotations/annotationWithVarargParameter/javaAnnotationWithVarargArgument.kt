// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
[A(*<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH(kotlin.Array<out kotlin.String>; IGNORE)!>array<!>(1, "b"))]
fun test() {
}
