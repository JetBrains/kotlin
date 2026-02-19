// TARGET_BACKEND: JVM

// FILE: 1.kt
inline fun f(crossinline block: () -> String): () -> String =
    {
        object : () -> String {
            override fun invoke() = block()
        }
    }.let {
        it()
    }

// FILE: 2.kt
fun box(): String = f { "OK" }()
