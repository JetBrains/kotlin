// DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST

fun <T : Any> foo(items: List<T>, handler: (T) -> Unit) {}

class Foo<T>(x: T)

fun <T> materialize(): T = null as T

fun main(x: List<String>?) {
    foo(x?.map { Foo(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>it<!>) } ?: listOf(materialize<Foo<Nothing>>())) {}
}
