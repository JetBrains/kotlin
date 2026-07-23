// LANGUAGE: +CompanionBlocks +CompanionExtensions
// IGNORE_BACKEND: JS_IR JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR

enum class E

class C {
    companion {
        fun values() = E.values()
        val entries = E.entries
    }
}

companion fun C.valueOf(x: String) = x

fun box(): String {
    if (C.values().size != 0) return "fail: values"
    if (C.entries.size != 0) return "fail: entries"
    return C.valueOf("OK")
}
