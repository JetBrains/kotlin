// !LANGUAGE: -ProperTypeInferenceConstraintsProcessing
// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS

fun <E : Enum<E>> createMap(enumClass: Class<E>) {}

fun reproduce() {
    val enumClass: Class<Enum<*>> = "any" as Class<Enum<*>>
    createMap(enumClass)
}
