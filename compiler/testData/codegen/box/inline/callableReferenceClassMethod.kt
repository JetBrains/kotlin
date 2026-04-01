// TARGET_BACKEND: JS_IR, JS_IR_ES6

// FILE: lib.kt
typealias Callback = () -> Unit

class CallbackComposer {
    inline fun addTo(noinline block: Callback) {
        asDynamic().push(block)
    }
}

inline fun CallbackComposer.addToExtension(noinline block: Callback) {
    asDynamic().push(block)
}

// FILE: main.kt
fun createCallbackBuilder(builder: CallbackComposer.() -> Unit): () -> Callback = {
    val callbacks = arrayOf<Callback>()
    builder(callbacks.unsafeCast<CallbackComposer>())

    val composed: Callback = {
        for (cb in callbacks) {
            cb()
        }
    }
    composed
}

var retVal = ""

fun appendToRetVal(c: String): () -> Unit = {
    retVal += c
}

fun box(): String {
    val callbackBuilder = createCallbackBuilder {
        appendToRetVal("O").also(::addTo)
        appendToRetVal("K").also(::addToExtension)
    }

    val callback = callbackBuilder()
    callback()

    return retVal
}
