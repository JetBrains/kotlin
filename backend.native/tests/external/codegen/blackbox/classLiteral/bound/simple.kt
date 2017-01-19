// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun box(): String {
    val x: CharSequence = ""
    val klass = x::class
    return if (klass == String::class) "OK" else "Fail: $klass"
}
