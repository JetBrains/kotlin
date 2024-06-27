// FIR_IDENTICAL
// WITH_STDLIB

fun create() = "OK"

@Deprecated("Use create() instead()", replaceWith = ReplaceWith("create()"))
fun create(s: String) = create()

@Deprecated("Use create() instead()")
fun create(b: Boolean) = create()
