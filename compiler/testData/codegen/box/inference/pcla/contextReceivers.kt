// FIR_IDENTICAL
// ISSUE: KT-67699
// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class Controller<E>(val e: E)

fun <E, A> either(a: Any?, block: Controller<E>.() -> A): A = Controller(a as E).block()

context(Controller<String>)
fun findUser(): String = e

context(Controller<String>)
val prop: String get() = e

fun box(): String {
    val x1 = either("O") {
        findUser()
    }

    val x2 = either("K") {
        prop
    }

    return x1 + x2
}
