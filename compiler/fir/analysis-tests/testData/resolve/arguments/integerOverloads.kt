// FILE: J.java
public class J {
    public static fun f(Byte x) {}
    public static fun f(Integer x) {}
    public static fun f(Long x) {}
}

// FILE: x.kt
fun f(x: Byte) {}
fun f(x: Int) {}
fun f(x: Long) {}

fun g(x: Byte?) {}
fun g(x: Int?) {}
fun g(x: Long?) {}

fun main() {
    J.f(123)
    J.f(123123123123)
    f(123)
    f(123123123123)
    g(123)
    g(123123123123)
}
