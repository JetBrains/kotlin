// KT-2823 TypeCastException has no message

fun box(): String {
    try {
        val a: Any? = null
        a as Array<String>
    }
    catch(e: TypeCastException) {
        if (e.getMessage() == "jet.Any? cannot be cast to jet.Array<jet.String>") {
            return "OK"
        }
    }
    return "fail"
}
