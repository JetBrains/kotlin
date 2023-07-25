// ORIGINAL: /compiler/testData/diagnostics/tests/deprecated/propertyWithInvoke.fir.kt
// WITH_STDLIB
@Deprecated("No")
val f: () -> Unit = {}

fun test() {
    f()
}


fun box() = "OK".also { test() }
