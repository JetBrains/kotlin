// !WITH_NEW_INFERENCE

// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
<!INAPPLICABLE_CANDIDATE!>@A(*arrayOf(1, "b"))<!>
fun test() {
}
