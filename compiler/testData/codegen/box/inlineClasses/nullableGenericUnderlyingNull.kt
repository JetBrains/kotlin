// WITH_STDLIB
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper<T>(val value: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class BoundedByNullable<T : String?>(val value: T)

fun <T> id(t: T) = t

fun box(): String {
    val wrapper: Wrapper<Int?>? = Wrapper(null)
    if (wrapper == null) return "Fail 1"
    if (wrapper.toString() != "Wrapper(value=null)") return "Fail 2"
    if (id(wrapper) == null) return "Fail 3"
    val bounded: BoundedByNullable<String?>? = BoundedByNullable(null)
    if (bounded == null) return "Fail 4"
    if (listOf<Wrapper<Int?>?>(Wrapper(null), null).map { it == null } != listOf(false, true)) return "Fail 5"
    return "OK"
}
