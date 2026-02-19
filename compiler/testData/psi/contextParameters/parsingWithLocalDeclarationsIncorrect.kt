// LANGUAGE: +ContextParameters
// ISSUE: KT-80853, KT-81521
// COMPILATION_ERRORS

import kotlin.reflect.KClass

fun test() {
    // Other modifiers

    suspend context(fun() {}) {
        contextFun()
    }

    context(fun() {}) suspend {
        contextFun()
    }
}

context(x: Any)
fun contextFun() {}
