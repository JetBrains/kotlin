// TARGET_BACKEND: JVM
// FILE: Sam.java
public interface Sam  {
    String accept(Integer a);
}
// FILE: 1.kt
val Int.a : String
    get() = "OK"

val b = Sam (Int::a)

fun box(): String {
    return b.accept(1)
}