// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// WITH_RUNTIME
// KT-44496

class C {
    val todo: String = TODO()

    val uninitializedVal: String

    var uninitializedVar: String
}

fun box(): String =
    try {
        C()
        "Fail"
    } catch (e: NotImplementedError) {
        "OK"
    }
