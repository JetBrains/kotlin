
fun def(i: Int = 0): Int {
    return i;
}

fun box():String {
    val clazz = Class.forName("SuperCallCheckKt")

    val method = clazz.getMethod("def\$default", Int::class.java, Int::class.java, Any::class.java)
    val result = method.invoke(null, -1, 1, null)

    if (result != 0) return "fail 1: $result"

    var failed = false
    try {
        method.invoke(null, -1, 1, "fail")
    } catch(e: Exception) {
        val cause = e.cause
        if (cause is java.lang.UnsupportedOperationException &&
            cause.message!!.startsWith("Super calls")) {
            failed = true
        }
    }

    return if (!failed) "fail" else "OK"
}
