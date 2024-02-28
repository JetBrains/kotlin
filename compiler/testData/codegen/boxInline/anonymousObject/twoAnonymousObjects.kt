// FILE: 1.kt
interface Run {
    fun run(): String
}

inline fun i1(crossinline s: () -> String): Run {
    // KT-66217 : Unrelated crash: K/Native and K/WASM crash with SIGSEGV, should explicit `Run` type be removed from `val intermediate`.
    val intermediate: Run = object : Run {
        override fun run(): String {
            return s()
        }
    }
    return object : Run {
        override fun run(): String {
            return intermediate.run()
        }
    }
}

// FILE: 2.kt
fun box(): String {
    val i1 = i1 { "OK" }
    return i1.run()
}
