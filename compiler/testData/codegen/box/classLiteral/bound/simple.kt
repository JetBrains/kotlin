// IGNORE_BACKEND: JS_IR

fun box(): String {
    val x: CharSequence = ""
    val klass = x::class
    return if (klass == String::class) "OK" else "Fail: $klass"
}
