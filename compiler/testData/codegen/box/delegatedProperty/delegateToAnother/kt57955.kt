// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: box.kt

class C {
    val x: Int? by J().s::length
}

fun box(): String =
    if (C().x == 0) "OK" else "Fail"

// FILE: J.java

public class J {
    public final String s = "";
}
