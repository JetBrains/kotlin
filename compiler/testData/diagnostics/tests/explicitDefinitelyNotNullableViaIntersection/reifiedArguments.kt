// FIR_IDENTICAL
// SKIP_TXT
// !LANGUAGE: +DefinitelyNonNullableTypes

inline fun <reified T : Any> foo() {}

inline fun <reified F> bar() {
    foo<<!DEFINITELY_NON_NULLABLE_AS_REIFIED!>F & Any<!>>()
}

class KAnnotatedElement(val annotations: List<Any>)

inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    return null
}

private inline fun <reified T> KAnnotatedElement.findAnnotation(): T? =
    annotations.firstIsInstanceOrNull()