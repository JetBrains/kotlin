// !DUMP_DEPENDENCIES
// FILE: javaSyntheticProperty.kt
val test = J().foo

// FILE: J.java
class J {
    public String getFoo() { return null; }
}