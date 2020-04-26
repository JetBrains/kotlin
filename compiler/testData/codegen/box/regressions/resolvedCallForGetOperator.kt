// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val targetNameLists: Map<String, String> = mapOf("1"         to "OK")

fun <T> id(t: T) = t
fun foo(argumentName: String?): String? = id(targetNameLists[argumentName])

fun box() = foo("1")

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: MINOR: NULLABLE_BOX_FUNCTION
