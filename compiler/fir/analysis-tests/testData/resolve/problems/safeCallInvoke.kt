class A {
    operator fun invoke() {}
}

class B {
    val bar: () -> Unit = {}
    val foo: A = A()
}

fun main(b: B?) {
    b?.bar() // allowed in FIR, prohibited in old FE
    b?.foo() // allowed in FIR, prohibited in old FE
}
