// IGNORE_BACKEND: JVM_IR
// FILE: J.java
// FULL_JDK
// WITH_RUNTIME
// TARGET_BACKEND: JVM
public class J {
    public interface Consumer {
        void accept(String p);
    }

    public static void invokeWithNull(Consumer x) {
        x.accept(null);
    }
}

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
    } catch (e: IllegalArgumentException) {
        return "OK"
    }

    return "fail: exception expected"
}