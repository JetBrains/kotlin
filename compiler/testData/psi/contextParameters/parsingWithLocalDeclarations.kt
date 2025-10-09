// LANGUAGE: +ContextParameters
// ISSUE: KT-80853, KT-81521
// COMPILATION_ERRORS

import kotlin.reflect.KClass

fun test() {
    // Class reference

    context(String::class) {
        contextFun()
    }

    // Anonymous function

    context(fun() {}) {
        contextFun()
    }

    // Mixed with other arguments

    context(1, fun() {}) {
        contextFun2()
    }

    context(fun() {}, 3) {
        contextFun3()
    }

    context(Int::class, fun() {}) {
        contextFun4()
    }

    // Recursion

    context(context(fun () = "str") { contextFun() }) { contextFun() }

    context(context(Int::class) { contextFun() }) { contextFun() }
}

context(x: Any)
fun contextFun() {}

context(x: Int, y: () -> Unit)
fun contextFun2() {}

context(x: () -> Unit, y: Int)
fun contextFun3() {}

context(x: KClass<*>, y: () -> Unit)
fun contextFun4() {}