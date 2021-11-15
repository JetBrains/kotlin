// FULL_JDK
// WITH_STDLIB
// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: J.java
public class J {
    public interface Consumer {
        void accept(String p);
    }

    public static void invokeWithNull(Consumer x) {
        x.accept(null);
    }
}

// MODULE: main(lib)
// FILE: Kotlin.kt
inline fun makeRunnable(crossinline lambda: () -> Unit) =
        object : Runnable {
            override fun run() {
                lambda()
            }
        }

fun box(): String {
    try {
        makeRunnable {
            J.invokeWithNull(object : J.Consumer {
                override fun accept(t: String) {
                    println(t)
                }
            })
        }.run()
    } catch (e: NullPointerException) {
        return "OK"
    }

    return "fail: exception expected"
}
