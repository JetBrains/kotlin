// WITH_RUNTIME
// !DIAGNOSTICS: -UNUSED_VARIABLE

class Foo<T : Enum<T>>(val values: Array<T>)

fun foo(x: Array<out Enum<*>>) {
    val y = Foo(x)
}
