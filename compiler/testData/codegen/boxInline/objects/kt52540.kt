// FILE: lib.kt
interface I {
    fun result(): String
}

inline fun <T> foo(block: () -> T): T = block()

inline fun bar() = foo {
    object : I {
        override fun result()  = "OK"
    }
}

// FILE: box.kt
fun box() = bar().result()