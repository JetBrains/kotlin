// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: kt44993.kt
fun box(): String =
    f(KotlinBox(JavaBox()))

fun f(r: KotlinBox<JavaBox>): String =
    r?.data?.element!!

class KotlinBox<T>(@JvmField val data: T?)

// FILE: JavaBox.java
public class JavaBox {
    public final String element = "OK";
}
