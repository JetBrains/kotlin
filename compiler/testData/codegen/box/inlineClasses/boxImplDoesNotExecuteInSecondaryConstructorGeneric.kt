// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: Int> private constructor(val i: T) {
    @Suppress("SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS")
    constructor() : this(0 as T) {
        counter += 1
    }
}

var counter = 0

fun <T> id(t: T) = t

fun box(): String {
    val ic = IC<Int>()
    if (counter != 1) return "FAIL 1: $counter"
    counter = 0

    id(ic)
    if (counter != 0) return "FAIL 2: $counter"

    return "OK"
}
