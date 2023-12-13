// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// WITH_STDLIB
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// K2 status: java.lang.Integer cannot be cast to java.lang.String (inference changed due to KT-63558)

object Test1 {
    fun foo(f: () -> Unit) {}

    object Scope {
        fun foo(f: suspend () -> Unit) {}

        fun test(g: () -> Unit) {
            foo(g)
        }
    }
}

object Test2 {
    inline fun <reified T> foo(crossinline f: suspend () -> T): T = 1 as T
    fun <T> foo2(f: suspend () -> T): T = 1 as T
    suspend fun bar(): Int = 0

    object Scope {
        fun bar(): String = ""

        fun test() {
            val result = foo(::bar)
            val result2 = foo2(::bar)
        }
    }
}

object Test3 {
    inline fun <reified T> foo(crossinline f: suspend () -> T): T = "" as T
    fun <T> foo2(f: suspend () -> T): T = "" as T

    suspend fun bar(x: Int = 42): Int = 0

    object Scope {
        fun bar(x: Int = 42): String = ""

        fun test() {
            val result = foo(::bar)
            val result2 = foo2(::bar)
        }
    }
}

fun box(): String {
    Test1.Scope.test {}
    Test2.Scope.test()
    Test3.Scope.test()
    return "OK"
}
