// IGNORE_BACKEND: WASM
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Sample.java
public class Sample {
    public static int max(int x, int y) {
        if (x > y) {
            return x;
        } else {
            return y;
        }
    }
}

// FILE: main.kt

import kotlin.reflect.KFunction2

abstract class Checker {
    fun check(): String {
        return run(
            Sample::max,
            { x, y -> x > y }
        )
    }
    abstract fun <T1, T2, R> run(method: KFunction2<T1, T2, R>, fn: (T1, T2) -> Boolean): String
}

fun box(): String {
    var result = ( object : Checker() {
        override fun <T1, T2, R> run(method: KFunction2<T1, T2, R>, fn: (T1, T2) -> Boolean): String {
            return "OK"
        }
    } ).check()

    return result
}
