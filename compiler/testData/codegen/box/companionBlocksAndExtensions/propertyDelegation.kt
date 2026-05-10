// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
// IGNORE_BACKEND: WASM_JS, WASM_WASI, JS_IR, JS_IR_ES6
object Delegate {
    var value = ""
    operator fun getValue(a: Any?, b: Any?) = value
    operator fun setValue(a: Any?, b: Any?, v: String) {
        value = v
    }
}

object DelegateProvider {
    operator fun provideDelegate(a: Any?, b: Any?) = Delegate
}

class C {
    companion {
        var a by Delegate
        var b by DelegateProvider
    }
}

companion var C.c by Delegate
companion var C.d by DelegateProvider

fun box(): String {
    C.a = "a"
    if (C.a != "a") return "FAIL 1"

    C.b = "b"
    if (C.b != "b") return "FAIL 2"

    C.c = "c"
    if (C.c != "c") return "FAIL 3"

    C.d = "d"
    if (C.d != "d") return "FAIL 4"

    return "OK"
}
