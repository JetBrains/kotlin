// TARGET_BACKEND: JVM
// FILE: Sam.java
public interface Sam  {
    String accept(String a);
}
// FILE: 1.kt
fun String.foo(): String {
    return this
}
val a = Sam (String::foo)

fun box(): String {
    return a.accept("OK")
}