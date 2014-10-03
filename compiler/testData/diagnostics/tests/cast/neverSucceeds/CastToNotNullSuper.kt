open class A {
    fun foo() {}
}
class B : A()

fun test(b: B?) {
    (b as A).foo()
}