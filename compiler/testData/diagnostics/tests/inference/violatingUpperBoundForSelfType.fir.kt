// !LANGUAGE: -ProperTypeInferenceConstraintsProcessing
// WITH_RUNTIME
// !DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS

fun <E : Enum<E>> createMap(enumClass: Class<E>) {}

fun reproduce() {
    val enumClass: Class<Enum<*>> = "any" as Class<Enum<*>>
    <!NEW_INFERENCE_ERROR!>createMap(enumClass)<!>
}
