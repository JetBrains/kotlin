import kotlinx.cinterop.*

fun foo(x: Any) = x

fun bar() {
    staticCFunction<String, Any>(::foo)
}
