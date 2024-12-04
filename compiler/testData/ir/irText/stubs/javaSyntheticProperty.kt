// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// DUMP_EXTERNAL_CLASS: J
// FILE: javaSyntheticProperty.kt
val test = J().foo

// FILE: J.java
class J {
    public String getFoo() { return null; }
}
