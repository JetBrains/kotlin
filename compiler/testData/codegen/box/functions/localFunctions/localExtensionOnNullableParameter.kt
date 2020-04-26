open class T(var value: Int) {}

fun localExtensionOnNullableParameter(): T {

    fun T.local(s: Int) {
        value += s
    }

    var t: T? = T(1)
    t?.local(2)

    return t!!
}


fun box(): String {
    val result = localExtensionOnNullableParameter().value
    if (result != 3) return "fail 2: $result"

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT