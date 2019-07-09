// FILE: 1.kt

inline fun <T> run(c: () -> T): T = c()

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING

interface Runnable {
    fun run(): String
}

interface RunnableString {
    fun run(s: String): String
}

fun fooInt(): String {
    val r = "O"
    val a = run {
        fun f(x: Int, y: String? = null): String = r + x + y
        f(1, "K")
    }
    return a
}

fun fooLong(): String {
    val r = "O"
    val a = run {
        fun f(x: Long, y: String? = null): String = r + x + y
        f(2, "K")
    }
    return a
}

fun fooLongInsideObject(): String {
    val r = "O"
    val a = object: Runnable {
        override fun run(): String {
            fun f(x: Long, y: String? = null): String = r + x + y
            return f(3, "K")
        }
    }
    return a.run()
}

fun fooLongCallableReference(): String {
    val r = "O"
    val a = run {
        fun f(x: Long, y: String? = null): String = r + x + y
        (::f)(4, "K")
    }
    return a
}

class A {
    fun fooLongSyntheticAccessor(capt: Int): String {
        val o: RunnableString = run {
            object: RunnableString {
                override fun run(captured: String): String {
                    return {
                        callPrivate(capt, captured)
                    }()
                }

                private fun callPrivate(x: Int, y: String?): String = "O" + x + y
            }
        }
        return o.run("K")
    }
}

fun box(): String {
    var res = fooInt()
    if (res != "O1K") return res
    res = fooLong()
    if (res != "O2K") return res
    res = fooLongInsideObject()
    if (res != "O3K") return res
    res = fooLongCallableReference()
    if (res != "O4K") return res
    res = A().fooLongSyntheticAccessor(5)
    if (res != "O5K") return res
    return "OK"
}
