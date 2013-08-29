open class X(val s: String)

fun f(a: String?) {
    if (a != null) {
        object : X(a) { // Type mismatch: inferred type is jet.String? but jet.String was expected
        }
    }
}