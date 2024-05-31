// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68546

class MyClass : GenericSuperClass<String>()

abstract class GenericSuperClass<T> : SuperClass()
abstract class SuperClass {
    fun <T> foo() {}

    @JvmName("foo2")
    fun foo() {}
}

fun main() {
    val m = MyClass()
    m.foo()
    m.foo<Int>()
}
