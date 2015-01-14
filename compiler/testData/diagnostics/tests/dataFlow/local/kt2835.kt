open class X(val s: String)

fun f(a: String?) {
    if (a != null) {
        object : X(<!DEBUG_INFO_SMARTCAST!>a<!>) { // Type mismatch: inferred type is kotlin.String? but kotlin.String was expected
        }
    }
}
