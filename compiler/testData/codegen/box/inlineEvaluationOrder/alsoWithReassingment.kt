// FILE: lib.kt
var _value: String = "OK"

inline fun String.myAlso(f: (String) -> Unit): String {
    f(this)
    return this
}

// FILE: main.kt
fun overrideValueAndReturnOld(newValue: String) = _value.myAlso { _value = newValue }

fun box() = overrideValueAndReturnOld("fail")