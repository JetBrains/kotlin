// WITH_STDLIB
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER on buildMap call (K)
// !LANGUAGE: +UseBuilderInferenceWithoutAnnotation

fun <K, V> buildMap(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> = mapOf()

fun box(): String {
    val x = buildMap {
        put("", "")
    }
    return "OK"
}