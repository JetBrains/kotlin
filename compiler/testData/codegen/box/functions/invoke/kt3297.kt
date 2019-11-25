// IGNORE_BACKEND_FIR: JVM_IR
//KT-3297 Calling the wrong function inside an extension method to the Function0 class

infix fun <R> Function0<R>.or(alt: () -> R): R {
    try {
        return this()
    } catch (e: Exception) {
        return alt()
    }
}

fun box(): String {
    return {
        throw RuntimeException("fail")
    } or {
        "OK"
    }
}