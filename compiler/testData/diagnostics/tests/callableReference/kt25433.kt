// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// Issue: KT-25433

import kotlin.reflect.*

fun <T, R> hidden(nameProp: KProperty1<T, R>, value: R) {}
fun <T, R> hiddenFun(nameFunc: KFunction1<T, R>, value: R) {}

class App(val nullable: String?) {
    fun nullableFun(): String? = null
}

fun test() {
    hidden(App::nullable, "foo")
    hiddenFun(App::nullableFun, "foo")
}