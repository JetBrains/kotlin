// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_LAZY
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

// KT-40216

inline fun <T> T.also(f: (T) -> Unit): T {
    f(this)
    return this
}

object FieldTest {

    var result = ""

    private val test = object {
        fun bar() = object {
            fun qux() = object {
                fun biq() = object {
                    fun caz() = object {
                    }.also { result += "d" }
                }.also { result += "c" }
            }.also { result += "b" }
        }.also { result += "a" }
    }.also { result += "!" }

    private val ttt = test.bar()

    private val qqq = ttt.qux()

    val bbb = qqq.biq().also { it.caz() }
}

object FunTest {

    var result = ""

    private fun bar() = object {
        fun qux() = object {
            fun biq() = object {
                fun caz() = object {
                }.also { result += "w" }
            }.also { result += "x" }
        }.also { result += "y" }
    }.also { result += "z" }

    private fun ttt() = bar()

    private fun qqq() = ttt().qux()

    fun bbb() = qqq().biq().also { it.caz() }
}

object DelegateTest {
    var result = ""

    val f by lazy {
        object { }.also { result += "OK" }
    }

    fun bbb() = f
}

// MODULE: lib2(lib)
// FILE: lib2.kt
fun test1(): String {
    return FieldTest.result
}

fun test2(): String {
    FunTest.bbb()
    return FunTest.result
}

fun test3(): String {
    DelegateTest.bbb()
    return DelegateTest.result
}

// MODULE: main(lib2)
// FILE: main.kt

fun box(): String {
    if (test1() != "!abcd") return "FAIL 1: ${test1()}"
    if (test2() != "zyxw") return "FAIL 2: ${test2()}"
    return test3()
}
