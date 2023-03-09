// FIR_IDENTICAL
// WITH_STDLIB

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

fun create() = "OK"

@Deprecated("Use create() instead()", replaceWith = ReplaceWith("create()"))
fun create(s: String) = create()

@Deprecated("Use create() instead()")
fun create(b: Boolean) = create()
