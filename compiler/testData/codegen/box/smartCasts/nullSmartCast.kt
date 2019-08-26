// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: WASM
// Wasm reason: Narrowing null reference to different types inline classes
fun String?.fooExtension() = this ?: "OK"

fun foo(i: Int?): String {
    if (i == null) return i.fooExtension()
    return "$i"
}

fun box() = foo(null)
