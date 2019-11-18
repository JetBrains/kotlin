// IGNORE_BACKEND_FIR: JVM_IR
inline fun <reified T> isNullable() = null is T

fun box(): String =
        if (isNullable<String?>()) "OK" else "Fail"