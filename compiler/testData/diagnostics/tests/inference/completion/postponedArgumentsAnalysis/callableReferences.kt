// !DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS -UNUSED_VARIABLE

class Foo<T>
class P<K, T>(x: K, y: T)

val Foo<Int>.bar: Foo<Int> get() = this

fun <T> Foo<T>.bar(x: String) = null as Foo<Int>

fun main() {
    val x: P<String, Foo<Int>.() -> Foo<Int>> = P("", <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty1<Foo<kotlin.Int>, Foo<kotlin.Int>>")!>Foo<Int>::bar<!>)
}