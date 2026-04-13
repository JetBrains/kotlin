// LANGUAGE: +ContextParameters
// IGNORE_BACKEND: ANDROID
// WITH_STDLIB
// WITH_COROUTINES

interface Context

class Receiver

interface Action {
    context(_: Context)
    fun run()
}

fun execute(block: suspend context(Context) Receiver.(Action) -> Unit) = Unit

fun box(): String {
    execute { it.run() }
    return "OK"
}
