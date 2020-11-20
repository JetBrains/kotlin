// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: NULLABLE_BOX_FUNCTION
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val targetNameLists: Map<String, String> = mapOf("1"         to "OK")

fun <T> id(t: T) = t
fun foo(argumentName: String?): String? = id(targetNameLists[argumentName])

fun box() = foo("1")
