// WITH_STDLIB
// IGNORE_BACKEND: WASM
// !LANGUAGE: -UseBuilderInferenceWithoutAnnotation

fun <K, V> buildMap(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> = mapOf()

fun box(): String {
    val x = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildMap<!> {
        put("", "")
    }
    return "OK"
}