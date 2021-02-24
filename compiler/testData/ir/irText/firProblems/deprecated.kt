// FIR_IDENTICAL
// WITH_RUNTIME

fun create() = "OK"

@Deprecated("Use create() instead()", replaceWith = ReplaceWith("create()"))
fun create(s: String) = create()

@Deprecated("Use create() instead()")
fun create(b: Boolean) = create()
