// IGNORE_BACKEND: WASM
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Sample.java
public class Sample {
    public static int max = 1;
}

// FILE: main.kt
import kotlin.reflect.KProperty0

abstract class Checker {
    fun check(): String {
        return run(
            Sample::max,
            { x -> x == Any() }
        )
    }
    abstract fun <T1> run(method: KProperty0<T1>, fn: (T1) -> Boolean): String
}

fun box(): String {
    var result = ( object : Checker() {
        override fun <T1> run(method: KProperty0<T1>, fn: (T1) -> Boolean): String {
            return "OK"
        }
    } ).check()

    return result
}
