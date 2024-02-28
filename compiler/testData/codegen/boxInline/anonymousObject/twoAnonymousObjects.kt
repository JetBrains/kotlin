// FILE: 1.kt
interface Run {
    fun run(): String
}

inline fun i1(crossinline s: () -> String): Run {
    val intermediate = object : Run {
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
