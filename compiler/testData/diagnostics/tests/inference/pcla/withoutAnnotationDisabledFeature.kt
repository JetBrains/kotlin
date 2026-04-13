// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// IGNORE_BACKEND: WASM_JS, WASM_WASI
// LANGUAGE: -UseBuilderInferenceWithoutAnnotation

fun <K, V> buildMap(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> = mapOf()

fun box(): String {
    val x = buildMap {
        put("", "")
    }
    return "OK"
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter, typeWithExtension */
