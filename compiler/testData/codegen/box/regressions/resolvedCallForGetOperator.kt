// V8 Crash https://bugs.chromium.org/p/v8/issues/detail?id=12640
// IGNORE_BACKEND: WASM

// WITH_STDLIB

val targetNameLists: Map<String, String> = mapOf("1"         to "OK")

fun <T> id(t: T) = t
fun foo(argumentName: String?): String? = id(targetNameLists[argumentName])

fun box() = foo("1")
