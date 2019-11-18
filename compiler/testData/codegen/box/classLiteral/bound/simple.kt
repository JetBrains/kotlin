// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    val x: CharSequence = ""
    val klass = x::class
    return if (klass == String::class) "OK" else "Fail: $klass"
}
