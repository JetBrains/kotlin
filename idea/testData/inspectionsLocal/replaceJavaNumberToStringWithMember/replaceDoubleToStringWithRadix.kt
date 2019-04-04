// WITH_RUNTIME
// PROBLEM: none
// ERROR: Too many arguments for public open fun toString(d: Double): String! defined in java.lang.Double

fun foo() {
    java.lang.Double.<caret>toString(5.0, 5)
}
