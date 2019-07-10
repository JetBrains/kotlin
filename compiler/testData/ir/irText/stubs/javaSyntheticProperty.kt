// !DUMP_DEPENDENCIES
// FILE: javaSyntheticProperty.kt
// FIR_IDENTICAL
val test = J().foo

// FILE: J.java
class J {
    public String getFoo() { return null; }
}