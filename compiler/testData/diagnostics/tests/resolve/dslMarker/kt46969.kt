// FIR_IDENTICAL
@DslMarker
annotation class Foo

@Foo
interface Scope<T> {
    fun value(value: T)
}

fun foo(block: Scope<Nothing>.() -> Unit) {}

inline fun <reified T> Scope<*>.nested(noinline block: Scope<T>.() -> Unit) {}
inline fun <reified K> Scope<*>.nested2(noinline block: Scope<K>.() -> Unit) {}


fun main() {
    foo {
        nested {
            value(1)

            nested2 {
                value("foo")
            }
        }
    }
}