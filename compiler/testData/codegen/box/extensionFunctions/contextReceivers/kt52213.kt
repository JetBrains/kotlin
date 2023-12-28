// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WITH_COROUTINES

interface Context

class Receiver

interface Action {
    context (Context)
    fun run()
}

fun execute(block: suspend context(Context) Receiver.(Action) -> Unit) = Unit

fun box(): String {
    execute { it.run() }
    return "OK"
}