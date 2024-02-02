// FIR_IDENTICAL
// ISSUE: KT-65255
// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_ANONYMOUS_PARAMETER

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class JsoDsl

inline fun <T : Any> jso(): T = js("({})")
inline fun <T : Any> jso(block: @JsoDsl T.() -> Unit): T = jso<T>().apply(block)

val value = { int: Int? ->
    jso<dynamic> {
        foo = jso {
            bar = jso { // line 7

            }
        }
    }
}
