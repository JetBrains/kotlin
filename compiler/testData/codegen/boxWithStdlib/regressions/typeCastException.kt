// KT-2823 TypeCastException has no message

fun box(): String {
    try {
        val a: Any? = null
        a as Array<String>
    }
    catch (e: TypeCastException) {
        if (e.getMessage() == "kotlin.Any? cannot be cast to kotlin.Array<kotlin.String>") {
            return "OK"
        }
    }
    return "fail"
}
