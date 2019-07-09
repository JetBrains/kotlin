// KJS_WITH_FULL_RUNTIME
//WITH_RUNTIME

fun box(): String {
    val map: Map<String, Boolean>? = mapOf()
    return if (map?.get("") == true) "fail" else "OK"
}