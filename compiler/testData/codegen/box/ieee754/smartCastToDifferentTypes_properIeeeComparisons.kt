// !LANGUAGE: +ProperIeee754Comparisons
// IGNORE_BACKEND_FIR: JVM_IR

fun ne(x: Any, y: Any) = x is Double && y is Float && x != y
fun lt(x: Any, y: Any) = x is Double && y is Float && x < y
fun gt(x: Any, y: Any) = x is Double && y is Float && x > y

fun box(): String {
    if (ne(0.0, -0.0F)) return "fail 1"
    if (lt(0.0, -0.0F)) return "fail 2"
    if (gt(0.0, -0.0F)) return "fail 3"

    return "OK"
}
