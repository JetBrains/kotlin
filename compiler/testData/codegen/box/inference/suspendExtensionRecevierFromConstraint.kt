// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: COROUTINES
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// !LANGUAGE: +NewInference

class ExtensionReceiver
typealias SuspendExtensionFunction = suspend ExtensionReceiver.() -> Unit
suspend fun ExtensionReceiver.extensionMethod() {}

fun test() {
    val map = mutableMapOf<Unit, SuspendExtensionFunction>()

    map[Unit] = {
        extensionMethod()
    }
}

fun box(): String {
    test()
    return "OK"
}
