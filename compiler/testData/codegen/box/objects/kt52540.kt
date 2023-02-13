// MODULE: lib
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

// MODULE: main(lib)
// FILE: box.kt
fun box() = bar().result()