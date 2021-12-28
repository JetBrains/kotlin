// WITH_STDLIB
// KJS_FULL_RUNTIME
// SKIP_MANGLE_VERIFICATION
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface I {
    companion object {
        val default: IC<String> by lazy(::IC)
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: String>(val ok: T = "OK" as T) : I

fun box(): String {
    return I.default.ok
}