// V8 Crash https://bugs.chromium.org/p/v8/issues/detail?id=12640
// IGNORE_BACKEND: WASM

class C {
    fun calc() : String {
        return "OK"
    }
}

fun box(): String? {
    val c: C? = C()
    val arrayList = arrayOf(c?.calc(), "")
    return arrayList[0]
}
