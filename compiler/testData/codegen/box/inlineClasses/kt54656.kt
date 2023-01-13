// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// FILE: J.java
public class J {
    public J(Email email) {}
}

// FILE: 1.kt
@JvmInline
value class Email(val address: String)

fun box():String {
    J(Email("test"))
    return "OK"
}

