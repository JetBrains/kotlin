// FIR_DUMP
package second

fun main() {
    val data = MyClass(object : Base<Base<Int>> {})
    data.foo()
}

interface Base<A> {
    fun foo() {}
}

class MyClass(val prop: second.Base<second.Base<Int>>): Base<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Base<Int>><!> by prop {
    interface Base
}
