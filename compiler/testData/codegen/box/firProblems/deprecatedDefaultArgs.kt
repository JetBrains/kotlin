// WITH_RUNTIME

fun create() = "OK"

@Deprecated("Use create() instead()", replaceWith = ReplaceWith("create()"))
fun create(s: String) = create()

fun box(): String = create("FAIL")