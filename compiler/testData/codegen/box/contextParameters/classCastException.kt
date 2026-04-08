// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters

class Receiver
object Context

interface Contract {
    context(context: Context)
    fun Receiver.foo() = "OK"
}

object Owner : Contract

fun box(): String {
    return with(Context) {
        with(Owner) {
            Receiver().foo()
        }
    }
}
