// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> materialize(): K = TODO()

class Foo
class Inv<T>

fun <T> test1(x: Inv<out T>) {}
fun <T> test2(x: Inv<in T>) {}
fun <T> test3(x: Inv<T>) {}

fun main() {
    test1<Foo>(materialize())
    test2<Foo>(materialize())
    test3<Foo>(materialize())
}