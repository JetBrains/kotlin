import kotlinx.cinterop.*

fun foo(f: Function1<*, Int>) = f

fun bar() {
    staticCFunction(::foo)
}
