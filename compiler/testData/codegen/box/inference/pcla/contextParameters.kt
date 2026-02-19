// ISSUE: KT-67699
// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

class Controller<E>(val e: E)

fun <E, A> either(a: Any?, block: Controller<E>.() -> A): A = Controller(a as E).block()

context(c: Controller<String>)
fun findUser(): String = c.e

context(c: Controller<String>)
val prop: String get() = c.e

fun box(): String {
    val x1 = either("O") {
        findUser()
    }

    val x2 = either("K") {
        prop
    }

    return x1 + x2
}
