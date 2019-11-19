// !LANGUAGE: +NewInference +FunctionInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun interface KRunnable {
    fun invoke()
}

fun isNull(r: KRunnable?): Boolean {
    if (r == null) return true
    r.invoke()
    return false
}

fun nullableFun(fromNull: Boolean): (() -> Unit)? =
    if (fromNull) null else {{}}

fun box(): String {
    if (!isNull(nullableFun(true))) return "Fail 1"
    if (isNull(nullableFun(false))) return "Fail 2"
    if (!isNull(null)) return "Fail 3"
    if (isNull {}) return "Fail 4"
    return "OK"
}
