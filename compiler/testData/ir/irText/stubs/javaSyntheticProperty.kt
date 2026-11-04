// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: J
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib reads Java declarations from compiled classes, not from sources.
// FILE: javaSyntheticProperty.kt
val test = J().foo

// FILE: J.java
class J {
    public String getFoo() { return null; }
}
