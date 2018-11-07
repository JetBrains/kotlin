// !CHECK_TYPE
//KT-4420 Type inference with type projections

class Foo<T>
fun <T> Foo<T>.bar(): T = throw Exception()

fun main() {
    val f: Foo<out String> = Foo()
    f.bar() checkType { _<String>() }
}