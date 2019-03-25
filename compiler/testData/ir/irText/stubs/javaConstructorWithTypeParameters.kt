// FILE: javaConstructorWithTypeParameters.kt
// DUMP_EXTERNAL_CLASS J
fun test() = J<String>()

// FILE: J.java
public class J {
    public <T> J() {}
}