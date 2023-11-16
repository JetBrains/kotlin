// DUMP_IR
package second

fun box(): String {
    val data = MyClass(object : Base {})
    return data.foo()
}

interface Base {
    fun foo(): String = "OK"
}

class MyClass(val prop: second.Base): Base by prop {
    interface Base
}
