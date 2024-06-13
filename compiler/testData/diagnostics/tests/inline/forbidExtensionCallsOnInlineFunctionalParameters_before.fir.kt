// LANGUAGE: -ForbidExtensionCallsOnInlineFunctionalParameters

fun main() {
    test { }
}
<!NOTHING_TO_INLINE!>inline<!> fun (() -> Unit).call() {
    invoke()
}
inline fun test(block: () -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>block<!>.call()
}
