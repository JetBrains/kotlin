// FIR_IDENTICAL
// FIR_DUMP
package second

fun main() {
    val data = MyClass(object : Base {})
    data.foo()
}

interface Base {
    fun foo() {}
}

class MyClass(val prop: second.Base): Base by prop {
    interface Base
}
