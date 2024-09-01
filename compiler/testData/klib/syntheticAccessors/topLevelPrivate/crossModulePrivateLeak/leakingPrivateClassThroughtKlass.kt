// IGNORE_BACKEND: ANY
// ^^^ Muted because a private type is leaked from the declaring file, and the visibility validator detects this.
//     This test should be converted to a test that checks reporting private types exposure. To be done in KT-69681.

// MODULE: lib
// FILE: a.kt
private class Private

internal inline fun getPrivateKlass(): String {
    val klass = Private::class
    return klass.simpleName ?: "null"
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    val result = getPrivateKlass()
    if (result != "Private") return result
    return "OK"
}
