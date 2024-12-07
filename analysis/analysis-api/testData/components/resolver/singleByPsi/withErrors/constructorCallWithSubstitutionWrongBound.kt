open class MyClass
class Foo<T : MyClass>(x: T)

fun main() {
    val a = <expr>Foo<String>()</expr>
}
