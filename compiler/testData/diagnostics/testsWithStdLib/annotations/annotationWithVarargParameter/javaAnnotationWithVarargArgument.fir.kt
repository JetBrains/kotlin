// !WITH_NEW_INFERENCE

// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
@A(*<!ARGUMENT_TYPE_MISMATCH!>arrayOf(1, "b")<!>)
fun test() {
}
