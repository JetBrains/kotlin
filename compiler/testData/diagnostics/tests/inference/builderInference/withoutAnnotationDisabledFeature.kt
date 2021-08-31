// WITH_RUNTIME
// DONT_TARGET_EXACT_BACKEND: WASM
// !LANGUAGE: -UseBuilderInferenceWithoutAnnotation

fun <K, V> buildMap(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> = mapOf()

fun box(): String {
    val x = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildMap<!> {
        put("", "")
    }
    return "OK"
}